package io.goudai.starter.kafka.consumer;

import io.goudai.starter.kafka.consumer.annotation.EnableKafka;
import io.goudai.starter.kafka.consumer.annotation.KafkaListener;
import io.goudai.starter.kafka.consumer.autoconfigure.KafkaConsumerAutoConfiguration;
import io.goudai.starter.kafka.core.StringUtils;
import jodd.mail.Email;
import jodd.mail.SendMailSession;
import jodd.mail.SmtpServer;
import lombok.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
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
    private long timeout;
    private KafkaConsumerAutoConfiguration.KafkaConsumerProperties.AutoRestart autoRestart;
    private KafkaConsumerAutoConfiguration.KafkaConsumerProperties kafkaConsumerProperties;
    private BlockingQueue<ConsumeMetadata> queue = new LinkedBlockingQueue<>();

    private BlockingQueue<EmailMate> emailQueue;
    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    Environment environment;
    private SmtpServer smtpServer;

    public KafkaBeanPostProcessor(Properties properties, KafkaConsumerAutoConfiguration.KafkaConsumerProperties kafkaConsumerProperties) {
        this.properties = properties;
        this.autoRestart = kafkaConsumerProperties.getAutoRestart();
        this.kafkaConsumerProperties = kafkaConsumerProperties;
        if (kafkaConsumerProperties.getEmail().isEnabled()) {
            this.emailQueue = new ArrayBlockingQueue<>(kafkaConsumerProperties.getEmail().getEmailQueueSize());
            final KafkaConsumerAutoConfiguration.KafkaConsumerProperties.Email.Smtp smtp = kafkaConsumerProperties.getEmail().getSmtp();
            this.smtpServer = SmtpServer.create()
                    .host(smtp.getHost())
                    .port(smtp.getPort())
                    .ssl(smtp.isUseSSL())
                    .auth(smtp.getUsername(), smtp.getPassword())
                    .debugMode(smtp.isDebugMode())
                    .buildSmtpMailServer();
        } else {
            this.emailQueue = new ArrayBlockingQueue<>(0);
        }
        this.timeout = kafkaConsumerProperties.getTimeout();


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
            thread.setName("QUEUE-Consumer-Thread " + POOL_SEQ.getAndIncrement());
            while (this.autoRestart.isEnabled()) {
                try {
                    ConsumeMetadata take = queue.take();
                    String key = take.group + ":" + take.topic;
                    startConsumer(take);
                    AtomicInteger atomicInteger = consumerRestartedCount.get(key);
                    if (atomicInteger == null) {
                        consumerRestartedCount.put(key, new AtomicInteger(0));
                        logger.info("starting consumer group {}, topic {},metadata {} successfully ", take.getGroup(), take.getTopic(), take);
                    } else {
                        logger.error("The {} restart of the consumer {}-{} succeeded", atomicInteger.get(), key);
                    }

                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    @PostConstruct
    @ConditionalOnProperty(name = "goudai.kafka.consumer.email.enabled", havingValue = "true")
    public void startEmail() {
        final String[] activeProfiles = environment.getActiveProfiles();
        final String activeProfile = activeProfiles.length == 1 ? activeProfiles[0] : environment.getDefaultProfiles()[0];
        ThreadKit.cachedThreadPool.execute(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("Email-Consumer-Thread " + POOL_SEQ.getAndIncrement());
            while (kafkaConsumerProperties.getEmail().isEnabled()) {
                try {
                    final EmailMate take = emailQueue.take();

                    try (final StringWriter out = new StringWriter();
                         final PrintWriter printWriter = new PrintWriter(out);
                         final SendMailSession session = smtpServer.createSession();) {
                        session.open();
                        take.throwable.printStackTrace(printWriter);
                        final KafkaConsumerAutoConfiguration.KafkaConsumerProperties.Email.Smtp smtp = kafkaConsumerProperties.getEmail().getSmtp();
                        session.sendMail(Email.create()
                                .from(smtp.getFrom())
                                .to(smtp.getTo().toArray(new String[smtp.getTo().size()]))
                                .subject(activeProfile + "-狗带kafka Consumer报警！" + applicationName)
                                .htmlMessage(
                                        String.format("<html><META http-equiv=Content-Type content=\"text/html; " +
                                                "charset=utf-8\"><body>%s</body></html>", out.toString().replaceAll("(\r\n|\n)", "<br />"))
                                        , "utf-8")
                        );
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }


    public void startConsumer(ConsumeMetadata metadata) {
        ThreadKit.cachedThreadPool.execute(() -> {
            String group = metadata.group;
            String topic = metadata.topic;
            Method method = metadata.method;
            Object bean = metadata.bean;

            val consumer = new KafkaConsumer<String, String>(metadata.config);
            consumer.subscribe(asList(topic));

            val key = group + topic;
            consumerRunningCache.put(key, new AtomicBoolean(true));
            Thread.currentThread().setName(group + "-" + topic + "-" + POOL_SEQ.getAndIncrement());
            boolean isException = false;
            while (consumerRunningCache.get(key).get()) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(30));
                isException = false;
                try {
                    handleRecord(topic, method, bean, consumer, consumerRecords);
                } catch (Throwable e) {
                    isException = true;
                    handeException(metadata, group, topic, consumer, key, e);
                }
            }
            if (!isException) {
                consumer.close();
            }

        });
    }

    private void handleRecord(String topic, Method method, Object bean, KafkaConsumer<String, String> consumer, ConsumerRecords<String, String> consumerRecords) throws Throwable {
        for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
            if (topic.equals(consumerRecord.topic())) {
                final String value = consumerRecord.value();
                if (StringUtils.isNotBlank(value)) {
                    try {
                        method.invoke(bean, consumerRecord);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() != null && e.getCause() instanceof SkipMessageException) {
                            logger.warn("skip message " + e.getCause().getMessage(), e.getCause());
                        } else {
                            throw e.getCause() != null ? e.getCause() : e;
                        }
                    }
                }
            }

            consumer.commitSync(singletonMap(
                    new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
                    new OffsetAndMetadata(consumerRecord.offset() + 1)));
        }
    }

    private void handeException(ConsumeMetadata metadata, String group, String topic, KafkaConsumer<String, String> consumer, String key, Throwable e) {
        AtomicInteger atomicInteger = consumerRestartedCount.get(key);
        if (atomicInteger == null) {
            consumerRestartedCount.put(key, atomicInteger = new AtomicInteger(0));
        }
        final String message = "consumer发生故障 当前第 " + atomicInteger.getAndIncrement() + "次故障";
        if (kafkaConsumerProperties.getEmail().isEnabled()) {
            emailQueue.offer(EmailMate.builder().message(message).throwable(e).build());
        }
        logger.error(message, e);
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
            logger.error(e.getMessage(), e);
        }
        if (this.autoRestart.isEnabled()) {
            //重新放回启动队列
            queue.add(metadata);
            logger.info("将" + metadata + "重新放回队列");
        } else {
            logger.warn("autoRestart is disabled,consumer close ,{},{}", topic, group);
        }
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

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    @Getter
    @ToString
    public static class EmailMate {
        private Throwable throwable;
        private String message;
    }


}

