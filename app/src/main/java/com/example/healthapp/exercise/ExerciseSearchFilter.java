package com.example.healthapp.exercise;

import com.example.healthapp.model.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** In-memory filtering for the exercise list returned by the repository. */
final class ExerciseSearchFilter {
    private ExerciseSearchFilter() {
    }

    static List<Exercise> filter(List<Exercise> exercises, String rawQuery) {
        List<Exercise> filtered = new ArrayList<>();
        if (exercises == null) {
            return filtered;
        }
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        for (Exercise exercise : exercises) {
            if (exercise == null || exercise.name == null) {
                continue;
            }
            if (query.isEmpty()
                    || exercise.name.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(exercise);
            }
        }
        return filtered;
    }
}
