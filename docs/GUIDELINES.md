# HealthApp 最终设计标准与组员接入指南

> **版本：v1.3 ｜ 制定：框架负责人 ｜ 适用范围：全组五人（A/B/C/D/E + 框架负责人）**
> v1.3：UI 全面切换**新粗野主义风格**（黑白主色 + 黄色高亮，粗黑边框直角卡片，设计令牌已由框架落地 `colors.xml`/`themes.xml`）；首页红黄绿量尺占位图**替换为小猫吉祥物静态图**（`cat_home`），E 的新任务改为"小猫动画"——运动让小猫动起来、进食让小猫吃东西；新增 `common/CatInteractionHub.java` 运动/进食事件总线（**这是全组唯一允许的事件通道**，仅限小猫联动使用，不引入任何第三方事件总线）。资源前缀 `gauge_home_*` 废弃，E 改用 `cat_home_*` / `home_cat_*`。
> v1.2：组内决策**移除睡眠模块**，底部导航为四项（首页/运动/饮食/资料）；UserProfile 移除睡眠目标字段；Repository 移除睡眠相关接口。
> 本文档由仓库 `docs/`（PRD.md、TEAM.md、DEVELOPMENT_GUIDE.md、MEMBER_GUIDE.md）与 `AGENTS.md` 整合修订而成，按本组五人分工（A 运动 / B 饮食 / C 数据库 / D UI 设计 / E 首页可视化）重写。
> **本文档是最高规范**：与任何人的口头约定冲突时以本文档为准；要改本文档，开 PR 经全组确认。

---

# 第一部分：设计标准规则

## 1. 产品范围

### 1.1 第一版做什么
个人健康管理单机 App：个人资料与健康目标、运动记录（含热量估算）、饮食记录（含热量与营养）、首页**小猫吉祥物互动可视化**（本版核心亮点：录入运动小猫动起来，录入饮食小猫吃东西，颜色与情绪联动热量状态）。数据全部存本机 Room 数据库，离线可用。

### 1.2 明确不做（任何成员不得私自加）
远程账号、多用户、网络同步、云数据库、AI 推荐、复杂饮食周计划、穿戴设备接入、深浅睡/心率分析、医疗诊断、复杂动画、系统级通知推送。**不得申请 `INTERNET` 权限，不得加后端/网络服务。**

## 2. 技术栈标准（不得更改）

| 项目 | 固定选型 |
|---|---|
| 语言 | Java 11（业务代码禁用 Kotlin） |
| UI | XML + Android Views（禁用 Jetpack Compose / Flutter / RN）；**允许自定义 View（E 的小猫动画/状态视图）** |
| 页面结构 | 单 `MainActivity` + AndroidX `Fragment` + `BottomNavigationView` |
| 组件 | Material Components、AppCompat、RecyclerView |
| 数据库 | Room 2.8.5，单例 `health_app.db`（`AppDatabase.getInstance()` 唯一入口） |
| 异步 | `DatabaseExecutor` 后台执行 + `RepositoryCallback` 主线程回调；禁止 `allowMainThreadQueries()` |
| SDK | `minSdk 26`、`compileSdk 37`、`targetSdk 37` |
| 工程 | 单 `app` module；Gradle Kotlin DSL 仅用于构建脚本 |
| 架构 | 禁止改成多 module、禁止引入依赖注入框架、禁止引入第三方事件总线/RxJava 等额外异步框架；**唯一允许的事件通道是 `common/CatInteractionHub`（仅限小猫联动，见 §3.5）** |

## 3. 数据标准

### 3.1 统一数据结构（表结构契约，改前必须全组通知）

