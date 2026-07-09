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

                // 重置为默认状态
                card.setTranslationZ(0f);
                card.setCardElevation(0f);
                card.setStrokeColor(0xFFE2E8F0);
                card.setStrokeWidth((int) itemView.getResources().getDisplayMetrics().density);
                card.setCardBackgroundColor(0xFFFFFFFF);
                nameView.setVisibility(View.GONE);
                roomView.setVisibility(View.GONE);
                card.setClickable(true);

                // 恢复默认尺寸
                applyCellSize(card, cellHeightPx, cellMarginPx, cellWidthPx);

                // 根据显示模式控制周六/周日列可见性
                if (day == 6 || day == 7) {
                    if (weekMode != 1) {
                        card.setVisibility(View.GONE);
                        continue;
                    }
                }
                card.setVisibility(View.VISIBLE);

                if (course != null) {
                    boolean isFirstRow = (course.getStartPeriod() == period);

                    if (isFirstRow) {
                        // 合并单元格的首行：正常渲染课程信息 + 扩展高度覆盖下方行
                        card.setCardElevation(3f * itemView.getResources().getDisplayMetrics().density);
                        card.setStrokeColor(0x26FFFFFF);
                        card.setCardBackgroundColor(course.getColor());
                        nameView.setText(course.getName());
                        nameView.setVisibility(View.VISIBLE);
                        roomView.setText(course.getClassroom());
                        roomView.setVisibility(View.VISIBLE);

                        int spanRows = course.getDuration();
                        if (spanRows > 1 && cellHeightPx > 0) {
                            expandCardForSpan(card, spanRows);
                        }

                        final Course c = course;
                        final int d = day;
                        final int p = period;
                        card.setOnClickListener(v -> listener.onCellClick(d, p, c));
                    } else {
                        // 被合并的单元格：隐藏，由上方首行卡片通过 clipChildren=false 覆盖显示
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

        /**
         * 扩展卡片高度以覆盖下方的 spanRows-1 行（合并单元格效果）
         * 使用 clipChildren=false（已在 RecyclerView 设置）允许卡片绘制超出自身行边界
         * 负 margin 用于抵消额外高度，避免当前行被撑高
         */
        private void expandCardForSpan(MaterialCardView card, int spanRows) {
            // 单格高度 + 双倍 margin（上下各一个）
            int singleUnit = cellHeightPx + 2 * cellMarginPx;
            // 总高度 = spanRows 个单元格 + 减去第一个的上margin(已包含在初始layout中)
            // 实际计算：总跨度高度 = spanRows * cellHeightPx + (spanRows-1)*2*cellMarginPx
            int totalSpanHeight = cellHeightPx * spanRows + 2 * cellMarginPx * (spanRows - 1);
            // 需要抵消的额外高度 = (spanRows-1)个完整单元（因为第一个单元的高度已经在默认layout中）
            int extraHeight = (spanRows - 1) * singleUnit;

            ViewGroup.LayoutParams lp = card.getLayoutParams();
            lp.height = totalSpanHeight;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).bottomMargin = cellMarginPx - extraHeight;
            }
            card.setLayoutParams(lp);
            card.setTranslationZ(10f);
        }

        /** 设置单元格的默认高度、边距和宽度 */
        private void applyCellSize(MaterialCardView card, int height, int margin, int width) {
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            if (height > 0) {
                lp.height = height;
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).bottomMargin = margin;
                }
            }
            if (width > 0) {
                lp.width = width;
            }
            card.setLayoutParams(lp);
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
