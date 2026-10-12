package com.example.healthapp.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A single logged workout. Covers both calorie models:
 * <ul>
 *   <li>aerobic — {@link #durationMinutes} holds the real minutes, {@link #sets} and
 *       {@link #repsPerSet} stay 0;</li>
 *   <li>strength — {@link #sets} and {@link #repsPerSet} hold the user's input,
 *       {@link #durationMinutes} stays 0 so the "minutes today" total only counts aerobic work.</li>
 * </ul>
 *
 * <p>{@link #estimatedCaloriesKcal} must be written for both kinds — the home mascot reads it.
 */
@Entity(
        tableName = "exercise_records",
        foreignKeys = @ForeignKey(
                entity = Exercise.class,
                parentColumns = "id",
                childColumns = "exercise_id",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index("exercise_id"), @Index("record_date")})
public class ExerciseRecord {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "exercise_id")
    public long exerciseId;

    @NonNull
    @ColumnInfo(name = "record_date")
    public String recordDate = "";

    @ColumnInfo(name = "duration_minutes")
    public int durationMinutes;

    /** Strength only: number of sets. Aerobic records keep 0. */
    @ColumnInfo(name = "sets", defaultValue = "0")
    public int sets;

    /** Strength only: reps per set, kept for display. Not part of the calorie formula. */
    @ColumnInfo(name = "reps_per_set", defaultValue = "0")
    public int repsPerSet;

    @ColumnInfo(name = "estimated_calories_kcal")
    public double estimatedCaloriesKcal;
}