| Entity → 表 | 关键字段与规则 |
|---|---|
| `UserProfile` → `user_profile` | 固定主键 `id=1`；身高 cm、体重 kg、年龄、性别、每周运动目标次数、每日热量目标 kcal |
| `Exercise` → `exercises` | 动作库；`name` 唯一；`metValue`；C 负责内置 ≥20 种 |
| `ExerciseRecord` → `exercise_records` | 只代表已发生的运动；`recordDate` + `durationMinutes` + `estimatedCaloriesKcal`（**E 的小猫状态依赖此字段**） |
| `Food` → `foods` | 食物库；`name` 唯一；每单位热量与营养（默认按每 100g 计）；C 负责内置 ≥100 种 |
| `FoodRecord` → `food_records` | **热量与营养值是保存时快照**，食物库以后修改不影响历史记录；餐次常量定义在 `FoodRecord` |
| `DailyHealthSummary` | 非数据库表，汇总用；字段：`date`、`exerciseCount`、`calorieIntakeKcal`、`exerciseCaloriesKcal`（运动消耗）；`netCaloriesKcal()` = 摄入 − 消耗 |

### 3.2 时间、单位与空值

- 只有日期：一律 `yyyy-MM-dd` 可排序字符串，用 `DateTimeUtils.formatDate/parseDate/today`，禁止手写拼接。
- 含时刻：一律 `long` epoch milliseconds，显示时按设备本地时区格式化。
- 单位：身高 cm｜体重 kg｜运动分钟｜食物 g｜能量 kcal｜营养素 g。
- 空值语义：计数/求和无记录返回 `0`/`0.0`；可选单条记录与目标返回 `null`；列表返回空列表。**UI 必须区分展示"无记录"和"读取失败"，禁止写死模拟值冒充数据。**
- 校验规则：插入前完成必填、正数、日期、餐次、外键校验。

### 3.3 线程与访问规则

- DAO 全是同步方法，只能在 `DatabaseExecutor.execute(...)` 内调用；禁止在 UI 线程直接调 DAO。
- 跨模块读数一律用 `HealthRepository`（已切线程、回调主线程），**禁止 import 其他模块的 Fragment/Adapter/自定义 View 内部类**。
- 全 App 只允许一个数据库实例；任何人不得自行 `Room.databaseBuilder`。

### 3.4 跨模块公共接口（`HealthRepository`，回调均在主线程）

| 方法 | 用途 | 空数据返回 |
|---|---|---|
| `getUserProfile` / `saveUserProfile` | 资料读写（框架负责人） | 未保存为 `null` |
| `getCurrentWeightKg` | A 估算运动消耗 | `null` |
| `getDailyCalorieGoalKcal` | E 小猫状态的目标对比 / B 对目标 | `null` |
| `getExerciseCountForDate(date)` | 统计用 | `0` |
| `getTotalCaloriesForDate(date)` | E 小猫状态：今日摄入 | `0.0` |
| `getTotalExerciseCaloriesForDate(date)` | **E 小猫状态：今日运动消耗（v1.1 新增，DAO 契约见 §3.6）** | `0.0` |
| `getDailySummaries(start, end)` | E 周趋势（含运动消耗） | 空列表 |

### 3.5 小猫状态数据契约（E 的核心依赖，v1.1 建立、v1.3 重述）

**净热量语义**：`netKcal = 今日摄入(getTotalCaloriesForDate) − 今日运动消耗(getTotalExerciseCaloriesForDate)`，与每日热量目标 `goalKcal` 对比得出小猫的状态与颜色。

- 状态档位（E 实现，全组统一语义）：`net ≤ 0.75×goal` → 绿色"状态良好，继续保持"；`0.75×goal < net ≤ goal` → 黄色"接近上限，注意控制"；`net > goal` → 红色"已超出今日目标"。
- 目标未设置（`goalKcal = null`）→ 不显示刻度百分比，展示"请先在资料页设置每日摄入目标"，**不得默认按 2000 计算**。
- 无运动/饮食记录时两个求和接口返回 `0.0`，应显示"今日还没有记录"，而不是随机状态。
- **实时性**：Fragment 返回前台时在 `onResume()` 重新查询刷新（框架统一此模式）。
- **事件联动（v1.3 新增）**：`common/CatInteractionHub.java` 是全组唯一的事件通道，仅限小猫联动——A 在 `ExerciseRecord` 入库成功后调 `CatInteractionHub.notifyExerciseLogged(kcal)`；B 在 `FoodRecord` 入库成功后调 `CatInteractionHub.notifyMealLogged(kcal)`；E 实现 `CatMascot` 接口并在首页可见/不可见时 `register/unregister`。**禁止引入任何第三方事件总线，禁止用该通道传其他业务事件。**

