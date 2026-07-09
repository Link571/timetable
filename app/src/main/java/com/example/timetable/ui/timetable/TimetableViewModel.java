package com.example.timetable.ui.timetable;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.DateUtils;
import com.example.timetable.util.WeekPatternUtils;
import com.example.timetable.util.PreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public class TimetableViewModel extends AndroidViewModel {

    private final TimetableRepository repository;
    private final MediatorLiveData<List<GridItem>> gridData = new MediatorLiveData<>();
    private final MutableLiveData<Integer> currentWeek = new MutableLiveData<>(1);
    private final MutableLiveData<String> weekInfo = new MutableLiveData<>("");
    private final MutableLiveData<String[]> headerDates = new MutableLiveData<>();

    private Semester activeSemester;
    private int lastLoadedSemesterId = -1;
    private List<Course> allCourses = new ArrayList<>();

    private LiveData<List<Course>> currentCoursesSource;

    public TimetableViewModel(@NonNull Application application) {
        super(application);
        repository = TimetableRepository.getInstance(application);

        // 初始化空网格（根据配置的节次数动态生成，含午休/晚修分隔行）
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

        LiveData<Semester> activeSemesterLiveData = repository.getActiveSemester();
        gridData.addSource(activeSemesterLiveData, semester -> {
            activeSemester = semester;
            if (currentCoursesSource != null) {
                gridData.removeSource(currentCoursesSource);
                currentCoursesSource = null;
            }

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
                currentCoursesSource = repository.getCoursesBySemester(semester.getId());
                gridData.addSource(currentCoursesSource, courses -> {
                    allCourses = courses != null ? courses : new ArrayList<>();
                    rebuildGrid();
                });
                return;
            }

            // 无活跃学期时返回空列表，清空状态
            lastLoadedSemesterId = -1;
            allCourses = new ArrayList<>();
            currentWeek.postValue(1);
            weekInfo.postValue("");
            rebuildGrid();
        });
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
        return DateUtils.calculateCurrentWeek(semester.getStartDate(), semester.getTotalWeeks());
    }

    private void rebuildGrid() {
        int week = currentWeek.getValue() != null ? currentWeek.getValue() : 1;

        if (activeSemester != null) {
            weekInfo.postValue(DateUtils.formatWeekInfo(week, activeSemester.getTotalWeeks()));
            String[] dates = new String[8];
            for (int d = 1; d <= 7; d++) {
                dates[d] = DateUtils.formatDateShort(activeSemester.getStartDate(), week, d);
            }
            headerDates.postValue(dates);
        } else {
            weekInfo.postValue("第" + week + "周");
        }

        int morningCount = PreferenceUtils.getMorningCount(getApplication());
        int afternoonCount = PreferenceUtils.getAfternoonCount(getApplication());
        int eveningCount = PreferenceUtils.getEveningCount(getApplication());
        int totalPeriods = morningCount + afternoonCount + eveningCount;

        List<GridItem> items = new ArrayList<>();
        java.util.Map<Integer, Integer> periodToIndex = new java.util.HashMap<>();

        for (int p = 1; p <= morningCount; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }
        items.add(GridItem.breakRow("午休"));

        for (int p = morningCount + 1; p <= morningCount + afternoonCount; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }
        items.add(GridItem.breakRow("晚修"));

        for (int p = morningCount + afternoonCount + 1; p <= totalPeriods; p++) {
            periodToIndex.put(p, items.size());
            items.add(GridItem.period(p, new Course[8]));
        }

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
}
