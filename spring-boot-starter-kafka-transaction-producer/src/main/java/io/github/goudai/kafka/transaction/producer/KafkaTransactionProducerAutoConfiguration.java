package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.kafka.core.JsonUtils;
import io.goudai.starter.kafka.core.StringUtils;
import io.goudai.starter.kafka.producer.autoconfigure.KafkaProducerAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@Slf4j
@EnableConfigurationProperties(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties.class)
public class KafkaTransactionProducerAutoConfiguration {

    @Autowired
    private KafkaTransactionProducerProperties producerProperties;

    @Value("${spring.application.name}")
    private String applicationName;



    @Setter
    @Getter
    @ConfigurationProperties(prefix = "goudai.kafka.producer.transaction")
    public static class KafkaTransactionProducerProperties {

        private String senderName;

    }

}
