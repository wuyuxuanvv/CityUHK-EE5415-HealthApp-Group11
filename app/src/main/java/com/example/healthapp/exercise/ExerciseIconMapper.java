package com.example.healthapp.exercise;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.healthapp.R;

/** Static icons for the named exercise dictionary entries; 0 means no icon. */
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
            case "羽毛球":
                return R.drawable.exercise_icon_badminton;
            case "篮球":
                return R.drawable.exercise_icon_basketball;
            case "足球":
                return R.drawable.exercise_icon_soccer;
            case "乒乓球":
                return R.drawable.exercise_icon_ping_pong;
            case "网球":
                return R.drawable.exercise_icon_tennis;
            case "排球":
                return R.drawable.exercise_icon_volleyball;
            case "瑜伽":
                return R.drawable.exercise_icon_yoga;
            case "普拉提":
                return R.drawable.exercise_icon_pilates;
            case "HIIT 间歇训练":
                return R.drawable.exercise_icon_hiit;
            case "低强度":
            case "中等强度":
            case "高强度":
            case "力量训练（低强度）":
            case "力量训练（中强度）":
            case "力量训练（高强度）":
                return R.drawable.exercise_icon_strength;
            default:
                return 0;
        }
    }
}
