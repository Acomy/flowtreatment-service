package com.bossien.flowtreatmentservice.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;
@Data
public class Question {

    private int questionId;

    private int Score;

    private String Analysis;

    /**
     * 试题结果
     */
    private byte result;

    private String answer;

    private Date CreatedDate;

    /***
     * mark 为0： 忽略，为1 ：已标记
     */
    private byte markstate;
    /***
     * 用户答题
     */
    private String option;


}
