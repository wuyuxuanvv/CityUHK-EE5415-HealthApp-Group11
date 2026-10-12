package com.example.healthapp.diet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.LruCache;

import androidx.core.content.ContextCompat;

import com.example.healthapp.R;
import com.example.healthapp.model.Food;

/** Creates a compact, food-specific illustration from each library resource name. */
final class FoodArtworkFactory {

    private static final int ARTWORK_SIZE_DP = 96;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(128);

    private FoodArtworkFactory() {
    }

    static Bitmap artworkFor(Context context, Food food) {
        String key = food.imageResourceName == null ? food.name : food.imageResourceName;
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        int size = Math.round(ARTWORK_SIZE_DP * context.getResources().getDisplayMetrics().density);
        Bitmap artwork = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(artwork);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float border = size * 0.055f;
        RectF bounds = new RectF(border, border, size - border, size - border);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.drawRoundRect(bounds, size * 0.18f, size * 0.18f, paint);
        bounds.inset(border, border);
        paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
        canvas.drawRoundRect(bounds, size * 0.13f, size * 0.13f, paint);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary_container));
        canvas.drawCircle(size * 0.5f, size * 0.5f, size * 0.33f, paint);

        if (!drawSpecificIllustration(canvas, paint, context, food.imageResourceName, size)) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size * 0.53f);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            Paint.FontMetrics metrics = paint.getFontMetrics();
            float baseline = size * 0.5f - (metrics.ascent + metrics.descent) / 2f;
            paint.setColor(ContextCompat.getColor(context, R.color.health_text_primary));
            canvas.drawText(symbolFor(food.imageResourceName), size * 0.5f, baseline, paint);
        }
        drawFoodStamp(canvas, paint, context, food.name, size);

        CACHE.put(key, artwork);
        return artwork;
    }

    /** A tiny name stamp keeps closely related foods visually distinguishable in the grid. */
    private static void drawFoodStamp(Canvas canvas, Paint paint, Context context, String name, int size) {
        float centerX = size * 0.76f;
        float centerY = size * 0.76f;
        float radius = size * 0.14f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
        canvas.drawCircle(centerX, centerY, radius * 0.76f, paint);
        paint.setColor(ContextCompat.getColor(context, R.color.health_accent_red));
        canvas.drawCircle(centerX, centerY, radius * 0.60f, paint);
        if (name == null || name.isEmpty()) return;
        int first = name.codePointAt(0);
        int last = name.codePointBefore(name.length());
        String marker = first == last ? new String(Character.toChars(first))
                : new String(Character.toChars(first)) + new String(Character.toChars(last));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(size * (marker.length() > 1 ? 0.11f : 0.15f));
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
        paint.setColor(ContextCompat.getColor(context, R.color.health_on_primary));
        canvas.drawText(marker, centerX, baseline, paint);
    }

    private static boolean drawSpecificIllustration(Canvas canvas, Paint paint, Context context,
                                                    String resourceName, int size) {
        if ("food_rice_porridge".equals(resourceName)) {
            drawWhitePorridge(canvas, paint, context, size);
            return true;
        }
        if ("food_purple_sweet_potato".equals(resourceName)) {
            drawPurpleSweetPotato(canvas, paint, context, size);
            return true;
        }
        if ("food_chicken_breast".equals(resourceName)) {
            drawChickenBreast(canvas, paint, context, size);
            return true;
        }
        if ("food_chicken_wing".equals(resourceName)) {
            drawCookedChickenWing(canvas, paint, context, size);
            return true;
        }
        if ("food_wood_ear".equals(resourceName) || "food_kelp".equals(resourceName)
                || "food_chive".equals(resourceName) || "food_asparagus".equals(resourceName)) {
            drawVegetable(canvas, paint, context, resourceName, size);
            return true;
        }
        if ("food_dragon_fruit".equals(resourceName)) {
            drawDragonFruit(canvas, paint, context, size);
            return true;
        }
        if ("food_baozi".equals(resourceName) || "food_mantou".equals(resourceName)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
            RectF bun = new RectF(size * 0.21f, size * 0.30f, size * 0.79f, size * 0.68f);
            canvas.drawOval(bun, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.04f);
            paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
            canvas.drawOval(bun, paint);
            canvas.drawLine(size * 0.5f, size * 0.33f, size * 0.5f, size * 0.49f, paint);
            canvas.drawLine(size * 0.36f, size * 0.38f, size * 0.42f, size * 0.51f, paint);
            canvas.drawLine(size * 0.64f, size * 0.38f, size * 0.58f, size * 0.51f, paint);
            paint.setStyle(Paint.Style.FILL);
            return true;
        }
        if ("food_hairtail".equals(resourceName)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
            RectF fish = new RectF(size * 0.18f, size * 0.40f, size * 0.73f, size * 0.60f);
            canvas.drawOval(fish, paint);
            android.graphics.Path tail = new android.graphics.Path();
            tail.moveTo(size * 0.70f, size * 0.50f);
            tail.lineTo(size * 0.86f, size * 0.34f);
            tail.lineTo(size * 0.83f, size * 0.50f);
            tail.lineTo(size * 0.86f, size * 0.66f);
            tail.close();
            canvas.drawPath(tail, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.04f);
            paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
            canvas.drawOval(fish, paint);
            canvas.drawPath(tail, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(size * 0.30f, size * 0.48f, size * 0.028f, paint);
            return true;
        }
        if ("food_tofu".equals(resourceName) || "food_silken_tofu".equals(resourceName)
                || "food_dried_tofu".equals(resourceName) || "food_tofu_skin".equals(resourceName)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
            RectF tofu = new RectF(size * 0.27f, size * 0.28f, size * 0.73f, size * 0.72f);
            canvas.drawRoundRect(tofu, size * 0.04f, size * 0.04f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.04f);
            paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
            canvas.drawRoundRect(tofu, size * 0.04f, size * 0.04f, paint);
            canvas.drawLine(size * 0.50f, size * 0.31f, size * 0.50f, size * 0.69f, paint);
            canvas.drawLine(size * 0.30f, size * 0.50f, size * 0.70f, size * 0.50f, paint);
            paint.setStyle(Paint.Style.FILL);
            return true;
        }
        return false;
    }

    private static void drawWhitePorridge(Canvas canvas, Paint paint, Context context, int size) {
        paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
        paint.setStyle(Paint.Style.FILL);
        RectF bowl = new RectF(size * 0.22f, size * 0.43f, size * 0.78f, size * 0.72f);
        canvas.drawOval(bowl, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.04f);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.drawOval(bowl, paint);
        canvas.drawLine(size * 0.28f, size * 0.50f, size * 0.72f, size * 0.50f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawPurpleSweetPotato(Canvas canvas, Paint paint, Context context, int size) {
        paint.setColor(ContextCompat.getColor(context, R.color.health_food_purple));
        paint.setStyle(Paint.Style.FILL);
        RectF potato = new RectF(size * 0.24f, size * 0.34f, size * 0.76f, size * 0.67f);
        canvas.rotate(-18, size * 0.5f, size * 0.5f);
        canvas.drawOval(potato, paint);
        canvas.rotate(18, size * 0.5f, size * 0.5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.04f);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.rotate(-18, size * 0.5f, size * 0.5f);
        canvas.drawOval(potato, paint);
        canvas.rotate(18, size * 0.5f, size * 0.5f);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawChickenBreast(Canvas canvas, Paint paint, Context context, int size) {
        android.graphics.Path breast = new android.graphics.Path();
        breast.moveTo(size * 0.25f, size * 0.62f);
        breast.cubicTo(size * 0.24f, size * 0.32f, size * 0.63f, size * 0.26f,
                size * 0.76f, size * 0.49f);
        breast.cubicTo(size * 0.68f, size * 0.70f, size * 0.40f, size * 0.75f,
                size * 0.25f, size * 0.62f);
        breast.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.health_surface));
        canvas.drawPath(breast, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.04f);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.drawPath(breast, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawCookedChickenWing(Canvas canvas, Paint paint, Context context, int size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary_container));
        canvas.drawCircle(size * 0.44f, size * 0.49f, size * 0.19f, paint);
        canvas.drawCircle(size * 0.63f, size * 0.57f, size * 0.13f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.04f);
        paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
        canvas.drawCircle(size * 0.44f, size * 0.49f, size * 0.19f, paint);
        canvas.drawCircle(size * 0.63f, size * 0.57f, size * 0.13f, paint);
        paint.setColor(ContextCompat.getColor(context, R.color.health_accent_red));
        canvas.drawLine(size * 0.35f, size * 0.44f, size * 0.51f, size * 0.49f, paint);
        canvas.drawLine(size * 0.39f, size * 0.55f, size * 0.53f, size * 0.59f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawVegetable(Canvas canvas, Paint paint, Context context,
                                      String resourceName, int size) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.06f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if ("food_wood_ear".equals(resourceName)) {
            paint.setColor(ContextCompat.getColor(context, R.color.health_primary));
            canvas.drawCircle(size * 0.42f, size * 0.50f, size * 0.18f, paint);
            canvas.drawCircle(size * 0.60f, size * 0.45f, size * 0.14f, paint);
        } else if ("food_kelp".equals(resourceName)) {
            paint.setColor(ContextCompat.getColor(context, R.color.health_accent_green));
            for (int i = 0; i < 3; i++) {
                float x = size * (0.36f + i * 0.14f);
                canvas.drawLine(x, size * 0.68f, x + size * 0.04f, size * 0.32f, paint);
            }
        } else if ("food_chive".equals(resourceName)) {
            paint.setColor(ContextCompat.getColor(context, R.color.health_accent_green));
            for (int i = 0; i < 4; i++) {
                canvas.drawLine(size * 0.34f, size * (0.68f - i * 0.03f),
                        size * (0.45f + i * 0.07f), size * 0.30f, paint);
            }
        } else {
            paint.setColor(ContextCompat.getColor(context, R.color.health_accent_green));
            for (int i = 0; i < 3; i++) {
                float x = size * (0.38f + i * 0.12f);
                canvas.drawLine(x, size * 0.68f, x + size * 0.06f, size * 0.29f, paint);
                canvas.drawLine(x + size * 0.06f, size * 0.29f, x + size * 0.10f, size * 0.35f, paint);
            }
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    private static void drawDragonFruit(Canvas canvas, Paint paint, Context context, int size) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(context, R.color.health_accent_red));
        RectF fruit = new RectF(size * 0.29f, size * 0.27f, size * 0.71f, size * 0.72f);
        canvas.drawOval(fruit, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.05f);
        paint.setColor(ContextCompat.getColor(context, R.color.health_accent_green));
        canvas.drawLine(size * 0.40f, size * 0.33f, size * 0.31f, size * 0.24f, paint);
        canvas.drawLine(size * 0.60f, size * 0.33f, size * 0.69f, size * 0.24f, paint);
        canvas.drawLine(size * 0.37f, size * 0.60f, size * 0.27f, size * 0.67f, paint);
        canvas.drawLine(size * 0.63f, size * 0.60f, size * 0.73f, size * 0.67f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private static String symbolFor(String resourceName) {
        if (resourceName == null) return "🍽";
        switch (resourceName) {
            case "food_rice": return "🍚";
            case "food_brown_rice": return "🌾";
            case "food_glutinous_rice": return "🍙";
            case "food_mantou": return "🥯";
            case "food_noodles": return "🍜";
            case "food_buckwheat_noodles": return "🥢";
            case "food_rice_noodles": return "🍝";
            case "food_oatmeal": return "🥣";
            case "food_rice_porridge": return "🥣";
            case "food_millet_porridge": return "🥣";
            case "food_sweet_potato": return "🍠";
            case "food_purple_sweet_potato": return "🍠";
            case "food_taro": return "🟣";
            case "food_corn": return "🌽";
            case "food_potato": case "food_yam": return "🥔";
            case "food_whole_wheat_bread": case "food_toast": return "🍞";
            case "food_dumpling": return "🥟";
            case "food_baozi": return "🥟";
            case "food_egg": return "🥚";
            case "food_egg_white": return "🍳";
            case "food_duck_egg": return "🪺";
            case "food_chicken_breast": return "🍗";
            case "food_chicken_leg": return "🍖";
            case "food_chicken_wing": return "🍗";
            case "food_duck": return "🦆";
            case "food_lean_beef": return "🥩";
            case "food_beef_brisket": return "🍲";
            case "food_lean_pork": return "🍖";
            case "food_pork_tenderloin": return "🥩";
            case "food_pork_liver": return "🟫";
            case "food_lamb": return "🐑";
            case "food_pork_belly": case "food_bacon": return "🥓";
            case "food_salmon": return "🍣";
            case "food_cod": return "🐠";
            case "food_hairtail": return "🐟";
            case "food_shrimp": return "🍤";
            case "food_ham_sausage": return "🌭";
            case "food_broccoli": return "🥦";
            case "food_tomato": return "🍅";
            case "food_cucumber": return "🥒";
            case "food_chinese_cabbage": return "🥬";
            case "food_spinach": return "🌿";
            case "food_lettuce": return "🥗";
            case "food_carrot": return "🥕";
            case "food_radish": return "⚪";
            case "food_eggplant": return "🍆";
            case "food_green_pepper": return "🫑";
            case "food_onion": return "🧅";
            case "food_celery": return "🌿";
            case "food_chive": return "🌿";
            case "food_asparagus": return "🌱";
            case "food_kelp": return "🌿";
            case "food_pumpkin": return "🎃";
            case "food_winter_melon": return "🍈";
            case "food_green_bean": case "food_pea": return "🫛";
            case "food_mushroom": return "🍄";
            case "food_wood_ear": return "⚫";
            case "food_apple": return "🍎";
            case "food_banana": return "🍌";
            case "food_orange": return "🍊";
            case "food_watermelon": return "🍉";
            case "food_grape": return "🍇";
            case "food_strawberry": return "🍓";
            case "food_pear": return "🍐";
            case "food_peach": return "🍑";
            case "food_kiwi": return "🥝";
            case "food_mango": return "🥭";
            case "food_pineapple": return "🍍";
            case "food_blueberry": return "🫐";
            case "food_pomelo": return "🍊";
            case "food_cherry": return "🍒";
            case "food_dragon_fruit": return "🥝";
            case "food_milk": return "🥛";
            case "food_yogurt": return "🥣";
            case "food_soy_milk": return "🫘";
            case "food_soy_drink": return "🧃";
            case "food_tofu": case "food_silken_tofu": return "🍱";
            case "food_dried_tofu": case "food_tofu_skin": return "🟨";
            case "food_soybean": return "🟡";
            case "food_mung_bean": return "🟢";
            case "food_red_bean": return "🔴";
            case "food_cheese": return "🧀";
            case "food_peanut": return "🥜";
            case "food_walnut": return "🌰";
            case "food_almond": return "🍂";
            case "food_cashew": return "🌙";
            case "food_pistachio": return "🟢";
            case "food_sunflower_seed": return "🌻";
            case "food_sesame": return "⚫";
            case "food_pine_nut": return "🌲";
            case "food_cola": return "🥤";
            case "food_orange_juice": return "🧃";
            case "food_black_coffee": return "☕";
            case "food_green_tea": return "🍵";
            case "food_beer": return "🍺";
            case "food_sports_drink": return "🧃";
            case "food_fried_chicken": return "🍗";
            case "food_popcorn_chicken": return "🍿";
            case "food_hamburger": return "🍔";
            case "food_french_fries": return "🍟";
            case "food_pizza": return "🍕";
            case "food_hot_dog": return "🌭";
            case "food_egg_tart": return "🥧";
            case "food_burrito": return "🌯";
            case "food_onion_rings": return "🧅";
            case "food_sugar": return "🧊";
            case "food_honey": return "🍯";
            case "food_chocolate": return "🍫";
            case "food_peanut_butter": return "🥜";
            case "food_cooking_oil": return "🫗";
            case "food_salt": return "🧂";
            case "food_light_soy_sauce": return "🫙";
            case "food_vinegar": return "🍶";
            case "food_oyster_sauce": return "🦪";
            case "food_chili_sauce": return "🌶";
            default: return "🍽";
        }
    }
}
