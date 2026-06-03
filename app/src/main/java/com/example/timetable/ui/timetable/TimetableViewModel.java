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
    private final MutableLiveData<List<GridItem>> gridData = new MutableLiveData<>();
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

        // 初始化空行（根据配置的节次数动态生成，含午休/晚修分隔行）
        int morningCount = PreferenceUtils.getMorningCount(application);
        int afternoonCount = PreferenceUtils.getAfternoonCount(application);
        int eveningCount = PreferenceUtils.getEveningCount(application);
        int totalPeriods = morningCount + afternoonCount + eveningCount;

        List<GridItem> initialItems = new ArrayList<>();
        for (int p = 1; p <= morningCount; p++) {
            initialItems.add(GridItem.period(p, new Course[8]));
        }
        initialItems.add(GridItem.breakRow("午休"));
        for (int p = morningCount + 1; p <= morningCount + afternoonCount; p++) {
            initialItems.add(GridItem.period(p, new Course[8]));
        }
        initialItems.add(GridItem.breakRow("晚修"));
        for (int p = morningCount + afternoonCount + 1; p <= totalPeriods; p++) {
            initialItems.add(GridItem.period(p, new Course[8]));
        }
        gridData.setValue(initialItems);
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

    public LiveData<List<GridItem>> getGridData() { return gridData; }
    public LiveData<Integer> getCurrentWeek() { return currentWeek; }
    public LiveData<String> getWeekInfo() { return weekInfo; }
    public LiveData<String[]> getHeaderDates() { return headerDates; }

    /**
     * 获取当前激活学期的总周数，用于边界检查
     * @return 总周数，无激活学期时返回 0
     */
    public int getTotalWeeks() {
        return activeSemester != null ? activeSemester.getTotalWeeks() : 0;
    }

    /**
     * 从外部强制刷新课表网格（如设置页修改节次数后调用）
     */
    public void refreshGrid() {
        rebuildGrid();
    }

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
            String[] dates = new String[8];
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

        // 获取上午/下午/晚课节数配置
        int morningCount = PreferenceUtils.getMorningCount(getApplication());
        int afternoonCount = PreferenceUtils.getAfternoonCount(getApplication());
        int eveningCount = PreferenceUtils.getEveningCount(getApplication());
        int totalPeriods = morningCount + afternoonCount + eveningCount;

        // 按顺序构建 GridItem 列表：上午节次 → 午休 → 下午节次 → 晚修 → 晚课节次
        List<GridItem> items = new ArrayList<>();
        java.util.Map<Integer, Integer> periodToIndex = new java.util.HashMap<>();

        // 上午节次：period 1 ~ morningCount
        for (int p = 1; p <= morningCount; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }
        // 午休分隔行
        items.add(GridItem.breakRow("午休"));

        // 下午节次：period (morningCount+1) ~ (morningCount+afternoonCount)
        for (int p = morningCount + 1; p <= morningCount + afternoonCount; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }
        // 晚修分隔行
        items.add(GridItem.breakRow("晚修"));

        // 晚课节次：period (morningCount+afternoonCount+1) ~ totalPeriods
        for (int p = morningCount + afternoonCount + 1; p <= totalPeriods; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }

        // 将课程填入对应的节次行
        for (Course course : allCourses) {
            if (WeekPatternUtils.isActiveInWeek(course.getWeekPattern(), week)) {
                int day = course.getDayOfWeek();
                if (day >= 1 && day <= 7) {
                    int startPeriod = course.getStartPeriod();
                    int duration = course.getDuration();
                    for (int offset = 0; offset < duration; offset++) {
                        int period = startPeriod + offset;
                        Integer idx = periodToIndex.get(period);
                        if (idx != null && idx < items.size()) {
                            items.get(idx).courses[day] = course;
                        }
                    }
                }
            }
        }

        gridData.postValue(items);
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
