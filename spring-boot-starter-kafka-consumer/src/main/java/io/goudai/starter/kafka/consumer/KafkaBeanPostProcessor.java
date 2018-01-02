package io.goudai.starter.kafka.consumer;

import io.goudai.starter.kafka.consumer.annotation.EnableKafka;
import io.goudai.starter.kafka.consumer.annotation.KafkaListener;
import io.goudai.starter.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration;
import io.goudai.starter.kafka.core.StringUtils;
import lombok.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

/**
 * Created by freeman on 17/2/21.
 */
@Setter
public class KafkaBeanPostProcessor implements BeanPostProcessor, DisposableBean {

    private Logger logger = LoggerFactory.getLogger(KafkaBeanPostProcessor.class);
    private Properties properties;
    private Map<String, AtomicBoolean> consumerRunningCache = new ConcurrentHashMap<>();
    private Map<String, AtomicInteger> consumerRestartedCount = new ConcurrentHashMap<>();
    private AtomicInteger POOL_SEQ = new AtomicInteger(1);
    private long timeout = 1000L;
    private KafkaConsumerAutoConfiguration.KafkaConsumerProperties.AutoRestart autoRestart;
    private BlockingQueue<ConsumeMetadata> queue = new LinkedBlockingQueue<>();

    public KafkaBeanPostProcessor(Properties properties, KafkaConsumerAutoConfiguration.KafkaConsumerProperties.AutoRestart autoRestart) {
        this.properties = properties;
        this.autoRestart = autoRestart;
    }

    @Override
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
        return o;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        val aClass = bean.getClass();
        val enableKafka = aClass.getAnnotation(EnableKafka.class);
        if (enableKafka != null) {
            for (val method : aClass.getMethods()) {
                val consumerAnnotation = method.getAnnotation(KafkaListener.class);
                if (consumerAnnotation != null) {
                    val topic = consumerAnnotation.topic();
                    if (StringUtils.isBlank(topic)) {
                        throw new IllegalArgumentException(String.format("topic is required by %s.%s @KafkaListener", aClass.getName(), method.getName()));
                    }
                    String group = consumerAnnotation.group();
                    if (StringUtils.isBlank(group)) {
                        logger.info("group is null,using default value in @EnableKafka");
                        if (StringUtils.isBlank(enableKafka.value())) {
                            throw new IllegalArgumentException(String.format("group is required by %s.%s @KafkaListener ", aClass.getName(), method.getName()));
                        } else {
                            group = enableKafka.value();
                        }
                    }
                    val config = (Properties) properties.clone();
                    config.put(ConsumerConfig.GROUP_ID_CONFIG, group);
                    ConsumeMetadata metadata = ConsumeMetadata.builder()
                            .bean(bean)
                            .config(config)
                            .group(group)
                            .topic(topic)
                            .method(method)
                            .build();
                    queue.add(metadata);

                }
            }
        }
        return bean;
    }

    @Override
    public void destroy() {
        this.autoRestart.setEnabled(false);
        for (AtomicBoolean atomicBoolean : consumerRunningCache.values()) {
            atomicBoolean.set(false);
        }
    }

    @PostConstruct
    public void start() {
        ThreadKit.cachedThreadPool.execute(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("QUEUE Consumer Thread " + POOL_SEQ.getAndIncrement());
            while (this.autoRestart.isEnabled()) {
                try {
                    ConsumeMetadata take = queue.take();
                    String key = take.group + ":" + take.topic;
                    startConsumer(take);
                    AtomicInteger atomicInteger = consumerRestartedCount.get(key);
                    if (atomicInteger == null) {
                        consumerRestartedCount.put(key, atomicInteger = new AtomicInteger(1));
                    }
                    logger.error("consumer " + key + "进行了第" + atomicInteger.get() + "次故障");
                    logger.info("启动 consumer group {}, topic {},metadata {} 成功 ", take.getGroup(), take.getTopic(), take);
                } catch (InterruptedException e) {
                    thread.interrupt();
                }
            }

        });
    }

    public void startConsumer(ConsumeMetadata metadata) {

        String group = metadata.group;
        String topic = metadata.topic;
        Method method = metadata.method;
        Object bean = metadata.bean;

        val consumer = new KafkaConsumer<String, String>(metadata.config);
        consumer.subscribe(asList(topic));

        val key = group + topic;
        consumerRunningCache.put(key, new AtomicBoolean(true));
        String finalGroup = group;
        ThreadKit.cachedThreadPool.execute(() -> {
            Thread.currentThread().setName(finalGroup + "-" + topic + "-" + POOL_SEQ.getAndIncrement());
            while (consumerRunningCache.get(key).get()) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(timeout);
                try {
                    for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                        if (topic.equals(consumerRecord.topic())) {
                            final String value = consumerRecord.value();
                            if (StringUtils.isNotBlank(value)) {
                                method.invoke(bean, consumerRecord);
                            }
                        }
                        consumer.commitSync(singletonMap(
                                new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                                new OffsetAndMetadata(consumerRecord.offset() + 1)));
                        logger.info(topic + ":" + finalGroup + ":" + consumerRecord.toString());
                    }
                } catch (Throwable e) {
                    AtomicInteger atomicInteger = consumerRestartedCount.get(key);
                    if (atomicInteger == null) {
                        consumerRestartedCount.put(key, atomicInteger = new AtomicInteger(0));
                    }
                    logger.error("consumer发生故障 当前第 " + atomicInteger.getAndIncrement() + "次故障", e);
                    consumerRunningCache.get(key).set(false);
                    try {
                        consumer.close();
                    } catch (Exception e1) {
                        logger.error("关闭close异常", e1);
                    }

                    try {
                        //故障之后sleep10s 避免死循环
                        logger.info("enter sleep {} s", this.autoRestart.getInterval());
                        TimeUnit.SECONDS.sleep(this.autoRestart.getInterval());
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                    if (this.autoRestart.isEnabled()) {
                        //重新放回启动队列
                        queue.add(metadata);
                        logger.info("将" + metadata + "重新放回队列");
                    } else {
                        logger.warn("autoRestart is disabled,consumer close ,{},{}", topic, group);
                    }

                }
            }
        });
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    @Getter
    @ToString
    public static class ConsumeMetadata {
        private Method method;
        private Object bean;
        private String group;
        private String topic;
        private Properties config;
    }


}

