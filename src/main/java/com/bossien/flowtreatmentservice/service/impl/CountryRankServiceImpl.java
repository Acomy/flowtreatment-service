package com.bossien.flowtreatmentservice.service.impl;

import com.bossien.common.base.ResponseData;
import com.bossien.common.constant.UnitType;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.CacheDataSource;
import com.bossien.flowtreatmentservice.cache.CacheManager;
import com.bossien.flowtreatmentservice.cache.DataHelper;
import com.bossien.flowtreatmentservice.cache.MemoryClient;
import com.bossien.flowtreatmentservice.constant.IgnoreUnitEnum;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.dao.mongo.UserExamRecordRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.GroupTotalScore;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.service.AbstractStrategyFactory;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * @author gb
 */
@Service
public class CountryRankServiceImpl implements ICountryRankService {
    @Autowired
    TimeManager timeManager;

    Logger logger = LoggerFactory.getLogger(CountryRankServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    AbstractStrategyFactory<String, List<Long>> strategyForFactory;
    /**
     * 缓存公司信息
     */
    private CacheManager<Long, Company> companyCache;
    /**
     * 缓存公司id 信息减小缓存
     */
    private CacheManager<String, List<Long>> companyIdsCache;

    private CacheManager<String, List<Long>> companyIdsCacheReverse;

    private MemoryClient memoryClient;
    @Autowired
    UserExamRecordRepository userExamRecordRepository;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    IgnoreUnitService ignoreUnitService;
    @Autowired
    OperationSumService operationSumService;

    /**
     * 全省个人排行
     *
     * @param provinceId
     * @return
     */
    @Override
    public ResponseData getRangeRankings(final Long provinceId) {
        List<Map<String, Object>> resultMap = new ArrayList<>();
        BoundZSetOperations boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(provinceId.toString()));
        Set<String> set = boundZSetOperations.reverseRange(0, 99);
        if (set != null) {
            for (String uid : set) {
                if (uid != null && !uid.equals("")) {
                    Map<String, Object> stringObjectMap = new HashMap<>();
                    Users usersById = userRepository.findUsersById(LangUtil.parseLong(uid));
                    Double score = boundZSetOperations.score(usersById.getId().toString());
                    if (score != null) {
                        stringObjectMap.put("personScore", LangUtil.parseDoubleLatterTwo(score, 0.00).toString());
                    }
                    stringObjectMap.put("userName", usersById.getNickname());
                    resultMap.add(stringObjectMap);
                }
            }
        } else {
            ResponseData.fail("该省份暂无人员参赛!!!");
        }
        return ResponseData.ok(resultMap);
    }

    /**
     * 查询指定的省份下面的公司信息
     *
     * @param provinceId
     * @return
     */
    @Override
    public List<Long> queryCompanyIdListByStarId(Long provinceId) {
        return strategyForFactory.calculation(String.valueOf(provinceId));
    }

