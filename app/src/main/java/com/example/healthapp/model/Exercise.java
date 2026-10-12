package com.example.healthapp.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Built-in exercise library entry.
 *
 * <p>Flat, single-model taxonomy: every row belongs to one of two categories —
 * {@link #CATEGORY_AEROBIC} or {@link #CATEGORY_STRENGTH} — and every row burns
 * calories with the same MET formula {@code metValue × weightKg × durationMinutes ÷ 60},
 * implemented by {@code HealthCalculations.estimateExerciseCalories}. Strength work is
 * logged by time exactly like aerobic work; there is no per-set coefficient.
 */
@Entity(tableName = "exercises", indices = {@Index(value = "name", unique = true)})
public class Exercise {
    /** Top-level taxonomy. Seed data must use one of these two values. */
    public static final String CATEGORY_AEROBIC = "有氧";
    public static final String CATEGORY_STRENGTH = "力量训练";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** One of the two CATEGORY_* constants above. */
    @NonNull
    public String category = "";

    @ColumnInfo(name = "met_value")
    public double metValue;

    /** Reserved for the looped demo animation resource name; null until the assets exist. */
    @ColumnInfo(name = "demo_resource_name")
    public String demoResourceName;
}
