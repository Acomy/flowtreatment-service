package com.bossien.flowtreatmentservice.task;

import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.service.AbstractStrategyFactory;
import com.bossien.flowtreatmentservice.service.IProvinceRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.service.impl.IgnoreUnitService;
import com.bossien.flowtreatmentservice.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务执行类
 * 在湖北的水利局 地级市等于去湖北中的省
 *
 * @author gb
 */
@Component
public class HBConvertTask {
    //有效的状态
    public static final int EFFECTIVE = 1;
    @Autowired
    TimeManager timeManager;
    public String startDay;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    IProvinceRankService iProvinceRankService;
    @Autowired
    RedisUtil redisUtil;

    @Autowired
    AbstractStrategyFactory<String, List<Long>> strategyForFactory;
    /**
     * 地级市信息
     */
    private List<Long> provinceIds;
    /**
     * 县市区下面的公司关系
     */
    private Map<Long, List<Long>> cityIds;

    private String currentDateDay;

    private List<Long> startAndDays;

    private ConcurrencyExecutor concurrencyExecutor;

    private Logger logger = LoggerFactory.getLogger(HBConvertTask.class);

    private List<Long> ignoreUnits;

    @Autowired
    OperationSumService operationSumService;
    @Autowired
    IgnoreUnitService ignoreUnitService;

    public boolean convert() {
        Long date = TimeUtil.getDate();

        operationSumService.refreshCompany();
        //学员信息
        List<Long> userInfos = operationSumService.queryStudentInUsers();
        //监管单位
        List<Long> companyPids = companyRepository.findPidsByHql();
        //公司对应的人数
        List<Users> countByHql = userRepository.findCountByHql();

        List<Users> realCountByHql = new ArrayList<>();


        Set<String> userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .reverseRange(0, -1);
        //有学院的公司集合
        Map<String, Long> fullUserCompanyIdsMap = operationSumService.queryUserIdAndCompanyIds(userInfos);
        insertResult("开始清理湖北公司的排名");
        clearHbRedisCollection(companyPids, countByHql, realCountByHql, fullUserCompanyIdsMap);
        insertResult("完成清理湖北公司的排名");
        //----------------------开始计算公司的耗时----------------------------------------
        Set<String> keys = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord()).keys();
        if (keys != null) {
            for (String userId : keys) {
                Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord()).get(userId);
                if (duration != null) {
                    Long aLong = LangUtil.parseLong(duration);
                    if (aLong == null) {
                        aLong = 0L;
                    }
                    Long companyId = fullUserCompanyIdsMap.get(userId);
                    redisTemplate.boundHashOps(RedisStatusKeyFactory.newCompanyDurationRecord())
                            .increment(companyId.toString(), aLong);
                }
            }
        }
        //-------------------计算个人在总分在公司总分的变化排名--------------------------
        logger.info("开始更新全国公司的排名");
        insertResult("开始更新全国公司的排名");
        final ConcurrencyExecutor concurrencyExecutor = ConcurrencyExecutor.newConcurrencyExecutor(2, 10, 0L);
        for (String userId : userIds) {
            final String userIdStr = userId;
            redisTemplate.boundHashOps("result").increment("已处理学员数量", 1);
            final Double aDouble = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder()).score(userIdStr);
            Long companyId = fullUserCompanyIdsMap.get(userIdStr);
            final Double personAllEveryDayTotal = LangUtil.parseDoubleLatterTwo(aDouble, 0.00);
            if (personAllEveryDayTotal == null || personAllEveryDayTotal.doubleValue() == 0) {
                continue;
            }
            if (companyId != null) {
                final String companyIdStr = String.valueOf(companyId);
                logger.info("更新 userId={},companyId={}", userIdStr, companyIdStr);
                //查询公司截止目前的总分
                operationSumService.saveUserCurrentDayTotalScoreToCompanyScore(userIdStr, companyIdStr, personAllEveryDayTotal);
                if (personAllEveryDayTotal != null) {
                    //公司上级列表
                    redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                            .incrementScore(companyIdStr, personAllEveryDayTotal);
                    redisTemplate.boundHashOps("totalScores").increment(companyIdStr, personAllEveryDayTotal);
                    List<Long> longs = MemoryClient.instance.getCompanyIdsCacheReverse().get("index_" + companyId);
                    if (longs != null) {
                        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                                .incrementScore(companyIdStr, personAllEveryDayTotal);
                        logger.info("companyId=" + companyId + "上级列表" + longs.toString());
                        for (Long parentIds : longs) {
                            double totalScores = redisTemplate.boundHashOps("totalScores").increment(parentIds.toString(), personAllEveryDayTotal);
                            logger.info("parentId=" + parentIds + "分数为:" + totalScores);
                        }
                    }

                }
                concurrencyExecutor.run(new Runnable() {
                    @Override
                    public void run() {
                        operationSumService.saveHbUserRankInCompany(userIdStr, companyIdStr, personAllEveryDayTotal, false);
                    }
                });

            }
            System.out.println("扫描绑定数据:" + userIdStr + "--->" + aDouble);
        }
        concurrencyExecutor.safeStop(5, TimeUnit.HOURS);
        redisTemplate.boundHashOps("result").delete("需要处理学员总数", "已处理学员数量");
        operationSumService.resetHbCompanyRankInParentCompany();
        //更新个人是否有效问题
        operationSumService.updateHbUserExamAvailable(fullUserCompanyIdsMap);
        logger.info("更新公司的排名成功");
        insertResult("更新公司的排名成功");

