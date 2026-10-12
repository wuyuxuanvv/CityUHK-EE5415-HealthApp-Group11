package com.example.healthapp.exercise;

import com.example.healthapp.model.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** In-memory filtering for the exercise list returned by the repository. */
final class ExerciseSearchFilter {
    private ExerciseSearchFilter() {
    }

    static List<Exercise> filter(List<Exercise> exercises, String rawQuery) {
        return filter(exercises, rawQuery, exercise -> exercise.name);
    }

    static List<Exercise> filter(List<Exercise> exercises, String rawQuery,
                                 Function<Exercise, String> displayNameFor) {
        List<Exercise> filtered = new ArrayList<>();
        if (exercises == null) {
            return filtered;
        }
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        for (Exercise exercise : exercises) {
            if (exercise == null || exercise.name == null) {
                continue;
            }
            String displayName = displayNameFor.apply(exercise);
            if (query.isEmpty()
                    || exercise.name.toLowerCase(Locale.ROOT).contains(query)
                    || (displayName != null
                    && displayName.toLowerCase(Locale.ROOT).contains(query))) {
                filtered.add(exercise);
            }
        }
        // Keep the database's order for other sports, but show strength as one low-to-high group.
        filtered.sort((left, right) -> {
            boolean leftStrength = Exercise.CATEGORY_STRENGTH.equals(left.category);
            boolean rightStrength = Exercise.CATEGORY_STRENGTH.equals(right.category);
            if (leftStrength != rightStrength) {
                return leftStrength ? 1 : -1;
            }
            return leftStrength
                    ? Integer.compare(strengthRank(left.name), strengthRank(right.name)) : 0;
        });
        return filtered;
    }

    private static int strengthRank(String name) {
        switch (name) {
            case "低强度":
                return 0;
            case "中等强度":
                return 1;
            case "高强度":
                return 2;
            default:
                return 3;
        }
    }
}
