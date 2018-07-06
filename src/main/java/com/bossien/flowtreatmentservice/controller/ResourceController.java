package com.bossien.flowtreatmentservice.controller;

import com.alibaba.fastjson.JSONObject;
import com.bossien.common.base.ResponseData;
import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.common.util.QueueUtils;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.dao.StuTotalScoreRepository;
import com.bossien.flowtreatmentservice.dao.mongo.UserExamRecordRepository;
import com.bossien.flowtreatmentservice.entity.ExamQuestionAnswer;
import com.bossien.flowtreatmentservice.entity.StuTotalScore;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.handler.child.RecordHandler;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/areaTotalScore")
public class ResourceController {
    Logger logger = LoggerFactory.getLogger(ResourceController.class);
    @Autowired
    UserExamRecordRepository userExamRecordRepository;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    StuTotalScoreRepository stuTotalScoreRepository;


    @RequestMapping("/refreshCompany")
    public ResponseData refreshCompany() {
        logger.info("start refresh company info>>>");
        try {
            MemoryClient.instance.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseData.fail("refresh company info error!!!");
        }
        return ResponseData.ok();
    }


    @RequestMapping("/refreshCompanyScore")
    public String refreshCompanyScore(@RequestBody List<ExamQuestionAnswer> examQuestionAnswers) {
        logger.info("start refresh refreshCompanyScore info>>>");
        if (examQuestionAnswers != null && examQuestionAnswers.size() > 0) {
            logger.info("start refresh refreshCompanyScore info>>>size={}",examQuestionAnswers.size());
            for (ExamQuestionAnswer  examQuestionAnswer : examQuestionAnswers) {
                long userId = examQuestionAnswer.getUserId();
                ComputingResourceModel computingResourceModel = new ComputingResourceModel();
                computingResourceModel.setScore(examQuestionAnswer.getScore());
                computingResourceModel.setUserId(userId+"");
                computingResourceModel.setCompanyId(examQuestionAnswer.getCompanyId());
                computingResourceModel.setDuration(examQuestionAnswer.getDuration());
                computingResourceModel.setCreateDate(examQuestionAnswer.getCreateDate());
                computingResourceModel.setEndDate(examQuestionAnswer.getEndDate());
                computingResourceModel.setCalculationType(0);
                rabbitTemplate.convertAndSend(QueueUtils.CALCULATION_QUEUE, computingResourceModel);
            }
        }else{
            logger.info("start refresh refreshCompanyScore 参数不正确>>>");
            return "参数不正确";
        }
        return "成功";
    }
    @RequestMapping("/refreshUserScore")
    public String refreshUserScore(@RequestParam(value = "userId") String userId, @RequestParam(value = "createTime") String createTime, @RequestParam(value = "score") Integer score) {
        if (userId == null) {
            return JSONObject.toJSONString(new ResponseData(500, "用户id 为空", false));
        }
        if (createTime == null) {
            return JSONObject.toJSONString(new ResponseData(500, "创建时间 为空", false));
        }
        if (score == null) {
            return JSONObject.toJSONString(new ResponseData(500, "修改的分数 为空", false));
        }
        Date createDate = null;
        try {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            createDate = format.parse(createTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userId);
        //   dbObject.put("createTime", createDate);
        BasicDBObject fieldsObject = new BasicDBObject();
        //指定返回的字段
        fieldsObject.put("companyId", true);
        fieldsObject.put("userId", true);
        fieldsObject.put("score", true);
        fieldsObject.put("createTime", true);
        fieldsObject.put("endTime", true);
        fieldsObject.put("duration", true);
        fieldsObject.put("createDay", true);
        fieldsObject.put("calculationType", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords != null && userExamRecords.size() > 0) {
            for (UserExamRecord userExamRecord : userExamRecords) {
                Date mongoCreateTime = userExamRecord.getCreateTime();
                long time = mongoCreateTime.getTime();
                long time1 = createDate.getTime();
                long abs = Math.abs(time - time1);
                if (abs < 500) {
                    ComputingResourceModel computingResourceModel = new ComputingResourceModel();
                    computingResourceModel.setScore(score);
                    computingResourceModel.setUserId(userExamRecord.getUserId());
                    computingResourceModel.setCompanyId(LangUtil.parseLong(userExamRecord.getCompanyId()));
                    computingResourceModel.setDuration(userExamRecord.getDuration());
                    computingResourceModel.setCreateDate(userExamRecord.getCreateTime());
                    computingResourceModel.setEndDate(userExamRecord.getEndTime());
                    computingResourceModel.setCalculationType(1);
                    rabbitTemplate.convertAndSend(QueueUtils.CALCULATION_QUEUE, computingResourceModel);
                }
            }
        } else {
            return JSONObject.toJSONString(new ResponseData(500, "查询不到记录为空或者用户同时答题违规", false));
        }
        return JSONObject.toJSONString(new ResponseData(200, "修改成功！", true));
    }

}