### 3.6 DAO 契约补充（v1.1，C 实现）

`ExerciseRecordDao` 新增方法：`double getTotalCaloriesByDate(String date)` — 汇总当日 `estimatedCaloriesKcal` 之和，无记录返回 0.0。SQL 由 C 编写，框架负责人审核。

## 4. 命名与资源规范

- Java 类 `UpperCamelCase`；方法变量 `lowerCamelCase`；常量 `UPPER_SNAKE_CASE`。
- Room 表/列 `lower_snake_case`；Java 字段 `lowerCamelCase` + `@ColumnInfo` 映射。
- Layout：`activity_` / `fragment_` / `item_` / `dialog_` + 模块前缀（各模块前缀见第二部分每人卡片）。
- View ID：`控件_模块_用途`，如 `image_home_cat`、`text_home_exercise_value`。
- string / color / drawable / menu：小写下划线；**禁止含空格、中文或成员姓名**。
- 文案全部进 `strings.xml`，禁止硬编码到布局或 Java 中（E 小猫的警示/鼓励语用 `home_cat_*` 前缀）。
- **设计令牌（v1.3 已落地）**：颜色、字体、间距、圆角一律引用 `colors.xml` / `themes.xml` / `dimens.xml` 的令牌，**禁止在布局里写死 `#FF0000` 或 16dp 这类裸值**。当前色板（新粗野主义）：主色黑 `#000000`、底色白 `#FFFFFF`、强调黄 `#FFCC00`（选中态/高亮）、警示红 `#FF0000`、辅助蓝 `#0000CC`；已落地令牌 `health_accent_red` / `health_accent_yellow` / `health_accent_blue`。红黄绿状态语义中的绿色令牌由 D 后续补充（如 `health_accent_green`），先引用既有令牌，勿写裸值。

## 5. 公共文件与设计体系归属（v1.1 重要变更）

**代码公共文件**（只有框架负责人能改）：`MainActivity.java`、`common/`、`model/`、`database/`（`AppDatabase` 注册配置、`HealthRepository` 公共方法）、`activity_main.xml`、`bottom_navigation_menu.xml`、Manifest、Gradle、docs、本文件。

**设计资源**（D 负责维护，框架负责人 Review）：`colors.xml`、`themes.xml`、`dimens.xml`（v1.1 新建）、drawable、全局 styles。规则：

1. D 出**设计令牌先行**：开工第一周 D 先交令牌（色板/字号/间距/圆角），所有人只引用令牌。
2. D 想改他人模块的布局 XML → 提 PR 给该模块负责人 Review；模块负责人实现功能、D 做视觉走查和调整。
3. **已被 Java 代码引用的资源名冻结**（改名 = 编译崩，必须 PR 公告全员）；新增资源随意。
4. 每个模块的 `fragment_*.xml` 归属不变（实现归模块负责人），但视觉风格必须遵循 D 的令牌体系。

确需修改公共代码时的流程：在 PR 中说明**使用场景、字段单位、空值规则、SQL、数据库版本/迁移方案、受影响调用方**→ 群里通知全员 → 框架负责人确认后才改。**禁止直接改名/删除已有公共接口；数据库发布后禁止用破坏性迁移代替正式迁移。**

## 6. 导航规范

- `MainActivity` 只持有 `BottomNavigationView`，按菜单 ID 替换 `fragment_container`；四个主页：`HomeFragment`（E）、`ExerciseFragment`（A）、`DietFragment`（B）、`ProfileFragment`（框架负责人）。
- 业务逻辑、表单、RecyclerView、自定义 View **不得写进 `MainActivity`**；Fragment 用本模块 XML。
- 屏幕旋转后底部选中项由 `STATE_SELECTED_ITEM` 恢复，不得写丢。
- `ExtraFragment` 已预留但不占主导航；本组暂不安排人开发，需要启用时与框架负责人商定入口，**任何人不得自行改 `MainActivity` 加入口**。

## 7. 质量红线（PR 一律打回）

