package com.example.timetable.ui.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.timetable.R;
import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.util.WeekPatternUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;
    private List<Course> todayCourses = new ArrayList<>();

    public WidgetRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDataSetChanged() {
        todayCourses.clear();
        AppDatabase db = AppDatabase.getInstance(context);
        Semester semester = db.semesterDao().getActiveSemesterSync();
        if (semester != null) {
            Calendar cal = Calendar.getInstance();
            int dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
            int currentWeek = semester.getCurrentWeek();
            List<Course> all = db.courseDao().getCoursesBySemesterAndDaySync(semester.getId(), dayOfWeek);
            for (Course c : all) {
                if (WeekPatternUtils.isActiveInWeek(c.getWeekPattern(), currentWeek)) {
                    todayCourses.add(c);
                }
            }
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Course course = todayCourses.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timetable_item);
        views.setTextViewText(R.id.tv_course_name, course.getName());
        String timeInfo = "第" + course.getStartPeriod() + "节 " + course.getClassroom();
        views.setTextViewText(R.id.tv_time_room, timeInfo);
        views.setInt(R.id.color_dot, "setBackgroundColor", course.getColor());

        Intent fillIntent = new Intent();
        views.setOnClickFillInIntent(R.id.color_dot, fillIntent);

        return views;
    }

    @Override
    public int getCount() { return todayCourses.size(); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public boolean hasStableIds() { return true; }

    @Override
    public RemoteViews getLoadingView() { return null; }

    @Override
    public int getViewTypeCount() { return 1; }

    @Override
    public void onDestroy() {}
}
