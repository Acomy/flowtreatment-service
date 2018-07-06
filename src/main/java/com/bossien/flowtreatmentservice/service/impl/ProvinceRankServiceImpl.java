package com.bossien.flowtreatmentservice.service.impl;

import com.bossien.common.base.ResponseData;
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
import com.bossien.flowtreatmentservice.service.IProvinceRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.utils.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * @author gb
 */
@Service
public class ProvinceRankServiceImpl implements IProvinceRankService {
    @Autowired
    TimeManager timeManager;
    Logger logger = LoggerFactory.getLogger(ProvinceRankServiceImpl.class);

    private static final int DEFAULT_CAPACITY = 16;

    private static final int DEFAULT_TIMEOUT = 300;
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

    private static final String PRIVINCEID = "437831680";

    @Autowired
    RedisTemplate redisTemplate;

    private List<Long> ignoreUnits;

    private List<Long> provinceIds;
    @Autowired
    UserExamRecordRepository userExamRecordRepository;
    @Autowired
    OperationSumService operationSumService;

    /**
     * 市安监局排名前6的
     * gb
     *
     * @return
     */
    @Override
    public ResponseData getCityOrderByScore() {
        List<Map<String, Object>> maps = new ArrayList<>();
        List<Map<String, Object>> resultMap = new ArrayList<>();
        //查出17个地级市
        List<Long> provinceIds = getProvinceIds();
        for (Long provinceId : provinceIds) {
            Map<String, Object> map = new HashMap<String, Object>();
            Company company = companyRepository.findCompanyById(provinceId);
            if (company == null) {
                continue;
            }
            Object totalCount = redisTemplate.boundHashOps("totalScores")
                    .get(provinceId.toString());
            map.put("score", LangUtil.parseDoubleLatterTwo(totalCount, 0.00));
            map.put("name", company.getCompanyName());
            map.put("cityId", company.getId());
            resultMap.add(map);
            if(!org.apache.commons.lang3.StringUtils.isEmpty(company.getCompanyName())){
                if(org.apache.commons.lang3.StringUtils.contains(company.getCompanyName(),"安监局")){
                    resultMap.add(map);
                }
            }
        }
        orderMapByField(resultMap, "score");
        if (resultMap.size() > 5) {
            resultMap = resultMap.subList(0, 6);
        }
        return ResponseData.ok(resultMap);
    }

    @Override
    public ResponseData getCityOrderByTypeAndScore(int type) {
        List<Map<String, Object>> resultMap = new ArrayList<>();
        List<Long> provinceIds = null;
        if (type == 0) {
            List<String> strings = Arrays.asList("仙桃市安监局", "天门市安监局", "潜江市安监局", "神农架林区安监局");
            provinceIds = companyRepository.findIdsByHql(strings);
        } else if (type == 1) {
            List<String> strings = Arrays.asList("咸宁市安监局", "恩施州安监局", "孝感市安监局", "武汉市安监局"
                    , "随州市安监局", "黄冈市安监局", "黄石市安监局", "十堰市安监局"
                    , "襄阳市安监局", "荆门市安监局", "荆州市安监局", "恩施州安监局直管单位", "鄂州市安监局"
                    , "宜昌市安监局", "湖北省安监局直管单位");
            provinceIds = companyRepository.findIdsByHql(strings);
        }
        for (Long provinceId : provinceIds) {
            Map<String, Object> map = new HashMap<String, Object>();
            Company company = companyRepository.findCompanyById(provinceId);
            if (company == null) {
                continue;
            }
            Object totalCount = redisTemplate.boundHashOps("totalScores")
                    .get(provinceId.toString());
            Object statePersonCount = redisTemplate.boundHashOps("companyPersonCount")
                    .get(provinceId.toString());
            Object unitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber")
                    .get(provinceId.toString());
            map.put("total", LangUtil.parseDoubleLatterTwo(totalCount, 0.0).longValue());
            map.put("enterpriseCount", unitsParticipatingNumber == null ? 0 : unitsParticipatingNumber);
            map.put("personCount", statePersonCount == null ? 0 : statePersonCount);
            map.put("cityId", company.getId());
            map.put("cityName", company.getCompanyName());
            resultMap.add(map);
        }
        orderMapByField(resultMap, "total");
        return ResponseData.ok(resultMap);
    }

