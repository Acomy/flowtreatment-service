package com.bossien.flowtreatmentservice.dao;

import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.entity.mongo.UsersCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<Users,Long> {
    /**
     * 查询用户信息通过公司id集合
     * @param companies
     * @return
     */
    List<Users> findAllByCompanyIdIn(List<Long> companies);


    @Query("select new Users(u.id) from Users u where u.companyId in :companies and u.state=1")
    List<Users> queryByHql(@Param("companies") List<Long> companies);
    /**
     * 查询用户的Id和公司I
     * @return
     */
    @Query("select new Users(u.id ,u.companyId) from Users u where u.state=1")
    List<Users> findIdAndCompanyIdByHql();

    /**
     * 查询用户的Id和公司I
     * @return
     */
    @Query("select new Users(u.id ,u.companyId) from Users u where u.id in :userIds  and u.state=1")
    List<Users> findIdsNotJgByhql(@Param("userIds") List<Long> userIds);

    Long countAllByCompanyIdIsIn(List<Long>companies);


    @Query("select new Users (count(u.id),u.companyId) from Users u group  by u.companyId")
    List<Users> findCountByHql();

    @Query("select count(u.id) from Users u where u.companyId =:companyId  and u.state=1 ")
    Long countByHql(@Param("companyId") Long companyId);

    /**
     * 查询所有的用户Id
     * @return
     */
    @Query("select u.id from Users u where u.state=1 ")
    List<Long> findIdsByHql();

    @Query("select u.id from Users u where  u.companyId not in :companyIds  and u.state=1  ")
    List<Long> findCompanyByhql(@Param("companyIds") List<Long> companyIds);


    /**
     * 查询所有的用户Id集合
     * @return
     */
    @Query("select u.id from Users u where u.companyId=:companyId and u.state=1")
    List<Long> findIdsByCompanyIdHql(@Param("companyId") Long companyId);

    @Query("select u from Users u where u.id =:id and u.state=1")
    Users findUsersById(@Param("id") Long id);
    /**
     * 查询指定uid的用户信息
     * @param uids
     * @return
     */
    List<Users> queryUsersByIdIsIn(Set<Long> uids);

    /**
     * 查询用户的Id和公司I
     * @return
     */
    @Query("select new Users(u.id ,u.nickname,u.telephone,u.companyId) from Users u where u.state=1 and u.id in :userIds")
    List<Users> findUserInfoByHql(@Param("userIds") List<Long> userIds);


}
