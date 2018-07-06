package com.bossien.flowtreatmentservice.controller;

import com.alibaba.fastjson.JSONObject;
import com.bossien.common.base.ResponseCode;
import com.bossien.common.base.ResponseData;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.PageBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping(value = "/areaTotalScore")
public class AdminController {
    Logger logger = LoggerFactory.getLogger(TotalAreaController.class);
    @Autowired
    ICountryRankService iCountryRankService;

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
    public String getCompanyPersonOrderBy(@RequestParam(value = "companyId", required = false) String companyId, @RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        logger.info("getCompanyPersonOrderBy,个人在本单位的排名:companyId={},pageNum={},pageSize{}", companyId, pageNum, pageSize);

        Long aLong = LangUtil.parseLong(companyId);
        return JSONObject.toJSONString(iCountryRankService.getPersonRangeRankingsById(aLong, pageNum, pageSize));
    }

    /**
     * @param
     * @return
     * @throws
     * @Title: 在全国的排名(name, score, CountryRanking (排序))
     * @author:gaobo
     * @Date:20:17 2018/4/26
     */
    @RequestMapping(value = "/getAllCompanyPersonOrderBy")
    public String getAllCompanyPersonOrderBy(@RequestParam(value = "companyId", required = false) Long companyId, @RequestParam(value = "pageNum", required = false) Integer pageNum, @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        logger.info("getAllCompanyPersonOrderBy,个人在全国的排名:pageNum={},pageSize{}", pageNum, pageSize);
        return JSONObject.toJSONString(iCountryRankService.getAllCompanyPersonOrderBy(companyId, pageNum, pageSize));
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
    public String getCompanyRangeRankingsById(@RequestParam(value = "companyId", required = false) String companyId,
                                              @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                              @RequestParam(value = "pageSize", required = false) Integer pageSize) {

        logger.info("getCompanyRangeRankingsById,企业排名:pageNum={},pageSize{}", pageNum, pageSize);
        return JSONObject.toJSONString(iCountryRankService.getCompanyRangeRankingsById(Long.valueOf(companyId), pageNum, pageSize));
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
        return iCountryRankService.getCountOfCompany(companyId);
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
        return iCountryRankService.getCountOfStudent(companyId);
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
            return iCountryRankService.queryPersonOrderInCompany(companyId);
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
            return iCountryRankService.queryCompanyInParent(companyId);
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
            return iCountryRankService.searchPersonInParent(companyName);
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
            return iCountryRankService.searchCompanyInParent(companyName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
    public String searchPersonRecord(@RequestParam(value = "userId") String userId) throws JsonProcessingException {
        logger.info("查询个人记录 userId={}", userId);
        if(userId==null){
          return  JSONObject.toJSONString(ResponseData.fail("userId is null"));
        }
        return JSONObject.toJSONString(iCountryRankService.searchPersonRecord(userId));

    }

    @GetMapping("/insert")
    public void insert() {
        iCountryRankService.insertTestDb();
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
