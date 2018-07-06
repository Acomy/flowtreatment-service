//package com.bossien.flowtreatmentservice.dao;
//
//import com.bossien.flowtreatmentservice.entity.StuScoreStatistics;
//import com.bossien.flowtreatmentservice.entity.StuTotalScore;
//import org.springframework.data.mongodb.repository.MongoRepository;
//
//import java.util.List;
//
//public interface StuScoreStatisticsRepository  extends MongoRepository<StuScoreStatistics, Long> {
//    /**
//     * 查询StuTotalScores的统计列表通过用户Ids
//     *
//     * @param userIds
//     * @return
//     */
//    List<StuScoreStatistics> queryStuScoreStatisticsByUserIdIsIn(List<Long> userIds);
//
//    /**
//     * 查询StuTotalScores的统计列表通过用户Ids
//     *
//     * @param userIds
//     * @return
//     */
//    List<StuScoreStatistics> queryStuScoreStatisticsByUserId(Long userIds);
//
//
//
//    /**
//     * 查询StuTotalScores的统计列表通过用户Ids
//     *
//     * @param userIds
//     * @return
//     */
//    List<StuScoreStatistics> queryStuScoreStatisticsByUserIdAndC(Long userIds);
//
//    /**
//     * 查询StuTotalScores的统计列表通过用户Ids
//     *
//     * @param userIds
//     * @param time
//     * @return
//     */
//    List<StuScoreStatistics> queryStuScoreStatisticsByUserIdAndAndEndTime(Long userIds,Long time);
//
//}
