package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.runner.zookeeper.AbstractMultipartRunner;
import io.goudai.starter.runner.zookeeper.AbstractRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Set;

/**
 * @author qingmu.io
 * @date 2018/11/19
 */
@EnableConfigurationProperties(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties.class)
public class MultipartEventSenderRunner extends AbstractRunner {


    public MultipartEventSenderRunner(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties producerProperties) {
        super(producerProperties.getSenderName(), "1");
    }

    @Override
    public void doRun() throws Exception {

    }
}
