package com.example.healthapp.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 小猫吉祥物的运动 / 进食事件总线（v1.3 新增）。
 *
 * <p>分工边界：
 * <ul>
 *   <li><b>F（框架）</b>：只负责提供本总线与首页 {@code container_home_gauge} 挂载点，不实现动画。</li>
 *   <li><b>TODO(E)</b>：实现 {@link CatMascot} 接口（运动→小猫动起来；进食→小猫吃东西），
 *       在首页可见时 {@link #register(CatMascot)}，不可见时 {@link #unregister(CatMascot)}。</li>
 *   <li><b>TODO(A/B)</b>：在运动 / 饮食记录插入数据库成功后调用
 *       {@link #notifyExerciseLogged(double)} / {@link #notifyMealLogged(double)}，
 *       入参为该次记录实际对应的卡路里值（kcal）。</li>
 * </ul>
 *
 * <p>用法（A/B 侧，伪代码）：
 * <pre>
 *   ExerciseRecord r = dao.insert(record);
 *   double kcal = CalorieCalculator.exerciseKcal(r.type, r.minutes, userProfile.weightKg);
 *   CatInteractionHub.notifyExerciseLogged(kcal);
 * </pre>
 */
public final class CatInteractionHub {

    /** 监听小猫事件的吉祥物实现（由 E 实现）。 */
    public interface CatMascot {
        /** 用户记录了一次运动：小猫应该“动起来”。 */
        void onExerciseLogged(double kcalBurned);

        /** 用户记录了一次进食：小猫应该“吃东西”。 */
        void onMealLogged(double kcalIntake);
    }

    private static final List<CatMascot> listeners = new CopyOnWriteArrayList<>();

    private CatInteractionHub() {
        // 工具类，禁止实例化
    }

    /** 吉祥物可见时注册（E 在首页 onStart / onResume 调用）。 */
    public static void register(CatMascot mascot) {
        if (mascot != null && !listeners.contains(mascot)) {
            listeners.add(mascot);
        }
    }

    /** 吉祥物不可见时注销（E 在首页 onStop / onPause 调用）。 */
    public static void unregister(CatMascot mascot) {
        listeners.remove(mascot);
    }

    /** A 同学：运动记录写入成功后调用。 */
    public static void notifyExerciseLogged(double kcalBurned) {
        for (CatMascot m : listeners) {
            m.onExerciseLogged(kcalBurned);
        }
    }

    /** B 同学：饮食记录写入成功后调用。 */
    public static void notifyMealLogged(double kcalIntake) {
        for (CatMascot m : listeners) {
            m.onMealLogged(kcalIntake);
        }
    }
}
