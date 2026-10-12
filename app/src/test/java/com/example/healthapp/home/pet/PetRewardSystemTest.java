package com.example.healthapp.home.pet;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PetRewardSystemTest {
    @Test public void eachUniqueWorkoutEarnsOneFish() {
        Set<Long> ids = PetRewardSystem.uniqueRecords(Arrays.asList(1L, 2L, 2L, 3L));
        assertEquals(3, ids.size());
        assertEquals(Long.valueOf(1), PetRewardSystem.nextPending(ids, Collections.emptySet()));
    }

    @Test public void emptyDayDoesNotGrantReward() {
        Set<Long> ids = PetRewardSystem.uniqueRecords(Collections.emptyList());
        assertEquals(0, ids.size());
        assertNull(PetRewardSystem.nextPending(ids, Collections.emptySet()));
    }

    @Test public void multipleRewardsAreFedOnceInRecordOrder() {
        Set<Long> ids = PetRewardSystem.uniqueRecords(Arrays.asList(3L, 2L, 1L));
        Set<String> fed = new HashSet<>();
        for (long id = 1; id <= 3; id++) {
            assertEquals(Long.valueOf(id), PetRewardSystem.nextPending(ids, fed));
            fed.add(Long.toString(id));
        }
        assertNull(PetRewardSystem.nextPending(ids, fed));
    }

    @Test public void replacingDeletedWorkoutStillEarnsNewReward() {
        Set<Long> ids = PetRewardSystem.uniqueRecords(Arrays.asList(2L, 3L));
        Set<String> fed = new HashSet<>(Arrays.asList("1", "2"));
        assertEquals(2, ids.size());
        assertEquals(Long.valueOf(3), PetRewardSystem.nextPending(ids, fed));
    }

    @Test public void refreshedOrEditedRecordDoesNotEarnAgain() {
        Set<Long> ids = PetRewardSystem.uniqueRecords(Collections.singletonList(8L));
        assertNull(PetRewardSystem.nextPending(ids, Collections.singleton("8")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsavedRecordCannotEarnReward() {
        PetRewardSystem.uniqueRecords(Collections.singletonList(0L));
    }
}
