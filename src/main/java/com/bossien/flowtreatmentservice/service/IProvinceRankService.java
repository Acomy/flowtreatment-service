package com.bossien.flowtreatmentservice.service;

import com.bossien.common.base.ResponseData;
import com.bossien.flowtreatmentservice.cache.CacheManager;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.utils.PageBean;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.List;


/**
 * 省范围排名
 *
 * @author gb
 */
public interface IProvinceRankService {


    CacheManager<Long, Company> getCompanyCache();

    CacheManager<String, List<Long>> getCompanyIdsCache();

    List<Long> queryCompanyIdListByStarId(Long startId);

    CacheManager<String, List<Long>> getCompanyIdsCacheReverse();


    ResponseData getCityOrderByScore();


    ResponseData getCityOrderByTypeAndScore(int type);

    ResponseData getCityNextLowerLevel();

    ResponseData getCityNextLowerLevelByCityId(Long cityId);

    ResponseData getPersonByCityId(Long cityId);

    ResponseData getCompanyByCityId(Long cityId);


    /**
     * 根据公司Id 查询下面个人的排名
     *
     * @param provinceId
     * @param currentPage
     * @param pageSize
     * @return
     */
    PageBean getPersonRangeRankingsById(Long provinceId, int currentPage, int pageSize);


    /**
     * 查询全国的学院统计信息
     *
     * @param companyId
     * @param currentPage
     * @param pageSize
     * @return
     */
    PageBean getAllCompanyPersonOrderBy(Long companyId, Integer currentPage, Integer pageSize);


    /**
     * 查询指定的公司排名在指定的公司下
     *
     * @param startId
     * @param currentPage
     * @param pageSize
     * @return
     */
    PageBean getCompanyRangeRankingsById(Long startId, int currentPage, int pageSize);

    /**
     * 查询全国的单位统计信息
     *
     * @param companyId
     * @return
     */
    ResponseData getCountOfCompany(String companyId);

    /**
     * 查询全国学院的统计信息
     *
     * @param companyId
     * @return
     */
    ResponseData getCountOfStudent(String companyId);

    /**
     * 查询个人在公司的排名
     *
     * @param companyId
     * @return
     * @throws JsonProcessingException
     */
    String queryPersonOrderInCompany(String companyId) throws JsonProcessingException;

    /**
     * 查询指定公司下的公司排名
     *
     * @param companyId
     * @return
     * @throws JsonProcessingException
     */
    String queryCompanyInParent(String companyId) throws JsonProcessingException;

    /**
     * 搜索公司在指定公司的排名
     *
     * @param companyName
     * @return
     * @throws IOException
     */
    String searchCompanyInParent(String companyName) throws IOException;

    /**
     * 模糊搜索公司在指定的公司下的人数
     *
     * @param companyName
     * @return
     * @throws IOException
     */
    String searchPersonInParent(String companyName) throws IOException;


    ResponseData searchPersonRecord(String userId);
}

