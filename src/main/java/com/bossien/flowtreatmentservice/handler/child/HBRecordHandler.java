package com.bossien.flowtreatmentservice.handler.child;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.handler.BasicHandler;
import com.bossien.flowtreatmentservice.handler.MessageHandler;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import com.bossien.flowtreatmentservice.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * 湖北的考试记录
 *
 * @author gb
 */

public class HBRecordHandler extends BasicHandler implements MessageHandler<ComputingResourceModel> {
    Logger logger = LoggerFactory.getLogger(HBRecordHandler.class);

    List<Long> start_endDay;

    //查询参赛人数
    //查询参赛率
    @Override
    public void process(ComputingResourceModel computingResourceModel) {
        outLog("--------start-------------------处理数据:"+computingResourceModel.toString());
        String platformCode = operationSumService.getPlatformCode();
        final String userId = computingResourceModel.getUserId().toString();
        String date = TimeUtil.getDate().toString();
        if (computingResourceModel.getCreateDate() != null) {
            date = TimeUtil.getDate(computingResourceModel.getCreateDate()).toString();
        }
        Long dateLong = LangUtil.parseLong(date);
        outLog("考试日期天数:"+dateLong);
        final String score = String.valueOf(computingResourceModel.getScore());
        outLog("考试分数:"+score);
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
        Long userIdLong = LangUtil.parseLong(userId);
        UserExamRecord saveUserExamRecord = operationUserExamRecord(computingResourceModel);
        if(saveUserExamRecord==null){
            logger.error("更新mongo 失败!!!");
            return ;
        }
        List<Integer> range =new ArrayList<>();
        if(platformCode.equals(PlatformCode.HB)){
            range =operationSumService.getRecordByMongo(dateLong, userIdLong);
            outLog("获取湖北当天的分数记录" + range.toString());
        }else if(platformCode.equals(PlatformCode.YNJT)){
            range =operationSumService.getRecordByMongo(dateLong, userIdLong);
            outLog("获取云南当天的分数记录" + range.toString());
        }

        if (computingResourceModel.getCalculationType() == 0) {
            if(platformCode.equals(PlatformCode.HB)){
                //获取当天最高分的耗时
                Long hbMaxScoreByMongo = operationSumService.getHbMaxScoreByMongo(dateLong, userIdLong);
                outLog("获取湖北用户当天最高分的耗时:{}" + hbMaxScoreByMongo);
                //添加用户当天耗时
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationDayRecord(dateLong))
                        .put(userId,hbMaxScoreByMongo.toString());
                //计算用户平均耗时之和
                Long hbDurationEveryDayRecordTotal = operationSumService.getHbDurationEveryDayRecordTotal(userIdLong);
                outLog("获取湖北用户平均分的耗时:{}" + hbDurationEveryDayRecordTotal);
                //计算用户的总耗时
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                        .put(userId, hbDurationEveryDayRecordTotal.toString());
            }else if(platformCode.equals(PlatformCode.YNJT)){
                //获取用户的平均耗时
                Long yNAverageSeekTime = operationSumService.getYNAverageSeekTime(userIdLong);
                outLog("获取云南用户总的耗时:{}" + yNAverageSeekTime);
                //计算用户的总耗时
                 redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                        .put(userId, yNAverageSeekTime.toString());
                outLog("获取云南用户总的耗时:{}" + yNAverageSeekTime);
            }
        }
         Double currentDayTotal =0.00;
        if(platformCode.equals(PlatformCode.HB)){
             currentDayTotal = operationSumService.getHbCurrentDayTotalScore(userId, range);
            outLog("获取湖北的当天最高分:"+currentDayTotal);
        }else if(platformCode.equals(PlatformCode.YNJT)){
            currentDayTotal = operationSumService.getYnCurrentDayTotalScore(userId, range);
            outLog("获取云南的当天平均分:"+currentDayTotal);
        }
        if (currentDayTotal == null) {
            outLog("不能计算用户平均分...");
        } else {
            operationSumService.saveSumCurrentDayTotalToRedis(userId, dateLong.toString(), currentDayTotal);
            outLog("添加用户当天的平均分添加到个人当天总分"+currentDayTotal);
            //截止目前每天分数统计
            List<Double> personScoreDoubles = operationSumService.getRecordByRedisAllDays(userIdLong);
            outLog("截止目前每天分数的分数列表"+personScoreDoubles.toString());
            //截止目前个人的当前总分(包含所有天数,取前5最高分)
            Double personAllEveryDayTotal = operationSumService.calculatingProvinceCurrentTotalScore(personScoreDoubles);
            outLog("结果截止目前个人的总分"+personAllEveryDayTotal);
            //设置个人在公司的总分
            //保存个人的总分到公司中,进行排名
            outLog("保存用户的总分:"+personAllEveryDayTotal+",至公司id:"+companyId);
            if (operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userId, companyId, personAllEveryDayTotal)) {
                operationSumService.saveHbUserRankInCompanyBatch(userId, companyId, personAllEveryDayTotal);
                outLog("保存用户的总分:"+personAllEveryDayTotal+",至公司id:"+companyId+"进行耗时比计算");
                operationSumService.saveDurationHBUserRankInCompany(userId, companyId, personAllEveryDayTotal);
            }
            //更新个人是否有效问题
            operationSumService.updateHbUserExamAvailable(userId, companyId);
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
        Long date = TimeUtil.getDate(computingResourceModel.getCreateDate());
        userExamRecord.setCreateDay(date);
        userExamRecord.setUserId(computingResourceModel.getUserId());
        userExamRecord.setEndTime(computingResourceModel.getEndDate());
        userExamRecord.setCreateTime(computingResourceModel.getCreateDate());
        userExamRecord.setCalculationType(computingResourceModel.getCalculationType());
        UserExamRecord result = null ;
        if (computingResourceModel.calculationType != 0) {
            outLog("修改用户id:"+userExamRecord.getUserId()+" 分数:日期:"+date+" 分数:"+userExamRecord.getScore()+" 公司id:"+userExamRecord.getCompanyId());
            result= operationSumService.updateUserExamRecord(userExamRecord);
        } else {
            outLog("添加用户id:"+userExamRecord.getUserId()+" 分数:日期:"+date+" 分数:"+userExamRecord.getScore()+" 公司id:"+userExamRecord.getCompanyId());
            result = operationSumService.saveUserExamRecord(userExamRecord);
        }
        return result;
    }

    private void outLog(String content) {
        logger.info("用户统计信息:" + content);
    }
}
