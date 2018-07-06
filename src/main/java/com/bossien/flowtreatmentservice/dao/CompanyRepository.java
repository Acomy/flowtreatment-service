package com.bossien.flowtreatmentservice.dao;

import com.bossien.flowtreatmentservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    /**
     * 查询公司信息通过pid
     *
     * @param pids
     * @return
     */
    List<Company> queryCompaniesByPidIsIn(List<Long> pids);

    /**
     * @param pids,typeId
     * @return List<Company>
     * @throws
     * @Title: queryCompaniesByPidIsInAndTAndTypeId
     * @Description:IN 查询数据并且指定type
     * @author:gaobo
     * @Date:14:25 2018/4/25
     */
    List<Company> queryCompaniesByPidIsInAndTypeId(List<Long> pids, Byte typeId);

    /**
     * 查询公司信息
     *
     * @param pids
     * @param code
     * @return
     */
    List<Company> queryCompaniesByPidIsInAndCodeIs(List<Long> pids, String code);

    /**
     * 查询pid 信息
     *
     * @return
     */
    @Query("select  co  from Company co where co.state=1 group by co.pid")
    List<Company> findByAndGroupByHql();

    /**
     * 查询pid 信息
     *
     * @return
     */
    @Query("select  co.pid  from Company co where co.state = 1  AND co.pid is not NULL group by co.pid ")
    List<Long> findPidsByHql();

    /**
     * 查询pid 信息
     *
     * @return
     */
    @Query("select  new Company (co.id,co.pid) from Company co  where co.state = 1")
    List<Company> findByHql();

    /**
     * 模糊查询公司名称 信息
     *
     * @return
     */
    @Query("select  co  from Company co where co.companyName like '%:keyword%' and co.state = 1")
    List<Company> findByHql(String keyword);

    /**
     * 查询公司信息的Id
     *
     * @return
     */
    @Query("select  co.id  from Company co where co.state = 1")
    List<Long> findIdByAndGroupByHql();

    /**
     * 查询公司信息的Id
     *
     * @return
     */
    //SELECT c.* from company c where
    @Query("select c.id from Company c where c.state=1 and c.id not in :companyIds")
    List<Long> queryByHql(@Param("companyIds") List<Long> companyIds);

    /**
     * 查询公司信息的Id
     *
     * @return
     */
    //SELECT c.* from company c where
    @Query("select c.id from Company c where c.state=1 and c.id in :companyIds")
    List<Long> queryCompaniesByHql(@Param("companyIds") List<Long> companyIds);

    /**
     * 查询公司信息的Id
     *
     * @return
     */
    //SELECT c.* from company c where
    @Query("SELECT c.id from Company c where  c.companyName like '%直管%' AND c.companyName not like'直管监管单位'")
    List<Long> queryIdsIgnoreByHql();

    /**
     * 查询pid
     *
     * @param pid
     * @return
     */
    @Query("select c from Company c where c.pid = :pid and c.state=1")
    List<Company> findByPidIs(@Param("pid") Long pid);

    /**
     * 查询pid
     *
     * @param pid
     * @return
     */
    List<Long> findIdByPid(Long pid);


    Company findCompanyById(Long id);

    Company findCompanyByCompanyName(String companyName);


    @Query("select co.id from Company co where co.companyName in :companyNames and co.state=1")
    List<Long> findIdsByHql(@Param("companyNames") List<String> companyNames);

    @Query("select co.id from Company co where co.state = 1")
    List<Long> findAllIdsByHql();

    /**
     * 查询用户的名称通过Id
     *
     * @param id
     * @return
     */
    @Query("select  co.companyName  from Company co where co.id= :id  and co.state=1")
    String findNameByHql(@Param("id") Long id);

    /**
     * 查询用户的名称通过Id
     *
     * @param id
     * @return
     */
    @Query("select co.id,co.companyName  from Company co where co.id in :id  and co.state=1")
    List<Map> findNameByIdsHql(@Param("id") Set<Long> id);

    List<Company> findCompaniesByIdIsIn(List<Long> longs);
    @Query("select new Company(co.id,co.companyName)  from Company co where co.state=1")
    List<Company> findCompaniesByHql();

    List<Company> findCompaniesByCreateDateGreaterThanEqualAndCreateDateIsNotNull(Date Date);

    /**
     * 查询公司信息的Id
     *
     * @return
     */
    @Query("select  new  Company(co.id,co.companyName,co.peopleNumber)  from Company co where co.state = 1")
    List<Company> findCompanyCountGroupByHql();

    @Query("select  new  Company(co.id,co.companyName,co.peopleNumber)  from Company co where  co.id =:id and co.state = 1")
    Company findCompanyCountGroupByHqlAndCompanyId(@Param("id") Long id);
}
