package com.example.timetable.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.timetable.data.converter.Converters;
import com.example.timetable.data.dao.CourseDao;
import com.example.timetable.data.dao.SemesterDao;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;

@Database(entities = {Course.class, Semester.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CourseDao courseDao();
    public abstract SemesterDao semesterDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "timetable.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
