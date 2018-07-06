package com.bossien.flowtreatmentservice.service.impl;

import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.service.SyncMongoRecordToRedisBatchService;
import com.bossien.flowtreatmentservice.task.CountryConvertTask;
import com.bossien.flowtreatmentservice.task.HBConvertTask;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import com.bossien.flowtreatmentservice.utils.TimeUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SyncMongoRecordToRedisBatchServiceImpl implements SyncMongoRecordToRedisBatchService {
    private Logger logger = LoggerFactory.getLogger(SyncMongoRecordToRedisBatchServiceImpl.class);
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    OperationSumService operationSumService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    TimeManager timeManager;
    @Autowired
    CountryConvertTask countryConvertTask;
    @Autowired
    HBConvertTask hBConvertTask;

    @Override
    public void batch() {

        logger.info("开始更新redis的数据");
        Map<String, Long> userIdCompanyIdMaps = operationSumService.queryUserIdAndCompanyIdsAll();
        logger.info("查询用户信息");
        redisTemplate.delete("totalCount");
        redisTemplate.delete("commitCountInCountry");
        redisTemplate.delete(RedisStatusKeyFactory.newCompanyCountryNumber());
        //查询所有mongo的数据
        //查询所有的用户,查询所有的用户的分组信息
        DBObject dbObject = new BasicDBObject();
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("userId", true);
        fieldsObject.put("createDay", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        outLog("userId=" + "全国答题次数:," + "添加一次答题次数");
        redisTemplate.boundHashOps("commitCountInCountry")
                .put("country", userExamRecords.size());
        logger.info("查询mongo中用的用户数量为:" + userExamRecords.size());
        Set<String> userIds = new HashSet<>();
        if (userExamRecords != null && userExamRecords.size() > 0) {
            for (UserExamRecord userExamRecord : userExamRecords) {
                userIds.add(userExamRecord.getUserId());
            }
        }
        String startDay = timeManager.getStartDay();
        int size = userIds.size();
        logger.info("项目开始时间:" + startDay);
        redisTemplate.boundHashOps("batch").put("需要处理用户数量", size + "");
        for (String userId : userIds) {
            logger.info("开始处理用户userId{}", userId);
            redisTemplate.boundHashOps("batch").increment("已处理用户数量", 1);
            //所有开始次数
            List<UserExamRecord> records = operationSumService.getRecordByMongoAllRecordAndDuration(LangUtil.parseLong(userId));
            logger.info("查询处理用户userId{},的所有记录", records.toString());
            //判断用户是否有效
            Double personExamAvailable = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                    .score(userId.toString());
            String companyId = userIdCompanyIdMaps.get(userId).toString();
            if (personExamAvailable != null) {
                outLog("userId=" + userId + "有效:");
            } else {
                for (UserExamRecord userExamRecord : records) {
                    if (userExamRecord.getScore().longValue() >= 60 && records.size() >= 5 && personExamAvailable == null) {
                        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                                .add(userId, 1);
                        //全国单位总有效的参赛人数
                        redisTemplate.boundHashOps("totalCount").increment("ExamPersonCount", 1);
                        //单位有效参赛人数
                        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(companyId.toString(), 1);
                        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                        if (longs != null && longs.size() > 0) {
                            for (Long parentId : longs) {
                                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(parentId.toString(), 1);
                            }
                        }
                        outLog("userId=" + userId + "有效:" + personExamAvailable);
                        break;
                    }
                }
            }
            //总耗时
            Long totalDuration = 0L;
            if (records != null && records.size() > 0) {
                for (UserExamRecord record : records) {
                    totalDuration += record.getDuration();
                }
                Long duration = totalDuration / records.size();
                //添加总耗时
                logger.info("存入用户{}全国用户的总耗时:{}", userId, totalDuration);
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord()).put(userId, totalDuration.toString());
                //添加平均耗时
                logger.info("存入用户{}全国用户的平均耗时:{}", userId, duration);
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord()).put(userId, duration.toString());
                //补充个人天数总分
                List<Long> startEndDay = TimeUtil.getStart_endDay(startDay, TimeUtil.getDate().toString());
                for (Long aLong : startEndDay) {
                    //查询指定天数的记录
                    List<Integer> recordByMongo = operationSumService.getRecordByMongo(aLong, LangUtil.parseLong(userId));
                    if (recordByMongo == null || recordByMongo.size() == 0) {
                        logger.info("存入用户{},日期:{},没有开始记录", userId, aLong);
                        continue;
                    }
                    //获取当天平均分
                    Double currentDayTotal = operationSumService.getCurrentDayTotalScore(userId, recordByMongo);
                    //当天的答题分数计算 存入redis
                    outLog("存入用户全国当天的:" + aLong + "的平均分数" + currentDayTotal);
                    operationSumService.saveSumCurrentDayTotalToRedis(userId, aLong.toString(), currentDayTotal);
                    //截止目前每天分数统计
                    List<Double> personScoreDoubles = operationSumService.getRecordByRedisAllDays(LangUtil.parseLong(userId));
                    outLog("获取全国每天的分数记录列表" + personScoreDoubles.toString());
                    //截止目前个人的当前总分(包含所有天数,取前5最高分)
                    Double personAllEveryDayTotal = operationSumService.calculatingCountryCurrentTotalScore(personScoreDoubles);
                    outLog("获取全国每天的分数记录列表前五最高的总分" + personAllEveryDayTotal);
                    //设置个人在公司的总分
                    //保存个人的总分到公司中,进行排名
                    outLog("保存用户的总分:" + personAllEveryDayTotal + ",至公司id:" + companyId);
                    if (operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userId, companyId, personAllEveryDayTotal)) {
                        outLog("保存用户的总分:" + personAllEveryDayTotal + ",至公司id" + companyId + "以及存储到各个上层的公司");
                        operationSumService.saveUserRankInCompanyBatch(userId, companyId, personAllEveryDayTotal);
                        outLog("保存用户的总分:" + personAllEveryDayTotal + ",至公司id" + companyId + "以及存储到各个上层的公司,并且计算耗时比...");
                        operationSumService.saveDurationUserRankInCompany(userId, companyId, personAllEveryDayTotal);
                    }
                    outLog("userId=" + "所在公司:" + companyId + "添加一次答题次数");
                    redisTemplate.boundHashOps("commitCountInCountry")
                            .put(companyId, recordByMongo.size()+"");
                }
            }
        }
        redisTemplate.boundHashOps("batch").delete("已处理用户数量");
        countryConvertTask.convert();
    }

    @Override
    public void updataCompany() {
        redisTemplate.delete("unitsParticipatingNumber");
        redisTemplate.delete("companyPersonCount");
        Map<String, Integer> companyPersonInnerCount = operationSumService.getCompanyPersonInnerCount();
        logger.info("全国单位数量:{}", companyPersonInnerCount.size());
        Set<String> companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).range(0, -1);
        logger.info("有效单位数量为:{}", companyIds.size());
        if (companyIds != null) {
            logger.info("统计参赛单位数为:" + companyIds.size());
            for (String companyId : companyIds) {
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                        .score(companyId);
                if (score != null) {
                    List<Long> parentIds = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                    if (parentIds != null && parentIds.size() > 0) {
                        logger.info("上级单位列表:" + parentIds.toString());
                        for (Long parentId : parentIds) {
                            //设置单位参赛数
                            Long unitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber").increment(parentId.toString(), 1);
                            logger.info("companyId=" + parentId + " 下的参赛单位数为:" + unitsParticipatingNumber);
                            //设置公司参赛人数
                            Long companyPersonCount = redisTemplate.boundHashOps("companyPersonCount").increment(parentId.toString(), score.longValue());
                            logger.info("companyId=" + parentId + " 下的公司参赛人数为:" + companyPersonCount);
                        }
                    }
                }
            }
        }
        logger.info("完成统计参赛单位数");
