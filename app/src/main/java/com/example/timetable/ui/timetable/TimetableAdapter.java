package com.example.timetable.ui.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timetable.data.model.Course;
import com.example.timetable.databinding.ItemTimetableRowBinding;
import com.example.timetable.util.WeekPatternUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.RowViewHolder> {

    // Each row: one period, with up to 7 courses (one per day, null if empty)
    private List<Course[]> gridData = new ArrayList<>();
    private OnCellClickListener listener;
    private int cellWidthPx = -1; // -1 表示未设置，使用 XML 默认宽度

    public interface OnCellClickListener {
        void onCellClick(int dayOfWeek, int period, Course existingCourse);
    }

    public TimetableAdapter(OnCellClickListener listener) {
        this.listener = listener;
    }

    public void setGridData(List<Course[]> gridData) {
        this.gridData = gridData;
        notifyDataSetChanged();
    }

    /**
     * 设置课表单元格宽度（px），由 Fragment 根据屏幕宽度动态计算
     * @param cellWidthPx 单元格宽度，传 -1 恢复 XML 默认
     */
    public void setCellWidth(int cellWidthPx) {
        this.cellWidthPx = cellWidthPx;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimetableRowBinding binding = ItemTimetableRowBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        int period = position + 1;
        holder.bind(period, gridData.get(position));
    }

    @Override
    public int getItemCount() {
        return gridData.size();
    }

    class RowViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimetableRowBinding binding;

        // Card arrays for each day (index 1-7 = Mon-Sun)
        private final MaterialCardView[] cards = new MaterialCardView[8];
        private final TextView[] nameViews = new TextView[8];
        private final TextView[] roomViews = new TextView[8];

        RowViewHolder(ItemTimetableRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Map day 1-7 to views
            cards[1] = binding.cardMon;  nameViews[1] = binding.tvMonName;  roomViews[1] = binding.tvMonRoom;
            cards[2] = binding.cardTue;  nameViews[2] = binding.tvTueName;  roomViews[2] = binding.tvTueRoom;
            cards[3] = binding.cardWed;  nameViews[3] = binding.tvWedName;  roomViews[3] = binding.tvWedRoom;
            cards[4] = binding.cardThu;  nameViews[4] = binding.tvThuName;  roomViews[4] = binding.tvThuRoom;
            cards[5] = binding.cardFri;  nameViews[5] = binding.tvFriName;  roomViews[5] = binding.tvFriRoom;
            cards[6] = binding.cardSat;  nameViews[6] = binding.tvSatName;  roomViews[6] = binding.tvSatRoom;
            cards[7] = binding.cardSun;  nameViews[7] = binding.tvSunName;  roomViews[7] = binding.tvSunRoom;
        }

        void bind(int period, Course[] dayCourses) {
            binding.tvPeriodLabel.setText("第" + period + "节\n" +
                WeekPatternUtils.getPeriodTime(itemView.getContext(), period));

            for (int day = 1; day <= 7; day++) {
                Course course = dayCourses[day];
                MaterialCardView card = cards[day];
                TextView nameView = nameViews[day];
                TextView roomView = roomViews[day];

                // 动态设置单元格宽度
                if (cellWidthPx > 0) {
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    lp.width = cellWidthPx;
                    card.setLayoutParams(lp);
                }

                if (course != null) {
                    card.setCardBackgroundColor(course.getColor());
                    nameView.setText(course.getName());
                    nameView.setVisibility(View.VISIBLE);
                    roomView.setText(course.getClassroom());
                    roomView.setVisibility(View.VISIBLE);
                    final Course c = course;
                    final int d = day;
                    final int p = period;
                    card.setOnClickListener(v -> listener.onCellClick(d, p, c));
                } else {
                    card.setCardBackgroundColor(0xFFF5F5F5);
                    nameView.setVisibility(View.GONE);
                    roomView.setVisibility(View.GONE);
                    final int d = day;
                    final int p = period;
                    card.setOnClickListener(v -> listener.onCellClick(d, p, null));
                }
            }
        }
    }
}
