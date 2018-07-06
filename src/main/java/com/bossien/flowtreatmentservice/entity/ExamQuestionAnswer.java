package com.bossien.flowtreatmentservice.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import java.util.Date;
import java.util.List;
@Data
public class ExamQuestionAnswer {
    private String id;
    private long examId;

    private long UserId;

    private List<Question> QuestionArray;

    private String createUser;

    private Date createDate;

    private String operUser;

    private Date operDate;
    private Date endDate;

    private int Score;
    private long duration;

    private int state;
    private String username;
    private long companyId;
    private String examName;
    private String telephone;
    private String nickname;


}