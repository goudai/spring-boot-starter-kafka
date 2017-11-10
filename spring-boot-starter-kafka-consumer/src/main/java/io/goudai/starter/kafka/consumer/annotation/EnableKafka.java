package io.goudai.starter.kafka.consumer.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by freeman on 17/2/21.
 */
@Component
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableKafka {
	String value() default "";
}
