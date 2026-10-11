> ⚠️ **本文档已过时（v1.2 起）**：五人分工、睡眠模块移除、量尺数据接口等新契约以 `docs/GUIDELINES.md` 为唯一权威来源。本文档保留仅供参考，不得作为开发依据。

# HealthApp 开发规范

## 1. 技术栈

- 应用代码：Java 11；UI：XML + Android Views。
- 页面：一个 `MainActivity` + AndroidX `Fragment`。
- 组件：Material Components、AppCompat、RecyclerView。
- 数据：Room 2.8.5，单例数据库 `health_app.db`。
- 异步：`DatabaseExecutor` + 主线程 `RepositoryCallback` 回调。
- 工程：单 `app` module；Gradle Kotlin DSL 仅用于构建脚本，不代表业务代码使用 Kotlin。
- SDK：`minSdk 26`、`compileSdk 37`、`targetSdk 37`。

## 2. 核心目录

```text
HealthApp/
├─ app/src/main/
│  ├─ java/com/example/healthapp/
│  │  ├─ MainActivity.java
│  │  ├─ common/       # 日期、计算、异步回调
│  │  ├─ model/        # 公共 Entity 和统计模型
│  │  ├─ database/     # AppDatabase、DAO、Repository、Executor
│  │  ├─ profile/      # 成员 1
│  │  ├─ exercise/     # 成员 2
│  │  ├─ diet/         # 成员 3
│  │  ├─ sleep/        # 成员 4
│  │  ├─ home/         # 成员 5
│  │  └─ extra/        # 成员 6
│  ├─ res/layout/      # activity_main 与各模块 XML
│  ├─ res/menu/        # 五项底部导航
│  └─ AndroidManifest.xml
├─ docs/
└─ AGENTS.md
```

真实 namespace/applicationId 均为 `com.example.healthapp`。

## 3. 统一数据结构

所有 Entity 位于 `model/`。日期字符串必须为可排序的 `yyyy-MM-dd`；完整时刻用 `long` epoch milliseconds。

### `UserProfile` → `user_profile`

固定主键 `id=1`。字段：`heightCm`(cm)、`weightKg`(kg)、`age`、`gender`、`weeklyExerciseGoalCount`、`dailyCalorieGoalKcal`(kcal)、`dailySleepGoalMinutes`(分钟)。

### `Exercise` → `exercises`

`id` 自增；`name` 唯一；`category`；`metValue`；可空 `demoResourceName`。动作库资料与实际记录分离。

### `ExerciseRecord` → `exercise_records`

`id` 自增；`exerciseId` 外键；`recordDate`；`durationMinutes`；`estimatedCaloriesKcal`。它只代表已发生的运动。

### `ExercisePlan` → `exercise_plans`

`id`；`exerciseId`；`dayOfWeek`（ISO 1=周一，7=周日）；`targetDurationMinutes`；`active`。它表示周期计划，不表示实际完成。

### `ExerciseCheckIn` → `exercise_check_ins`

`id`；`exercisePlanId`；`checkInDate`；`completed`。同一计划和日期唯一；删除计划会级联删除相应打卡。

### `Food` → `foods`

`id`；唯一 `name`；`caloriesPer100g`、`proteinPer100g`、`carbsPer100g`、`fatPer100g`；可空 `imageResourceName`。

### `FoodRecord` → `food_records`

`id`；`foodId`；`recordDate`；`mealType`；`weightGrams`；本次实际 `caloriesKcal`、`proteinGrams`、`carbsGrams`、`fatGrams`。允许的餐次常量定义在 `FoodRecord`。

实际营养值作为历史快照保存，避免食物库以后修正时改变旧记录。

### `SleepRecord` → `sleep_records`

`id`；`sleepStartEpochMillis`；`wakeTimeEpochMillis`；`durationMinutes`；`sleepDate`。`sleepDate` 统一取起床时的本地日期。

### `DailyHealthSummary`

不是数据库表。字段为 `date`、`exerciseCount`、`calorieIntakeKcal` 和可空 `sleepDurationMinutes`；`null` 睡眠代表无记录。

## 4. 数据库接口

### 4.1 DAO

DAO 都是同步方法，禁止从 UI 线程直接调用。

