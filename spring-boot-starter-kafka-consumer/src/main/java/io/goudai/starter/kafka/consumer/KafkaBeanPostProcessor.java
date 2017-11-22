package io.goudai.starter.kafka.consumer;

import io.goudai.starter.kafka.consumer.annotation.EnableKafka;
import io.goudai.starter.kafka.consumer.annotation.KafkaListener;
import io.goudai.starter.kafka.core.JsonUtils;
import io.goudai.starter.kafka.core.StringUtils;
import lombok.Setter;
import lombok.val;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private Map<String, KafkaConsumer> caches = new ConcurrentHashMap<>();
    private Map<String, AtomicBoolean> consumerRunningCache = new ConcurrentHashMap<>();
    private AtomicInteger POOL_SEQ = new AtomicInteger(1);
    private long timeout = 1000L;
    private int retry = 1;

    public KafkaBeanPostProcessor(Properties properties, int retry) {
        this.retry = retry;
        this.properties = properties;
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
                    val consumer = new KafkaConsumer<String, String>(config);
                    consumer.subscribe(asList(topic));

                    val key = group + topic;
                    caches.put(key, consumer);
                    consumerRunningCache.put(key, new AtomicBoolean(true));
                    String finalGroup = group;
                    ThreadKit.cachedThreadPool.execute(() -> {
                        Thread.currentThread().setName(finalGroup + "-" + topic + "-" + POOL_SEQ.getAndIncrement());
                        while (consumerRunningCache.get(key).get()) {
                            ConsumerRecords<String, String> consumerRecords = consumer.poll(timeout);
                            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                                for (int i = 0; i < retry; i++) {
                                    try {
                                        if (topic.equals(consumerRecord.topic())) {
                                            if("test01".equals(consumerRecord.topic())){
                                                logger.info("test01 : "+consumerRecord.value());
                                                break;
                                            }
                                            final String value = consumerRecord.value();
                                            if (StringUtils.isNotBlank(value)) {
                                                method.invoke(bean, consumerRecord);
                                            }
                                        }
                                        break;
                                    } catch (Throwable e) {
                                        try {
                                            TimeUnit.SECONDS.sleep(3);
                                        } catch (InterruptedException e1) {
                                            Thread.currentThread().interrupt();
                                        }
                                        if (i >= retry - 1) {
                                            logger.error(e.getMessage(), e);
                                            consumerRunningCache.get(key).set(false);
                                            caches.get(key).close();
                                            caches.remove(key);
                                            break;
                                        }
                                    }

                                }
                                if (consumerRunningCache.get(key).get()) {
                                    consumer.commitSync(singletonMap(
                                            new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                                            new OffsetAndMetadata(consumerRecord.offset() + 1)));
                                    logger.info(topic + ":" + finalGroup + ":" + consumerRecord.toString());
                                }

                            }
                        }

                        consumer.close();
                    });

                    logger.info(String.format("starting kafka consumer success,group [%s] topic [%s]", group, topic));

                }
            }

        }

        return bean;
    }

    @Override
    public void destroy() {
        for (AtomicBoolean atomicBoolean : consumerRunningCache.values()) {
            atomicBoolean.set(false);
        }
    }


}