    public List<Long> getProvinceIds() {
        String hubeijianyuxitong = IgnoreUnitEnum.HUBEIJIANYUXITONG.getUnitName();
        String hubeijieduxiton = IgnoreUnitEnum.HUBEIJIEDUXITON.getUnitName();
        String hubeishengsifating = IgnoreUnitEnum.HUBEISHENGSIFATING.getUnitName();
        if (null != ignoreUnits && ignoreUnits.size() > 0) {

        } else {
            //不需要统计的的单位
            ignoreUnits = companyRepository.findIdsByHql(Arrays.asList(hubeijianyuxitong, hubeijieduxiton, hubeishengsifating));
            if (ignoreUnits == null) {
                ignoreUnits = new ArrayList<>();
            }
        }
        //湖北省安监局
        List<Company> lowCountryIds = companyRepository.findByPidIs(0L);
        List<Company> cityCompanies = companyRepository.findByPidIs(lowCountryIds.get(0).getId());
        List<Long> cityCompanyIds1 = new ArrayList<>();
        for (Company company : cityCompanies) {
            //从省级排除湖北司法厅
            if (ignoreUnits.contains(company.getId())) {
                continue;
            }
            cityCompanyIds1.add(company.getId());
        }
        return cityCompanyIds1;
    }

    public Map<Long, List<Long>> getCityIds() {
        if (null != provinceIds && provinceIds.size() > 0) {
        } else {
            provinceIds = getProvinceIds();
        }
        //地级市
        Map<Long, List<Long>> cityMap = new HashMap<>();
        for (Long cityId : provinceIds) {
            //,县,市,区下面的单位
            List<Long> longs = MemoryClient.instance.getCompanyIdsCache().get("index_" + cityId.toString());
            if (longs != null) {
                for (Long aLong : longs) {
                    List<Long> lowCityIds = queryCompanyIdListByStarId(aLong);
                    if (lowCityIds == null) {
                        cityMap.put(aLong, new ArrayList<Long>());
                    } else {
                        cityMap.put(aLong, lowCityIds);
                    }
                }
            }

        }
        return cityMap;
    }

