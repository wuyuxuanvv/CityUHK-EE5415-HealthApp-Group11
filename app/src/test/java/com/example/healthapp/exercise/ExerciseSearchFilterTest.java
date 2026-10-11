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
