package com.example.healthapp.home.pet;

import android.content.Context;
import android.content.SharedPreferences;

/** Local, date-keyed reward ledger; never changes health records or goals. */
public final class PetRewardSystem {
    private final SharedPreferences preferences;

    public PetRewardSystem(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences("home_pet_rewards", Context.MODE_PRIVATE);
    }

    public static boolean isEligible(double foodKcal, double exerciseKcal) {
        return Double.isFinite(foodKcal) && Double.isFinite(exerciseKcal)
                && foodKcal >= 0 && exerciseKcal >= 0 && foodKcal < exerciseKcal;
    }

    public boolean wasClaimed(String date) {
        return preferences.getBoolean("claimed_" + date, false);
    }

    /** Called only after the animation completes. An interrupted reward can resume later. */
    public void markClaimed(String date) {
        preferences.edit().putBoolean("claimed_" + date, true).apply();
    }
}
