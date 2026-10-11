package com.example.healthapp.database;

import android.content.Context;

import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.Food;

import java.util.ArrayList;
import java.util.List;

/**
 * Seed entry point for the built-in exercise and food libraries.
 *
 * Framework owns the trigger and the empty-check; C (database owner) fills the
 * seed lists below. Rules:
 * <ul>
 *   <li>Seeds run once per install, only while the table is empty — never re-insert.</li>
 *   <li>After clearAllData() the tables are empty again, so the next launch
 *       re-seeds automatically and the libraries are never lost.</li>
 *   <li>DAO calls stay on DatabaseExecutor; never call this on the UI thread
 *       expecting completion — fire and forget.</li>
 * </ul>
 */
public final class SeedData {

    private SeedData() {
    }

    /** Called once from MainActivity.onCreate. Safe to call on every launch. */
    public static void seedIfEmpty(Context context) {
        DatabaseExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            if (database.exerciseDao().getAll().isEmpty()) {
                database.exerciseDao().insertAll(buildExerciseSeeds());
            }
            if (database.foodDao().getAll().isEmpty()) {
                database.foodDao().insertAll(buildFoodSeeds());
            }
        });
    }

    /** v2.0 本地完整版内置运动库：10 种，含 MET 值（PPT 公式：MET × 体重kg × 小时）。 */
    private static List<Exercise> buildExerciseSeeds() {
        List<Exercise> seeds = new ArrayList<>();
        seeds.add(newExercise("散步", "有氧", 3.5));
        seeds.add(newExercise("快走", "有氧", 5.0));
        seeds.add(newExercise("跑步", "有氧", 9.8));
        seeds.add(newExercise("骑行", "有氧", 6.8));
        seeds.add(newExercise("游泳", "有氧", 6.0));
        seeds.add(newExercise("跳绳", "有氧", 11.0));
        seeds.add(newExercise("瑜伽", "身心", 3.0));
        seeds.add(newExercise("拉伸", "身心", 2.5));
        seeds.add(newExercise("篮球", "球类", 6.5));
        seeds.add(newExercise("羽毛球", "球类", 5.5));
        return seeds;
    }

    /** v2.0 本地完整版内置食物库：20 种，每 100g 热量与三大营养素。 */
    private static List<Food> buildFoodSeeds() {
        List<Food> seeds = new ArrayList<>();
        seeds.add(newFood("米饭", 116, 2.6, 25.9, 0.3));
        seeds.add(newFood("馒头", 223, 7.0, 47.0, 1.1));
        seeds.add(newFood("燕麦片", 377, 13.0, 67.0, 6.7));
        seeds.add(newFood("面条", 301, 10.3, 65.6, 1.1));
        seeds.add(newFood("红薯", 86, 1.6, 20.1, 0.1));
        seeds.add(newFood("玉米", 112, 4.0, 22.8, 1.2));
        seeds.add(newFood("土豆", 77, 2.0, 17.5, 0.1));
        seeds.add(newFood("鸡蛋", 144, 13.3, 2.8, 8.8));
        seeds.add(newFood("鸡胸肉", 133, 19.4, 2.5, 5.0));
        seeds.add(newFood("瘦牛肉", 106, 20.2, 1.2, 2.3));
        seeds.add(newFood("瘦猪肉", 143, 20.3, 1.5, 6.2));
        seeds.add(newFood("豆腐", 82, 8.1, 4.2, 3.7));
        seeds.add(newFood("牛奶", 54, 3.0, 3.4, 3.2));
        seeds.add(newFood("酸奶", 72, 2.5, 9.3, 2.7));
        seeds.add(newFood("西兰花", 36, 4.1, 4.3, 0.6));
        seeds.add(newFood("番茄", 20, 0.9, 4.0, 0.2));
        seeds.add(newFood("黄瓜", 16, 0.8, 2.9, 0.2));
        seeds.add(newFood("苹果", 53, 0.2, 13.7, 0.2));
        seeds.add(newFood("香蕉", 93, 1.4, 22.0, 0.2));
        seeds.add(newFood("橙子", 48, 0.8, 11.1, 0.2));
        return seeds;
    }

    private static Exercise newExercise(String name, String category, double metValue) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.category = category;
        exercise.metValue = metValue;
        return exercise;
    }

    private static Food newFood(String name, double calories, double protein,
                                double carbs, double fat) {
        Food food = new Food();
        food.name = name;
        food.caloriesPer100g = calories;
        food.proteinPer100g = protein;
        food.carbsPer100g = carbs;
        food.fatPer100g = fat;
        return food;
    }
}
