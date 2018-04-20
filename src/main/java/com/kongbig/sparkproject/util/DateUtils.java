package com.kongbig.sparkproject.util;

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Describe: 日期时间工具类
 * Author:   kongbig
 * Data:     2018/1/30.
 */
public class DateUtils {

    private static final Logger LOGGER = Logger.getLogger(DateUtils.class);

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATEKEY_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /**
     * 判断一个时间是否在另一个时间之前
     *
     * @param time1 第一个时间
     * @param time2 第二个时间
     * @return 判断结果
     */
    public static boolean before(String time1, String time2) {
        try {
            Date dateTime1 = TIME_FORMAT.parse(time1);
            Date dateTime2 = TIME_FORMAT.parse(time2);

            if (dateTime1.before(dateTime2)) {
                return true;
            }
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 判断一个时间是否在另一个时间之后
     *
     * @param time1 第一个时间
     * @param time2 第二个时间
     * @return 判断结果
     */
    public static boolean after(String time1, String time2) {
        try {
            Date dateTime1 = TIME_FORMAT.parse(time1);
            Date dateTime2 = TIME_FORMAT.parse(time2);

            if (dateTime1.after(dateTime2)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 计算时间差值（单位为秒）
     *
     * @param time1 时间1
     * @param time2 时间2
     * @return 差值
     */
    public static int minus(String time1, String time2) {
        try {
            Date dateTime1 = TIME_FORMAT.parse(time1);
            Date dateTime2 = TIME_FORMAT.parse(time2);

            long millisecond = dateTime1.getTime() - dateTime2.getTime();

            return Integer.valueOf(String.valueOf(millisecond / 1000));
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return 0;
    }

    /**
     * 获取年月日和小时
     *
     * @param datetime yyyy-MM-dd HH:mm:ss
     * @return yyyy-MM-dd_HH
     */
    public static String getDateHour(String datetime) {
        String date = datetime.split(" ")[0];
        String hourMinuteSecond = datetime.split(" ")[1];
        String hour = hourMinuteSecond.split(":")[0];
        return date + "_" + hour;
    }

    /**
     * 获取当天日期
     *
     * @return yyyy-MM-dd
     */
    public static String getTodayDate() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * 获取昨天的日期
     *
     * @return yyyy-MM-dd
     */
    public static String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, -1);

        Date date = cal.getTime();

        return DATE_FORMAT.format(date);
    }

    /**
     * 格式化日期
     *
     * @param date Date对象
     * @return yyyy-MM-dd
     */
    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    /**
     * 格式化时间
     *
     * @param date Date对象
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String formatTime(Date date) {
        return TIME_FORMAT.format(date);
    }

    /**
     * 解析时间字符串
     *
     * @param time 时间字符串
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static Date parseTime(String time) {
        try {
            return TIME_FORMAT.parse(time);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 格式化日期key
     *
     * @param date Date
     * @return yyyyMMdd
     */
    public static String formatDateKey(Date date) {
        return DATEKEY_FORMAT.format(date);
    }

    /**
     * 将yyyyMMdd转换成Date格式
     *
     * @param dateKey yyyyMMdd
     * @return Date
     */
    public static Date parseDateKey(String dateKey) {
        try {
            return DATEKEY_FORMAT.parse(dateKey);
        } catch (ParseException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
