package com.example.timetable.ui.timetable;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.timetable.data.model.Course;
import com.example.timetable.databinding.FragmentTimetableBinding;
import com.example.timetable.R;
import com.example.timetable.ui.course.CourseEditDialogFragment;
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

        // Apply theme color
        int themeColor = PreferenceUtils.getThemeColor(requireContext());
        binding.weekIndicator.setBackgroundColor(themeColor);
        binding.headerRow.setBackgroundColor(themeColor);

        adapter = new TimetableAdapter((dayOfWeek, period, existingCourse) -> {
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(existingCourse, dayOfWeek, period);
            dialog.show(getParentFragmentManager(), "CourseEditDialog");
        });

        binding.rvTimetable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTimetable.setAdapter(adapter);
        binding.rvTimetable.setNestedScrollingEnabled(false);

        // 根据屏幕宽度动态计算课表单元格尺寸
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

        viewModel.getWeekInfo().observe(getViewLifecycleOwner(), info -> {
            binding.tvWeekInfo.setText(info);
        });

        binding.btnPrevWeek.setOnClickListener(v -> viewModel.prevWeek());
        binding.btnNextWeek.setOnClickListener(v -> viewModel.nextWeek());
    }

    /**
     * 根据屏幕宽度动态计算课表单元格宽度，使7天列尽量完整显示
     * 公式：cellWidth = (screenWidth - periodLabel - margins) / 7
     * 结果钳位在 minWidth ~ maxWidth 之间，防止过小或过大
     */
    private void applyDynamicCellSizing() {
        // 获取屏幕宽度（px）
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;

        // 从 dimens 资源读取限制值（dp → px）
        int minWidthPx = (int) (getResources().getDimension(R.dimen.timetable_cell_min_width));
        int maxWidthPx = (int) (getResources().getDimension(R.dimen.timetable_cell_max_width));
        int labelWidthPx = (int) (getResources().getDimension(R.dimen.timetable_period_label_width));

        // 每个单元格 margin 为 1dp（左右共 2dp），7 个单元格共 7 个 margin
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        // 总间距 = 7 个单元格 × 每格左右各 1dp margin = 14dp，加上可能的父容器边距
        int totalMarginsPx = marginPx * 14;

        // 计算单元格宽度（px）
        int cellWidthPx = (screenWidthPx - labelWidthPx - totalMarginsPx) / 7;

        // 钳位到合理范围
        if (cellWidthPx < minWidthPx) {
            cellWidthPx = minWidthPx;
        } else if (cellWidthPx > maxWidthPx) {
            cellWidthPx = maxWidthPx;
        }

        // 设置 Adapter 单元格宽度
        adapter.setCellWidth(cellWidthPx);

        // 同步调整表头行（周一~周日，跳过第一个空占位 TextView）
        LinearLayout headerRow = binding.headerRow;
        for (int i = 1; i < headerRow.getChildCount(); i++) {
            View headerCell = headerRow.getChildAt(i);
            ViewGroup.LayoutParams lp = headerCell.getLayoutParams();
            lp.width = cellWidthPx;
            headerCell.setLayoutParams(lp);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
