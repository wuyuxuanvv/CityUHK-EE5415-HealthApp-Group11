package com.example.healthapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.ExerciseCheckIn;
import com.example.healthapp.model.ExercisePlan;
import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.Food;
import com.example.healthapp.model.FoodRecord;
import com.example.healthapp.model.UserProfile;

@Database(
        entities = {
                UserProfile.class,
                Exercise.class,
                ExerciseRecord.class,
                ExercisePlan.class,
                ExerciseCheckIn.class,
                Food.class,
                FoodRecord.class
        },
        version = 2,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "health_app.db";
    private static volatile AppDatabase instance;

    public abstract UserProfileDao userProfileDao();

    public abstract ExerciseDao exerciseDao();

    public abstract ExerciseRecordDao exerciseRecordDao();

    public abstract ExercisePlanDao exercisePlanDao();

    public abstract ExerciseCheckInDao exerciseCheckInDao();

    public abstract FoodDao foodDao();

    public abstract FoodRecordDao foodRecordDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME)
                            // 本地完整版（未发布）：结构变更直接重建库，不保留旧数据。
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
