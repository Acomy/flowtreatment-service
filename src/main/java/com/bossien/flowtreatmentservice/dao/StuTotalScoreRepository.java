package com.bossien.flowtreatmentservice.dao;

import com.bossien.flowtreatmentservice.entity.StuTotalScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * mongo的操作
 *
 * @author gb
 */
@Repository
public interface StuTotalScoreRepository extends MongoRepository<StuTotalScore, Long> {
    //查询用户当天分数之和
    List<StuTotalScore> queryStuTotalScoresByUseridAndDayIs( Long UserId,Long day);
    //查询用户所有分数
    List<StuTotalScore> queryStuTotalScoresByUserid(Long userid);

    List<StuTotalScore> queryStuTotalScoresByDepartid(Long epartid);


}
