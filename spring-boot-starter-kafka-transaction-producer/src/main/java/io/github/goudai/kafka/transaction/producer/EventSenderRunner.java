package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.runner.zookeeper.AbstractMultipartRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author qingmu.io
 * 2018/11/19
 */
@Slf4j
public class EventSenderRunner extends AbstractMultipartRunner {

    @Autowired
    private Producer<String, String> producer;

    @Autowired
    private KafkaEventDatasource eventMapper;

    @Override
    public void apply(String projectId) throws Exception {
        final List<KafkaEvent> unsentEvents = eventMapper.getUnsentEvents();
        for (KafkaEvent event : unsentEvents) {
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(event.getTopic(), event.getId(), event.getPayload()));
            /* waiting for send result. */
            future.get(5, TimeUnit.SECONDS);
            log.info("send event [%s] successfully", event);
            eventMapper.onSendSuccessful(event);
        }
    }

    @Override
    public Set<String> getAllProjects() {
        return new HashSet<>(Arrays.asList("1"));
    }
}
