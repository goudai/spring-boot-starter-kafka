package io.goudai.starter.kafka.consumer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by freeman on 17/2/21.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface KafkaListener {

	String group() default "";

	String topic();

	String description() default "";

}
