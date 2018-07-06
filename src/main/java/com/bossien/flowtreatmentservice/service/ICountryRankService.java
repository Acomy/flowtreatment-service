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
public interface ICountryRankService {
    /**
     * 全省个人排行top20
     *
     * @param startId
     * @return
     */
    ResponseData getRangeRankings(Long startId);

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
     * 全省企业排行
     *
     * @param startId
     * @return
     */
    ResponseData getProvinceRangeRankings(Long startId);

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
     * 施工单位参赛统计
     *
     * @param startId
     * @return
     */
    ResponseData countConstructionUnit(String startId);

    /**
     * 县市水务局参赛统计
     *
     * @param startId
     * @return
     */
    ResponseData countShuiWuUnit(String startId);

    /**
     * 厅直单位排行
     *
     * @param startId
     * @return
     */
    ResponseData countTingZhiUnit(String startId);

    /**
     * 市水利局排名(2级）
     *
     * @param startId
     * @return
     */
    ResponseData getRankingSLJ(String startId);


    /**
     * 全国地(市)水利(水务)局排行TOP100 （ 2级）
     *
     * @return
     */
    ResponseData getCountryRankingSLJ();

    /**
     * 获取全国省水利部排行 直属单位排行
     *
     * @param type
     * @return
     */
    ResponseData getSLBOfCountOrderByScore(int type);

    /**
     * 查询全国的学院统计信息
     *@param companyId
     * @param currentPage
     * @param pageSize
     * @return
     */
    PageBean getAllCompanyPersonOrderBy(Long companyId ,Integer currentPage, Integer pageSize);

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

    void insertTestDb();

    CacheManager<Long, Company> getCompanyCache();

    CacheManager<String, List<Long>> getCompanyIdsCache();

    CacheManager<String, List<Long>> getCompanyIdsCacheReverse();

    List<Long> queryCompanyIdListByStarId(Long startId);

    String queryPersonOrderInCompany(String companyId) throws JsonProcessingException;

    String searchCompanyInParent(String companyName) throws IOException;

    String searchPersonInParent(String companyName) throws IOException;

    String queryCompanyInParent(String companyId) throws JsonProcessingException;

    ResponseData searchPersonRecord(String userId) throws JsonProcessingException;
}

