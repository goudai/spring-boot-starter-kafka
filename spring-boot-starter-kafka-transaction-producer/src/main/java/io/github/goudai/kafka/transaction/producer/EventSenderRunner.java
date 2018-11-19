package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.runner.zookeeper.AbstractRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author qingmu.io
 * @date 2018/11/19
 */
@EnableConfigurationProperties(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties.class)
@Slf4j
public class EventSenderRunner extends AbstractRunner {

    @Autowired
    private Producer<String, String> producer;

    @Autowired
    private GoudaiEventMapper eventMapper;

    private int sendTimeout;

    public EventSenderRunner(KafkaTransactionProducerAutoConfiguration.KafkaTransactionProducerProperties producerProperties) {
        super(producerProperties.getSenderName(), "1");
        this.sendTimeout = producerProperties.getSendTimeout();
    }

    @Override
    public void doRun() throws Exception {
        final List<GoudaiEvent> unsentEvents = eventMapper.getUnsentEvents();
        for (GoudaiEvent event : unsentEvents) {
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(event.getTopic(), event.getId(), event.getPayload()));
            /* waiting for send success. */
            future.get(sendTimeout, TimeUnit.SECONDS);
            log.info("send event [%s] successfully", event);
            eventMapper.onSendSuccessful(event);
        }

    }
}
