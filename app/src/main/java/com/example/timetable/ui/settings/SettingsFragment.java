package com.example.timetable.ui.settings;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.FragmentSettingsBinding;
import com.example.timetable.util.ColorUtils;
import com.example.timetable.util.PreferenceUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private List<Semester> allSemesters;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupReminderSettings();
        setupThemeColorPicker();

        binding.btnEditPeriodTimes.setOnClickListener(v -> showEditPeriodTimesDialog());

        viewModel.getAllSemesters().observe(getViewLifecycleOwner(), semesters -> {
            allSemesters = semesters;
            String[] names = new String[semesters.size()];
            int selectedIdx = 0;
            for (int i = 0; i < semesters.size(); i++) {
                names[i] = semesters.get(i).getName();
                if (semesters.get(i).isActive()) selectedIdx = i;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, names);
            binding.spinnerSemester.setAdapter(adapter);
            if (semesters.size() > 0) binding.spinnerSemester.setSelection(selectedIdx);
        });

        viewModel.getActiveSemester().observe(getViewLifecycleOwner(), semester -> {
            if (semester != null) {
                setupWeekPicker(semester);
            }
        });

        binding.spinnerSemester.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                if (allSemesters != null && position < allSemesters.size()) {
                    Semester selected = allSemesters.get(position);
                    if (!selected.isActive()) {
                        viewModel.activateSemester(selected.getId());
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.btnAddSemester.setOnClickListener(v -> showAddSemesterDialog());
    }

    private void setupWeekPicker(Semester semester) {
        binding.npWeek.setMinValue(1);
        binding.npWeek.setMaxValue(semester.getTotalWeeks());
        binding.npWeek.setValue(semester.getCurrentWeek());
        binding.npWeek.setOnValueChangedListener((picker, oldVal, newVal) -> {
            viewModel.setCurrentWeek(semester.getId(), newVal);
        });

        long today = System.currentTimeMillis();
        long diff = today - semester.getStartDate();
        int autoWeek = (int) (diff / (7L * 86400000L)) + 1;
        if (autoWeek < 1) autoWeek = 1;
        if (autoWeek > semester.getTotalWeeks()) autoWeek = semester.getTotalWeeks();
        binding.tvAutoWeek.setText("根据日期自动计算：第" + autoWeek + "周");
    }

    private void setupReminderSettings() {
        binding.switchReminder.setChecked(PreferenceUtils.isReminderEnabled(requireContext()));
        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) ->
            PreferenceUtils.setReminderEnabled(requireContext(), isChecked));

        binding.npAdvanceMinutes.setMinValue(5);
        binding.npAdvanceMinutes.setMaxValue(60);
        binding.npAdvanceMinutes.setValue(PreferenceUtils.getReminderMinutes(requireContext()));
        binding.npAdvanceMinutes.setOnValueChangedListener((picker, oldVal, newVal) ->
            PreferenceUtils.setReminderMinutes(requireContext(), newVal));
    }

    private void setupThemeColorPicker() {
        int currentColor = PreferenceUtils.getThemeColor(requireContext());

        for (int color : ColorUtils.PREDEFINED_COLORS) {
            View dot = new View(requireContext());
            int size = (int) (40 * getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(size, size);
            params.setMargins(6, 6, 6, 6);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(color);

            if (color == currentColor) {
                dot.setScaleX(1.15f);
                dot.setScaleY(1.15f);
            }

            dot.setOnClickListener(v -> {
                PreferenceUtils.setThemeColor(requireContext(), color);
                Toast.makeText(requireContext(), "主题色已保存，重启后生效", Toast.LENGTH_SHORT).show();
                for (int i = 0; i < binding.themeColorPicker.getChildCount(); i++) {
                    View child = binding.themeColorPicker.getChildAt(i);
                    child.setScaleX(1.0f);
                    child.setScaleY(1.0f);
                }
                v.setScaleX(1.15f);
                v.setScaleY(1.15f);
            });
            binding.themeColorPicker.addView(dot);
        }
    }

    private void showEditPeriodTimesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("编辑节次时间");

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 0);

        String[] currentTimes = PreferenceUtils.getPeriodTimes(requireContext());
        EditText[] editTexts = new EditText[12];

        for (int i = 0; i < 12; i++) {
            EditText et = new EditText(requireContext());
            et.setText(currentTimes[i]);
            et.setHint("第" + (i + 1) + "节 如: 08:00-08:45");
            et.setSingleLine(true);
            editTexts[i] = et;
            container.addView(et);
        }

        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String[] newTimes = new String[12];
            for (int i = 0; i < 12; i++) {
                String t = editTexts[i].getText().toString().trim();
                if (t.isEmpty()) {
                    newTimes[i] = PreferenceUtils.DEFAULT_PERIOD_TIMES[i];
                } else {
                    newTimes[i] = t;
                }
            }
            PreferenceUtils.setPeriodTimes(requireContext(), newTimes);
            Toast.makeText(requireContext(), "节次时间已保存", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showAddSemesterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加新学期");

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 0);

        final EditText inputName = new EditText(requireContext());
        inputName.setHint("例如：2025-2026 第一学期");
        container.addView(inputName);

        final EditText inputWeeks = new EditText(requireContext());
        inputWeeks.setHint("总教学周数（默认16）");
        inputWeeks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(inputWeeks);

        builder.setView(container);

        // Use a mutable container for the selected date
        final long[] selectedDate = {0};

        builder.setNeutralButton("选择起始日期", (dialog, which) -> {
            // Don't dismiss; we show another dialog
        });

        builder.setPositiveButton("确定", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入学期名称", Toast.LENGTH_SHORT).show();
                return;
            }

            int weeks = 16;
            try {
                weeks = Integer.parseInt(inputWeeks.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            // If no date selected, use nearest past Monday
            if (selectedDate[0] == 0) {
                Calendar cal = Calendar.getInstance();
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                int daysSinceMon = (dow + 5) % 7;
                cal.add(Calendar.DAY_OF_MONTH, -daysSinceMon);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                selectedDate[0] = cal.getTimeInMillis();
            }

            Semester semester = new Semester();
            semester.setName(name);
            semester.setStartDate(selectedDate[0]);
            semester.setTotalWeeks(weeks);
            semester.setCurrentWeek(1);

            viewModel.addSemester(semester);
            Toast.makeText(requireContext(), "已添加 " + name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set up date picker on the neutral button
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    // Adjust to nearest Monday
                    int dow = c.get(Calendar.DAY_OF_WEEK);
                    int daysSinceMon = (dow + 5) % 7;
                    c.add(Calendar.DAY_OF_MONTH, -daysSinceMon);
                    selectedDate[0] = c.getTimeInMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (周一)", Locale.getDefault());
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("起始: " + sdf.format(new Date(selectedDate[0])));
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
