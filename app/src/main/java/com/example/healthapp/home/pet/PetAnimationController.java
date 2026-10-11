package com.example.healthapp.home.pet;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/** Owns all animation work and cancels it when the home page is hidden. */
public final class PetAnimationController {
    private final PetView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private ValueAnimator animator;
    private boolean running;
    private float position;

    public PetAnimationController(PetView view) {
        this.view = view;
    }

    public void startIdle() {
        stop();
        running = true;
        idleCycle();
    }

    private void idleCycle() {
        if (!running) return;
        if (random.nextBoolean()) {
            playFrames(0, 3, 350, 2 + random.nextInt(3), this::idleCycle);
        } else {
            float start = position;
            float target = random.nextFloat() * 2 - 1;
            int pose = target >= start ? 1 : 2;
            animate(1440, elapsed -> {
                position = start + (target - start) * elapsed;
                view.show(pose, Math.min(7, (int) (elapsed * 8)) % 4, position, -1);
            }, this::idleCycle);
        }
    }

    public void playReward(Runnable completed) {
        stop();
        running = true;
        position = 0;
        view.show(0, 0, position, 0);
        handler.postDelayed(() -> animate(800,
                progress -> view.show(0, 0, position, progress),
                () -> playFrames(3, 3, 350, 1,
                        () -> playFrames(4, 3, 220, 1, completed))), 350);
    }

    private void playFrames(int pose, int count, long frameMs, int loops, Runnable completed) {
        for (int i = 0; i < count * loops; i++) {
            final int frame = i % count;
            handler.postDelayed(() -> view.show(pose, frame, position, -1), i * frameMs);
        }
        handler.postDelayed(completed, count * loops * frameMs);
    }

    private interface Progress { void update(float fraction); }

    private void animate(long duration, Progress progress, Runnable completed) {
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> progress.update((float) value.getAnimatedValue()));
        animator.start();
        handler.postDelayed(completed, duration);
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        if (animator != null) {
            animator.cancel();
            animator.removeAllUpdateListeners();
            animator = null;
        }
        view.show(0, 0, position, -1);
    }
}
