# dependency
* kafka client 1.0.0
* spring boot

# Usage
## Download 
wget https://github.com/goudai/spring-boot-starter-kafka/archive/spring-boot-starter-kafka-1.0.1.zip

## Update

* V1.0.1 
</br> 1.支持故障重启，默认启用故障重启，重启间隔为20s秒默认
</br> 2.支持idea自动补全
    
## consumer

* add dependency to maven
 
 ```xml
<dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-consumer</artifactId>
    <version>1.0.1</version>
 </dependency>
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-core</artifactId>
    <version>1.0.1</version>
 </dependency>
 ```
 
 * using on spring boot 
 
```yaml
# application.yml
goudai:
  kafka:
    consumer:
      bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
      auto-restart:
        enabled: false # 默认为true 设置为false 表示关闭故障重启
        interval: 20   # 默认间隔20s
``` 
```java
/**
* 括号中指定group
*/
@EnableKafka("user-consumer")
public class UserConsumer {
    
    @KafkaListener(topic = "xxx")
    public void onUserRegisterCouponGranted(ConsumerRecord<String, String> consumerRecord) {
        System.out.println(JsonUtils.toJson(consumerRecord));
    }
}

```
 
 
## producer

* add dependency to maven
 
 ```xml
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-core</artifactId>
    <version>1.0.1</version>
 </dependency>
 <dependency>
     <groupId>io.goudai</groupId>
     <artifactId>spring-boot-starter-kafka-producer</artifactId>
    <version>1.0.1</version>
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