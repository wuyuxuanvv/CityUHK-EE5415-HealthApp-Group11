package com.example.healthapp.diet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.example.healthapp.model.Food;
import com.example.healthapp.model.FoodRecord;
import com.example.healthapp.model.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 饮食记录页（v2.0）：黄色今日摄入大卡（目标+进度）+ 四餐次列表 + 添加餐食。
 * 营养公式（PPT）：每 100g 含量 × 克数 ÷ 100，保存时快照入库。
 */
public class DietFragment extends Fragment {

    private static final double MIN_GRAMS = 1;
    private static final double MAX_GRAMS = 2000;

    private static final String[] MEAL_TYPES = {
            FoodRecord.MEAL_BREAKFAST, FoodRecord.MEAL_LUNCH,
            FoodRecord.MEAL_DINNER, FoodRecord.MEAL_SNACK};
    private static final int[] MEAL_LABELS = {
            R.string.diet_meal_breakfast, R.string.diet_meal_lunch,
            R.string.diet_meal_dinner, R.string.diet_meal_snack};

    private TextView intakeText;
    private TextView goalText;
    private TextView percentText;
    private android.widget.ProgressBar intakeProgress;
    private LinearLayout mealsContainer;

    private HealthRepository repository;
    private final List<Food> allFoods = new ArrayList<>();
    private final Map<Long, String> foodNames = new HashMap<>();
    private List<FoodRecord> todayRecords = new ArrayList<>();
    private UserProfile profile;

