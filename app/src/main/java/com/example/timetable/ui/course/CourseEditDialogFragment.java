package com.example.timetable.ui.course;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.timetable.data.database.AppDatabase;
import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.DialogCourseEditBinding;
import com.example.timetable.repository.TimetableRepository;
import com.example.timetable.util.ColorUtils;
import com.example.timetable.util.WeekPatternUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
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
        binding.npStartPeriod.setMinValue(1);
        binding.npStartPeriod.setMaxValue(12);
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
        for (int color : ColorUtils.PREDEFINED_COLORS) {
            View dot = new View(requireContext());
            int size = (int) (32 * getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(size, size);
            params.setMargins(4, 4, 4, 4);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(color);
            dot.setOnClickListener(v -> selectedColor = color);
            binding.colorPicker.addView(dot);
        }
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

            if (existingCourse != null) {
                repository.updateCourse(course);
            } else {
                repository.insertCourse(course);
            }

            requireActivity().runOnUiThread(this::dismiss);
        });
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
