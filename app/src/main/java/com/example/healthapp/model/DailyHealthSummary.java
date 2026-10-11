package com.example.healthapp.model;

/** Cross-module daily statistics for the home gauge and weekly trend. */
public class DailyHealthSummary {
    public final String date;
    public final int exerciseCount;
    public final double calorieIntakeKcal;
    public final double exerciseCaloriesKcal;

    public DailyHealthSummary(String date, int exerciseCount, double calorieIntakeKcal,
                              double exerciseCaloriesKcal) {
        this.date = date;
        this.exerciseCount = exerciseCount;
        this.calorieIntakeKcal = calorieIntakeKcal;
        this.exerciseCaloriesKcal = exerciseCaloriesKcal;
    }

    /** Net calories consumed today: intake minus exercise burn. May be negative after exercise. */
    public double netCaloriesKcal() {
        return calorieIntakeKcal - exerciseCaloriesKcal;
    }
}
