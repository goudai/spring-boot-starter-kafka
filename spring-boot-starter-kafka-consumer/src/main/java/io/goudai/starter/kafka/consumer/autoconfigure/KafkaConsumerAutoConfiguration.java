package io.goudai.starter.kafka.consumer.autoconfigure;

import io.goudai.starter.kafka.consumer.KafkaBeanPostProcessor;
import io.goudai.starter.kafka.core.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(KafkaConsumerAutoConfiguration.KafkaConsumerProperties.class)
@Slf4j
public class KafkaConsumerAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public KafkaBeanPostProcessor kafkaBeanPostProcessor(KafkaConsumerProperties kafkaConsumerProperties) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerProperties.bootstrapServers);
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, String.valueOf(kafkaConsumerProperties.enableAutoCommit));
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerProperties.autoOffsetReset);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerProperties.keyDeserializer);
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerProperties.valueDeserializer);
        log.info("initing KafkaBeanPostProcessor using properties : {}", JsonUtils.toJson(properties));
        KafkaBeanPostProcessor kafkaBeanPostProcessor = new KafkaBeanPostProcessor(properties, kafkaConsumerProperties);
        log.info("inited KafkaBeanPostProcessor successfully {} using properties : {}", kafkaBeanPostProcessor, JsonUtils.toJson(properties));
        return kafkaBeanPostProcessor;
    }


    @Setter
    @Getter
    @ConfigurationProperties(prefix = "goudai.kafka.consumer")
    public static class KafkaConsumerProperties {

        private String bootstrapServers;

        private long timeout = 3000L;

        private boolean enableAutoCommit = false;

        private String autoOffsetReset = "earliest";

        private String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";

        private String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";

        private AutoRestart autoRestart = new AutoRestart();


        private Email email = new Email();

        @Setter
        @Getter
        public static class AutoRestart {
            private boolean enabled = true;
            private int interval = 20;
        }


        @Setter
        @Getter
        public static class Email {
            private boolean enabled = true;
            private Smtp smtp = new Smtp();
            private int emailQueueSize = 100;

            @Setter
            @Getter
            public static class Smtp {
                private String host = "";
                private int port = 465;
                private String username = "";
                private String password = "";
                private boolean useSSL = true;
                private boolean debugMode = false;
                private String from = "";
                private List<String> to = Arrays.asList("");

            }
        }
    }

}
