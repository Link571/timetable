package com.example.timetable.util;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;

import com.example.timetable.R;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.receiver.CourseReminderReceiver;

import java.util.List;

public class NotificationHelper {

    public static final String CHANNEL_ID = "course_reminder";
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_NAME = "上课提醒";
    /** 一天的毫秒数，用于精确偏移计算，避免 Calendar.WEEK_OF_YEAR 跨年边界问题 */
    private static final long MILLIS_PER_DAY = 86400000L;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("上课前提醒通知");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    /**
     * 为指定学期中的所有课程批量创建闹钟提醒
     * 使用毫秒偏移算法计算目标日期时间，避免 Calendar.WEEK_OF_YEAR 在跨年时的边界错误
     *
     * @param context        上下文
     * @param courses        课程列表
     * @param semester       当前活跃学期
     * @param advanceMinutes 提前多少分钟提醒
     */
    public static void scheduleAllReminders(Context context, List<Course> courses,
                                            Semester semester, int advanceMinutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        cancelAllReminders(context, courses, semester);

        long semesterStart = semester.getStartDate(); // 学期第一周周一的 00:00:00

        for (Course course : courses) {
            List<Integer> weeks = course.getWeekPattern();
            if (weeks == null || weeks.isEmpty()) continue;

            int dayOfWeek = course.getDayOfWeek(); // 1=周一 ~ 7=周日
            int startHour = WeekPatternUtils.getPeriodStartHour(context, course.getStartPeriod());
            int startMinute = WeekPatternUtils.getPeriodStartMinute(context, course.getStartPeriod());

            for (int weekNum : weeks) {
                // 精确计算目标日期的毫秒时间戳（避免 Calendar.WEEK_OF_YEAR 跨年问题）
                // offsetDays = (周数-1)*7天 + (星期几-1)天
                long dayMillis = semesterStart
                    + (weekNum - 1) * 7L * MILLIS_PER_DAY
                    + (dayOfWeek - 1) * MILLIS_PER_DAY
                    + startHour * 3600000L
                    + startMinute * 60000L
                    - advanceMinutes * 60000L;

                // 仅安排未来时间的闹钟
                if (dayMillis <= System.currentTimeMillis()) continue;

                Intent intent = new Intent(context, CourseReminderReceiver.class);
                intent.putExtra("course_name", course.getName());
                intent.putExtra("classroom", course.getClassroom());
                intent.putExtra("period_number", course.getStartPeriod());
                intent.putExtra("day_of_week", dayOfWeek);

                int requestCode = course.getId() * 1000 + weekNum;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                try {
                    if (canScheduleExactAlarms(alarmManager)) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            dayMillis,
                            pendingIntent);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            dayMillis,
                            pendingIntent);
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, dayMillis, pendingIntent);
                    }
                } catch (SecurityException e) {
                    Log.w(TAG, "Exact alarm permission denied, falling back to inexact alarm.", e);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, dayMillis, pendingIntent);
                }
            }
        }
    }

    /**
     * 根据课程的实际周次模式精确取消闹钟
     * 不再硬编码遍历 1-20 周，改为按课程的 weekPattern 和 semester.totalWeeks 精确取消
     */
    public static void cancelAllReminders(Context context, List<Course> courses, Semester semester) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int totalWeeks = semester != null ? semester.getTotalWeeks() : 20;

        for (Course course : courses) {
            List<Integer> weeks = course.getWeekPattern();
            if (weeks != null && !weeks.isEmpty()) {
                for (int weekNum : weeks) {
                    cancelSingleReminder(context, alarmManager, course.getId(), weekNum);
                }
            } else {
                // 无周次信息时回退到遍历总周数（保险起见）
                for (int weekNum = 1; weekNum <= totalWeeks; weekNum++) {
                    cancelSingleReminder(context, alarmManager, course.getId(), weekNum);
                }
            }
        }
    }

    private static void cancelSingleReminder(Context context, AlarmManager alarmManager,
                                              int courseId, int weekNum) {
        Intent intent = new Intent(context, CourseReminderReceiver.class);
        int requestCode = courseId * 1000 + weekNum;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void showReminderNotification(Context context, String courseName,
                                                 String classroom, int periodNumber) {
        if (!canPostNotifications(context)) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        String timeInfo = WeekPatternUtils.getPeriodTime(context, periodNumber);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timetable)
            .setContentTitle("上课提醒：" + courseName)
            .setContentText("地点：" + classroom + " | 第" + periodNumber + "节 " + timeInfo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        manager.notify(courseName.hashCode(), builder.build());
    }

    public static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canScheduleExactAlarms(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && canScheduleExactAlarms(alarmManager);
    }

    private static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms();
    }
}
