package com.example.timetable.ui.settings;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.timetable.BuildConfig;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.FragmentSettingsBinding;
import com.example.timetable.R;
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
        setupWeekModeToggle();

        // 动态设置版本号
        binding.tvAppVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));

        binding.btnEditPeriodTimes.setOnClickListener(v -> showCourseTimeSettingsDialog());

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

        updateMinutesDisplay();
        binding.btnMinutesMinus.setOnClickListener(v -> {
            int current = PreferenceUtils.getReminderMinutes(requireContext());
            if (current > 5) {
                PreferenceUtils.setReminderMinutes(requireContext(), current - 5);
                updateMinutesDisplay();
            }
        });
        binding.btnMinutesPlus.setOnClickListener(v -> {
            int current = PreferenceUtils.getReminderMinutes(requireContext());
            if (current < 60) {
                PreferenceUtils.setReminderMinutes(requireContext(), current + 5);
                updateMinutesDisplay();
            }
        });
    }

    private void updateMinutesDisplay() {
        int minutes = PreferenceUtils.getReminderMinutes(requireContext());
        binding.tvMinutesDisplay.setText(String.format("%d 分钟", minutes));
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
                Toast.makeText(requireContext(), "主题色已保存，重启后生效", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), "显示模式已保存，请切换到课表页面查看", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteSemesterDialog() {
        if (allSemesters == null || allSemesters.isEmpty()) {
            Toast.makeText(requireContext(), "没有可删除的学期", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = binding.spinnerSemester.getSelectedItemPosition();
        if (pos < 0 || pos >= allSemesters.size()) return;
        Semester selected = allSemesters.get(pos);
        new AlertDialog.Builder(requireContext())
            .setTitle("删除学期")
            .setMessage(getString(R.string.confirm_delete_semester))
            .setPositiveButton("确定删除", (dialog, which) -> {
                viewModel.deleteSemester(selected.getId());
                Toast.makeText(requireContext(), "已删除 " + selected.getName(), Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 课程时间设置弹窗：设置上午/下午/晚课节数和每节课起止时间
     */
    private void showCourseTimeSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("课程时间设置");

        // 读取当前配置
        int[] counts = {
            PreferenceUtils.getMorningCount(requireContext()),
            PreferenceUtils.getAfternoonCount(requireContext()),
            PreferenceUtils.getEveningCount(requireContext())
        };
        String[] currentTimes = PreferenceUtils.getPeriodTimes(requireContext());

        // 可变的 period 时间列表
        final String[][] mutableTimes = {currentTimes.clone()};
        // 动态编辑框引用列表
        final java.util.ArrayList<EditText> startEditorList = new java.util.ArrayList<>();
        final java.util.ArrayList<EditText> endEditorList = new java.util.ArrayList<>();

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(24, 12, 24, 0);

        String[] sectionTitles = {"上午课程", "下午课程", "晚课"};
        String[] sectionLabels = {"上午", "下午", "晚课"};

        LinearLayout[] sectionTimeContainers = new LinearLayout[3];
        TextView[] countTextViews = new TextView[3];

        for (int sec = 0; sec < 3; sec++) {
            final int sectionIndex = sec;

            // 区块标题
            TextView secTitle = new TextView(requireContext());
            secTitle.setText(sectionTitles[sec]);
            secTitle.setTextSize(16);
            secTitle.setPadding(0, 12, 0, 4);
            rootLayout.addView(secTitle);

            // 数量调节行
            LinearLayout stepperRow = new LinearLayout(requireContext());
            stepperRow.setOrientation(LinearLayout.HORIZONTAL);
            stepperRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            com.google.android.material.button.MaterialButton btnMinus = new com.google.android.material.button.MaterialButton(requireContext());
            btnMinus.setText("−");
            btnMinus.setTextSize(18);
            btnMinus.setLayoutParams(new LinearLayout.LayoutParams(80, 80));

            TextView tvCount = new TextView(requireContext());
            tvCount.setText(String.valueOf(counts[sec]));
            tvCount.setTextSize(18);
            tvCount.setGravity(android.view.Gravity.CENTER);
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            countTextViews[sec] = tvCount;

            com.google.android.material.button.MaterialButton btnPlus = new com.google.android.material.button.MaterialButton(requireContext());
            btnPlus.setText("+");
            btnPlus.setTextSize(18);
            btnPlus.setLayoutParams(new LinearLayout.LayoutParams(80, 80));

            stepperRow.addView(btnMinus);
            stepperRow.addView(tvCount);
            stepperRow.addView(btnPlus);
            rootLayout.addView(stepperRow);

            // 节次时间编辑容器
            LinearLayout timeContainer = new LinearLayout(requireContext());
            timeContainer.setOrientation(LinearLayout.VERTICAL);
            timeContainer.setPadding(0, 4, 0, 4);
            sectionTimeContainers[sec] = timeContainer;
            rootLayout.addView(timeContainer);

            // 分隔线
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(0x33000000);
            rootLayout.addView(divider);

            btnMinus.setOnClickListener(v -> {
                if (counts[sectionIndex] <= 1) return;
                counts[sectionIndex]--;
                refreshSectionRows(counts, mutableTimes, sectionTimeContainers, countTextViews,
                    sectionLabels, startEditorList, endEditorList);
            });
            btnPlus.setOnClickListener(v -> {
                counts[sectionIndex]++;
                refreshSectionRows(counts, mutableTimes, sectionTimeContainers, countTextViews,
                    sectionLabels, startEditorList, endEditorList);
            });
        }

        // 初始渲染
        refreshSectionRows(counts, mutableTimes, sectionTimeContainers, countTextViews,
            sectionLabels, startEditorList, endEditorList);

        scrollView.addView(rootLayout);
        builder.setView(scrollView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            int total = counts[0] + counts[1] + counts[2];
            String[] newTimes = new String[total];
            for (int i = 0; i < total; i++) {
                String start = (i < startEditorList.size()) ? startEditorList.get(i).getText().toString().trim() : "";
                String end = (i < endEditorList.size()) ? endEditorList.get(i).getText().toString().trim() : "";
                if (start.isEmpty() || end.isEmpty() || !start.matches("\\d{2}:\\d{2}") || !end.matches("\\d{2}:\\d{2}")) {
                    newTimes[i] = (i < PreferenceUtils.DEFAULT_PERIOD_TIMES.length)
                        ? PreferenceUtils.DEFAULT_PERIOD_TIMES[i]
                        : String.format("%02d:00-%02d:45", 8 + i, 8 + i);
                } else {
                    newTimes[i] = start + "-" + end;
                }
            }
            PreferenceUtils.setMorningCount(requireContext(), counts[0]);
            PreferenceUtils.setAfternoonCount(requireContext(), counts[1]);
            PreferenceUtils.setEveningCount(requireContext(), counts[2]);
            PreferenceUtils.setPeriodTimes(requireContext(), newTimes);
            Toast.makeText(requireContext(), "课程时间已保存，请刷新课表查看", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 当任一区块节数变更时，重建所有区块的节次时间编辑行
     */
    private void refreshSectionRows(int[] counts, String[][] mutableTimes,
                                     LinearLayout[] containers, TextView[] countTextViews,
                                     String[] sectionLabels,
                                     java.util.ArrayList<EditText> startEditorList,
                                     java.util.ArrayList<EditText> endEditorList) {
        int newTotal = counts[0] + counts[1] + counts[2];
        String[] oldTimes = mutableTimes[0];
        String[] newTimes = new String[newTotal];

        for (int i = 0; i < newTotal; i++) {
            if (i < oldTimes.length) {
                newTimes[i] = oldTimes[i];
            } else {
                newTimes[i] = (i < PreferenceUtils.DEFAULT_PERIOD_TIMES.length)
                    ? PreferenceUtils.DEFAULT_PERIOD_TIMES[i]
                    : String.format("%02d:00-%02d:45", 8 + i, 8 + i);
            }
        }
        mutableTimes[0] = newTimes;
        startEditorList.clear();
        endEditorList.clear();

        int periodIdx = 0;
        for (int sec = 0; sec < 3; sec++) {
            countTextViews[sec].setText(String.valueOf(counts[sec]));
            LinearLayout container = containers[sec];
            container.removeAllViews();

            for (int local = 0; local < counts[sec]; local++) {
                int globalIdx = periodIdx + local;
                String label = sectionLabels[sec] + "第" + (local + 1) + "节";
                String timeStr = newTimes[globalIdx];
                String[] parts = timeStr.split("-");

                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, 2, 0, 2);

                TextView labelView = new TextView(requireContext());
                labelView.setText(label);
                labelView.setTextSize(12);
                labelView.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (75 * getResources().getDisplayMetrics().density),
                    LinearLayout.LayoutParams.WRAP_CONTENT));

                EditText etStart = new EditText(requireContext());
                etStart.setText(parts.length > 0 ? parts[0] : "08:00");
                etStart.setSingleLine(true);
                etStart.setTextSize(13);
                etStart.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                TextView sep = new TextView(requireContext());
                sep.setText("至");
                sep.setTextSize(13);
                sep.setPadding(4, 0, 4, 0);

                EditText etEnd = new EditText(requireContext());
                etEnd.setText(parts.length > 1 ? parts[1] : "08:45");
                etEnd.setSingleLine(true);
                etEnd.setTextSize(13);
                etEnd.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                row.addView(labelView);
                row.addView(etStart);
                row.addView(sep);
                row.addView(etEnd);
                container.addView(row);

                startEditorList.add(etStart);
                endEditorList.add(etEnd);
            }
            periodIdx += counts[sec];
        }
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
            // 根据日期自动计算当前周次
            long today = System.currentTimeMillis();
            long diff = today - selectedDate[0];
            int autoWeek = (int) (diff / (7L * 86400000L)) + 1;
            if (autoWeek < 1) autoWeek = 1;
            if (autoWeek > weeks) autoWeek = weeks;
            semester.setCurrentWeek(autoWeek);

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
