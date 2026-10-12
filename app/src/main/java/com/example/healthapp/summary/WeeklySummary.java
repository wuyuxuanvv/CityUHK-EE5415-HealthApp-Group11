package com.example.healthapp.summary;

import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.FoodRecord;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Derived from saved records only; never persisted as a second source of truth. */
final class WeeklySummary {
    final float[] minutes = new float[7];
    final float[] intake = new float[7];
    final float[] burn = new float[7];
    final boolean[] exerciseDays = new boolean[7];
    final boolean[] foodDays = new boolean[7];
    int totalMinutes;
    double totalIntake;
    double totalBurn;

    static WeeklySummary from(LocalDate monday, LocalDate today,
                              List<ExerciseRecord> exercises, List<FoodRecord> foods) {
        WeeklySummary result = new WeeklySummary();
        for (ExerciseRecord record : exercises) {
            int day = index(monday, today, record.recordDate);
            if (day < 0) continue;
            result.minutes[day] += record.durationMinutes;
            result.burn[day] += record.estimatedCaloriesKcal;
            result.exerciseDays[day] = true;
            result.totalMinutes += record.durationMinutes;
            result.totalBurn += record.estimatedCaloriesKcal;
        }
        for (FoodRecord record : foods) {
            int day = index(monday, today, record.recordDate);
            if (day < 0) continue;
            result.intake[day] += record.caloriesKcal;
            result.foodDays[day] = true;
            result.totalIntake += record.caloriesKcal;
        }
        return result;
    }

    int exerciseDayCount() { return count(exerciseDays); }
    int foodDayCount() { return count(foodDays); }
    private static int count(boolean[] days) {
        int count = 0;
        for (boolean day : days) if (day) count++;
        return count;
    }
    private static int index(LocalDate monday, LocalDate today, String date) {
        LocalDate day = LocalDate.parse(date);
        long index = ChronoUnit.DAYS.between(monday, day);
        return index >= 0 && index < 7 && !day.isAfter(today) ? (int) index : -1;
    }
}
