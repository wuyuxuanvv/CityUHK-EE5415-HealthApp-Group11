package com.example.healthapp.exercise;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.example.healthapp.R;

import org.junit.Test;

public class ExerciseIconMapperTest {
    @Test
    public void mapsAllTenDatabaseNames() {
        assertEquals(R.drawable.exercise_icon_walking, ExerciseIconMapper.iconFor("快走"));
        assertEquals(R.drawable.exercise_icon_walking, ExerciseIconMapper.iconFor("散步"));
        assertEquals(R.drawable.exercise_icon_running, ExerciseIconMapper.iconFor("慢跑"));
        assertEquals(R.drawable.exercise_icon_running, ExerciseIconMapper.iconFor("跑步"));
        assertEquals(R.drawable.exercise_icon_aerobics, ExerciseIconMapper.iconFor("有氧操"));
        assertEquals(R.drawable.exercise_icon_elliptical, ExerciseIconMapper.iconFor("椭圆机"));
        assertEquals(R.drawable.exercise_icon_swimming, ExerciseIconMapper.iconFor("游泳"));
        assertEquals(R.drawable.exercise_icon_stairs, ExerciseIconMapper.iconFor("爬楼梯"));
        assertEquals(R.drawable.exercise_icon_jump_rope, ExerciseIconMapper.iconFor("跳绳"));
        assertEquals(R.drawable.exercise_icon_cycling, ExerciseIconMapper.iconFor("骑行"));
        assertNotEquals(ExerciseIconMapper.iconFor("快走"), ExerciseIconMapper.iconFor("慢跑"));
    }

    @Test
    public void unknownNamesHaveNoIcon() {
        assertEquals(0, ExerciseIconMapper.iconFor(null));
        assertEquals(0, ExerciseIconMapper.iconFor(""));
        assertEquals(0, ExerciseIconMapper.iconFor("俯卧撑"));
        assertEquals(0, ExerciseIconMapper.iconFor("骑行训练"));
    }

    @Test
    public void mapsNewExercisesAndSharedStrengthIcon() {
        assertEquals(R.drawable.exercise_icon_badminton, ExerciseIconMapper.iconFor("羽毛球"));
        assertEquals(R.drawable.exercise_icon_basketball, ExerciseIconMapper.iconFor("篮球"));
        assertEquals(R.drawable.exercise_icon_soccer, ExerciseIconMapper.iconFor("足球"));
        assertEquals(R.drawable.exercise_icon_ping_pong, ExerciseIconMapper.iconFor("乒乓球"));
        assertEquals(R.drawable.exercise_icon_tennis, ExerciseIconMapper.iconFor("网球"));
        assertEquals(R.drawable.exercise_icon_volleyball, ExerciseIconMapper.iconFor("排球"));
        assertEquals(R.drawable.exercise_icon_yoga, ExerciseIconMapper.iconFor("瑜伽"));
        assertEquals(R.drawable.exercise_icon_pilates, ExerciseIconMapper.iconFor("普拉提"));
        assertEquals(R.drawable.exercise_icon_hiit, ExerciseIconMapper.iconFor("HIIT 间歇训练"));
        for (String name : new String[]{"低强度", "中等强度", "高强度"}) {
            assertEquals(R.drawable.exercise_icon_strength, ExerciseIconMapper.iconFor(name));
        }
        assertEquals(0, ExerciseIconMapper.iconFor("拳击"));
    }
}
