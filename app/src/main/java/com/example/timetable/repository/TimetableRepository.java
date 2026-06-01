package com.example.timetable.repository;

import androidx.lifecycle.LiveData;

import com.example.timetable.data.dao.CourseDao;
import com.example.timetable.data.dao.SemesterDao;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimetableRepository {

    private final CourseDao courseDao;
    private final SemesterDao semesterDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TimetableRepository(CourseDao courseDao, SemesterDao semesterDao) {
        this.courseDao = courseDao;
        this.semesterDao = semesterDao;
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
        executor.execute(() -> semesterDao.insert(semester));
    }

    public long insertSemesterSync(Semester semester) {
        return semesterDao.insert(semester);
    }

    public void activateSemester(int semesterId) {
        executor.execute(() -> {
            semesterDao.deactivateAll();
            semesterDao.activate(semesterId);
        });
    }

    public void setCurrentWeek(int semesterId, int week) {
        executor.execute(() -> semesterDao.setCurrentWeek(semesterId, week));
    }

    public void deleteSemester(int semesterId) {
        executor.execute(() -> {
            courseDao.deleteAllBySemester(semesterId);
            semesterDao.deleteById(semesterId);
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
        executor.execute(() -> courseDao.insert(course));
    }

    public void updateCourse(Course course) {
        executor.execute(() -> courseDao.update(course));
    }

    public void deleteCourse(Course course) {
        executor.execute(() -> courseDao.delete(course));
    }

    public void deleteCourseById(int courseId) {
        executor.execute(() -> courseDao.deleteById(courseId));
    }
}
