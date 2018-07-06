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
@Document(collection = "ranking_record")
public class RankingRecord {

    @Id
    @Field("_id")
    public String id;
    //0 个人 1 公司 2 显示市区 3 市 4省 5 机构 6 国家
    public int type;
    //0否 1 市
    public int isRegulatory;

    public String userId;

    public String userName;

    public String companyId;

    public String companyName;

    public Date createTime;

    public Long duration;

    public Double score;

    public Long firstRanking; //国家

    public Long secondRanking; //机构

    public Long thirdRanking; //省

    public Long fourthRanking;//市

    public Long fifthRanking; //区

    public Long ranking; //所属单位
}
