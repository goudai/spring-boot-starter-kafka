package io.github.goudai.kafka.transaction.producer;

import io.goudai.starter.kafka.core.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

public class TransactionProducerImpl implements TransactionProducer {

    @Autowired
    private KafkaEventDatasource goudaiEventMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public void send(String topic, Object payload) {
        KafkaEvent event = new KafkaEvent();
        event.setTopic(topic);
        event.setPayload(JsonUtils.toJson(payload));
        event.setProjectId("1");
        event.setId(idGenerator.nextId());
        event.setVersion(0);
        event.setIsSent(false);
        event.setCreatedTime(new Date());
        goudaiEventMapper.insertEvent(event);
    }


}
