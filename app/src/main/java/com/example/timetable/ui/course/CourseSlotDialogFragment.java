package com.example.timetable.ui.course;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.timetable.R;
import com.example.timetable.data.AppExecutors;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.DialogCourseSlotBinding;
import com.example.timetable.databinding.ItemSlotCourseBinding;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.WeekPatternUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 课表时段二级页面：点击课表单元格后弹出，展示该时段所有课程
 */
public class CourseSlotDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_DAY = "day";
    private static final String ARG_PERIOD = "period";

    private DialogCourseSlotBinding binding;
    private TimetableRepository repository;
    private final ExecutorService executor = AppExecutors.getInstance().diskIO();
    private int dayOfWeek;
    private int period;

    public static CourseSlotDialogFragment newInstance(int dayOfWeek, int period) {
        CourseSlotDialogFragment fragment = new CourseSlotDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DAY, dayOfWeek);
        args.putInt(ARG_PERIOD, period);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = TimetableRepository.getInstance(requireContext());
        if (getArguments() != null) {
            dayOfWeek = getArguments().getInt(ARG_DAY, 1);
            period = getArguments().getInt(ARG_PERIOD, 1);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogCourseSlotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        binding.tvSlotTitle.setText(dayNames[dayOfWeek] + " 第" + period + "节");

        executor.execute(() -> {
            Semester semester = repository.getActiveSemesterSync();
            if (semester == null) {
                requireActivity().runOnUiThread(() -> dismiss());
                return;
            }
            List<Course> dayCourses = repository.getCoursesBySemesterAndDaySync(
                semester.getId(), dayOfWeek);
            List<Course> matched = new ArrayList<>();
            for (Course c : dayCourses) {
                int endPeriod = c.getStartPeriod() + c.getDuration();
                if (period >= c.getStartPeriod() && period < endPeriod) {
                    matched.add(c);
                }
            }

            requireActivity().runOnUiThread(() -> {
                String timeStr = WeekPatternUtils.getPeriodTime(requireContext(), period);
                if (timeStr != null && !timeStr.isEmpty()) {
                    binding.tvSlotTime.setText(timeStr);
                } else {
                    binding.tvSlotTime.setVisibility(View.GONE);
                }

                int totalWeeks = semester.getTotalWeeks();
                binding.containerCourses.removeAllViews();
                if (matched.isEmpty()) {
                    binding.tvSlotEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvSlotEmpty.setVisibility(View.GONE);
                    for (Course course : matched) {
                        addCourseItem(course, totalWeeks);
                    }
                }
            });
        });

        binding.btnSlotAdd.setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            dismiss();
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(null, dayOfWeek, period);
            dialog.show(fm, "CourseEditDialog");
        });
    }

    private void addCourseItem(Course course, int totalWeeks) {
        ItemSlotCourseBinding itemBinding = ItemSlotCourseBinding.inflate(
            LayoutInflater.from(requireContext()), binding.containerCourses, false);

        itemBinding.vColorBar.setBackgroundColor(course.getColor());
        itemBinding.tvCourseName.setText(course.getName());

        StringBuilder info = new StringBuilder();
        if (course.getTeacher() != null && !course.getTeacher().isEmpty()) {
            info.append(course.getTeacher());
        }
        if (course.getClassroom() != null && !course.getClassroom().isEmpty()) {
            if (info.length() > 0) info.append(" · ");
            info.append(course.getClassroom());
        }
        int endPeriod = course.getStartPeriod() + course.getDuration() - 1;
        if (endPeriod > course.getStartPeriod()) {
            info.append(" · 第").append(course.getStartPeriod()).append("-").append(endPeriod).append("节");
        }
        itemBinding.tvCourseInfo.setText(info.toString());

        itemBinding.tvCourseWeeks.setText(WeekPatternUtils.formatForDisplay(course.getWeekPattern(), totalWeeks));

        itemBinding.getRoot().setOnClickListener(v -> {
            androidx.fragment.app.FragmentManager fm = getParentFragmentManager();
            dismiss();
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(course, dayOfWeek, period);
            dialog.show(fm, "CourseEditDialog");
        });

        binding.containerCourses.addView(itemBinding.getRoot());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
