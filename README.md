# Usage
## dependency
* kafka client 1.0.0

## consumer

* add dependency to maven
 
 ```xml
<dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-consumer</artifactId>
     <version>1.0.0</version>
 </dependency>
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-core</artifactId>
     <version>1.0.0</version>
 </dependency>
 ```
 
 * using on spring boot 
 
```yaml
# application.yml
goudai:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
      
``` 
```java
/**
* 括号中指定group
*/
@EnableKafka("user-consumer")
public class UserConsumer {
    
    @KafkaListener(topic = "xxx")
    public void onUserRegisterCouponGranted(KafkaContext kafkaContext) {
        System.out.println(JsonUtils.toJson(kafkaContext));
    }
}

```
 
 
## producer

* add dependency to maven
 
 ```xml
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-core</artifactId>
     <version>1.0.0</version>
 </dependency>
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-producer</artifactId>
     <version>1.0.0</version>
 </dependency>
 ```
 
 * using on spring boot 
 
```yaml
# application.yml
goudai:
  kafka:
    producer:
      bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
      enable-idempotence: true  #当设置事物id时必须设置为true
      transactionalId: ${spring.application.name}-transactional-id # 是否开启事物支持
      
``` 
```java

@Component
public class UserProducer {

    @Autowired
    Producer<String, String> producer;

    public void sendMsg()  {
        producer.beginTransaction();
        producer.send(new ProducerRecord<String, String>("topic","kafkaContext json"));
        producer.commitTransaction();

    }
}

```