> ⚠️ **本文档已过时（v1.2 起）**：五人分工、睡眠模块移除、量尺数据接口等新契约以 `docs/GUIDELINES.md` 为唯一权威来源。本文档保留仅供参考，不得作为开发依据。

# HealthApp 成员操作指南

## 开始前所有人都做

1. 阅读 `docs/PRD.md`、`docs/TEAM.md`、`docs/DEVELOPMENT_GUIDE.md`。
2. 在 Android Studio 打开仓库根目录 `HealthApp`，等待 Gradle Sync 完成。
3. 只修改自己负责的包和 XML；公共文件先联系成员 1。
4. 运行 App 后用底部导航进入自己页面。页面当前显示“待开发”是正常的。

## 成员 1：资料、公共框架与集成

- 你负责：个人资料、本地 PIN、公共导航/数据库维护、合并、测试、APK。
- 先打开：`app/src/main/java/com/example/healthapp/profile/ProfileFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_profile.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/profile/`。
- 具体待开发：资料输入校验、首次填写、查看修改、本地 PIN；协调数据库迁移和最终集成。
- 已提供接口：`HealthRepository.getUserProfile(...)`、`saveUserProfile(...)`；`UserProfile` 固定 ID=1。
- 你需实现：资料表单、保存反馈、PIN 安全存储/验证和相关测试。
- 公共职责：你可以维护 `MainActivity`、`common/`、`model/`、`database/`、导航、主题、Gradle 和文档，但改接口前要通知所有调用者。
- 运行页面：启动 App，点击底部“资料”。
- 完成后：运行测试与 Debug 构建，检查所有成员 PR，再提交 `feature/profile` 的 Pull Request。

## 成员 2：运动

- 你负责：动作库、实际运动、MET 估算、周计划和打卡。
- 先打开：`app/src/main/java/com/example/healthapp/exercise/ExerciseFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_exercise.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/exercise/`；新 XML 使用 `exercise_`、`item_exercise_` 或 `dialog_exercise_` 前缀。
- 具体待开发：不少于 20 个动作、RecyclerView、记录 CRUD、周计划、打卡、输入校验和估算值标注。
- 已提供接口：`ExerciseDao`、`ExerciseRecordDao`、`ExercisePlanDao`、`ExerciseCheckInDao`；`getCurrentWeightKg(...)`；`HealthCalculations.estimateExerciseCalories(...)`。
- 你需实现：用 `DatabaseExecutor` 异步调用 DAO、刷新列表、生成动作初始数据和测试。计划、实际记录、打卡不能混用。
- 禁止修改：`MainActivity.java`、`AppDatabase.java`、公共 Entity/DAO/Repository、Gradle、Manifest、导航和其他模块；需要变更先联系成员 1。
- 运行页面：启动 App，点击底部“运动”。
- 完成后：运行测试与构建，提交 `feature/exercise` 的 Pull Request。

## 成员 3：饮食

- 你负责：食物库、搜索、四餐次记录、营养计算和目标比较。
- 先打开：`app/src/main/java/com/example/healthapp/diet/DietFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_diet.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/diet/`；新 XML 使用 `diet_`、`item_food_` 或 `dialog_food_` 前缀。
- 具体待开发：不少于 100 种食物、搜索、RecyclerView、克数输入、营养快照、记录 CRUD、每日总量与目标比较。
- 已提供接口：`FoodDao`、`FoodRecordDao`；`getDailyCalorieGoalKcal(...)`；`HealthCalculations.calculateNutrientAmount(...)`。
- 你需实现：用 `DatabaseExecutor` 异步调用 DAO、食物初始数据、餐次校验和测试。
- 禁止修改：公共数据库/Entity/接口、导航、Gradle、Manifest 和其他模块；需要变更先联系成员 1。
- 运行页面：启动 App，点击底部“饮食”。
- 完成后：运行测试与构建，提交 `feature/diet` 的 Pull Request。

## 成员 4：睡眠

- 你负责：入睡/起床时间、跨午夜时长、记录 CRUD 和简单评价。
- 先打开：`app/src/main/java/com/example/healthapp/sleep/SleepFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_sleep.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/sleep/`；新 XML 使用 `sleep_`、`item_sleep_` 或 `dialog_sleep_` 前缀。
- 具体待开发：时间选择、记录列表/编辑/删除、目标比较和非医疗评价。
- 已提供接口：`SleepRecordDao`；`getDailySleepGoalMinutes(...)`；`DateTimeUtils.calculateDurationMinutes(...)` 和 `dateFromEpochMillis(...)`。
- 你需实现：保存 epoch milliseconds，以起床日作为 `sleepDate`，异步 DAO 调用，以及跨午夜/异常时间测试。
- 禁止修改：公共数据库/Entity/接口、导航、Gradle、Manifest 和其他模块；需要变更先联系成员 1。
- 运行页面：启动 App，点击底部“睡眠”。
- 完成后：运行测试与构建，提交 `feature/sleep` 的 Pull Request。

