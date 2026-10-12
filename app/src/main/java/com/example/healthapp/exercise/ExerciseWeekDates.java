package com.example.healthapp.exercise;

import androidx.annotation.Nullable;

import com.example.healthapp.common.DateTimeUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Local-calendar bounds for this week's exercise records. */
final class ExerciseWeekDates {
    private ExerciseWeekDates() {
    }

    static LocalDate mondayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    static boolean isSelectable(@Nullable String rawDate, LocalDate today) {
        if (rawDate == null) {
            return false;
        }
        try {
            LocalDate date = DateTimeUtils.parseDate(rawDate);
            return rawDate.equals(DateTimeUtils.formatDate(date))
                    && !date.isBefore(mondayOf(today))
                    && !date.isAfter(today);
        } catch (DateTimeParseException error) {
            return false;
        }
    }
}
