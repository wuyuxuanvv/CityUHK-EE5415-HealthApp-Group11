package com.example.healthapp.common;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 小猫吉祥物的运动 / 进食事件通道（v2.0 本地完整版）。
 *
 * <p>工作方式：任何模块记录成功后调用 {@link #notifyExerciseLogged(double)} /
 * {@link #notifyMealLogged(double)}，事件先计入挂起计数；首页小猫在 {@code onResume()}
 * 时调用 {@link #drainPendingExercise()} / {@link #drainPendingMeal()} 取走并播放对应动画
 * （播放一次后停止）。这样用户在运动/饮食页记录、再切回首页时动画照样触发。
 *
 * <p>保留了 register/unregister 监听接口供将来扩展；当前首页用挂起计数即可。
 */
public final class CatInteractionHub {

    /** 监听小猫事件的吉祥物实现（可选扩展用）。 */
    public interface CatMascot {
        /** 用户记录了一次运动：小猫应该“动起来”。 */
        void onExerciseLogged(double kcalBurned);

        /** 用户记录了一次进食：小猫应该“吃东西”。 */
        void onMealLogged(double kcalIntake);
    }

    private static final List<CatMascot> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicInteger pendingExercise = new AtomicInteger();
    private static final AtomicInteger pendingMeal = new AtomicInteger();

    private CatInteractionHub() {
        // 工具类，禁止实例化
    }

    /** 吉祥物可见时注册（扩展用）。 */
    public static void register(CatMascot mascot) {
        if (mascot != null && !listeners.contains(mascot)) {
            listeners.add(mascot);
        }
    }

    /** 吉祥物不可见时注销（扩展用）。 */
    public static void unregister(CatMascot mascot) {
        listeners.remove(mascot);
    }

    /** 运动记录写入成功后调用（A/运动页）。 */
    public static void notifyExerciseLogged(double kcalBurned) {
        pendingExercise.incrementAndGet();
        for (CatMascot m : listeners) {
            m.onExerciseLogged(kcalBurned);
        }
    }

    /** 饮食记录写入成功后调用（B/饮食页）。 */
    public static void notifyMealLogged(double kcalIntake) {
        pendingMeal.incrementAndGet();
        for (CatMascot m : listeners) {
            m.onMealLogged(kcalIntake);
        }
    }

    /** 首页取走挂起的运动事件数量（取走后清零）。 */
    public static int drainPendingExercise() {
        return pendingExercise.getAndSet(0);
    }

    /** 首页取走挂起的进食事件数量（取走后清零）。 */
    public static int drainPendingMeal() {
        return pendingMeal.getAndSet(0);
    }
}
