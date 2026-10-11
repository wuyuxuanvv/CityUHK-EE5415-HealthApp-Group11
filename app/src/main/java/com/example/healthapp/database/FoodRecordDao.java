package com.example.healthapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.healthapp.model.FoodRecord;

import java.util.List;

@Dao
public interface FoodRecordDao {
    @Query("SELECT * FROM food_records WHERE record_date = :date ORDER BY meal_type, id DESC")
    List<FoodRecord> getByDate(String date);

    @Query("SELECT * FROM food_records WHERE record_date BETWEEN :startDate AND :endDate "
            + "ORDER BY record_date DESC, id DESC")
    List<FoodRecord> getByDateRange(String startDate, String endDate);

    @Query("SELECT COALESCE(SUM(calories_kcal), 0.0) FROM food_records WHERE record_date = :date")
    double getTotalCaloriesByDate(String date);

    @Query("SELECT COUNT(*) FROM food_records WHERE record_date = :date")
    int countByDate(String date);

    @Insert
    long insert(FoodRecord record);

    @Update
    int update(FoodRecord record);

    @Delete
    int delete(FoodRecord record);
}
