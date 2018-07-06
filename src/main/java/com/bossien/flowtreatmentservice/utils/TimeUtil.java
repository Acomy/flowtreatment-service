package com.bossien.flowtreatmentservice.utils;

import javax.xml.crypto.Data;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 时间工具类
 */
public class TimeUtil {

    public static Long getDate() {
        String s = new SimpleDateFormat("yyyyMMdd").format(new Date()).toString();
        return LangUtil.parseLong(s);
    }
    public static Long getDate(Date changeDate) {
        String s = new SimpleDateFormat("yyyyMMdd").format(changeDate).toString();
        return LangUtil.parseLong(s);
    }
    public static Date getDateBefore(Date d, int day) {
        Calendar now = Calendar.getInstance();
        now.setTime(d);
        now.set(Calendar.DATE, now.get(Calendar.DATE) - day);
        return now.getTime();
    }

    public static long get_D_Plaus_1(Calendar c) {
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 1);
        return c.getTimeInMillis();
    }

    public static List<Long> getStart_endDay(String startDate, String endDate) {
        List<Long> allDays = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            for (long d = cal.getTimeInMillis(); d <= sdf.parse(endDate).getTime(); d = get_D_Plaus_1(cal)) {
                String format = sdf.format(d);
                allDays.add(Long.parseLong(format));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return allDays;
    }

}
