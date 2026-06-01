package com.example.timetable.ui.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.timetable.MainActivity;
import com.example.timetable.R;
import com.example.timetable.util.WeekPatternUtils;

import java.util.Calendar;

public class TimetableWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timetable);

            Calendar cal = Calendar.getInstance();
            int dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1;
            String dateText = (cal.get(Calendar.MONTH) + 1) + "月" +
                cal.get(Calendar.DAY_OF_MONTH) + "日 " +
                WeekPatternUtils.getDayName(dayOfWeek);
            views.setTextViewText(R.id.tv_date, dateText);

            Intent intent = new Intent(context, WidgetRemoteViewsService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            views.setRemoteAdapter(R.id.lv_courses, intent);

            views.setEmptyView(R.id.lv_courses, R.id.tv_empty);

            Intent clickIntent = new Intent(context, MainActivity.class);
            PendingIntent clickPI = PendingIntent.getActivity(context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setPendingIntentTemplate(R.id.lv_courses, clickPI);

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}
