package com.example.healthapp;

import static org.junit.Assert.assertEquals;

import com.example.healthapp.common.HealthCalculations;

import org.junit.Test;

public class HealthCalculationsTest {
    @Test
    public void estimateExerciseCalories_usesMetFormula() {
        assertEquals(280.0,
                HealthCalculations.estimateExerciseCalories(8.0, 70.0, 30), 0.001);
    }
}
