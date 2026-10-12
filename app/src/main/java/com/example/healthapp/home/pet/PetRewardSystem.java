package com.example.healthapp.home.pet;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Local, date-keyed reward ledger; never changes health records or goals. */
public final class PetRewardSystem {
    private final SharedPreferences preferences;

    public PetRewardSystem(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences("home_pet_rewards", Context.MODE_PRIVATE);
    }

    /** One real record earns one fish. Deleted records are excluded from today's total. */
    public static Set<Long> uniqueRecords(List<Long> ids) {
        Set<Long> result = new HashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) throw new IllegalArgumentException("Invalid record ID");
            result.add(id);
        }
        return result;
    }

    public static Long nextPending(Set<Long> ids, Set<String> fedIds) {
        Long next = null;
        for (Long id : ids) {
            if (!fedIds.contains(Long.toString(id)) && (next == null || id < next)) next = id;
        }
        return next;
    }

    public Long nextPending(String date, Set<Long> ids) {
        return nextPending(ids, preferences.getStringSet("fed_records_" + date,
                Collections.emptySet()));
    }

    /** Called only after the animation completes. An interrupted reward can resume later. */
    public void markFed(String date, long recordId) {
        Set<String> fedIds = new HashSet<>(preferences.getStringSet("fed_records_" + date,
                Collections.emptySet()));
        fedIds.add(Long.toString(recordId));
        preferences.edit().putStringSet("fed_records_" + date, fedIds).apply();
    }
}
