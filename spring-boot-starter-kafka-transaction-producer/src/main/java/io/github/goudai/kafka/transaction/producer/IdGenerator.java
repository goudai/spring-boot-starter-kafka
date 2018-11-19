package io.github.goudai.kafka.transaction.producer;

import java.util.UUID;

public interface IdGenerator {

    default String nextId(){
        return UUID.randomUUID().toString().replace("-","");
    }
}
