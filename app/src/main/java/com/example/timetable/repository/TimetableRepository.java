package com.example.timetable.repository;

import android.content.Context;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

import androidx.lifecycle.LiveData;

import com.example.timetable.R;
import com.example.timetable.data.AppExecutors;
import com.example.timetable.data.dao.CourseDao;
import com.example.timetable.data.dao.SemesterDao;
import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.ui.widget.TimetableWidgetProvider;
import com.example.timetable.util.NotificationHelper;
import com.example.timetable.util.PreferenceUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 数据仓库层：封装 Course 和 Semester 的所有数据操作
 * 使用单例模式 + 全局线程池，避免重复创建 Executor 实例
 *
 * 获取实例：
 * <pre>{@code
 *   TimetableRepository repo = TimetableRepository.getInstance(context);
 * }</pre>
 */
public class TimetableRepository {

    private static volatile TimetableRepository INSTANCE;

    private final Context appContext;
    private final AppDatabase db;
    private final CourseDao courseDao;
    private final SemesterDao semesterDao;
    private final ExecutorService executor;

    private TimetableRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(appContext);
        this.courseDao = db.courseDao();
        this.semesterDao = db.semesterDao();
        this.executor = AppExecutors.getInstance().diskIO();
    }

    /** 获取全局唯一实例 */
    public static TimetableRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TimetableRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TimetableRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // --- Semester operations ---

    public LiveData<Semester> getActiveSemester() {
        return semesterDao.getActiveSemester();
    }

    public Semester getActiveSemesterSync() {
        return semesterDao.getActiveSemesterSync();
    }

    public LiveData<List<Semester>> getAllSemesters() {
        return semesterDao.getAllSemesters();
    }

    public void insertSemester(Semester semester) {
        executor.execute(() -> {
            semesterDao.insert(semester);
            afterScheduleAffectingChange();
        });
    }

    public long insertSemesterSync(Semester semester) {
        return semesterDao.insert(semester);
    }

    public void activateSemester(int semesterId) {
        executor.execute(() -> {
            db.runInTransaction(() -> {
                semesterDao.deactivateAll();
                semesterDao.activate(semesterId);
            });
            afterScheduleAffectingChange();
        });
    }

    public void setCurrentWeek(int semesterId, int week) {
        executor.execute(() -> {
            semesterDao.setCurrentWeek(semesterId, week);
            updateWidgets();
        });
    }

    public void updateSemester(Semester semester) {
        executor.execute(() -> {
            semesterDao.update(semester);
            afterScheduleAffectingChange();
        });
    }

    public void deleteSemester(int semesterId) {
        executor.execute(() -> {
            db.runInTransaction(() -> {
                courseDao.deleteAllBySemester(semesterId);
                semesterDao.deleteById(semesterId);
            });
            afterScheduleAffectingChange();
        });
    }

    // --- Course operations ---

    public LiveData<List<Course>> getCoursesBySemester(int semesterId) {
        return courseDao.getCoursesBySemester(semesterId);
    }

    public List<Course> getCoursesBySemesterSync(int semesterId) {
        return courseDao.getCoursesBySemesterSync(semesterId);
    }

    public List<Course> getCoursesBySemesterAndDaySync(int semesterId, int dayOfWeek) {
        return courseDao.getCoursesBySemesterAndDaySync(semesterId, dayOfWeek);
    }

    public LiveData<Course> getCourseById(int courseId) {
        return courseDao.getCourseById(courseId);
    }

    public void insertCourse(Course course) {
        executor.execute(() -> {
            long id = courseDao.insert(course);
            course.setId((int) id);
            afterScheduleAffectingChange();
        });
    }

    public void updateCourse(Course course) {
        executor.execute(() -> {
            courseDao.update(course);
            afterScheduleAffectingChange();
        });
    }

    public void deleteCourse(Course course) {
        executor.execute(() -> {
            courseDao.delete(course);
            afterScheduleAffectingChange();
        });
    }

    public void deleteCourseById(int courseId) {
        executor.execute(() -> {
            courseDao.deleteById(courseId);
            afterScheduleAffectingChange();
        });
    }

    public void rescheduleReminders() {
        executor.execute(this::rescheduleRemindersSync);
    }

    public void cancelReminders() {
        executor.execute(this::cancelRemindersSync);
    }

    private void afterScheduleAffectingChange() {
        updateWidgets();
        if (!PreferenceUtils.isReminderEnabled(appContext)) return;

        rescheduleRemindersSync();
    }

    private void rescheduleRemindersSync() {
        Semester semester = semesterDao.getActiveSemesterSync();
        if (semester == null) return;

        List<Course> courses = courseDao.getCoursesBySemesterSync(semester.getId());
        NotificationHelper.scheduleAllReminders(
            appContext,
            courses,
            semester,
            PreferenceUtils.getReminderMinutes(appContext));
    }

    private void cancelRemindersSync() {
        Semester semester = semesterDao.getActiveSemesterSync();
        if (semester == null) return;

        List<Course> courses = courseDao.getCoursesBySemesterSync(semester.getId());
        NotificationHelper.cancelAllReminders(appContext, courses, semester);
    }

    private void updateWidgets() {
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        ComponentName provider = new ComponentName(appContext, TimetableWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(provider);
        if (ids.length == 0) return;

        manager.notifyAppWidgetViewDataChanged(ids, R.id.lv_courses);
        new TimetableWidgetProvider().onUpdate(appContext, manager, ids);
    }
}
