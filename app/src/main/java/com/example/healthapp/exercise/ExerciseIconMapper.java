package com.example.healthapp.exercise;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.healthapp.R;

/** Static icons for the ten named aerobic exercises; 0 means no icon. */
final class ExerciseIconMapper {
    private ExerciseIconMapper() {
    }

    @DrawableRes
    static int iconFor(@Nullable String exerciseName) {
        if (exerciseName == null) {
            return 0;
        }
        switch (exerciseName.trim()) {
            case "快走":
            case "散步":
                return R.drawable.exercise_icon_walking;
            case "慢跑":
            case "跑步":
                return R.drawable.exercise_icon_running;
            case "有氧操":
                return R.drawable.exercise_icon_aerobics;
            case "椭圆机":
                return R.drawable.exercise_icon_elliptical;
            case "游泳":
                return R.drawable.exercise_icon_swimming;
            case "爬楼梯":
                return R.drawable.exercise_icon_stairs;
            case "跳绳":
                return R.drawable.exercise_icon_jump_rope;
            case "骑行":
                return R.drawable.exercise_icon_cycling;
            default:
                return 0;
        }
    }
}