    /**
     * 县（市，区）安监局总分排行前35
     * 湖北下面的县市排名(不包含地级市)
     * gb
     *
     * @return
     */
    @Override
    public ResponseData getCityNextLowerLevel() {
        final Map<Long, List<Long>> cityIds = getCityIds();
        System.out.println(cityIds.toString());
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Long key : cityIds.keySet()) {
            Map<String, Object> map = new HashMap<>();
            Double aDouble = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .score(key.toString());
            if (aDouble == null) {
                aDouble = 0.00;
            }
            map.put("cityId", key);
            map.put("score", LangUtil.parseDoubleLatterTwo(aDouble, 0.00));
            maps.add(map);
        }
        orderMapByField(maps, "score");
        if (maps.size() > 35) {
            maps = maps.subList(0, 35);
        }
        for (Map<String, Object> map : maps) {
            Object companyId = map.get("cityId");
            Long aLong = LangUtil.parseLong(companyId);
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newCompanyDurationRecord())
                    .get(aLong.toString());
            Company companyById = companyRepository.findCompanyById(aLong);
            map.put("duration", duration == null ? String.valueOf(Integer.MAX_VALUE) : duration.toString());
            map.put("name", companyById.getCompanyName());
        }
        for (Map<String, Object> map : maps) {
            String companyName = map.get("name").toString();
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(companyName)) {
                if (org.apache.commons.lang3.StringUtils.contains(companyName, "安监局")) {
                    result.add(map);
                }
            }
        }
        CollectionUtils.orderMapByFields(result, "score", "duration");
        return ResponseData.ok(result);
    }

    @Override
    public ResponseData getCityNextLowerLevelByCityId(final Long cityId) {

        List<Map<String, Object>> maps = new ArrayList<>();
        List<Company> provinceIds = companyRepository.findByPidIs(cityId);
        //,县,市,区
        for (Company company : provinceIds) {
            if (company == null) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            Long aLong = company.getId();
            //县市区下面的总分
            Object score = redisTemplate.boundHashOps("totalScores").get(company.getId().toString());
            if (score != null) {
                map.put("cityId", company.getId());
                map.put("total", LangUtil.parseDoubleLatterTwo(score, 0.00));
                Object unitsParticipatingNumber = redisTemplate.boundHashOps("unitsParticipatingNumber").get(aLong.toString());
                Object companyPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(aLong.toString());
                if (unitsParticipatingNumber == null) {
                    unitsParticipatingNumber = 0;
                }
                if (companyPersonCount == null) {
                    companyPersonCount = 0;
                }
                map.put("enterpriseCount", unitsParticipatingNumber);
                map.put("personCount", companyPersonCount);
                maps.add(map);
            }
        }
        orderMapByField(maps, "total");
        for (Map<String, Object> map : maps) {
            Object lowCityId = map.get("cityId");
            Long aLong = LangUtil.parseLong(lowCityId);
            Company companyById = companyRepository.findCompanyById(aLong);
            map.put("countryName", companyById.getCompanyName());
        }
        return ResponseData.ok(maps);
    }

    /**
     * 指定地级市安监局下面的个人得分排名
     *
     * @param cityId
     * @return
     */
    @Override
    public ResponseData getPersonByCityId(Long cityId) {
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(cityId.toString()))
                .reverseRange(0, 35);
        for (String uid : set) {
            if (StringUtils.isEmpty(uid)) {
                continue;
            }
            Map<String, Object> stringObjectMap = new HashMap<>();
            Users usersById = userRepository.findUsersById(LangUtil.parseLong(uid));
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonProvinceOrder(cityId.toString()))
                    .score(usersById.getId().toString());
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                    .get(usersById.getId().toString());
            stringObjectMap.put("duration", duration == null ? String.valueOf(Integer.MAX_VALUE) : duration.toString());
            stringObjectMap.put("cityId", usersById.getCompanyId());
            stringObjectMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
            stringObjectMap.put("name", usersById.getUsername());
            resultMap.add(stringObjectMap);
        }
        CollectionUtils.orderMapByFields(resultMap, "score", "duration");
        return ResponseData.ok(resultMap);
    }

    @Override
    public ResponseData getCompanyByCityId(Long cityId) {
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(cityId.toString()))
                .reverseRange(0, 49);
        for (String companyId : set) {
            if (StringUtils.isEmpty(companyId)) {
                continue;
            }
            Map<String, Object> stringObjectMap = new HashMap<>();
            Company companyById = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(cityId.toString()))
                    .score(companyById.getId().toString());
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newCompanyDurationRecord())
                    .get(companyById.getId().toString());
            if (score == null) {
                score = 0.00;
            }
            stringObjectMap.put("duration", duration == null ? String.valueOf(Integer.MAX_VALUE) : duration.toString());
            stringObjectMap.put("cityId", companyById.getId());
            stringObjectMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
            stringObjectMap.put("name", companyById.getCompanyName());
            resultMap.add(stringObjectMap);
        }
        CollectionUtils.orderMapByFields(resultMap, "score", "duration");
        return ResponseData.ok(resultMap);
    }


    private List<Long> typeToIds(List<Company> companies1) {
        List<Long> longs = new ArrayList<>();
        for (Company company : companies1) {
            longs.add(company.getId());
        }
        return longs;
    }


    /**
     * 水利部直属根据指定字段排名
     *
     * @param resultTotalScores
     * @param field
     */
    private void orderMapByField(List<Map<String, Object>> resultTotalScores, final String field) {
        CollectionUtils.orderMapByField(resultTotalScores, field);
    }

    private boolean checkNotNullList(List<?> list) {
        if (null != list && list.size() != 0) {
            return true;
        } else {
            return false;
        }
    }

    private void loadData() {
        System.out.println("开始加载湖北或云南专用数据...");
        companyCache = CacheManager.newCache();
        companyIdsCache = CacheManager.newCache();
        companyIdsCacheReverse = CacheManager.newCache();
        strategyForFactory.additionalDataSources(new DataHelper<CacheDataSource<String, List<Long>>>() {
            @Override
            public CacheDataSource<String, List<Long>> loadData() {
                return companyIdsCache;
            }
        });
        MemoryClient memoryClient = MemoryClient.instance
                .addCompanyCache(companyCache)
                .addCompanyIdsCache(companyIdsCache)
                .addCompanyIdsCacheReverse(companyIdsCacheReverse)
                .addCompanyRepository(companyRepository)
                .build();
        memoryClient.loadDataIntoMem();
    }

    //----------------------------湖北统计--------------------------
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageBean getPersonRangeRankingsById(final Long companyId, int currentPage, int pageSize) {
        PageBean pageBean = queryPersonOrderInCompanyLimit(companyId.toString(), currentPage, pageSize);
        return pageBean;
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

    @Override
    public PageBean getCompanyRangeRankingsById(Long provinceId, int currentPage, int pageSize) {
        PageBean pageBean = queryCompanyInParentLimit(provinceId.toString(), currentPage, pageSize);
        return pageBean;
    }

    @Override
    public ResponseData getCountOfCompany(String companyId) {
        Map<String, Object> result = new HashMap<>();
        String date = TimeUtil.getDate().toString();
        if (companyId.equals("0")) {
            int personCount = operationSumService.getPersonCount(0L);
            //实际参加人数;
            Object examPersonCount = redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            if (examPersonCount == null) {
                examPersonCount = 0;
            }
            //总得分
            String totalAllStudentScoreCount = (String) redisTemplate.boundHashOps("totalCount").get("totalAllStudentScoreCount");
            List<Company> byPidIs = companyRepository.findByPidIs(0L);
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(byPidIs.get(0).getId().toString());
            Long numberOfParticipatingUnits = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).size();
            result.put("personCount", personCount);
            result.put("examPersonCount", examPersonCount);
            result.put("totalAllStudentScoreCount", LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits ", numberOfParticipatingUnits);
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
            result.put("personCount", operationSumService.getPersonCount(LangUtil.parseLong(companyId)));
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
        if (companyId == null) {
            //应参加人数
            long count = operationSumService.getPersonCount(0L);
            //实际参加人数;
            Long examPersonCount = (Long) redisTemplate.boundHashOps("companyPersonCount").get(companyId);
            //总得分
            String totalAllStudentScoreCount = (String) redisTemplate.boundHashOps("totalCount").get("totalAllStudentScoreCount");
            List<Company> byPidIs = companyRepository.findByPidIs(0L);
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(byPidIs.get(0).getId().toString());
            Long numberOfParticipatingUnits = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryNumber()).size();
            result.put("personCount", count);
            result.put("examPersonCount", examPersonCount == null ? 0 : examPersonCount);
            result.put("totalAllStudentScoreCount", totalAllStudentScoreCount == null ? 0 : LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits == null ? 0 : numberOfParticipatingUnits);
            result.put("unitRanking", unitRanking);
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
            Long unitRanking = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                    .reverseRank(companyId);
            result.put("personCount", operationSumService.getPersonCount(LangUtil.parseLong(companyId)));
            result.put("examPersonCount", examPersonCount == null ? 0 : examPersonCount);
            result.put("totalAllStudentScoreCount", totalAllStudentScoreCount == null ? 0 : LangUtil.parseDoubleLatterTwo(totalAllStudentScoreCount, 0.00));
            result.put("numberOfParticipatingUnits", numberOfParticipatingUnits == null ? 0 : numberOfParticipatingUnits);
            result.put("unitRanking", unitRanking);
        }
        return ResponseData.ok(result);
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
        if (companyId == null || companyId.equals("")||companyId.equals("0")) {
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
            itemMap = new HashMap<>();
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .score(userId);
            Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .reverseRank(userId);
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                    .get(userId);
            if (duration == null) {
                duration = 0;
            }
            Users users = allUsersMaps.get(userId);
            // Users users = userRepository.findUsersById(LangUtil.parseLong(userId));
            if (score == null) {
                score = 0.00;
            }
            if (users != null) {
                Long companyId1 = users.getCompanyId();
                Long companyRank = -1L;
                if (companyId1 != null) {
                    companyRank = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId1.toString())).reverseRank(userId);
                    itemMap.put("userId", users.getId().toString());
                    itemMap.put("userName", users.getNickname());
                    itemMap.put("countryRank", aLong == null ? -1 : aLong);
                    itemMap.put("phone", users.getTelephone());
                    itemMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
                    itemMap.put("duration", duration);
                    itemMap.put("companyRank", companyRank==null?-1:companyRank);
                    itemMap.put("companyName", allCompanyMaps.get(companyId1.toString()));
                    maps.add(itemMap);
                }
            }

        }
        CollectionUtils.orderMapByFields(maps, "score", "duration");
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
            itemMap = new HashMap<>();
            Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .score(userId);
            Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                    .reverseRank(userId);
            Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                    .get(userId);
            if (duration == null) {
                duration = 0;
            }
            Users users = userRepository.findUsersById(LangUtil.parseLong(userId));
            if (score == null) {
                score = 0.00;
            }
            if (users != null) {
                Long companyId1 = users.getCompanyId();
                Long companyRank = -1L;
                if (companyId1 != null) {
                    companyRank = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyScore(companyId1.toString())).reverseRank(userId);
                    if (companyRank != null) {
                    } else {
                        companyRank = -1L;
                    }
                    itemMap.put("userId", users.getId().toString());
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
        pageBean.setPageSize(pageSize);
        pageBean.setPageNum(currentPage);
        int pages = pageSize;
        pageBean.setPages(pages);
        pageBean.setTotal(totalSize);
        CollectionUtils.orderMapByFields(maps, "score", "duration");
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
                //  List<Long> provinceIds = getProvinceIds();
                GroupTotalScore groupTotalScore = new GroupTotalScore();
                Company company = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
                if (company == null) {
                    continue;
                }
                Double score = boundZSetOperations
                        .score(companyId);
                groupTotalScore.setCompanyScores(score == null ? "0" : LangUtil.parseDoubleLatterTwo(score, 0.00).toString());
                groupTotalScore.setDepartName(company.getCompanyName());
                groupTotalScore.setDepartid(company.getId().toString());
                Long aLong = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryOrder())
                        .reverseRank(companyId);
                groupTotalScore.setCountryRanking(aLong == null ? "-1" : aLong.toString());
                List<Long> longs = queryCompanyIdListByStarId(LangUtil.parseLong(companyId));
                if (longs != null) {
                    longs.add(LangUtil.parseLong(companyId));
                }
                String count = String.valueOf(operationSumService.getPersonCount(LangUtil.parseLong(companyId)));
                groupTotalScore.setCount(count);
                Double totalPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory
                        .newCompanyCountryNumber()
                ).score(companyId);
                Long provinceRank = -1L;
