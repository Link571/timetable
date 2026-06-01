package com.example.timetable.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timetable.data.model.Semester;
import java.util.List;

@Dao
public interface SemesterDao {

    @Query("SELECT * FROM semester WHERE isActive = 1 LIMIT 1")
    LiveData<Semester> getActiveSemester();

    @Query("SELECT * FROM semester WHERE isActive = 1 LIMIT 1")
    Semester getActiveSemesterSync();

    @Query("SELECT * FROM semester ORDER BY startDate DESC")
    LiveData<List<Semester>> getAllSemesters();

    @Query("SELECT * FROM semester ORDER BY startDate DESC")
    List<Semester> getAllSemestersSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Semester semester);

    @Update
    void update(Semester semester);

    @Query("UPDATE semester SET isActive = 0")
    void deactivateAll();

    @Query("UPDATE semester SET isActive = 1 WHERE id = :semesterId")
    void activate(int semesterId);

    @Query("UPDATE semester SET currentWeek = :week WHERE id = :semesterId")
    void setCurrentWeek(int semesterId, int week);

    @Query("SELECT * FROM semester WHERE id = :semesterId")
    Semester getByIdSync(int semesterId);

    @Query("DELETE FROM semester WHERE id = :semesterId")
    void deleteById(int semesterId);
}
