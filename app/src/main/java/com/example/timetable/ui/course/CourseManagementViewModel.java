package com.example.timetable.ui.course;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.repository.TimetableRepository;

import java.util.List;

public class CourseManagementViewModel extends AndroidViewModel {

    private final TimetableRepository repository;
    private final MediatorLiveData<List<Course>> allCourses = new MediatorLiveData<>();
    private LiveData<List<Course>> currentCoursesSource;

    public CourseManagementViewModel(Application application) {
        super(application);
        repository = new TimetableRepository(
            AppDatabase.getInstance(application).courseDao(),
            AppDatabase.getInstance(application).semesterDao()
        );

        allCourses.addSource(repository.getActiveSemester(), semester -> {
            if (semester != null) {
                if (currentCoursesSource != null) {
                    allCourses.removeSource(currentCoursesSource);
                }
                currentCoursesSource = repository.getCoursesBySemester(semester.getId());
                allCourses.addSource(currentCoursesSource, courses -> {
                    allCourses.postValue(courses);
                });
            } else {
                allCourses.postValue(new java.util.ArrayList<>());
            }
        });
    }

    public LiveData<List<Course>> getAllCourses() { return allCourses; }

    public LiveData<Semester> getActiveSemester() { return repository.getActiveSemester(); }

    public void deleteCourse(Course course) {
        repository.deleteCourse(course);
    }
}
