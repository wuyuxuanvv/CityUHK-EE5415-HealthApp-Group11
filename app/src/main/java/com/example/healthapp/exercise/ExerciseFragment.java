package com.example.healthapp.exercise;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Exercise page backed by the existing Room repository and MET calculation contract. */
public class ExerciseFragment extends Fragment {

    private static final String STATE_RECORD_DATE = "exercise_record_date";
    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 600;

    private enum LoadState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private TextView todayMinutesValue;
    private TextView todayMinutesState;
    private TextView todayCaloriesValue;
    private TextView todayCaloriesState;
    private TextView weekSummaryText;
    private TextView weekGoalText;
    private TextView weekStateText;
    private LinearLayout weekDaysContainer;
    private TextView recordsStateText;
    private LinearLayout recordsContainer;
    private MaterialButton recordsDateButton;

    private HealthRepository repository;
    private final List<Exercise> allExercises = new ArrayList<>();
    private final Map<Long, String> exerciseNames = new HashMap<>();
    private final List<WeekDayView> weekDayViews = new ArrayList<>();
    private List<ExerciseRecord> selectedDateRecords = new ArrayList<>();
    private Set<String> completedWeekDates = new HashSet<>();

    private LoadState libraryState = LoadState.IDLE;
    private LoadState profileState = LoadState.IDLE;
    private LoadState weekState = LoadState.IDLE;
    private LoadState recordsState = LoadState.IDLE;
    private Double currentWeightKg;
    private Integer weeklyGoalDays;
    private int completedWeekDays;
    private LocalDate currentWeekMonday;
    private String selectedRecordDate = DateTimeUtils.today();
    private AddExerciseDialogController activeAddDialog;

    public ExerciseFragment() {
        super(R.layout.fragment_exercise);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = HealthRepository.getInstance(requireContext());

        todayMinutesValue = view.findViewById(R.id.text_exercise_today_minutes_value);
        todayMinutesState = view.findViewById(R.id.text_exercise_today_minutes_state);
        todayCaloriesValue = view.findViewById(R.id.text_exercise_today_calories_value);
        todayCaloriesState = view.findViewById(R.id.text_exercise_today_calories_state);
        weekSummaryText = view.findViewById(R.id.text_exercise_week_summary);
        weekGoalText = view.findViewById(R.id.text_exercise_week_goal_value);
        weekStateText = view.findViewById(R.id.text_exercise_week_state);
        weekDaysContainer = view.findViewById(R.id.container_exercise_week_days);
        recordsStateText = view.findViewById(R.id.text_exercise_records_state);
        recordsContainer = view.findViewById(R.id.container_exercise_records);
        recordsDateButton = view.findViewById(R.id.button_exercise_record_date);

        if (savedInstanceState != null) {
            String restoredDate = savedInstanceState.getString(STATE_RECORD_DATE);
            if (isValidPastOrToday(restoredDate)) {
                selectedRecordDate = restoredDate;
            }
        }

        setupWeekDayViews();
        updateRecordsDateButton();
        recordsDateButton.setOnClickListener(v -> showRecordDatePicker());
        view.findViewById(R.id.button_exercise_add_record)
                .setOnClickListener(v -> showAddDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLibrary();
        loadProfile();
        loadTodaySummary();
        loadWeekRecords();
        loadSelectedDateRecords();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_RECORD_DATE, selectedRecordDate);
    }

    @Override
    public void onDestroyView() {
        activeAddDialog = null;
        super.onDestroyView();
    }

    private void loadLibrary() {
        libraryState = LoadState.LOADING;
        updateActiveDialogLibrary();
        repository.getAllExercises(new RepositoryCallback<List<Exercise>>() {
            @Override
            public void onSuccess(List<Exercise> result) {
                libraryState = LoadState.SUCCESS;
                allExercises.clear();
                exerciseNames.clear();
                if (result != null) {
                    allExercises.addAll(result);
                    for (Exercise exercise : result) {
                        exerciseNames.put(exercise.id, exercise.name);
                    }
                }
                if (isUiReady()) {
                    renderRecords();
                    updateActiveDialogLibrary();
                }
            }

            @Override
            public void onError(Throwable error) {
                libraryState = LoadState.ERROR;
                allExercises.clear();
                exerciseNames.clear();
                if (isUiReady()) {
                    renderRecords();
                    updateActiveDialogLibrary();
                }
            }
        });
    }

