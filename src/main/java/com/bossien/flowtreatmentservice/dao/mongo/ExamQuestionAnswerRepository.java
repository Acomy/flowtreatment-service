package com.bossien.flowtreatmentservice.dao.mongo;

import com.bossien.flowtreatmentservice.entity.ExamQuestionAnswer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionAnswerRepository extends MongoRepository<ExamQuestionAnswer, Long> {

}
