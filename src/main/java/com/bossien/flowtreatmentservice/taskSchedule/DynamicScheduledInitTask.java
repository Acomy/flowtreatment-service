package com.bossien.flowtreatmentservice.taskSchedule;

import com.bossien.flowtreatmentservice.task.InitializationHbTask;
import com.bossien.flowtreatmentservice.task.InitializationTask;
import com.bossien.flowtreatmentservice.utils.PlatformCode;
import com.bossien.flowtreatmentservice.utils.TimeManager;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DynamicScheduledInitTask implements SchedulingConfigurer {

    private static final String DEFAULT_CRON = "0/5 * * * * ?";

    @Autowired
    TimeManager timeManager;

    @Autowired
    InitializationTask initializationTask;
    @Autowired
    InitializationHbTask  initializationHbTask;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        System.out.println(timeManager.toString());
        if(timeManager.getScheduled()!=null && timeManager.getScheduled().equals("1")){
            taskRegistrar.addTriggerTask(new Runnable() {
                @Override
                public void run() {
                    if(timeManager.getCode().equals(PlatformCode.HB)){
                        initializationHbTask.convert();
                    }else if(timeManager.getCode().equals(PlatformCode.YNJT)){
                        initializationHbTask.convert();
                    }else if(timeManager.getCode().equals(PlatformCode.COUNTRY)){
                        initializationTask.convert();
                    }else{
                        System.out.println("不能确定初始化规则");
                    }

                }
            }, new Trigger() {
                @Override
                public Date nextExecutionTime(TriggerContext triggerContext) {
                    // 定时任务触发，可修改定时任务的执行周期
                    CronTrigger trigger = new CronTrigger(timeManager.getInitializationCron());
                    Date nextExecDate = trigger.nextExecutionTime(triggerContext);
                    return nextExecDate;
                }
            });
        }else{
            System.out.println("不需要启动,初始化定时任务");
        }

    }

}