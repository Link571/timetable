package com.example.timetable.ui.timetable;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.WeekPatternUtils;
import com.example.timetable.util.PreferenceUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimetableViewModel extends AndroidViewModel {

    private final TimetableRepository repository;
    private final MutableLiveData<List<Course[]>> gridData = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentWeek = new MutableLiveData<>(1);
    private final MutableLiveData<String> weekInfo = new MutableLiveData<>("");
    private final MutableLiveData<String[]> headerDates = new MutableLiveData<>();

    private Semester activeSemester;
    private int lastLoadedSemesterId = -1;
    private List<Course> allCourses = new ArrayList<>();
    private LiveData<List<Course>> currentCoursesLiveData;
    private Observer<List<Course>> coursesObserver;
    private Observer<Semester> semesterObserver;

    public TimetableViewModel(Application application) {
        super(application);
        repository = new TimetableRepository(
            AppDatabase.getInstance(application).courseDao(),
            AppDatabase.getInstance(application).semesterDao()
        );

        // 初始化空行（根据配置的节次数动态生成）
        int totalPeriods = PreferenceUtils.getTotalPeriodCount(application);
        List<Course[]> initialRows = new ArrayList<>();
        for (int i = 0; i < totalPeriods; i++) {
            initialRows.add(new Course[8]);
        }
        gridData.setValue(initialRows);
        weekInfo.setValue("");

        coursesObserver = courses -> {
            allCourses = courses != null ? courses : new ArrayList<>();
            rebuildGrid();
        };

        semesterObserver = semester -> {
            activeSemester = semester;
            if (semester != null) {
                if (semester.getId() != lastLoadedSemesterId) {
                    // 首次加载或切换学期：根据日期自动计算当前周次
                    int autoWeek = calculateCurrentWeek(semester);
                    currentWeek.postValue(autoWeek);
                    if (autoWeek != semester.getCurrentWeek()) {
                        repository.setCurrentWeek(semester.getId(), autoWeek);
                    }
                    lastLoadedSemesterId = semester.getId();
                } else {
                    // 同一学期内 DB 更新（如手动切周）：直接使用 DB 中的值
                    currentWeek.postValue(semester.getCurrentWeek());
                }
                if (currentCoursesLiveData != null) {
                    currentCoursesLiveData.removeObserver(coursesObserver);
                }
                currentCoursesLiveData = repository.getCoursesBySemester(semester.getId());
                currentCoursesLiveData.observeForever(coursesObserver);
            } else {
                lastLoadedSemesterId = -1;
                allCourses = new ArrayList<>();
                currentWeek.postValue(1);
                rebuildGrid();
            }
        };

        repository.getActiveSemester().observeForever(semesterObserver);
    }

    public LiveData<List<Course[]>> getGridData() { return gridData; }
    public LiveData<Integer> getCurrentWeek() { return currentWeek; }
    public LiveData<String> getWeekInfo() { return weekInfo; }
    public LiveData<String[]> getHeaderDates() { return headerDates; }

    public void nextWeek() {
        if (activeSemester == null) return;
        int week = currentWeek.getValue() != null ? currentWeek.getValue() : 1;
        if (week < activeSemester.getTotalWeeks()) {
            currentWeek.setValue(week + 1);
            repository.setCurrentWeek(activeSemester.getId(), week + 1);
            rebuildGrid();
        }
    }

    public void prevWeek() {
        int week = currentWeek.getValue() != null ? currentWeek.getValue() : 1;
        if (week > 1) {
            currentWeek.setValue(week - 1);
            if (activeSemester != null) {
                repository.setCurrentWeek(activeSemester.getId(), week - 1);
            }
            rebuildGrid();
        }
    }

    /**
     * 根据当天日期和学期起始日自动计算当前周次，结果钳位在 [1, totalWeeks]
     */
    private int calculateCurrentWeek(Semester semester) {
        long today = System.currentTimeMillis();
        long diff = today - semester.getStartDate();
        int autoWeek = (int) (diff / (7L * 86400000L)) + 1;
        if (autoWeek < 1) autoWeek = 1;
        if (autoWeek > semester.getTotalWeeks()) autoWeek = semester.getTotalWeeks();
        return autoWeek;
    }

    private void rebuildGrid() {
        int week = currentWeek.getValue() != null ? currentWeek.getValue() : 1;

        if (activeSemester != null) {
            weekInfo.postValue("第" + week + "周 (共" + activeSemester.getTotalWeeks() + "周)");
            // 计算周一~周日对应的实际日期
            String[] dates = new String[8]; // 索引 1-7 对应周一~周日
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(activeSemester.getStartDate());
            cal.add(Calendar.WEEK_OF_YEAR, week - 1);
            for (int d = 1; d <= 7; d++) {
                Calendar dayCal = (Calendar) cal.clone();
                dayCal.add(Calendar.DAY_OF_MONTH, d - 1);
                dates[d] = (dayCal.get(Calendar.MONTH) + 1) + "/" + dayCal.get(Calendar.DAY_OF_MONTH);
            }
            headerDates.postValue(dates);
        } else {
            weekInfo.postValue("第" + week + "周");
        }

        int totalPeriods = PreferenceUtils.getTotalPeriodCount(getApplication());
        List<Course[]> rows = new ArrayList<>();
        for (int period = 1; period <= totalPeriods; period++) {
            rows.add(new Course[8]);
        }

        for (Course course : allCourses) {
            if (WeekPatternUtils.isActiveInWeek(course.getWeekPattern(), week)) {
                int day = course.getDayOfWeek();
                if (day >= 1 && day <= 7) {
                    int startIdx = course.getStartPeriod() - 1;
                    int endIdx = startIdx + course.getDuration(); // 结束节次（不含）
                    for (int idx = startIdx; idx < endIdx && idx < totalPeriods; idx++) {
                        if (idx >= 0) {
                            rows.get(idx)[day] = course;
                        }
                    }
                }
            }
        }

        gridData.postValue(rows);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getActiveSemester().removeObserver(semesterObserver);
        if (currentCoursesLiveData != null) {
            currentCoursesLiveData.removeObserver(coursesObserver);
        }
    }
}