    private void loadProfile() {
        profileState = LoadState.LOADING;
        currentWeightKg = null;
        weeklyGoalDays = null;
        renderWeekGoal();
        updateActiveDialogEstimate();
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                profileState = LoadState.SUCCESS;
                currentWeightKg = result != null && result.weightKg > 0
                        ? result.weightKg : null;
                weeklyGoalDays = result != null && result.weeklyExerciseGoalCount > 0
                        ? result.weeklyExerciseGoalCount : null;
                if (isUiReady()) {
                    renderWeekGoal();
                    updateActiveDialogEstimate();
                }
            }

            @Override
            public void onError(Throwable error) {
                profileState = LoadState.ERROR;
                currentWeightKg = null;
                weeklyGoalDays = null;
                if (isUiReady()) {
                    renderWeekGoal();
                    updateActiveDialogEstimate();
                }
            }
        });
    }

    private void loadTodaySummary() {
        todayMinutesValue.setText(R.string.exercise_value_unavailable);
        todayMinutesState.setText(R.string.exercise_loading);
        todayMinutesState.setVisibility(View.VISIBLE);
        todayCaloriesValue.setText(R.string.exercise_value_unavailable);
        todayCaloriesState.setText(R.string.exercise_loading);
        todayCaloriesState.setVisibility(View.VISIBLE);

        String today = DateTimeUtils.today();
        repository.getExerciseMinutesForDate(today, new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                if (!isUiReady()) {
                    return;
                }
                int minutes = result == null ? 0 : result;
                if (minutes < 0) {
                    showSummaryUnavailable(todayMinutesValue, todayMinutesState);
                    return;
                }
                todayMinutesValue.setText(String.format(Locale.getDefault(), "%,d", minutes));
                todayMinutesState.setVisibility(View.GONE);
            }

            @Override
            public void onError(Throwable error) {
                if (isUiReady()) {
                    showSummaryError(todayMinutesValue, todayMinutesState);
                }
            }
        });

        repository.getTotalExerciseCaloriesForDate(today,
                new RepositoryCallback<Double>() {
                    @Override
                    public void onSuccess(Double result) {
                        if (!isUiReady()) {
                            return;
                        }
                        double kcal = result == null ? 0.0 : result;
                        if (!Double.isFinite(kcal) || kcal < 0) {
                            showSummaryUnavailable(todayCaloriesValue, todayCaloriesState);
                            return;
                        }
                        todayCaloriesValue.setText(formatKcal(kcal));
                        todayCaloriesState.setText(R.string.exercise_estimated_label);
                        todayCaloriesState.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (isUiReady()) {
                            showSummaryError(todayCaloriesValue, todayCaloriesState);
                        }
                    }
                });
    }

    private void showSummaryError(TextView valueView, TextView stateView) {
        valueView.setText(R.string.exercise_value_unavailable);
        stateView.setText(R.string.exercise_read_error_short);
        stateView.setVisibility(View.VISIBLE);
    }

    private void showSummaryUnavailable(TextView valueView, TextView stateView) {
        valueView.setText(R.string.exercise_value_unavailable);
        stateView.setText(R.string.exercise_cannot_calculate);
        stateView.setVisibility(View.VISIBLE);
    }

    private void loadWeekRecords() {
        LocalDate today = LocalDate.now();
        currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = currentWeekMonday.plusDays(6);
        weekState = LoadState.LOADING;
        completedWeekDates = new HashSet<>();
        completedWeekDays = 0;
        renderWeek();

        repository.getExerciseRecordsInRange(DateTimeUtils.formatDate(currentWeekMonday),
                DateTimeUtils.formatDate(sunday),
                new RepositoryCallback<List<ExerciseRecord>>() {
                    @Override
                    public void onSuccess(List<ExerciseRecord> result) {
                        weekState = LoadState.SUCCESS;
                        Set<String> completedDates = new HashSet<>();
                        if (result != null) {
                            for (ExerciseRecord record : result) {
                                try {
                                    LocalDate recordDate = DateTimeUtils.parseDate(record.recordDate);
                                    if (!recordDate.isBefore(currentWeekMonday)
                                            && !recordDate.isAfter(currentWeekMonday.plusDays(6))
                                            && !recordDate.isAfter(LocalDate.now())) {
                                        completedDates.add(record.recordDate);
                                    }
                                } catch (DateTimeParseException ignored) {
                                    // Ignore malformed legacy rows instead of presenting fake status.
                                }
                            }
                        }
                        completedWeekDates = completedDates;
                        completedWeekDays = completedDates.size();
                        if (isUiReady()) {
                            renderWeek();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        weekState = LoadState.ERROR;
                        completedWeekDates = new HashSet<>();
                        completedWeekDays = 0;
                        if (isUiReady()) {
                            renderWeek();
                        }
                    }
                });
    }

    private void loadSelectedDateRecords() {
        recordsState = LoadState.LOADING;
        selectedDateRecords = new ArrayList<>();
        renderRecords();
        repository.getExerciseRecordsForDate(selectedRecordDate,
                new RepositoryCallback<List<ExerciseRecord>>() {
                    @Override
                    public void onSuccess(List<ExerciseRecord> result) {
                        recordsState = LoadState.SUCCESS;
                        selectedDateRecords = result == null
                                ? new ArrayList<>() : new ArrayList<>(result);
                        if (isUiReady()) {
                            renderRecords();
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        recordsState = LoadState.ERROR;
                        selectedDateRecords = new ArrayList<>();
                        if (isUiReady()) {
                            renderRecords();
                        }
                    }
                });
    }

    private void setupWeekDayViews() {
        weekDayViews.clear();
        weekDaysContainer.removeAllViews();
        String[] dayLabels = getResources().getStringArray(R.array.exercise_week_day_labels);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (String dayLabel : dayLabels) {
            View item = inflater.inflate(R.layout.item_exercise_week_day,
                    weekDaysContainer, false);
            WeekDayView dayView = new WeekDayView(item);
            dayView.dayText.setText(dayLabel);
            weekDayViews.add(dayView);
            weekDaysContainer.addView(item);
        }
    }

    private void renderWeek() {
        if (!isUiReady() || currentWeekMonday == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (int i = 0; i < weekDayViews.size(); i++) {
            LocalDate date = currentWeekMonday.plusDays(i);
            WeekDayView dayView = weekDayViews.get(i);
            dayView.dateText.setText(String.format(Locale.getDefault(), "%d",
                    date.getDayOfMonth()));

            if (weekState == LoadState.LOADING || weekState == LoadState.IDLE) {
                dayView.statusText.setText(R.string.exercise_week_status_loading);
                dayView.statusText.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.health_text_secondary));
                dayView.itemView.setContentDescription(getString(
                        R.string.exercise_week_day_loading_description,
                        dayView.dayText.getText(), date.getDayOfMonth()));
            } else if (weekState == LoadState.ERROR) {
                dayView.statusText.setText(R.string.exercise_week_status_error);
                dayView.statusText.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.health_accent_red));
                dayView.itemView.setContentDescription(getString(
                        R.string.exercise_week_day_error_description,
                        dayView.dayText.getText(), date.getDayOfMonth()));
            } else if (date.isAfter(today)) {
                dayView.statusText.setText(R.string.exercise_week_status_future);
                dayView.statusText.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.health_grey_light));
                dayView.itemView.setContentDescription(getString(
                        R.string.exercise_week_day_future_description,
                        dayView.dayText.getText(), date.getDayOfMonth()));
            } else if (completedWeekDates.contains(DateTimeUtils.formatDate(date))) {
                dayView.statusText.setText(R.string.exercise_week_status_complete);
                dayView.statusText.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.health_accent_green));
                dayView.itemView.setContentDescription(getString(
                        R.string.exercise_week_day_complete_description,
                        dayView.dayText.getText(), date.getDayOfMonth()));
            } else {
                dayView.statusText.setText(R.string.exercise_week_status_incomplete);
                dayView.statusText.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.health_text_primary));
                dayView.itemView.setContentDescription(getString(
                        R.string.exercise_week_day_incomplete_description,
                        dayView.dayText.getText(), date.getDayOfMonth()));
            }
        }

        if (weekState == LoadState.LOADING || weekState == LoadState.IDLE) {
            weekSummaryText.setText(R.string.exercise_week_loading);
            weekStateText.setText(R.string.exercise_week_records_note);
        } else if (weekState == LoadState.ERROR) {
            weekSummaryText.setText(R.string.exercise_value_unavailable);
            weekStateText.setText(R.string.exercise_week_read_error);
        } else {
            weekSummaryText.setText(getString(R.string.exercise_week_completed_days,
                    completedWeekDays));
            weekStateText.setText(R.string.exercise_week_records_note);
        }
        weekStateText.setVisibility(View.VISIBLE);
        renderWeekGoal();
    }

    private void renderWeekGoal() {
        if (!isUiReady()) {
            return;
        }
        if (profileState == LoadState.LOADING || profileState == LoadState.IDLE) {
            weekGoalText.setText(R.string.exercise_week_goal_loading);
        } else if (profileState == LoadState.ERROR) {
            weekGoalText.setText(R.string.exercise_week_goal_error);
        } else if (weeklyGoalDays == null) {
            weekGoalText.setText(R.string.exercise_week_goal_unset);
        } else if (weekState == LoadState.SUCCESS) {
            weekGoalText.setText(getString(R.string.exercise_week_goal_progress_v2,
                    completedWeekDays, weeklyGoalDays));
        } else {
            weekGoalText.setText(getString(R.string.exercise_week_goal_only,
                    weeklyGoalDays));
        }
    }

    private void renderRecords() {
        if (!isUiReady()) {
            return;
        }
        recordsContainer.removeAllViews();
        if (recordsState == LoadState.LOADING || recordsState == LoadState.IDLE) {
            recordsStateText.setText(R.string.exercise_records_loading);
            recordsStateText.setVisibility(View.VISIBLE);
            return;
        }
        if (recordsState == LoadState.ERROR) {
            recordsStateText.setText(R.string.exercise_records_read_error);
            recordsStateText.setVisibility(View.VISIBLE);
            return;
        }
        if (selectedDateRecords.isEmpty()) {
            recordsStateText.setText(R.string.exercise_records_empty);
            recordsStateText.setVisibility(View.VISIBLE);
            return;
        }

        recordsStateText.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (ExerciseRecord record : selectedDateRecords) {
            View item = inflater.inflate(R.layout.item_exercise_record,
                    recordsContainer, false);
            TextView nameView = item.findViewById(R.id.text_exercise_record_name);
            TextView detailsView = item.findViewById(R.id.text_exercise_record_details);
            MaterialButton deleteButton = item.findViewById(R.id.button_exercise_record_delete);

            String name = exerciseNames.get(record.exerciseId);
            if (name == null || name.trim().isEmpty()) {
                name = getString(R.string.exercise_unknown_name, record.exerciseId);
            }
            nameView.setText(name);
            if (!Double.isFinite(record.estimatedCaloriesKcal)
                    || record.estimatedCaloriesKcal < 0) {
                detailsView.setText(getString(R.string.exercise_record_details_unavailable,
                        record.durationMinutes));
            } else {
                detailsView.setText(getString(R.string.exercise_record_details,
                        record.durationMinutes, formatKcal(record.estimatedCaloriesKcal)));
            }
            String exerciseName = name;
            deleteButton.setOnClickListener(v -> confirmDelete(record, exerciseName));
            recordsContainer.addView(item);
        }
    }

    private void confirmDelete(ExerciseRecord record, String exerciseName) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.exercise_delete_title)
                .setMessage(getString(R.string.exercise_delete_message, exerciseName))
                .setPositiveButton(R.string.exercise_delete_confirm, (dialog, which) ->
                        repository.deleteExerciseRecord(record,
                                new RepositoryCallback<Integer>() {
                                    @Override
                                    public void onSuccess(Integer result) {
                                        if (!isUiReady()) {
                                            return;
                                        }
                                        toast(R.string.exercise_deleted);
                                        refreshAfterRecordChange();
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        if (isUiReady()) {
                                            toast(R.string.exercise_delete_error);
                                        }
                                    }
                                }))
                .setNegativeButton(R.string.exercise_cancel, null)
                .show();
    }

    private void showRecordDatePicker() {
        LocalDate current = parseDateOrToday(selectedRecordDate);
        DatePickerDialog picker = new DatePickerDialog(requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    selectedRecordDate = DateTimeUtils.formatDate(
                            LocalDate.of(year, month + 1, dayOfMonth));
                    updateRecordsDateButton();
                    loadSelectedDateRecords();
                }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth());
        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private void updateRecordsDateButton() {
        if (DateTimeUtils.today().equals(selectedRecordDate)) {
            recordsDateButton.setText(getString(R.string.exercise_date_today,
                    selectedRecordDate));
        } else {
            recordsDateButton.setText(selectedRecordDate);
        }
    }

    private void showAddDialog() {
        if (!isUiReady()) {
            return;
        }
        AddExerciseDialogController controller = new AddExerciseDialogController();
        activeAddDialog = controller;
        controller.show();
    }

    private final class AddExerciseDialogController {
        private final View dialogView;
        private final EditText searchInput;
        private final EditText minutesInput;
        private final TextView searchStateText;
        private final TextView selectedExerciseText;
        private final TextView estimateText;
        private final TextView dialogErrorText;
        private final LinearLayout searchResultsContainer;
        private final MaterialButton clearSearchButton;
        private final MaterialButton dateButton;
        private AlertDialog dialog;
        private Button saveButton;
        private Exercise selectedExercise;
        private String recordDate = DateTimeUtils.today();
        private boolean suppressSearchChange;
        private boolean saving;

        AddExerciseDialogController() {
            dialogView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_exercise_add,
                            (ViewGroup) requireView(), false);
            searchInput = dialogView.findViewById(R.id.input_exercise_search);
            minutesInput = dialogView.findViewById(R.id.input_exercise_minutes);
            searchStateText = dialogView.findViewById(R.id.text_exercise_search_state);
            selectedExerciseText = dialogView.findViewById(
                    R.id.text_exercise_selected_action);
            estimateText = dialogView.findViewById(R.id.text_exercise_estimate);
            dialogErrorText = dialogView.findViewById(R.id.text_exercise_dialog_error);
            searchResultsContainer = dialogView.findViewById(
                    R.id.container_exercise_search_results);
            clearSearchButton = dialogView.findViewById(R.id.button_exercise_clear_search);
            dateButton = dialogView.findViewById(R.id.button_exercise_dialog_date);
        }

        void show() {
            updateDateButton();
            searchInput.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    if (suppressSearchChange) {
                        return;
                    }
                    if (selectedExercise != null
                            && !selectedExercise.name.equalsIgnoreCase(
                            editable.toString().trim())) {
                        selectedExercise = null;
                        renderSelectedExercise();
                    }
                    renderSearchResults();
                    updateEstimate();
                    clearDialogError();
                }
            });
            minutesInput.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable editable) {
                    updateEstimate();
                    clearDialogError();
                }
            });
            clearSearchButton.setOnClickListener(v -> {
                selectedExercise = null;
                searchInput.setText("");
                renderSelectedExercise();
                searchInput.requestFocus();
            });
            dateButton.setOnClickListener(v -> showDialogDatePicker());

            dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.exercise_add_dialog_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.exercise_save, null)
                    .setNegativeButton(R.string.exercise_cancel, null)
                    .create();
            dialog.setOnShowListener(ignored -> {
                saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                saveButton.setOnClickListener(v -> validateAndSave());
                renderSearchResults();
                renderSelectedExercise();
                updateEstimate();
            });
            dialog.setOnDismissListener(ignored -> {
                if (activeAddDialog == this) {
                    activeAddDialog = null;
                }
            });
            dialog.show();
        }

        void renderSearchResults() {
            if (dialog == null || !dialog.isShowing()) {
                return;
            }
            searchResultsContainer.removeAllViews();
            if (libraryState == LoadState.LOADING || libraryState == LoadState.IDLE) {
                searchStateText.setText(R.string.exercise_search_loading);
                searchStateText.setVisibility(View.VISIBLE);
                return;
            }
            if (libraryState == LoadState.ERROR) {
                searchStateText.setText(R.string.exercise_search_load_error);
                searchStateText.setVisibility(View.VISIBLE);
                return;
            }

            List<Exercise> filtered = ExerciseSearchFilter.filter(allExercises,
                    searchInput.getText() == null ? "" : searchInput.getText().toString());
            if (filtered.isEmpty()) {
                searchStateText.setText(allExercises.isEmpty()
                        ? R.string.exercise_library_empty
                        : R.string.exercise_search_empty);
                searchStateText.setVisibility(View.VISIBLE);
                return;
            }

            searchStateText.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (Exercise exercise : filtered) {
                View item = inflater.inflate(R.layout.item_exercise_search_result,
                        searchResultsContainer, false);
                TextView nameView = item.findViewById(R.id.text_exercise_search_result_name);
                TextView detailView = item.findViewById(
                        R.id.text_exercise_search_result_details);
                nameView.setText(exercise.name);
                detailView.setText(getString(R.string.exercise_search_result_details,
                        exercise.category, formatMet(exercise.metValue)));
                item.setOnClickListener(v -> selectExercise(exercise));
                searchResultsContainer.addView(item);
            }
        }

        void updateEstimate() {
            if (dialog == null || !dialog.isShowing()) {
                return;
            }
            if (profileState == LoadState.LOADING || profileState == LoadState.IDLE) {
                estimateText.setText(R.string.exercise_weight_loading);
                return;
            }
            if (profileState == LoadState.ERROR) {
                estimateText.setText(R.string.exercise_weight_read_error);
                return;
            }
            if (currentWeightKg == null || currentWeightKg <= 0) {
                estimateText.setText(R.string.exercise_need_weight);
                return;
            }
            if (selectedExercise == null) {
                estimateText.setText(R.string.exercise_choose_action_for_estimate);
                return;
            }
            Integer minutes = parseMinutes(minutesInput.getText() == null
                    ? null : minutesInput.getText().toString());
            if (minutes == null) {
                estimateText.setText(R.string.exercise_enter_duration_for_estimate);
                return;
            }
            double kcal = HealthCalculations.estimateExerciseCalories(
                    selectedExercise.metValue, currentWeightKg, minutes);
            estimateText.setText(getString(R.string.exercise_estimate_value,
                    formatKcal(kcal)));
        }

        private void selectExercise(Exercise exercise) {
            selectedExercise = exercise;
            suppressSearchChange = true;
            searchInput.setText(exercise.name);
            searchInput.setSelection(searchInput.length());
            suppressSearchChange = false;
            renderSelectedExercise();
            renderSearchResults();
            updateEstimate();
            clearDialogError();
        }

        private void renderSelectedExercise() {
            if (selectedExercise == null) {
                selectedExerciseText.setText(R.string.exercise_selected_none);
            } else {
                selectedExerciseText.setText(getString(R.string.exercise_selected_action,
                        selectedExercise.name, formatMet(selectedExercise.metValue)));
            }
        }

        private void showDialogDatePicker() {
            LocalDate current = parseDateOrToday(recordDate);
            DatePickerDialog picker = new DatePickerDialog(requireContext(),
                    (datePicker, year, month, dayOfMonth) -> {
                        recordDate = DateTimeUtils.formatDate(
                                LocalDate.of(year, month + 1, dayOfMonth));
                        updateDateButton();
                        clearDialogError();
                    }, current.getYear(), current.getMonthValue() - 1,
                    current.getDayOfMonth());
            picker.getDatePicker().setMaxDate(System.currentTimeMillis());
            picker.show();
        }

        private void updateDateButton() {
            if (DateTimeUtils.today().equals(recordDate)) {
                dateButton.setText(getString(R.string.exercise_date_today, recordDate));
            } else {
                dateButton.setText(recordDate);
            }
        }

        private void validateAndSave() {
            clearDialogError();
            if (saving) {
                return;
            }
            if (libraryState == LoadState.LOADING || libraryState == LoadState.IDLE) {
                showDialogError(R.string.exercise_search_loading);
                return;
            }
            if (libraryState == LoadState.ERROR) {
                showDialogError(R.string.exercise_search_load_error);
                return;
            }
            if (selectedExercise == null) {
                showDialogError(R.string.exercise_error_action_required);
                return;
            }
            if (profileState == LoadState.LOADING || profileState == LoadState.IDLE) {
                showDialogError(R.string.exercise_weight_loading);
                return;
            }
            if (profileState == LoadState.ERROR) {
                showDialogError(R.string.exercise_weight_read_error);
                return;
            }
            if (currentWeightKg == null || currentWeightKg <= 0) {
                showDialogError(R.string.exercise_need_weight);
                return;
            }
            Integer minutes = parseMinutes(minutesInput.getText() == null
                    ? null : minutesInput.getText().toString());
            if (minutes == null) {
                minutesInput.setError(getString(R.string.exercise_error_minutes));
                minutesInput.requestFocus();
                return;
            }
            if (!isValidPastOrToday(recordDate)) {
                showDialogError(R.string.exercise_error_date);
                return;
            }

            ExerciseRecord record = new ExerciseRecord();
            record.exerciseId = selectedExercise.id;
            record.recordDate = recordDate;
            record.durationMinutes = minutes;
            record.estimatedCaloriesKcal = HealthCalculations.estimateExerciseCalories(
                    selectedExercise.metValue, currentWeightKg, minutes);

            saving = true;
            saveButton.setEnabled(false);
            repository.addExerciseRecord(record, new RepositoryCallback<Long>() {
                @Override
                public void onSuccess(Long rowId) {
                    if (!isUiReady()) {
                        return;
                    }
                    saving = false;
                    CatInteractionHub.notifyExerciseLogged(record.estimatedCaloriesKcal);
                    toast(R.string.exercise_saved);
                    dialog.dismiss();
                    selectedRecordDate = record.recordDate;
                    updateRecordsDateButton();
                    refreshAfterRecordChange();
                }

                @Override
                public void onError(Throwable error) {
                    saving = false;
                    if (dialog != null && dialog.isShowing()) {
                        saveButton.setEnabled(true);
                        showDialogError(R.string.exercise_save_error);
                    }
                }
            });
        }

        private void showDialogError(int messageRes) {
            dialogErrorText.setText(messageRes);
            dialogErrorText.setVisibility(View.VISIBLE);
        }

        private void clearDialogError() {
            dialogErrorText.setVisibility(View.GONE);
        }
    }

    private void updateActiveDialogLibrary() {
        if (activeAddDialog != null) {
            activeAddDialog.renderSearchResults();
        }
    }

    private void updateActiveDialogEstimate() {
        if (activeAddDialog != null) {
            activeAddDialog.updateEstimate();
        }
    }

    private void refreshAfterRecordChange() {
        loadTodaySummary();
        loadWeekRecords();
        loadSelectedDateRecords();
    }

    @Nullable
    static Integer parseMinutes(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= MIN_MINUTES && value <= MAX_MINUTES ? value : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private static boolean isValidPastOrToday(@Nullable String rawDate) {
        if (rawDate == null) {
            return false;
        }
        try {
            return !DateTimeUtils.parseDate(rawDate).isAfter(LocalDate.now());
        } catch (DateTimeParseException error) {
            return false;
        }
    }

    private static LocalDate parseDateOrToday(String rawDate) {
        try {
            return DateTimeUtils.parseDate(rawDate);
        } catch (DateTimeParseException error) {
            return LocalDate.now();
        }
    }

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    private static String formatMet(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private boolean isUiReady() {
        return isAdded() && getView() != null;
    }

    private void toast(int messageRes) {
        if (isUiReady()) {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence text, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence text, int start, int before, int count) {
        }
    }

    private static final class WeekDayView {
        final View itemView;
        final TextView statusText;
        final TextView dayText;
        final TextView dateText;

        WeekDayView(View itemView) {
            this.itemView = itemView;
            statusText = itemView.findViewById(R.id.text_exercise_week_day_status);
            dayText = itemView.findViewById(R.id.text_exercise_week_day_label);
            dateText = itemView.findViewById(R.id.text_exercise_week_day_date);
        }
    }
}
