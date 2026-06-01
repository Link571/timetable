package com.example.timetable.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.util.NotificationHelper;
import com.example.timetable.util.PreferenceUtils;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (!PreferenceUtils.isReminderEnabled(context)) return;

            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(context);
                Semester semester = db.semesterDao().getActiveSemesterSync();
                if (semester != null) {
                    List<Course> courses = db.courseDao().getCoursesBySemesterSync(semester.getId());
                    int advanceMin = PreferenceUtils.getReminderMinutes(context);
                    NotificationHelper.scheduleAllReminders(context, courses, semester, advanceMin);
                }
            }).start();
        }
    }
}
