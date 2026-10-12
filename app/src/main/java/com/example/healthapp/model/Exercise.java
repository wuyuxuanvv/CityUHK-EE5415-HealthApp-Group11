package com.example.healthapp.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Built-in exercise library entry.
 *
 * <p>Two calorie models share this one table and are told apart by {@link #category}:
 * <ul>
 *   <li>{@link #CATEGORY_AEROBIC} — aerobic. Calories come from
 *       {@code metValue × weightKg × minutes ÷ 60}; {@link #kcalPerSetPerKg} stays 0.</li>
 *   <li>every other category — strength. Calories come from
 *       {@code sets × kcalPerSetPerKg × weightKg}; {@link #metValue} stays 0.</li>
 * </ul>
 *
 * <p>Never mix the two formulas: the unused coefficient is 0, so adding them
 * would silently zero out the result.
 */
@Entity(tableName = "exercises", indices = {@Index(value = "name", unique = true)})
public class Exercise {
    /** Body-part taxonomy. Seed data must use one of these values. */
    public static final String CATEGORY_AEROBIC = "有氧";
    public static final String CATEGORY_CHEST = "胸";
    public static final String CATEGORY_BACK = "背";
    public static final String CATEGORY_SHOULDER = "肩";
    public static final String CATEGORY_LEGS = "腿";
    public static final String CATEGORY_ARMS = "手臂";
    public static final String CATEGORY_CORE = "核心";
    public static final String CATEGORY_FULL_BODY = "全身";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** One of the CATEGORY_* constants above. */
    @NonNull
    public String category = "";

    @ColumnInfo(name = "met_value")
    public double metValue;

    /** Strength only: kcal burned per set, per kg of body weight. Aerobic exercises keep 0. */
    @ColumnInfo(name = "kcal_per_set_per_kg", defaultValue = "0")
    public double kcalPerSetPerKg;

    /** Reserved for the looped demo animation resource name; null until the assets exist. */
    @ColumnInfo(name = "demo_resource_name")
    public String demoResourceName;
}
