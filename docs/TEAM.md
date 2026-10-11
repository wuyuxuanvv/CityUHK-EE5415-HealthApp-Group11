> ⚠️ **本文档已过时（v1.2 起）**：五人分工、睡眠模块移除、量尺数据接口等新契约以 `docs/GUIDELINES.md` 为唯一权威来源。本文档保留仅供参考，不得作为开发依据。

# HealthApp 六人分工

## 共同约定

- `MainActivity`、`database/`、`model/`、`common/`、Gradle、Manifest、主题、导航菜单和文档属于公共文件，由成员 1 维护。
- 每位成员主要修改自己的 Java 包和同前缀 XML。需要改公共 Entity、DAO、`AppDatabase` 或 `HealthRepository` 时，先说明字段、数据库版本、接口和调用方影响。
- DAO 是同步方法，只能在 `DatabaseExecutor.execute(...)` 内调用；跨模块统计优先使用异步 `HealthRepository`。

## 成员 1：项目基础与个人资料

- 范围：`profile/`、`fragment_profile.xml`、统一导航、公共数据库协调、PIN、最终集成和 APK。
- Java：完善 `ProfileFragment`，在 `profile/` 新增资料表单/PIN 类；维护公共文件。
- XML：完善 `fragment_profile.xml`，新增布局必须使用 `profile_`、`fragment_profile_`、`dialog_profile_` 前缀。
- 数据库：通过 `HealthRepository.saveUserProfile(...)` 和 `getUserProfile(...)` 保存/读取固定 ID=1 的资料。
- 对外提供：体重、每日热量目标和睡眠目标，由现有 `HealthRepository` 方法提供。
- 交付：资料 CRUD、本地 PIN、导航与数据库集成、全量构建和 APK。
- 依赖：收集成员 2–6 的 PR；协调所有公共数据结构变化。

## 成员 2：运动

- 范围：`exercise/`、`fragment_exercise.xml` 及 `exercise_`/`item_exercise_`/`dialog_exercise_` XML。
- Java：完善 `ExerciseFragment`；新增 RecyclerView Adapter、表单和业务类。
- 数据库：实现 `ExerciseDao`、`ExerciseRecordDao`、`ExercisePlanDao`、`ExerciseCheckInDao` 的页面调用，调用必须放入 `DatabaseExecutor`。
- 使用接口：`HealthRepository.getCurrentWeightKg(...)`；热量使用 `HealthCalculations.estimateExerciseCalories(...)`。
- 提供给首页：保存真实 `ExerciseRecord` 后，现有 `getExerciseCountForDate(...)` 自动可用。
- 交付：不少于 20 个动作、记录 CRUD、估算标注、周计划和打卡。
- 依赖：体重来自成员 1；成员 5 依赖其真实记录。

## 成员 3：饮食

- 范围：`diet/`、`fragment_diet.xml` 及 `diet_`/`item_food_`/`dialog_food_` XML。
- Java：完善 `DietFragment`；新增搜索、Adapter、记录表单和营养计算类。
- 数据库：调用 `FoodDao`、`FoodRecordDao`；所有实际营养快照写入 `FoodRecord`。
- 使用接口：`HealthRepository.getDailyCalorieGoalKcal(...)`；营养换算使用 `HealthCalculations.calculateNutrientAmount(...)`。
- 提供给首页：保存真实 `FoodRecord` 后，现有 `getTotalCaloriesForDate(...)` 自动可用。
- 交付：不少于 100 种食物、搜索、四餐次记录 CRUD、每日总量与目标比较。
- 依赖：目标来自成员 1；成员 5 依赖其真实记录。

## 成员 4：睡眠

- 范围：`sleep/`、`fragment_sleep.xml` 及 `sleep_`/`item_sleep_`/`dialog_sleep_` XML。
- Java：完善 `SleepFragment`；新增时间输入、记录列表和评价规则类。
- 数据库：调用 `SleepRecordDao` 完成 CRUD；`sleepDate` 必须是起床本地日期。
- 使用接口：`HealthRepository.getDailySleepGoalMinutes(...)`；时长使用 `DateTimeUtils.calculateDurationMinutes(...)`。
- 提供给首页：保存真实 `SleepRecord` 后，现有 `getLatestSleepRecord()` 自动可用。
- 交付：跨午夜计算、记录 CRUD、非医疗时长评价和相关测试。
- 依赖：目标来自成员 1；成员 5 依赖其真实记录。

## 成员 5：首页与统计

- 范围：`home/`、`fragment_home.xml` 及 `home_`/`item_home_` XML。
- Java：扩展现有 `HomeFragment`，新增历史和简单趋势展示类。
- 数据库：不直接创建数据库；使用 `HealthRepository.getExerciseCountForDate(...)`、`getTotalCaloriesForDate(...)`、`getLatestSleepRecord(...)` 和 `getDailySummaries(...)`。
- 对外提供：统计页面和展示逻辑，不拥有运动/饮食/睡眠原始表。
- 交付：首页完善、历史记录、每日/每周汇总、真实空状态和趋势。
- 依赖：等待成员 2–4 写入真实记录；接口已可在空库安全工作。

## 成员 6：扩展功能与测试

- 范围：`extra/`、`fragment_extra.xml` 及 `extra_`/`dialog_extra_` XML。
- Java：完善 `ExtraFragment`；新增固定规则建议、通知调度、清除确认和跨模块测试类。
- 数据库：读取 `HealthRepository.getUserProfile(...)`、`getDailySummaries(...)`；用户确认后才调用 `clearAllData(...)`。
- 提供给其他成员：提醒入口、演示检查清单、跨模块测试结果。
- 交付：规则建议、本地通知、清除全部数据、跨模块/演示测试。
- 依赖：使用成员 1–5 已集成数据；不能自行更改其业务逻辑。

## 文件所有权速查

| 类型 | 主要维护者 | 文件/目录 |
|---|---|---|
| 公共 | 成员 1 | `MainActivity.java`、`common/`、`model/`、`database/`、`activity_main.xml`、`bottom_navigation_menu.xml`、values、Manifest、Gradle、docs、`AGENTS.md` |
| 资料 | 成员 1 | `profile/`、`fragment_profile.xml` |
| 运动 | 成员 2 | `exercise/`、`fragment_exercise.xml` |
| 饮食 | 成员 3 | `diet/`、`fragment_diet.xml` |
| 睡眠 | 成员 4 | `sleep/`、`fragment_sleep.xml` |
| 首页 | 成员 5 | `home/`、`fragment_home.xml` |
| 扩展 | 成员 6 | `extra/`、`fragment_extra.xml` |