package com.bossien.flowtreatmentservice.controller;

import com.bossien.common.base.ResponseData;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.service.IProvinceRankService;
import com.bossien.flowtreatmentservice.utils.CollectionUtils;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping(value = "/hbwater")
public class HBWaterController {
    Logger logger = LoggerFactory.getLogger(HBWaterController.class);

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    IProvinceRankService iProvinceRankService;

    /**
     * 市排行榜分成两块
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCityOrder", method = RequestMethod.POST)
    public ResponseData getCityOrder(@RequestParam int type) {
        logger.info("省(自治区、直辖市)水利厅排名:参数" + type);
        return iProvinceRankService.getCityOrderByTypeAndScore(type);
    }

    /**
     * 市安监局排名前6的
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCityOrderByScore", method = RequestMethod.POST)
    public ResponseData getCityOrderByScore() {
        logger.info("getCityOrderByScore:参数");
        return iProvinceRankService.getCityOrderByScore();
    }

    /**
     * 县（市，区）安监局总分排行前35
     * 湖北下面的县市排名(不包含地级市)
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCityNextLowerLevel", method = RequestMethod.POST)
    public ResponseData getCityNextLowerLevel() {
        logger.info("getCityNextLowerLevel");
        return iProvinceRankService.getCityNextLowerLevel();
    }

    /**
     * 地级市下面的县市排名.县排行榜
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCityNextLowerLevelByCityId", method = RequestMethod.POST)
    public ResponseData getCityNextLowerLevelByCityId(Long cityId) {
        logger.info("getCityNextLowerLevel");
        return iProvinceRankService.getCityNextLowerLevelByCityId(cityId);
    }

    /**
     * 地级市下面的县市区下面的个人排名.个人总分排行榜35名
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getPersonByCityId", method = RequestMethod.POST)
    public ResponseData getPersonByCityId(Long cityId) {
        logger.info("getCityNextLowerLevel");
        return iProvinceRankService.getPersonByCityId(cityId);
    }

    /**
     * 地级市下面的县市区下面的单位排名.单位排名排行榜35名
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCompanyByCityId", method = RequestMethod.POST)
    public ResponseData getCompanyByCityId(Long cityId) {
        logger.info("getCityNextLowerLevel");
        return iProvinceRankService.getCompanyByCityId(cityId);
    }

    /**
     * 个人总分排行榜前50名
     *
     * @return
     */
    @RequestMapping(value = "/getPerson")
    public ResponseData getpersonScore() {
        logger.info("获取个人排名,getperson");
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .reverseRange(0, 49);
        for (String uid : set) {
            if(StringUtils.isEmpty(uid)){
                continue;
            }
            Map<String, Object> stringObjectMap = new HashMap<>();
            Users usersById = userRepository.findUsersById(LangUtil.parseLong(uid));
            if (usersById != null) {
                Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                        .get(usersById.getId().toString());
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder()).score(usersById.getId().toString());
                if (score == null) {
                    score = 0.00;
                }
                stringObjectMap.put("duration",duration==null?String.valueOf(Integer.MAX_VALUE):duration.toString());
                stringObjectMap.put("score", score.intValue());
                stringObjectMap.put("name", usersById.getUsername());
                resultMap.add(stringObjectMap);
            }
        }
        CollectionUtils.orderMapByFields(resultMap,"score","duration");
        return ResponseData.ok(resultMap);
    }

    /**
     * 全部单位排行榜前50名
     *
     * @return
     */
    @RequestMapping(value = "/getCompany")
    public ResponseData getcompanyScore() {
        logger.info("获取公司排名,getcompany");
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .reverseRange(0, 49);
        for (String companyIds : set) {
            if(StringUtils.isEmpty(companyIds)){
                continue;
            }
            Company companyById = companyRepository.findCompanyById(LangUtil.parseLong(companyIds));
            if (companyById != null) {
                Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newCompanyDurationRecord())
                        .get(companyById.getId().toString());
                Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                        .score(companyById.getId().toString());
                if (score == null) {
                    score = 0.00;
                }
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("cityId", companyById.getId());
                map.put("duration",duration==null?String.valueOf(Integer.MAX_VALUE):duration.toString());
                map.put("name", companyById.getCompanyName());
                map.put("score", score);
                resultMap.add(map);
            }
        }
        CollectionUtils.orderMapByFields(resultMap,"score","duration");
        return ResponseData.ok(resultMap);
    }

    /**
     * 获取昨天新增的公司
     *
     * @return
     */
    @RequestMapping(value = "/getAppendCompany")
    public ResponseData getAppendCompany() {
        logger.info("getAppendCompany");
        Date dateBefore = TimeUtil.getDateBefore(new Date(), 1);
        List<Company> companies = companyRepository.findCompaniesByCreateDateGreaterThanEqualAndCreateDateIsNotNull(dateBefore);
        List<Map<String, Object>> resultMaps = new ArrayList<>();
        if (CollectionUtils.checkNotNullList(companies)) {
            for (Company company : companies) {
                Map<String, Object> objectMap = new HashMap<>();
                objectMap.put("companyName", company.getCompanyName());
                resultMaps.add(objectMap);
            }
        }
        return ResponseData.ok(resultMaps);
    }


}