## 成员 5：首页与统计

- 你负责：首页、历史、每日/每周统计和简单趋势。
- 先打开：`app/src/main/java/com/example/healthapp/home/HomeFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_home.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/home/`；新 XML 使用 `home_` 或 `item_home_` 前缀。
- 具体待开发：完善刷新/错误状态、历史页面和简单趋势；不得写死正式统计值。
- 已提供接口：`getExerciseCountForDate(...)`、`getTotalCaloriesForDate(...)`、`getLatestSleepRecord(...)`、`getDailySummaries(...)`。
- 你需实现：把异步结果绑定到列表/图表，保留 `null` 睡眠的无记录语义，补充统计展示测试。
- 禁止修改：运动/饮食/睡眠业务类、公共数据库/Entity/接口、导航、Gradle 和 Manifest；接口不足先联系成员 1。
- 运行页面：启动 App 默认就是“首页”。
- 完成后：运行测试与构建，提交 `feature/home` 的 Pull Request。

## 成员 6：扩展与测试

- 你负责：固定规则建议、本地提醒、清除数据、跨模块测试和演示检查。
- 先打开：`app/src/main/java/com/example/healthapp/extra/ExtraFragment.java`。
- 修改 XML：`app/src/main/res/layout/fragment_extra.xml`。
- 新类放入：`app/src/main/java/com/example/healthapp/extra/`；新 XML 使用 `extra_` 或 `dialog_extra_` 前缀。
- 具体待开发：可解释建议、本地通知、清除二次确认、测试数据流程和演示清单。不得接入 AI 或网络。
- 已提供接口：`getUserProfile(...)`、`getDailySummaries(...)`、`clearAllData(...)`。
- 你需实现：规则和通知调度；只在用户确认后调用清除；编写跨模块测试。
- 禁止修改：其他成员业务、公共数据库/Entity/接口、导航、Gradle 和 Manifest；需要入口或通知权限调整时联系成员 1。
- 运行页面：当前扩展页不在五项底部导航。可先使用 XML Preview 和测试验证；需要正式入口时由成员 1 统一接入，不要自行改 `MainActivity`。
- 完成后：运行测试与构建，提交 `feature/extra` 的 Pull Request。

## 公共数据库最简单用法

跨模块读取优先使用 `HealthRepository`，回调已经在主线程。模块自己的 CRUD 使用 DAO 时：

```java
DatabaseExecutor.execute(() -> {
    List<FoodRecord> records = AppDatabase.getInstance(context)
            .foodRecordDao()
            .getByDate(DateTimeUtils.today());
    activity.runOnUiThread(() -> {
        // Fragment 仍有效时再更新 RecyclerView。
    });
});
```

不要在点击事件或 `onCreateView` 中直接执行 DAO，不要新建第二个 Room 数据库。

## GitHub 协作指南

### 1. 第一次下载

```bash
git clone <GROUP11_REPOSITORY_URL>
cd group11
git status
```

若实际仓库文件夹名不是 `group11`，第二条命令替换成真实文件夹名。

### 2. 从最新 main 创建自己的分支

先确认 `git status` 没有未提交修改：

```bash
git switch main
git pull origin main
git switch -c feature/exercise
```

最后一行按成员替换为：

- 成员 1：`feature/profile`
- 成员 2：`feature/exercise`
- 成员 3：`feature/diet`
- 成员 4：`feature/sleep`
- 成员 5：`feature/home`
- 成员 6：`feature/extra`

若分支已经存在，使用 `git switch feature/exercise`，不要再次加 `-c`。

### 3. 获取 main 的最新公共代码

先保存并提交自己的工作，确保 `git status` 干净，然后：

```bash
git switch main
git pull origin main
git switch feature/exercise
git merge main
```

出现冲突时停止修改并联系成员 1。不要使用 `git reset --hard` 或 `git push --force`。

### 4. 保存并上传自己的修改

以成员 2 为例，只添加自己确认过的文件：

```bash
git status
git add app/src/main/java/com/example/healthapp/exercise
git add app/src/main/res/layout/fragment_exercise.xml
git commit -m "feat: implement exercise records"
git push -u origin feature/exercise
```

之后同一分支再上传可使用 `git push`。其他成员替换目录、XML 和分支名。提交前必须运行测试，不要直接提交到 `main`。

### 5. 创建 Pull Request

1. 打开 GitHub 的 group11 仓库。
2. 选择自己的 `feature/...` 分支，点击 **Compare & pull request**。
3. Base 选择 `main`，Compare 选择自己的分支。
4. 标题写清模块，正文列出修改文件、已完成/待完成内容和真实测试结果。
5. 创建 PR 后通知成员 1；成员 1 检查、合并并验证完整工程。

PR 合并后，再按“获取 main 的最新公共代码”同步，然后继续开发。若 `git status` 显示未提交修改，不要直接切分支；先提交自己的修改，或与成员 1 商量如何安全保存。