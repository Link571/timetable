package com.example.timetable.ui.timetable;

import com.example.timetable.data.model.Course;

/**
 * 课表网格中的一行数据，可以是普通节次行或午休/晚修分隔行
 */
public class GridItem {
    public static final int TYPE_PERIOD = 0; // 普通节次行
    public static final int TYPE_BREAK = 1;  // 午休/晚修分隔行

    public final int type;
    public final int period;       // 节次编号（1-based，仅 TYPE_PERIOD 有效）
    public final Course[] courses; // 7天课程数据（仅 TYPE_PERIOD 有效）
    public final String label;     // 分隔行标签文字，如"午休"（仅 TYPE_BREAK 有效）

    private GridItem(int type, int period, Course[] courses, String label) {
        this.type = type;
        this.period = period;
        this.courses = courses;
        this.label = label;
    }

    /** 创建普通节次行 */
    public static GridItem period(int period, Course[] courses) {
        return new GridItem(TYPE_PERIOD, period, courses, null);
    }

    /** 创建午休/晚修分隔行 */
    public static GridItem breakRow(String label) {
        return new GridItem(TYPE_BREAK, 0, null, label);
    }
}
