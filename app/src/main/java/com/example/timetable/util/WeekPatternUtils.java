package com.example.timetable.util;

import android.content.Context;

import com.example.timetable.data.model.Semester;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WeekPatternUtils {

    public static final String PRESET_ALL = "all";
    public static final String PRESET_ODD = "odd";
    public static final String PRESET_EVEN = "even";
    public static final String PRESET_CUSTOM = "custom";

    public static List<Integer> generatePattern(int totalWeeks, String presetType, List<Integer> customWeeks) {
        List<Integer> result = new ArrayList<>();
        switch (presetType) {
            case PRESET_ALL:
                for (int i = 1; i <= totalWeeks; i++) result.add(i);
                break;
            case PRESET_ODD:
                for (int i = 1; i <= totalWeeks; i++) {
                    if (i % 2 == 1) result.add(i);
                }
                break;
            case PRESET_EVEN:
                for (int i = 1; i <= totalWeeks; i++) {
                    if (i % 2 == 0) result.add(i);
                }
                break;
            case PRESET_CUSTOM:
                if (customWeeks != null) result.addAll(customWeeks);
                break;
        }
        return result;
    }

    public static boolean isActiveInWeek(List<Integer> weekPattern, int currentWeek) {
        return weekPattern != null && weekPattern.contains(currentWeek);
    }

    public static String formatForDisplay(List<Integer> weeks, int totalWeeks) {
        if (weeks == null || weeks.isEmpty()) return "无";
        List<Integer> all = generatePattern(totalWeeks, PRESET_ALL, null);
        List<Integer> odd = generatePattern(totalWeeks, PRESET_ODD, null);
        List<Integer> even = generatePattern(totalWeeks, PRESET_EVEN, null);

        if (weeks.equals(all)) return "1-" + totalWeeks + "周 每周";
        if (weeks.equals(odd)) return "1-" + totalWeeks + "周 单周";
        if (weeks.equals(even)) return "1-" + totalWeeks + "周 双周";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weeks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(weeks.get(i));
        }
        sb.append("周");
        return sb.toString();
    }

    public static String getDayName(int dayOfWeek) {
        String[] days = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return (dayOfWeek >= 1 && dayOfWeek <= 7) ? days[dayOfWeek] : "";
    }

    public static String getPeriodTime(int period) {
        String[] times = {
            "", "08:00-08:45", "08:55-09:40", "09:50-10:35", "10:45-11:30",
            "11:40-12:25", "14:00-14:45", "14:55-15:40", "15:50-16:35",
            "16:45-17:30", "17:40-18:25", "19:00-19:45", "19:55-20:40"
        };
        return (period >= 1 && period <= 12) ? times[period] : "";
    }

    public static int getPeriodStartHour(int period) {
        int[] hours = {0, 8, 8, 9, 10, 11, 14, 14, 15, 16, 17, 19, 19};
        return (period >= 1 && period <= 12) ? hours[period] : 8;
    }

    public static int getPeriodStartMinute(int period) {
        int[] minutes = {0, 0, 55, 50, 45, 40, 0, 55, 50, 45, 40, 0, 55};
        return (period >= 1 && period <= 12) ? minutes[period] : 0;
    }

    // Context-aware versions for dynamic period times
    public static String getPeriodTime(Context context, int period) {
        if (context == null) return getPeriodTime(period);
        String[] times = PreferenceUtils.getPeriodTimes(context);
        return (period >= 1 && period <= 12) ? times[period - 1] : "";
    }

    public static int getPeriodStartHour(Context context, int period) {
        if (context == null) return getPeriodStartHour(period);
        String[] times = PreferenceUtils.getPeriodTimes(context);
        if (period >= 1 && period <= 12) {
            return PreferenceUtils.parsePeriodStartHour(times[period - 1]);
        }
        return 8;
    }

    public static int getPeriodStartMinute(Context context, int period) {
        if (context == null) return getPeriodStartMinute(period);
        String[] times = PreferenceUtils.getPeriodTimes(context);
        if (period >= 1 && period <= 12) {
            return PreferenceUtils.parsePeriodStartMinute(times[period - 1]);
        }
        return 0;
    }

    // Calculate actual date for a day of week in a given week of the semester
    // Returns formatted string like "6月2日 周一"
    public static String getDateForDay(Semester semester, int week, int dayOfWeek) {
        if (semester == null || week < 1 || dayOfWeek < 1 || dayOfWeek > 7) return getDayName(dayOfWeek);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(semester.getStartDate());
        cal.add(Calendar.WEEK_OF_YEAR, week - 1);
        cal.add(Calendar.DAY_OF_MONTH, dayOfWeek - 1);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return month + "月" + day + "日 " + getDayName(dayOfWeek);
    }
}
