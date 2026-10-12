package com.example.healthapp.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.Food;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guards the built-in libraries against the two failure modes that the database
 * cannot report: a count below the contract minimum, and a duplicate name that
 * {@code insertAll(IGNORE)} would silently drop.
 */
public class SeedDataTest {

    private static final Set<String> EXERCISE_CATEGORIES = new HashSet<>(Arrays.asList(
            Exercise.CATEGORY_AEROBIC, Exercise.CATEGORY_CHEST, Exercise.CATEGORY_BACK,
            Exercise.CATEGORY_SHOULDER, Exercise.CATEGORY_LEGS, Exercise.CATEGORY_ARMS,
            Exercise.CATEGORY_CORE, Exercise.CATEGORY_FULL_BODY));

    private static final Set<String> FOOD_CATEGORIES = new HashSet<>(Arrays.asList(
            Food.CATEGORY_STAPLE, Food.CATEGORY_MEAT_EGG_DAIRY_BEAN,
            Food.CATEGORY_VEGETABLE, Food.CATEGORY_FRUIT, Food.CATEGORY_NUT,
            Food.CATEGORY_DRINK, Food.CATEGORY_FAST_FOOD, Food.CATEGORY_OTHER));

    @Test
    public void exerciseSeeds_meetContractMinimum() {
        assertTrue("exercise library must hold at least 20 entries",
                SeedData.buildExerciseSeeds().size() >= 20);
    }

    @Test
    public void foodSeeds_meetContractMinimum() {
        assertTrue("food library must hold at least 100 entries",
                SeedData.buildFoodSeeds().size() >= 100);
    }

    @Test
    public void everyExercise_usesOneCalorieModelOnly() {
        for (Exercise exercise : SeedData.buildExerciseSeeds()) {
            assertTrue("unknown category: " + exercise.name,
                    EXERCISE_CATEGORIES.contains(exercise.category));
            if (Exercise.CATEGORY_AEROBIC.equals(exercise.category)) {
                assertTrue(exercise.name + " is aerobic but has no MET value",
                        exercise.metValue > 0);
                assertEquals(exercise.name + " is aerobic but carries a strength coefficient",
                        0.0, exercise.kcalPerSetPerKg, 0.0001);
            } else {
                assertEquals(exercise.name + " is strength but carries a MET value",
                        0.0, exercise.metValue, 0.0001);
                assertTrue(exercise.name + " is strength but has no set coefficient",
                        exercise.kcalPerSetPerKg > 0);
            }
        }
    }

    @Test
    public void everyFood_hasKnownCategoryAndSaneNutrition() {
        for (Food food : SeedData.buildFoodSeeds()) {
            assertTrue("unknown category: " + food.name,
                    FOOD_CATEGORIES.contains(food.category));
            assertTrue(food.name + " has negative calories", food.caloriesPer100g >= 0);
            assertTrue(food.name + " has negative protein", food.proteinPer100g >= 0);
            assertTrue(food.name + " has negative carbs", food.carbsPer100g >= 0);
            assertTrue(food.name + " has negative fat", food.fatPer100g >= 0);
        }
    }

    @Test
    public void names_areUnique_soNothingIsDroppedByTheUniqueIndex() {
        assertEquals(SeedData.buildExerciseSeeds().size(), uniqueExerciseNames().size());
        assertEquals(SeedData.buildFoodSeeds().size(), uniqueFoodNames().size());
    }

    private static Set<String> uniqueExerciseNames() {
        Set<String> names = new HashSet<>();
        for (Exercise exercise : SeedData.buildExerciseSeeds()) {
            names.add(exercise.name);
        }
        return names;
    }

    private static Set<String> uniqueFoodNames() {
        Set<String> names = new HashSet<>();
        List<Food> foods = SeedData.buildFoodSeeds();
        for (Food food : foods) {
            names.add(food.name);
        }
        return names;
    }
}
