package com.example.timetable.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timetable.data.model.Course;
import java.util.List;

@Dao
public interface CourseDao {

    @Query("SELECT * FROM course WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startPeriod ASC")
    LiveData<List<Course>> getCoursesBySemester(int semesterId);

    @Query("SELECT * FROM course WHERE semesterId = :semesterId AND dayOfWeek = :dayOfWeek ORDER BY startPeriod ASC")
    LiveData<List<Course>> getCoursesBySemesterAndDay(int semesterId, int dayOfWeek);

    @Query("SELECT * FROM course WHERE semesterId = :semesterId")
    List<Course> getCoursesBySemesterSync(int semesterId);

    @Query("SELECT * FROM course WHERE semesterId = :semesterId AND dayOfWeek = :dayOfWeek ORDER BY startPeriod ASC")
    List<Course> getCoursesBySemesterAndDaySync(int semesterId, int dayOfWeek);

    @Query("SELECT * FROM course WHERE id = :courseId")
    LiveData<Course> getCourseById(int courseId);

    @Query("SELECT * FROM course WHERE id = :courseId")
    Course getCourseByIdSync(int courseId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Course course);

    @Update
    void update(Course course);

    @Delete
    void delete(Course course);

    @Query("DELETE FROM course WHERE id = :courseId")
    void deleteById(int courseId);

    @Query("DELETE FROM course WHERE semesterId = :semesterId")
    void deleteAllBySemester(int semesterId);
}
