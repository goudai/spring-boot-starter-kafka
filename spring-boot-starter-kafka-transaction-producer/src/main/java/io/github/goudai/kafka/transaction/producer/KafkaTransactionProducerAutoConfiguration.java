package io.github.goudai.kafka.transaction.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
//@EnableConfigurationProperties(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties.class)
public class KafkaTransactionProducerAutoConfiguration {


    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public EventSenderRunner eventSenderRunner() {
        return new EventSenderRunner();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator() {
        return new IdGenerator() {
        };
    }


}
