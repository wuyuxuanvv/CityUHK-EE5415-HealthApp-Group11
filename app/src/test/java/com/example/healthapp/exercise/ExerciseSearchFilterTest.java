package com.example.healthapp.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.healthapp.model.Exercise;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ExerciseSearchFilterTest {

    @Test
    public void filter_matchesChineseSubstring() {
        List<Exercise> result = ExerciseSearchFilter.filter(sampleExercises(), "走");

        assertEquals(2, result.size());
        assertEquals("散步走路", result.get(0).name);
        assertEquals("快走", result.get(1).name);
    }

    @Test
    public void filter_isCaseInsensitiveForEnglishNames() {
        List<Exercise> result = ExerciseSearchFilter.filter(sampleExercises(), "RUN");

        assertEquals(1, result.size());
        assertEquals("Morning Run", result.get(0).name);
    }

    @Test
    public void filter_emptyQueryReturnsAllDatabaseResults() {
        List<Exercise> exercises = sampleExercises();

        assertEquals(exercises.size(), ExerciseSearchFilter.filter(exercises, "  ").size());
    }

    @Test
    public void filter_groupsStrengthLowToHighAndMatchesDisplayedName() {
        Exercise high = exercise("高强度", 7.0);
        Exercise low = exercise("低强度", 3.5);
        Exercise middle = exercise("中等强度", 5.0);
        high.category = Exercise.CATEGORY_STRENGTH;
        low.category = Exercise.CATEGORY_STRENGTH;
        middle.category = Exercise.CATEGORY_STRENGTH;
        List<Exercise> exercises = Arrays.asList(high, exercise("快走", 5.0), middle, low);

        List<Exercise> all = ExerciseSearchFilter.filter(exercises, "",
                item -> displayName(item));
        assertEquals("快走", all.get(0).name);
        assertEquals("低强度", all.get(1).name);
        assertEquals("中等强度", all.get(2).name);
        assertEquals("高强度", all.get(3).name);

        List<Exercise> matching = ExerciseSearchFilter.filter(exercises, "力量训练",
                item -> displayName(item));
        assertEquals(3, matching.size());
        assertEquals("低强度", matching.get(0).name);
        assertEquals("中等强度", matching.get(1).name);
        assertEquals("高强度", matching.get(2).name);
        assertEquals(1, ExerciseSearchFilter.filter(exercises, "中等强度",
                item -> displayName(item)).size());
    }

    private static String displayName(Exercise exercise) {
        if (Exercise.CATEGORY_STRENGTH.equals(exercise.category)) {
            return "力量训练（" + exercise.name + "）";
        }
        return exercise.name;
    }

    @Test
    public void parseMinutes_rejectsInvalidBoundaries() {
        assertNull(ExerciseFragment.parseMinutes("0"));
        assertNull(ExerciseFragment.parseMinutes("601"));
        assertNull(ExerciseFragment.parseMinutes("abc"));
        assertEquals(Integer.valueOf(1), ExerciseFragment.parseMinutes("1"));
        assertEquals(Integer.valueOf(600), ExerciseFragment.parseMinutes("600"));
    }

    private static List<Exercise> sampleExercises() {
        return Arrays.asList(
                exercise("散步走路", 3.5),
                exercise("快走", 5.0),
                exercise("Morning Run", 9.3));
    }

    private static Exercise exercise(String name, double metValue) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.category = "测试";
        exercise.metValue = metValue;
        return exercise;
    }
}
