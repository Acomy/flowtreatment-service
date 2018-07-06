package com.bossien.flowtreatmentservice.taskSchedule;

import com.bossien.flowtreatmentservice.task.CountryConvertTask;
import com.bossien.flowtreatmentservice.task.HBConvertTask;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author
 */
@Component
public class DynamicScheduledCountryTask implements SchedulingConfigurer {

    private static final String DEFAULT_CRON = "0/5 * * * * ?";

    @Value("${platform.cron}")
    private String cron;
    @Value("${platform.code}")
    private String code;
    @Value("${platform.scheduled}")
    private String scheduled;
    @Autowired
    CountryConvertTask countryConvertTask;
    @Autowired
    HBConvertTask hbConvertTask;
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        System.out.println(cron);
        System.out.println(code);
        System.out.println(scheduled);
        if(scheduled!=null &&scheduled.trim().equals("1") ){ //需要启动
            taskRegistrar.addTriggerTask(new Runnable() {
                @Override
                public void run() {
                    if (code.equals(PlatformCode.COUNTRY)) {
                        countryConvertTask.convert();
                    } else if (code.equals(PlatformCode.HB)) {
                        hbConvertTask.convert();
                    } else if (code.equals(PlatformCode.YNJT)) {
                        hbConvertTask.convert();
                    } else {
                        System.out.println("不能确定使用规则");
                    }
                }
            }, new Trigger() {
                @Override
                public Date nextExecutionTime(TriggerContext triggerContext) {
                    // 定时任务触发，可修改定时任务的执行周期
                    CronTrigger trigger = new CronTrigger(cron);
                    Date nextExecDate = trigger.nextExecutionTime(triggerContext);
                    return nextExecDate;
                }
            });
        }else{
            System.out.println("不需要执行计算定时任务");
        }
    }

}