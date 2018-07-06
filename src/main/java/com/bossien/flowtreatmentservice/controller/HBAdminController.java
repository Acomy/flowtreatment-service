package com.bossien.flowtreatmentservice.controller;

import com.alibaba.fastjson.JSONObject;
import com.bossien.common.base.ResponseCode;
import com.bossien.common.base.ResponseData;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.service.IProvinceRankService;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.PageBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping(value = "/hbWaterAdmin")
public class HBAdminController {
    Logger logger = LoggerFactory.getLogger(TotalAreaController.class);
    @Autowired
    IProvinceRankService iProvinceRankService;
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * @param
     * @return
     * @throws
     * @Title: 在本单位的排名(name, score, companyRanking ( 默认排序), CountryRanking)
     * @Description:
     * @author:gaobo
     * @Date:15:18 2018/4/26
     */
    @RequestMapping(value = "/getCompanyPersonOrderBy")
    public PageBean getCompanyPersonOrderBy(@RequestParam(value = "companyId", required = false) String companyId, @RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        logger.info("getCompanyPersonOrderBy,个人在本单位的排名:companyId={},pageNum={},pageSize{}", companyId, pageNum, pageSize);
        PageBean pageBean = new PageBean();
        if (companyId == null) {
            pageBean.setMessage("参数为空!!!");
            return pageBean;
        }
        Long aLong = LangUtil.parseLong(companyId);
        return iProvinceRankService.getPersonRangeRankingsById(aLong, pageNum, pageSize);
    }

    /**
     * @param
     * @return
     * @throws
     * @Title: 在全国的排名(name, score, CountryRanking ( 排序))
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/getAllCompanyPersonOrderBy")
    public PageBean getAllCompanyPersonOrderBy(@RequestParam(value = "companyId", required = false) Long companyId, @RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        logger.info("getAllCompanyPersonOrderBy,个人在全国的排名:pageNum={},pageSize{}", pageNum, pageSize);
        return iProvinceRankService.getAllCompanyPersonOrderBy(companyId, pageNum, pageSize);
    }

    /**
     * @param
     * @return
     * @throws
     * @Title: 企业排名
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/getCompanyRangeRankingsById")
    public PageBean getCompanyRangeRankingsById(@RequestParam(value = "companyId", required = false) String companyId,
                                                @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Long aLong = null;
        if (companyId != null) {
            aLong = LangUtil.parseLong(companyId);
        }
        logger.info("getCompanyRangeRankingsById,企业排名:pageNum={},pageSize{}", pageNum, pageSize);
        return iProvinceRankService.getCompanyRangeRankingsById(aLong, pageNum, pageSize);
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:获取单位统计数量
     * @author:gaobo
     * @Date:20:16 2018/4/26
     */
    @RequestMapping(value = "/getCountOfCompany")
    public ResponseData getCountOfCompany(@RequestParam(required = false) String companyId) {
        logger.info("getCountOfCompany,获取单位统计数量:companyId={}", companyId);
        return iProvinceRankService.getCountOfCompany(companyId);
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:获取学员统计数量
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/getCountOfStudent")
    public ResponseData getCountOfStudent(@RequestParam(required = false) String companyId) {
        logger.info("getCountOfStudent,获取学员统计数量:companyId={}", companyId);
        return iProvinceRankService.getCountOfStudent(companyId);
    }


    /**
     * @param
     * @return
     * @throws
     * @Title:指定单位下所有人
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/queryPersonOrderInCompany")
    public String queryPersonInCompany(@RequestParam(required = false) String companyId) {
        logger.info("getCountOfStudent,获取学员统计数量:companyId={}", companyId);
        try {
            return iProvinceRankService.queryPersonOrderInCompany(companyId);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:指定单位下所有人
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/queryCompanyInParent")
    public String queryCompanyInParent(@RequestParam(required = false) String companyId) {
        logger.info("getCountOfStudent,获取学员统计数量:companyId={}", companyId);
        try {
            return iProvinceRankService.queryCompanyInParent(companyId);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:搜索指定监管单位下的人员信息
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/searchPersonInParent")
    public String searchPersonInParent(@RequestParam(required = false) String companyName) {
        logger.info("searchCompanyInParent,获取学员统计数量:companyName={}", companyName);
        try {
            return iProvinceRankService.searchPersonInParent(companyName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:搜索公司信息下的公司信息
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/searchCompanyInParent")
    public String searchCompanyInParent(@RequestParam(required = false) String companyName) {
        logger.info("searchCompanyInParent,获取学员统计数量:companyName={}", companyName);
        try {
            return iProvinceRankService.searchCompanyInParent(companyName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param
     * @return
     * @throws
     * @Title:搜索公司信息下的公司信息
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/getCountryTotal")
    public ResponseData getCountryTotal() {
        logger.info("getCountryTotal,获取全国统计");
        Map<String, Object> resultMap = new HashMap<>();
        Long examPersonCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonExamAvailable())
                .zCard();
        resultMap.put("examPersonCount", examPersonCount);
        Long examCompanyCount = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newCompanyCountryOrder())
                .zCard();
        resultMap.put("examCompanyCount", examCompanyCount);
        Object examCount = redisTemplate.boundHashOps("commitCountInCountry")
                .get("country");
        if (examCount == null) {
            examCount = 0;
        }
        resultMap.put("examCount", examCount);
        return ResponseData.ok(resultMap);
    }
    /**
     * @param
     * @return
     * @throws
     * @Title:查询个人记录
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/searchPersonRecord")
    public String searchPersonRecord(String userId) throws JsonProcessingException {
        logger.info("查询个人记录 userId={}", userId);
        if(userId==null){
            return  JSONObject.toJSONString(ResponseData.fail("userId is null"));
        }
        return JSONObject.toJSONString(iProvinceRankService.searchPersonRecord(userId));

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
