package com.example.timetable.util;

import java.util.List;
import com.example.timetable.data.model.Course;

public class ColorUtils {

    public static final int[] PREDEFINED_COLORS = {
        0xFFE53935, // Red
        0xFF1E88E5, // Blue
        0xFF43A047, // Green
        0xFFFB8C00, // Orange
        0xFF8E24AA, // Purple
        0xFF00ACC1, // Cyan
        0xFFD81B60, // Pink
        0xFF3949AB, // Indigo
        0xFF6D4C41, // Brown
        0xFF00897B, // Teal
        0xFFC0CA33, // Lime
        0xFF5E35B1, // Deep Purple
        0xFFF4511E, // Deep Orange
        0xFF039BE5, // Light Blue
        0xFF7CB342, // Light Green
        0xFFFFB300  // Amber
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
