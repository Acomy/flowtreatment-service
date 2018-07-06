package com.bossien.flowtreatmentservice.task;

import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.dao.UserRoleRepository;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.service.AbstractStrategyFactory;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.RedisUtil;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 定时任务执行类
 *
 * @author gb
 */
@Component
public class CountryConvertTask {

    @Autowired
    TimeManager timeManager;
    //默认十天以上的需要计算前十
    public static final int DEFAULTDAYCOUNT = 10;
    //用户答题数
    public static final int NUMBEROFANSWERS = 5;
    //有效分数60
    public static final int ADMISSIBLEMARK = 60;
    //redis的数据库
    public static final int DEFAULTDATEBASE = 6;
    //有效的状态
    public static final int EFFECTIVE = 1;
    //开始时间
    private String startDay;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    ICountryRankService iCountryRankService;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    AbstractStrategyFactory<String, List<Long>> strategyForFactory;

    private List<Long> provinceIds;

    private Map<Long, List<Long>> cityIds;

    private String currentDateDay;

    List<Long> startAndDays;

    Logger logger = LoggerFactory.getLogger(CountryConvertTask.class);

    @Autowired
    OperationSumService operationSumService;
    @Autowired
    UserRoleRepository userRoleRepository;

    public boolean convert() {
        operationSumService.refreshCompany();
        //学员信息
        List<Long> userInfos = operationSumService.queryStudentInUsers();
        //监管单位
        List<Long> companyPids = companyRepository.findPidsByHql();
        //公司对应的人数
        List<Users> countByHql = userRepository.findCountByHql();

        Map<String, Integer> companyPersonInnerCount = operationSumService.getCompanyPersonInnerCount();

        List<Users> realCountByHql = new ArrayList<>();
        Set<String> userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .reverseRange(0, -1);
        //有学院的公司集合
      //  Map<String, Long> fullUserCompanyIdsMap = operationSumService.queryUserIdAndCompanyIdsAll();
        Map<String, Long> fullUserCompanyIdsMap = operationSumService.queryUserIdAndCompanyIds(userInfos);
        Set<String> allKeys = redisTemplate.keys("*");
        insertResult("删除上一次计算结果");
        clearRedisCollections(companyPids, countByHql, realCountByHql, fullUserCompanyIdsMap);
        insertResult("删除上一次计算完成");
        //保留 全国个人分数 全国个人每天分数  个人总耗时
        //-------------------计算个人在总分在公司总分的变化排名--------------------------
        logger.info("开始更新全国公司的排名");
        insertResult("开始更新全国公司的排名");
        //更新公司的排名
        if (userIds != null) {
            redisTemplate.boundHashOps("result").put("需要处理学员总数", userIds.size() + "人");
            for (Long userId : userInfos) {
                final String userIdStr = userId.toString();
                final Double personAllEveryDayTotal = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder()).score(userIdStr);
                redisTemplate.boundHashOps("result").increment("已处理学员数量", 1);
                Long companyId = fullUserCompanyIdsMap.get(userIdStr);
                if (personAllEveryDayTotal == null) {
                    continue;
                }
                if (companyId != null) {
                    final String companyIdStr = String.valueOf(companyId);
                    logger.info("更新 userId={},companyId={}", userIdStr, companyIdStr);
                    final Double realPersonAllEveryDayTotal = LangUtil.parseDoubleLatterTwo(personAllEveryDayTotal, 0.00);
                    //查询公司截止目前的总分
                    operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userIdStr, companyIdStr, personAllEveryDayTotal);
                    if (personAllEveryDayTotal != null) {
                        //公司上级列表
                        redisTemplate.boundHashOps("totalScores").increment(companyId.toString(), realPersonAllEveryDayTotal);
                        List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                        if (longs != null) {
                            redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                                    .incrementScore(companyIdStr, personAllEveryDayTotal);
                            logger.info("companyId=" + companyId + "上级列表" + longs.toString());
                            for (Long parentIds : longs) {
                                Double totalScores = redisTemplate.boundHashOps("totalScores").increment(parentIds.toString(), realPersonAllEveryDayTotal);
                                logger.info("parentId=" + parentIds + "分数为:" + totalScores);
                            }
                        }
                    }
                    operationSumService.saveUserRankInCompany(userIdStr, companyIdStr, realPersonAllEveryDayTotal, false);
                }
            }
        }
        redisTemplate.boundHashOps("result").delete("需要处理学员总数", "已处理学员数量");
        insertResult("开始更新公司在各个层级的排名");
        operationSumService.resetCompanyRankInParentCompany();
        insertResult("完成更新公司在各个层级的排名");
        //更新个人是否有效问题
        insertResult("开始统计公司下面的有效人数");
        operationSumService.updateUserAllExamAvailable(fullUserCompanyIdsMap);
        insertResult("完成统计公司下面的有效人数");
        logger.info("完成全国公司的排名成功");

