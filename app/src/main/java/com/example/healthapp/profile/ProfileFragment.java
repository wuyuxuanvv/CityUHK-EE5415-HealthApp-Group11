package com.example.healthapp.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthapp.R;
import com.example.healthapp.common.RepositoryCallback;
import com.example.healthapp.database.HealthRepository;
import com.example.healthapp.model.UserProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 资料页（框架负责人，v1.3 重做）：
 * <ul>
 *   <li>默认<b>档案展示态</b>：已保存的身高/体重/年龄/性别/目标直接显示（数据来自 Room，
 *       固定主键 id=1），不会每次打开都变成空表单；未填写的项显示"未填写"。</li>
 *   <li>点基本信息卡右上角的"修改"进入<b>编辑态</b>才能改数据；"保存"写库后回到展示态，
 *       "取消"放弃改动。旋转屏幕编辑态不丢。</li>
 *   <li><b>头像</b>：编辑态点头像框从相册选择，压缩后存内部存储
 *       {@code files/profile_avatar.jpg}（不占数据库、不涉及表结构变更），展示态常显。</li>
 * </ul>
 */
public class ProfileFragment extends Fragment {

    private static final double MIN_HEIGHT_CM = 50;
    private static final double MAX_HEIGHT_CM = 250;
    private static final double MIN_WEIGHT_KG = 20;
    private static final double MAX_WEIGHT_KG = 300;
    private static final int MIN_AGE = 1;
    private static final int MAX_AGE = 120;
    private static final int MIN_WEEKLY_GOAL = 1;
    private static final int MAX_WEEKLY_GOAL = 7;
    private static final int MIN_EXERCISE_MINUTES_GOAL = 5;
    private static final int MAX_EXERCISE_MINUTES_GOAL = 600;
    private static final double MIN_CALORIE_GOAL = 500;
    private static final double MAX_CALORIE_GOAL = 10000;

    private static final String KEY_EDITING = "profile_editing";
    private static final String AVATAR_FILE = "profile_avatar.jpg";
    private static final int AVATAR_MAX_PX = 512;

    private EditText heightInput;
    private EditText weightInput;
    private EditText ageInput;
    private EditText nicknameInput;
    private EditText exerciseGoalInput;
    private EditText exerciseMinutesGoalInput;
    private EditText calorieGoalInput;
    private RadioGroup genderGroup;

    private TextView heightValue;
    private TextView weightValue;
    private TextView ageValue;
    private TextView genderValue;
    private TextView nicknameValue;
    private TextView exerciseGoalValue;
    private TextView exerciseMinutesGoalValue;
    private TextView calorieGoalValue;
    private TextView goalFlagsValue;
    private View goalFlagsContainer;
    private android.widget.CheckBox goalGeneralCheck;
    private android.widget.CheckBox goalFitnessCheck;
    private android.widget.CheckBox goalDietCheck;

    private View actionsLayout;
    private FrameLayout avatarContainer;
    private ImageView avatarImage;
    private TextView avatarEmptyText;
    private TextView avatarHint;

    /** 当前已入库的档案；null = 首次使用尚未保存。 */
    @Nullable
    private UserProfile currentProfile;
    private boolean editing;

