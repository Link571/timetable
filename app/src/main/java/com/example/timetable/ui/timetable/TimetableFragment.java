package com.example.timetable.ui.timetable;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.timetable.data.model.Course;
import com.example.timetable.databinding.FragmentTimetableBinding;
import com.example.timetable.R;
import com.example.timetable.ui.course.CourseEditDialogFragment;
import com.example.timetable.ui.course.CourseSlotDialogFragment;
import com.example.timetable.util.PreferenceUtils;

public class TimetableFragment extends Fragment {

    private FragmentTimetableBinding binding;
    private TimetableViewModel viewModel;
    private TimetableAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimetableBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TimetableViewModel.class);

        // 点击课表单元格 → 打开时段课程列表（二级页面）
        adapter = new TimetableAdapter((dayOfWeek, period, existingCourse) -> {
            CourseSlotDialogFragment dialog = CourseSlotDialogFragment.newInstance(dayOfWeek, period);
            dialog.show(getParentFragmentManager(), "CourseSlotDialog");
        });

        binding.rvTimetable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTimetable.setAdapter(adapter);
        binding.rvTimetable.setNestedScrollingEnabled(false);

        // 根据屏幕宽度和显示模式动态计算课表单元格尺寸
        applyDynamicCellSizing();

        viewModel.getGridData().observe(getViewLifecycleOwner(), rows -> {
            adapter.setGridData(rows);
            // Check if grid is empty
            boolean empty = true;
            if (rows != null) {
                for (Course[] row : rows) {
                    for (int d = 1; d <= 7; d++) {
                        if (row[d] != null) { empty = false; break; }
                    }
                    if (!empty) break;
                }
            }
            binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        // 当前周次文字
        viewModel.getWeekInfo().observe(getViewLifecycleOwner(), weekText -> {
            if (weekText != null && !weekText.isEmpty()) {
                binding.tvWeekInfo.setText(weekText);
                binding.tvWeekInfo.setVisibility(View.VISIBLE);
            } else {
                binding.tvWeekInfo.setVisibility(View.GONE);
            }
        });

        // 表头日期
        viewModel.getHeaderDates().observe(getViewLifecycleOwner(), dates -> {
            if (dates != null) {
                updateHeaderDates(dates);
            }
        });

        // FAB 添加课程（不预设星期和节次）
        binding.fabAddCourse.setOnClickListener(v -> {
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(null, 0, 0);
            dialog.show(getParentFragmentManager(), "CourseEditDialog");
        });
    }

    /**
     * 根据屏幕宽度和显示模式动态计算课表单元格宽度
     * 公式：cellWidth = (screenWidth - periodLabel - margins) / dayCount
     * 正常模式（周一至周五）：dayCount = 5
     * 周末模式（周一至周日）：dayCount = 7
     */
    private void applyDynamicCellSizing() {
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;

        int minWidthPx = (int) (getResources().getDimension(R.dimen.timetable_cell_min_width));
        int maxWidthPx = (int) (getResources().getDimension(R.dimen.timetable_cell_max_width));
        int labelWidthPx = (int) (getResources().getDimension(R.dimen.timetable_period_label_width));

        // 读取显示模式
        int weekMode = PreferenceUtils.getWeekMode(requireContext());
        int dayCount = (weekMode == PreferenceUtils.WEEK_MODE_WEEKEND) ? 7 : 5;

        // 设置 Adapter 显示模式（控制周六/周日列可见性）
        adapter.setWeekMode(weekMode);

        // 每个单元格 margin = 1dp，总间距 = dayCount × 2dp
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        int totalMarginsPx = marginPx * dayCount * 2;

        // 计算单元格宽度
        int cellWidthPx = (screenWidthPx - labelWidthPx - totalMarginsPx) / dayCount;

        // 钳位到合理范围
        if (cellWidthPx < minWidthPx) {
            cellWidthPx = minWidthPx;
        } else if (cellWidthPx > maxWidthPx) {
            cellWidthPx = maxWidthPx;
        }

        adapter.setCellWidth(cellWidthPx);

        // 同步调整表头行（跳过第一个空占位 TextView）
        LinearLayout headerRow = binding.headerRow;
        for (int i = 1; i < headerRow.getChildCount(); i++) {
            View headerCell = headerRow.getChildAt(i);
            ViewGroup.LayoutParams lp = headerCell.getLayoutParams();
            lp.width = cellWidthPx;
            headerCell.setLayoutParams(lp);
            // 周六（i=6）、周日（i=7）：正常模式隐藏，周末模式显示
            if (i == 6 || i == 7) {
                headerCell.setVisibility(weekMode == PreferenceUtils.WEEK_MODE_WEEKEND ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * 更新表头周一~周日下方的日期小字
     * @param dates 索引 1-7 为周一~周日的日期字符串，如 "6/2"
     */
    private void updateHeaderDates(String[] dates) {
        LinearLayout headerRow = binding.headerRow;
        for (int d = 1; d <= 7 && d < headerRow.getChildCount(); d++) {
            View dayLayout = headerRow.getChildAt(d);
            if (dayLayout instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) dayLayout;
                if (ll.getChildCount() >= 2 && ll.getChildAt(1) instanceof TextView) {
                    ((TextView) ll.getChildAt(1)).setText(dates[d] != null ? dates[d] : "");
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
