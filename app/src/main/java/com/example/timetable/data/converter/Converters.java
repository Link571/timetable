package com.example.timetable.data.converter;

import androidx.room.TypeConverter;
import java.util.ArrayList;
import java.util.List;

public class Converters {

    @TypeConverter
    public static String fromWeekList(List<Integer> weeks) {
        if (weeks == null || weeks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weeks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(weeks.get(i));
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<Integer> toWeekList(String value) {
        List<Integer> list = new ArrayList<>();
        if (value == null || value.isEmpty()) return list;
        for (String s : value.split(",")) {
            try {
                list.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }
}
