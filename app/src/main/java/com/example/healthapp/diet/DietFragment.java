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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthapp.R;
import com.example.healthapp.common.CatInteractionHub;
import com.example.healthapp.common.DateTimeUtils;
import com.example.healthapp.common.HealthCalculations;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.database.HealthRepository;
import com.example.healthapp.database.SeedData;
import com.example.healthapp.model.Food;
import com.example.healthapp.model.FoodRecord;
import com.example.healthapp.model.UserProfile;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private static final List<String> CATEGORY_ORDER = Arrays.asList(
            Food.CATEGORY_STAPLE, Food.CATEGORY_MEAT_EGG_DAIRY_BEAN,
            Food.CATEGORY_VEGETABLE, Food.CATEGORY_FRUIT, Food.CATEGORY_NUT,
            Food.CATEGORY_DRINK, Food.CATEGORY_FAST_FOOD, Food.CATEGORY_OTHER);

    private TextView intakeText;
    private TextView goalText;
    private TextView percentText;
    private android.widget.ProgressBar intakeProgress;
    private LinearLayout mealsContainer;
    private LinearLayout weekDaysContainer;
    private TextView weekSummaryText;
    private TextView mealsTitleText;

    private HealthRepository repository;
    private final List<Food> allFoods = new ArrayList<>();
    private final Map<Long, String> foodNames = new HashMap<>();
    private List<FoodRecord> todayRecords = new ArrayList<>();
    private List<FoodRecord> displayedRecords = new ArrayList<>();
    private final List<WeekDayView> weekDayViews = new ArrayList<>();
    private Set<String> recordedWeekDates = new HashSet<>();
    private LocalDate weekMonday;
    private String selectedRecordDate = DateTimeUtils.today();
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
        weekDaysContainer = view.findViewById(R.id.container_diet_week_days);
        weekSummaryText = view.findViewById(R.id.text_diet_week_summary);
        mealsTitleText = view.findViewById(R.id.text_diet_meals_title);
        setupWeekDayViews();

        view.findViewById(R.id.button_diet_add).setOnClickListener(v -> showAddDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLibrary();
        loadProfile();
        loadToday();
        loadWeekRecords();
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
                    allFoods.removeIf(food -> SeedData.isRetiredFoodName(food.name));
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
                if (DateTimeUtils.today().equals(selectedRecordDate)) {
                    displayedRecords = new ArrayList<>(todayRecords);
                }
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
        if (DateTimeUtils.today().equals(selectedRecordDate)) {
            mealsTitleText.setText(R.string.diet_today_meals_title);
        } else {
            mealsTitleText.setText(getString(R.string.diet_selected_meals, selectedRecordDate));
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
        for (FoodRecord record : displayedRecords) {
            if (mealType.equals(record.mealType)) {
                result.add(record);
            }
        }
        return result;
    }

    private void setupWeekDayViews() {
        String[] labels = getResources().getStringArray(R.array.diet_week_day_labels);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String label : labels) {
            View item = inflater.inflate(R.layout.item_diet_week_day, weekDaysContainer, false);
            WeekDayView day = new WeekDayView(item);
            day.label.setText(label);
            weekDayViews.add(day);
            weekDaysContainer.addView(item);
        }
    }

    private void renderWeek() {
        if (weekMonday == null || !isAdded()) return;
        LocalDate today = LocalDate.now();
        int recorded = 0;
        for (int i = 0; i < weekDayViews.size(); i++) {
            LocalDate date = weekMonday.plusDays(i);
            String dateText = DateTimeUtils.formatDate(date);
            WeekDayView view = weekDayViews.get(i);
            boolean future = date.isAfter(today);
            boolean hasRecord = recordedWeekDates.contains(dateText);
            if (hasRecord) recorded++;
            view.date.setText(String.format(Locale.getDefault(), "%d", date.getDayOfMonth()));
            view.status.setText(future ? R.string.diet_week_status_future
                    : hasRecord ? R.string.diet_week_status_recorded : R.string.diet_week_status_empty);
            view.status.setTextColor(ContextCompat.getColor(requireContext(), future ? R.color.health_grey_light
                    : hasRecord ? R.color.health_accent_green : R.color.health_text_primary));
            view.item.setOnClickListener(future ? null : v -> {
                selectedRecordDate = dateText;
                loadSelectedDateRecords();
                renderWeek();
            });
        }
        weekSummaryText.setText(getString(R.string.diet_week_recorded_days, recorded));
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
                                loadSelectedDateRecords();
                                loadWeekRecords();
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
        Spinner categorySpinner = dialogView.findViewById(R.id.spinner_diet_category);
        RecyclerView foodPicker = dialogView.findViewById(R.id.list_diet_food_picker);
        EditText gramsInput = dialogView.findViewById(R.id.input_diet_grams);
        TextView estimate = dialogView.findViewById(R.id.text_diet_estimate);

        List<String> categories = foodCategories();
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        final Food[] selectedFood = new Food[1];
        FoodPickerAdapter foodAdapter = new FoodPickerAdapter(requireContext(), food -> {
            selectedFood[0] = food;
            updateEstimate(food, gramsInput, estimate);
        });
        foodPicker.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        foodPicker.setAdapter(foodAdapter);
        selectedFood[0] = populateFoodGrid(categories.isEmpty()
                ? Food.CATEGORY_OTHER : categories.get(0), foodAdapter);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateEstimate(selectedFood[0], gramsInput, estimate);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        gramsInput.addTextChangedListener(watcher);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String category = categories.get(position < categories.size() ? position : 0);
                selectedFood[0] = populateFoodGrid(category, foodAdapter);
                updateEstimate(selectedFood[0], gramsInput, estimate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        updateEstimate(selectedFood[0], gramsInput, estimate);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.diet_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.profile_save, (dialog, which) ->
                        saveRecord(mealGroup, selectedFood[0], gramsInput))
                .setNegativeButton(R.string.profile_cancel, null)
                .show();
    }

    private void loadSelectedDateRecords() {
        repository.getFoodRecordsForDate(selectedRecordDate, new RepositoryCallback<List<FoodRecord>>() {
            @Override public void onSuccess(List<FoodRecord> result) {
                displayedRecords = result == null ? new ArrayList<>() : new ArrayList<>(result);
                renderMeals();
            }
            @Override public void onError(Throwable error) { displayedRecords = new ArrayList<>(); renderMeals(); }
        });
    }

    private void loadWeekRecords() {
        LocalDate today = LocalDate.now();
        weekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = weekMonday.plusDays(6);
        weekSummaryText.setText(R.string.diet_week_loading);
        repository.getFoodRecordsInRange(DateTimeUtils.formatDate(weekMonday), DateTimeUtils.formatDate(sunday),
                new RepositoryCallback<List<FoodRecord>>() {
                    @Override public void onSuccess(List<FoodRecord> result) {
                        recordedWeekDates = new HashSet<>();
                        if (result != null) for (FoodRecord record : result) recordedWeekDates.add(record.recordDate);
                        renderWeek();
                    }
                    @Override public void onError(Throwable error) { weekSummaryText.setText(R.string.diet_week_error); }
                });
    }

    private List<String> foodCategories() {
        Set<String> availableCategories = new LinkedHashSet<>();
        for (Food food : allFoods) {
            availableCategories.add(normalizedCategory(food));
        }
        List<String> categories = new ArrayList<>();
        for (String category : CATEGORY_ORDER) {
            if (availableCategories.contains(category)) {
                categories.add(category);
            }
        }
        categories.addAll(availableCategories);
        Set<String> uniqueCategories = new LinkedHashSet<>(categories);
        if (categories.isEmpty()) {
            uniqueCategories.add(Food.CATEGORY_OTHER);
        }
        return new ArrayList<>(uniqueCategories);
    }

    private String normalizedCategory(Food food) {
        if (Food.CATEGORY_MEAT_EGG.equals(food.category)
                || Food.CATEGORY_DAIRY_BEAN.equals(food.category)) {
            return Food.CATEGORY_MEAT_EGG_DAIRY_BEAN;
        }
        return food.category == null || food.category.trim().isEmpty()
                ? Food.CATEGORY_OTHER : food.category;
    }

    @Nullable
    private Food populateFoodGrid(String category, FoodPickerAdapter adapter) {
        List<Food> foodsInCategory = new ArrayList<>();
        for (Food food : allFoods) {
            if (category.equals(normalizedCategory(food))) {
                foodsInCategory.add(food);
            }
        }
        // 中文 Collator 按拼音顺序比较，因此同一类别中的食物会按拼音首字母排列。
        final Collator chineseCollator = Collator.getInstance(Locale.CHINA);
        Collections.sort(foodsInCategory, new Comparator<Food>() {
            @Override
            public int compare(Food left, Food right) {
                return chineseCollator.compare(left.name, right.name);
            }
        });
        adapter.replaceFoods(foodsInCategory);
        return adapter.getSelectedFood();
    }

    private void updateEstimate(@Nullable Food selectedFood, EditText gramsInput, TextView estimate) {
        Double grams = parseGrams(gramsInput.getText().toString());
        if (grams == null || selectedFood == null) {
            estimate.setText("");
            return;
        }
        double kcal = HealthCalculations.calculateNutrientAmount(
                selectedFood.caloriesPer100g, grams);
        estimate.setText(getString(R.string.diet_kcal_estimate, formatKcal(kcal)));
    }

    private void saveRecord(RadioGroup mealGroup, @Nullable Food food, EditText gramsInput) {
        Double grams = parseGrams(gramsInput.getText().toString());
        if (grams == null) {
            gramsInput.setError(getString(R.string.diet_error_grams));
            return;
        }
        if (food == null) {
            return;
        }

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
        record.recordDate = selectedRecordDate;
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
                loadSelectedDateRecords();
                loadWeekRecords();
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

    private static final class WeekDayView {
        final View item;
        final TextView status;
        final TextView label;
        final TextView date;

        WeekDayView(View item) {
            this.item = item;
            status = item.findViewById(R.id.text_diet_week_day_status);
            label = item.findViewById(R.id.text_diet_week_day_label);
            date = item.findViewById(R.id.text_diet_week_day_date);
        }
    }
}
