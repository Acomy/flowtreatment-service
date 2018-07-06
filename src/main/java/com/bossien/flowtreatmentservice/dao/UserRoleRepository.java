package com.bossien.flowtreatmentservice.dao;

import com.bossien.flowtreatmentservice.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole,Long> {

    @Query("select co.userId  from UserRole co where co.roleId = :roleId and co.userId in :userIds  ")
     List<Long> findIdsByHql(@Param("userIds") List<Long> userIds , @Param("roleId") Long roleId);


    @Query("select co.userId  from UserRole co where co.roleId = :roleId ")
    List<Long> findAllByHql( @Param("roleId") Long roleId);
}
