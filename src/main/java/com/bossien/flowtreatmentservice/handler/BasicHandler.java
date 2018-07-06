package com.bossien.flowtreatmentservice.handler;

import com.bossien.flowtreatmentservice.dao.*;
import com.bossien.flowtreatmentservice.dao.mongo.UserExamRecordRepository;
import com.bossien.flowtreatmentservice.service.*;
import com.bossien.flowtreatmentservice.utils.LangUtil;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author gb
 */
public class BasicHandler {

    public RedisTemplate redisTemplate;

    public CompanyRepository companyRepository;

    public IProvinceRankService iProvinceRankService;

    public  ICountryRankService iCountryRankService;

    public AbstractStrategyFactory<String, List<Long>> strategyForFactory;

    public UserRepository userRepository;

    public UserExamRecordRepository userExamRecordRepository;

    public OperationSumService operationSumService;

    public BasicHandler(){
        getRedisTemplate();
        userRepository();
        iCountryRankService();
        userExamRecordRepository();
        iProvinceRankService();
        operationSumService();

    }


    public Long getDate() {
        String s = new SimpleDateFormat("yyyymmdd").format(new Date()).toString();
        return LangUtil.parseLong(s);
    }

    public RedisTemplate getRedisTemplate() {
        if (redisTemplate == null) {
            ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
            redisTemplate = (RedisTemplate) applicationContext.getBean("redisTemplate");
        }
        return redisTemplate;
    }
//
//    public StudentScoreRecordRepository studentScoreRecordRepository() {
//        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
//        StudentScoreRecordRepository studentScoreRecordRepository = (StudentScoreRecordRepository) applicationContext.getBean("studentScoreRecordRepository");
//        return studentScoreRecordRepository;
//    }


    public void iProvinceRankService() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        iProvinceRankService = applicationContext.getBean(IProvinceRankService.class);
        System.out.println("1234 ");
    }

    public AbstractStrategyFactory<String, List<Long>> strategyForFactory() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        AbstractStrategyFactory<String, List<Long>> strategyForFactory = (AbstractStrategyFactory<String, List<Long>>) applicationContext.getBean("AbstractStrategyFactory");
        return strategyForFactory;
    }

    public CompanyRepository companyRepository() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        companyRepository = (CompanyRepository) applicationContext.getBean("companyRepository");
        return companyRepository;
    }

    public TimeManager timeManager() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        TimeManager timeManager = (TimeManager) applicationContext.getBean("timeManager");
        return timeManager;
    }
    public void userRepository() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        userRepository = (UserRepository) applicationContext.getBean("userRepository");
    }
    public void iCountryRankService() {
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
         iCountryRankService = applicationContext.getBean(ICountryRankService.class);
    }
//    public void  stuTotalScoreRepository(){
//        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
//        stuTotalScoreRepository = applicationContext.getBean(StuTotalScoreRepository.class);
//    }
    public void  userExamRecordRepository(){
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
         userExamRecordRepository = applicationContext.getBean(UserExamRecordRepository.class);
    }
    public void operationSumService(){
        ApplicationContext applicationContext = ApplicationContextHelper.getApplicationContext();
        operationSumService = applicationContext.getBean(OperationSumService.class);
    }

}
