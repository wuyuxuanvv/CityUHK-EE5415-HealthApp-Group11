package com.example.healthapp.home.pet;

import android.content.Context;
import android.widget.TextView;

import com.example.healthapp.R;
import com.example.healthapp.common.DateTimeUtils;

/** State machine; only complete, successful data snapshots may grant a reward. */
public final class PetManager {
    private final PetAnimationController animation;
    private final PetRewardSystem rewards;
    private final TextView status;
    private boolean active;
    private boolean rewarding;

    public PetManager(Context context, PetView view, TextView status) {
        animation = new PetAnimationController(view);
        rewards = new PetRewardSystem(context);
        this.status = status;
    }

    public void start() {
        active = true;
        animation.startIdle();
        status.setText(R.string.home_cat_pet_loading);
    }

    public void update(String date, double foodKcal, double exerciseKcal) {
        if (!active || rewarding || !DateTimeUtils.today().equals(date)) return;
        if (!rewards.wasClaimed(date) && PetRewardSystem.isEligible(foodKcal, exerciseKcal)) {
            rewarding = true;
            status.setText(R.string.home_cat_pet_reward);
            animation.playReward(() -> {
                rewarding = false;
                if (!active) return;
                rewards.markClaimed(date);
                animation.startIdle();
                status.setText(DateTimeUtils.today().equals(date)
                        ? R.string.home_cat_pet_claimed : R.string.home_cat_pet_loading);
            });
        } else {
            status.setText(rewards.wasClaimed(date) ? R.string.home_cat_pet_claimed
                    : foodKcal == 0 && exerciseKcal == 0 ? R.string.home_cat_pet_empty
                    : R.string.home_cat_pet_idle);
        }
    }

    public void loading() {
        if (active && !rewarding) status.setText(R.string.home_cat_pet_loading);
    }

    public void error() {
        if (active && !rewarding) status.setText(R.string.home_cat_pet_error);
    }

    public void stop() {
        active = false;
        rewarding = false;
        animation.stop();
    }
}
