package com.example.healthapp.home.pet;

import android.content.Context;
import android.widget.TextView;

import com.example.healthapp.R;
import com.example.healthapp.common.DateTimeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** State machine; only complete, successful data snapshots may grant a reward. */
public final class PetManager {
    private final PetAnimationController animation;
    private final PetRewardSystem rewards;
    private final TextView status;
    private final TextView earned;
    private boolean active;
    private boolean rewarding;
    private boolean dataReady;
    private String date;
    private Set<Long> recordIds = Collections.emptySet();

    public PetManager(Context context, PetView view, TextView status, TextView earned) {
        animation = new PetAnimationController(view);
        rewards = new PetRewardSystem(context);
        this.status = status;
        this.earned = earned;
    }

    public void start() {
        active = true;
        animation.startIdle();
        status.setText(R.string.home_cat_pet_loading);
        earned.setText(R.string.home_cat_pet_count_loading);
    }

    public void update(String date, List<Long> ids) {
        if (!active || !DateTimeUtils.today().equals(date)) return;
        this.date = date;
        recordIds = PetRewardSystem.uniqueRecords(ids);
        dataReady = true;
        earned.setText(earned.getResources().getString(R.string.home_cat_pet_earned,
                recordIds.size()));
        feedNext();
    }

    private void feedNext() {
        if (!active || rewarding || !dataReady || !DateTimeUtils.today().equals(date)) return;
        Long recordId = rewards.nextPending(date, recordIds);
        if (recordId != null) {
            final String rewardDate = date;
            rewarding = true;
            status.setText(R.string.home_cat_pet_reward);
            animation.playReward(() -> {
                rewarding = false;
                if (!active) return;
                rewards.markFed(rewardDate, recordId);
                if (dataReady && DateTimeUtils.today().equals(date)) {
                    feedNext();
                } else {
                    animation.startIdle();
                }
            });
        } else {
            animation.startIdle();
            status.setText(recordIds.isEmpty() ? R.string.home_cat_pet_empty
                    : R.string.home_cat_pet_claimed);
        }
    }

    public void loading(String date) {
        if (this.date != null && !this.date.equals(date)) {
            animation.startIdle();
            rewarding = false;
            recordIds = Collections.emptySet();
        }
        this.date = date;
        dataReady = false;
        earned.setText(R.string.home_cat_pet_count_loading);
        if (active && !rewarding) status.setText(R.string.home_cat_pet_loading);
    }

    public void error() {
        dataReady = false;
        if (active) {
            status.setText(R.string.home_cat_pet_error);
            earned.setText(R.string.home_cat_pet_count_error);
        }
    }

    public void stop() {
        active = false;
        rewarding = false;
        dataReady = false;
        animation.stop();
    }
}
