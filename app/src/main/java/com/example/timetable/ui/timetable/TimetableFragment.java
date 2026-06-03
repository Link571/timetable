package com.example.timetable.ui.timetable;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    // 滑动切换周次的动画状态
    private boolean isWeekAnimating = false;
    private int pendingWeekDir = 0; // 1=下一周, -1=上一周
    // 防卡死：动画超时后强制重置
    private final Runnable animationTimeoutRunnable = () -> {
        if (isWeekAnimating) {
            isWeekAnimating = false;
            pendingWeekDir = 0;
            if (binding != null && binding.timetableContainer != null) {
                binding.timetableContainer.animate().cancel();
                binding.timetableContainer.setTranslationX(0f);
                binding.timetableContainer.setAlpha(1f);
                binding.timetableContainer.setScaleX(1f);
                binding.timetableContainer.setScaleY(1f);
            }
        }
    };
    // 手动触摸追踪（用于检测横向滑动切周）
    private float touchStartX = 0;
    private float touchStartY = 0;
    private boolean touchIsHorizontal = false;
    private static final int SWIPE_MIN_DISTANCE_DP = 60;  // 最小滑动距离（dp）

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTimetableBinding.inflate(inflater, container, false);
        // 统一应用主题色：状态栏安全区 → 周次导航栏 → 星期标题行，一体化视觉效果
        applyThemeColor();

        // 适配系统状态栏：给根布局添加顶部内边距，避免内容被状态栏遮挡
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
            // FAB 底部边距需要考虑系统导航栏高度
            FrameLayout.LayoutParams fabParams = (FrameLayout.LayoutParams) binding.fabAddCourse.getLayoutParams();
            int fabBaseMargin = (int) getResources().getDimension(R.dimen.fab_margin);
            fabParams.bottomMargin = fabBaseMargin + navBarHeight;
            binding.fabAddCourse.setLayoutParams(fabParams);
            return insets;
        });
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
        // 允许子 View 绘制超出自身边界（合并单元格需要）
        binding.rvTimetable.setClipChildren(false);
        binding.rvTimetable.setClipToPadding(false);

        // 根据屏幕宽度和显示模式动态计算课表单元格尺寸
        applyDynamicCellSizing();

        viewModel.getGridData().observe(getViewLifecycleOwner(), items -> {
            adapter.setGridData(items);
            // 检查是否有课程数据（仅检查节次行，忽略分隔行）
            boolean empty = true;
            if (items != null) {
                for (GridItem item : items) {
                    if (item.type == GridItem.TYPE_PERIOD && item.courses != null) {
                        for (int d = 1; d <= 7; d++) {
                            if (item.courses[d] != null) { empty = false; break; }
                        }
                    }
                    if (!empty) break;
                }
            }
            binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

            // 滑动动画第二阶段：新内容从对侧滑入 + 淡入 + 放大还原
            if (isWeekAnimating && pendingWeekDir != 0) {
                View timetableArea = binding.timetableContainer;
                if (timetableArea != null) {
                    // 移除防卡死超时回调
                    timetableArea.removeCallbacks(animationTimeoutRunnable);

                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    // 初始位置：从滑动方向的对侧 35% 处开始
                    timetableArea.setTranslationX(pendingWeekDir > 0 ? screenWidth * 0.35f : -screenWidth * 0.35f);
                    timetableArea.setAlpha(0f);
                    timetableArea.setScaleX(0.92f);
                    timetableArea.setScaleY(0.92f);

                    timetableArea.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(280)
                        .setInterpolator(new DecelerateInterpolator(2.0f))
                        .withEndAction(() -> {
                            isWeekAnimating = false;
                            pendingWeekDir = 0;
                        })
                        .start();
                } else {
                    isWeekAnimating = false;
                    pendingWeekDir = 0;
                }
            }
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

        // 左右滑动切换周次（手动触摸追踪，避免与 HorizontalScrollView 冲突）
        int swipeMinPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, SWIPE_MIN_DISTANCE_DP, getResources().getDisplayMetrics());

        binding.hsvTimetable.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getRawX();
                    touchStartY = event.getRawY();
                    touchIsHorizontal = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!touchIsHorizontal) {
                        float dx = Math.abs(event.getRawX() - touchStartX);
                        float dy = Math.abs(event.getRawY() - touchStartY);
                        // 横向位移明显大于纵向时，标记为横向滑动
                        if (dx > dy * 2f || (dx > swipeMinPx && dx > dy)) {
                            touchIsHorizontal = true;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    float endX = event.getRawX();
                    float endY = event.getRawY();
                    float totalDx = endX - touchStartX;
                    float totalDy = Math.abs(endY - touchStartY);

                    // 判断是否为有效的横向滑动切周手势
                    if (touchIsHorizontal && Math.abs(totalDx) > swipeMinPx
                        && Math.abs(totalDx) > totalDy) {
                        if (totalDx < 0) {
                            animateWeekChange(1);  // 左滑 → 下一周
                        } else {
                            animateWeekChange(-1); // 右滑 → 上一周
                        }
                        // 消费事件，阻止 HorizontalScrollView 的惯性滚动
                        return true;
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    touchIsHorizontal = false;
                    break;
            }
            return false;
        });

        // FAB 添加课程（不预设星期和节次）
        binding.fabAddCourse.setOnClickListener(v -> {
            CourseEditDialogFragment dialog = CourseEditDialogFragment.newInstance(null, 0, 0);
            dialog.show(getParentFragmentManager(), "CourseEditDialog");
        });
    }

    /**
     * 滑动切换周次的动画（两阶段：滑出旧内容 → 切换数据 → 滑入新内容）
     * @param direction 1=下一周（左滑）, -1=上一周（右滑）
     */
    private void animateWeekChange(int direction) {
        if (isWeekAnimating) return;

        Integer week = viewModel.getCurrentWeek().getValue();
        if (week == null) return;

        // 双向边界检查：防止在首/末周触发无效动画导致界面卡死
        if (direction < 0 && week <= 1) return;                       // 已是第一周，无法再往前
        if (direction > 0 && week >= viewModel.getTotalWeeks()) return; // 已是最后一周，无法再往后

        isWeekAnimating = true;
        pendingWeekDir = direction;

        View timetableArea = binding.timetableContainer;
        if (timetableArea == null) {
            // 降级：无动画直接切换
            if (direction > 0) viewModel.nextWeek();
            else viewModel.prevWeek();
            isWeekAnimating = false;
            pendingWeekDir = 0;
            return;
        }

        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // 防卡死保护：动画 1.5 秒后仍未结束则强制重置
        timetableArea.removeCallbacks(animationTimeoutRunnable);
        timetableArea.postDelayed(animationTimeoutRunnable, 1500);

        // 第一阶段：当前内容跟随手指方向滑出 + 淡出 + 轻微缩小（营造"卡片被推走"的层次感）
        timetableArea.animate()
            .translationX(direction > 0 ? -screenWidth * 0.35f : screenWidth * 0.35f)
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(180)
            .setInterpolator(new AccelerateInterpolator(1.2f))
            .withEndAction(() -> {
                // 切换周次数据（触发 LiveData → 观察者中执行第二阶段滑入动画）
                if (direction > 0) viewModel.nextWeek();
                else viewModel.prevWeek();
            })
            .start();
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

        // 传递单元格高度和边距给 Adapter，用于合并单元格高度计算
        int cellHeightPx = (int) (getResources().getDimension(R.dimen.timetable_cell_height));
        int cellMarginPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1.5f, getResources().getDisplayMetrics());
        adapter.setCellHeight(cellHeightPx, cellMarginPx);

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

    /**
     * 每次页面恢复时重新应用主题色，确保从设置页切回时立即生效
     */
    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && viewModel != null) {
            applyThemeColor();
            // 从设置页返回后刷新课表（节次数/课程时间等配置可能已变更）
            viewModel.refreshGrid();
        }
    }

    private void applyThemeColor() {
        int themeColor = PreferenceUtils.getThemeColor(requireContext());
        binding.getRoot().setBackgroundColor(themeColor);
        binding.weekNavBar.setBackgroundColor(themeColor);
        binding.headerRow.setBackgroundColor(themeColor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
