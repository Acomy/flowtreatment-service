package com.bossien.flowtreatmentservice.task;

import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.constant.IgnoreUnitEnum;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.dao.UserRoleRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.service.AbstractStrategyFactory;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.service.IProvinceRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.service.impl.IgnoreUnitService;
import com.bossien.flowtreatmentservice.utils.ConcurrencyExecutor;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class InitializationHbTask {
    Logger logger = LoggerFactory.getLogger(InitializationHbTask.class);
    @Autowired
    UserRepository userRepository;
    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    OperationSumService operationSumService;

    @Autowired
    AbstractStrategyFactory<String, List<Long>> strategyForFactory;
    @Autowired
    IgnoreUnitService ignoreUnitService;
    @Autowired
    UserRoleRepository userRoleRepository;
    public boolean convert() {
        operationSumService.refreshCompany();

        //所有监管单位下面的用户不考试
        List<Long> pids = companyRepository.findPidsByHql();

        //查询所有公司id
        final List<Long> companyAll = companyRepository.queryByHql(pids);
        //查询所有的用户id
        final List<Long> userAll = operationSumService.queryStudentInUsers();

        Map<String, Long> fullUserCompanyIdsMap = operationSumService.queryUserIdAndCompanyIds(userAll);

        Map<String, Long> userCompanyIdsMap = new HashMap<>(fullUserCompanyIdsMap);

        BoundZSetOperations boundPersonCountryOrder = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder());
        Set<String> userIds = boundPersonCountryOrder.range(0, -1);
        Set<Long> userIdLongs = LangUtil.setStringToLong(userIds);
        //只生成用户在全国的排名.不计入每天统计,已经参赛人员,不计入答题次数,已经总分
        logger.info("批量生产用户开始信息...");
        logger.info("获取redis中用户排名信息...");
        logger.info("查询所有用户信息...");
        //批量插入用户信息
        Set<Long> newUserAll = new HashSet<>(userAll);
        //排除已经存在的用户在userId列表中
        Sets.SetView<Long> intersection = Sets.intersection(userIdLongs, newUserAll);
        //排除已经存在的用户在userIdAndCompanyIds列表中
        if (intersection != null) {
            for (Long aLong : intersection) {
                userCompanyIdsMap.remove(aLong.toString());
            }
        }
        userAll.removeAll(intersection);
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                if (userAll != null) {
                    for (Long userId : userAll) {
                        if (userId != null) {
                            String usersId = userId.toString();
                            redisConnection.zAdd(RedisStatusKeyFactory.newPersonCountryOrder().getBytes(), 0, usersId.getBytes());
                            logger.info("用户Id:{},完成初始化,成绩为0", usersId);
                        }
                    }
                }
                return null;
            }
        });
        logger.info("生成用户开始信息完成...");
//------------------------------------------------------------
        logger.info("批量生产公司开始信息...");
        logger.info("获取redis中公司排名信息...");
        final Map<String, Double> companyMaps = new HashMap<>();
        final Set<String> companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .range(0, -1);
        Set<Long> companyIdLongs = LangUtil.setStringToLong(companyIds);
        logger.info("查询所有公司信息...");
        //批量插入用户信息
        Set<Long> newCompanyAll = new HashSet<>(companyAll);
        Sets.SetView<Long> intersection1 = Sets.intersection(companyIdLongs, newCompanyAll);
        companyAll.removeAll(intersection1);
        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                if (userAll != null) {
                    for (Long companyId : companyAll) {
                        if (companyId != null) {
                            String companyIdStr = companyId.toString();
                            redisConnection.zAdd(RedisStatusKeyFactory.newCompanyCountryNumber().getBytes(), 0, companyIdStr.getBytes());
                            redisConnection.zAdd(RedisStatusKeyFactory.newCompanyCountryOrder().getBytes(), 0, companyIdStr.getBytes());
                            logger.info("公司id:{},完成初始化,成绩为0", companyIdStr);
                        }
                    }
                }
                return null;
            }
        });

        //--------------------------------------------------------------------------------
        logger.info("生成公司信息完成...");
        logger.info("开始生成用户公司排名信息");
        ConcurrencyExecutor concurrencyExecutor = ConcurrencyExecutor.newConcurrencyExecutor();
        for (Map.Entry<String, Long> stringLongEntry : userCompanyIdsMap.entrySet()) {
            final String usersId = stringLongEntry.getKey();
            final Long companyId = stringLongEntry.getValue();
            concurrencyExecutor.run(new Runnable() {
                @Override
                public void run() {
                    operationSumService.saveHbCompanyAndUserRankBatch(usersId, companyId.toString(), 0.00, 0.00);
                }
            });
        }
        concurrencyExecutor.safeStop(3, TimeUnit.HOURS);
        logger.info("用户公司排名信息生成完成...");

        userAll.clear();
        userIds.clear();
        userCompanyIdsMap.clear();
        userIdLongs.clear();
        companyAll.clear();
        companyIdLongs.clear();
        companyIds.clear();
        companyMaps.clear();
       userAll.clear();
       userIds.clear();
       userCompanyIdsMap.clear();
       userIdLongs.clear();
       companyAll.clear();
       companyIdLongs.clear();
       companyIds.clear();
       companyMaps.clear();
       return true;
    }



    public List<Long> getProvinceIds() {
        List<Long> ignoreUnitIds = ignoreUnitService.getIgnoreUnitIds();
        //湖北省安监局
        List<Company> lowCountryIds = companyRepository.findByPidIs(0L);
        List<Company> cityCompanies = companyRepository.findByPidIs(lowCountryIds.get(0).getId());
        List<Long> cityCompanyIds1 = new ArrayList<>();
        for (Company company : cityCompanies) {
            //从省级排除湖北司法厅
            if (ignoreUnitIds.contains(company.getId())) {
                continue;
            }
            cityCompanyIds1.add(company.getId());
        }
        return cityCompanyIds1;
    }

    public List<Long> queryCompanyIdListByStarId(Long provinceId) {
        return strategyForFactory.calculation(String.valueOf(provinceId));
    }

    private Map<String, Long> usersToUserIdAndCompanyIdMap(List<Users> all) {
        Map<String,Long> userCompanyIdsMap = new HashMap<>();
        for (Users users : all) {
            if(users!=null && users.getId()!=null && users.getCompanyId()!=null){
                userCompanyIdsMap.put(users.getId().toString(),users.getCompanyId());
            }
        }
        return userCompanyIdsMap;
    }

}
