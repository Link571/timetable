package com.example.timetable.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.timetable.util.NotificationHelper;

public class CourseReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String courseName = intent.getStringExtra("course_name");
        String classroom = intent.getStringExtra("classroom");
        int periodNumber = intent.getIntExtra("period_number", 0);

        if (courseName != null) {
            NotificationHelper.showReminderNotification(context, courseName, classroom, periodNumber);
        }
    }
}