//-----------------------统计参赛率-------------------------------
        //统计参赛率(只统计省级别的参赛率,如果是湖北那就是市安监局级别,全国就是省这个级别)
        logger.info("统计参赛率");
        logger.info("开始统计公司参赛率");
        for (String companyId : companyIds) {
            Integer allPersonCount = companyPersonInnerCount.get(companyId);
            if (allPersonCount != null && allPersonCount != 0) {
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                        .score(companyId);
                if (score != null && score.intValue() != 0) {
                    double percentageDouble = score / allPersonCount;
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                            .add(companyId, LangUtil.parseDoubleLatterFive(percentageDouble, 0.00));
                    logger.info("计算公司id为{}的参赛率{}", companyId, percentageDouble);
                }
            }
        }
        logger.info("完成统计公司参赛率");
    }

    @Override
    public void batchHb() {
        String platformCode = operationSumService.getPlatformCode();
        logger.info("开始更新redis的数据");
        Map<String, Long> userIdCompanyIdMaps = operationSumService.queryUserIdAndCompanyIdsAll();
        logger.info("查询用户信息");
        redisTemplate.delete("totalCount");
        redisTemplate.delete("commitCountInCountry");
        redisTemplate.delete(RedisStatusKeyFactory.newCompanyCountryNumber());
        //查询所有mongo的数据
        //查询所有的用户,查询所有的用户的分组信息
        DBObject dbObject = new BasicDBObject();
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("userId", true);
        fieldsObject.put("createDay", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        outLog("userId=" + "全国答题次数:," + "添加一次答题次数");
        redisTemplate.boundHashOps("commitCountInCountry")
                .put("country", userExamRecords.size()+"");
        logger.info("查询mongo中用的用户数量为:" + userExamRecords.size());
        Set<String> userIds = new HashSet<>();
        if (userExamRecords != null && userExamRecords.size() > 0) {
            for (UserExamRecord userExamRecord : userExamRecords) {
                userIds.add(userExamRecord.getUserId());
            }
        }
        String startDay = timeManager.getStartDay();
        int size = userIds.size();
        logger.info("项目开始时间:" + startDay);
        redisTemplate.boundHashOps("batch").put("需要处理用户数量", size + "");
        for (String userId : userIds) {
            logger.info("开始处理用户userId{}", userId);
            redisTemplate.boundHashOps("batch").increment("已处理用户数量", 1);
            //所有开始次数
            List<UserExamRecord> records = operationSumService.getRecordByMongoAllRecordAndDuration(LangUtil.parseLong(userId));
            logger.info("查询处理用户userId{},的所有记录", records.toString());
            //判断用户是否有效
            Double personExamAvailable = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                    .score(userId.toString());
            String companyId = userIdCompanyIdMaps.get(userId).toString();
            if (personExamAvailable != null) {
                logger.info("userId=" + userId + "有效:");
            } else {
                if (records.size() > 0) {
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                            .add(userId, 1);
                    //全国单位总有效的参赛人数
                    redisTemplate.boundHashOps("totalCount").increment("ExamPersonCount", 1);
                    //单位有效参赛人数
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(companyId.toString(), 1);
                    List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                    if (longs != null && longs.size() > 0) {
                        for (Long parentId : longs) {
                            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(parentId.toString(), 1);
                        }
                    }
                    logger.info("userId=" + userId + "有效:" + personExamAvailable);
                    break;
                }
            }
            List<Long> startEndDay = TimeUtil.getStart_endDay(startDay, TimeUtil.getDate().toString());
            Long userIdLong = LangUtil.parseLong(userId);
            if (platformCode.equals(PlatformCode.HB)) {
                for (Long dateLong : startEndDay) {
                    //获取当天最高分的耗时
                    Long hbMaxScoreByMongo = operationSumService.getHbMaxScoreByMongo(dateLong, userIdLong);
                    outLog("获取湖北用户当天最高分的耗时:{}" + hbMaxScoreByMongo);
                    //添加用户当天耗时
                    redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationDayRecord(dateLong))
                            .put(userId, hbMaxScoreByMongo.toString());
                    //计算用户平均耗时之和
                    Long hbDurationEveryDayRecordTotal = operationSumService.getHbDurationEveryDayRecordTotal(userIdLong);
                    outLog("获取湖北用户平均分的耗时:{}" + hbDurationEveryDayRecordTotal);
                    //计算用户的总耗时
                    redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                            .put(userId, hbDurationEveryDayRecordTotal.toString());
                }
            } else if (platformCode.equals(PlatformCode.YNJT)) {
                //获取用户的平均耗时
                Long yNAverageSeekTime = operationSumService.getYNAverageSeekTime(userIdLong);
                outLog("获取云南用户总的耗时:{}" + yNAverageSeekTime);
                //计算用户的总耗时
                redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                        .put(userId, yNAverageSeekTime.toString());
                outLog("获取云南用户总的耗时:{}" + yNAverageSeekTime);
            }
            for (Long dateLong : startEndDay) {
                List<Integer> range = new ArrayList<>();
                if (platformCode.equals(PlatformCode.HB)) {
                    range = operationSumService.getRecordByMongo(dateLong, userIdLong);
                    outLog("获取湖北当天的分数记录" + range.toString());
                } else if (platformCode.equals(PlatformCode.YNJT)) {
                    range = operationSumService.getRecordByMongo(dateLong, userIdLong);
                    outLog("获取云南当天的分数记录" + range.toString());
                }
                if(range.size()==0){
                    continue;
                }
                Double currentDayTotal = 0.00;
                if (platformCode.equals(PlatformCode.HB)) {
                    currentDayTotal = operationSumService.getHbCurrentDayTotalScore(userId, range);
                    outLog("获取湖北的当天最高分:" + currentDayTotal);
                } else if (platformCode.equals(PlatformCode.YNJT)) {
                    currentDayTotal = operationSumService.getYnCurrentDayTotalScore(userId, range);
                    outLog("获取云南的当天平均分:" + currentDayTotal);
                }
                if (currentDayTotal == null) {
                    outLog("不能计算用户平均分...");
                } else {
                    operationSumService.saveSumCurrentDayTotalToRedis(userId, dateLong.toString(), currentDayTotal);
                    outLog("添加用户当天的平均分添加到个人当天总分" + currentDayTotal);
                    //截止目前每天分数统计
                    List<Double> personScoreDoubles = operationSumService.getRecordByRedisAllDays(userIdLong);
                    outLog("截止目前每天分数的分数列表" + personScoreDoubles.toString());
                    //截止目前个人的当前总分(包含所有天数,取前5最高分)
                    Double personAllEveryDayTotal = operationSumService.calculatingProvinceCurrentTotalScore(personScoreDoubles);
                    outLog("结果截止目前个人的总分" + personAllEveryDayTotal);
                    //设置个人在公司的总分
                    //保存个人的总分到公司中,进行排名
                    outLog("保存用户的总分:" + personAllEveryDayTotal + ",至公司id:" + companyId);
                    if (operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userId, companyId, personAllEveryDayTotal)) {
                        operationSumService.saveHbUserRankInCompanyBatch(userId, companyId, personAllEveryDayTotal);
                        outLog("保存用户的总分:" + personAllEveryDayTotal + ",至公司id:" + companyId + "进行耗时比计算");
                        operationSumService.saveDurationHBUserRankInCompany(userId, companyId, personAllEveryDayTotal);
                    }
                }
                outLog("userId=" + "所在公司:" + companyId + "添加一次答题次数");
                redisTemplate.boundHashOps("commitCountInCountry")
                        .increment(companyId, range.size());
            }
        }
        hBConvertTask.convert();
    }

    @Override
    public void durationUpdata() {
        DBObject dbObject = new BasicDBObject();
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("userId", true);
        fieldsObject.put("createDay", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        outLog("userId=" + "全国答题次数:," + "添加一次答题次数");
        redisTemplate.boundHashOps("commitCountInCountry")
                .put("country", userExamRecords.size());
        logger.info("查询mongo中用的用户数量为:" + userExamRecords.size());
        Set<String> userIds = new HashSet<>();
        if (userExamRecords != null && userExamRecords.size() > 0) {
            for (UserExamRecord userExamRecord : userExamRecords) {
                userIds.add(userExamRecord.getUserId());
            }
        }
        for (String userId : userIds) {
            List<UserExamRecord> records = operationSumService.getRecordByMongoAllRecordAndDuration(LangUtil.parseLong(userId));
            Long totalDuration = 0L;
            for (UserExamRecord record : records) {
                if(record.getDuration()!=null){
                    totalDuration += record.getDuration();
                }
            }
            Long duration = totalDuration / records.size();
            //添加总耗时
            logger.info("存入用户{}全国用户的总耗时:{}", userId, totalDuration);
            redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord()).put(userId, totalDuration.toString());
            //添加平均耗时
            logger.info("存入用户{}全国用户的平均耗时:{}", userId, duration);
            redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord()).put(userId, duration.toString());
        }

    }
    @Override
    public void scoreUpdate() {
        DBObject dbObject = new BasicDBObject();
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("userId", true);
        fieldsObject.put("createDay", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        outLog("userId=" + "全国答题次数:," + "添加一次答题次数");
        redisTemplate.boundHashOps("commitCountInCountry")
                .put("country", userExamRecords.size());
        logger.info("查询mongo中用的用户数量为:" + userExamRecords.size());
        Set<String> userIds = new HashSet<>();
        if (userExamRecords != null && userExamRecords.size() > 0) {
            for (UserExamRecord userExamRecord : userExamRecords) {
                userIds.add(userExamRecord.getUserId());
            }
        }
        for (String userId : userIds) {
            List<UserExamRecord> records = operationSumService.getRecordByMongoAllRecordAndDuration(LangUtil.parseLong(userId));
            Long totalDuration = 0L;
            for (UserExamRecord record : records) {
                if(record.getDuration()!=null){
                    totalDuration += record.getDuration();
                }
            }
            Long duration = totalDuration / records.size();
            //添加总耗时
            logger.info("存入用户{}全国用户的总耗时:{}", userId, totalDuration);
            redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord()).put(userId, totalDuration.toString());
            //添加平均耗时
            logger.info("存入用户{}全国用户的平均耗时:{}", userId, duration);
            redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord()).put(userId, duration.toString());
        }

    }
    public List<UserExamRecord> getAllUserRecordInMongo(String collectionName, DBObject query, String source) {
        return mongoTemplate.getCollection(collectionName).distinct(source, query);
    }

    private void outLog(String content) {
        logger.info("Mongo 数据迁移到redis:" + content);
    }
}
