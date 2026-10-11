package com.example.healthapp.exercise;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.healthapp.R;
import com.example.healthapp.common.CatInteractionHub;
import com.example.healthapp.common.DateTimeUtils;
import com.example.healthapp.common.HealthCalculations;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.database.HealthRepository;
import com.example.healthapp.model.Exercise;
import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.UserProfile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 运动记录页（v2.0）：分类标签 + 今日分钟 + 本周目标 X/Y 天 + 今日记录列表 + 开始运动。
 * 热量估算公式（PPT）：MET × 体重kg × 小时。
 */
public class ExerciseFragment extends Fragment {

    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 600;

    private TextView minutesText;
    private TextView weekGoalText;
    private android.widget.ProgressBar weekProgress;
    private LinearLayout recordsContainer;
    private TextView emptyText;

    private HealthRepository repository;
    private final List<Exercise> allExercises = new ArrayList<>();
    private final Map<Long, String> exerciseNames = new HashMap<>();
    private List<ExerciseRecord> todayRecords = new ArrayList<>();
    private UserProfile profile;

    private String selectedCategory = "";
    private final Map<String, com.google.android.material.button.MaterialButton> chips =
            new HashMap<>();

    public ExerciseFragment() {
        super(R.layout.fragment_exercise);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = HealthRepository.getInstance(requireContext());

        minutesText = view.findViewById(R.id.text_exercise_minutes);
        weekGoalText = view.findViewById(R.id.text_exercise_week_goal);
        weekProgress = view.findViewById(R.id.progress_exercise_week);
        recordsContainer = view.findViewById(R.id.container_exercise_records);
        emptyText = view.findViewById(R.id.text_exercise_empty);

        chips.put("", view.findViewById(R.id.chip_exercise_all));
        chips.put("有氧", view.findViewById(R.id.chip_exercise_aerobic));
        chips.put("身心", view.findViewById(R.id.chip_exercise_mind));
        chips.put("球类", view.findViewById(R.id.chip_exercise_ball));
        for (Map.Entry<String, com.google.android.material.button.MaterialButton> entry
                : chips.entrySet()) {
            entry.getValue().setOnClickListener(v -> selectCategory(entry.getKey()));
        }
        selectCategory("");

        view.findViewById(R.id.button_exercise_start).setOnClickListener(v -> showAddDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLibrary();
        loadProfile();
        loadToday();
        loadWeek();
    }

    // ---------- 数据加载 ----------

    private void loadLibrary() {
        repository.getAllExercises(new RepositoryCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> result) {
                allExercises.clear();
                exerciseNames.clear();
                if (result != null) {
                    allExercises.addAll(result);
                    for (Exercise exercise : result) {
                        exerciseNames.put(exercise.id, exercise.name);
                    }
                }
                renderRecords();
            }

            @Override
            public void onError(Throwable error) {
                toast(R.string.exercise_library_error);
            }
        });
    }

    private void loadProfile() {
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                profile = result;
            }

            @Override
            public void onError(Throwable error) {
                profile = null;
            }
        });
    }

    private void loadToday() {
        String today = DateTimeUtils.today();
        repository.getExerciseMinutesForDate(today, new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (isAdded()) {
                    minutesText.setText(String.format(Locale.getDefault(), "%,d",
                            result == null ? 0 : result));
                }
            }

            @Override
            public void onError(Throwable error) {
                // 保持现状
            }
        });
        repository.getExerciseRecordsForDate(today, new RepositoryCallback<List<ExerciseRecord>>() {
            @Override
            public void onSuccess(List<ExerciseRecord> result) {
                todayRecords = result == null ? new ArrayList<>() : result;
                renderRecords();
            }

            @Override
            public void onError(Throwable error) {
                todayRecords = new ArrayList<>();
                renderRecords();
            }
        });
    }

    private void loadWeek() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        String start = DateTimeUtils.formatDate(monday);
        String end = DateTimeUtils.formatDate(sunday);
        repository.getDistinctExerciseDaysInRange(start, end,
                new RepositoryCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer days) {
                        if (!isAdded()) {
                            return;
                        }
                        int goalDays = profile != null && profile.weeklyExerciseGoalCount > 0
                                ? profile.weeklyExerciseGoalCount : 5;
                        int done = Math.min(days == null ? 0 : days, 7);
                        weekGoalText.setText(getString(R.string.exercise_week_goal_progress,
                                done, goalDays));
                        weekProgress.setMax(goalDays);
                        weekProgress.setProgress(Math.min(done, goalDays));
                    }

                    @Override
                    public void onError(Throwable error) {
                        // 保持现状
                    }
                });
    }

    // ---------- 分类标签 ----------

    private void selectCategory(String category) {
        selectedCategory = category;
        int yellow = ContextCompat.getColor(requireContext(), R.color.health_primary_container);
        int white = ContextCompat.getColor(requireContext(), R.color.health_surface);
        int black = ContextCompat.getColor(requireContext(), R.color.health_primary);
        for (Map.Entry<String, com.google.android.material.button.MaterialButton> entry
                : chips.entrySet()) {
            boolean selected = entry.getKey().equals(category);
            com.google.android.material.button.MaterialButton button = entry.getValue();
            button.setBackgroundTintList(ColorStateList.valueOf(selected ? yellow : white));
            button.setTextColor(black);
            button.setStrokeColor(ColorStateList.valueOf(black));
        }
    }

    // ---------- 今日记录列表 ----------

    private void renderRecords() {
        if (!isAdded()) {
            return;
        }
        recordsContainer.removeAllViews();
        emptyText.setVisibility(todayRecords.isEmpty() ? View.VISIBLE : View.GONE);
        float density = getResources().getDisplayMetrics().density;
        for (ExerciseRecord record : todayRecords) {
            String name = exerciseNames.getOrDefault(record.exerciseId, "运动");
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = (int) (10 * density);
            row.setLayoutParams(params);

            TextView line = new TextView(requireContext());
            line.setText(getString(R.string.exercise_record_line, name,
                    record.durationMinutes, formatKcal(record.estimatedCaloriesKcal)));
            line.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            row.addView(line);

            TextView delete = new TextView(requireContext());
            delete.setText("✕");
            delete.setTextSize(20);
            delete.setPadding((int) (12 * density), 0, 0, 0);
            delete.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.health_accent_red));
            delete.setOnClickListener(v -> confirmDelete(record));
            row.addView(delete);

            recordsContainer.addView(row);
        }
    }

    private void confirmDelete(ExerciseRecord record) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.exercise_delete)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        repository.deleteExerciseRecord(record,
                                new RepositoryCallback<Integer>() {
                                    @Override
                                    public void onSuccess(Integer result) {
                                        toast(R.string.exercise_deleted);
                                        loadToday();
                                        loadWeek();
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        toast(R.string.exercise_save_error);
                                    }
                                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------- 添加运动 ----------

    private void showAddDialog() {
        if (!isAdded()) {
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_exercise, null);
        Spinner spinner = dialogView.findViewById(R.id.spinner_exercise);
        EditText minutesInput = dialogView.findViewById(R.id.input_exercise_minutes);
        TextView estimate = dialogView.findViewById(R.id.text_exercise_estimate);

        List<Exercise> filtered = filterByCategory();
        List<String> labels = new ArrayList<>();
        for (Exercise exercise : filtered) {
            labels.add(exercise.name + "（" + exercise.category + " · MET "
                    + formatMet(exercise.metValue) + "）");
        }
        if (filtered.isEmpty()) {
            labels.add("（无）");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateEstimate(spinner, minutesInput, estimate, filtered);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        minutesInput.addTextChangedListener(watcher);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateEstimate(spinner, minutesInput, estimate, filtered);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.exercise_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.profile_save, (dialog, which) ->
                        saveRecord(spinner, minutesInput, filtered))
                .setNegativeButton(R.string.profile_cancel, null)
                .show();
    }

    private List<Exercise> filterByCategory() {
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise exercise : allExercises) {
            if (selectedCategory.isEmpty()
                    || selectedCategory.equals(exercise.category)) {
                filtered.add(exercise);
            }
        }
        return filtered;
    }

    private void updateEstimate(Spinner spinner, EditText minutesInput, TextView estimate,
                                List<Exercise> filtered) {
        Integer minutes = parseMinutes(minutesInput.getText().toString());
        double weight = profile != null ? profile.weightKg : 0;
        if (minutes == null || filtered.isEmpty() || weight <= 0) {
            estimate.setText(weight <= 0 && minutes != null
                    ? R.string.exercise_kcal_unknown
                    : R.string.exercise_kcal_unknown);
            return;
        }
        Exercise exercise = filtered.get(spinner.getSelectedItemPosition() < filtered.size()
                ? spinner.getSelectedItemPosition() : 0);
        double kcal = HealthCalculations.estimateExerciseCalories(
                exercise.metValue, weight, minutes);
        estimate.setText(getString(R.string.exercise_kcal_estimate, formatKcal(kcal)));
    }

    private void saveRecord(Spinner spinner, EditText minutesInput, List<Exercise> filtered) {
        if (profile == null || profile.weightKg <= 0) {
            toast(R.string.exercise_need_profile);
            return;
        }
        Integer minutes = parseMinutes(minutesInput.getText().toString());
        if (minutes == null) {
            minutesInput.setError(getString(R.string.exercise_error_minutes));
            return;
        }
        if (filtered.isEmpty()) {
            return;
        }
        int position = spinner.getSelectedItemPosition();
        Exercise exercise = filtered.get(position < filtered.size() ? position : 0);

        ExerciseRecord record = new ExerciseRecord();
        record.exerciseId = exercise.id;
        record.recordDate = DateTimeUtils.today();
        record.durationMinutes = minutes;
        record.estimatedCaloriesKcal = HealthCalculations.estimateExerciseCalories(
                exercise.metValue, profile.weightKg, minutes);

        repository.addExerciseRecord(record, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long rowId) {
                if (!isAdded()) {
                    return;
                }
                CatInteractionHub.notifyExerciseLogged(record.estimatedCaloriesKcal);
                toast(R.string.exercise_saved);
                loadToday();
                loadWeek();
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    toast(R.string.exercise_save_error);
                }
            }
        });
    }

    // ---------- 工具 ----------

    @Nullable
    private static Integer parseMinutes(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= MIN_MINUTES && value <= MAX_MINUTES ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    private static String formatMet(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private void toast(int messageRes) {
        if (isAdded()) {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        }
    }
}
