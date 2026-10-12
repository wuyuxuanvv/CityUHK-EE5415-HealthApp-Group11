package com.example.healthapp.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.healthapp.home.pet.PetManager;
import com.example.healthapp.home.pet.PetView;
import com.example.healthapp.model.UserProfile;
import com.example.healthapp.model.ExerciseRecord;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

/**
 * 今日健康首页（v2.0）：小猫状态块 + 运动/摄入双栏 + 今日任务 + 开始运动。
 * 所有数字均来自 Room；宠物模块独立管理循环待机和每日奖励。
 */
public class HomeFragment extends Fragment implements CatInteractionHub.CatMascot {

    private TextView dateText;
    private TextView statusText;
    private TextView netLineText;
    private TextView minutesText;
    private TextView intakeText;
    private TextView profileHintText;
    private LinearLayout tasksContainer;
    private PetManager petManager;
    private int loadGeneration;
    private String loadedDate;

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
        petManager = new PetManager(requireContext(),
                (PetView) view.findViewById(R.id.image_home_cat),
                view.findViewById(R.id.text_home_pet_status),
                view.findViewById(R.id.text_home_pet_earned));
        profileHintText = view.findViewById(R.id.text_home_profile_hint);
        repository = HealthRepository.getInstance(requireContext());

        dateText.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));

        profileHintText.setOnClickListener(v -> {
            if (getActivity() != null) {
                BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                nav.setSelectedItemId(R.id.navigation_profile);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        petManager.start();
        CatInteractionHub.register(this);
        CatInteractionHub.drainPendingExercise();
        CatInteractionHub.drainPendingMeal();
        loadAll();
        handler.postDelayed(dayCheck, 30_000);
    }

    private final Runnable dayCheck = new Runnable() {
        @Override public void run() {
            if (!isResumed() || petManager == null) return;
            if (!DateTimeUtils.today().equals(loadedDate)) loadAll();
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    public void onExerciseLogged(double kcalBurned) {
        refreshFromEvent();
    }

    @Override
    public void onMealLogged(double kcalIntake) {
        refreshFromEvent();
    }

    private void refreshFromEvent() {
        handler.post(() -> {
            if (isResumed() && petManager != null) loadAll();
        });
    }

    @Override
    public void onPause() {
        CatInteractionHub.unregister(this);
        loadGeneration++;
        handler.removeCallbacksAndMessages(null);
        if (petManager != null) petManager.stop();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        loadGeneration++;
        if (petManager != null) petManager.stop();
        petManager = null;
        super.onDestroyView();
    }

    // ---------- 数据 ----------

    private void loadAll() {
        String today = DateTimeUtils.today();
        loadedDate = today;
        final int generation = ++loadGeneration;
        petManager.loading(today);
        repository.getExerciseRecordsForDate(today,
                new RepositoryCallback<List<ExerciseRecord>>() {
                    @Override
                    public void onSuccess(List<ExerciseRecord> records) {
                        if (!isCurrentLoad(generation, today)) return;
                        if (records == null) {
                            petManager.error();
                            return;
                        }
                        List<Long> ids = new ArrayList<>();
                        for (ExerciseRecord record : records) {
                            if (record == null || record.id <= 0 || !today.equals(record.recordDate)) {
                                petManager.error();
                                return;
                            }
                            ids.add(record.id);
                        }
                        petManager.update(today, ids);
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (isCurrentLoad(generation, today)) petManager.error();
                    }
                });
        dateText.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                if (!isCurrentLoad(generation, today)) return;
                profile = result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (!isCurrentLoad(generation, today)) return;
                render();
            }
        });
        repository.getExerciseMinutesForDate(today, new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (!isCurrentLoad(generation, today)) return;
                todayMinutes = result == null ? 0 : result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (!isCurrentLoad(generation, today)) return;
                render();
            }
        });
        repository.getTotalCaloriesForDate(today, new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                if (!isCurrentLoad(generation, today)) return;
                if (result == null || !Double.isFinite(result) || result < 0) {
                    return;
                }
                todayIntake = result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (!isCurrentLoad(generation, today)) return;
                render();
            }
        });
        repository.getTotalExerciseCaloriesForDate(today, new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                if (!isCurrentLoad(generation, today)) return;
                if (result == null || !Double.isFinite(result) || result < 0) {
                    return;
                }
                todayBurn = result;
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (!isCurrentLoad(generation, today)) return;
                render();
            }
        });
        repository.hasFoodRecordsForDate(today, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (!isCurrentLoad(generation, today)) return;
                todayHasFood = Boolean.TRUE.equals(result);
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (!isCurrentLoad(generation, today)) return;
                render();
            }
        });
    }

    // ---------- 渲染 ----------

    private boolean isCurrentLoad(int generation, String date) {
        return generation == loadGeneration && isResumed() && getView() != null
                && DateTimeUtils.today().equals(date);
    }

    private void render() {
        if (!isAdded() || getView() == null) {
            return;
        }
        minutesText.setText(String.format(Locale.getDefault(), "%,d", todayMinutes));
        intakeText.setText(formatKcal(todayIntake));
        renderStatus();
        renderTasks();
        profileHintText.setVisibility(isProfileComplete(profile) ? View.GONE : View.VISIBLE);
    }

    /** 资料完整性：身高/体重/年龄/每日摄入目标齐全才算完整 */
    private static boolean isProfileComplete(UserProfile p) {
        return p != null
                && p.heightCm > 0
                && p.weightKg > 0
                && p.age > 0
                && p.dailyCalorieGoalKcal > 0;
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

    // ---------- 工具 ----------

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(requireContext(), res);
    }

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }
}
