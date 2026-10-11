package com.example.healthapp.summary;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.healthapp.R;
import com.example.healthapp.common.DateTimeUtils;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.database.HealthRepository;
import com.example.healthapp.model.ExerciseRecord;
import com.example.healthapp.model.FoodRecord;
import com.example.healthapp.model.UserProfile;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 每日总结（v2.0）：今日任务完成蓝卡 + 四项数据格 + 本周运动柱状图 + 近 30 天完整记录（可删除）。
 */
public class SummaryFragment extends Fragment {

    private static final int HISTORY_DAYS = 30;
    private static final int HISTORY_CAP = 100;

    private TextView dateText;
    private TextView doneText;
    private TextView totalText;
    private TextView minutesText;
    private TextView intakeText;
    private TextView burnText;
    private TextView netText;
    private WeekBarChartView weekChart;

    private HealthRepository repository;
    private UserProfile profile;
    private int todayMinutes;
    private double todayIntake;
    private double todayBurn;
    private boolean todayHasFood;

    public SummaryFragment() {
        super(R.layout.fragment_summary);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = HealthRepository.getInstance(requireContext());

        dateText = view.findViewById(R.id.text_summary_date);
        doneText = view.findViewById(R.id.text_summary_done);
        totalText = view.findViewById(R.id.text_summary_total);
        minutesText = view.findViewById(R.id.text_summary_minutes);
        intakeText = view.findViewById(R.id.text_summary_intake);
        burnText = view.findViewById(R.id.text_summary_burn);
        netText = view.findViewById(R.id.text_summary_net);
        weekChart = view.findViewById(R.id.chart_summary_week);

        dateText.setText(new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()));
        view.findViewById(R.id.button_summary_history).setOnClickListener(v -> showHistory());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAll();
    }

    private void loadAll() {
        String today = DateTimeUtils.today();
        repository.getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile result) {
                profile = result;
                loadWeek();
                render();
            }

            @Override
            public void onError(Throwable error) {
                profile = null;
                loadWeek();
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

    private void loadWeek() {
        LocalDate monday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final float[] weekMinutes = new float[7];
        final float goal = profile != null && profile.dailyExerciseGoalMinutes > 0
                ? profile.dailyExerciseGoalMinutes : 30;
        for (int i = 0; i < 7; i++) {
            final int index = i;
            String date = DateTimeUtils.formatDate(monday.plusDays(i));
            repository.getExerciseMinutesForDate(date, new RepositoryCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    weekMinutes[index] = result == null ? 0 : result;
                    weekChart.setData(weekMinutes, goal);
                }

                @Override
                public void onError(Throwable error) {
                    weekMinutes[index] = 0;
                    weekChart.setData(weekMinutes, goal);
                }
            });
        }
        weekChart.setData(weekMinutes, goal);
    }

    // ---------- 渲染 ----------

    private void render() {
        if (!isAdded()) {
            return;
        }
        minutesText.setText(String.format(Locale.getDefault(), "%,d 分钟", todayMinutes));
        intakeText.setText(formatKcal(todayIntake) + " 千卡");
        burnText.setText(formatKcal(todayBurn) + " 千卡");
        double net = todayIntake - todayBurn;
        netText.setText(formatSigned(net) + " 千卡");

        int goalMinutes = profile != null && profile.dailyExerciseGoalMinutes > 0
                ? profile.dailyExerciseGoalMinutes : 30;
        int done = 0;
        if (todayMinutes >= goalMinutes) {
            done++;
        }
        if (todayHasFood) {
            done++;
        }
        if (profile != null && profile.dailyCalorieGoalKcal > 0
                && net <= profile.dailyCalorieGoalKcal) {
            done++;
        }
        doneText.setText(String.valueOf(done));
        totalText.setText(" / 3");
    }

    // ---------- 完整记录 ----------

    private void showHistory() {
        if (!isAdded()) {
            return;
        }
        LocalDate start = LocalDate.now().minusDays(HISTORY_DAYS - 1);
        String startDate = DateTimeUtils.formatDate(start);
        String endDate = DateTimeUtils.today();

        repository.getAllExercises(new RepositoryCallback<List<com.example.healthapp.model.Exercise>>() {
            @Override
            public void onSuccess(List<com.example.healthapp.model.Exercise> exercises) {
                Map<Long, String> names = new HashMap<>();
                for (com.example.healthapp.model.Exercise exercise : exercises) {
                    names.put(exercise.id, exercise.name);
                }
                repository.getAllFoods(new RepositoryCallback<List<com.example.healthapp.model.Food>>() {
                    @Override
                    public void onSuccess(List<com.example.healthapp.model.Food> foods) {
                        for (com.example.healthapp.model.Food food : foods) {
                            names.put(food.id, food.name);
                        }
                        loadHistoryRows(names, startDate, endDate);
                    }

                    @Override
                    public void onError(Throwable error) {
                        loadHistoryRows(names, startDate, endDate);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                loadHistoryRows(new HashMap<>(), startDate, endDate);
            }
        });
    }

    private void loadHistoryRows(Map<Long, String> names, String startDate, String endDate) {
        repository.getExerciseRecordsInRange(startDate, endDate,
                new RepositoryCallback<List<ExerciseRecord>>() {
                    @Override
                    public void onSuccess(List<ExerciseRecord> exerciseRecords) {
                        repository.getFoodRecordsInRange(startDate, endDate,
                                new RepositoryCallback<List<FoodRecord>>() {
                                    @Override
                                    public void onSuccess(List<FoodRecord> foodRecords) {
                                        showHistoryDialog(names,
                                                exerciseRecords == null
                                                        ? new ArrayList<>() : exerciseRecords,
                                                foodRecords == null
                                                        ? new ArrayList<>() : foodRecords);
                                    }

                                    @Override
                                    public void onError(Throwable error) {
                                        showHistoryDialog(names,
                                                exerciseRecords == null
                                                        ? new ArrayList<>() : exerciseRecords,
                                                new ArrayList<>());
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable error) {
                        showHistoryDialog(names, new ArrayList<>(), new ArrayList<>());
                    }
                });
    }

    private void showHistoryDialog(Map<Long, String> names,
                                   List<ExerciseRecord> exerciseRecords,
                                   List<FoodRecord> foodRecords) {
        if (!isAdded()) {
            return;
        }
        List<HistoryRow> rows = new ArrayList<>();
        for (ExerciseRecord record : exerciseRecords) {
            rows.add(HistoryRow.exercise(record,
                    names.getOrDefault(record.exerciseId, "运动")));
        }
        for (FoodRecord record : foodRecords) {
            rows.add(HistoryRow.food(record, names.getOrDefault(record.foodId, "食物")));
        }
        rows.sort((a, b) -> {
            int byDate = b.date.compareTo(a.date);
            return byDate != 0 ? byDate : Long.compare(b.id, a.id);
        });
        if (rows.size() > HISTORY_CAP) {
            rows = new ArrayList<>(rows.subList(0, HISTORY_CAP));
        }

        float density = getResources().getDisplayMetrics().density;
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * density);
        container.setPadding(pad, pad, pad, pad);

        if (rows.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.summary_history_empty);
            container.addView(empty);
        }
        for (HistoryRow row : rows) {
            LinearLayout line = new LinearLayout(requireContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = (int) (10 * density);
            line.setLayoutParams(params);

            TextView text = new TextView(requireContext());
            text.setText(row.label);
            text.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
            text.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            line.addView(text);

            TextView delete = new TextView(requireContext());
            delete.setText("✕");
            delete.setTextSize(18);
            delete.setPadding((int) (12 * density), 0, 0, 0);
            delete.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.health_accent_red));
            delete.setOnClickListener(v -> confirmDeleteRow(row));
            line.addView(delete);

            container.addView(line);
        }

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(container);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.summary_history_title)
                .setView(scrollView)
                .setPositiveButton(R.string.summary_close, null)
                .show();
    }

    private void confirmDeleteRow(HistoryRow row) {
        if (row.exerciseRecord != null) {
            repository.deleteExerciseRecord(row.exerciseRecord,
                    new RepositoryCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer result) {
                            toast(R.string.exercise_deleted);
                            loadAll();
                        }

                        @Override
                        public void onError(Throwable error) {
                            toast(R.string.exercise_save_error);
                        }
                    });
        } else if (row.foodRecord != null) {
            repository.deleteFoodRecord(row.foodRecord, new RepositoryCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    toast(R.string.diet_deleted);
                    loadAll();
                }

                @Override
                public void onError(Throwable error) {
                    toast(R.string.diet_save_error);
                }
            });
        }
    }

    // ---------- 工具 ----------

    private static String formatKcal(double value) {
        return String.format(Locale.getDefault(), "%,.0f", value);
    }

    private static String formatSigned(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + formatKcal(value);
    }

    private void toast(int messageRes) {
        if (isAdded()) {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        }
    }

    /** 历史记录行：运动或饮食二选一。 */
    private static final class HistoryRow {
        final long id;
        final String date;
        final String label;
        final ExerciseRecord exerciseRecord;
        final FoodRecord foodRecord;

        private HistoryRow(long id, String date, String label,
                           ExerciseRecord exerciseRecord, FoodRecord foodRecord) {
            this.id = id;
            this.date = date;
            this.label = label;
            this.exerciseRecord = exerciseRecord;
            this.foodRecord = foodRecord;
        }

        static HistoryRow exercise(ExerciseRecord record, String name) {
            return new HistoryRow(record.id, record.recordDate,
                    record.recordDate + "  运动 · " + name + " · " + record.durationMinutes
                            + " 分钟 · 约 " + formatKcal(record.estimatedCaloriesKcal) + " 千卡",
                    record, null);
        }

        static HistoryRow food(FoodRecord record, String name) {
            return new HistoryRow(record.id, record.recordDate,
                    record.recordDate + "  饮食 · " + name + " · " + formatKcal(record.caloriesKcal)
                            + " 千卡",
                    null, record);
        }
    }
}
