package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.kafka.core.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableConfigurationProperties(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties.class)
public class KafkaTransactionProducerAutoConfiguration {

    @Autowired
    private KafkaTransactionProducerProperties producerProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public EventSenderRunner eventSenderRunner(KafkaTransactionProducerProperties producerProperties) {
        if (StringUtils.isBlank(producerProperties.senderName)) {
            producerProperties.senderName = applicationName;
        }
        return new EventSenderRunner(producerProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(){
        return new IdGenerator(){};
    }




    @Setter
    @Getter
    @ConfigurationProperties(prefix = "goudai.kafka.producer.transaction")
    public static class KafkaTransactionProducerProperties {

        private String senderName;

        private int sendTimeout = 5;

    }

}
