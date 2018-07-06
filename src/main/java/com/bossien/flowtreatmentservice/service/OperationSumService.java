package com.bossien.flowtreatmentservice.service;

import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.entity.mongo.RankingRecord;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.List;
import java.util.Map;

public interface OperationSumService {

    List<Integer> getRecordByMongo(Long dateLong, Long userIdLong);

    List<Integer> getAverageRecordByMongo(Long userIdLong);


    Long  getHbMaxScoreByMongo(Long dateLong, Long userIdLong);

    Long  getYNAverageSeekTime(Long userIdLong);

    List<Integer> getRecordByMongoFast(Long dateLong, Long userIdLong);

    List<Integer> getRecordByMongoAllRecord(Long userIdLong);

    List<UserExamRecord> getRecordByMongoAllRecordAndDuration(Long userIdLong);

    List<String> getRecordByRedis(int  calculationType, String userId, String date, String score);

    List<Double>  getRecordByRedisAllDays(Long userIdLong);

    Long getHbDurationEveryDayRecordTotal(Long userIdLong);

    UserExamRecord saveUserExamRecord(UserExamRecord userExamRecord);

    UserExamRecord updateUserExamRecord(UserExamRecord userExamRecord);

    RankingRecord updateRankingRecord(RankingRecord rankingRecord);

    void resetCompanyRankInParentCompany();

    void resetHbCompanyRankInParentCompany();

    void saveCompanyAndUserRank(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore);

    void saveCompanyRankInParentCompany(String companyId, Double companyTotalScore);

    void saveCompanyAndUserRankBatch(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore);

    void saveCompanyAndUserRankBatchAndRedisConn(RedisConnection redisConnection, String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore);

    void saveCompanyRankInParentCompanyBatch(String companyId, Double companyTotalScore);

    void saveUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal);

    void saveUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal, boolean saveToCountry);

    void saveDurationUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal);

    void  saveDurationHBUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal);

    void saveUserRankInCompanyBatch(String userId, String companyId, Double personAllEveryDayTotal);

    void saveHbCompanyAndUserRank(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore);

    void saveHbCompanyRankInParentCompany(String companyId, Double companyTotalScore);

    void saveHbUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal);

    void saveHbUserRankInCompany(String userId, String companyId, Double personAllEveryDayTotal, boolean saveToCountry);

    void saveHbUserRankInCompanyBatch(String userId, String companyId, Double personAllEveryDayTotal);

    void saveHbCompanyRankInParentCompanyBatch(String companyId, Double companyTotalScore);

    void saveHbCompanyAndUserRankBatch(String userId, String companyId, Double personAllEveryDayTotal, Double companyTotalScore);

    Double searchCompanyScoreByIdFromRedis(String companyId);

    Boolean saveUserCurrentDayTotalScoreToCompanyScore(String userId, String companyId, Double personAllEveryDayTotal);

    Double calculatingCountryCurrentTotalScore(List<Double> personScoreDoubles);

    Double calculatingProvinceCurrentTotalScore(List<Double> personScoreDoubles);

    void updateUserExamAvailable(String userId, String companyId);

    void updateHbUserExamAvailable(String userId, String companyId);

    void updateHbUserExamAvailable(Map<String, Long> userIdAndCompanyIdMaps);

    void updateUserAllExamAvailable(Map<String, Long> userIdAndCompanyIdMaps);

    Double getCurrentDayTotalScore(String userId, List<Integer> range);

    Double getHbCurrentDayTotalScore(String userId, List<Integer> range);

    Double getYnCurrentDayTotalScore(String userId, List<Integer> range);

    boolean saveSumCurrentDayTotalToRedis(String userId, String date, Double currentDayTotal);

    boolean refreshCompany();

    List<Long> queryStudentInUsers();

    Map<String, Long> queryUserIdAndCompanyIdsAll();

    Map<String, Long> queryUserIdAndCompanyIds(List<Long> userIds);

    Map<String, Integer> getCompanyPersonInnerCount();

    Company getCompanyPersonInnerCountById(Long companyId);

    String getPlatformCode();

    Map<String,Users> getAllUsersMaps();

    Map<String,String> getAllCompanyMaps();

    Map<String,Integer> getAllCompanyCount();

    int getPersonCount(Long companyId);
}
