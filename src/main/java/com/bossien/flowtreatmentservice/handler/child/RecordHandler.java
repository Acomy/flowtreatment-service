package com.bossien.flowtreatmentservice.handler.child;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.handler.BasicHandler;
import com.bossien.flowtreatmentservice.handler.MessageHandler;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 考试的消息记录
 *
 * @author gb
 */
public class RecordHandler extends BasicHandler implements MessageHandler<ComputingResourceModel> {
    Logger logger = LoggerFactory.getLogger(RecordHandler.class);

    @Override
    public void process(ComputingResourceModel computingResourceModel) {
        outLog("--------start-------------------处理数据:"+computingResourceModel.toString());
        final String userId = computingResourceModel.getUserId().toString();
        String date = TimeUtil.getDate().toString();
        if (computingResourceModel.getCreateDate() != null) {
            date = TimeUtil.getDate(computingResourceModel.getCreateDate()).toString();
        }
        outLog("考试日期天数:"+date);
        final String score = String.valueOf(computingResourceModel.getScore());
        String companyId = computingResourceModel.getCompanyId().toString();
        outLog("userId=" + userId + "开始处理");
        if (userId == null) {
            outLog("没有userId,不处理");
            return;
        }
        outLog("score=" + score + "开始处理");
        if (score == null) {
            outLog("没有score,不处理");
            return;
        }
        outLog("companyId=" + companyId + "开始处理");
        if (companyId == null) {
            outLog("companyId,不处理");
            return;
        }
        if (computingResourceModel.getDuration() == null) {
            outLog("没有duration,不处理");
            return;
        }
        Long dateLong = LangUtil.parseLong(date);
        Long userIdLong = LangUtil.parseLong(userId);
        UserExamRecord saveUserExamRecord = operationUserExamRecord(computingResourceModel);
        //从mongo 读取数据 .默认mongo有数据
        List<Integer> range = operationSumService.getRecordByMongo(dateLong, userIdLong);
        outLog("获取全国当天的分数记录" + range.toString());
        if (computingResourceModel.getCalculationType() == 0) {
            List<Integer> recordByMongoAllRecord = operationSumService.getRecordByMongoAllRecord(userIdLong);
            Long increment1 = redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord())
                    .increment(userId, computingResourceModel.getDuration());
            outLog("userId=" + "所在公司:" + companyId + "答题总耗时:" + increment1);
            outLog("userId=" + "所在公司:" + companyId + "用户耗时的分数记录:" + recordByMongoAllRecord.toString());
            if(recordByMongoAllRecord!=null &&recordByMongoAllRecord.size()>0){
                Long averageIncrement = increment1 / recordByMongoAllRecord.size();
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                        .put(userId, averageIncrement.toString());
                outLog("userId=" + "所在公司:" + companyId + "平均耗时:" + averageIncrement);
            }else{
                outLog("userId=" + "所在公司:" + companyId + "平均耗时:没有计算出用户的平均耗时" );
            }
        }
        final Double currentDayTotal = operationSumService.getCurrentDayTotalScore(userId, range);
        outLog("获取全国当天的平均分数" + currentDayTotal);
        if (currentDayTotal == null) {
            outLog("不能计算用户平均分...");
        } else {
            //当天的答题分数计算 存入redis
            outLog("存入用户全国当天的:"+date+"的平均分数" + currentDayTotal);
            operationSumService.saveSumCurrentDayTotalToRedis(userId, dateLong.toString(), currentDayTotal);
            //截止目前每天分数统计
            List<Double> personScoreDoubles = operationSumService.getRecordByRedisAllDays(userIdLong);
            outLog("获取全国每天的分数记录列表" + personScoreDoubles.toString());
            //截止目前个人的当前总分(包含所有天数,取前5最高分)
            Double personAllEveryDayTotal = operationSumService.calculatingCountryCurrentTotalScore(personScoreDoubles);
            outLog("获取全国每天的分数记录列表前五最高的总分" +personAllEveryDayTotal);
            //设置个人在公司的总分
            //保存个人的总分到公司中,进行排名
            outLog("保存用户的总分:"+personAllEveryDayTotal+",至公司id:"+companyId);
            if (operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userId, companyId, personAllEveryDayTotal)) {
                outLog("保存用户的总分:"+personAllEveryDayTotal+",至公司id"+companyId+"以及存储到各个上层的公司");
                operationSumService.saveUserRankInCompanyBatch(userId, companyId, personAllEveryDayTotal);
                outLog("保存用户的总分:"+personAllEveryDayTotal+",至公司id"+companyId+"以及存储到各个上层的公司,并且计算耗时比...");
                operationSumService.saveDurationUserRankInCompany(userId, companyId, personAllEveryDayTotal);
            }
            //更新个人是否有效问题
            operationSumService.updateUserExamAvailable(userId, companyId);
            outLog("userId=" + "全国答题次数:," + "添加一次答题次数");
            redisTemplate.boundHashOps("commitCountInCountry")
                    .increment("country", 1);
            outLog("userId=" + "所在公司:" + companyId + "添加一次答题次数");
            redisTemplate.boundHashOps("commitCountInCountry")
                    .increment(companyId, 1);

        }
        outLog("--------success------------处理完成的数据为:"+computingResourceModel.toString());
}

    private UserExamRecord operationUserExamRecord(ComputingResourceModel computingResourceModel) {
        UserExamRecord userExamRecord = new UserExamRecord();
        userExamRecord.setScore(LangUtil.parseInt(computingResourceModel.getScore()));
        userExamRecord.setDuration(computingResourceModel.getDuration());
        userExamRecord.setCompanyId(computingResourceModel.getCompanyId().toString());
        Long dateLong = TimeUtil.getDate(computingResourceModel.getCreateDate());
        userExamRecord.setCreateDay(dateLong);
        userExamRecord.setUserId(computingResourceModel.getUserId());
        userExamRecord.setEndTime(computingResourceModel.getEndDate());
        userExamRecord.setCreateTime(computingResourceModel.getCreateDate());
        userExamRecord.setCalculationType(computingResourceModel.getCalculationType());
        if (computingResourceModel.calculationType != 0) {
            outLog("修改用户id:"+userExamRecord.getUserId()+" 分数:日期:"+dateLong+" 分数:"+userExamRecord.getScore()+" 公司id:"+userExamRecord.getCompanyId());
            operationSumService.updateUserExamRecord(userExamRecord);
        } else {
            outLog("添加用户id:"+userExamRecord.getUserId()+" 分数:日期:"+dateLong+" 分数:"+userExamRecord.getScore()+" 公司id:"+userExamRecord.getCompanyId());

            operationSumService.saveUserExamRecord(userExamRecord);
        }
        return userExamRecord;
    }

    private void outLog(String content) {
        logger.info("用户统计信息:" + content);
    }
}
