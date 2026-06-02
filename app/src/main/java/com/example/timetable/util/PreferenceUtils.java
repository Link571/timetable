package com.example.timetable.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

public class PreferenceUtils {

    private static final String PREFS_NAME = "timetable_prefs";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_MINUTES = "reminder_minutes";
    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_PERIOD_TIMES = "period_times";
    private static final String KEY_WEEK_MODE = "week_mode";
    private static final String KEY_MORNING_COUNT = "morning_count";
    private static final String KEY_AFTERNOON_COUNT = "afternoon_count";
    private static final String KEY_EVENING_COUNT = "evening_count";

    public static final int WEEK_MODE_NORMAL = 0;  // 周一至周五
    public static final int WEEK_MODE_WEEKEND = 1; // 周一至周日

    // 默认节次时间（12节，作为初始值和回退值）
    public static final String[] DEFAULT_PERIOD_TIMES = {
        "08:00-08:45", "08:55-09:40", "09:50-10:35", "10:45-11:30",
        "11:40-12:25", "14:00-14:45", "14:55-15:40", "15:50-16:35",
        "16:45-17:30", "17:40-18:25", "19:00-19:45", "19:55-20:40"
    };

    // --- 节次数量管理 ---

    /** 上午课程节数，默认 5 */
    public static int getMorningCount(Context context) {
        return getPrefs(context).getInt(KEY_MORNING_COUNT, 5);
    }

    public static void setMorningCount(Context context, int count) {
        getPrefs(context).edit().putInt(KEY_MORNING_COUNT, count).apply();
    }

    /** 下午课程节数，默认 5 */
    public static int getAfternoonCount(Context context) {
        return getPrefs(context).getInt(KEY_AFTERNOON_COUNT, 5);
    }

    public static void setAfternoonCount(Context context, int count) {
        getPrefs(context).edit().putInt(KEY_AFTERNOON_COUNT, count).apply();
    }

    /** 晚课节数，默认 2 */
    public static int getEveningCount(Context context) {
        return getPrefs(context).getInt(KEY_EVENING_COUNT, 2);
    }

    public static void setEveningCount(Context context, int count) {
        getPrefs(context).edit().putInt(KEY_EVENING_COUNT, count).apply();
    }

    /** 总节次数 = 上午 + 下午 + 晚课 */
    public static int getTotalPeriodCount(Context context) {
        return getMorningCount(context) + getAfternoonCount(context) + getEveningCount(context);
    }

    /**
     * 根据节次编号返回所属时段
     * @param period 节次编号（从 1 开始）
     * @return "上午" / "下午" / "晚课"
     */
    public static String getPeriodSection(Context context, int period) {
        int morning = getMorningCount(context);
        int afternoon = getAfternoonCount(context);
        if (period <= morning) return "上午";
        if (period <= morning + afternoon) return "下午";
        return "晚课";
    }

    /**
     * 当节数变更时，调整节次时间数组大小
     * 增加节次：末尾追加默认时间
     * 减少节次：从末尾截断
     */
    public static void adjustPeriodTimesForNewCount(Context context, int newTotal) {
        String[] current = getPeriodTimes(context);
        String[] adjusted = new String[newTotal];
        for (int i = 0; i < newTotal; i++) {
            if (i < current.length) {
                adjusted[i] = current[i];
            } else {
                // 追加节次使用默认时间
                adjusted[i] = (i < DEFAULT_PERIOD_TIMES.length)
                    ? DEFAULT_PERIOD_TIMES[i]
                    : String.format("%02d:00-%02d:45", 8 + i, 8 + i);
            }
        }
        setPeriodTimes(context, adjusted);
    }

    // --- 提醒设置 ---

    public static boolean isReminderEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_REMINDER_ENABLED, false);
    }

    public static void setReminderEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public static int getReminderMinutes(Context context) {
        return getPrefs(context).getInt(KEY_REMINDER_MINUTES, 15);
    }

    public static void setReminderMinutes(Context context, int minutes) {
        getPrefs(context).edit().putInt(KEY_REMINDER_MINUTES, minutes).apply();
    }

    // --- 主题颜色 ---

    public static int getThemeColor(Context context) {
        return getPrefs(context).getInt(KEY_THEME_COLOR, 0xFF1565C0);
    }

    public static void setThemeColor(Context context, int color) {
        getPrefs(context).edit().putInt(KEY_THEME_COLOR, color).apply();
    }

    // --- 节次时间（动态长度） ---

    /**
     * 获取节次时间数组，长度 = 总节次数
     * 若未存储过，则返回默认 12 节时间
     */
    public static String[] getPeriodTimes(Context context) {
        String json = getPrefs(context).getString(KEY_PERIOD_TIMES, null);
        if (json == null) return DEFAULT_PERIOD_TIMES.clone();
        try {
            JSONArray arr = new JSONArray(json);
            int len = arr.length();
            String[] times = new String[len];
            for (int i = 0; i < len; i++) {
                times[i] = arr.getString(i);
            }
            return times;
        } catch (JSONException e) {
            return DEFAULT_PERIOD_TIMES.clone();
        }
    }

    public static void setPeriodTimes(Context context, String[] times) {
        JSONArray arr = new JSONArray();
        for (String t : times) arr.put(t);
        getPrefs(context).edit().putString(KEY_PERIOD_TIMES, arr.toString()).apply();
    }

    // --- 显示模式 ---

    public static int getWeekMode(Context context) {
        return getPrefs(context).getInt(KEY_WEEK_MODE, WEEK_MODE_NORMAL);
    }

    public static void setWeekMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_WEEK_MODE, mode).apply();
    }

    // --- 时间解析工具 ---

    /** 从 "08:00-08:45" 格式解析开始小时 */
    public static int parsePeriodStartHour(String periodTime) {
        try { return Integer.parseInt(periodTime.substring(0, 2)); } catch (Exception e) { return 8; }
    }

    /** 从 "08:00-08:45" 格式解析开始分钟 */
    public static int parsePeriodStartMinute(String periodTime) {
        try { return Integer.parseInt(periodTime.substring(3, 5)); } catch (Exception e) { return 0; }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
