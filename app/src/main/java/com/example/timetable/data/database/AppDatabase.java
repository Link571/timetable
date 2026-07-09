package com.example.timetable.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.timetable.BuildConfig;
import com.example.timetable.data.converter.Converters;
import com.example.timetable.data.dao.CourseDao;
import com.example.timetable.data.dao.SemesterDao;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;

@Database(entities = {Course.class, Semester.class}, version = 2, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract CourseDao courseDao();
    public abstract SemesterDao semesterDao();

    /**
     * v1 → v2 迁移：预留迁移模板（当前无结构变更）
     * 后续版本变更时，在此处添加新的 Migration 子类并递增 version
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 当前版本无表结构变更，仅为演示迁移模式
            // 实际需要变更时，在此执行 SQL：
            // database.execSQL("ALTER TABLE course ADD COLUMN xxx TEXT");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    RoomDatabase.Builder<AppDatabase> builder = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "timetable.db"
                    );
                    // 仅在 Debug 构建中允许破坏性迁移，Release 版必须使用 Migration 策略
                    if (BuildConfig.DEBUG) {
                        builder.fallbackToDestructiveMigration();
                    } else {
                        builder.addMigrations(MIGRATION_1_2);
                    }
                    INSTANCE = builder.build();
                }
            }
        }
        return INSTANCE;
    }
}
