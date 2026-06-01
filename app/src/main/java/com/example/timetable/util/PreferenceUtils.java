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

    public static final String[] DEFAULT_PERIOD_TIMES = {
        "08:00-08:45", "08:55-09:40", "09:50-10:35", "10:45-11:30",
        "11:40-12:25", "14:00-14:45", "14:55-15:40", "15:50-16:35",
        "16:45-17:30", "17:40-18:25", "19:00-19:45", "19:55-20:40"
    };

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

    public static int getThemeColor(Context context) {
        return getPrefs(context).getInt(KEY_THEME_COLOR, 0xFF1565C0);
    }

    public static void setThemeColor(Context context, int color) {
        getPrefs(context).edit().putInt(KEY_THEME_COLOR, color).apply();
    }

    public static String[] getPeriodTimes(Context context) {
        String json = getPrefs(context).getString(KEY_PERIOD_TIMES, null);
        if (json == null) return DEFAULT_PERIOD_TIMES.clone();
        try {
            JSONArray arr = new JSONArray(json);
            String[] times = new String[12];
            for (int i = 0; i < 12; i++) {
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

    // Parse start time from period time string like "08:00-08:45"
    public static int parsePeriodStartHour(String periodTime) {
        try { return Integer.parseInt(periodTime.substring(0, 2)); } catch (Exception e) { return 8; }
    }

    public static int parsePeriodStartMinute(String periodTime) {
        try { return Integer.parseInt(periodTime.substring(3, 5)); } catch (Exception e) { return 0; }
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