    /**
     * 查询指定公司下面的个人排名
     *
     * @param companyId
     * @return
     * @throws JsonProcessingException
     */
    @Override
    public String queryPersonOrderInCompany(String companyId) throws JsonProcessingException {
        Set<String> userIds = new HashSet<>();
        Map<String, Users> allUsersMaps = operationSumService.getAllUsersMaps();
        Map<String, String> allCompanyMaps = operationSumService.getAllCompanyMaps();
        if (allUsersMaps != null && allUsersMaps.size() > 0) {
        } else {
            logger.error("没有查询任何符合的学员信息");
            return "";
        }
        if (allCompanyMaps != null && allCompanyMaps.size() > 0) {
        } else {
            logger.error("没有查询任何符合的公司信息");
            return "";
        }
        BoundZSetOperations boundZSetOperations = null;
        if (companyId.equals("0")) {
            boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder());
            //查询该公司下的所有人
            userIds = boundZSetOperations.reverseRange(0, -1);
        } else {
            Boolean newCompanyScore = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyScore(companyId));
            if (newCompanyScore) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId)).reverseRange(0, -1);
            }
            Boolean newPersonProvinceOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonProvinceOrder(companyId));
            if (newPersonProvinceOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(companyId));
                userIds = boundZSetOperations.reverseRange(0, -1);
            }
            Boolean newPersonCityOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonCityOrder(companyId));
            if (newPersonCityOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(companyId)).reverseRange(0, -1);
            }
            Boolean newPersonCityAreaOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId));
            if (newPersonCityAreaOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId)).reverseRange(0, -1);
            }

            if (userIds == null || userIds.size() == 0) {
                ResponseData.fail("当前公司下没有人参赛,或者公司不存在");
                return "";
            }
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        Map<String, Object> itemMap;
        for (String userId : userIds) {
            if (userId == null || userId.equals("")) {
                continue;
            }
            itemMap = new HashMap<>();
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .score(userId);
            Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCountryOrder())
                    .reverseRank(userId);
            if(aLong==null){
                aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                        .reverseRank(userId);

            }
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                    .get(userId);
            if (duration == null) {
                duration = 0;
            }
            if (score == null) {
                score = 0.00;
            }
            Users users = allUsersMaps.get(userId);
            if (users != null) {
                Long companyId1 = users.getCompanyId();
                Long companyRank = -1L;
                if (companyId1 != null) {
                    companyRank = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId1.toString())).reverseRank(userId);
                    if (companyRank != null) {
                    } else {
                        companyRank = -1L;
                    }
                    String companyName = allCompanyMaps.get(companyId1);
                    if (companyName != null) {
                        itemMap.put("companyName", companyName);
                    }
                    itemMap.put("userName", users.getNickname());
                    itemMap.put("countryRank", aLong == null ? -1 : aLong);
                    itemMap.put("phone", users.getTelephone());
                    itemMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
                    itemMap.put("duration", duration);
                    itemMap.put("companyRank", companyRank);
                    maps.add(itemMap);
                }

            }

        }
       // CollectionUtils.orderMapByFields(maps, "score", "duration");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(maps);
    }

    public PageBean queryPersonOrderInCompanyLimit(String companyId, Integer currentPage, Integer pageSize) {
        PageBean pageBean = new PageBean();
        Set<String> userIds = new HashSet<>();
        BoundZSetOperations boundZSetOperations = null;
        if (companyId.equals("0")) {
            boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder());
            //查询该公司下的所有人
            userIds = boundZSetOperations.reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
        } else {
            Boolean newCompanyScore = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyScore(companyId));
            if (newCompanyScore) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId)).reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }
            Boolean newPersonProvinceOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonProvinceOrder(companyId));
            if (newPersonProvinceOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(companyId));
                userIds = boundZSetOperations.reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }
            Boolean newPersonCityOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonCityOrder(companyId));
            if (newPersonCityOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityOrder(companyId)).reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }
            Boolean newPersonCityAreaOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId));
            if (newPersonCityAreaOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId));
                userIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCityAreaOrder(companyId)).reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }

            if (userIds == null || userIds.size() == 0) {
                ResponseData.fail("当前公司下没有人参赛,或者公司不存在");
                pageBean.setPageSize(pageSize);
                pageBean.setPageNum(currentPage);
                int pages = pageSize;
                pageBean.setPages(pages);
                pageBean.setTotal(0);
                pageBean.setList(null);
                return pageBean;
            }
        }
        Long totalSize = boundZSetOperations.zCard();
        List<Map<String, Object>> maps = new ArrayList<>();
        Map<String, Object> itemMap;
        for (String userId : userIds) {
            if (userId == null || userId.equals("")) {
                continue;
            }
            itemMap = new HashMap<>();
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .score(userId);
            Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.durationPersonCountryOrder())
                    .reverseRank(userId);
            if(aLong==null){
                aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                        .reverseRank(userId);

            }
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                    .get(userId);
            if (duration == null) {
                duration = "0";
            }
            Users users = userRepository.findUsersById(LangUtil.parseLong(userId));
            if (score == null) {
                score = 0.00;
            }
            if (users != null) {
                Long companyId1 = users.getCompanyId();
                Company companyById = companyRepository.findCompanyById(companyId1);
                Long companyRank = -1L;
                if (companyById != null) {
                    companyRank = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId1.toString())).reverseRank(userId);
                    if (companyRank != null && companyRank == 0) {
                    } else {
                        companyRank = -1L;
                    }
                    if (companyById != null) {
                        itemMap.put("companyName", companyById.getCompanyName());
                    }
                    itemMap.put("userName", users.getNickname());
                    itemMap.put("userId", users.getId().toString());
                    itemMap.put("countryRank", aLong == null ? -1 : aLong);
                    itemMap.put("phone", users.getTelephone());
                    itemMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
                    itemMap.put("duration", duration);
                    itemMap.put("companyRank", companyRank);
                    maps.add(itemMap);
                }
            }
        }
        pageBean.setPageSize(pageSize);
        pageBean.setPageNum(currentPage);
        int pages = pageSize;
        pageBean.setPages(pages);
        pageBean.setTotal(totalSize);
        pageBean.setList(maps);
        return pageBean;
    }

    public PageBean queryCompanyInParentLimit(String pid, Integer currentPage, Integer pageSize) {
        PageBean pageBean = new PageBean();
        Set<String> companyIds = new HashSet<>();
        BoundZSetOperations boundZSetOperations = null;
        if (pid.equals("0")) {
            boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder());
            //查询该公司下的所有人
            companyIds = boundZSetOperations.reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
        } else {
            Boolean newCompanyCityAreaOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid));
            if (newCompanyCityAreaOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid));
                companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid)).reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }
            Boolean newCompanyProvinceOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyProvinceOrder(pid));
            if (newCompanyProvinceOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(pid));
                companyIds = boundZSetOperations.reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }
            Boolean newCompanyCityOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyCityOrder(pid));
            if (newCompanyCityOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(pid));
                companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(pid)).reverseRange(currentPage * pageSize, (currentPage + 1) * pageSize - 1);
            }

            if (companyIds == null || companyIds.size() == 0) {
                ResponseData.fail("当前公司下没有人参赛,或者公司不存在");
                pageBean.setPageSize(pageSize);
                pageBean.setPageNum(currentPage);
                int pages = pageSize;
                pageBean.setPages(pages);
                pageBean.setTotal(0);
                pageBean.setList(null);
                return pageBean;
            }
        }
        Long totalSize = boundZSetOperations.zCard();
        List<GroupTotalScore> groupTotalScoreByCompanyIds = new ArrayList<>();
        if (companyIds != null) {
            for (String companyId : companyIds) {
                if (companyId == null || companyId.equals("")) {
                    continue;
                }
                // List<Long> provinceIds = getProvinceIds(1);
                GroupTotalScore groupTotalScore = new GroupTotalScore();
                Company company = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
                Double score = boundZSetOperations
                        .score(companyId);
                groupTotalScore.setCompanyScores(score == null ? "0" : LangUtil.parseDoubleLatterTwo(score, 0.00).toString());
                groupTotalScore.setDepartName(company.getCompanyName());
                groupTotalScore.setDepartid(company.getId().toString());
                Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryOrder())
                        .reverseRank(companyId);
                groupTotalScore.setCountryRanking(aLong == null ? "-1" : aLong.toString());
                Integer count = getPersonCount(LangUtil.parseLong(companyId));
                groupTotalScore.setCount(count == null ? "0" : count.toString());
                Double totalPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryNumber()
                ).score(companyId);
                Long provinceRank = -1L;
                provinceRank = boundZSetOperations.reverseRank(companyId);
                groupTotalScore.setProvinceRanking(provinceRank == null ? "-1" : provinceRank.toString());
                groupTotalScore.setTotalPersonCount(totalPersonCount == null ? "0" : totalPersonCount.toString());
                groupTotalScoreByCompanyIds.add(groupTotalScore);
            }
        }
        pageBean.setPageSize(pageSize);
        pageBean.setPageNum(currentPage);
        int pages = pageSize;
        pageBean.setPages(pages);
        pageBean.setTotal(totalSize);
        pageBean.setList(groupTotalScoreByCompanyIds);
        return pageBean;
    }

    @Override
    public String queryCompanyInParent(String pid) throws JsonProcessingException {
        Set<String> companyIds = new HashSet<>();
        Map<String, String> allCompanyMaps = operationSumService.getAllCompanyMaps();
        Map<String, Integer> allCompanyCount = operationSumService.getCompanyPersonInnerCount();
        if (allCompanyMaps != null && allCompanyMaps.size() > 0) {
        } else {
            logger.error("没有查询任何符合的公司信息");
            return "";
        }
        if (allCompanyCount != null && allCompanyCount.size() > 0) {
        } else {
            logger.error("没有查询任何符合的公司数量信息");
            return "";
        }
        BoundZSetOperations boundZSetOperations = null;
        if (pid.equals("0")) {
            boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder());
            //查询该公司下的所有人
            companyIds = boundZSetOperations.reverseRange(0, -1);
        } else {
            Boolean newCompanyCityAreaOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid));
            if (newCompanyCityAreaOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid));
                companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityAreaOrder(pid)).reverseRange(0, -1);
            }
            Boolean newCompanyProvinceOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyProvinceOrder(pid));
            if (newCompanyProvinceOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(pid));
                companyIds = boundZSetOperations.reverseRange(0, -1);
            }
            Boolean newCompanyCityOrder = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyCityOrder(pid));
            if (newCompanyCityOrder) {
                boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(pid));
                companyIds = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCityOrder(pid)).reverseRange(0, -1);
            }

            if (companyIds == null || companyIds.size() == 0) {
                ResponseData.fail("当前公司下没有人参赛,或者公司不存在");
                return "";
            }
        }
        List<GroupTotalScore> groupTotalScoreByCompanyIds = new ArrayList<>();
        if (companyIds != null) {
            for (String companyId : companyIds) {
                if (companyId == null || companyId.equals("")) {
                    continue;
                }
                GroupTotalScore groupTotalScore = new GroupTotalScore();
                String companyName = allCompanyMaps.get(companyId);
                //Company company = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
                Double score = boundZSetOperations
                        .score(companyId);
                groupTotalScore.setCompanyScores(score == null ? "0" : LangUtil.parseDoubleLatterTwo(score, 0.00).toString());
                groupTotalScore.setDepartName(companyName);
                groupTotalScore.setDepartid(companyId);
                Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryOrder())
                        .reverseRank(companyId);
                groupTotalScore.setCountryRanking(aLong == null ? "-1" : aLong.toString());
                List<Long> longs = queryCompanyIdListByStarId(LangUtil.parseLong(companyId));
                if (longs != null) {
                    longs.add(LangUtil.parseLong(companyId));
                }
                Integer count = allCompanyCount.get(companyId);
                // Long count = userRepository.countAllByCompanyIdIsIn(longs);
                groupTotalScore.setCount(count == null ? "0" : count.toString());
                Double totalPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryNumber()
                ).score(companyId);
                Long provinceRank = -1L;
                provinceRank = boundZSetOperations.reverseRank(companyId);
                groupTotalScore.setProvinceRanking(provinceRank == null ? "-1" : provinceRank.toString());
                groupTotalScore.setTotalPersonCount(totalPersonCount == null ? "0" : totalPersonCount.toString());
                groupTotalScoreByCompanyIds.add(groupTotalScore);
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(groupTotalScoreByCompanyIds);
    }

    @Override
    public ResponseData searchPersonRecord(String userId) throws JsonProcessingException {
        List<UserExamRecord> userExamRecords = userExamRecordRepository.queryUserExamRecordsByUserId(userId);
        return ResponseData.ok(userExamRecords);
    }

    @Override
    public String searchCompanyInParent(String companyName) throws IOException {
        List<Company> companies = companyRepository.findByHql(companyName);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> maps = new ArrayList<>();
        if (companies != null && companies.size() > 0) {
            for (Company company : companies) {
                String json = queryCompanyInParent(company.getId().toString());
                List<Map<String, Object>> data = (List<Map<String, Object>>) objectMapper.readValue(json, List.class);
                if (maps != null) {
                    maps.addAll(data);
                }
            }
        }

        return objectMapper.writeValueAsString(maps);
    }

    @Override
    public String searchPersonInParent(String companyName) throws IOException {
        List<Company> companies = companyRepository.findByHql(companyName);
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> maps = new ArrayList<>();
        if (CollectionUtils.checkNotNullList(companies)) {
            for (Company company : companies) {
                String json = queryPersonOrderInCompany(company.getId().toString());
                List<Map<String, Object>> data = (List<Map<String, Object>>) objectMapper.readValue(json, List.class);
                if (data != null) {
                    maps.addAll(data);
                }

            }
        }
        return objectMapper.writeValueAsString(maps);
    }

    @Override
    public PageBean getPersonRangeRankingsById(final Long companyId, int currentPage, int pageSize) {
        PageBean pageBean = queryPersonOrderInCompanyLimit(companyId.toString(), currentPage, pageSize);
        return pageBean;
    }


    /**
     * 全省企业排名
     *
     * @param provinceId
     * @return
     */
    @Override
    public ResponseData getProvinceRangeRankings(Long provinceId) {
        logger.info("|-> 全省企业排名 请求参数" + provinceId);
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                .reverseRange(0, 100);
        for (String companyId : set) {
            if (StringUtils.isEmpty(companyId)) {
                continue;
            }
            Map<String, Object> stringObjectMap = new HashMap<>();
            Company companyById = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
            if (companyById == null && !companyById.equals("")) {
                continue;
            }
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
                    .score(companyById.getId().toString());
            if (score == null) {
                score = 0.00;
            }
            stringObjectMap.put("personScore", score.intValue());
            stringObjectMap.put("companyScore", LangUtil.parseDoubleLatterTwo(score, 0.00));
            stringObjectMap.put("companyName", companyById.getCompanyName());
            stringObjectMap.put("departName", companyById.getCompanyName());
            resultMap.add(stringObjectMap);
        }
        return ResponseData.ok(resultMap);
    }

    @Override
    public PageBean getCompanyRangeRankingsById(Long provinceId, int currentPage, int pageSize) {
        PageBean pageBean = queryCompanyInParentLimit(provinceId.toString(), currentPage, pageSize);
        return pageBean;
    }


    /**
     * 施工单位参赛统计
     *
     * @param provinceId
     * @return
     */
    @Override
    public ResponseData countConstructionUnit(final String provinceId) {
        logger.info("|-> 施工单位参赛统计 请求参数" + provinceId);
        final List<Map<String, Object>> scoreList = getCompanyOrderByType(provinceId, (byte) UnitType.CONSTRUCTION);
        return ResponseData.ok(scoreList);
    }


    /**
     * 县市水务局参赛统计
     *
     * @param provinceId
     * @return
     */
    @Override
    public ResponseData countShuiWuUnit(String provinceId) {
        logger.info("|-> 县市水务局参赛统计 请求参数" + provinceId);
        final List<Map<String, Object>> scoreList = getCompanyOrderByType(provinceId, (byte) UnitType.SHUIWU);
        return ResponseData.ok(scoreList);
    }

    /**
     * 厅直单位参赛统计
     *
     * @param provinceId
     * @return
     */
    @Override
    public ResponseData countTingZhiUnit(String provinceId) {
        logger.info("|-> 厅直单位参赛统计 请求参数" + provinceId);
        final List<Map<String, Object>> scoreList = getCompanyOrderByType(provinceId, (byte) UnitType.TINGZHI);
        return ResponseData.ok(scoreList);
    }

    /**
     * 市水利局排名(2级）
     *
     * @param provinceId 省水利厅的Id
     * @return
     */
    @Override
    public ResponseData getRankingSLJ(String provinceId) {
        List<Map<String, Object>> resultMaps = getResultRankingSLJ(provinceId);
        CollectionUtils.orderMapByFieldsAsc(resultMaps, "companyScores", "percentage");
        return ResponseData.ok(resultMaps);
    }


    private List<Map<String, Object>> getResultRankingSLJ(String provinceId) {
        List<Map<String, Object>> resultMaps = new ArrayList<>();
        List<Long> cityIds = MemoryClient.instance.getCompanyIdsCache().get("index_" + provinceId);
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        if (cityIds != null) {
            //地市排名不需要添加省
            cityIds.remove(LangUtil.parseLong(provinceId));
            cityIds.removeAll(countryIgnoreUnitIds);
            List<Company> companiesByIdIsIn = companyRepository.findCompaniesByIdIsIn(cityIds);
            for (Long cityId : cityIds) {
                for (Company company : companiesByIdIsIn) {
                    if (company != null && company.getId().longValue() == cityId.longValue()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        Double percentage = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                                .score(cityId.toString());
                        Object temTotalScores = redisTemplate.boundHashOps("totalScores").get(cityId.toString());
                        Double totalScores = LangUtil.parseDoubleLatterTwo(temTotalScores, 0.00);
                        Object temCompanyPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(cityId.toString());
                        Long companyPersonCount = LangUtil.parseLong(temCompanyPersonCount, 0L);
                        Object temUnitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber").get(cityId.toString());
                        Long unitsParticipatingNumber = LangUtil.parseLong(temUnitsParticipatingNumber, 0L);
                        itemMap.put("companyId", company.getId());
                        itemMap.put("companyName", company.getCompanyName());
                        itemMap.put("unitsParticipatingNumber", unitsParticipatingNumber);
                        itemMap.put("companyScores", LangUtil.parseDoubleLatterTwo(totalScores, 0.00));
                        itemMap.put("companyPersonCount", companyPersonCount);
                        itemMap.put("percentage", LangUtil.parseDoubleLatterFive(percentage, 0.00));
                        resultMaps.add(itemMap);
                    }
                }
            }
        }
        CollectionUtils.orderMapByField(resultMaps, "companyScores");
        return resultMaps;
    }


    /**
     * 指定全国 下面市水利局排名  3级
     *
     * @return
     */
    @Override
    public ResponseData getCountryRankingSLJ() {
        List<Map<String, Object>> resultRankingSLJ = new ArrayList<>();
        //国家水利部
        List<Company> lowCountryIds = companyRepository.findByPidIs(0L);
        if (CollectionUtils.checkNotNullList(lowCountryIds)) {
            logger.info(lowCountryIds.toString());
        } else {
            logger.info("no search country");
        }
        List<Map<String, Object>> resultRankingSLJVo = new ArrayList<>();
        //多个单位
        List<Company> byPidIs = companyRepository.findByPidIs(lowCountryIds.get(0).getId());
        if (byPidIs != null && byPidIs.size() > 0) {
            for (int i = 0; i < byPidIs.size(); i++) {
                Company company = byPidIs.get(i);
                if (company != null && company.getCode().equals("001")) {
                    //省级水利厅
                    List<Company> provinceIds = companyRepository.findByPidIs(company.getId());
                    if (CollectionUtils.checkNotNullList(provinceIds)) {
                        for (Company companyInfo : provinceIds) {
                            List<Map<String, Object>> rankingSLJ = getResultRankingSLJ(companyInfo.getId().toString());
                            resultRankingSLJ.addAll(rankingSLJ);
                        }
                    }
                    break;
                }
            }
            if (resultRankingSLJ != null && resultRankingSLJ.size() > 0) {
                for (int i = 0; i < resultRankingSLJ.size(); i++) {
                    Map<String, Object> objectMap = resultRankingSLJ.get(i);
                    String companyName = (String) objectMap.get("companyName");
                    if (StringUtils.isNotEmpty(companyName)) {
                        if (StringUtils.contains(companyName, "水利局") || StringUtils.contains(companyName, "水务局")) {
                            if (!StringUtils.contains(companyName, "局机关")) {
                                resultRankingSLJVo.add(resultRankingSLJ.get(i));
                            }
                        }
                    }
                }
            }
        } else {
            logger.info("no search company");
        }
        CollectionUtils.orderMapByFields(resultRankingSLJVo, "companyScores","percentage");
        if(resultRankingSLJVo.size()>100){
            return ResponseData.ok(resultRankingSLJVo.subList(0,100));
        }else{
            return ResponseData.ok(resultRankingSLJVo);
        }
    }

    /**
     * 水利部直属单位名称
     *
     * @return
     */

    @Override
    public ResponseData getSLBOfCountOrderByScore(int type) {
        final List<Map<String, Object>> resultMap = new ArrayList<>();
        //0直属监管单位,1省级水利厅
        List<Long> provinceIds = getProvinceIds(type);
        for (Long provinceId : provinceIds) {
            Map<String, Object> map = new HashMap<String, Object>();
            //Company company = companyCache.get(provinceId);
            Company company = companyRepository.findCompanyById(provinceId);
            Object totalCount = redisTemplate.boundHashOps("totalScores")
                    .get(provinceId.toString());
            Object statePersonCount = redisTemplate.boundHashOps("companyPersonCount")
                    .get(provinceId.toString());
            Object unitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber")
                    .get(provinceId.toString());
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                    .score(provinceId.toString());
            map.put("provinceId", provinceId);
            map.put("totalCount", totalCount == null ? 0 : LangUtil.parseDoubleLatterTwo(totalCount, 0.00));
            map.put("statePersonCount", statePersonCount == null ? 0 : statePersonCount);
            map.put("unitsParticipatingNumber", unitsParticipatingNumber == null ? 0 : unitsParticipatingNumber);
            map.put("parentCompanyId", company.getId());
            map.put("parentCompanyName", company.getCompanyName());
            int personCount = getPersonCount(provinceId);
            map.put("personNunmber", personCount);
            if (score == null) {
                score = 0.00;
            }
            LangUtil.parseDoubleLatterFour(score, 0.00);
            map.put("arenaRatedBattle", score);
            resultMap.add(map);
        }
        CollectionUtils.OrderByMapContainsDoubleBigToLittle(resultMap, "totalCount");
        for (int i = 0; i < resultMap.size(); i++) {
            resultMap.get(i).put("totalCountRanking", i + 1);
        }
        CollectionUtils.OrderByMapContainsDoubleBigToLittle(resultMap, "arenaRatedBattle");
        for (int i = 0; i < resultMap.size(); i++) {
            resultMap.get(i).put("arenaRatedBattleRanking", i + 1);
        }
        for (int i = 0; i < resultMap.size(); i++) {
            int totalCountRanking = (int) resultMap.get(i).get("totalCountRanking");
            int arenaRatedBattleRanking = (int) resultMap.get(i).get("arenaRatedBattleRanking");
            resultMap.get(i).put("countryRanking", totalCountRanking + arenaRatedBattleRanking);
        }
        CollectionUtils.OrderByMapContainsIntegerLittleToBig(resultMap, "countryRanking");
        for (int i = 0; i < resultMap.size(); i++) {
            resultMap.get(i).put("ranking", i + 1);
        }
        for (Map<String, Object> objectMap : resultMap) {
            Object arenaRatedBattle = objectMap.get("arenaRatedBattle");
            Double aDouble = LangUtil.parseDoubleLatterFour(arenaRatedBattle, 0.00);
            String arenaRatedBattlePrc = "0.00%";
            if (aDouble != null) {
                String s = LangUtil.parserDoubleTwo(aDouble * 100);
                arenaRatedBattlePrc = s + "%";
            }
            objectMap.put("arenaRatedBattle", arenaRatedBattlePrc);
        }
        for (Map<String, Object> objectMap : resultMap) {
            Object totalCount = objectMap.get("totalCount");
            objectMap.put("totalCount", totalCount.toString());
        }
        return ResponseData.ok(resultMap);
    }

    public List<Long> getProvinceIds(int orderType) {
        List<Long> longs = new ArrayList<>();
        logger.info("getProvinceIds,orderType:" + orderType);
        //国家水利部
        List<Company> lowCountryIds = companyRepository.findByPidIs(0L);

        if (lowCountryIds != null && lowCountryIds.size() > 0) {
            logger.info(lowCountryIds.toString());
        } else {
            logger.info("getProvinceIds,no search country");
        }
        Long id = lowCountryIds.get(0).getId();
        logger.info("countryId:" + id);
        //多个单位
        List<Company> byPidIs = companyRepository.findByPidIs(lowCountryIds.get(0).getId());
        if (byPidIs == null) {
            logger.info("getProvinceIds,no search country next level...");
            return longs;
        }
        String type;
        Company companyByCode = null;
        if (orderType == 0) {
            type = "002";
            String unitName = IgnoreUnitEnum.JIANANZHONGXIN.getUnitName();
            companyByCode = companyRepository.findCompanyByCompanyName(unitName);
        } else if (orderType == 1) {
            type = "001";
        } else if (orderType == 2) {
            type = "003";
        } else {
            type = "";
        }
        for (int i = 0; i < byPidIs.size(); i++) {
            if (byPidIs.get(i) != null && byPidIs.get(i).getCode().equals(type)) {
                //0直属监管单位,1省级水利厅,2水利部直属普通单位
                //companyIdsCache
                //
                longs = MemoryClient.instance.getCompanyIdsCache().get("index_" + byPidIs.get(i).getId());
                if (companyByCode != null) {
                    if (longs.contains(companyByCode.getId())) {
                        longs.remove(companyByCode.getId());
                    }
                }
            }
        }
        return longs;
    }

    /**
     * 查询全国用户下面的信息单位排名,全国排名
     */
    @Override
    public PageBean getAllCompanyPersonOrderBy(Long cityId, Integer currentPage, Integer pageSize) {
        logger.info("getAllCompanyPersonOrderBy:参数{},{},{}"
                , cityId, currentPage, pageSize);
        PageBean pageBean = queryPersonOrderInCompanyLimit(cityId.toString(), currentPage, pageSize);
        return pageBean;


    }

    private Set<Long> getCompanyIdsFromUserList(List<Users> usersList) {
        Set<Long> longs = new HashSet<>();
        for (Users users : usersList) {
            Long companyId = users.getCompanyId();
            if (companyId != null) {
                longs.add(companyId);
            }
        }
        return longs;
    }

    @Override
    public ResponseData getCountOfCompany(String companyId) {
        Map<String, Object> result = new HashMap<>();
        String date = TimeUtil.getDate().toString();
        if (companyId.equals("0")) {
            Object examPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            if (examPersonCount == null) {
                examPersonCount = 0;
            }
            //总得分
            String totalAllStudentScoreCount = (String) redisTemplate.boundHashOps("totalCount").get("totalAllStudentScoreCount");
            List<Company> byPidIs = companyRepository.findByPidIs(0L);
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(byPidIs.get(0).getId().toString());
            Long numberOfParticipatingUnits = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).zCard();
            result.put("personCount", getPersonCount(LangUtil.parseLong(companyId)));
            result.put("examPersonCount", examPersonCount);
            result.put("totalAllStudentScoreCount", LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits);
        } else {
            Object examPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            if (examPersonCount == null) {
                examPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                        .score(companyId);
            }
            if (examPersonCount == null) {
                examPersonCount = 0;
            }
            Object totalAllStudentScoreCount = redisTemplate.boundHashOps("totalScores").get(companyId);
            if (totalAllStudentScoreCount == null) {
                totalAllStudentScoreCount = 0;
            }
            Object numberOfParticipatingUnits = redisTemplate.boundHashOps("unitsParticipatingNumber").get(companyId);
            if (numberOfParticipatingUnits == null) {
                numberOfParticipatingUnits = 0;
            }
            result.put("personCount", getPersonCount(LangUtil.parseLong(companyId)));
            result.put("examPersonCount", examPersonCount);
            result.put("totalAllStudentScoreCount", LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits);
        }
        return ResponseData.ok(result);
    }


    @Override
    public ResponseData getCountOfStudent(String companyId) {
        Map<String, Object> result = new HashMap<>();
        String date = TimeUtil.getDate().toString();
        if (companyId.equals("0")) {
            //实际参加人数;
            Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                    .reverseRange(0, -1);
            Object examPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            if (examPersonCount == null) {
                examPersonCount = 0;
            }
            //总得分
            String totalAllStudentScoreCount = (String) redisTemplate.boundHashOps("totalCount").get("totalAllStudentScoreCount");
            List<Company> byPidIs = companyRepository.findByPidIs(0L);
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(byPidIs.get(0).getId().toString());
            if (unitRanking == null) {
                unitRanking = -1L;
            }
            Long numberOfParticipatingUnits = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).zCard();
            result.put("personCount", getPersonCount(LangUtil.parseLong(companyId)));
            result.put("examPersonCount", examPersonCount == null ? 0 : examPersonCount);
            result.put("totalAllStudentScoreCount", totalAllStudentScoreCount == null ? 0 : LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits == null ? 0 : numberOfParticipatingUnits);
            result.put("unitRanking", unitRanking);
        } else {
            // Long count = userRepository.countAllByCompanyIdIsIn(longs);
            //   List<Users> allByCompanyIdIn = userRepository.findAllByCompanyIdIn(longs);
            Object examPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            if (examPersonCount == null) {
                examPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber())
                        .score(companyId);
            }
            if (examPersonCount == null) {
                examPersonCount = 0;
            }
            Object totalAllStudentScoreCount = redisTemplate.boundHashOps("totalScores").get(companyId);
            if (totalAllStudentScoreCount == null) {
                totalAllStudentScoreCount = 0;
            }
            Object numberOfParticipatingUnits = redisTemplate.boundHashOps("unitsParticipatingNumber").get(companyId);
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(companyId);
            if (unitRanking == null) {
                unitRanking = -1L;
            }
            result.put("personCount", getPersonCount(LangUtil.parseLong(companyId)));
            result.put("examPersonCount", examPersonCount == null ? 0 : examPersonCount);
            result.put("totalAllStudentScoreCount", totalAllStudentScoreCount == null ? 0 : LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits == null ? 0 : numberOfParticipatingUnits);
            result.put("unitRanking", unitRanking);
        }
        return ResponseData.ok(result);
    }

    public int getPersonCount(Long companyId) {
        return operationSumService.getPersonCount(companyId);
    }

    /**
     * 通过类型查询公司信息
     *
     * @param provinceId
     * @param Type
     * @return
     */
    private List<Company> queryOfTypeCompanies(String provinceId, final Byte Type) {
        List<Company> companyContents = getCompanyContent(LangUtil.parseLong(provinceId), Type);
        return companyContents;
    }


    /**
     * 获取公司信息
     *
     * @param provinceId
     * @return
     */
    private List<Company> getCompanyContent(Long provinceId, Byte typeId) {
        List<Long> longs = new ArrayList<>();
        longs.add(provinceId);
        List<Long> companies = strategyForFactory.calculation(String.valueOf(provinceId));
        List<Company> companies1 = companyRepository.queryCompaniesByPidIsInAndTypeId(companies, typeId);
        return companies1;
    }


    private List<Map<String, Object>> getCompanyOrderByType(String provinceId, Byte type) {
        final List<Map<String, Object>> scoreList = new ArrayList<>();
        BoundZSetOperations boundZSetOperations = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                .newCompanyProvinceOrder(provinceId.toString()));
        List<Company> companies = queryOfTypeCompanies(provinceId, type);
        for (Company company : companies) {
            Double score = boundZSetOperations.score(company.getId().toString());
            Object personCount = redisTemplate.boundHashOps("companyPersonCount").get(company.getId().toString());
//            Double personCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceNumber(provinceId.toString()))
//                    .score(company.getId().toString());
            Double percentage = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                    .score(company.getId().toString());
            Map<String, Object> companyIntegerMap = new HashMap<>();
            companyIntegerMap.put("company_id", String.valueOf(company.getId()));
            companyIntegerMap.put("company_name", company.getCompanyName());
            companyIntegerMap.put("people_count", personCount == null ? 0 : personCount);
            companyIntegerMap.put("score_total", score == null ? 0 : score);
            companyIntegerMap.put("percentage", LangUtil.parseDoubleLatterFive(percentage, 0.00));
            scoreList.add(companyIntegerMap);
        }
        CollectionUtils.orderMapByFieldsAsc(scoreList, "score_total", "percentage");
        return scoreList;
    }


    private void loadData() {
        System.out.println("开始加载全国专用数据...");
        companyCache = CacheManager.newCache();
        companyIdsCache = CacheManager.newCache();
        companyIdsCacheReverse = CacheManager.newCache();
        strategyForFactory.additionalDataSources(new DataHelper<CacheDataSource<String, List<Long>>>() {
            @Override
            public CacheDataSource<String, List<Long>> loadData() {
                return companyIdsCache;
            }
        });
        memoryClient = MemoryClient.instance
                .addCompanyCache(companyCache)
                .addCompanyIdsCache(companyIdsCache)
                .addCompanyIdsCacheReverse(companyIdsCacheReverse)
                .addCompanyRepository(companyRepository)
                .build();
        memoryClient.loadDataIntoMem();
    }

    @PostConstruct
    public void initData() {
        if (timeManager.getCode() != null && timeManager.getCode().equals(PlatformCode.COUNTRY)) {
            System.out.println("当前平台为:" + PlatformCode.COUNTRY);
            loadData();
        }
    }

    @Override
    public CacheManager<Long, Company> getCompanyCache() {
        return companyCache;
    }

    @Override
    public CacheManager<String, List<Long>> getCompanyIdsCache() {
        return companyIdsCache;
    }

    @Override
    public CacheManager<String, List<Long>> getCompanyIdsCacheReverse() {
        return companyIdsCacheReverse;
    }

    @Override
    public void insertTestDb() {
    }

}