package com.bossien.flowtreatmentservice.controller;

import com.bossien.common.producer.ComputingResourceModel;
import com.bossien.common.util.QueueUtils;
import com.bossien.common.util.RedisStatusKeyFactory;
import com.bossien.flowtreatmentservice.cache.CacheManager;
import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.dao.UserRepository;
import com.bossien.flowtreatmentservice.dao.UserRoleRepository;
import com.bossien.flowtreatmentservice.dao.mongo.UserExamRecordRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import com.bossien.flowtreatmentservice.entity.Users;
import com.bossien.flowtreatmentservice.entity.mongo.UserExamRecord;
import com.bossien.flowtreatmentservice.handler.child.RecordHandler;
import com.bossien.flowtreatmentservice.service.ICountryRankService;
import com.bossien.flowtreatmentservice.service.OperationSumService;
import com.bossien.flowtreatmentservice.service.SyncMongoRecordToRedisBatchService;
import com.bossien.flowtreatmentservice.service.impl.IgnoreUnitService;
import com.bossien.flowtreatmentservice.task.CountryConvertTask;
import com.bossien.flowtreatmentservice.task.HBConvertTask;
import com.bossien.flowtreatmentservice.task.InitializationHbTask;
import com.bossien.flowtreatmentservice.task.InitializationTask;
import com.bossien.flowtreatmentservice.utils.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    CompanyRepository companyRepository;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ICountryRankService iCountryRankService;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    TimeManager timeManager;
    @Autowired
    SyncMongoRecordToRedisBatchService syncMongoRecordToRedisBatchService;

    @RequestMapping("/start")
    public String start() {
        List<Users> all = userRepository.findAll().subList(0, 1000);
        final String s2 = TimeUtil.getDate().toString();
        ConcurrencyExecutor concurrencyExecutor = ConcurrencyExecutor
                .newConcurrencyExecutor(4, 8, 0L);
        for (Users users : all) {
            int max = 100;
            int min = 10;
            Random random = new Random();
            final String date = TimeUtil.getDate().toString();
            final int score = random.nextInt(max) % (max - min + 1) + min;
            final String userId = users.getId().toString();
            final Long id = users.getCompanyId();
            if (id == null) {
                continue;
            }
            final String companyId = id.toString();
            concurrencyExecutor.run(new Runnable() {
                @Override
                public void run() {
                    ComputingResourceModel computingResourceModel = new ComputingResourceModel();
                    computingResourceModel.setScore(score);
                    computingResourceModel.setUserId(userId);
                    computingResourceModel.setCompanyId(id);
                    rabbitTemplate.convertAndSend(QueueUtils.CALCULATION_QUEUE, computingResourceModel);
                }
            });
        }
        concurrencyExecutor.safeStop(30, TimeUnit.MINUTES);
        return "ok";
    }

    //436161929904390144
    //20182601
    @GetMapping("/get")
    public String getValue() {
        List<Users> all = userRepository.findAll();
        int max = 100;
        int min = 10;
        String userId = "436455935255248897";
        String companyId = "255248896";
        Random random = new Random();
        final int score = random.nextInt(max) % (max - min + 1) + min;
        String date = TimeUtil.getDate().toString();
        testExam(userId, date, String.valueOf(score), companyId);
        return "ok";
    }

    @Autowired
    UserRoleRepository userRoleRepository;

    @GetMapping("/send")
    public String send() {
        List<Long> allByHql = userRoleRepository.findAllByHql(418061569927151616L);
        List<Users> users1 = userRepository.findIdsNotJgByhql(allByHql);
        List<Long> start_endDay = TimeUtil.getStart_endDay("20180612", "20180630");
        for (Long aLong : start_endDay) {
            Date parse = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            try {
                parse = sdf.parse(aLong.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < users1.size(); i++) {
                Users users2 = users1.get(i);
                Long id = users2.getId();
                Long companyId = users2.getCompanyId();
                for (int j = 0; j < 5; j++) {
                    ComputingResourceModel computingResourceModel = new ComputingResourceModel();
                    int max = 100;
                    int min = 10;
                    Random random = new Random();
                    int score = random.nextInt(max) % (max - min + 1) + min;
                    computingResourceModel.setScore(score);
                    computingResourceModel.setUserId(id.toString());
                    computingResourceModel.setCompanyId(companyId);
                    computingResourceModel.setDuration((long) score + 1000);
                    computingResourceModel.setCreateDate(parse);
                    computingResourceModel.setEndDate(parse);
                    computingResourceModel.setCalculationType(0);
                    rabbitTemplate.convertAndSend(QueueUtils.CALCULATION_QUEUE, computingResourceModel);
                    System.out.println("发送..");
                }
            }
        }
        return "ok";
    }

    @Autowired
    UserExamRecordRepository userExamRecordRepository;
    @Autowired
    MongoTemplate mongoTemplate;

    @GetMapping("/updataScore")
    public String updataScore() {
        List<UserExamRecord> userExamRecords = userExamRecordRepository.queryUserExamRecordsByUserIdAndCreateDay("436456121025167361", 20180522L);
        for (UserExamRecord userExamRecord : userExamRecords) {
            ComputingResourceModel computingResourceModel = new ComputingResourceModel();
            int max = 100;
            int min = 10;
            Random random = new Random();
            int score = random.nextInt(max) % (max - min + 1) + min;
            computingResourceModel.setScore(100);
            computingResourceModel.setUserId("123456");
            computingResourceModel.setCompanyId(908553216L);
            computingResourceModel.setDuration(65L);
            computingResourceModel.setCreateDate(userExamRecord.getCreateTime());
            computingResourceModel.setEndDate(new Date());
            computingResourceModel.setCalculationType(1);
            String userId = userExamRecord.getUserId();
            RecordHandler recordHandler = new RecordHandler();
            recordHandler.process(computingResourceModel);
        }

        return "ok";
    }

    @GetMapping("/company")
    public String company() {
        CacheManager<String, List<Long>> companyIdsCacheReverse = iCountryRankService.getCompanyIdsCacheReverse();
        List<Long> longs = companyIdsCacheReverse.get("index_" + 165929472);
        List<Long> longs1 = companyIdsCacheReverse.get("index_" + 823296);
        List<Long> longs2 = companyIdsCacheReverse.get("index_" + 5342208);
        System.out.println(longs.toString());
        System.out.println(longs1.toString());
        System.out.println(longs2.toString());
        return "ok";
    }

    @GetMapping("/query")
    public List<Long> query(Long id) {
        List<Long> longs = iCountryRankService.queryCompanyIdListByStarId(id);
        return longs;
    }

    @Autowired
    OperationSumService operationSumService;

    @GetMapping("/getMaxDuration")
    public String getMaxDuration() {
        Long hbMaxScoreByMongo = operationSumService.getHbMaxScoreByMongo(20180525L, 123456L);
        return hbMaxScoreByMongo + "";
    }

    @Autowired
    IgnoreUnitService ignoreUnitService;

    @GetMapping("/ig")
    public String getIg() {
        List<Long> countryIgnoreUnitIds = ignoreUnitService.getCountryIgnoreUnitIds();
        return countryIgnoreUnitIds + "";
    }

    private void testExam(String userId, String date, String score, String companyId) {
        System.out.println("开始模拟答题信息.....");
        //用户每天的排名
        redisTemplate.boundListOps(userId + date).rightPush(String.valueOf(score));
        redisTemplate.expire(userId + date, 24, TimeUnit.HOURS);
        //获取今天的考试记录
        List<String> range = redisTemplate.boundListOps(RedisStatusKeyFactory.newFractionalRecord(userId, date))
                .range(0, -1);
        int totalScore = 0;
        for (String aLong : range) {
            totalScore += Integer.parseInt(aLong);
        }
        //获取当天的平均分
        int currentDayTotal = totalScore / range.size();
        //设置当天平均分的全国排名
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder(date)).add(userId, currentDayTotal);
        //查询今天之前的所有分数的总分
        List<Long> start_endDay = TimeUtil.getStart_endDay(timeManager.getStartDay(), date);
        Double allEveryDaytotal = 0.00;
        List<Double> doubles = new ArrayList<>();
        for (Long aLong : start_endDay) {
            Double score1 = redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder(aLong.toString()))
                    .score(userId);
            if (score1 != null) {
                doubles.add(score1);
            }
        }
        if (doubles.size() > 10) {
            Collections.sort(doubles);
            Collections.reverse(doubles);
            doubles = doubles.subList(0, 10);
        }
        for (Double aDouble : doubles) {
            allEveryDaytotal += aDouble;
        }
        //设置全国的排名,实时计算无视天数
        redisTemplate.boundZSetOps(RedisStatusKeyFactory.newPersonCountryOrder()).add(userId, currentDayTotal + allEveryDaytotal);
        //全国答题次数
        redisTemplate.boundHashOps("commitCountInCountry")
                .increment("country", 1);
        //指定公司答题次数
        redisTemplate.boundHashOps("commitCountInCountry")
                .increment(companyId, 1);
    }

    @GetMapping("/getCompany")
    public String getParent(Long companyId) {
        List<Long> childProvince = getChildProvince(companyId);
        return childProvince.toString();
    }

    @GetMapping("/getProvinceIds")
    public List<Long> getProvinceIds() {
        //国家水利部
        List<Company> lowCountryIds = companyRepository.findByPidIs(0L);
        //三个单位
        List<Company> byPidIs = companyRepository.findByPidIs(lowCountryIds.get(0).getId());
        List<Long> parentProvinceIds = new ArrayList<>();
        for (Company company : byPidIs) {
            List<Long> longs = iCountryRankService.getCompanyIdsCache().get("index_" + company.getId());
            parentProvinceIds.addAll(longs);
        }
        return parentProvinceIds;
    }

    public List<Long> getChildProvince(Long companyId) {
        Object parentCompany = redisUtil.hget("parentCompany", String.valueOf(companyId));
        List<Long> longs = new ArrayList<>();
        if (parentCompany == null) {
            longs = new ArrayList<>();
            ArrayList<Long> longs1 = new ArrayList<>();
            longs1.add(companyId);
            recCompanyIds(longs1, longs);
            String join = StringUtils.join(longs.toArray(), ",");
            redisUtil.hset("parentCompany", String.valueOf(companyId), join);
        } else {
            String parentCompany1 = (String) parentCompany;
            String[] split = parentCompany1.split(",");
            for (String s : split) {
                longs.add(LangUtil.parseLong(s));
            }
        }
        return longs;
    }

    public void recCompanyIds(List<Long> companyIds, List<Long> longs) {
        if (companyIds != null && companyIds.size() > 0) {
            List<Company> companies = companyRepository.findCompaniesByIdIsIn(companyIds);
            List<Long> longs1 = typeCompaniesToLong(companies);
            if (longs1 != null && longs1.size() != 0) {
                longs.addAll(longs1);
                companyIds.clear();
                companyIds.addAll(longs1);
                recCompanyIds(companyIds, longs);
            }
        }
    }

    private List<Long> typeCompaniesToLong(List<Company> companyIds) {
        List<Long> longs = new ArrayList<>();
        for (Company companyId : companyIds) {
            longs.add(companyId.getPid());
        }
        return longs;
    }

    @GetMapping("/del")
    public String delRedis() {
        Set<String> keys = new HashSet<>();
        keys.add("totalScores");
        keys.add(RedisStatusKeyFactory.newCompanyCountryOrder());
        keys.add(RedisStatusKeyFactory.newCompanyCountryPercentage());
        keys.add(RedisStatusKeyFactory.newCompanyCountryNumber());
        keys.add("unitsParticipatingNumber");
        keys.add("companyPersonCount");

        Set<String> companyScoreKeys = redisTemplate.keys("companyScore" + "*");
        keys.addAll(companyScoreKeys);

        Set<String> personMechanismOrderKeys = redisTemplate.keys("personMechanismOrder" + "*");
        keys.addAll(personMechanismOrderKeys);

        Set<String> personProvinceOrderKeys = redisTemplate.keys("personProvinceOrder" + "*");
        keys.addAll(personProvinceOrderKeys);

        Set<String> PersonCityOrderKeys = redisTemplate.keys("PersonCityOrder" + "*");
        keys.addAll(PersonCityOrderKeys);

        Set<String> personCityAreaOrderKeys = redisTemplate.keys("personCityAreaOrder" + "*");
        keys.addAll(personCityAreaOrderKeys);

        Set<String> CompanyMechanismOrderKeys = redisTemplate.keys("CompanyMechanismOrder" + "*");
        keys.addAll(CompanyMechanismOrderKeys);

        Set<String> companyProvinceOrderKeys = redisTemplate.keys("companyProvinceOrder" + "*");
        keys.addAll(companyProvinceOrderKeys);

        Set<String>companyCityOrderKeys = redisTemplate.keys("companyCityOrder" + "*");
        keys.addAll(companyCityOrderKeys);

        Set<String> companyCityAreaOrderKeys = redisTemplate.keys("companyCityAreaOrder" + "*");
        keys.addAll(companyCityAreaOrderKeys);
        for (String key : keys) {
            redisTemplate.delete(key);
        }
        return "ok";
    }

    @Autowired
    InitializationTask initializationTask;
    @Autowired
    InitializationHbTask initializationHbTask;
    @Autowired
    CountryConvertTask countryConvertTask;
    @Autowired
    HBConvertTask hbConvertTask;

    @GetMapping("/refreshData")
    public String refreshData(Integer code, Integer level) {
        if (code == 0) {
            if (level == -1) {
                boolean convert = initializationTask.convert();
                if (convert) {
                    countryConvertTask.convert();
                }
            }
            if (level == 0) {
                boolean convert = initializationTask.convert();
            }
            if (level == 1) {
                countryConvertTask.convert();
            }
            return "完成更新全国";
        } else if (code == 1) {
            if (level == -1) {
                boolean convert = initializationHbTask.convert();
                if (convert) {
                    hbConvertTask.convert();
                }
            }
            if (level == 0) {
                boolean convert = initializationHbTask.convert();
            }
            if (level == 1) {
                hbConvertTask.convert();
            }
            return "完成更新湖北";
        } else if (code == 2) {
            if (level == -1) {
                boolean convert = initializationHbTask.convert();
                if (convert) {
                    hbConvertTask.convert();
                }
            }
            if (level == 0) {
                boolean convert = initializationHbTask.convert();
            }
            if (level == 1) {
                hbConvertTask.convert();
            }
            return "完成更新湖北";
        } else {
            return "不能确定平台类型";
        }
    }

    @GetMapping("/data")
    public void getData() {
        Map<String, Users> allUsersMaps = operationSumService.getAllUsersMaps();
        Map<String, String> allCompanyMaps = operationSumService.getAllCompanyMaps();
        System.out.println(allCompanyMaps);
        System.out.println(allUsersMaps);
    }

    @GetMapping("/personCount")
    public void personCount() {
        Map<String, Integer> companyPersonInnerCount = operationSumService.getCompanyPersonInnerCount();
        Company companyPersonInnerCountById = operationSumService.getCompanyPersonInnerCountById(51235328L);
        System.out.println(companyPersonInnerCount.toString());
    }

    @GetMapping("/batch")
    public String  batch() {
        syncMongoRecordToRedisBatchService.batch();
        return "ok";
    }
    @GetMapping("/durationUpdata")
    public String durationUpdata() {
        syncMongoRecordToRedisBatchService.durationUpdata();
        return "ok";
    }
    @GetMapping("/companyUpdata")
    public String companyUpdata() {
        syncMongoRecordToRedisBatchService.updataCompany();
        return "ok";
    }

    @GetMapping("/batchHb")
    public String batchHb() {
        syncMongoRecordToRedisBatchService.batchHb();
        return "ok";
    }




}
