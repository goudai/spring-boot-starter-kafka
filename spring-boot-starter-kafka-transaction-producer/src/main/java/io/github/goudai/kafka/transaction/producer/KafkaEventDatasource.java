package io.github.goudai.kafka.transaction.producer;


import java.util.List;

/**
 * @author qingmu.io
 *  2018/11/19
 */

public interface KafkaEventDatasource {

    /**
     * 该接口将事件插入到数据库中，请保证当前这方法与当前业务使用的是同一套ORM框架,
     * 即同一条javax.sql.Connection
     *
     * @param event
     */
    void insertEvent(KafkaEvent event);

    /**
     * 该接口请返回所有的为发送的到mq的事件列表
     *
     * @return
     */
    List<KafkaEvent> getUnsentEvents();

    /**
     * 该接口会在成功发送到Kafka之后调用，请在该接口更新DB中的event status。
     *
     * @param event
     */
    void onSendSuccessful(KafkaEvent event);

}