1. 提交后 `testDebugUnitTest` 或 `assembleDebug` 不过。
2. 写死假数据/假统计（**包括小猫状态/情绪**），或无数据与读取失败不区分。
3. UI 线程调 DAO，或新建第二个数据库实例。
4. import 其他模块的 Fragment/Adapter，或改了公共契约未通知。
5. 改了别人负责的文件（ merge 冲突除外，但冲突要交框架负责人处理）。
6. 页面崩溃、切导航状态丢失、Logcat 出现 `FATAL EXCEPTION` 或 Room 主线程异常；自定义 View 在 `onDraw` 里分配对象导致卡顿。
7. 使用 Kotlin/Compose/网络/INTERNET 权限/事件总线等 §1.2、§2 明令禁止的技术。
8. 布局里写死颜色/尺寸裸值，不用 D 的设计令牌（v1.1 新增）。

---

# 第二部分：组员接入指南

## 8. 分工总表（五人 + 待确认项）

| 角色 | 分支名 | 负责范围 | XML/资源前缀 |
|---|---|---|---|
| **框架负责人**（你） | `feature/profile` | 工程配置、导航、公共数据层契约、`HealthRepository`、小猫状态数据接口与 `CatInteractionHub`、资料页 `profile/`、集成合并、最终 APK | `profile_` |
| **A · 运动模块** | `feature/exercise` | `exercise/` 包：动作列表、运动记录、热量估算、周计划打卡 | `exercise_` / `item_exercise_` / `dialog_exercise_` |
| **B · 饮食模块** | `feature/diet` | `diet/` 包：食物搜索、四餐次记录、克数输入、当日汇总 | `diet_` / `item_food_` / `dialog_food_` |
| **C · 数据库** | `feature/database` | `model/`+`database/` 内 Entity、DAO、**种子数据**（≥20 运动、≥100 食物及单位热量）、用户记录表 | DAO/SQL 无 XML |
| **D · UI 设计** | `feature/ui` | 设计令牌（colors/themes/dimens/drawable）、全局样式、各模块视觉走查 | 令牌文件，无前缀限制 |
| **E · 首页可视化** | `feature/home` | `home/` 包：**小猫吉祥物动画与状态可视化**、今日概览、周趋势 | `home_` / `cat_home_` / `item_home_` |

> C 与 A/B 的协作：C 建表+种子数据先行交付，A/B 只写页面和业务调用，不建表。

## 9. 五分钟环境准备（所有人）

1. 安装 Android Studio（最新稳定版）+ Git（git-scm.com）。
2. 让仓库 owner 把你加为协作者（`Settings → Collaborators`），否则克隆报 404。
3. Android Studio 启动页 **Get from VCS** → 粘贴仓库地址 → Clone（用自己的 GitHub 账号登录）。
4. 打开后 **Trust Project**，等右下角 Gradle Sync 跑完；提示装 SDK 就点 Install。
5. 按 `Alt+F12` 打开 Terminal，执行：

```bash
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的邮箱"
git switch main && git pull origin main
git switch -c feature/你的分支名        # 分支名见上表；已建过就用 git switch feature/xxx
git push -u origin feature/你的分支名
```

6. 点 ▶️ Run 跑起来，能看到"待开发"占位页 = 环境 OK。

## 10. 每人一张卡

### 🧩 框架负责人

- **能做的**：维护公共代码文件（§5 清单）、开发资料页、实现数据接口（`getTotalExerciseCaloriesForDate`、`DailyHealthSummary.exerciseCaloriesKcal`、审核 C 的 `ExerciseRecordDao.getTotalCaloriesByDate`）、维护 `CatInteractionHub`、Review 并合并全员 PR、出最终 APK。
- **特别职责**：D 的视觉走查 PR 和 E 的小猫动画 PR 必须亲自 Review——一个是全局视觉基底，一个是跨模块数据消费方。
- **对外提供**：体重、每日热量目标、今日摄入、今日运动消耗、周汇总（全部经 `HealthRepository`）。

### 🏃 A · 运动模块

