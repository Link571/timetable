package com.example.timetable.ui.settings;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.timetable.BuildConfig;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.FragmentSettingsBinding;
import com.example.timetable.R;
import com.example.timetable.util.ColorUtils;
import com.example.timetable.util.DateUtils;
import com.example.timetable.util.NotificationHelper;
import com.example.timetable.util.PreferenceUtils;

import java.util.Calendar;
import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
    private List<Semester> allSemesters;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        // 适配状态栏：给根布局添加顶部内边距
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupReminderSettings();
        setupThemeColorPicker();
        setupWeekModeToggle();

        // 动态设置版本号
        binding.tvAppVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        binding.btnEditPeriodTimes.setOnClickListener(v -> {
            new CourseTimeSettingsDialog().show(getParentFragmentManager(), "CourseTimeSettings");
        });

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
                setupWeekStepper(semester);
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
        binding.btnEditSemester.setOnClickListener(v -> showEditSemesterDialog());
        binding.btnDeleteSemester.setOnClickListener(v -> showDeleteSemesterDialog());
    }

    private void setupWeekStepper(Semester semester) {
        binding.tvWeekDisplay.setText(String.format("第%d周", semester.getCurrentWeek()));

        binding.btnWeekMinus.setOnClickListener(v -> {
            if (semester.getCurrentWeek() > 1) {
                viewModel.setCurrentWeek(semester.getId(), semester.getCurrentWeek() - 1);
            }
        });
        binding.btnWeekPlus.setOnClickListener(v -> {
            if (semester.getCurrentWeek() < semester.getTotalWeeks()) {
                viewModel.setCurrentWeek(semester.getId(), semester.getCurrentWeek() + 1);
            }
        });

        // 使用 DateUtils 统一计算自动周次
        int autoWeek = DateUtils.calculateCurrentWeek(semester.getStartDate(), semester.getTotalWeeks());
        binding.tvAutoWeek.setText(getString(R.string.auto_week_hint, autoWeek));
    }

    private void setupReminderSettings() {
        binding.switchReminder.setChecked(PreferenceUtils.isReminderEnabled(requireContext()));
        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceUtils.setReminderEnabled(requireContext(), isChecked);
            if (isChecked) {
                requestExactAlarmPermissionIfNeeded();
                viewModel.rescheduleReminders();
            } else {
                viewModel.cancelReminders();
            }
        });

        updateMinutesDisplay();
        binding.btnMinutesMinus.setOnClickListener(v -> {
            int current = PreferenceUtils.getReminderMinutes(requireContext());
            if (current > 5) {
                PreferenceUtils.setReminderMinutes(requireContext(), current - 5);
                updateMinutesDisplay();
                if (PreferenceUtils.isReminderEnabled(requireContext())) {
                    viewModel.rescheduleReminders();
                }
            }
        });
        binding.btnMinutesPlus.setOnClickListener(v -> {
            int current = PreferenceUtils.getReminderMinutes(requireContext());
            if (current < 60) {
                PreferenceUtils.setReminderMinutes(requireContext(), current + 5);
                updateMinutesDisplay();
                if (PreferenceUtils.isReminderEnabled(requireContext())) {
                    viewModel.rescheduleReminders();
                }
            }
        });
    }

    private void updateMinutesDisplay() {
        int minutes = PreferenceUtils.getReminderMinutes(requireContext());
        binding.tvMinutesDisplay.setText(getString(R.string.minutes_display, minutes));
    }

    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            || NotificationHelper.canScheduleExactAlarms(requireContext())) {
            return;
        }

        Toast.makeText(requireContext(), R.string.exact_alarm_permission_hint, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    private void setupThemeColorPicker() {
        int currentColor = PreferenceUtils.getThemeColor(requireContext());
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (32 * density);
        int margin = (int) (6 * density);

        for (int color : ColorUtils.PREDEFINED_COLORS) {
            // 创建圆形色块（带描边作为选中指示）
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(color);

            if (color == currentColor) {
                // 选中态：白色粗边框 + 轻微放大
                drawable.setStroke((int) (3 * density), 0xFFFFFFFF);
                drawable.setAlpha(255);
            } else {
                // 未选中：浅灰细边框
                drawable.setStroke((int) (1 * density), 0xDDDDDDDD);
            }

            View dot = new View(requireContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(dotSize, dotSize);
            params.setMargins(margin, margin, margin, margin);
            dot.setLayoutParams(params);
            dot.setBackground(drawable);
            dot.setElevation(2 * density);

            dot.setOnClickListener(v -> {
                PreferenceUtils.setThemeColor(requireContext(), color);
                Toast.makeText(requireContext(), R.string.theme_color_saved, Toast.LENGTH_SHORT).show();
                // 刷新所有色块样式
                for (int i = 0; i < binding.themeColorPicker.getChildCount(); i++) {
                    View child = binding.themeColorPicker.getChildAt(i);
                    android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) child.getBackground();
                    int childColor = ColorUtils.PREDEFINED_COLORS[i];
                    if (childColor == color) {
                        gd.setStroke((int) (3 * density), 0xFFFFFFFF);
                    } else {
                        gd.setStroke((int) (1 * density), 0xDDDDDDDD);
                    }
                }
            });
            binding.themeColorPicker.addView(dot);
        }
    }

    private void setupWeekModeToggle() {
        boolean isWeekend = PreferenceUtils.getWeekMode(requireContext()) == PreferenceUtils.WEEK_MODE_WEEKEND;
        binding.switchWeekendMode.setChecked(isWeekend);
        binding.switchWeekendMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? PreferenceUtils.WEEK_MODE_WEEKEND : PreferenceUtils.WEEK_MODE_NORMAL;
            PreferenceUtils.setWeekMode(requireContext(), mode);
            Toast.makeText(requireContext(), R.string.week_mode_saved, Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteSemesterDialog() {
        if (allSemesters == null || allSemesters.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_semester_to_delete, Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = binding.spinnerSemester.getSelectedItemPosition();
        if (pos < 0 || pos >= allSemesters.size()) return;
        Semester selected = allSemesters.get(pos);
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_semester)
            .setMessage(getString(R.string.confirm_delete_semester))
            .setPositiveButton(R.string.confirm_delete, (dialog, which) -> {
                viewModel.deleteSemester(selected.getId());
                Toast.makeText(requireContext(), getString(R.string.semester_deleted, selected.getName()), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showAddSemesterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.add_semester);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 0);

        final EditText inputName = new EditText(requireContext());
        inputName.setHint(R.string.semester_name_hint);
        container.addView(inputName);

        final EditText inputWeeks = new EditText(requireContext());
        inputWeeks.setHint(R.string.total_weeks_hint);
        inputWeeks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        container.addView(inputWeeks);

        builder.setView(container);

        // Use a mutable container for the selected date
        final long[] selectedDate = {0};

        builder.setNeutralButton(R.string.select_start_date, (dialog, which) -> {
            // Don't dismiss; we show another dialog
        });

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.semester_name_required, Toast.LENGTH_SHORT).show();
                return;
            }

            int weeks = 16;
            try {
                weeks = Integer.parseInt(inputWeeks.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            // 如果未选择日期，默认使用最近的周一作为起始日
            if (selectedDate[0] == 0) {
                selectedDate[0] = DateUtils.getNearestMonday(System.currentTimeMillis());
            }

            Semester semester = new Semester();
            semester.setName(name);
            semester.setStartDate(selectedDate[0]);
            semester.setTotalWeeks(weeks);
            // 使用 DateUtils 根据日期自动计算当前周次
            semester.setCurrentWeek(DateUtils.calculateCurrentWeek(selectedDate[0], weeks));

            viewModel.addSemester(semester);
            Toast.makeText(requireContext(), getString(R.string.semester_added, name), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 日期选择器：选中后自动对齐到最近的周一
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    // 使用 DateUtils 对齐到最近的周一
                    selectedDate[0] = DateUtils.getNearestMonday(c.getTimeInMillis());
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setText(getString(R.string.start_date_prefix, DateUtils.formatMondayDate(selectedDate[0])));
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });
    }

    /**
     * 编辑学期对话框：修改已有学期的名称、起始日期、总周数
     */
    private void showEditSemesterDialog() {
        if (allSemesters == null || allSemesters.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_semester_to_edit, Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = binding.spinnerSemester.getSelectedItemPosition();
        if (pos < 0 || pos >= allSemesters.size()) return;

        Semester selected = allSemesters.get(pos);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.edit_semester);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 16, 32, 0);

        final EditText inputName = new EditText(requireContext());
        inputName.setText(selected.getName());
        container.addView(inputName);

        final EditText inputWeeks = new EditText(requireContext());
        inputWeeks.setHint("总教学周数");
        inputWeeks.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputWeeks.setText(String.valueOf(selected.getTotalWeeks()));
        container.addView(inputWeeks);

        builder.setView(container);

        // 使用数组包装以支持闭包内修改
        final long[] editedDate = {selected.getStartDate()};

        builder.setNeutralButton(getString(R.string.start_date_prefix, DateUtils.formatMondayDate(editedDate[0])), (d, w) -> {});

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), R.string.semester_name_required, Toast.LENGTH_SHORT).show();
                return;
            }

            int weeks = selected.getTotalWeeks();
            try {
                weeks = Integer.parseInt(inputWeeks.getText().toString().trim());
            } catch (NumberFormatException ignored) {}
            if (weeks < 1) weeks = 1;

            // 根据新起始日期，使用 DateUtils 重新计算当前周次
            int autoWeek = DateUtils.calculateCurrentWeek(editedDate[0], weeks);

            selected.setName(name);
            selected.setStartDate(editedDate[0]);
            selected.setTotalWeeks(weeks);
            selected.setCurrentWeek(autoWeek);

            viewModel.updateSemester(selected);
            Toast.makeText(requireContext(), getString(R.string.semester_updated, name), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 日期选择器：选中后自动对齐到最近的周一
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(editedDate[0]);
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    // 使用 DateUtils 对齐到最近的周一
                    editedDate[0] = DateUtils.getNearestMonday(c.getTimeInMillis());
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setText(getString(R.string.start_date_prefix, DateUtils.formatMondayDate(editedDate[0])));
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
