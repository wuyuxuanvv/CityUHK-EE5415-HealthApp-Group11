package com.example.healthapp.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;

public class ExerciseWeekDatesTest {
    @Test
    public void mondayAllowsOnlyMonday() {
        LocalDate today = LocalDate.of(2026, 3, 2);
        assertTrue(ExerciseWeekDates.isSelectable("2026-03-02", today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-01", today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-03", today));
    }

    @Test
    public void wednesdayAllowsMondayThroughWednesday() {
        LocalDate today = LocalDate.of(2026, 3, 4);
        assertTrue(ExerciseWeekDates.isSelectable("2026-03-02", today));
        assertTrue(ExerciseWeekDates.isSelectable("2026-03-03", today));
        assertTrue(ExerciseWeekDates.isSelectable("2026-03-04", today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-05", today));
    }

    @Test
    public void sundayAllowsWholeWeek() {
        LocalDate today = LocalDate.of(2026, 3, 8);
        for (int day = 2; day <= 8; day++) {
            assertTrue(ExerciseWeekDates.isSelectable(
                    String.format("2026-03-%02d", day), today));
        }
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-01", today));
    }

    @Test
    public void spansMonthAndYearBoundaries() {
        assertEquals(LocalDate.of(2026, 3, 30),
                ExerciseWeekDates.mondayOf(LocalDate.of(2026, 4, 2)));
        assertTrue(ExerciseWeekDates.isSelectable("2026-03-30",
                LocalDate.of(2026, 4, 2)));
        assertEquals(LocalDate.of(2025, 12, 29),
                ExerciseWeekDates.mondayOf(LocalDate.of(2026, 1, 1)));
        assertTrue(ExerciseWeekDates.isSelectable("2025-12-29",
                LocalDate.of(2026, 1, 1)));
        assertFalse(ExerciseWeekDates.isSelectable("2025-12-28",
                LocalDate.of(2026, 1, 1)));
    }

    @Test
    public void rejectsMalformedOrNonexistentDates() {
        LocalDate today = LocalDate.of(2026, 3, 4);
        assertFalse(ExerciseWeekDates.isSelectable(null, today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-4", today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-02-30", today));
        assertFalse(ExerciseWeekDates.isSelectable("2026-03-04T00:00:00Z", today));
    }
}