- **能改**：`exercise/` 包、`exercise_*`/`item_exercise_*`/`dialog_exercise_*` XML。
- **禁改**：`MainActivity`、公共数据库类、导航、Gradle、设计令牌文件、别人的包。
- **依赖 C 提供**：`ExerciseDao`、`ExerciseRecordDao`、`ExercisePlanDao`、`ExerciseCheckInDao` 及 ≥20 种动作种子数据。
- **可用接口**：`getCurrentWeightKg(...)`；`HealthCalculations.estimateExerciseCalories(...)`（公式 `MET × 体重kg × 分钟 ÷ 60`，**界面必须标注"估算"**）。
- **交付标准**：动作浏览、记录 CRUD（**每条记录必须写 `estimatedCaloriesKcal`**，E 的小猫状态靠它）、周计划与打卡、输入校验。
- **给 E 的承诺**：保存真实 `ExerciseRecord` 后调 `CatInteractionHub.notifyExerciseLogged(kcal)`，小猫自动"动起来"。

### 🍚 B · 饮食模块

- **能改**：`diet/` 包、`diet_*`/`item_food_*`/`dialog_food_*` XML。
- **依赖 C 提供**：`FoodDao`、`FoodRecordDao` 及 ≥100 种食物种子数据。
- **可用接口**：`getDailyCalorieGoalKcal(...)`；`HealthCalculations.calculateNutrientAmount(...)`。
- **交付标准**：食物搜索、四餐次记录、克数输入、**热量快照入库**、当日摄入合计展示。
- **给 E 的承诺**：保存真实 `FoodRecord` 后调 `CatInteractionHub.notifyMealLogged(kcal)`，小猫自动"吃东西"。

### 🗄️ C · 数据库

- **能改**：`model/`、`database/` 内的 Entity、DAO、种子数据类（含 `AppDatabase` 的 `@Database(entities=...)` 注册，改前通知框架负责人）。
- **禁改**：`HealthRepository` 公共方法签名、`MainActivity`、任何 UI。
- **契约**：严格按 §3.1/§3.6 实现；日期 `yyyy-MM-dd`；DAO 全同步；种子数据用 `insertAll` 且**只在库为空时插入一次**（框架已备好入口，见下）。
- **种子入口已备好**：`database/SeedData.java` 的 `buildExerciseSeeds()`（≥20 运动，含 MET）和 `buildFoodSeeds()`（≥100 食物）两个方法留给你填，触发时机和空库判断已写好；`clearAllData()` 后下次启动会自动重灌，动作库/食物库永不丢失。
- **交付标准**：① 全部 Entity/DAO 建齐并通过 Room 编译；② 种子数据 ≥20 运动（含 MET 值）、≥100 食物（含单位热量）；③ `ExerciseRecordDao.getTotalCaloriesByDate` 等新契约方法；④ 用户记录表（运动/饮食记录）CRUD 可用。

### 🎨 D · UI 设计

- **能改**：`colors.xml`、`themes.xml`、`dimens.xml`、`res/drawable/`、全局样式；他人模块布局**走 PR 给对方 Review**。
- **禁改**：Java 业务代码、DAO/Repository、导航结构（底部四个 tab 的顺序和 ID 冻结）、Gradle。
- **现状（v1.3）**：新粗野主义令牌已由框架落地（黑 `#000000` / 白 `#FFFFFF` / 黄 `#FFCC00` / 红 `#FF0000` / 蓝 `#0000CC`，直角粗边框卡片、黑底白字按钮、黄色导航选中块）；D 在此基础上做：① 补充绿色状态令牌（`health_accent_green`）；② 对各模块页面做视觉走查与微调，保证四个页面像同一个 App。
- **红线**：不改已被 Java 引用的资源名；不动导航菜单 ID；改令牌值须 PR 公告全组（全员都在引用）。

### 📊 E · 首页可视化（小猫动画是全场核心）