    public DietFragment() {
        super(R.layout.fragment_diet);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = HealthRepository.getInstance(requireContext());

        intakeText = view.findViewById(R.id.text_diet_intake);
        goalText = view.findViewById(R.id.text_diet_goal);
        percentText = view.findViewById(R.id.text_diet_percent);
        intakeProgress = view.findViewById(R.id.progress_diet_intake);
        mealsContainer = view.findViewById(R.id.container_diet_meals);

        view.findViewById(R.id.button_diet_add).setOnClickListener(v -> showAddDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLibrary();
        loadProfile();
        loadToday();
    }

    // ---------- 数据加载 ----------

    private void loadLibrary() {
        repository.getAllFoods(new RepositoryCallback<List<Food>>() {
            @Override
            public void onSuccess(List<Food> result) {
                allFoods.clear();
                foodNames.clear();
                if (result != null) {
                    allFoods.addAll(result);
                    for (Food food : result) {
                        foodNames.put(food.id, food.name);
                    }
                }
                renderMeals();
            }

            @Override
            public void onError(Throwable error) {
                toast(R.string.diet_library_error);
            }
        });
    }

    private void loadProfile() {
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                profile = result;
                renderIntake();
            }

            @Override
            public void onError(Throwable error) {
                profile = null;
                renderIntake();
            }
        });
    }

    private void loadToday() {
        String today = DateTimeUtils.today();
        repository.getFoodRecordsForDate(today, new RepositoryCallback<List<FoodRecord>>() {
            @Override
            public void onSuccess(List<FoodRecord> result) {
                todayRecords = result == null ? new ArrayList<>() : result;
                renderMeals();
                renderIntake();
            }

            @Override
            public void onError(Throwable error) {
                todayRecords = new ArrayList<>();
                renderMeals();
            }
        });
    }

    // ---------- 渲染 ----------

    private double todayIntakeKcal() {
        double total = 0;
        for (FoodRecord record : todayRecords) {
            total += record.caloriesKcal;
        }
        return total;
    }

    private void renderIntake() {
        if (!isAdded()) {
            return;
        }
        double intake = todayIntakeKcal();
        intakeText.setText(formatKcal(intake));
        double goal = profile != null ? profile.dailyCalorieGoalKcal : 0;
        if (goal <= 0) {
            goalText.setText(R.string.diet_no_goal);
            percentText.setText("");
            intakeProgress.setProgress(0);
            return;
        }
        goalText.setText(getString(R.string.diet_goal_line, formatKcal(goal)));
        int percent = (int) Math.min(999, Math.round(intake / goal * 100));
        percentText.setText(percent + "%");
        intakeProgress.setProgress(Math.min(percent, 100));
    }

    private void renderMeals() {
        if (!isAdded()) {
            return;
        }
        mealsContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < MEAL_TYPES.length; i++) {
            String mealType = MEAL_TYPES[i];
            addMealHeader(mealType, MEAL_LABELS[i], density);
            List<FoodRecord> records = recordsOfMeal(mealType);
            if (records.isEmpty()) {
                addMealEmptyRow(density);
            } else {
                for (FoodRecord record : records) {
                    addMealRecordRow(record, density);
                }
            }
        }
    }

    private List<FoodRecord> recordsOfMeal(String mealType) {
        List<FoodRecord> result = new ArrayList<>();
        for (FoodRecord record : todayRecords) {
            if (mealType.equals(record.mealType)) {
                result.add(record);
            }
        }
        return result;
    }

    private void addMealHeader(String mealType, int labelRes, float density) {
        TextView header = new TextView(requireContext());
        header.setText(getString(labelRes));
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.health_text_primary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (14 * density);
        header.setLayoutParams(params);
        mealsContainer.addView(header);
    }

    private void addMealEmptyRow(float density) {
        TextView empty = new TextView(requireContext());
        empty.setText(getString(R.string.diet_meal_empty));
        empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        empty.setTextColor(ContextCompat.getColor(requireContext(),
                R.color.health_text_secondary));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (4 * density);
        params.setMarginStart((int) (16 * density));
        empty.setLayoutParams(params);
        mealsContainer.addView(empty);
    }

    private void addMealRecordRow(FoodRecord record, float density) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (4 * density);
        params.setMarginStart((int) (16 * density));
        row.setLayoutParams(params);

        String name = foodNames.getOrDefault(record.foodId, "食物");
        TextView line = new TextView(requireContext());
        line.setText(getString(R.string.diet_record_line, name,
                formatGrams(record.weightGrams), formatKcal(record.caloriesKcal)));
        line.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(line);

        TextView delete = new TextView(requireContext());
        delete.setText("✕");
        delete.setTextSize(20);
        delete.setPadding((int) (12 * density), 0, 0, 0);
        delete.setTextColor(ContextCompat.getColor(requireContext(), R.color.health_accent_red));
        delete.setOnClickListener(v -> confirmDelete(record));
        row.addView(delete);

        mealsContainer.addView(row);
    }

    private void confirmDelete(FoodRecord record) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.diet_delete)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        repository.deleteFoodRecord(record, new RepositoryCallback<Integer>() {
                            @Override
                            public void onSuccess(Integer result) {
                                toast(R.string.diet_deleted);
                                loadToday();
                            }

                            @Override
                            public void onError(Throwable error) {
                                toast(R.string.diet_save_error);
                            }
                        }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------- 添加餐食 ----------

    private void showAddDialog() {
        if (!isAdded()) {
            return;
        }
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_food, null);
        RadioGroup mealGroup = dialogView.findViewById(R.id.radio_diet_meal);
        Spinner spinner = dialogView.findViewById(R.id.spinner_diet_food);
        EditText gramsInput = dialogView.findViewById(R.id.input_diet_grams);
        TextView estimate = dialogView.findViewById(R.id.text_diet_estimate);

        List<String> labels = new ArrayList<>();
        for (Food food : allFoods) {
            labels.add(food.name + "（" + formatKcal(food.caloriesPer100g) + " 千卡/100g）");
        }
        if (allFoods.isEmpty()) {
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
                updateEstimate(spinner, gramsInput, estimate);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        gramsInput.addTextChangedListener(watcher);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateEstimate(spinner, gramsInput, estimate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        updateEstimate(spinner, gramsInput, estimate);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.diet_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.profile_save, (dialog, which) ->
                        saveRecord(mealGroup, spinner, gramsInput))
                .setNegativeButton(R.string.profile_cancel, null)
                .show();
    }

    private void updateEstimate(Spinner spinner, EditText gramsInput, TextView estimate) {
        Double grams = parseGrams(gramsInput.getText().toString());
        if (grams == null || allFoods.isEmpty()) {
            estimate.setText("");
            return;
        }
        int position = spinner.getSelectedItemPosition();
        Food food = allFoods.get(position < allFoods.size() ? position : 0);
        double kcal = HealthCalculations.calculateNutrientAmount(
                food.caloriesPer100g, grams);
        estimate.setText(getString(R.string.diet_kcal_estimate, formatKcal(kcal)));
    }

    private void saveRecord(RadioGroup mealGroup, Spinner spinner, EditText gramsInput) {
        Double grams = parseGrams(gramsInput.getText().toString());
        if (grams == null) {
            gramsInput.setError(getString(R.string.diet_error_grams));
            return;
        }
        if (allFoods.isEmpty()) {
            return;
        }
        int position = spinner.getSelectedItemPosition();
        Food food = allFoods.get(position < allFoods.size() ? position : 0);

        int checkedId = mealGroup.getCheckedRadioButtonId();
        String mealType = FoodRecord.MEAL_BREAKFAST;
        if (checkedId == R.id.radio_diet_lunch) {
            mealType = FoodRecord.MEAL_LUNCH;
        } else if (checkedId == R.id.radio_diet_dinner) {
            mealType = FoodRecord.MEAL_DINNER;
        } else if (checkedId == R.id.radio_diet_snack) {
            mealType = FoodRecord.MEAL_SNACK;
        }

        FoodRecord record = new FoodRecord();
        record.foodId = food.id;
        record.recordDate = DateTimeUtils.today();
        record.mealType = mealType;
        record.weightGrams = grams;
        record.caloriesKcal = HealthCalculations.calculateNutrientAmount(
                food.caloriesPer100g, grams);
        record.proteinGrams = HealthCalculations.calculateNutrientAmount(
                food.proteinPer100g, grams);
        record.carbsGrams = HealthCalculations.calculateNutrientAmount(
                food.carbsPer100g, grams);
        record.fatGrams = HealthCalculations.calculateNutrientAmount(
                food.fatPer100g, grams);

        repository.addFoodRecord(record, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long rowId) {
                if (!isAdded()) {
                    return;
                }
                CatInteractionHub.notifyMealLogged(record.caloriesKcal);
                toast(R.string.diet_saved);
                loadToday();
            }

            @Override
            public void onError(Throwable error) {
                if (isAdded()) {
                    toast(R.string.diet_save_error);
                }
            }
        });
    }

    // ---------- 工具 ----------

    @Nullable
    private static Double parseGrams(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return value >= MIN_GRAMS && value <= MAX_GRAMS ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    private static String formatGrams(double value) {
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    private void toast(int messageRes) {
        if (isAdded()) {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        }
    }
}
