package com.example.healthapp.summary;

import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.FoodRecord;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class WeeklySummaryTest {
    @Test public void aggregatesMultipleRecordsAndExcludesFutureAndOtherWeeks() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        ExerciseRecord a = exercise("2026-10-12", 20, 100);
        ExerciseRecord b = exercise("2026-10-12", 10, 50);
        ExerciseRecord c = exercise("2026-10-13", 40, 200);
        FoodRecord f = food("2026-10-12", 500);
        WeeklySummary week = WeeklySummary.from(monday, monday.plusDays(1),
                Arrays.asList(a, b, c, exercise("2026-10-14", 90, 900),
                        exercise("2026-10-11", 90, 900)),
                Arrays.asList(f, food("2026-10-12", 300), food("2026-10-14", 1000)));
        assertEquals(70, week.totalMinutes);
        assertEquals(350, week.totalBurn, 0.001);
        assertEquals(800, week.totalIntake, 0.001);
        assertArrayEquals(new float[]{30, 40, 0, 0, 0, 0, 0}, week.minutes, 0.001f);
        assertEquals(2, week.exerciseDayCount());
        assertEquals(1, week.foodDayCount());
    }

    @Test public void emptyWeekAndZeroCalorieFoodRemainDistinct() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        WeeklySummary empty = WeeklySummary.from(monday, monday,
                Collections.emptyList(), Collections.emptyList());
        assertEquals(0, empty.foodDayCount());
        WeeklySummary logged = WeeklySummary.from(monday, monday,
                Collections.emptyList(), Collections.singletonList(food("2026-10-12", 0)));
        assertEquals(1, logged.foodDayCount());
        assertEquals(0, logged.totalIntake, 0);
    }

    private static ExerciseRecord exercise(String date, int minutes, double kcal) {
        ExerciseRecord result = new ExerciseRecord();
        result.recordDate = date;
        result.durationMinutes = minutes;
        result.estimatedCaloriesKcal = kcal;
        return result;
    }
    private static FoodRecord food(String date, double kcal) {
        FoodRecord result = new FoodRecord();
        result.recordDate = date;
        result.caloriesKcal = kcal;
        return result;
    }
}
