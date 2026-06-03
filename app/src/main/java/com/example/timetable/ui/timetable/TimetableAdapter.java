package com.example.timetable.ui.timetable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timetable.data.model.Course;
import com.example.timetable.R;
import com.example.timetable.databinding.ItemTimetableBreakBinding;
import com.example.timetable.databinding.ItemTimetableRowBinding;
import com.example.timetable.util.WeekPatternUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<GridItem> gridData = new ArrayList<>();
    private OnCellClickListener listener;
    private int cellWidthPx = -1;
    private int cellHeightPx = -1;
    private int cellMarginPx = 0;
    private int weekMode = 0; // 0 = 周一至周五, 1 = 周一至周日

    public interface OnCellClickListener {
        void onCellClick(int dayOfWeek, int period, Course existingCourse);
    }

    public TimetableAdapter(OnCellClickListener listener) {
        this.listener = listener;
    }

    public void setGridData(List<GridItem> gridData) {
        this.gridData = gridData != null ? gridData : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCellWidth(int cellWidthPx) {
        this.cellWidthPx = cellWidthPx;
        notifyDataSetChanged();
    }

    public void setCellHeight(int cellHeightPx, int cellMarginPx) {
        this.cellHeightPx = cellHeightPx;
        this.cellMarginPx = cellMarginPx;
        notifyDataSetChanged();
    }

    public void setWeekMode(int weekMode) {
        this.weekMode = weekMode;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < gridData.size()) {
            return gridData.get(position).type;
        }
        return GridItem.TYPE_PERIOD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == GridItem.TYPE_BREAK) {
            ItemTimetableBreakBinding binding = ItemTimetableBreakBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new BreakViewHolder(binding);
        } else {
            ItemTimetableRowBinding binding = ItemTimetableRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new PeriodViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == GridItem.TYPE_BREAK) {
            ((BreakViewHolder) holder).bind(gridData.get(position));
        } else {
            ((PeriodViewHolder) holder).bind(gridData.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return gridData.size();
    }

    // ==================== 节次行 ViewHolder ====================

    class PeriodViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimetableRowBinding binding;
        private final MaterialCardView[] cards = new MaterialCardView[8];
        private final TextView[] nameViews = new TextView[8];
        private final TextView[] roomViews = new TextView[8];

        PeriodViewHolder(ItemTimetableRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            cards[1] = binding.cardMon;  nameViews[1] = binding.tvMonName;  roomViews[1] = binding.tvMonRoom;
            cards[2] = binding.cardTue;  nameViews[2] = binding.tvTueName;  roomViews[2] = binding.tvTueRoom;
            cards[3] = binding.cardWed;  nameViews[3] = binding.tvWedName;  roomViews[3] = binding.tvWedRoom;
            cards[4] = binding.cardThu;  nameViews[4] = binding.tvThuName;  roomViews[4] = binding.tvThuRoom;
            cards[5] = binding.cardFri;  nameViews[5] = binding.tvFriName;  roomViews[5] = binding.tvFriRoom;
            cards[6] = binding.cardSat;  nameViews[6] = binding.tvSatName;  roomViews[6] = binding.tvSatRoom;
            cards[7] = binding.cardSun;  nameViews[7] = binding.tvSunName;  roomViews[7] = binding.tvSunRoom;
        }

        void bind(GridItem item) {
            int period = item.period;
            Course[] dayCourses = item.courses;

            binding.tvPeriodLabel.setText("第" + period + "节\n" +
                WeekPatternUtils.getPeriodTime(itemView.getContext(), period));

            for (int day = 1; day <= 7; day++) {
                Course course = dayCourses != null ? dayCourses[day] : null;
                MaterialCardView card = cards[day];
                TextView nameView = nameViews[day];
                TextView roomView = roomViews[day];

                // 重置单元格为默认状态
                card.setTranslationZ(0f);
                card.setCardBackgroundColor(0xFFF5F5F5);
                nameView.setVisibility(View.GONE);
                roomView.setVisibility(View.GONE);
                card.setClickable(true);
                // 恢复默认高度和边距
                if (cellHeightPx > 0) {
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    lp.height = cellHeightPx;
                    if (lp instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) lp).bottomMargin = cellMarginPx;
                    }
                    card.setLayoutParams(lp);
                }
                // 动态设置单元格宽度
                if (cellWidthPx > 0) {
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    lp.width = cellWidthPx;
                    card.setLayoutParams(lp);
                }

                // 根据显示模式控制周六/周日列可见性
                if (day == 6 || day == 7) {
                    if (weekMode != 1) {
                        card.setVisibility(View.GONE);
                        continue;
                    }
                }
                card.setVisibility(View.VISIBLE);

                if (course != null) {
                    int spanRows = course.getDuration();
                    boolean isFirstRow = (course.getStartPeriod() == period);

                    if (isFirstRow) {
                        // 合并单元格的首行：正常渲染课程信息
                        card.setCardBackgroundColor(course.getColor());
                        nameView.setText(course.getName());
                        nameView.setVisibility(View.VISIBLE);
                        roomView.setText(course.getClassroom());
                        roomView.setVisibility(View.VISIBLE);

                        // 跨多行时，扩展卡片高度覆盖下方被合并的单元格
                        if (spanRows > 1 && cellHeightPx > 0) {
                            ViewGroup.LayoutParams lp = card.getLayoutParams();
                            lp.height = cellHeightPx * spanRows + 2 * cellMarginPx * (spanRows - 1);
                            // 用负底部边距抵消额外高度，防止当前行被撑高导致后续行下移
                            if (lp instanceof ViewGroup.MarginLayoutParams) {
                                int extraHeight = (spanRows - 1) * (cellHeightPx + 2 * cellMarginPx);
                                ((ViewGroup.MarginLayoutParams) lp).bottomMargin = cellMarginPx - extraHeight;
                            }
                            card.setLayoutParams(lp);
                            card.setTranslationZ(10f);
                        }

                        final Course c = course;
                        final int d = day;
                        final int p = period;
                        card.setOnClickListener(v -> listener.onCellClick(d, p, c));
                    } else {
                        // 被合并的单元格：隐藏，由上方首行卡片覆盖
                        card.setVisibility(View.INVISIBLE);
                        card.setClickable(false);
                    }
                } else {
                    // 空单元格：浅灰背景，点击可添加课程
                    final int d = day;
                    final int p = period;
                    card.setOnClickListener(v -> listener.onCellClick(d, p, null));
                }
            }
        }
    }

    // ==================== 午休/晚修分隔行 ViewHolder ====================

    class BreakViewHolder extends RecyclerView.ViewHolder {
        private final View breakRoot;
        private final TextView tvLabel;

        BreakViewHolder(ItemTimetableBreakBinding binding) {
            super(binding.getRoot());
            this.breakRoot = binding.breakRoot;
            this.tvLabel = binding.tvBreakLabel;
        }

        void bind(GridItem item) {
            tvLabel.setText(item.label);

            // 设置分隔行宽度与课表总宽度一致，使标签和装饰线居中横跨整行
            if (cellWidthPx > 0 && cellHeightPx > 0) {
                int labelWidthPx = (int) itemView.getResources()
                    .getDimension(R.dimen.timetable_period_label_width);
                int dayCount = (weekMode == 1) ? 7 : 5;
                int totalWidth = labelWidthPx + dayCount * (cellWidthPx + 2 * cellMarginPx);
                ViewGroup.LayoutParams lp = breakRoot.getLayoutParams();
                lp.width = totalWidth;
                breakRoot.setLayoutParams(lp);
            }
        }
    }
}
