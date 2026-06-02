package com.example.timetable.ui.course;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.drawable.GradientDrawable;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.timetable.R;
import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.DialogCourseEditBinding;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.ColorUtils;
import com.example.timetable.util.PreferenceUtils;
import com.example.timetable.util.WeekPatternUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseEditDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_COURSE = "course";
    private static final String ARG_DAY = "day";
    private static final String ARG_PERIOD = "period";

    private DialogCourseEditBinding binding;
    private TimetableRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Course existingCourse;
    private int presetDay;
    private int presetPeriod;
    private int selectedColor;
    private String selectedPatternType = WeekPatternUtils.PRESET_ALL;
    private List<Integer> customWeeks = new ArrayList<>();

    public static CourseEditDialogFragment newInstance(@Nullable Course course, int dayOfWeek, int startPeriod) {
        CourseEditDialogFragment fragment = new CourseEditDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COURSE, course);
        args.putInt(ARG_DAY, dayOfWeek);
        args.putInt(ARG_PERIOD, startPeriod);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new TimetableRepository(
            AppDatabase.getInstance(requireContext()).courseDao(),
            AppDatabase.getInstance(requireContext()).semesterDao()
        );
        if (getArguments() != null) {
            existingCourse = (Course) getArguments().getSerializable(ARG_COURSE);
            presetDay = getArguments().getInt(ARG_DAY, 0);
            presetPeriod = getArguments().getInt(ARG_PERIOD, 0);
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
        binding = DialogCourseEditBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDaySpinner();
        setupPeriodPicker();
        setupDurationPicker();
        setupColorPicker();

        binding.btnSave.setOnClickListener(v -> saveCourse());
        binding.btnDelete.setOnClickListener(v -> deleteCourse());

        // Load semester data on background thread
        executor.execute(() -> {
            Semester semester = repository.getActiveSemesterSync();
            // Auto-assign color for new course (must be on bg thread)
            int autoColor = selectedColor;
            if (existingCourse == null && semester != null) {
                List<Course> courses = repository.getCoursesBySemesterSync(semester.getId());
                autoColor = ColorUtils.autoAssignColor(courses);
            }
            final int finalColor = autoColor;

            requireActivity().runOnUiThread(() -> {
                if (existingCourse == null) {
                    selectedColor = finalColor;
                }
                setupWeekChips(semester);

                if (existingCourse != null) {
                    fillExistingData(existingCourse, semester);
                    binding.btnDelete.setVisibility(View.VISIBLE);
                } else {
                    binding.btnDelete.setVisibility(View.GONE);
                    binding.chipAll.setChecked(true); // 新建课程默认选中"全部周"
                    if (presetDay > 0) binding.spinnerDay.setSelection(presetDay - 1);
                    if (presetPeriod > 0) {
                        binding.npStartPeriod.setValue(presetPeriod);
                    }
                }
            });
        });
    }

    private void setupDaySpinner() {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, days);
        binding.spinnerDay.setAdapter(adapter);
    }

    private void setupPeriodPicker() {
        int totalPeriods = PreferenceUtils.getTotalPeriodCount(requireContext());
        binding.npStartPeriod.setMinValue(1);
        binding.npStartPeriod.setMaxValue(totalPeriods);
        binding.npStartPeriod.setValue(1);
    }

    private void setupDurationPicker() {
        binding.npDuration.setMinValue(1);
        binding.npDuration.setMaxValue(4);
        binding.npDuration.setValue(1);
    }

    private void setupWeekChips(Semester semester) {
        binding.chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPatternType = WeekPatternUtils.PRESET_ALL;
                binding.customWeekGroup.setVisibility(View.GONE);
            }
        });
        binding.chipOdd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPatternType = WeekPatternUtils.PRESET_ODD;
                binding.customWeekGroup.setVisibility(View.GONE);
            }
        });
        binding.chipEven.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPatternType = WeekPatternUtils.PRESET_EVEN;
                binding.customWeekGroup.setVisibility(View.GONE);
            }
        });
        binding.chipCustom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPatternType = WeekPatternUtils.PRESET_CUSTOM;
                binding.customWeekGroup.setVisibility(View.VISIBLE);
            }
        });

        int totalWeeks = semester != null ? semester.getTotalWeeks() : 16;
        binding.customWeekGroup.removeAllViews();
        for (int i = 1; i <= totalWeeks; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(String.valueOf(i));
            chip.setCheckable(true);
            chip.setTag(i);
            binding.customWeekGroup.addView(chip);
        }
    }

    private void setupColorPicker() {
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (30 * density);
        int margin = (int) (5 * density);

        // 默认选中第一个颜色
        selectedColor = (existingCourse != null) ? existingCourse.getColor() : ColorUtils.PREDEFINED_COLORS[0];

        for (int color : ColorUtils.PREDEFINED_COLORS) {
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke((int) (1.5f * density), 0x33000000);

            View dot = new View(requireContext());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(dotSize, dotSize);
            params.setMargins(margin, margin, margin, margin);
            dot.setLayoutParams(params);
            dot.setBackground(drawable);
            dot.setElevation(2 * density);

            dot.setOnClickListener(v -> {
                selectedColor = color;
                // 更新选中态
                for (int i = 0; i < binding.colorPicker.getChildCount(); i++) {
                    View child = binding.colorPicker.getChildAt(i);
                    android.graphics.drawable.GradientDrawable gd =
                        (android.graphics.drawable.GradientDrawable) child.getBackground();
                    if (ColorUtils.PREDEFINED_COLORS[i] == color) {
                        gd.setStroke((int) (3 * density), 0xFFFFFFFF);
                    } else {
                        gd.setStroke((int) (1.5f * density), 0x33000000);
                    }
                }
            });
            binding.colorPicker.addView(dot);
        }

        // 初始选中态
        binding.colorPicker.post(() -> {
            for (int i = 0; i < binding.colorPicker.getChildCount(); i++) {
                if (ColorUtils.PREDEFINED_COLORS[i] == selectedColor) {
                    View child = binding.colorPicker.getChildAt(i);
                    android.graphics.drawable.GradientDrawable gd =
                        (android.graphics.drawable.GradientDrawable) child.getBackground();
                    gd.setStroke((int) (3 * density), 0xFFFFFFFF);
                    break;
                }
            }
        });
    }

    private void fillExistingData(Course course, Semester semester) {
        binding.etName.setText(course.getName());
        binding.etTeacher.setText(course.getTeacher());
        binding.etClassroom.setText(course.getClassroom());
        binding.spinnerDay.setSelection(course.getDayOfWeek() - 1);
        binding.npStartPeriod.setValue(course.getStartPeriod());
        binding.npDuration.setValue(course.getDuration());
        binding.etNotes.setText(course.getNotes());
        selectedColor = course.getColor();

        List<Integer> pattern = course.getWeekPattern();
        int totalWeeks = semester != null ? semester.getTotalWeeks() : 16;
        List<Integer> all = WeekPatternUtils.generatePattern(totalWeeks, WeekPatternUtils.PRESET_ALL, null);
        List<Integer> odd = WeekPatternUtils.generatePattern(totalWeeks, WeekPatternUtils.PRESET_ODD, null);
        List<Integer> even = WeekPatternUtils.generatePattern(totalWeeks, WeekPatternUtils.PRESET_EVEN, null);

        if (pattern.equals(all)) {
            binding.chipAll.setChecked(true);
        } else if (pattern.equals(odd)) {
            binding.chipOdd.setChecked(true);
        } else if (pattern.equals(even)) {
            binding.chipEven.setChecked(true);
        } else {
            binding.chipCustom.setChecked(true);
            customWeeks = new ArrayList<>(pattern);
            for (int i = 0; i < binding.customWeekGroup.getChildCount(); i++) {
                Chip chip = (Chip) binding.customWeekGroup.getChildAt(i);
                if (pattern.contains(chip.getTag())) {
                    chip.setChecked(true);
                }
            }
        }
    }

    private void saveCourse() {
        String name = binding.etName.getText().toString().trim();
        String classroom = binding.etClassroom.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "请输入课程名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (classroom.isEmpty()) {
            Toast.makeText(requireContext(), "请输入上课地点", Toast.LENGTH_SHORT).show();
            return;
        }

        // 在后台线程构建课程数据并检测冲突
        executor.execute(() -> {
            Semester semester = repository.getActiveSemesterSync();
            if (semester == null) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "请先在设置中创建学期", Toast.LENGTH_SHORT).show());
                return;
            }

            List<Integer> finalWeeks;
            if (WeekPatternUtils.PRESET_CUSTOM.equals(selectedPatternType)) {
                finalWeeks = new ArrayList<>();
                for (int i = 0; i < binding.customWeekGroup.getChildCount(); i++) {
                    Chip chip = (Chip) binding.customWeekGroup.getChildAt(i);
                    if (chip.isChecked()) {
                        finalWeeks.add((Integer) chip.getTag());
                    }
                }
                if (finalWeeks.isEmpty()) {
                    requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "请选择至少一周", Toast.LENGTH_SHORT).show());
                    return;
                }
            } else {
                finalWeeks = WeekPatternUtils.generatePattern(semester.getTotalWeeks(), selectedPatternType, null);
            }

            // 构建待保存的课程对象（先不入库）
            Course course = existingCourse != null ? existingCourse : new Course();
            course.setName(name);
            course.setTeacher(binding.etTeacher.getText().toString().trim());
            course.setClassroom(classroom);
            course.setDayOfWeek(binding.spinnerDay.getSelectedItemPosition() + 1);
            course.setStartPeriod(binding.npStartPeriod.getValue());
            course.setDuration(binding.npDuration.getValue());
            course.setWeekPattern(finalWeeks);
            course.setColor(selectedColor);
            course.setSemesterId(semester.getId());
            course.setNotes(binding.etNotes.getText().toString().trim());

            // 检测时间冲突
            List<Course> allCourses = repository.getCoursesBySemesterSync(semester.getId());
            List<Course> conflicts = findConflicts(course, allCourses);

            if (!conflicts.isEmpty()) {
                String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                Course conflict = conflicts.get(0);
                int confEnd = conflict.getStartPeriod() + conflict.getDuration() - 1;
                String msg = getString(R.string.course_conflict_message,
                    course.getName(),
                    conflict.getName(),
                    dayNames[conflict.getDayOfWeek()],
                    conflict.getStartPeriod(),
                    confEnd);

                requireActivity().runOnUiThread(() ->
                    new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.course_conflict_title)
                        .setMessage(msg)
                        .setPositiveButton("仍然保存", (d, w) -> executor.execute(() -> {
                            doSaveCourse(course);
                            requireActivity().runOnUiThread(this::dismiss);
                        }))
                        .setNegativeButton("取消", null)
                        .show());
            } else {
                doSaveCourse(course);
                requireActivity().runOnUiThread(this::dismiss);
            }
        });
    }

    /**
     * 检测新课程与已有课程列表的时间冲突
     * 冲突条件：同一天 + 节次范围重叠 + 周次有交集
     */
    private List<Course> findConflicts(Course newCourse, List<Course> allCourses) {
        List<Course> result = new ArrayList<>();
        int newDay = newCourse.getDayOfWeek();
        int newStart = newCourse.getStartPeriod();
        int newEnd = newStart + newCourse.getDuration(); // 不含结束节次
        List<Integer> newWeeks = newCourse.getWeekPattern();

        for (Course existing : allCourses) {
            // 编辑模式：跳过自身
            if (existingCourse != null && existing.getId() == existingCourse.getId()) continue;
            // 必须同一天
            if (existing.getDayOfWeek() != newDay) continue;
            // 节次范围是否重叠
            int exStart = existing.getStartPeriod();
            int exEnd = exStart + existing.getDuration();
            if (newStart >= exEnd || newEnd <= exStart) continue;
            // 周次是否有交集
            if (Collections.disjoint(newWeeks, existing.getWeekPattern())) continue;

            result.add(existing);
        }
        return result;
    }

    /**
     * 执行实际的入库操作（在后台线程调用）
     */
    private void doSaveCourse(Course course) {
        if (existingCourse != null) {
            repository.updateCourse(course);
        } else {
            repository.insertCourse(course);
        }
    }

    private void deleteCourse() {
        if (existingCourse != null) {
            repository.deleteCourse(existingCourse);
        }
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
