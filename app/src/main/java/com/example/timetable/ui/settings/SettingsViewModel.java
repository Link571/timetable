package com.example.timetable.ui.settings;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Semester;
import com.example.timetable.repository.TimetableRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsViewModel extends AndroidViewModel {

    private final TimetableRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SettingsViewModel(Application application) {
        super(application);
        repository = new TimetableRepository(
            AppDatabase.getInstance(application).courseDao(),
            AppDatabase.getInstance(application).semesterDao()
        );
    }

    public LiveData<Semester> getActiveSemester() { return repository.getActiveSemester(); }
    public LiveData<List<Semester>> getAllSemesters() { return repository.getAllSemesters(); }

    public void activateSemester(int semesterId) { repository.activateSemester(semesterId); }
    public void setCurrentWeek(int semesterId, int week) { repository.setCurrentWeek(semesterId, week); }

    public void addSemester(Semester semester) {
        executor.execute(() -> {
            long id = repository.insertSemesterSync(semester);
            repository.activateSemester((int) id);
        });
    }

    public void deleteSemester(int semesterId) {
        repository.deleteSemester(semesterId);
    }
}
