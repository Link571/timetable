package com.example.timetable.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.timetable.data.AppExecutors;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.NotificationHelper;
import com.example.timetable.util.PreferenceUtils;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (!PreferenceUtils.isReminderEnabled(context)) return;

            PendingResult pendingResult = goAsync();
            Context appContext = context.getApplicationContext();
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    TimetableRepository repository = TimetableRepository.getInstance(appContext);
                    Semester semester = repository.getActiveSemesterSync();
                    if (semester != null) {
                        List<Course> courses = repository.getCoursesBySemesterSync(semester.getId());
                        int advanceMin = PreferenceUtils.getReminderMinutes(appContext);
                        NotificationHelper.scheduleAllReminders(appContext, courses, semester, advanceMin);
                    }
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
