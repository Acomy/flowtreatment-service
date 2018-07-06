package com.bossien.flowtreatmentservice.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author gb
 */
@Component
public class TimeManager {
    @Value("${platform.code}")
    private  String code ;
    @Value("${platform.startDay}")
    public String startDay;
    @Value("${platform.initializationCron}")
    public String initializationCron;
    @Value("${platform.scheduled}")
    private String scheduled;

    public String getInitializationCron() {
        return initializationCron;
    }

    public void setInitializationCron(String initializationCron) {
        this.initializationCron = initializationCron;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getScheduled() {
        return scheduled;
    }

    public void setScheduled(String scheduled) {
        this.scheduled = scheduled;
    }

    @Override
    public String toString() {
        return "TimeManager{" +
                "code='" + code + '\'' +
                ", startDay='" + startDay + '\'' +
                ", initializationCron='" + initializationCron + '\'' +
                ", scheduled='" + scheduled + '\'' +
                '}';
    }
}