- **能改**：`home/` 包、`home_*`/`cat_home_*`/`item_home_*` XML、**自定义 View / 动画类（放 `home/` 包内）**。
- **禁改**：任何数据库类（只读）、A/B 的业务代码、设计令牌文件（向 D 提需求）、`CatInteractionHub` 的接口签名（需要扩展走 PR 找框架负责人）。
- **可用接口**（全在 `HealthRepository`，回调在主线程）：`getTotalCaloriesForDate`（摄入）、`getTotalExerciseCaloriesForDate`（消耗）、`getDailyCalorieGoalKcal`（目标）、`getDailySummaries`（周趋势）。
- **事件接口（v1.3）**：实现 `common/CatInteractionHub.CatMascot`，首页 `onResume()` 时 `CatInteractionHub.register(this)`、`onPause()` 时 `unregister(this)`；收到 `onExerciseLogged(kcal)` 让小猫"动起来"，收到 `onMealLogged(kcal)` 让小猫"吃东西"。
- **状态契约（§3.5）**：净热量三档语义（绿/黄/红）、目标为 `null` 的引导态、无记录的零态、`onResume()` 刷新；警示/鼓励语文案进 `strings.xml`（`home_cat_*`）。
- **挂载点已备好**：首页 `fragment_home.xml` 顶部有 `container_home_gauge`（FrameLayout，**id 冻结勿改**），里面已有静态小猫图 `image_home_cat`；E 可将它替换/包进自己的动画 View。
- **技术红线**：`onDraw` 不分配新对象（预分配 Paint/Path）；动画用 `ValueAnimator` 不得阻塞主线程；颜色引用设计令牌。
- **交付标准**：小猫动画（运动→动起来，进食→吃东西，状态颜色随净热量变化）+ 状态文案、今日摄入/消耗卡、周趋势；A/B 没开发完时显示"暂无记录"**就是正确行为**。

## 11. 日常开发循环

```bash
git status                            # 0. 有未提交修改先处理，保持干净
git switch main && git pull origin main
git switch feature/你的分支 && git merge main    # 1. 开工前同步队友最新代码
# …开发…
git add 自己的目录/文件                # 2. 只 add 自己确认过的文件，.idea 不加
git commit -m "feat: 说清楚做了什么"
git push                              # 3. 推到自己分支
```

然后 GitHub 网页 → **Compare & pull request** → base 选 `main` → 标题写模块、正文写"改了哪些文件 / 已完成 / 真实测试结果" → at 框架负责人 Review。被提意见就继续在自己分支改并 push，PR 自动更新。

**Git 纪律**：不用 `git reset --hard`、不 force push、不直接提交 main、不覆盖他人修改；出现合并冲突**停止操作，找框架负责人**。

## 12. 提交前检查清单（PR 必须全过）

1. **Sync Project with Gradle Files** 无错误。
2. **Build → Make Project** 无 Java/XML/Room 编译错误。
3. 运行到 API 26+ 模拟器，逐项点四个底部导航不崩。
4. Logcat 无 `FATAL EXCEPTION`、无 Room 主线程异常。
5. Terminal 执行 `gradlew.bat testDebugUnitTest assembleDebug` 全绿（改数据库后重点检查 Room 编译 SQL、外键、索引、版本）。
6. 单元测试覆盖新逻辑；自定义 View 注意旋转和暗色模式不崩。

## 13. 最终验收标准

1. 五模块功能按各自交付标准完成；资料含目标设置；运动/饮食记录真实落库且热量字段完整。
2. **首页小猫验收**：录入饮食后小猫"吃东西"且状态向黄/红变化并出现警示文案；录入运动后小猫"动起来"且状态向绿回落并出现鼓励文案；无目标时显示引导态；数据与 Room 完全一致。
3. 全 App 视觉统一（D 令牌体系），无写死颜色裸值。
4. 所有数据真实落 Room，空值语义正确，无写死演示数据。
5. 四个导航页交互完整，旋转/后台恢复不丢状态。
6. `testDebugUnitTest` 与 `assembleDebug` 通过，模拟器走查无崩溃。
7. 全程未引入 §1.2 禁止项（网络/AI/多用户/事件总线等）。
8. 五人代码均经 PR 合并进 main，公共接口与本文档保持一致。

## 14. 一句话记住

> **只改自己的包，取数走 Repository，DAO 只进后台线程，视觉用 D 的令牌，每天 merge main，完事提 PR 等合并。**
