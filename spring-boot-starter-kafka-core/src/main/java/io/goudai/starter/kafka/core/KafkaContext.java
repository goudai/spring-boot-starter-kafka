package io.goudai.starter.kafka.core;

import lombok.*;

/**
 * Created by freeman on 17/5/3.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KafkaContext {
	private String projectId;
	private String refId;
	private String token;
}
