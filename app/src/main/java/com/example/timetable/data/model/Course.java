package com.example.timetable.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.List;

@Entity(
    tableName = "course",
    foreignKeys = @ForeignKey(
        entity = Semester.class,
        parentColumns = "id",
        childColumns = "semesterId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("semesterId")
)
public class Course implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private String teacher;

    @NonNull
    private String classroom;

    private int dayOfWeek;   // 1=周一 ~ 7=周日

    private int startPeriod; // 1-12

    private int duration = 1;

    @NonNull
    private List<Integer> weekPattern;

    private int color;

    private int semesterId;

    private String notes;

    public Course() {}

    // Getters and setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    @NonNull
    public String getClassroom() { return classroom; }
    public void setClassroom(@NonNull String classroom) { this.classroom = classroom; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getStartPeriod() { return startPeriod; }
    public void setStartPeriod(int startPeriod) { this.startPeriod = startPeriod; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    @NonNull
    public List<Integer> getWeekPattern() { return weekPattern; }
    public void setWeekPattern(@NonNull List<Integer> weekPattern) { this.weekPattern = weekPattern; }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int semesterId) { this.semesterId = semesterId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
