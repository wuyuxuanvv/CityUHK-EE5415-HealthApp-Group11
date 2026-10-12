package com.example.healthapp.database;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.healthapp.common.HealthCalculations;
import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.ExerciseRecord;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies dictionary supplementation without changing existing IDs or user history. */
@RunWith(AndroidJUnit4.class)
public class ExerciseSeedUpgradeTest {
    @Test
    public void addsMissingExercisesWithoutReplacingExistingRowsOrRecords() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        try {
            Exercise existing = new Exercise();
            existing.name = "散步";
            existing.category = Exercise.CATEGORY_AEROBIC;
            existing.metValue = 4.4;
            long existingId = database.exerciseDao().insert(existing);

            Exercise historical = new Exercise();
            historical.name = "历史自定义运动";
            historical.category = Exercise.CATEGORY_AEROBIC;
            historical.metValue = 6.0;
            database.exerciseDao().insert(historical);

            ExerciseRecord record = new ExerciseRecord();
            record.exerciseId = existingId;
            record.recordDate = "2026-10-01";
            record.durationMinutes = 30;
            record.estimatedCaloriesKcal = 132.0;
            database.exerciseRecordDao().insert(record);

            SeedData.syncExerciseLibrary(database);
            SeedData.syncExerciseLibrary(database);

            assertEquals(23, database.exerciseDao().getAll().size());
            Exercise retained = database.exerciseDao().getById(existingId);
            assertEquals("散步", retained.name);
            assertEquals(4.4, retained.metValue, 0.0);
            assertEquals(1, database.exerciseRecordDao().getByDate("2026-10-01").size());
            assertEquals(existingId,
                    database.exerciseRecordDao().getByDate("2026-10-01").get(0).exerciseId);

            Exercise hiit = database.exerciseDao().searchByName("HIIT").get(0);
            assertEquals("HIIT 间歇训练", hiit.name);
            assertEquals(11.0, hiit.metValue, 0.0);
            ExerciseRecord newRecord = new ExerciseRecord();
            newRecord.exerciseId = hiit.id;
            newRecord.recordDate = "2026-10-02";
            newRecord.durationMinutes = 30;
            newRecord.estimatedCaloriesKcal = HealthCalculations.estimateExerciseCalories(
                    hiit.metValue, 60.0, newRecord.durationMinutes);
            database.exerciseRecordDao().insert(newRecord);
            assertEquals(330.0,
                    database.exerciseRecordDao().getByDate("2026-10-02")
                            .get(0).estimatedCaloriesKcal, 0.0);
        } finally {
            database.close();
        }
    }
}
