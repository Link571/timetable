package com.example.timetable.ui.settings;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
                Toast.makeText(requireContext(), "主题色已保存", Toast.LENGTH_SHORT).show();
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
     * 使用 Material Slider 调节节数，点击时间按钮唤起 MaterialTimePicker 选择时分
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
        final String[][] mutableTimes = {currentTimes.clone()};

        // 时间按钮引用列表（按全局节次索引排列）
        final java.util.ArrayList<TextView> startBtnList = new java.util.ArrayList<>();
        final java.util.ArrayList<TextView> endBtnList = new java.util.ArrayList<>();

        float density = getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding((int)(28 * density), (int)(20 * density),
            (int)(28 * density), (int)(24 * density));

        String[] sectionTitles = {"上午课程", "下午课程", "晚课"};
        String[] sectionLabels = {"上午", "下午", "晚课"};

        LinearLayout[] sectionTimeContainers = new LinearLayout[3];

        for (int sec = 0; sec < 3; sec++) {
            final int sectionIndex = sec;

            // 区块标题
            TextView secTitle = new TextView(requireContext());
            secTitle.setText(sectionTitles[sec]);
            secTitle.setTextSize(17);
            secTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            secTitle.setTextColor(0xFF333333);
            secTitle.setPadding(0, (int)(12 * density), 0, (int)(8 * density));
            rootLayout.addView(secTitle);

            // 节数滑块行：标签 + Slider + 数量显示
            LinearLayout sliderRow = new LinearLayout(requireContext());
            sliderRow.setOrientation(LinearLayout.HORIZONTAL);
            sliderRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            sliderRow.setPadding(0, 0, 0, (int)(8 * density));

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText("节数");
            tvLabel.setTextSize(14);
            tvLabel.setTextColor(0xFF888888);
            sliderRow.addView(tvLabel);

            Slider slider = new Slider(requireContext());
            slider.setValueFrom(1);
            slider.setValueTo(10);
            slider.setStepSize(1);
            slider.setValue(counts[sec]);
            LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            sliderParams.setMargins((int)(12 * density), 0, (int)(12 * density), 0);
            slider.setLayoutParams(sliderParams);

            TextView tvCount = new TextView(requireContext());
            tvCount.setText(String.valueOf(counts[sec]));
            tvCount.setTextSize(16);
            tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
            tvCount.setTextColor(0xFF333333);
            tvCount.setGravity(android.view.Gravity.CENTER);
            tvCount.setMinWidth((int)(32 * density));
            sliderRow.addView(slider);
            sliderRow.addView(tvCount);
            rootLayout.addView(sliderRow);

            // 节次时间编辑容器
            LinearLayout timeContainer = new LinearLayout(requireContext());
            timeContainer.setOrientation(LinearLayout.VERTICAL);
            timeContainer.setPadding((int)(4 * density), (int)(4 * density),
                (int)(4 * density), (int)(4 * density));
            sectionTimeContainers[sec] = timeContainer;
            rootLayout.addView(timeContainer);

            // 区块间分隔线
            if (sec < 2) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int)(1 * density));
                divParams.setMargins(0, (int)(10 * density), 0, (int)(14 * density));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(0x1E000000);
                rootLayout.addView(divider);
            }

            slider.addOnChangeListener((s, value, fromUser) -> {
                int newCount = (int) value;
                tvCount.setText(String.valueOf(newCount));
                counts[sectionIndex] = newCount;
                // 重建前：将按钮文本同步回 mutableTimes，保留已选时间
                syncMutableTimes(mutableTimes, startBtnList, endBtnList);
                refreshSectionRows(counts, mutableTimes, sectionTimeContainers,
                    sectionLabels, startBtnList, endBtnList, density);
            });
        }

        // 初始渲染
        refreshSectionRows(counts, mutableTimes, sectionTimeContainers,
            sectionLabels, startBtnList, endBtnList, density);

        scrollView.addView(rootLayout);
        builder.setView(scrollView);

        builder.setPositiveButton("保存", (dialog, which) -> {
            int total = counts[0] + counts[1] + counts[2];
            String[] newTimes = new String[total];
            for (int i = 0; i < total; i++) {
                String start = startBtnList.get(i).getText().toString().trim();
                String end = endBtnList.get(i).getText().toString().trim();
                // 时间来自滚轮选择器，格式已保证正确，兜底使用默认值
                if (start.isEmpty() || end.isEmpty()) {
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

    /** 将时间按钮文本同步回 mutableTimes，保证节数变更时已修改的时间不丢失 */
    private void syncMutableTimes(String[][] mutableTimes,
                                  java.util.ArrayList<TextView> startBtnList,
                                  java.util.ArrayList<TextView> endBtnList) {
        int len = Math.min(mutableTimes[0].length, startBtnList.size());
        for (int i = 0; i < len; i++) {
            mutableTimes[0][i] = startBtnList.get(i).getText() + "-"
                + endBtnList.get(i).getText();
        }
    }

    /**
     * 当任一区块节数变更时，重建所有区块的节次时间编辑行
     * 时间录入改为可点击的 TextView 按钮，点击唤起 MaterialTimePicker
     */
    private void refreshSectionRows(int[] counts, String[][] mutableTimes,
                                     LinearLayout[] containers,
                                     String[] sectionLabels,
                                     java.util.ArrayList<TextView> startBtnList,
                                     java.util.ArrayList<TextView> endBtnList,
                                     float density) {
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
        startBtnList.clear();
        endBtnList.clear();

        int periodIdx = 0;
        for (int sec = 0; sec < 3; sec++) {
            LinearLayout container = containers[sec];
            container.removeAllViews();

            for (int local = 0; local < counts[sec]; local++) {
                final int globalIdx = periodIdx + local;
                String label = sectionLabels[sec] + "第" + (local + 1) + "节";
                String timeStr = newTimes[globalIdx];
                String[] parts = timeStr.split("-");
                final String startTime = parts.length > 0 ? parts[0] : "08:00";
                final String endTime = parts.length > 1 ? parts[1] : "08:45";

                // 行布局：标签 + 开始时间 + 分隔符 + 结束时间
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, (int)(4 * density), 0, (int)(4 * density));

                // 节次标签
                TextView labelView = new TextView(requireContext());
                labelView.setText(label);
                labelView.setTextSize(13);
                labelView.setTextColor(0xFF666666);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    (int)(80 * density), LinearLayout.LayoutParams.WRAP_CONTENT);
                labelView.setLayoutParams(labelParams);

                // 开始时间按钮（点击唤起时间选择器）
                TextView btnStart = createTimeButton(startTime, density);
                btnStart.setOnClickListener(v -> showTimePicker(btnStart, mutableTimes,
                    startBtnList, endBtnList));

                // 分隔符
                TextView sep = new TextView(requireContext());
                sep.setText("—");
                sep.setTextSize(14);
                sep.setTextColor(0xFFAAAAAA);
                sep.setGravity(android.view.Gravity.CENTER);
                sep.setPadding((int)(4 * density), 0, (int)(4 * density), 0);

                // 结束时间按钮
                TextView btnEnd = createTimeButton(endTime, density);
                btnEnd.setOnClickListener(v -> showTimePicker(btnEnd, mutableTimes,
                    startBtnList, endBtnList));

                row.addView(labelView);
                row.addView(btnStart);
                row.addView(sep);
                row.addView(btnEnd);
                container.addView(row);

                startBtnList.add(btnStart);
                endBtnList.add(btnEnd);
            }
            periodIdx += counts[sec];
        }
    }

    /** 创建时间显示按钮：浅圆角 + 浅灰边框 + 按压高亮反馈 */
    private TextView createTimeButton(String time, float density) {
        TextView btn = new TextView(requireContext());
        btn.setText(time);
        btn.setTextSize(14);
        btn.setTextColor(0xFF333333);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setClickable(true);
        btn.setFocusable(true);
        int pad = (int)(8 * density);
        btn.setPadding(pad, pad, pad, pad);

        // 普通态：白底 + 浅灰边框 + 圆角
        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setShape(GradientDrawable.RECTANGLE);
        normalBg.setCornerRadius(8 * density);
        normalBg.setColor(0xFFFFFFFF);
        normalBg.setStroke((int)(1 * density), 0xFFDDDDDD);

        // 按压态：浅灰底 + 深边框（高亮反馈）
        GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setShape(GradientDrawable.RECTANGLE);
        pressedBg.setCornerRadius(8 * density);
        pressedBg.setColor(0xFFF0F0F0);
        pressedBg.setStroke((int)(1.5f * density), 0xFFBBBBBB);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        sld.addState(new int[]{}, normalBg);
        btn.setBackground(sld);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btn.setLayoutParams(params);
        return btn;
    }

    /** 唤起 MaterialTimePicker（时钟滚轮模式），确认后回填时间到按钮并同步 mutableTimes */
    private void showTimePicker(TextView targetBtn, String[][] mutableTimes,
                                java.util.ArrayList<TextView> startBtnList,
                                java.util.ArrayList<TextView> endBtnList) {
        String current = targetBtn.getText().toString();
        int hour = 8, minute = 0;
        try {
            String[] hm = current.split(":");
            hour = Integer.parseInt(hm[0]);
            minute = Integer.parseInt(hm[1]);
        } catch (Exception ignored) {}

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
            .setTitleText("选择时间")
            .build();
        picker.addOnPositiveButtonClickListener(d -> {
            String newTime = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            targetBtn.setText(newTime);
            // 同步到 mutableTimes，确保节数变更时保留已选时间
            syncMutableTimes(mutableTimes, startBtnList, endBtnList);
        });
        picker.show(getParentFragmentManager(), "time_picker");
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

    /**
     * 编辑学期对话框：修改已有学期的名称、起始日期、总周数
     */
    private void showEditSemesterDialog() {
        if (allSemesters == null || allSemesters.isEmpty()) {
            Toast.makeText(requireContext(), "没有可编辑的学期", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = binding.spinnerSemester.getSelectedItemPosition();
        if (pos < 0 || pos >= allSemesters.size()) return;

        Semester selected = allSemesters.get(pos);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("编辑学期");

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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (周一)", Locale.getDefault());
        builder.setNeutralButton("起始: " + sdf.format(new Date(editedDate[0])), (d, w) -> {});

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入学期名称", Toast.LENGTH_SHORT).show();
                return;
            }

            int weeks = selected.getTotalWeeks();
            try {
                weeks = Integer.parseInt(inputWeeks.getText().toString().trim());
            } catch (NumberFormatException ignored) {}
            if (weeks < 1) weeks = 1;

            // 根据新起始日期重新计算当前周次
            long today = System.currentTimeMillis();
            long diff = today - editedDate[0];
            int autoWeek = (int) (diff / (7L * 86400000L)) + 1;
            if (autoWeek < 1) autoWeek = 1;
            if (autoWeek > weeks) autoWeek = weeks;

            selected.setName(name);
            selected.setStartDate(editedDate[0]);
            selected.setTotalWeeks(weeks);
            selected.setCurrentWeek(autoWeek);

            viewModel.updateSemester(selected);
            Toast.makeText(requireContext(), "已更新 " + name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // 日期选择器
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(editedDate[0]);
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, dayOfMonth, 0, 0, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    // 调整为最近的周一
                    int dow = c.get(Calendar.DAY_OF_WEEK);
                    int daysSinceMon = (dow + 5) % 7;
                    c.add(Calendar.DAY_OF_MONTH, -daysSinceMon);
                    editedDate[0] = c.getTimeInMillis();
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setText("起始: " + sdf.format(new Date(editedDate[0])));
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