    /** 相册选图回调：编辑态点头像框触发。 */
    private final ActivityResultLauncher<String> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onAvatarPicked);

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        loadAvatar();

        view.findViewById(R.id.button_profile_edit).setOnClickListener(v -> enterEditMode());
        view.findViewById(R.id.button_profile_cancel).setOnClickListener(v -> cancelEdit());
        view.findViewById(R.id.button_profile_save).setOnClickListener(v -> attemptSave());
        avatarContainer.setOnClickListener(v -> {
            if (editing) {
                pickAvatar.launch("image/*");
            }
        });

        if (savedInstanceState != null) {
            editing = savedInstanceState.getBoolean(KEY_EDITING, false);
        }
        loadProfile();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_EDITING, editing);
    }

    private void bindViews(@NonNull View view) {
        heightInput = view.findViewById(R.id.input_profile_height);
        weightInput = view.findViewById(R.id.input_profile_weight);
        ageInput = view.findViewById(R.id.input_profile_age);
        nicknameInput = view.findViewById(R.id.input_profile_nickname);
        exerciseGoalInput = view.findViewById(R.id.input_profile_exercise_goal);
        exerciseMinutesGoalInput = view.findViewById(R.id.input_profile_exercise_minutes_goal);
        calorieGoalInput = view.findViewById(R.id.input_profile_calorie_goal);
        genderGroup = view.findViewById(R.id.radio_profile_gender);

        heightValue = view.findViewById(R.id.value_profile_height);
        weightValue = view.findViewById(R.id.value_profile_weight);
        ageValue = view.findViewById(R.id.value_profile_age);
        genderValue = view.findViewById(R.id.value_profile_gender);
        nicknameValue = view.findViewById(R.id.value_profile_nickname);
        exerciseGoalValue = view.findViewById(R.id.value_profile_exercise_goal);
        exerciseMinutesGoalValue = view.findViewById(R.id.value_profile_exercise_minutes_goal);
        calorieGoalValue = view.findViewById(R.id.value_profile_calorie_goal);
        goalFlagsValue = view.findViewById(R.id.value_profile_goal_flags);
        goalFlagsContainer = view.findViewById(R.id.container_profile_goal_flags);
        goalGeneralCheck = view.findViewById(R.id.check_profile_goal_general);
        goalFitnessCheck = view.findViewById(R.id.check_profile_goal_fitness);
        goalDietCheck = view.findViewById(R.id.check_profile_goal_diet);

        actionsLayout = view.findViewById(R.id.layout_profile_actions);
        avatarContainer = view.findViewById(R.id.container_profile_avatar);
        avatarImage = view.findViewById(R.id.image_profile_avatar);
        avatarEmptyText = view.findViewById(R.id.text_profile_avatar_empty);
        avatarHint = view.findViewById(R.id.text_profile_avatar_hint);
    }

    // ---------- 数据加载 ----------

    private void loadProfile() {
        repository().getUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile profile) {
                if (!isViewAlive()) {
                    return;
                }
                currentProfile = profile;
                populateForm(profile);
                // 首次使用（库里没有档案）直接进入编辑态，不让用户对着一页"未填写"。
                if (currentProfile == null && !editing) {
                    editing = true;
                }
                render();
            }

            @Override
            public void onError(Throwable error) {
                if (isViewAlive()) {
                    Toast.makeText(requireContext(), R.string.profile_load_error,
                            Toast.LENGTH_SHORT).show();
                    render();
                }
            }
        });
    }

    /** 同时填充展示态（TextView）与编辑态（EditText/RadioGroup）两套视图。 */
    private void populateForm(@Nullable UserProfile profile) {
        if (profile == null) {
            heightValue.setText(R.string.profile_value_not_set);
            weightValue.setText(R.string.profile_value_not_set);
            ageValue.setText(R.string.profile_value_not_set);
            genderValue.setText(R.string.profile_value_not_set);
            nicknameValue.setText(R.string.profile_value_not_set);
            exerciseGoalValue.setText(R.string.profile_value_not_set);
            exerciseMinutesGoalValue.setText(R.string.profile_value_not_set);
            calorieGoalValue.setText(R.string.profile_value_not_set);
            goalFlagsValue.setText(R.string.profile_goals_summary_none);
            heightInput.setText("");
            weightInput.setText("");
            ageInput.setText("");
            nicknameInput.setText("");
            exerciseGoalInput.setText("");
            exerciseMinutesGoalInput.setText("");
            calorieGoalInput.setText("");
            genderGroup.clearCheck();
            goalGeneralCheck.setChecked(true);
            goalFitnessCheck.setChecked(false);
            goalDietCheck.setChecked(false);
            return;
        }
        heightValue.setText(formatNumber(profile.heightCm));
        weightValue.setText(formatNumber(profile.weightKg));
        ageValue.setText(String.valueOf(profile.age));
        genderValue.setText(profile.gender);
        nicknameValue.setText(profile.nickname.isEmpty()
                ? getString(R.string.profile_value_not_set) : profile.nickname);
        exerciseGoalValue.setText(String.valueOf(profile.weeklyExerciseGoalCount));
        exerciseMinutesGoalValue.setText(String.valueOf(profile.dailyExerciseGoalMinutes));
        calorieGoalValue.setText(formatNumber(profile.dailyCalorieGoalKcal));
        goalFlagsValue.setText(goalFlagsSummary(profile));

        heightInput.setText(formatNumber(profile.heightCm));
        weightInput.setText(formatNumber(profile.weightKg));
        ageInput.setText(String.valueOf(profile.age));
        nicknameInput.setText(profile.nickname);
        exerciseGoalInput.setText(String.valueOf(profile.weeklyExerciseGoalCount));
        exerciseMinutesGoalInput.setText(String.valueOf(profile.dailyExerciseGoalMinutes));
        calorieGoalInput.setText(formatNumber(profile.dailyCalorieGoalKcal));

        goalGeneralCheck.setChecked(profile.goalGeneralHealth);
        goalFitnessCheck.setChecked(profile.goalFitness);
        goalDietCheck.setChecked(profile.goalRegularDiet);

        if (getString(R.string.profile_gender_male).equals(profile.gender)) {
            genderGroup.check(R.id.radio_profile_gender_male);
        } else if (getString(R.string.profile_gender_female).equals(profile.gender)) {
            genderGroup.check(R.id.radio_profile_gender_female);
        } else {
            genderGroup.clearCheck();
        }
    }

    private String goalFlagsSummary(@NonNull UserProfile profile) {
        StringBuilder builder = new StringBuilder();
        if (profile.goalGeneralHealth) {
            builder.append(getString(R.string.profile_goal_general));
        }
        if (profile.goalFitness) {
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(getString(R.string.profile_goal_fitness));
        }
        if (profile.goalRegularDiet) {
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(getString(R.string.profile_goal_diet));
        }
        return builder.length() == 0
                ? getString(R.string.profile_goals_summary_none) : builder.toString();
    }

    // ---------- 展示态 / 编辑态切换 ----------

    private void enterEditMode() {
        editing = true;
        render();
    }

    private void cancelEdit() {
        editing = false;
        populateForm(currentProfile); // 放弃未保存的输入
        render();
    }

    /** 按当前状态切换两套视图：编辑态显示输入框+保存/取消，展示态显示档案值。 */
    private void render() {
        int inputVisibility = editing ? View.VISIBLE : View.GONE;
        int valueVisibility = editing ? View.GONE : View.VISIBLE;

        heightInput.setVisibility(inputVisibility);
        weightInput.setVisibility(inputVisibility);
        ageInput.setVisibility(inputVisibility);
        nicknameInput.setVisibility(inputVisibility);
        exerciseGoalInput.setVisibility(inputVisibility);
        exerciseMinutesGoalInput.setVisibility(inputVisibility);
        calorieGoalInput.setVisibility(inputVisibility);
        genderGroup.setVisibility(inputVisibility);
        goalFlagsContainer.setVisibility(inputVisibility);

        heightValue.setVisibility(valueVisibility);
        weightValue.setVisibility(valueVisibility);
        ageValue.setVisibility(valueVisibility);
        genderValue.setVisibility(valueVisibility);
        nicknameValue.setVisibility(valueVisibility);
        exerciseGoalValue.setVisibility(valueVisibility);
        exerciseMinutesGoalValue.setVisibility(valueVisibility);
        calorieGoalValue.setVisibility(valueVisibility);
        goalFlagsValue.setVisibility(valueVisibility);

        actionsLayout.setVisibility(editing ? View.VISIBLE : View.GONE);
        avatarHint.setText(editing ? R.string.profile_avatar_hint_edit
                : R.string.profile_avatar_hint_view);
    }

    // ---------- 保存 ----------

    private void attemptSave() {
        clearErrors();

        String gender = readGender();
        if (gender == null) {
            toast(R.string.profile_error_gender);
            return;
        }

        Double height = readDouble(heightInput, R.string.profile_error_height,
                MIN_HEIGHT_CM, MAX_HEIGHT_CM);
        Double weight = readDouble(weightInput, R.string.profile_error_weight,
                MIN_WEIGHT_KG, MAX_WEIGHT_KG);
        Integer age = readInt(ageInput, R.string.profile_error_age, MIN_AGE, MAX_AGE);
        Integer exerciseGoal = readInt(exerciseGoalInput, R.string.profile_error_exercise_goal_v2,
                MIN_WEEKLY_GOAL, MAX_WEEKLY_GOAL);
        Integer exerciseMinutesGoal = readInt(exerciseMinutesGoalInput,
                R.string.profile_error_exercise_minutes_goal,
                MIN_EXERCISE_MINUTES_GOAL, MAX_EXERCISE_MINUTES_GOAL);
        Double calorieGoal = readDouble(calorieGoalInput, R.string.profile_error_calorie_goal,
                MIN_CALORIE_GOAL, MAX_CALORIE_GOAL);

        if (height == null || weight == null || age == null
                || exerciseGoal == null || exerciseMinutesGoal == null || calorieGoal == null) {
            return; // The failing field already shows its error message.
        }

        UserProfile profile = new UserProfile();
        profile.id = UserProfile.SINGLE_USER_ID;
        profile.heightCm = height;
        profile.weightKg = weight;
        profile.age = age;
        profile.gender = gender;
        profile.nickname = nicknameInput.getText().toString().trim();
        profile.weeklyExerciseGoalCount = exerciseGoal;
        profile.dailyExerciseGoalMinutes = exerciseMinutesGoal;
        profile.dailyCalorieGoalKcal = calorieGoal;
        profile.goalGeneralHealth = goalGeneralCheck.isChecked();
        profile.goalFitness = goalFitnessCheck.isChecked();
        profile.goalRegularDiet = goalDietCheck.isChecked();

        repository().saveUserProfile(profile, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long rowId) {
                if (!isViewAlive()) {
                    return;
                }
                currentProfile = profile;
                editing = false;
                populateForm(profile);
                render();
                Toast.makeText(requireContext(), R.string.profile_saved,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Throwable error) {
                if (isViewAlive()) {
                    Toast.makeText(requireContext(), R.string.profile_save_error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ---------- 头像 ----------

    @NonNull
    private File avatarFile() {
        return new File(requireContext().getFilesDir(), AVATAR_FILE);
    }

    private void loadAvatar() {
        Bitmap bitmap = decodeAvatar();
        if (bitmap != null) {
            avatarImage.setImageBitmap(bitmap);
            avatarEmptyText.setVisibility(View.GONE);
        } else {
            avatarImage.setImageDrawable(null);
            avatarEmptyText.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    private Bitmap decodeAvatar() {
        File file = avatarFile();
        if (!file.exists()) {
            return null;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        return bitmap;
    }

    private void onAvatarPicked(@Nullable Uri uri) {
        if (uri == null || !isViewAlive()) {
            return;
        }
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("cannot open " + uri);
            }
            Bitmap source = BitmapFactory.decodeStream(in);
            if (source == null) {
                throw new IOException("cannot decode " + uri);
            }
            Bitmap scaled = scaleDown(source);
            try (FileOutputStream out = new FileOutputStream(avatarFile())) {
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            if (scaled != source) {
                source.recycle();
            }
            loadAvatar();
            Toast.makeText(requireContext(), R.string.profile_avatar_updated,
                    Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), R.string.profile_avatar_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private static Bitmap scaleDown(@NonNull Bitmap source) {
        int max = Math.max(source.getWidth(), source.getHeight());
        if (max <= AVATAR_MAX_PX) {
            return source;
        }
        float ratio = AVATAR_MAX_PX / (float) max;
        return Bitmap.createScaledBitmap(source,
                Math.max(1, Math.round(source.getWidth() * ratio)),
                Math.max(1, Math.round(source.getHeight() * ratio)),
                true);
    }

    // ---------- 输入校验工具 ----------

    @Nullable
    private String readGender() {
        int checkedId = genderGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_profile_gender_male) {
            return getString(R.string.profile_gender_male);
        }
        if (checkedId == R.id.radio_profile_gender_female) {
            return getString(R.string.profile_gender_female);
        }
        return null;
    }

    @Nullable
    private Double readDouble(@NonNull EditText input, int errorRes, double min, double max) {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            input.setError(getString(R.string.profile_error_empty));
            return null;
        }
        try {
            double value = Double.parseDouble(raw);
            if (value < min || value > max) {
                input.setError(getString(errorRes));
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            input.setError(getString(errorRes));
            return null;
        }
    }

    @Nullable
    private Integer readInt(@NonNull EditText input, int errorRes, int min, int max) {
        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) {
            input.setError(getString(R.string.profile_error_empty));
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) {
                input.setError(getString(errorRes));
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            input.setError(getString(errorRes));
            return null;
        }
    }

    private void clearErrors() {
        heightInput.setError(null);
        weightInput.setError(null);
        ageInput.setError(null);
        exerciseGoalInput.setError(null);
        exerciseMinutesGoalInput.setError(null);
        calorieGoalInput.setError(null);
    }

    private void toast(int messageRes) {
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
    }

    private boolean isViewAlive() {
        return isAdded() && getView() != null;
    }

    @NonNull
    private HealthRepository repository() {
        return HealthRepository.getInstance(requireContext());
    }

    @NonNull
    private static String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
