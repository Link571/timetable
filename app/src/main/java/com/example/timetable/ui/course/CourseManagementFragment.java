package com.example.timetable.ui.course;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timetable.data.model.Course;
import com.example.timetable.databinding.FragmentCourseManagementBinding;

public class CourseManagementFragment extends Fragment {

    private FragmentCourseManagementBinding binding;
    private CourseManagementViewModel viewModel;
    private CourseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCourseManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CourseManagementViewModel.class);

        adapter = new CourseAdapter(course -> {
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(course, 0, 0);
            dialog.show(getParentFragmentManager(), "CourseEditDialog");
        });
        binding.rvCourseList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCourseList.setAdapter(adapter);

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Course course = adapter.getCourseAt(viewHolder.getAdapterPosition());
                viewModel.deleteCourse(course);
            }
        }).attachToRecyclerView(binding.rvCourseList);

        binding.fabAddCourse.setOnClickListener(v -> {
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(null, 0, 0);
            dialog.show(getParentFragmentManager(), "CourseEditDialog");
        });

        viewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            adapter.submitList(courses);
            binding.tvEmpty.setVisibility(courses == null || courses.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getActiveSemester().observe(getViewLifecycleOwner(), semester -> {
            if (semester != null) {
                adapter.setTotalWeeks(semester.getTotalWeeks());
                adapter.setSemesterInfo(semester, semester.getCurrentWeek());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
