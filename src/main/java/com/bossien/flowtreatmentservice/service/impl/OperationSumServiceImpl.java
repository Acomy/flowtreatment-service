package com.bossien.flowtreatmentservice.service.impl;

import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.dao.UserRoleRepository;
import com.bossien.flowtreatmentservice.dao.mongo.UserExamRecordRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.entity.mongo.RankingRecord;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.utils.*;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OperationSumServiceImpl implements OperationSumService {

    Logger logger = LoggerFactory.getLogger(OperationSumService.class);

    @Autowired
    UserExamRecordRepository userExamRecordRepository;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    TimeManager timeManager;

    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    IgnoreUnitService ignoreUnitService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserRoleRepository userRoleRepository;
    @Autowired
    CompanyRepository companyRepository;

    @Override
    public List<Integer> getRecordByMongo(Long dateLong, Long userIdLong) {
        String userId = userIdLong.toString();
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        dbObject.put("createDay", dateLong);  //查询条件
        BasicDBObject fieldsObject = new BasicDBObject();
        //指定返回的字段
        fieldsObject.put("score", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords != null) {
            return stuScoreStatisticsToScores(userExamRecords);
        } else {
            logger.info("no search record from mongo data={},userId={}" + dateLong, userIdLong);
            return new ArrayList<Integer>();
        }
    }

    @Override
    public List<Integer> getAverageRecordByMongo(Long userIdLong) {
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        BasicDBObject fieldsObject = new BasicDBObject();
        //指定返回的字段
        fieldsObject.put("score", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords != null) {
            return stuScoreStatisticsToScores(userExamRecords);
        } else {
            logger.info("no search record from mongo userId={}", userIdLong);
            return new ArrayList<Integer>();
        }
    }

    @Override
    public Long getHbMaxScoreByMongo(Long dateLong, Long userIdLong) {
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        dbObject.put("createDay", dateLong);  //查询条件
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("duration", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        Sort.Order score = new Sort.Order(Sort.Direction.DESC, "score");
        Sort.Order duration = new Sort.Order(Sort.Direction.ASC, "duration");
        Sort orders = new Sort(Arrays.asList(score, duration));
        query.with(orders);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords.size() > 0) {
            return userExamRecords.get(0).getDuration();
        } else {
            return 0L;
        }
    }

    @Override
    public Long getYNAverageSeekTime(Long userIdLong) {
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("duration", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords.size() > 0) {
            Long duration = 0L;
            for (UserExamRecord userExamRecord : userExamRecords) {
                duration += userExamRecord.getDuration();
            }
            long averageSeekTime = 0L;
            if (duration.longValue() == 0) {
                averageSeekTime = 0L;
            } else {
                averageSeekTime = duration / userExamRecords.size();
            }
            return averageSeekTime;
        } else {
            return 0L;
        }
    }

    @Override
    public List<Integer> getRecordByMongoFast(Long dateLong, Long userIdLong) {
        DBObject query1 = new BasicDBObject(); //setup the query criteria 设置查询条件
        query1.put("userId", new BasicDBObject("$in", userIdLong.toString()));
        query1.put("createDay", (new BasicDBObject("$in", dateLong)));
//        query1.put("userId", new BasicDBObject("$in", idList));
//        query1.put("createDay", (new BasicDBObject("$gte", startTime)).append("$lte", endTime));
        DBCursor dbCursor = mongoTemplate.getCollection("user_exam_record").find(query1);
        List<Integer> list = new ArrayList<>();
        while (dbCursor.hasNext()) {
            DBObject object = dbCursor.next();
            Object score = object.get("score");
            Integer intScore = (Integer) score;
            list.add(intScore);
        }
        return list;
    }

    @Override
    public List<Integer> getRecordByMongoAllRecord(Long userIdLong) {
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        BasicDBObject fieldsObject = new BasicDBObject();
        //指定返回的字段
        fieldsObject.put("score", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        if (userExamRecords != null) {
            return stuScoreStatisticsToScores(userExamRecords);
        } else {
            logger.info("no search record from mongo userId={}", userIdLong);
            return new ArrayList<Integer>();
        }
    }
    @Override
    public List<UserExamRecord> getRecordByMongoAllRecordAndDuration(Long userIdLong) {
        DBObject dbObject = new BasicDBObject();
        dbObject.put("userId", userIdLong.toString());
        BasicDBObject fieldsObject = new BasicDBObject();
        fieldsObject.put("score", true);
        fieldsObject.put("duration", true);
        Query query = new BasicQuery(dbObject, fieldsObject);
        List<UserExamRecord> userExamRecords = mongoTemplate.find(query, UserExamRecord.class);
        return userExamRecords ;

    }
    @Override
    public List<String> getRecordByRedis(int calculationType, final String userId, final String date, final String score) {
        if (calculationType == 0) {
            //用户每天的排名
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    redisConnection.lPush(RedisStatusKeyFactory.newFractionalRecord(userId, date).getBytes(),
                            score.getBytes());
                    return null;
                }
            });
            //修改分数
        } else {
            //用户每天的排名
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    //    redisConnection.lIndex();
//                    redisConnection.lPush(RedisStatusKeyFactory.newFractionalRecord(userId, date).getBytes(),
//                            score.getBytes());
                    return null;
                }
            });
        }
        return redisTemplate.boundListOps(RedisStatusKeyFactory.newFractionalRecord(userId, date))
                .range(0, -1);
    }

    @Override
    public List<Double> getRecordByRedisAllDays(Long userIdLong) {
        String startDay = timeManager.getStartDay();
        List<Long> startEndDay = TimeUtil.getStart_endDay(startDay, TimeUtil.getDate().toString());
        List<Double> toDayAllScores = new ArrayList<>();
        for (Long aLong : startEndDay) {
            Double score1 = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder(aLong.toString()))
                    .score(userIdLong.toString());
            if (score1 != null) {
                toDayAllScores.add(score1);
            }
        }
        return toDayAllScores;
    }

    @Override
    public Long getHbDurationEveryDayRecordTotal(Long userIdLong) {
        String startDay = timeManager.getStartDay();
        List<Long> startEndDay = TimeUtil.getStart_endDay(startDay, TimeUtil.getDate().toString());
        Long toDayAllScores = 0L;
        for (Long aLong : startEndDay) {
            Object score1 = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationDayRecord(aLong))
                    .get(userIdLong.toString());
            if (score1 != null) {
                Long aLong1 = LangUtil.parseLong(score1);
                if (aLong1 == null) {
                    aLong1 = 0L;
                }
                toDayAllScores += aLong1;
            }
        }
        return toDayAllScores;
    }

    @Override
    public UserExamRecord saveUserExamRecord(UserExamRecord userExamRecord) {
        UserExamRecord save = userExamRecordRepository.save(userExamRecord);
        return save;
    }

    @Override
    public UserExamRecord updateUserExamRecord(UserExamRecord userExamRecord) {
        String userId = userExamRecord.getUserId();
        Date createTime = userExamRecord.getCreateTime();
        Integer score = userExamRecord.getScore();
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId).and("createTime").is(createTime));
        Update update = Update.update("score", score);
        WriteResult writeResult = mongoTemplate.updateFirst(query, update, UserExamRecord.class);
        Object upsertedId = writeResult.getUpsertedId();
        if (upsertedId != null) {
            return userExamRecord;
        } else {
            return null;
        }
    }

    @Override
    public RankingRecord updateRankingRecord(RankingRecord rankingRecord) {
        logger.info("进行更新的接口数据{}",rankingRecord.toString());
        Query query = new Query();
        Double score = rankingRecord.getScore();
        if (rankingRecord.getType() == 0) {
            String userId = rankingRecord.getUserId();
            query.addCriteria(Criteria.where("userId").is(userId));
        } else {
            String companyId = rankingRecord.getCompanyId();
            query.addCriteria(Criteria.where("companyId").is(companyId));
        }
        Update update = Update.update("score", score);
        if (rankingRecord.getFirstRanking() != null) {
            update.set("firstRanking", rankingRecord.getFirstRanking());
        }
        if (rankingRecord.getSecondRanking() != null) {
            update.set("secondRanking", rankingRecord.getSecondRanking());
        }
        if (rankingRecord.getThirdRanking() != null) {
            update.set("thirdRanking", rankingRecord.getThirdRanking());
        }
        if (rankingRecord.getFourthRanking() != null) {
            update.set("fourthRanking", rankingRecord.getFourthRanking());
        }
        if (rankingRecord.getFifthRanking() != null) {
            update.set("fifthRanking", rankingRecord.getFifthRanking());
        }
        if (rankingRecord.getRanking() != null) {
            update.set("ranking", rankingRecord.getRanking());
        }
        if(rankingRecord.getCompanyName()!=null){
            update.set("companyName", rankingRecord.getCompanyName());
        }
        if(rankingRecord.getUserName()!=null){
            update.set("userName", rankingRecord.getUserName());
        }
        if(rankingRecord.getDuration()!=null){
            update.set("duration", rankingRecord.getDuration());
        }
        if(rankingRecord.getCreateTime()!=null){
            update.set("createTime", rankingRecord.getCreateTime());
        }
        WriteResult writeResult = mongoTemplate.upsert(query, update, RankingRecord.class);
        return rankingRecord ;
    }


    private List<Integer> stuScoreStatisticsToScores(List<UserExamRecord> stuScoreStatistics) {
        List<Integer> scores = new ArrayList<>();
        if (stuScoreStatistics != null && stuScoreStatistics.size() > 0) {
            for (UserExamRecord userExamRecord : stuScoreStatistics) {
                if (userExamRecord != null && userExamRecord.getScore() != null) {
                    scores.add(userExamRecord.getScore());
                }
            }
        }
        return scores;
    }

    @Override
    public void resetCompanyRankInParentCompany() {
        //设置公司总分在全国的排名
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder()).reverseRange(0, -1);
        for (String companyId : set) {
            Double companyTotalScore = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder()).score(companyId);
            List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
            //用户在本公司的排名
            List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
            if (longs == null) {
                outLog("companyId=" + companyId + " 查询不到上级列表");
            } else {
                longs.removeAll(countryIgnoreUnitIds);
                outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                if (longs != null && longs.size() > 2) {
                    Long mechanismId = longs.get(longs.size() - 3);
                    //添加公司在机构安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString()))
                            .add(companyId, companyTotalScore);
                }
                if (longs != null && longs.size() > 3) {
                    Long provinceId = longs.get(longs.size() - 4);
                    //添加公司在省安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                            .add(companyId, companyTotalScore);
                }
                if (longs != null && longs.size() > 4) {
                    Long cityId = longs.get(longs.size() - 5);
                    //添加公司在市安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                            .add(companyId, companyTotalScore);
                }
                if (longs != null && longs.size() > 5) {
                    Long cityId = longs.get(longs.size() - 6);
                    //添加公司在县市区安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                            .add(companyId, companyTotalScore);
                }
            }
        }
    }


    @Override
    public void resetHbCompanyRankInParentCompany() {
        //设置公司总分在全国的排名
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder()).reverseRange(0, -1);
        for (String companyId : set) {
            Double companyTotalScore = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder()).score(companyId);
            //用户在本公司的排名
            List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
            List<Long> ignoreUnitIds = getIgnoreUnitIds();
            if (longs == null) {
                outLog("companyId=" + companyId + " 查询不到上级列表");
            } else {
                longs.removeAll(ignoreUnitIds);
                outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                if (longs != null && longs.size() > 2) {
                    Long provinceId = longs.get(longs.size() - 3);
                    //添加公司在省安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                            .add(companyId, companyTotalScore);
                }
                if (longs != null && longs.size() > 3) {
                    Long cityId = longs.get(longs.size() - 4);
                    //添加公司在市安监局排名
                    outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                            .add(companyId, companyTotalScore);
                }
            }
        }
    }


    @Override
    public void saveCompanyAndUserRank(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        //设置公司总分在全国的排名
        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .add(companyId.toString(), companyTotalScore);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加用户在机构安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                        .add(userId, personAllEveryDayTotal);
                //添加公司在机构安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
                //添加公司在省安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
                //添加公司在市安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加用户在县市区安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
                //添加公司在县市区安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
        }
    }

    public List<Long> getIgnoreUnitIds() {
        List<Long> ignoreUnitIds = ignoreUnitService.getIgnoreUnitIds();
        return ignoreUnitIds;
    }

    public List<Long> getCountryIgnoreUnitIds() {
        List<Long> ignoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        return ignoreUnitIds;
    }

    @Override
    public void saveCompanyRankInParentCompany(String companyId, Double companyTotalScore) {
        //设置公司总分在全国的排名
        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .add(companyId.toString(), companyTotalScore);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加公司在机构安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加公司在省安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加公司在市安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加公司在县市区安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
        }
    }

    @Override
    public void saveCompanyAndUserRankBatch(final String userId, final String companyId, final Double personAllEveryDayTotal, final Double companyTotalScore) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //公司用户排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCountryOrder())
                        .add(userId, personAllEveryDayTotal);
                //设置公司总分在全国的排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCountryOrder())
                        .add(companyId.toString(), companyTotalScore);
                List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                //    RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyOrder(companyId.toString())).add(userId, personAllEveryDayTotal);
                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(countryIgnoreUnitIds);
                    outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                    if (longs != null && longs.size() > 2) {
                        Long mechanismId = longs.get(longs.size() - 3);
                        //添加用户在机构安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                                .add(userId, personAllEveryDayTotal);
                        //添加公司在机构安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString())).add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long provinceId = longs.get(longs.size() - 4);
                        //添加用户在省安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                                .add(userId, personAllEveryDayTotal);
                        //添加公司在省安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 4) {
                        Long cityId = longs.get(longs.size() - 5);
                        //添加用户在市安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                                .add(userId.toString(), personAllEveryDayTotal);
                        //添加公司在市安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 5) {
                        Long cityId = longs.get(longs.size() - 6);
                        //添加用户在县市区安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString())).add(userId.toString(), personAllEveryDayTotal);
                        //添加公司在县市区安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                }
                return null;
            }
        });

    }


    @Override
    public void saveCompanyAndUserRankBatchAndRedisConn(RedisConnection redisConnection, final String userId, final String companyId, final Double personAllEveryDayTotal, final Double companyTotalScore) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        //设置公司总分在全国的排名
        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCountryOrder())
                .add(companyId.toString(), companyTotalScore);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
        // RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyOrder(companyId.toString())).add(userId, personAllEveryDayTotal);
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加用户在机构安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                        .add(userId, personAllEveryDayTotal);
                //添加公司在机构安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString())).add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
                //添加公司在省安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
                //添加公司在市安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加用户在县市区安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString())).add(userId.toString(), personAllEveryDayTotal);
                //添加公司在县市区安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
        }
    }

    @Override
    public void saveCompanyRankInParentCompanyBatch(final String companyId, final Double companyTotalScore) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //设置公司总分在全国的排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCountryOrder())
                        .add(companyId.toString(), companyTotalScore);
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(countryIgnoreUnitIds);
                    outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                    if (longs != null && longs.size() > 2) {
                        Long mechanismId = longs.get(longs.size() - 3);
                        //添加公司在机构安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在机构安监局排名:id=" + mechanismId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyMechanismOrder(mechanismId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long provinceId = longs.get(longs.size() - 4);
                        //添加公司在省安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 4) {
                        Long cityId = longs.get(longs.size() - 5);
                        //添加公司在市安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 5) {
                        Long cityId = longs.get(longs.size() - 6);
                        //添加公司在县市区安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在县市区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityAreaOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                }
                return null;
            }
        });

    }


    @Override
    public void saveUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到全国排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加用户在机构安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加用户在县市区安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
        }
    }

    @Override
    public void saveUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal, boolean saveToCountry) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到全国排名:");
        if (saveToCountry) {
            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .add(userId, personAllEveryDayTotal);
        }
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加用户在机构安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加用户在县市区安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
        }
    }

    @Override
    public void saveDurationUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal) {
        logger.info("开始处理用户{},分数{}的耗时比", userId, personAllEveryDayTotal);
        Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                .get(userId);

        logger.info("开始处理用户{},耗时为{}:", userId, duration);
        if (personAllEveryDayTotal == null || duration == null) {
            logger.info("没有考试分数或者耗时,不进行处理...");
            return;
        }
        Long durationLong = LangUtil.parseLong(duration);
        //耗时比
        Double aDouble = CollectionUtils.convertScore(personAllEveryDayTotal, durationLong);
        personAllEveryDayTotal = aDouble;
        //公司用户排名
        outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加到全国排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在本机构排名:id=" + companyId);
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationCompanyScore(companyId))
                .add(userId.toString(), personAllEveryDayTotal);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(countryIgnoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long mechanismId = longs.get(longs.size() - 3);
                //添加用户在机构安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonMechanismOrder(mechanismId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long provinceId = longs.get(longs.size() - 4);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 4) {
                Long cityId = longs.get(longs.size() - 5);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 5) {
                Long cityId = longs.get(longs.size() - 6);
                //添加用户在县市区安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCityAreaOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
        }
    }


    @Override
    public void saveDurationHBUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal) {
        logger.info("开始处理用户{},分数{}的耗时比", userId, personAllEveryDayTotal);
        Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                .get(userId);

        logger.info("开始处理用户{},耗时为{}:", userId, duration);
        if (personAllEveryDayTotal == null || duration == null) {
            logger.info("没有考试分数或者耗时,不进行处理...");
            return;
        }
        Long durationLong = LangUtil.parseLong(duration);
        //耗时比
        Double aDouble = CollectionUtils.convertScore(personAllEveryDayTotal, durationLong);
        personAllEveryDayTotal = aDouble;
        //公司用户排名
        outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加到全国排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在本机构排名:id=" + companyId);
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationCompanyScore(companyId))
                .add(userId.toString(), personAllEveryDayTotal);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
        List<Long> ignoreUnitIds = getIgnoreUnitIds();
        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(ignoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long provinceId = longs.get(longs.size() - 3);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long cityId = longs.get(longs.size() - 4);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分耗时比=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }
        }
    }

    @Override
    public void saveUserRankInCompanyBatch(final String userId, final String companyId, final Double personAllEveryDayTotal) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //公司用户排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCountryOrder())
                        .add(userId, personAllEveryDayTotal);
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
//                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                        .add(userId, personAllEveryDayTotal);
                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(countryIgnoreUnitIds);
                    outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                    if (longs != null && longs.size() > 2) {
                        Long mechanismId = longs.get(longs.size() - 3);
                        //添加用户在机构安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在机构安监局排名:id=" + mechanismId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonMechanismOrder(mechanismId.toString()))
                                .add(userId, personAllEveryDayTotal);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long provinceId = longs.get(longs.size() - 4);
                        //添加用户在省安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                                .add(userId, personAllEveryDayTotal);
                    }
                    if (longs != null && longs.size() > 4) {
                        Long cityId = longs.get(longs.size() - 5);
                        //添加用户在市安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                                .add(userId.toString(), personAllEveryDayTotal);
                    }
                    if (longs != null && longs.size() > 5) {
                        Long cityId = longs.get(longs.size() - 6);
                        //添加用户在县市区安监局排名

                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在县市区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityAreaOrder(cityId.toString()))
                                .add(userId.toString(), personAllEveryDayTotal);
                    }
                }
                return null;
            }
        });

    }

    @Override
    public void saveHbCompanyAndUserRank(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        //设置公司总分在全国的排名
        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .add(companyId.toString(), companyTotalScore);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> ignoreUnitIds = getIgnoreUnitIds();

        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(ignoreUnitIds);
            if (longs != null && longs.size() > 2) {
                Long provinceId = longs.get(longs.size() - 3);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
                //添加公司在省安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 3) {
                Long cityId = longs.get(longs.size() - 4);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
                //添加公司在市安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在区安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
        }
    }

    @Override
    public void saveHbCompanyRankInParentCompany(String companyId, Double companyTotalScore) {
        //设置公司总分在全国的排名
        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .add(companyId.toString(), companyTotalScore);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
        List<Long> ignoreUnitIds = getIgnoreUnitIds();

        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(ignoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long provinceId = longs.get(longs.size() - 3);
                //添加公司在省安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                        .add(companyId, companyTotalScore);
            }
            if (longs != null && longs.size() > 3) {
                Long cityId = longs.get(longs.size() - 4);
                //添加公司在市安监局排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                        .add(companyId, companyTotalScore);
            }
        }
    }


    @Override
    public void saveHbUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .add(userId, personAllEveryDayTotal);
        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> ignoreUnitIds = getIgnoreUnitIds();

        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(ignoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long provinceId = longs.get(longs.size() - 3);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long cityId = longs.get(longs.size() - 4);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }

        }
    }


    @Override
    public void saveHbUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal, boolean saveToCountry) {
        //公司用户排名
        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
        if (saveToCountry) {
            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .add(userId, personAllEveryDayTotal);
        }

        //用户在本公司的排名
        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                .add(userId, personAllEveryDayTotal);
        List<Long> ignoreUnitIds = getIgnoreUnitIds();

        if (longs == null) {
            outLog("companyId=" + companyId + " 查询不到上级列表");
        } else {
            longs.removeAll(ignoreUnitIds);
            outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
            if (longs != null && longs.size() > 2) {
                Long provinceId = longs.get(longs.size() - 3);
                //添加用户在省安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                        .add(userId, personAllEveryDayTotal);
            }
            if (longs != null && longs.size() > 3) {
                Long cityId = longs.get(longs.size() - 4);
                //添加用户在市安监局排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                        .add(userId.toString(), personAllEveryDayTotal);
            }

        }
    }

    @Override
    public void saveHbUserRankInCompanyBatch(final String userId, final String companyId, final Double personAllEveryDayTotal) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //公司用户排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北云南安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCountryOrder())
                        .add(userId, personAllEveryDayTotal);
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                        .add(userId, personAllEveryDayTotal);
                List<Long> ignoreUnitIds = getIgnoreUnitIds();

                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(ignoreUnitIds);
                    outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                    if (longs != null && longs.size() > 2) {
                        Long provinceId = longs.get(longs.size() - 3);
                        //添加用户在省安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                                .add(userId, personAllEveryDayTotal);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long cityId = longs.get(longs.size() - 4);
                        //添加用户在市安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                                .add(userId.toString(), personAllEveryDayTotal);
                    }

                }
                return null;
            }
        });

    }

    @Override
    public void saveHbCompanyRankInParentCompanyBatch(final String companyId, final Double companyTotalScore) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //设置公司总分在全国的排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCountryOrder())
                        .add(companyId.toString(), companyTotalScore);
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                List<Long> ignoreUnitIds = getIgnoreUnitIds();

                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(ignoreUnitIds);
                    outLog(" companyId=" + companyId + " 公司上级单位列表:" + longs.toString());
                    if (longs != null && longs.size() > 2) {
                        Long provinceId = longs.get(longs.size() - 3);
                        //添加公司在省安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在省安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long cityId = longs.get(longs.size() - 4);
                        //添加公司在市安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                }
                return null;
            }
        });

    }

    @Override
    public void saveHbCompanyAndUserRankBatch(final String userId, final String companyId, final Double personAllEveryDayTotal, final Double companyTotalScore) {
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                //公司用户排名
                outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCountryOrder())
                        .add(userId, personAllEveryDayTotal);
                //设置公司总分在全国的排名
                outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加到湖北安监局排名:");
                RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCountryOrder())
                        .add(companyId.toString(), companyTotalScore);
                //用户在本公司的排名
                List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