| DAO | 已提供方法 |
|---|---|
| `UserProfileDao` | `getProfile()`、`save(UserProfile)`、`update(UserProfile)` |
| `ExerciseDao` | `getAll()`、`searchByName(String)`、`getById(long)`、`insert(Exercise)`、`insertAll(List<Exercise>)`、`update(Exercise)`、`delete(Exercise)` |
| `ExerciseRecordDao` | `getByDate(String)`、`getByDateRange(String,String)`、`countByDate(String)`、`insert`、`update`、`delete` |
| `ExercisePlanDao` | `getActivePlans()`、`getActivePlansForDay(int)`、`insert`、`update`、`delete` |
| `ExerciseCheckInDao` | `getByDate(String)`、`getByDateRange(String,String)`、`save`、`update`、`delete` |
| `FoodDao` | `getAll()`、`searchByName(String)`、`getById(long)`、`insert`、`insertAll`、`update`、`delete` |
| `FoodRecordDao` | `getByDate(String)`、`getByDateRange(String,String)`、`getTotalCaloriesByDate(String)`、`insert`、`update`、`delete` |
| `SleepRecordDao` | `getLatest()`、`getLatestBySleepDate(String)`、`getByDateRange(String,String)`、`insert`、`update`、`delete` |

`AppDatabase.getInstance(Context)` 是全 App 唯一入口。禁止自己调用 `Room.databaseBuilder` 创建第二个实例。

### 4.2 异步公共接口 `HealthRepository`

所有回调都会回到 Android 主线程；查询失败进入 `onError(Throwable)`。

| 方法 | 输入 | 成功返回/空数据 |
|---|---|---|
| `getUserProfile` | callback | `UserProfile`；未保存为 `null` |
| `saveUserProfile` | profile, callback | `Long` 行 ID；强制保存为 ID=1 |
| `getCurrentWeightKg` | callback | `Double` kg；无资料为 `null` |
| `getDailyCalorieGoalKcal` | callback | `Double` kcal；无资料为 `null` |
| `getDailySleepGoalMinutes` | callback | `Integer` 分钟；无资料为 `null` |
| `getExerciseCountForDate` | `yyyy-MM-dd`, callback | `Integer`；无记录为 0 |
| `getTotalCaloriesForDate` | `yyyy-MM-dd`, callback | `Double`；无记录为 0.0 |
| `getLatestSleepRecord` | callback | `SleepRecord`；无记录为 `null` |
| `getDailySummaries` | 起止日期, callback | 按日期升序的 `List<DailyHealthSummary>` |
| `clearAllData` | callback | 成功为 `true`；必须先由 UI 取得用户确认 |

使用示例：

```java
HealthRepository.getInstance(requireContext())
        .getDailyCalorieGoalKcal(new RepositoryCallback<Double>() {
            @Override
            public void onSuccess(Double goalKcal) {
                // null 表示用户尚未保存资料。
            }

            @Override
            public void onError(Throwable error) {
                // 显示读取失败状态，不伪造数据。
            }
        });
```

## 5. 跨模块取数

- 首页今日运动：`getExerciseCountForDate(DateTimeUtils.today(), callback)`。
- 首页今日摄入：`getTotalCaloriesForDate(DateTimeUtils.today(), callback)`。
- 首页最近睡眠：`getLatestSleepRecord(callback)`，返回 `null` 时显示“尚未记录”。
- 运动估算：`getCurrentWeightKg(callback)` 后调用 `HealthCalculations.estimateExerciseCalories(...)`。
- 饮食目标：`getDailyCalorieGoalKcal(callback)`。
- 睡眠目标：`getDailySleepGoalMinutes(callback)`。
- 周趋势/规则建议：`getDailySummaries(startDate, endDate, callback)`。

页面之间不得互相引用 Fragment。跨模块只引用 `common`、`model`、`database`。

## 6. 命名规范

- Java 类：`UpperCamelCase`；方法/变量：`lowerCamelCase`；常量：`UPPER_SNAKE_CASE`。
- Room 表/列：`lower_snake_case`；Java 字段保持 `lowerCamelCase` 并用 `@ColumnInfo` 映射。
- Layout：`activity_`、`fragment_`、`item_`、`dialog_` + 模块名。
- View ID：`控件_模块_用途`，如 `text_home_sleep_value`。
- string/color/drawable/menu：小写下划线。禁止新增含空格、中文或成员姓名的资源名。

