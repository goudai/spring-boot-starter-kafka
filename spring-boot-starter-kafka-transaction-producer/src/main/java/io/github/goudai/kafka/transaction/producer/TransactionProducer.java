package io.github.goudai.kafka.transaction.producer;

/**
 * @author qingmu.io
 *  2018/11/19
 */
public interface TransactionProducer {


    void send(String topic, Object object);

}