//-------------------------------------------------
        //统计湖北参赛人数的总分
        logger.info("统计湖北参赛人数的总分:");
        insertResult("开始统计湖北参赛人数的总分");
        Double totalAllStudentScoreCount = 0.00;
        BoundZSetOperations boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder());
        Cursor<ZSetOperations.TypedTuple> cursor = boundZSetOperations.scan(ScanOptions.NONE);
        while (cursor.hasNext()) {
            ZSetOperations.TypedTuple typedTuple = cursor.next();
            Double score1 = typedTuple.getScore();
            if (score1 == null) {
                score1 = 0.00;
            }
            totalAllStudentScoreCount += LangUtil.parseDoubleLatterTwo(score1, 0.00);
        }
        //设置湖北参赛人数的总分
        logger.info("湖北参赛人数的总分:" + totalAllStudentScoreCount);
        redisTemplate.boundHashOps("totalCount").put("totalAllStudentScoreCount", String.valueOf(totalAllStudentScoreCount));
        insertResult("湖北参赛人数的总分");
//-------------------------------------------------
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
                            //有单位参赛自增1
                            int number = 1;
                            //没有人参赛为0
                            //设置单位参赛数
                            Long unitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber").increment(parentId.toString(), number);
                            logger.info("companyId=" + parentId + " 下的参赛单位数为:" + unitsParticipatingNumber);
                            //设置公司参赛人数
                            Long companyPersonCount = redisTemplate.boundHashOps("companyPersonCount").increment(parentId.toString(), score.longValue());
                            logger.info("companyId=" + parentId + " 下的公司参赛人数为:" + companyPersonCount);

                        }
                    }
                }
            }

        }
        insertResult("完成统计参赛单位");
        return true;
    }

    /**
     * 清楚湖北排名以及总分信息
     *
     * @param companyPids           监管单位ids
     * @param countByHql            单位下面学院的分组
     * @param realCountByHql        排除没有学院的单位
     * @param fullUserCompanyIdsMap 学院对应公司的id集合
     */
    private void clearHbRedisCollection(List<Long> companyPids, List<Users> countByHql, List<Users> realCountByHql, Map<String, Long> fullUserCompanyIdsMap) {
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

}
