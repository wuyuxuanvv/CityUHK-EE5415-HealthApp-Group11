package com.example.healthapp.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.healthapp.common.DateTimeUtils;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.model.DailyHealthSummary;
import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.Food;
import com.example.healthapp.model.FoodRecord;
import com.example.healthapp.model.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Stable asynchronous cross-module data API. DAO methods remain available for module-owned CRUD,
 * but other modules should use this class instead of depending on another module's UI code.
 */
public final class HealthRepository {
    private static volatile HealthRepository instance;

    private final AppDatabase database;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private HealthRepository(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public static HealthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (HealthRepository.class) {
                if (instance == null) {
                    instance = new HealthRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void getUserProfile(RepositoryCallback<UserProfile> callback) {
        submit(() -> database.userProfileDao().getProfile(), callback);
    }

    public void saveUserProfile(UserProfile profile, RepositoryCallback<Long> callback) {
        profile.id = UserProfile.SINGLE_USER_ID;
        submit(() -> database.userProfileDao().save(profile), callback);
    }

    /** Returns null when a profile has not yet been saved. */
    public void getCurrentWeightKg(RepositoryCallback<Double> callback) {
        submit(() -> {
            UserProfile profile = database.userProfileDao().getProfile();
            return profile == null ? null : profile.weightKg;
        }, callback);
    }

    /** Returns null when a profile has not yet been saved. */
    public void getDailyCalorieGoalKcal(RepositoryCallback<Double> callback) {
        submit(() -> {
            UserProfile profile = database.userProfileDao().getProfile();
            return profile == null ? null : profile.dailyCalorieGoalKcal;
        }, callback);
    }

    public void getExerciseCountForDate(String date, RepositoryCallback<Integer> callback) {
        submit(() -> database.exerciseRecordDao().countByDate(date), callback);
    }

    public void getTotalCaloriesForDate(String date, RepositoryCallback<Double> callback) {
        submit(() -> database.foodRecordDao().getTotalCaloriesByDate(date), callback);
    }

    /** Estimated exercise burn for one day; 0.0 when no records. Feeds the home calorie gauge. */
    public void getTotalExerciseCaloriesForDate(String date, RepositoryCallback<Double> callback) {
        submit(() -> database.exerciseRecordDao().getTotalCaloriesByDate(date), callback);
    }

    public void getDailySummaries(String startDate, String endDate,
                                  RepositoryCallback<List<DailyHealthSummary>> callback) {
        submit(() -> {
            List<DailyHealthSummary> summaries = new ArrayList<>();
            for (String date : DateTimeUtils.datesBetweenInclusive(startDate, endDate)) {
                int exerciseCount = database.exerciseRecordDao().countByDate(date);
                double intake = database.foodRecordDao().getTotalCaloriesByDate(date);
                double burn = database.exerciseRecordDao().getTotalCaloriesByDate(date);
                summaries.add(new DailyHealthSummary(date, exerciseCount, intake, burn));
            }
            return summaries;
        }, callback);
    }

    public void clearAllData(RepositoryCallback<Boolean> callback) {
        submit(() -> {
            database.clearAllTables();
            return true;
        }, callback);
    }

    // ---------- 运动库 / 食物库 ----------

    public void getAllExercises(RepositoryCallback<List<Exercise>> callback) {
        submit(() -> database.exerciseDao().getAll(), callback);
    }

    public void getAllFoods(RepositoryCallback<List<Food>> callback) {
        submit(() -> database.foodDao().getAll(), callback);
    }

    // ---------- 运动记录 ----------

    public void addExerciseRecord(ExerciseRecord record, RepositoryCallback<Long> callback) {
        submit(() -> database.exerciseRecordDao().insert(record), callback);
    }

    public void deleteExerciseRecord(ExerciseRecord record, RepositoryCallback<Integer> callback) {
        submit(() -> database.exerciseRecordDao().delete(record), callback);
    }

    public void getExerciseRecordsForDate(String date,
                                          RepositoryCallback<List<ExerciseRecord>> callback) {
        submit(() -> database.exerciseRecordDao().getByDate(date), callback);
    }

    public void getExerciseRecordsInRange(String startDate, String endDate,
                                          RepositoryCallback<List<ExerciseRecord>> callback) {
        submit(() -> database.exerciseRecordDao().getByDateRange(startDate, endDate), callback);
    }

    public void getExerciseMinutesForDate(String date, RepositoryCallback<Integer> callback) {
        submit(() -> database.exerciseRecordDao().getTotalMinutesByDate(date), callback);
    }

    /** 区间内有多少天记过运动（本周目标 X/Y 天的分子）。 */
    public void getDistinctExerciseDaysInRange(String startDate, String endDate,
                                               RepositoryCallback<Integer> callback) {
        submit(() -> database.exerciseRecordDao().countDistinctDatesInRange(startDate, endDate),
                callback);
    }

    // ---------- 饮食记录 ----------

    public void addFoodRecord(FoodRecord record, RepositoryCallback<Long> callback) {
        submit(() -> database.foodRecordDao().insert(record), callback);
    }

    public void deleteFoodRecord(FoodRecord record, RepositoryCallback<Integer> callback) {
        submit(() -> database.foodRecordDao().delete(record), callback);
    }

    public void getFoodRecordsForDate(String date,
                                      RepositoryCallback<List<FoodRecord>> callback) {
        submit(() -> database.foodRecordDao().getByDate(date), callback);
    }

    public void getFoodRecordsInRange(String startDate, String endDate,
                                      RepositoryCallback<List<FoodRecord>> callback) {
        submit(() -> database.foodRecordDao().getByDateRange(startDate, endDate), callback);
    }

    public void hasFoodRecordsForDate(String date, RepositoryCallback<Boolean> callback) {
        submit(() -> database.foodRecordDao().countByDate(date) > 0, callback);
    }

    private <T> void submit(Callable<T> task, RepositoryCallback<T> callback) {
        DatabaseExecutor.execute(() -> {
            try {
                T result = task.call();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Throwable error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }
}