//                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyOrder(companyId.toString()))
//                        .add(userId, personAllEveryDayTotal);
                List<Long> ignoreUnitIds = getIgnoreUnitIds();

                if (longs == null) {
                    outLog("companyId=" + companyId + " 查询不到上级列表");
                } else {
                    longs.removeAll(ignoreUnitIds);
                    if (longs != null && longs.size() > 2) {
                        Long provinceId = longs.get(longs.size() - 3);
                        //添加用户在省安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在市安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()))
                                .add(userId, personAllEveryDayTotal);
                        //添加公司在省安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在市安监局排名:id=" + provinceId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                    if (longs != null && longs.size() > 3) {
                        Long cityId = longs.get(longs.size() - 4);
                        //添加用户在市安监局排名
                        outLog("userId=" + userId + " 个人总分=" + personAllEveryDayTotal + "添加用户在区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newPersonCityOrder(cityId.toString()))
                                .add(userId.toString(), personAllEveryDayTotal);
                        //添加公司在市安监局排名
                        outLog("companyId=" + companyId + " 公司总分=" + companyTotalScore + "添加公司在区安监局排名:id=" + cityId);
                        RedisConnectionUtils.boundZSetOps(redisConnection, RedisStatusKeyFactory.newCompanyCityOrder(cityId.toString()))
                                .add(companyId, companyTotalScore);
                    }
                }
                return null;
            }
        });

    }

    @Override
    public Double searchCompanyScoreByIdFromRedis(String companyId) {
        //查询公司当前所有的个人Id
        Set<String> lastCompanyTotalScoreStr = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId)).reverseRange(0, -1);
        Double companyTotalScore = 0.00;
        if (lastCompanyTotalScoreStr != null) {
            // outLog("公司下所有参加开始个人的Id" + lastCompanyTotalScoreStr.toString());
            for (String personUserId : lastCompanyTotalScoreStr) {
                Double score1 = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId))
                        .score(personUserId);
                if (score1 != null) {
                    companyTotalScore += score1.longValue();
                }
            }
        }
        outLog(" companyId=" + companyId + " 公司总分:" + companyTotalScore);
        return companyTotalScore;
    }

    @Override
    public Boolean saveUserCurrentDayTotalScoreToCompanyScore(String userId, String companyId, Double personAllEveryDayTotal) {
        Boolean add = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId))
                .add(userId.toString(), personAllEveryDayTotal);
        return true;
    }

    @Override
    public Double calculatingCountryCurrentTotalScore(List<Double> personScoreDoubles) {
        Double personAllEveryDayTotal = 0.00;
        if (personScoreDoubles.size() > 5) {
            CollectionUtils.orderDoubleList(personScoreDoubles);
            personScoreDoubles.subList(0, 5);
        }
        for (Double aDouble : personScoreDoubles) {
            personAllEveryDayTotal += aDouble;
        }
        return LangUtil.parseDoubleLatterTwo(personAllEveryDayTotal, 0.00);
    }

    @Override
    public Double calculatingProvinceCurrentTotalScore(List<Double> personScoreDoubles) {
        Double personAllEveryDayTotal = 0.00;
        for (Double aDouble : personScoreDoubles) {
            personAllEveryDayTotal += aDouble;
        }
        return LangUtil.parseDoubleLatterTwo(personAllEveryDayTotal, 0.00);
    }

    @Override
    public void updateUserExamAvailable(String userId, String companyId) {

        //判断用户是否有效
        Double personExamAvailable = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                .score(userId.toString());
        if (personExamAvailable != null) {
            outLog("userId=" + userId + "有效:");
            return;
        }
        //所有记录
        List<Integer> dayAllScores = getRecordByMongoAllRecord(LangUtil.parseLong(userId));
        outLog("userId=" + userId + " 是否都有效:" + personExamAvailable);
        for (Integer personScoreAllDayDouble : dayAllScores) {
            if (personScoreAllDayDouble.longValue() >= 60 && dayAllScores.size() >= 5 && personExamAvailable == null) {
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                        .add(userId, 1);
                //全国单位总有效的参赛人数
                redisTemplate.boundHashOps("totalCount").increment("ExamPersonCount", 1);
                //单位有效参赛人数
                redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(companyId.toString(), 1);
                break;
            }
        }
    }

    @Override
    public void updateHbUserExamAvailable(String userId, String companyId) {
        //判断用户是否有效
        Double personExamAvailable = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                .score(userId.toString());
        if (personExamAvailable != null) {
            outLog("userId=" + userId + "有效:");
            return;
        }
        //所有记录
        Long aLong = userExamRecordRepository.countDistinctByUserId(userId);
        if (aLong != null && aLong != 0 && personExamAvailable != null) {
            outLog("userId=" + userId + " 已经存在有效记录");
        } else {
            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                    .add(userId, 1);
            //全国单位总有效的参赛人数
            redisTemplate.boundHashOps("totalCount").increment("ExamPersonCount", 1);
            //单位有效参赛人数
            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(companyId.toString(), 1);
            outLog("userId=" + userId + " 确定有效");
            List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
            if (longs != null && longs.size() > 0) {
                for (Long parentId : longs) {
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).incrementScore(parentId.toString(), 1);
                }
            }

        }


    }

    @Override
    public void updateHbUserExamAvailable(Map<String, Long> userIdAndCompanyIdMaps) {
        //判断用户是否有效
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                .reverseRange(0, -1);
        if (set != null && set.size() > 0) {
            for (String userId : set) {
                Long companyId = userIdAndCompanyIdMaps.get(userId);
                if (companyId != null) {
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
                }
            }

        }

    }

    @Override
    public void updateUserAllExamAvailable(Map<String, Long> userIdAndCompanyIdMaps) {
        //判断用户是否有效
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                .reverseRange(0, -1);
        if (set != null && set.size() > 0) {
            for (String userId : set) {
                Long companyId = userIdAndCompanyIdMaps.get(userId);
                if (companyId != null) {
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
                }
            }

        }
    }

    @Override
    public Double getCurrentDayTotalScore(String userId, List<Integer> range) {
        if (range != null && range.size() > 0) {
            outLog("userId=" + userId + " 查询所有考试记录=" + range.toString());
        }
        outLog("userId=" + userId + " 查询当天所有考试记录结果=" + range.toString());
        //获取当天的平均分
        Double average = LangUtil.average(range);
        final Double currentDayTotal = LangUtil.parseDoubleLatterTwo(average, 0.00);
        outLog("userId=" + userId + " 当天最高分=" + currentDayTotal);
        return currentDayTotal;
    }

    @Override
    public Double getHbCurrentDayTotalScore(String userId, List<Integer> range) {
        if (range != null && range.size() > 0) {
            outLog("userId=" + userId + " 查询所有考试记录=" + range.toString());
        }
        outLog("userId=" + userId + " 查询当天所有考试记录结果=" + range.toString());
        //获取当天的平均分
        Double average = LangUtil.max(range);
        final Double currentDayTotal = LangUtil.parseDoubleLatterTwo(average, 0.00);
        outLog("userId=" + userId + " 当天最高分=" + currentDayTotal);
        return currentDayTotal;
    }

    @Override
    public Double getYnCurrentDayTotalScore(String userId, List<Integer> range) {
        if (range != null && range.size() > 0) {
            outLog("userId=" + userId + " 查询所有考试记录=" + range.toString());
        }
        outLog("userId=" + userId + " 查询当天所有考试记录结果=" + range.toString());
        //获取当天的平均分
        Double average = LangUtil.average(range);
        final Double currentDayTotal = LangUtil.parseDoubleLatterTwo(average, 0.00);
        outLog("userId=" + userId + " 当天平均分=" + currentDayTotal);
        return currentDayTotal;
    }

    @Override
    public boolean saveSumCurrentDayTotalToRedis(final String userId, final String date, final Double currentDayTotal) {
        try {
            redisTemplate.executePipelined(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    redisConnection.zAdd(RedisStatusKeyFactory.newPersonCountryOrder(date).getBytes()
                            , currentDayTotal, userId.getBytes());
                    return null;
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public boolean refreshCompany() {
        logger.info("refresh company infos ---->>>>> start");
        try {
            MemoryClient.instance.refresh();
            logger.info("refresh company infos ---->>>>> ok");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("refresh company infos ---->>>failed reason:{}", e.getMessage());
            return false;
        }

    }

    @Override
    public List<Long> queryStudentInUsers() {
        List<Long> pids = queryJgCompanyIds();
        //查询所有的用户id
        final List<Long> userAll = userRepository.findCompanyByhql(pids);

        List<Long> userInDbAll = new ArrayList<>();
        if (userAll != null && userAll.size() > 0) {
            List<Long> idsByUserIdInAndRoleIdIs = userRoleRepository.findIdsByHql(userAll, 418061569927151616L);
            userAll.clear();
            userAll.addAll(idsByUserIdInAndRoleIdIs);
            userInDbAll.addAll(idsByUserIdInAndRoleIdIs);
        }
        return userInDbAll;
    }

    private List<Long> queryJgCompanyIds() {
        //所有监管单位下面的用户不考试
        List<Long> pids = companyRepository.findPidsByHql();
        return pids;
    }


    @Override
    public Map<String, Long> queryUserIdAndCompanyIdsAll() {
        //查询所有的用户id和公司id
        List<Users> all = userRepository.findIdAndCompanyIdByHql();
        Map<String, Long> stringLongMap = usersToUserIdAndCompanyIdMap(all);
        return stringLongMap;
    }

    @Override
    public Map<String, Long> queryUserIdAndCompanyIds(List<Long> userIds) {
        //查询所有的用户id和公司id
        List<Users> all = userRepository.findIdsNotJgByhql(userIds);
        Map<String, Long> stringLongMap = usersToUserIdAndCompanyIdMap(all);
        return stringLongMap;
    }

    private Map<String, Long> usersToUserIdAndCompanyIdMap(List<Users> all) {
        Map<String, Long> userCompanyIdsMap = new HashMap<>();
        for (Users users : all) {
            if (users != null && users.getId() != null && users.getCompanyId() != null) {
                userCompanyIdsMap.put(users.getId().toString(), users.getCompanyId());
            }
        }
        return userCompanyIdsMap;
    }

    @Override
    public Map<String, Integer> getCompanyPersonInnerCount() {
        Map<String, Integer> userCompanyIdsMap = new HashMap<>();
        List<Company> companyCountGroupByHql = companyRepository.findCompanyCountGroupByHql();
        if (companyCountGroupByHql != null && companyCountGroupByHql.size() > 0) {
            for (Company company : companyCountGroupByHql) {
                Long id = company.getId();
                Integer peopleNumber = company.getPeopleNumber();
                if (id != null && id != 0 && peopleNumber != null) {
                    userCompanyIdsMap.put(id.toString(), peopleNumber);
                }
            }
        }
        return userCompanyIdsMap;
    }

    @Override
    public Company getCompanyPersonInnerCountById(Long companyId) {
        Company company = companyRepository.findCompanyCountGroupByHqlAndCompanyId(companyId);
        return company;
    }

    @Override
    public String getPlatformCode() {
        String code = timeManager.getCode();
        if (code != null && code.equals(PlatformCode.COUNTRY)) {
            return PlatformCode.COUNTRY;
        }
        if (code != null && code.equals(PlatformCode.HB)) {
            return PlatformCode.HB;
        }
        if (code != null && code.equals(PlatformCode.YNJT)) {
            return PlatformCode.YNJT;
        }
        return "";
    }

    @Override
    public Map<String, Users> getAllUsersMaps() {
        List<Long> longs = queryStudentInUsers();
        List<Users> userInfoByHql = userRepository.findUserInfoByHql(longs);
        Map<String, Users> usersMap = new HashMap<>();
        if (userInfoByHql != null && userInfoByHql.size() > 0) {
            for (Users users : userInfoByHql) {
                if (users.getId() != null) {
                    usersMap.put(users.getId().toString(), users);
                }
            }
        }
        return usersMap;
    }

    @Override
    public Map<String, String> getAllCompanyMaps() {
        List<Company> companiesByState = companyRepository.findCompaniesByHql();
        Map<String, String> companyMaps = new HashMap<>();
        if (companiesByState != null && companiesByState.size() > 0) {
            for (Company company : companiesByState) {
                if (company != null && company.getId() != null) {
                    companyMaps.put(company.getId().toString(), company.getCompanyName());
                }
            }
        }
        return companyMaps;
    }

    @Override
    public Map<String, Integer> getAllCompanyCount() {
        List<Users> countByHql = userRepository.findCountByHql();
        Map<String, Integer> companyMaps = new HashMap<>();
        if (countByHql != null && countByHql.size() > 0) {
            for (Users users : countByHql) {
                if (users != null && users.getId() != null && users.getCompanyId() != null) {
                    companyMaps.put(users.getCompanyId().toString(), users.getId().intValue());
                }
            }
        }
        return companyMaps;
    }

    @Override
    public int getPersonCount(Long companyId) {
        if (companyId == null) {
            return 0;
        }
        Integer peopleNumber = 0;
        if (companyId != null && companyId == 0) {
            List<Company> byPidIs1 = companyRepository.findByPidIs(0L);
            if (byPidIs1 != null && byPidIs1.size() > 0) {
                Company company = byPidIs1.get(0);
                if (company != null) {
                    peopleNumber = company.getPeopleNumber();
                    if (peopleNumber == null) {
                        peopleNumber = 0;
                    }
                }
            }
            return peopleNumber;
        } else {
            Company companyPersonInnerCountById = this.getCompanyPersonInnerCountById(companyId);
            if (companyPersonInnerCountById != null) {
                peopleNumber = companyPersonInnerCountById.getPeopleNumber();
                if (peopleNumber == null) {
                    peopleNumber = 0;
                }
            }
        }
        return peopleNumber;
    }

    private void outLog(String content) {
        logger.info("用户统计信息:" + content);
    }


}
