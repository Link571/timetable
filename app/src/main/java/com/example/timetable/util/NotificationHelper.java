package com.example.timetable.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.timetable.R;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.receiver.CourseReminderReceiver;

import java.util.Calendar;
import java.util.List;

public class NotificationHelper {

    public static final String CHANNEL_ID = "course_reminder";
    private static final String CHANNEL_NAME = "上课提醒";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("上课前提醒通知");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void scheduleAllReminders(Context context, List<Course> courses,
                                            Semester semester, int advanceMinutes) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Cancel existing alarms first
        cancelAllReminders(context, courses);

        for (Course course : courses) {
            List<Integer> weeks = course.getWeekPattern();
            if (weeks == null || weeks.isEmpty()) continue;

            int dayOfWeek = course.getDayOfWeek(); // 1=Mon..7=Sun
            int startHour = WeekPatternUtils.getPeriodStartHour(context, course.getStartPeriod());
            int startMinute = WeekPatternUtils.getPeriodStartMinute(context, course.getStartPeriod());

            // Convert dayOfWeek (1=Mon) to Calendar.DAY_OF_WEEK (Mon=2, Sun=1)
            int calendarDay = (dayOfWeek % 7) + 1; // 1->2(Mon), 7->1(Sun)

            // Schedule alarm for each active week
            for (int weekNum : weeks) {
                Calendar weekStartMonday = Calendar.getInstance();
                weekStartMonday.setTimeInMillis(semester.getStartDate());
                // Calculate the Monday of the given week
                Calendar targetDay = Calendar.getInstance();
                targetDay.setTimeInMillis(semester.getStartDate());
                targetDay.add(Calendar.WEEK_OF_YEAR, weekNum - 1);
                targetDay.add(Calendar.DAY_OF_MONTH, dayOfWeek - 1);
                targetDay.set(Calendar.HOUR_OF_DAY, startHour);
                targetDay.set(Calendar.MINUTE, startMinute);
                targetDay.set(Calendar.SECOND, 0);
                targetDay.set(Calendar.MILLISECOND, 0);

                // Subtract advance minutes
                targetDay.add(Calendar.MINUTE, -advanceMinutes);

                // Only schedule if it's in the future
                if (targetDay.getTimeInMillis() <= System.currentTimeMillis()) continue;

                Intent intent = new Intent(context, CourseReminderReceiver.class);
                intent.putExtra("course_name", course.getName());
                intent.putExtra("classroom", course.getClassroom());
                intent.putExtra("period_number", course.getStartPeriod());
                intent.putExtra("day_of_week", dayOfWeek);

                int requestCode = course.getId() * 1000 + weekNum;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (alarmManager != null) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetDay.getTimeInMillis(),
                            pendingIntent);
                    } catch (SecurityException ignored) {}
                }
            }
        }
    }

    public static void cancelAllReminders(Context context, List<Course> courses) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (Course course : courses) {
            for (int weekNum = 1; weekNum <= 20; weekNum++) {
                Intent intent = new Intent(context, CourseReminderReceiver.class);
                int requestCode = course.getId() * 1000 + weekNum;
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    public static void showReminderNotification(Context context, String courseName,
                                                 String classroom, int periodNumber) {
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
}
