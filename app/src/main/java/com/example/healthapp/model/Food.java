package com.example.healthapp.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Built-in food library entry. All nutrition values are per 100 g.
 *
 * <p>{@link #imageResourceName} is a name only — the drawable itself is owned by the
 * UI designer. Seed data fills the name; the matching file lands later, so the UI must
 * fall back to a placeholder while it is missing.
 */
@Entity(tableName = "foods", indices = {@Index(value = "name", unique = true)})
public class Food {
    /** Food-group taxonomy. Seed data must use one of these values. */
    public static final String CATEGORY_STAPLE = "主食";
    public static final String CATEGORY_MEAT_EGG = "肉蛋";
    public static final String CATEGORY_VEGETABLE = "蔬菜";
    public static final String CATEGORY_FRUIT = "水果";
    public static final String CATEGORY_DAIRY_BEAN = "奶豆";
    public static final String CATEGORY_NUT = "坚果";
    public static final String CATEGORY_DRINK = "饮品";
    public static final String CATEGORY_OTHER = "其他";

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** One of the CATEGORY_* constants above. */
    @NonNull
    @ColumnInfo(name = "category", defaultValue = "")
    public String category = CATEGORY_OTHER;

    @ColumnInfo(name = "calories_per_100g")
    public double caloriesPer100g;

    @ColumnInfo(name = "protein_per_100g")
    public double proteinPer100g;

    @ColumnInfo(name = "carbs_per_100g")
    public double carbsPer100g;

    @ColumnInfo(name = "fat_per_100g")
    public double fatPer100g;

    @ColumnInfo(name = "image_resource_name")
    public String imageResourceName;
}
