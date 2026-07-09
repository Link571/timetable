package com.example.timetable.util;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 日期计算工具类
 * 统一处理学期起始日、周次计算、最近周一等逻辑，避免代码重复
 */
public class DateUtils {

    /** 一天的毫秒数 */
    private static final long MILLIS_PER_DAY = 86400000L;

    /**
     * 将指定时间戳调整为最近的周一（向前回退）
     * 用于学期起始日期对齐
     *
     * @param timestamp 任意日期的毫秒时间戳
     * @return 该日期所在周周一的 00:00:00.000 毫秒时间戳
     */
    public static long getNearestMonday(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        // DAY_OF_WEEK: 周日=1, 周一=2, ..., 周六=7
        // (dayOfWeek + 5) % 7 = 周一→0, 周二→1, ..., 周日→6
        int daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        cal.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * 获取今天的 00:00:00（按当前时区）
     */
    public static long getTodayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * 根据学期起始日期和当前日期，自动计算当前教学周次
     * 结果钳位在 [1, totalWeeks] 范围内
     *
     * @param startDate  学期第一周周一的毫秒时间戳
     * @param totalWeeks 学期总教学周数
     * @return 计算出的当前周次（1-based）
     */
    public static int calculateCurrentWeek(long startDate, int totalWeeks) {
        long today = getTodayStart();
        long diff = today - startDate;
        // 起始周为第1周，所以 diff/7天 + 1
        int autoWeek = (int) (diff / (7L * MILLIS_PER_DAY)) + 1;
        if (autoWeek < 1) autoWeek = 1;
        if (autoWeek > totalWeeks) autoWeek = totalWeeks;
        return autoWeek;
    }

    /**
     * 计算学期中某周某天的具体日期
     * 使用精确的毫秒偏移量计算，避免 Calendar.WEEK_OF_YEAR 在跨年时的边界问题
     *
     * @param semesterStartDate 学期第一周周一的毫秒时间戳
     * @param week              周次（1-based）
     * @param dayOfWeek         星期几（1=周一, 7=周日）
     * @return 该天的 Calendar 对象（已设置时分秒为 00:00:00）
     */
    public static Calendar getDateForDay(long semesterStartDate, int week, int dayOfWeek) {
        // 从学期起始日（周一）偏移：(周数-1)*7天 + (星期几-1)天
        long dayMillis = semesterStartDate
            + (week - 1) * 7L * MILLIS_PER_DAY
            + (dayOfWeek - 1) * MILLIS_PER_DAY;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * 格式化日期为 "M/d" 格式（如 "6/3"），用于课表表头
     *
     * @param semesterStartDate 学期起始日
     * @param week              周次
     * @param dayOfWeek         星期几（1=周一）
     * @return 格式化后的日期字符串
     */
    public static String formatDateShort(long semesterStartDate, int week, int dayOfWeek) {
        Calendar cal = getDateForDay(semesterStartDate, week, dayOfWeek);
        return (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 格式化日期为 "M月d日 周X" 格式（如 "6月3日 周一"）
     * 用于课程时段弹窗等需要展示完整日期的场景
     *
     * @param semesterStartDate 学期起始日
     * @param week              周次
     * @param dayOfWeek         星期几（1=周一）
     * @return 格式化后的日期字符串
     */
    public static String formatDateFull(long semesterStartDate, int week, int dayOfWeek) {
        Calendar cal = getDateForDay(semesterStartDate, week, dayOfWeek);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String dayName = (dayOfWeek >= 1 && dayOfWeek <= 7) ? dayNames[dayOfWeek] : "";
        return month + "月" + day + "日 " + dayName;
    }

    /**
     * 将时间戳格式化为 "yyyy-MM-dd (周一)" 格式
     * 用于学期设置对话框中的起始日期显示
     *
     * @param timestamp 毫秒时间戳
     * @return 格式化后的日期字符串
     */
    public static String formatMondayDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (周一)", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * 格式化当前周次信息文字
     *
     * @param week       当前周次
     * @param totalWeeks 总周数（为0时只显示"第X周"）
     * @return 格式化的周次信息（如 "第3周 (共16周)"）
     */
    public static String formatWeekInfo(int week, int totalWeeks) {
        if (totalWeeks > 0) {
            return "第" + week + "周 (共" + totalWeeks + "周)";
        }
        return "第" + week + "周";
    }
}
