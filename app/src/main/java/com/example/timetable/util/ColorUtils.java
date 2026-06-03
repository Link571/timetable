package com.example.timetable.util;

import java.util.List;
import com.example.timetable.data.model.Course;

public class ColorUtils {

    // 柔美化配色：兼顾视觉美感与白色文字对比度，每个颜色都经过精心挑选
    public static final int[] PREDEFINED_COLORS = {
        0xFFEF5350, // 柔和红 Coral Red
        0xFF42A5F5, // 天蓝 Sky Blue
        0xFF66BB6A, // 嫩绿 Mint Green
        0xFFFFA726, // 暖橙 Warm Orange
        0xFFAB47BC, // 雅紫 Lavender Purple
        0xFF26C6DA, // 亮青 Aqua Cyan
        0xFFEC407A, // 玫红 Rose Pink
        0xFF5C6BC0, // 淡靛 Periwinkle Indigo
        0xFF8D6E63, // 暖棕 Warm Taupe
        0xFF26A69A, // 柔青 Seafoam Teal
        0xFF7E57C2, // 深紫 Soft Violet
        0xFFFF7043, // 橘红 Coral Orange
        0xFF29B6F6, // 浅蓝 Ocean Blue
        0xFF9CCC65, // 草绿 Fresh Green
        0xFFD4E157, // 柠黄 Lemon Lime
        0xFFFFCA28  // 琥珀 Golden Amber
    };

    public static int autoAssignColor(List<Course> existingCourses) {
        if (existingCourses == null || existingCourses.isEmpty()) return PREDEFINED_COLORS[0];

        int[] usageCount = new int[PREDEFINED_COLORS.length];
        for (Course c : existingCourses) {
            int color = c.getColor();
            for (int i = 0; i < PREDEFINED_COLORS.length; i++) {
                if (PREDEFINED_COLORS[i] == color) {
                    usageCount[i]++;
                    break;
                }
            }
        }

        int minIndex = 0;
        for (int i = 1; i < usageCount.length; i++) {
            if (usageCount[i] < usageCount[minIndex]) minIndex = i;
        }
        return PREDEFINED_COLORS[minIndex];
    }

    public static int getColorIndex(int color) {
        for (int i = 0; i < PREDEFINED_COLORS.length; i++) {
            if (PREDEFINED_COLORS[i] == color) return i;
        }
        return 0;
    }
}
