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

import java.util.ArrayList;
import java.util.List;

public class TimetableViewModel extends AndroidViewModel {

    private final TimetableRepository repository;
    private final MutableLiveData<List<Course[]>> gridData = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentWeek = new MutableLiveData<>(1);
    private final MutableLiveData<String> weekInfo = new MutableLiveData<>("");

    private Semester activeSemester;
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

        // Initialize with 12 empty rows so RecyclerView has data before Room query completes
        List<Course[]> initialRows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
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
                currentWeek.postValue(semester.getCurrentWeek());
                if (currentCoursesLiveData != null) {
                    currentCoursesLiveData.removeObserver(coursesObserver);
                }
                currentCoursesLiveData = repository.getCoursesBySemester(semester.getId());
                currentCoursesLiveData.observeForever(coursesObserver);
            } else {
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

    private void rebuildGrid() {
        int week = currentWeek.getValue() != null ? currentWeek.getValue() : 1;

        if (activeSemester != null) {
            weekInfo.postValue("第" + week + "周 (共" + activeSemester.getTotalWeeks() + "周)");
        } else {
            weekInfo.postValue("第" + week + "周");
        }

        List<Course[]> rows = new ArrayList<>();
        for (int period = 1; period <= 12; period++) {
            rows.add(new Course[8]);
        }

        for (Course course : allCourses) {
            if (WeekPatternUtils.isActiveInWeek(course.getWeekPattern(), week)) {
                int periodIdx = course.getStartPeriod() - 1;
                if (periodIdx >= 0 && periodIdx < 12) {
                    int day = course.getDayOfWeek();
                    if (day >= 1 && day <= 7) {
                        rows.get(periodIdx)[day] = course;
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
