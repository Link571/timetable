package com.example.timetable.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "semester")
public class Semester {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private long startDate;  // 学期第一周周一的 epoch millis

    private int totalWeeks;  // 总教学周数

    private int currentWeek = 1; // 当前教学周

    private boolean isActive;

    public Semester() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public int getTotalWeeks() { return totalWeeks; }
    public void setTotalWeeks(int totalWeeks) { this.totalWeeks = totalWeeks; }

    public int getCurrentWeek() { return currentWeek; }
    public void setCurrentWeek(int currentWeek) { this.currentWeek = currentWeek; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
