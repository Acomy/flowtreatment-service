package com.bossien.flowtreatmentservice.dao.mongo;

import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserExamRecordRepository extends MongoRepository<UserExamRecord, Long> {


    List<UserExamRecord> queryUserExamRecordsByUserIdAndCreateDay(String userId, Long createDay);

    List<Integer> findScoresByUserIdAndCreateDay(String userId, Long createDay);

    List<UserExamRecord> queryUserExamRecordsByUserId(String userId);

    Long countDistinctByUserId(String userId);

 //   List<UserExamRecord> queryUserExamRecordsByUserIdAndAndOrderByCreateTime(Long userId);
}
