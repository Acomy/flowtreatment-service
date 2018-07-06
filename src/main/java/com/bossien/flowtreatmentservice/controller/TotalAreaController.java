package com.bossien.flowtreatmentservice.controller;

import com.alibaba.fastjson.JSONObject;
import com.bossien.common.base.ResponseCode;
import com.bossien.common.base.ResponseData;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.StuTotalScore;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.task.CountryConvertTask;
import com.bossien.flowtreatmentservice.task.HBConvertTask;
import com.bossien.flowtreatmentservice.task.InitializationHbTask;
import com.bossien.flowtreatmentservice.task.InitializationTask;
import com.bossien.flowtreatmentservice.utils.CollectionUtils;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.netflix.discovery.converters.Auto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.language.bm.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;


/**
 * @author
 */
@RestController
@RequestMapping(value = "/areaTotalScore")
public class TotalAreaController {

    Logger logger = LoggerFactory.getLogger(TotalAreaController.class);
    @Autowired
    ICountryRankService iCountryRankService;

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CompanyRepository companyRepository;


    /**
     * 全省个人排名(top20)
     * gb
     *
     * @param provinceId
     * @return
     */
    @RequestMapping(value = "/getRangeRankings", method = RequestMethod.POST)
    public ResponseData getRangeRankings(@RequestParam String provinceId) {
        logger.info("全省个人排名(top20):参数" + provinceId);
        if (provinceId == null) {
            return parameterErrorDescription();
        }
        Long aLong = LangUtil.parseLong(provinceId);
        return iCountryRankService.getRangeRankings(aLong);
    }

    /**
     * 全省企业排名(top20)
     * gb
     *
     * @param companyId
     * @return
     */
    @RequestMapping(value = "/getProvinceRangeRankings", method = RequestMethod.POST)
    public ResponseData getProvinceRangeRankings(@RequestParam String companyId, Integer currentPage, Integer pageSize) {
        logger.info("全省企业排名(top20):参数" + companyId);
        if (companyId == null) {
            return parameterErrorDescription();
        }
        Long aLong = LangUtil.parseLong(companyId);
        return iCountryRankService.getProvinceRangeRankings(aLong);
    }


    /**
     * 施工单位参赛统计
     * gb
     *
     * @param provinceId
     * @return
     */
    @RequestMapping(value = "/getCountConstructionUnit", method = RequestMethod.POST)
    public ResponseData countConstructionUnit(@RequestParam String provinceId) {
        logger.info("施工单位参赛统计:参数" + provinceId);
        if (provinceId == null) {
            return parameterErrorDescription();
        }
        return iCountryRankService.countConstructionUnit(provinceId);
    }

    /**
     * 县市水务局参赛统计
     * gb
     *
     * @param provinceId
     * @return
     */
    @RequestMapping(value = "/getCountShuiWuUnit", method = RequestMethod.POST)
    public ResponseData countShuiWuUnit(@RequestParam String provinceId) {
        logger.info("县市水务局参赛统计:参数" + provinceId);
        if (provinceId == null) {
            return parameterErrorDescription();
        }
        Long aLong = LangUtil.parseLong(provinceId);
        return iCountryRankService.countShuiWuUnit(provinceId);
    }

    /**
     * 厅直单位参赛统计
     * gb
     *
     * @param provinceId
     * @return
     */
    @RequestMapping(value = "/getCountTingZhiUnit", method = RequestMethod.POST)
    public ResponseData countTingZhiUnit(@RequestParam String provinceId) {
        logger.info("厅直单位参赛统计:参数" + provinceId);
        if (provinceId == null) {
            return parameterErrorDescription();
        }
        return iCountryRankService.countTingZhiUnit(provinceId);
    }


    /**
     * 指定省 下面市水利局排名  2级
     * gb
     *
     * @param provinceId
     * @return
     */
    @RequestMapping(value = "/getRankingSLJ", method = RequestMethod.POST)
    public ResponseData getRankingSLJ(@RequestParam String provinceId) {
        logger.info("省市水利局排名:参数" + provinceId);
        if (provinceId == null) {
            return parameterErrorDescription();
        }
        return iCountryRankService.getRankingSLJ(provinceId);
    }


// 全国地市水利(水务)局排行TOP100 （沒有）

    /**
     * 指定全国 下面市水利局排名  2级
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getCountryRankingSLJ", method = RequestMethod.POST)
    public ResponseData getCountryRankingSLJ() {
        logger.info("全国市水利局排名");
        return iCountryRankService.getCountryRankingSLJ();
    }

    /**
     * 省(自治区、直辖市)水利厅排名
     * gb
     *
     * @return
     */
    @RequestMapping(value = "/getOfCountOrderByScore", method = RequestMethod.POST)
    public ResponseData getSLBOfCountOrderByScore(@RequestParam int type) {
        logger.info("省(自治区、直辖市)水利厅排名:参数" + type);
        return iCountryRankService.getSLBOfCountOrderByScore(type);
    }


    @RequestMapping(value = "/getperson", method = RequestMethod.GET)
    public ResponseData getpersonScore() {
        logger.info("获取个人排名,getperson");
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder())
                .reverseRange(0, 69);
        for (String uid : set) {
            if (uid != null && !uid.equals("")) {
                Map<String, Object> stringObjectMap = new HashMap<>();
                Users usersById = userRepository.findUsersById(LangUtil.parseLong(uid));
                if (usersById != null) {
                    Object duration = redisTemplate.boundHashOps(RedisStatusKeyFactory.newDurationRecord())
                            .get(uid);
                    Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder()).score(uid);
                    stringObjectMap.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
                    stringObjectMap.put("name", usersById.getNickname() == null ? "" : usersById.getNickname());
                    stringObjectMap.put("duration", duration == null ? String.valueOf(Integer.MAX_VALUE) : duration.toString());
                    resultMap.add(stringObjectMap);
                }
            }
        }
        CollectionUtils.orderMapByFields(resultMap, "score", "duration");
        return ResponseData.ok(resultMap);
    }

    @RequestMapping(value = "/getcompany", method = RequestMethod.GET)
    public ResponseData getcompanyScore() {
        logger.info("获取公司排名,getcompany");
        List<Map<String, Object>> resultMap = new ArrayList<>();
        Set<String> set = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .reverseRange(0, 200);
        for (String companyIds : set) {
            if (companyIds != null && !companyIds.equals("")) {
                Company companyById = companyRepository.findCompanyById(LangUtil.parseLong(companyIds));
                if (companyById == null) {
                    continue;
                } else {
                    Double score = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                            .score(companyById.getId().toString());
                    Double percentage = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryPercentage())
                            .score(companyIds);
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("name", companyById.getCompanyName() == null ? "" : companyById.getCompanyName());
                    map.put("score", LangUtil.parseDoubleLatterTwo(score, 0.00));
                    map.put("percentage", LangUtil.parseDoubleLatterFive(percentage, 0.00));
                    resultMap.add(map);
                }
            }
        }
      //  CollectionUtils.sort(resultMap, "score", "percentage");
        CollectionUtils.orderMapByFieldsAsc(resultMap, "score", "percentage");
        return ResponseData.ok(resultMap);
    }

    /**
     * 参数不能为空
     *
     * @param map
     * @return
     */
    private boolean checkParamterNull(Map<String, Object> map) {
        if (null == map || map.size() == 0) {
            return true;
        }
        return false;
    }

    /**
     * 返回错误参数的描述
     *
     * @return
     */
    private ResponseData parameterErrorDescription() {
        return ResponseData.fail("参数不合法,不能为空", ResponseCode.PARAM_ERROR_CODE.getCode());
    }

}
