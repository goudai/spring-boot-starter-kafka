package io.goudai.starter.kafka.producer.autoconfigure;

import io.goudai.starter.kafka.core.JsonUtils;
import io.goudai.starter.kafka.core.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(KafkaProducerAutoConfiguration.KafkaProducerProperties.class)
@Slf4j
public class KafkaProducerAutoConfiguration {

    @Bean
    public Producer<String, String> producer(KafkaProducerProperties producerProperties) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerProperties.bootstrapServers);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, String.valueOf(producerProperties.enableIdempotence));
        if (StringUtils.isNotBlank(producerProperties.transactionalId)) {
            properties.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, producerProperties.transactionalId);
        }
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producerProperties.keySerializer);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producerProperties.valueSerializer);
        if(!producerProperties.enableIdempotence){
            properties.put(ProducerConfig.ACKS_CONFIG, producerProperties.acks);
        }
        log.info("initing stringStringKafkaProducer using properties : {}", JsonUtils.toJson(properties));
        KafkaProducer<String, String> stringStringKafkaProducer = new KafkaProducer<>(properties);
        log.info("init stringStringKafkaProducer successfully {} using properties : {}", stringStringKafkaProducer, JsonUtils.toJson(properties));
        return stringStringKafkaProducer;
    }


    @Setter
    @Getter
    @ConfigurationProperties(prefix = "goudai.kafka.producer")
    public static class KafkaProducerProperties {

        private String bootstrapServers;

        private boolean enableIdempotence = false;

        private String transactionalId = "";

        private String acks = "-1";

        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";

        private String valueSerializer = "org.apache.kafka.common.serialization.StringSerializer";

    }

}