//--------------------计算全国总分-----------------------------
        insertResult("开始更新个人的耗时结果");
        Set<String> keys = redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord()).keys();
        if(keys!=null){
            for (String key : keys) {
                if(key!=null){
                    Object value = redisTemplate.boundHashOps(RedisStatusKeyFactory.newAverageDurationRecord())
                            .get(key);
                    if(value!=null){
                        Long totalDuration = LangUtil.parseLong(value);
                        List<Integer> recordByMongoAllRecord = operationSumService.getRecordByMongoAllRecord(LangUtil.parseLong(key));
                        if(recordByMongoAllRecord!=null && recordByMongoAllRecord.size()>0){
                            Long duration = totalDuration /recordByMongoAllRecord.size() ;
                            redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                                    .put(key,duration.toString());
                        }
                    }
                }else{
                    logger.info("没有查询到用户的耗时信息:");
                }
            }
        }
        insertResult("完成更新个人的耗时结果");
        //统计全国参赛人数的总分
        logger.info("统计全国参赛人数的总分:");
        insertResult("开始统计全国参赛人数的总分");
        Long totalAllStudentScoreCount = 0L;
        if (userIds != null) {
            logger.info("参赛人数为:" + userIds.size());
            for (String userId : userIds) {
                Long companyId = fullUserCompanyIdsMap.get(userId);
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                        .score(userId);
                if (score == null) {
                    score = 0.00;
                }
                totalAllStudentScoreCount += score.longValue();
                operationSumService.saveDurationUserRankInCompany(userId,companyId.toString(),score);
            }
        }
        //设置全国参赛人数的总分
        logger.info("全国参赛人数的总分:" + totalAllStudentScoreCount);
        redisTemplate.boundHashOps("totalCount").put("totalAllStudentScoreCount", String.valueOf(totalAllStudentScoreCount));
        insertResult("完成全国参赛人数的总分");

//------------------------计算参数单位数,已经公司参赛人数-------------------------
        logger.info("统计参赛单位数");
        insertResult("开始统计参赛单位数");
        Set<String> companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).range(0, -1);
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
        insertResult("完成统计参赛单位数");
//-----------------------统计参赛率-------------------------------
        //统计参赛率(只统计省级别的参赛率,如果是湖北那就是市安监局级别,全国就是省这个级别)
        logger.info("统计参赛率");
        insertResult("开始统计公司参赛率");
        Map<String, Integer> countOfPersonMap = companyPersonInnerCount;
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                .reverseRange(0, -1);
        for (String companyId : set) {
            Integer allPersonCount = countOfPersonMap.get(companyId);
            if (allPersonCount != null && allPersonCount != 0) {
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                        .score(companyId);
                if (score != null && score.intValue() != 0) {
                    double percentageDouble = score / allPersonCount;
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                            .add(companyId, LangUtil.parseDoubleLatterFive(percentageDouble, 0.00));
                }
            }
        }
        insertResult("完成统计公司参赛率");
        return true;
    }

    /**
     * 清楚排名已经总分信息
     *
     * @param companyPids           监管单位ids
     * @param countByHql            单位下面学院的分组
     * @param realCountByHql        排除没有学院的单位
     * @param fullUserCompanyIdsMap 学院对应公司的id集合
     */
    private void clearRedisCollections(List<Long> companyPids, List<Users> countByHql, List<Users> realCountByHql, Map<String, Long> fullUserCompanyIdsMap) {

        Set<String> keys = new HashSet<>();
        keys.add("totalScores");
        keys.add("totalCount");
        keys.add(RedisStatusKeyFactory.newCompanyCountryOrder());
        keys.add(RedisStatusKeyFactory.newCompanyCountryPercentage());
        keys.add(RedisStatusKeyFactory.newCompanyCountryNumber());
        keys.add("unitsParticipatingNumber");
        keys.add("companyPersonCount");
        keys.add("result");
        keys.add(RedisStatusKeyFactory.newDurationRecord());
        Set<String> companyScoreKeys = redisTemplate.keys("companyScore" + "*");
        keys.addAll(companyScoreKeys);

        Set<String> personMechanismOrderKeys = redisTemplate.keys("personMechanismOrder" + "*");
        keys.addAll(personMechanismOrderKeys);

        Set<String> personProvinceOrderKeys = redisTemplate.keys("personProvinceOrder" + "*");
        keys.addAll(personProvinceOrderKeys);

        Set<String> PersonCityOrderKeys = redisTemplate.keys("PersonCityOrder" + "*");
        keys.addAll(PersonCityOrderKeys);

        Set<String> personCityAreaOrderKeys = redisTemplate.keys("personCityAreaOrder" + "*");
        keys.addAll(personCityAreaOrderKeys);

        Set<String> CompanyMechanismOrderKeys = redisTemplate.keys("CompanyMechanismOrder" + "*");
        keys.addAll(CompanyMechanismOrderKeys);

        Set<String> companyProvinceOrderKeys = redisTemplate.keys("companyProvinceOrder" + "*");
        keys.addAll(companyProvinceOrderKeys);

        Set<String>companyCityOrderKeys = redisTemplate.keys("companyCityOrder" + "*");
        keys.addAll(companyCityOrderKeys);

        Set<String> companyCityAreaOrderKeys = redisTemplate.keys("companyCityAreaOrder" + "*");
        keys.addAll(companyCityAreaOrderKeys);
        for (String key : keys) {
            redisTemplate.delete(key);
        }
    }

    public List<Long> queryCompanyIdListByStarId(Long provinceId) {
        return strategyForFactory.calculation(String.valueOf(provinceId));
    }

    public void insertResult(String content) {
        Date date =new Date();
        redisTemplate.boundHashOps("result").put(content, date.toString());
    }

    public static void main(String[] args) {
        Date date = new Date();
        System.out.println(date.toString());
    }
}
