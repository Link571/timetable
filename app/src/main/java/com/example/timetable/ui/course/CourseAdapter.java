package com.example.timetable.ui.course;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timetable.data.model.Course;
import com.example.timetable.data.model.Semester;
import com.example.timetable.databinding.ItemCourseBinding;
import com.example.timetable.util.WeekPatternUtils;

public class CourseAdapter extends ListAdapter<Course, CourseAdapter.CourseViewHolder> {

    private final OnCourseClickListener listener;
    private int totalWeeks = 16;
    private Semester semester;
    private int currentWeek = 1;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
    }

    public CourseAdapter(OnCourseClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setTotalWeeks(int totalWeeks) {
        this.totalWeeks = totalWeeks;
    }

    public void setSemesterInfo(Semester semester, int currentWeek) {
        this.semester = semester;
        this.currentWeek = currentWeek;
    }

    private static final DiffUtil.ItemCallback<Course> DIFF_CALLBACK = new DiffUtil.ItemCallback<Course>() {
        @Override
        public boolean areItemsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Course oldItem, @NonNull Course newItem) {
            return oldItem.getName().equals(newItem.getName())
                && oldItem.getDayOfWeek() == newItem.getDayOfWeek()
                && oldItem.getStartPeriod() == newItem.getStartPeriod()
                && oldItem.getColor() == newItem.getColor();
        }
    };

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseBinding binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public Course getCourseAt(int position) {
        return getItem(position);
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private final ItemCourseBinding binding;

        CourseViewHolder(ItemCourseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course) {
            binding.tvCourseName.setText(course.getName());
            binding.tvTeacherRoom.setText(
                (course.getTeacher() != null && !course.getTeacher().isEmpty() ? course.getTeacher() : "") +
                "  " + course.getClassroom()
            );
            String dayInfo;
            if (semester != null) {
                dayInfo = WeekPatternUtils.getDateForDay(semester, currentWeek, course.getDayOfWeek());
            } else {
                dayInfo = WeekPatternUtils.getDayName(course.getDayOfWeek());
            }
            String timeInfo = dayInfo + " 第" + course.getStartPeriod() + "节";
            if (course.getDuration() > 1) {
                timeInfo += "-" + (course.getStartPeriod() + course.getDuration() - 1) + "节";
            }
            binding.tvTimeInfo.setText(timeInfo);

            binding.tvWeekInfo.setText(WeekPatternUtils.formatForDisplay(
                course.getWeekPattern(), totalWeeks));

            GradientDrawable dot = (GradientDrawable) binding.colorDot.getBackground();
            dot.setColor(course.getColor());

            itemView.setOnClickListener(v -> listener.onCourseClick(course));
        }
    }
}
