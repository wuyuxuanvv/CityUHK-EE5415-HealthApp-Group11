package com.example.healthapp.home.pet;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PetRewardSystemTest {
    @Test public void strictComparisonUsesActualKcal() {
        assertTrue(PetRewardSystem.isEligible(100, 100.01));
        assertFalse(PetRewardSystem.isEligible(100, 100));
        assertFalse(PetRewardSystem.isEligible(101, 100));
    }

    @Test public void emptyDayDoesNotGrantReward() {
        assertFalse(PetRewardSystem.isEligible(0, 0));
        assertFalse(PetRewardSystem.isEligible(200, 0));
        assertTrue(PetRewardSystem.isEligible(0, 200));
    }

    @Test public void invalidValuesCannotGrantReward() {
        assertFalse(PetRewardSystem.isEligible(-1, 100));
        assertFalse(PetRewardSystem.isEligible(0, -1));
        assertFalse(PetRewardSystem.isEligible(Double.NaN, 100));
        assertFalse(PetRewardSystem.isEligible(0, Double.NaN));
        assertFalse(PetRewardSystem.isEligible(0, Double.POSITIVE_INFINITY));
        assertFalse(PetRewardSystem.isEligible(Double.NEGATIVE_INFINITY, 100));
    }
}