## 7. 公共文件修改规则

下列内容原则上只由成员 1 修改：`MainActivity.java`、`common/`、`model/`、`database/`、`activity_main.xml`、`bottom_navigation_menu.xml`、全局 values、Manifest、Gradle、docs 和 `AGENTS.md`。

如业务需要新增字段或方法，先在 PR 中说明：使用场景、字段单位、空值规则、SQL、数据库版本/迁移、受影响调用方。不要直接改名或删除已有公共接口。数据库发布后禁止用破坏性迁移代替正式迁移。

## 8. 导航规范

`MainActivity` 只持有 `BottomNavigationView`，按菜单 ID 替换 `fragment_container`。五个主页面是 `HomeFragment`、`ExerciseFragment`、`DietFragment`、`SleepFragment`、`ProfileFragment`。`ExtraFragment` 已预留，但不占用五项底部导航；成员 6 与成员 1 商定入口后再接入。

业务逻辑、表单和 RecyclerView 不得写入 `MainActivity`。Fragment 使用本模块 XML；屏幕旋转时底部选中项由 `STATE_SELECTED_ITEM` 恢复。

## 9. 数据库操作规范

模块自己的 CRUD 可以直接使用 DAO，但必须放在后台执行器：

```java
DatabaseExecutor.execute(() -> {
    long id = AppDatabase.getInstance(context).exerciseRecordDao().insert(record);
    activity.runOnUiThread(() -> {
        // 更新页面前检查 Fragment/View 仍然有效。
    });
});
```

- 禁止 `allowMainThreadQueries()`。
- 插入前完成必填、正数、日期、餐次和外键校验。
- 更新/删除按 Entity 主键操作；不要用“先删全表再重建”代替更新。
- 跨模块读数使用 `HealthRepository`，不要直接访问另一模块的 Fragment/Adapter。
- 数据库只允许 `AppDatabase.getInstance(context)` 单例。

## 10. 日期、单位和空数据

- 只有日期：`yyyy-MM-dd`；用 `DateTimeUtils.formatDate/parseDate/today`。
- 包含时间：epoch milliseconds；显示时再按设备本地时区格式化。
- 睡眠：起床时刻必须大于入睡时刻；用 `calculateDurationMinutes`；归属起床日。
- 身高 cm；体重 kg；运动/睡眠分钟；食物 g；能量 kcal；宏量营养素 g。
- 计数和求和无记录返回 0/0.0；可选单条记录与可选目标返回 `null`；列表返回空列表。
- UI 必须分别展示“无记录”和“读取失败”，禁止用写死模拟值代替。

## 11. 新增功能步骤

1. 阅读 `PRD.md`、`TEAM.md` 和本文件，确认模块归属。
2. 执行 `git status`，从最新 `main` 创建对应 feature 分支。
3. 优先在自己的包和 XML 前缀内实现。
4. 复用现有 Entity/DAO/Repository/工具；公共接口不足时先联系成员 1。
5. 输入校验和数据库工作完成后再更新 UI。
6. 添加单元测试；涉及 Room/页面时添加 instrumentation 测试。
7. 运行 `testDebugUnitTest` 和 `assembleDebug`，再提交 Pull Request。

## 12. Android Studio 检查方法

1. 使用 **Sync Project with Gradle Files**，确认无同步错误。
2. 在 **Build > Make Project** 查看 Java/XML/Room 编译错误。
3. 运行 `app` 到 API 26+ 模拟器；逐项点击五个底部导航。
4. 检查 Logcat 是否有 `FATAL EXCEPTION` 或 Room 主线程异常。
5. 在 Terminal 运行 `./gradlew testDebugUnitTest assembleDebug`（Windows 可用 `gradlew.bat`）。
6. 修改数据库后重点检查 Room 编译 SQL、外键、索引、版本和迁移。

当前框架在本机已执行并通过 `testDebugUnitTest` 与 `assembleDebug`；模拟器交互仍需在 Android Studio 手动验证。