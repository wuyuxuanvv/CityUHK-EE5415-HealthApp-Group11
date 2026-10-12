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
            Exercise.CATEGORY_AEROBIC, Exercise.CATEGORY_STRENGTH));

    private static final Set<String> FOOD_CATEGORIES = new HashSet<>(Arrays.asList(
            Food.CATEGORY_STAPLE, Food.CATEGORY_MEAT_EGG_DAIRY_BEAN,
            Food.CATEGORY_VEGETABLE, Food.CATEGORY_FRUIT, Food.CATEGORY_NUT,
            Food.CATEGORY_DRINK, Food.CATEGORY_FAST_FOOD, Food.CATEGORY_OTHER));

    @Test
    public void exerciseSeeds_meetContractMinimum() {
        assertEquals("10 original aerobic + 9 new aerobic + 3 strength tiers", 22,
                SeedData.buildExerciseSeeds().size());
        assertTrue("exercise library must hold at least 20 entries",
                SeedData.buildExerciseSeeds().size() >= 20);
    }

    @Test
    public void newExercises_haveRequestedMetValues() {
        String[] names = {"羽毛球", "篮球", "足球", "乒乓球", "网球", "排球",
                "瑜伽", "普拉提", "HIIT 间歇训练"};
        double[] mets = {5.5, 7.5, 7.0, 4.0, 6.8, 3.0, 2.3, 2.8, 11.0};
        List<Exercise> seeds = SeedData.buildExerciseSeeds();
        for (int i = 0; i < names.length; i++) {
            Exercise match = null;
            for (Exercise exercise : seeds) {
                if (names[i].equals(exercise.name)) {
                    match = exercise;
                    break;
                }
            }
            assertTrue("missing exercise: " + names[i], match != null);
            assertEquals(names[i] + " MET", mets[i], match.metValue, 0.0);
            assertEquals(names[i] + " category", Exercise.CATEGORY_AEROBIC, match.category);
        }
    }

    @Test
    public void foodSeeds_meetContractMinimum() {
        assertTrue("food library must hold at least 100 entries",
                SeedData.buildFoodSeeds().size() >= 100);
    }

    @Test
    public void everyExercise_usesMetModelOnly() {
        Set<Double> strengthMets = new HashSet<>();
        int strengthCount = 0;
        for (Exercise exercise : SeedData.buildExerciseSeeds()) {
            assertTrue("unknown category: " + exercise.name,
                    EXERCISE_CATEGORIES.contains(exercise.category));
            assertTrue(exercise.name + " has no MET value — the only calorie model is "
                    + "metValue × weightKg × minutes ÷ 60", exercise.metValue > 0);
            if (Exercise.CATEGORY_STRENGTH.equals(exercise.category)) {
                strengthCount++;
                strengthMets.add(exercise.metValue);
            }
        }
        assertEquals("力量训练 must be exactly three intensity tiers", 3, strengthCount);
        assertEquals("力量训练 METs must be 3.5 / 5.0 / 7.0",
                new HashSet<>(Arrays.asList(3.5, 5.0, 7.0)), strengthMets);
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
