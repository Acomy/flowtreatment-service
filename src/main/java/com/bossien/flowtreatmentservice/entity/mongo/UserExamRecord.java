package com.bossien.flowtreatmentservice.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * 考试记录表
 */
@Data
@Document(collection = "user_exam_record")
public class UserExamRecord {

    @Id
    @Field("_id")
    public String id;

    public String userId;

    public String companyId;

    public Date createTime;

    public Date endTime;

    public Long duration;
    //20180511,20180512
    public Long createDay;

    public Integer score;

    public int calculationType;




}
