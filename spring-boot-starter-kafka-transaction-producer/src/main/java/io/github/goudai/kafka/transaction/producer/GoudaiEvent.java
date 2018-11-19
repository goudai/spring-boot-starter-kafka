package io.github.goudai.kafka.transaction.producer;

import io.gd.generator.annotation.Field;
import io.gd.generator.annotation.query.Query;
import io.gd.generator.annotation.view.View;
import io.gd.generator.api.query.Predicate;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

import static io.gd.generator.api.query.Predicate.EQ;

/**
 * @author qingmu.io
 * @date 2018/11/19
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class GoudaiEvent {

    @Id
    @Field(label = "id")
    @NotBlank
    @Column(columnDefinition = "BigInt(20)")
    private String id;

    @Query({EQ})
    @Field(label = "项目id")
    @View
    @NotBlank
    @Column(columnDefinition = "BigInt(20)")
    private String projectId;

    @NotBlank
    @Field(label = "主题")
    @NotNull
    private String topic;

    @NotNull
    @Field(label = "创建时间")
    private Date createdTime;

    @NotNull
    @Version
    @Field(label = "乐观锁")
    private Integer version;

    @NotNull
    @Field(label = "是否发送")
    @Query(Predicate.EQ)
    private Boolean isSent;

    @Field(label = "发送时间")
    private Date sentTime;

    @Field(label = "消息体")
    @NotBlank
    @Lob
    private String payload;


}
