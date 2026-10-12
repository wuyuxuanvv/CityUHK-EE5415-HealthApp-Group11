package com.example.healthapp.database;

import android.content.Context;

import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.Food;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 *
 * <p><b>Changing the data below.</b> All library content lives in the two constant
 * tables. Editing them does <em>not</em> reach an already-installed app: seeding only
 * runs on an empty table, and {@code insertAll} is IGNORE-on-conflict, so an existing
 * row is never updated. To make an edit take effect you must also bump
 * {@code AppDatabase.version}, which drops and rebuilds the whole database —
 * including the user's records.
 */
public final class SeedData {

    /** Foods removed from the picker; referenced historical records remain intact. */
    private static final Set<String> RETIRED_FOOD_NAMES = new HashSet<>(Arrays.asList(
            "山药", "芋头", "豆腐干", "豆浆", "红豆", "黄豆", "绿豆", "炸鸡翅"));

    private SeedData() {
    }

    /** Called once from MainActivity.onCreate. Safe to call on every launch. */
    public static void seedIfEmpty(Context context) {
        DatabaseExecutor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            if (database.exerciseDao().getAll().isEmpty()) {
                database.exerciseDao().insertAll(buildExerciseSeeds());
            }
            syncFoodLibrary(database);
        });
    }

    /** Adds newly shipped foods and normalizes renamed categories without touching meal records. */
    private static void syncFoodLibrary(AppDatabase database) {
        List<Food> existingFoods = database.foodDao().getAll();
        Set<String> existingNames = new HashSet<>();
        for (Food food : existingFoods) {
            existingNames.add(food.name);
            if (isRetiredFoodName(food.name)) {
                try {
                    database.foodDao().delete(food);
                } catch (RuntimeException ignored) {
                    // FoodRecord uses RESTRICT; retain a referenced row only for history.
                }
                continue;
            }
            if (Food.CATEGORY_MEAT_EGG.equals(food.category)
                    || Food.CATEGORY_DAIRY_BEAN.equals(food.category)) {
                food.category = Food.CATEGORY_MEAT_EGG_DAIRY_BEAN;
                database.foodDao().update(food);
            }
        }

        List<Food> additions = new ArrayList<>();
        for (Food seed : buildFoodSeeds()) {
            if (!existingNames.contains(seed.name)) {
                additions.add(seed);
            }
        }
        if (!additions.isEmpty()) {
            database.foodDao().insertAll(additions);
        }
    }

    /** Lets the picker hide retired rows which must remain only for historical records. */
    public static boolean isRetiredFoodName(String name) {
        return name != null && RETIRED_FOOD_NAMES.contains(name);
    }

    // ======================================================================
    // Exercise library
    // ======================================================================

    /**
     * {name, category, metValue}
     *
     * <p>Every row uses the same MET model, {@code MET × weightKg × minutes ÷ 60};
     * strength is logged by time like aerobic work. There is no per-set coefficient.
     *
     * <p>有氧 rows are named movements. 力量训练 is deliberately coarse — exactly three
     * rows, one per intensity tier, each with a fixed MET (低 3.5 / 中 5.0 / 高 7.0) —
     * because the user logs a duration, not a movement.
     *
     * <p>The aerobic METs and the three tier METs overlap on purpose (散步/低强度 both
     * 3.5, 快走/中等强度 both 5.0); that is not a copy-paste slip.
     */
    private static final Object[][] EXERCISE_SEEDS = {
            // ----- 有氧 -----
            {"散步", Exercise.CATEGORY_AEROBIC, 3.5},
            {"快走", Exercise.CATEGORY_AEROBIC, 5.0},
            {"慢跑", Exercise.CATEGORY_AEROBIC, 7.0},
            {"跑步", Exercise.CATEGORY_AEROBIC, 9.8},
            {"骑行", Exercise.CATEGORY_AEROBIC, 6.8},
            {"游泳", Exercise.CATEGORY_AEROBIC, 6.0},
            {"跳绳", Exercise.CATEGORY_AEROBIC, 11.0},
            {"椭圆机", Exercise.CATEGORY_AEROBIC, 5.0},
            {"爬楼梯", Exercise.CATEGORY_AEROBIC, 8.8},
            {"有氧操", Exercise.CATEGORY_AEROBIC, 7.3},

            // ----- 力量训练（三个强度档位）-----
            {"低强度", Exercise.CATEGORY_STRENGTH, 3.5},
            {"中等强度", Exercise.CATEGORY_STRENGTH, 5.0},
            {"高强度", Exercise.CATEGORY_STRENGTH, 7.0},
    };

    // ======================================================================
    // Food library
    // ======================================================================

    /**
     * {name, category, kcal/100g, protein/100g, carbs/100g, fat/100g, imageResourceName}
     *
     * <p>Values follow the per-100 g convention of the Chinese Food Composition Table.
     * {@code imageResourceName} is a name only — no drawable exists yet, so the UI must
     * fall back to a placeholder when the resource is missing.
     */
    private static final Object[][] FOOD_SEEDS = {
            // ----- 主食 -----
            {"米饭", Food.CATEGORY_STAPLE, 116, 2.6, 25.9, 0.3, "food_rice"},
            {"馒头", Food.CATEGORY_STAPLE, 223, 7.0, 47.0, 1.1, "food_mantou"},
            {"面条", Food.CATEGORY_STAPLE, 301, 10.3, 65.6, 1.1, "food_noodles"},
            {"燕麦片", Food.CATEGORY_STAPLE, 377, 13.0, 67.0, 6.7, "food_oatmeal"},
            {"红薯", Food.CATEGORY_STAPLE, 86, 1.6, 20.1, 0.1, "food_sweet_potato"},
            {"玉米", Food.CATEGORY_STAPLE, 112, 4.0, 22.8, 1.2, "food_corn"},
            {"土豆", Food.CATEGORY_STAPLE, 77, 2.0, 17.5, 0.1, "food_potato"},
            {"白粥", Food.CATEGORY_STAPLE, 46, 1.1, 9.9, 0.3, "food_rice_porridge"},
            {"小米粥", Food.CATEGORY_STAPLE, 46, 1.4, 8.4, 0.7, "food_millet_porridge"},
            {"全麦面包", Food.CATEGORY_STAPLE, 246, 8.5, 45.0, 3.3, "food_whole_wheat_bread"},
            {"吐司面包", Food.CATEGORY_STAPLE, 313, 8.3, 58.6, 5.1, "food_toast"},
            {"荞麦面", Food.CATEGORY_STAPLE, 337, 11.3, 66.5, 2.3, "food_buckwheat_noodles"},
            {"紫薯", Food.CATEGORY_STAPLE, 82, 1.6, 18.9, 0.2, "food_purple_sweet_potato"},
            {"糙米", Food.CATEGORY_STAPLE, 348, 7.7, 72.7, 2.7, "food_brown_rice"},
            {"糯米", Food.CATEGORY_STAPLE, 350, 7.3, 78.3, 1.0, "food_glutinous_rice"},
            {"米粉", Food.CATEGORY_STAPLE, 346, 8.0, 78.0, 0.5, "food_rice_noodles"},
            {"饺子", Food.CATEGORY_STAPLE, 240, 8.0, 30.0, 9.0, "food_dumpling"},
            {"包子", Food.CATEGORY_STAPLE, 227, 7.0, 35.0, 6.0, "food_baozi"},

            // ----- 肉蛋奶豆 -----
            {"鸡蛋", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 144, 13.3, 2.8, 8.8, "food_egg"},
            {"鸡蛋白", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 60, 11.6, 3.1, 0.1, "food_egg_white"},
            {"鸡胸肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 133, 19.4, 2.5, 5.0, "food_chicken_breast"},
            {"鸡腿", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 181, 16.0, 0.0, 13.0, "food_chicken_leg"},
            {"鸡翅", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 194, 17.4, 4.6, 11.8, "food_chicken_wing"},
            {"瘦牛肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 106, 20.2, 1.2, 2.3, "food_lean_beef"},
            {"牛腩", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 332, 17.1, 0.0, 29.3, "food_beef_brisket"},
            {"瘦猪肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 143, 20.3, 1.5, 6.2, "food_lean_pork"},
            {"猪里脊", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 155, 20.2, 0.7, 7.9, "food_pork_tenderloin"},
            {"五花肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 508, 7.7, 0.0, 53.0, "food_pork_belly"},
            {"羊肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 203, 19.0, 0.0, 14.1, "food_lamb"},
            {"三文鱼", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 139, 17.2, 0.0, 7.8, "food_salmon"},
            {"鳕鱼", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 88, 20.4, 0.5, 0.5, "food_cod"},
            {"带鱼", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 127, 17.7, 3.1, 4.9, "food_hairtail"},
            {"虾", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 93, 18.6, 2.8, 0.8, "food_shrimp"},
            {"鸭肉", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 240, 15.5, 0.2, 19.7, "food_duck"},
            {"火腿肠", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 212, 14.0, 15.6, 10.4, "food_ham_sausage"},
            {"猪肝", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 129, 19.3, 5.0, 3.5, "food_pork_liver"},
            {"鸭蛋", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 180, 12.6, 3.1, 13.0, "food_duck_egg"},
            {"培根", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 181, 22.3, 0.0, 10.0, "food_bacon"},

            // ----- 蔬菜 -----
            {"西兰花", Food.CATEGORY_VEGETABLE, 36, 4.1, 4.3, 0.6, "food_broccoli"},
            {"番茄", Food.CATEGORY_VEGETABLE, 20, 0.9, 4.0, 0.2, "food_tomato"},
            {"黄瓜", Food.CATEGORY_VEGETABLE, 16, 0.8, 2.9, 0.2, "food_cucumber"},
            {"白菜", Food.CATEGORY_VEGETABLE, 17, 1.5, 3.2, 0.1, "food_chinese_cabbage"},
            {"菠菜", Food.CATEGORY_VEGETABLE, 28, 2.6, 4.5, 0.3, "food_spinach"},
            {"生菜", Food.CATEGORY_VEGETABLE, 15, 1.3, 2.0, 0.3, "food_lettuce"},
            {"胡萝卜", Food.CATEGORY_VEGETABLE, 39, 1.0, 8.8, 0.2, "food_carrot"},
            {"白萝卜", Food.CATEGORY_VEGETABLE, 21, 0.9, 5.0, 0.1, "food_radish"},
            {"茄子", Food.CATEGORY_VEGETABLE, 21, 1.1, 4.9, 0.2, "food_eggplant"},
            {"青椒", Food.CATEGORY_VEGETABLE, 22, 1.0, 5.4, 0.2, "food_green_pepper"},
            {"洋葱", Food.CATEGORY_VEGETABLE, 40, 1.1, 9.0, 0.2, "food_onion"},
            {"芹菜", Food.CATEGORY_VEGETABLE, 14, 1.2, 3.3, 0.2, "food_celery"},
            {"南瓜", Food.CATEGORY_VEGETABLE, 23, 0.7, 5.3, 0.1, "food_pumpkin"},
            {"冬瓜", Food.CATEGORY_VEGETABLE, 11, 0.4, 2.6, 0.2, "food_winter_melon"},
            {"豆角", Food.CATEGORY_VEGETABLE, 30, 2.5, 5.7, 0.2, "food_green_bean"},
            {"蘑菇", Food.CATEGORY_VEGETABLE, 20, 2.7, 4.1, 0.1, "food_mushroom"},
            {"木耳", Food.CATEGORY_VEGETABLE, 205, 12.1, 65.6, 1.5, "food_wood_ear"},
            {"海带", Food.CATEGORY_VEGETABLE, 13, 1.2, 2.1, 0.1, "food_kelp"},
            {"韭菜", Food.CATEGORY_VEGETABLE, 26, 2.4, 4.6, 0.4, "food_chive"},
            {"芦笋", Food.CATEGORY_VEGETABLE, 19, 1.4, 4.9, 0.1, "food_asparagus"},

            // ----- 水果 -----
            {"苹果", Food.CATEGORY_FRUIT, 53, 0.2, 13.7, 0.2, "food_apple"},
            {"香蕉", Food.CATEGORY_FRUIT, 93, 1.4, 22.0, 0.2, "food_banana"},
            {"橙子", Food.CATEGORY_FRUIT, 48, 0.8, 11.1, 0.2, "food_orange"},
            {"西瓜", Food.CATEGORY_FRUIT, 31, 0.5, 6.8, 0.3, "food_watermelon"},
            {"葡萄", Food.CATEGORY_FRUIT, 45, 0.5, 10.3, 0.2, "food_grape"},
            {"草莓", Food.CATEGORY_FRUIT, 32, 1.0, 7.1, 0.2, "food_strawberry"},
            {"梨", Food.CATEGORY_FRUIT, 51, 0.4, 13.3, 0.2, "food_pear"},
            {"桃子", Food.CATEGORY_FRUIT, 51, 0.9, 12.2, 0.1, "food_peach"},
            {"猕猴桃", Food.CATEGORY_FRUIT, 61, 0.8, 14.5, 0.6, "food_kiwi"},
            {"芒果", Food.CATEGORY_FRUIT, 35, 0.6, 8.3, 0.2, "food_mango"},
            {"菠萝", Food.CATEGORY_FRUIT, 44, 0.5, 10.8, 0.1, "food_pineapple"},
            {"蓝莓", Food.CATEGORY_FRUIT, 57, 0.7, 14.5, 0.3, "food_blueberry"},
            {"柚子", Food.CATEGORY_FRUIT, 42, 0.8, 9.5, 0.2, "food_pomelo"},
            {"樱桃", Food.CATEGORY_FRUIT, 46, 1.1, 10.2, 0.2, "food_cherry"},
            {"火龙果", Food.CATEGORY_FRUIT, 55, 1.1, 13.3, 0.2, "food_dragon_fruit"},

            {"牛奶", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 54, 3.0, 3.4, 3.2, "food_milk"},
            {"酸奶", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 72, 2.5, 9.3, 2.7, "food_yogurt"},
            {"豆腐", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 82, 8.1, 4.2, 3.7, "food_tofu"},
            {"腐竹", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 459, 44.6, 22.3, 21.7, "food_tofu_skin"},
            {"奶酪", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 328, 25.7, 3.5, 23.5, "food_cheese"},
            {"豆奶", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 30, 2.4, 1.8, 1.5, "food_soy_drink"},
            {"内酯豆腐", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 50, 5.0, 3.3, 1.9, "food_silken_tofu"},
            {"豌豆", Food.CATEGORY_MEAT_EGG_DAIRY_BEAN, 313, 20.3, 65.8, 1.1, "food_pea"},

            // ----- 坚果 -----
            {"花生", Food.CATEGORY_NUT, 574, 24.8, 21.7, 44.3, "food_peanut"},
            {"核桃", Food.CATEGORY_NUT, 646, 14.9, 19.1, 58.8, "food_walnut"},
            {"杏仁", Food.CATEGORY_NUT, 578, 22.5, 23.9, 45.4, "food_almond"},
            {"腰果", Food.CATEGORY_NUT, 559, 17.3, 41.6, 36.7, "food_cashew"},
            {"开心果", Food.CATEGORY_NUT, 614, 20.6, 21.9, 53.0, "food_pistachio"},
            {"葵花籽", Food.CATEGORY_NUT, 606, 22.6, 17.3, 52.8, "food_sunflower_seed"},
            {"芝麻", Food.CATEGORY_NUT, 559, 19.1, 24.0, 46.1, "food_sesame"},
            {"松子", Food.CATEGORY_NUT, 698, 13.4, 12.2, 70.6, "food_pine_nut"},

            // ----- 饮品 -----
            {"可乐", Food.CATEGORY_DRINK, 43, 0.0, 10.8, 0.0, "food_cola"},
            {"橙汁", Food.CATEGORY_DRINK, 45, 0.7, 10.4, 0.2, "food_orange_juice"},
            {"黑咖啡", Food.CATEGORY_DRINK, 1, 0.1, 0.0, 0.0, "food_black_coffee"},
            {"绿茶", Food.CATEGORY_DRINK, 1, 0.0, 0.3, 0.0, "food_green_tea"},
            {"啤酒", Food.CATEGORY_DRINK, 32, 0.4, 3.1, 0.0, "food_beer"},
            {"运动饮料", Food.CATEGORY_DRINK, 26, 0.0, 6.4, 0.0, "food_sports_drink"},

            // ----- 快餐 -----
            {"炸鸡", Food.CATEGORY_FAST_FOOD, 295, 20.0, 9.0, 19.0, "food_fried_chicken"},
            {"汉堡", Food.CATEGORY_FAST_FOOD, 250, 13.0, 27.0, 10.0, "food_hamburger"},
            {"薯条", Food.CATEGORY_FAST_FOOD, 312, 3.4, 41.0, 15.0, "food_french_fries"},
            {"披萨", Food.CATEGORY_FAST_FOOD, 266, 11.0, 33.0, 10.0, "food_pizza"},
            {"热狗", Food.CATEGORY_FAST_FOOD, 290, 10.0, 25.0, 17.0, "food_hot_dog"},
            {"鸡米花", Food.CATEGORY_FAST_FOOD, 300, 16.0, 20.0, 17.0, "food_popcorn_chicken"},
            {"蛋挞", Food.CATEGORY_FAST_FOOD, 328, 5.0, 35.0, 18.0, "food_egg_tart"},
            {"墨西哥卷饼", Food.CATEGORY_FAST_FOOD, 229, 9.0, 25.0, 10.0, "food_burrito"},
            {"炸洋葱圈", Food.CATEGORY_FAST_FOOD, 332, 4.0, 38.0, 18.0, "food_onion_rings"},

            // ----- 其他 -----
            {"白砂糖", Food.CATEGORY_OTHER, 400, 0.0, 99.9, 0.0, "food_sugar"},
            {"蜂蜜", Food.CATEGORY_OTHER, 321, 0.4, 79.5, 1.9, "food_honey"},
            {"巧克力", Food.CATEGORY_OTHER, 586, 4.3, 51.9, 40.1, "food_chocolate"},
            {"花生酱", Food.CATEGORY_OTHER, 600, 25.0, 20.0, 50.0, "food_peanut_butter"},
            {"食用油", Food.CATEGORY_OTHER, 899, 0.0, 0.0, 99.9, "food_cooking_oil"},
            {"食盐", Food.CATEGORY_OTHER, 0, 0.0, 0.0, 0.0, "food_salt"},
            {"生抽", Food.CATEGORY_OTHER, 63, 5.6, 10.5, 0.1, "food_light_soy_sauce"},
            {"陈醋", Food.CATEGORY_OTHER, 31, 2.1, 4.9, 0.0, "food_vinegar"},
            {"蚝油", Food.CATEGORY_OTHER, 114, 5.9, 22.4, 0.0, "food_oyster_sauce"},
            {"辣椒酱", Food.CATEGORY_OTHER, 74, 1.5, 14.0, 1.2, "food_chili_sauce"},
    };

    /**
     * Package-private so SeedDataTest can assert the counts, category values and
     * duplicate-freedom without touching the database.
     */
    static List<Exercise> buildExerciseSeeds() {
        List<Exercise> seeds = new ArrayList<>(EXERCISE_SEEDS.length);
        for (Object[] row : EXERCISE_SEEDS) {
            seeds.add(newExercise((String) row[0], (String) row[1],
                    ((Number) row[2]).doubleValue()));
        }
        return seeds;
    }

    /** Package-private for SeedDataTest. See {@link #buildExerciseSeeds()}. */
    static List<Food> buildFoodSeeds() {
        List<Food> seeds = new ArrayList<>(FOOD_SEEDS.length);
        for (Object[] row : FOOD_SEEDS) {
            seeds.add(newFood((String) row[0], (String) row[1],
                    ((Number) row[2]).doubleValue(), ((Number) row[3]).doubleValue(),
                    ((Number) row[4]).doubleValue(), ((Number) row[5]).doubleValue(),
                    (String) row[6]));
        }
        return seeds;
    }

    private static Exercise newExercise(String name, String category, double metValue) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.category = category;
        exercise.metValue = metValue;
        return exercise;
    }

    private static Food newFood(String name, String category, double calories, double protein,
                                double carbs, double fat, String imageResourceName) {
        Food food = new Food();
        food.name = name;
        food.category = category;
        food.caloriesPer100g = calories;
        food.proteinPer100g = protein;
        food.carbsPer100g = carbs;
        food.fatPer100g = fat;
        food.imageResourceName = imageResourceName;
        return food;
    }
}
