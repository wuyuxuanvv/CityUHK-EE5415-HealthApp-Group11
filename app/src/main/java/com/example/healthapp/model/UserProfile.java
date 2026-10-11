package com.example.healthapp.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Single-device, single-user profile. The only valid primary key is SINGLE_USER_ID. */
@Entity(tableName = "user_profile")
public class UserProfile {
    public static final int SINGLE_USER_ID = 1;

    @PrimaryKey
    public int id = SINGLE_USER_ID;

    @ColumnInfo(name = "height_cm")
    public double heightCm;

    @ColumnInfo(name = "weight_kg")
    public double weightKg;

    public int age;

    @NonNull
    public String gender = "";

    /** 展示昵称，可为空串。 */
    @NonNull
    public String nickname = "";

    @ColumnInfo(name = "weekly_exercise_goal_count")
    public int weeklyExerciseGoalCount;

    @ColumnInfo(name = "daily_calorie_goal_kcal")
    public double dailyCalorieGoalKcal;

    @ColumnInfo(name = "daily_exercise_goal_minutes")
    public int dailyExerciseGoalMinutes = 30;

    @ColumnInfo(name = "goal_general_health")
    public boolean goalGeneralHealth = true;

    @ColumnInfo(name = "goal_fitness")
    public boolean goalFitness;

    @ColumnInfo(name = "goal_regular_diet")
    public boolean goalRegularDiet;
}
