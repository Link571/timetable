package com.example.timetable;

import android.os.Bundle;

import com.example.timetable.databinding.ActivityMainBinding;
import com.example.timetable.util.NotificationHelper;
import com.example.timetable.util.PreferenceUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.createNotificationChannel(this);

        // Apply saved theme color to status bar
        int themeColor = PreferenceUtils.getThemeColor(this);
        getWindow().setStatusBarColor(themeColor);
        getWindow().setNavigationBarColor(themeColor);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

}