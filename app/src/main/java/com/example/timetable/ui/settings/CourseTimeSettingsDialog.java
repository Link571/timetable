package com.example.timetable.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.timetable.R;
import com.example.timetable.util.PreferenceUtils;
import com.google.android.material.slider.Slider;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;

/**
 * 课程时间设置弹窗
 * 负责设置每日上午/下午/晚课节数和每节课的起止时间
 * 从 SettingsFragment 中抽取，降低单一类的复杂度
 *
 * 使用时只需调用：
 * <pre>{@code
 *   new CourseTimeSettingsDialog().show(getParentFragmentManager(), "CourseTimeSettings");
 * }</pre>
 */
public class CourseTimeSettingsDialog extends DialogFragment {

    // 三个时段的节数配置
    private final int[] counts = new int[3];
    // 节次时间数组（可变，通过单元素数组实现闭包内修改）
    private final String[][] mutableTimes = new String[1][];
    // 时间按钮引用列表（按全局节次索引排列）
    private final ArrayList<TextView> startBtnList = new ArrayList<>();
    private final ArrayList<TextView> endBtnList = new ArrayList<>();
    // 每个时段的时间编辑行容器
    private final LinearLayout[] sectionTimeContainers = new LinearLayout[3];

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.course_time_title);

        // 读取当前配置
        counts[0] = PreferenceUtils.getMorningCount(context);
        counts[1] = PreferenceUtils.getAfternoonCount(context);
        counts[2] = PreferenceUtils.getEveningCount(context);
        String[] currentTimes = PreferenceUtils.getPeriodTimes(context);
        mutableTimes[0] = currentTimes.clone();

        float density = getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(context);
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding((int) (28 * density), (int) (20 * density),
            (int) (28 * density), (int) (24 * density));

        String[] sectionTitles = {
            getString(R.string.section_morning),
            getString(R.string.section_afternoon),
            getString(R.string.section_evening)
        };
        String[] sectionLabels = {"上午", "下午", "晚课"};

        for (int sec = 0; sec < 3; sec++) {
            final int sectionIndex = sec;

            // 区块标题
            TextView secTitle = new TextView(context);
            secTitle.setText(sectionTitles[sec]);
            secTitle.setTextSize(17);
            secTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            secTitle.setTextColor(0xFF333333);
            secTitle.setPadding(0, (int) (12 * density), 0, (int) (8 * density));
            rootLayout.addView(secTitle);

            // 节数滑块行：标签 + Slider + 数量显示
            LinearLayout sliderRow = createSliderRow(context, sec, density);
            rootLayout.addView(sliderRow);

            // 节次时间编辑容器
            LinearLayout timeContainer = new LinearLayout(context);
            timeContainer.setOrientation(LinearLayout.VERTICAL);
            timeContainer.setPadding((int) (4 * density), (int) (4 * density),
                (int) (4 * density), (int) (4 * density));
            sectionTimeContainers[sec] = timeContainer;
            rootLayout.addView(timeContainer);

            // 区块间分隔线
            if (sec < 2) {
                View divider = new View(context);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * density));
                divParams.setMargins(0, (int) (10 * density), 0, (int) (14 * density));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(0x1E000000);
                rootLayout.addView(divider);
            }
        }

        // 初始渲染时间行
        refreshSectionRows(density);

        scrollView.addView(rootLayout);
        builder.setView(scrollView);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            int total = counts[0] + counts[1] + counts[2];
            String[] newTimes = new String[total];
            for (int i = 0; i < total; i++) {
                String start = startBtnList.get(i).getText().toString().trim();
                String end = endBtnList.get(i).getText().toString().trim();
                if (start.isEmpty() || end.isEmpty()) {
                    newTimes[i] = (i < PreferenceUtils.DEFAULT_PERIOD_TIMES.length)
                        ? PreferenceUtils.DEFAULT_PERIOD_TIMES[i]
                        : String.format("%02d:00-%02d:45", 8 + i, 8 + i);
                } else {
                    newTimes[i] = start + "-" + end;
                }
            }
            PreferenceUtils.setMorningCount(context, counts[0]);
            PreferenceUtils.setAfternoonCount(context, counts[1]);
            PreferenceUtils.setEveningCount(context, counts[2]);
            PreferenceUtils.setPeriodTimes(context, newTimes);
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    /**
     * 创建节数滑块行（标签 + Slider + 数量显示）
     */
    private LinearLayout createSliderRow(Context context, int sectionIndex, float density) {
        LinearLayout sliderRow = new LinearLayout(context);
        sliderRow.setOrientation(LinearLayout.HORIZONTAL);
        sliderRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        sliderRow.setPadding(0, 0, 0, (int) (8 * density));

        TextView tvLabel = new TextView(context);
        tvLabel.setText(R.string.period_count_label);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(0xFF888888);
        sliderRow.addView(tvLabel);

        Slider slider = new Slider(context);
        slider.setValueFrom(1);
        slider.setValueTo(10);
        slider.setStepSize(1);
        slider.setValue(counts[sectionIndex]);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        sliderParams.setMargins((int) (12 * density), 0, (int) (12 * density), 0);
        slider.setLayoutParams(sliderParams);

        TextView tvCount = new TextView(context);
        tvCount.setText(String.valueOf(counts[sectionIndex]));
        tvCount.setTextSize(16);
        tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCount.setTextColor(0xFF333333);
        tvCount.setGravity(android.view.Gravity.CENTER);
        tvCount.setMinWidth((int) (32 * density));
        sliderRow.addView(slider);
        sliderRow.addView(tvCount);

        slider.addOnChangeListener((s, value, fromUser) -> {
            int newCount = (int) value;
            tvCount.setText(String.valueOf(newCount));
            counts[sectionIndex] = newCount;
            // 重建前同步按钮文本到 mutableTimes
            syncMutableTimes();
            refreshSectionRows(density);
        });

        return sliderRow;
    }

    /**
     * 将时间按钮文本同步回 mutableTimes，保证节数变更时已修改的时间不丢失
     */
    private void syncMutableTimes() {
        int len = Math.min(mutableTimes[0].length, startBtnList.size());
        for (int i = 0; i < len; i++) {
            mutableTimes[0][i] = startBtnList.get(i).getText() + "-"
                + endBtnList.get(i).getText();
        }
    }

    /**
     * 当任一区块节数变更时，重建所有区块的节次时间编辑行
     */
    private void refreshSectionRows(float density) {
        Context context = requireContext();
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

        String[] sectionLabels = {"上午", "下午", "晚课"};
        int periodIdx = 0;

        for (int sec = 0; sec < 3; sec++) {
            LinearLayout container = sectionTimeContainers[sec];
            container.removeAllViews();

            for (int local = 0; local < counts[sec]; local++) {
                final int globalIdx = periodIdx + local;
                String label = sectionLabels[sec] + "第" + (local + 1) + "节";
                String timeStr = newTimes[globalIdx];
                String[] parts = timeStr.split("-");
                final String startTime = parts.length > 0 ? parts[0] : "08:00";
                final String endTime = parts.length > 1 ? parts[1] : "08:45";

                // 行布局：标签 + 开始时间 + 分隔符 + 结束时间
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, (int) (4 * density), 0, (int) (4 * density));

                // 节次标签
                TextView labelView = new TextView(context);
                labelView.setText(label);
                labelView.setTextSize(13);
                labelView.setTextColor(0xFF666666);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    (int) (80 * density), LinearLayout.LayoutParams.WRAP_CONTENT);
                labelView.setLayoutParams(labelParams);

                // 开始时间按钮
                TextView btnStart = createTimeButton(context, startTime, density);
                btnStart.setOnClickListener(v -> showTimePicker(btnStart));

                // 分隔符
                TextView sep = new TextView(context);
                sep.setText("—");
                sep.setTextSize(14);
                sep.setTextColor(0xFFAAAAAA);
                sep.setGravity(android.view.Gravity.CENTER);
                sep.setPadding((int) (4 * density), 0, (int) (4 * density), 0);

                // 结束时间按钮
                TextView btnEnd = createTimeButton(context, endTime, density);
                btnEnd.setOnClickListener(v -> showTimePicker(btnEnd));

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

    /**
     * 创建时间显示按钮：浅圆角 + 浅灰边框 + 按压高亮反馈
     */
    private TextView createTimeButton(Context context, String time, float density) {
        TextView btn = new TextView(context);
        btn.setText(time);
        btn.setTextSize(14);
        btn.setTextColor(0xFF333333);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setClickable(true);
        btn.setFocusable(true);
        int pad = (int) (8 * density);
        btn.setPadding(pad, pad, pad, pad);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setShape(GradientDrawable.RECTANGLE);
        normalBg.setCornerRadius(8 * density);
        normalBg.setColor(0xFFFFFFFF);
        normalBg.setStroke((int) (1 * density), 0xFFDDDDDD);

        GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setShape(GradientDrawable.RECTANGLE);
        pressedBg.setCornerRadius(8 * density);
        pressedBg.setColor(0xFFF0F0F0);
        pressedBg.setStroke((int) (1.5f * density), 0xFFBBBBBB);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        sld.addState(new int[]{}, normalBg);
        btn.setBackground(sld);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btn.setLayoutParams(params);
        return btn;
    }

    /**
     * 唤起 MaterialTimePicker（时钟滚轮模式），确认后回填时间到按钮并同步 mutableTimes
     */
    private void showTimePicker(TextView targetBtn) {
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
            .setTitleText(getString(R.string.time_picker_title))
            .build();
        picker.addOnPositiveButtonClickListener(d -> {
            String newTime = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            targetBtn.setText(newTime);
            syncMutableTimes();
        });
        picker.show(getParentFragmentManager(), "time_picker");
    }
}
