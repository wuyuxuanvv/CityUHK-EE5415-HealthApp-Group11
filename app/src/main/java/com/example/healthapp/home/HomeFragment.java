package com.example.healthapp.home;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.healthapp.R;
import com.example.healthapp.common.CatInteractionHub;
import com.example.healthapp.common.DateTimeUtils;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.database.HealthRepository;
import com.example.healthapp.model.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 今日健康首页（v2.0）：小猫状态块 + 运动/摄入双栏 + 今日任务 + 开始运动。
 * 所有数字均来自 Room；小猫动画播放一次后自动回到静态图。
 */
public class HomeFragment extends Fragment {

    private static final long FRAME_MS = 130;
    private static final int FRAME_COUNT = 6;

    private TextView dateText;
    private TextView statusText;
    private TextView netLineText;
    private TextView minutesText;
    private TextView intakeText;
    private LinearLayout tasksContainer;
    private ImageView catImage;

    private HealthRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 聚合数据
    private UserProfile profile;
    private int todayMinutes;
    private double todayIntake;
    private double todayBurn;
    private boolean todayHasFood;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dateText = view.findViewById(R.id.text_home_date);
        statusText = view.findViewById(R.id.text_home_status);
        netLineText = view.findViewById(R.id.text_home_net_line);
        minutesText = view.findViewById(R.id.text_home_minutes);
        intakeText = view.findViewById(R.id.text_home_intake);
        tasksContainer = view.findViewById(R.id.container_home_tasks);
        catImage = view.findViewById(R.id.image_home_cat);
        repository = HealthRepository.getInstance(requireContext());

        dateText.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));

        view.findViewById(R.id.button_home_start).setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.navigation_exercise);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAll();
        int run = CatInteractionHub.drainPendingExercise();
        int eat = CatInteractionHub.drainPendingMeal();
        if (run > 0) {
            playCatAnimation(R.drawable.cat_run_anim);
            if (eat > 0) {
                handler.postDelayed(() -> playCatAnimation(R.drawable.cat_eat_anim),
                        FRAME_MS * FRAME_COUNT + 250);
            }
        } else if (eat > 0) {
            playCatAnimation(R.drawable.cat_eat_anim);
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    // ---------- 数据 ----------

    private void loadAll() {
        String today = DateTimeUtils.today();
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                profile = result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                render();
            }
        });
        repository.getExerciseMinutesForDate(today, new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                todayMinutes = result == null ? 0 : result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                render();
            }
        });
        repository.getTotalCaloriesForDate(today, new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                todayIntake = result == null ? 0 : result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                render();
            }
        });
        repository.getTotalExerciseCaloriesForDate(today, new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                todayBurn = result == null ? 0 : result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                render();
            }
        });
        repository.hasFoodRecordsForDate(today, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                todayHasFood = Boolean.TRUE.equals(result);
                render();
            }

            @Override
            public void onError(Throwable error) {
                render();
            }
        });
    }

    // ---------- 渲染 ----------

    private void render() {
        if (!isAdded()) {
            return;
        }
        minutesText.setText(String.format(Locale.getDefault(), "%,d", todayMinutes));
        intakeText.setText(formatKcal(todayIntake));
        renderStatus();
        renderTasks();
    }

    private void renderStatus() {
        if (profile == null || profile.dailyCalorieGoalKcal <= 0) {
            statusText.setText(R.string.home_status_no_goal);
            statusText.setTextColor(color(R.color.health_text_primary));
            netLineText.setText(R.string.profile_description);
            return;
        }
        double goal = profile.dailyCalorieGoalKcal;
        double net = todayIntake - todayBurn;
        if (todayIntake <= 0 && todayBurn <= 0) {
            statusText.setText(R.string.home_status_no_record);
            statusText.setTextColor(color(R.color.health_text_primary));
        } else if (net <= 0.75 * goal) {
            statusText.setText(R.string.home_status_good);
            statusText.setTextColor(color(R.color.health_accent_green));
        } else if (net <= goal) {
            statusText.setText(R.string.home_status_warning);
            statusText.setTextColor(color(R.color.health_accent_yellow));
        } else {
            statusText.setText(R.string.home_status_over);
            statusText.setTextColor(color(R.color.health_accent_red));
        }
        int percent = (int) Math.min(999, Math.round(todayIntake / goal * 100));
        netLineText.setText(getString(R.string.home_net_line,
                formatKcal(todayIntake), formatKcal(goal), percent));
    }

    private void renderTasks() {
        while (tasksContainer.getChildCount() > 1) {
            tasksContainer.removeViewAt(1);
        }
        int goalMinutes = profile != null && profile.dailyExerciseGoalMinutes > 0
                ? profile.dailyExerciseGoalMinutes : 30;
        boolean exerciseDone = todayMinutes >= goalMinutes;
        addTaskRow(getString(R.string.home_task_exercise, goalMinutes), exerciseDone);

        addTaskRow(getString(R.string.home_task_diet), todayHasFood);

        boolean calorieOk;
        if (profile == null || profile.dailyCalorieGoalKcal <= 0) {
            calorieOk = false;
        } else {
            calorieOk = todayIntake - todayBurn <= profile.dailyCalorieGoalKcal;
        }
        addTaskRow(getString(R.string.home_task_calorie), calorieOk);
    }

    private void addTaskRow(String label, boolean done) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (14 * getResources().getDisplayMetrics().density);
        row.setLayoutParams(params);

        TextView box = new TextView(requireContext());
        box.setText(done ? "☑" : "☐");
        box.setTextSize(22);
        box.setTextColor(color(done ? R.color.health_accent_blue : R.color.health_text_primary));
        row.addView(box);

        TextView text = new TextView(requireContext());
        text.setText(label);
        text.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart((int) (12 * getResources().getDisplayMetrics().density));
        text.setLayoutParams(textParams);
        row.addView(text);

        TextView state = new TextView(requireContext());
        state.setText(done ? R.string.home_task_done : R.string.home_task_todo);
        state.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        state.setTextColor(color(done ? R.color.health_accent_red : R.color.health_text_secondary));
        row.addView(state);

        tasksContainer.addView(row);
    }

    // ---------- 小猫动画 ----------

    private void playCatAnimation(int animRes) {
        if (!isAdded() || catImage == null) {
            return;
        }
        catImage.setImageResource(animRes);
        AnimationDrawable animation = (AnimationDrawable) catImage.getDrawable();
        animation.setOneShot(true);
        animation.start();
        handler.postDelayed(this::resetCat, FRAME_MS * FRAME_COUNT + 150);
    }

    private void resetCat() {
        if (isAdded() && catImage != null) {
            catImage.setImageResource(R.drawable.cat_home);
            loadAll(); // 动画播完刷新数字
        }
    }

    // ---------- 工具 ----------

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(requireContext(), res);
    }

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }
}