//                if (provinceIds != null) {
//                    for (Long provinceId : provinceIds) {
//                        Boolean aBoolean = redisTemplate.hasKey(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()));
//                        if (aBoolean) {
//                            provinceRank = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyProvinceOrder(provinceId.toString()))
//                                    .reverseRank(companyId);
//                            if (provinceRank != null) {
//                                provinceRank = 1L;
//                            }
//                        }
//
//                    }
//                }
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
            logger.error("没有查询任何符合公司下面的学员数量");
            return "";
        }
        BoundZSetOperations boundZSetOperations = null;
        if (pid == null || pid.equals("")||pid.equals("0")) {
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
                // List<Long> provinceIds = getProvinceIds();
                GroupTotalScore groupTotalScore = new GroupTotalScore();
                //Company company = companyRepository.findCompanyById(LangUtil.parseLong(companyId));
                String companyName = allCompanyMaps.get(companyId);
                if (companyName == null) {
                    continue;
                }
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
                //Long count = userRepository.countAllByCompanyIdIsIn(longs);
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
    public String searchPersonInParent(String companyName) throws IOException {
        List<Company> companies = companyRepository.findByHql(companyName);
        List<Map<String, Object>> maps = new ArrayList<>();
        if (checkNotNullList(companies)) {
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
    public ResponseData searchPersonRecord(String userId) {
        List<UserExamRecord> userExamRecords = userExamRecordRepository.queryUserExamRecordsByUserId(userId);
        return ResponseData.ok(userExamRecords);
    }

    @Override
    public String searchCompanyInParent(String companyName) throws IOException {
        List<Company> companies = companyRepository.findByHql(companyName);
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

    @PostConstruct
    public void initData() {
        if (timeManager.getCode() != null) {
            if (timeManager.getCode().equals(PlatformCode.HB) || timeManager.getCode().equals(PlatformCode.YNJT)) {
                System.out.println("当前平台为:" + timeManager.getCode());
                loadData();
            }
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
    public List<Long> queryCompanyIdListByStarId(Long provinceId) {
        return strategyForFactory.calculation(String.valueOf(provinceId));
    }
}