package com.bossien.flowtreatmentservice.dao.mongo;

import com.bossien.flowtreatmentservice.entity.mongo.RankingRecord;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRecordRepository extends MongoRepository<RankingRecord, Long> {
    List<RankingRecord> queryRankingRecordsByCompanyIdIsAndType(String companyId,int type);

}
