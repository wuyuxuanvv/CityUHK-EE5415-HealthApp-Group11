# 数据库使用说明书（C · 数据库）

> 面向全组。看完这一页你就知道：数据存在哪、怎么在 Android Studio 里看、代码里怎么取。
>
> **维护者**：C · 数据库（分支 `feature/database`）
> **适用版本**：`AppDatabase.version = 3`

---

## 1. 这到底是什么数据库

**不是 MySQL，不需要服务器，不联网。** 是本机嵌入式 **Room + SQLite**，跟着 APK 装在用户手机上。

| 项目 | 值 |
|---|---|
| 数据库文件名 | `health_app.db` |
| 手机上的真实路径 | `/data/data/com.example.healthapp/databases/health_app.db` |
| 代码位置 | `app/src/main/java/com/example/healthapp/database/` |
| Entity 位置 | `app/src/main/java/com/example/healthapp/model/` |
| 当前版本 | `version = 3` |
| 迁移策略 | `fallbackToDestructiveMigration()` —— 见 §6 |

> 路径里的 `com.example.healthapp` 就是 `applicationId`（`app/build.gradle.kts:12`）。改包名路径就变。

### 七张表

| 表名 | Entity | 性质 | 谁写 |
|---|---|---|---|
| `exercises` | `Exercise` | **内置字典**，39 条种子 | 只有 C（改种子） |
| `foods` | `Food` | **内置字典**，106 条种子 | 只有 C（改种子） |
| `exercise_records` | `ExerciseRecord` | 用户运动记录 | A · 运动 |
| `food_records` | `FoodRecord` | 用户饮食记录 | B · 饮食 |
| `user_profile` | `UserProfile` | 用户资料（单行，`id=1`） | 资料页 |
| `exercise_plans` | `ExercisePlan` | 运动计划 | A · 运动 |
| `exercise_check_ins` | `ExerciseCheckIn` | 计划打卡 | A · 运动 |

前三行列的是「字典表」——**只读**，用户改不了；后面是用户数据。

---

## 2. 在 Android Studio 里看数据（Database Inspector）

不用装任何插件。

### 打开方式

1. **先把 App 跑起来**（模拟器或真机都行），保持 App 在前台
2. 菜单 **View → Tool Windows → App Inspection**
3. 底部弹出面板，切到 **Database Inspector** 标签页
4. 下拉框里选 `com.example.healthapp` 进程 → 展开 `health_app.db` → 双击表名

### 三个前提（不满足就看不到）

- ✅ 装的是 **debug 版**（`assembleDebug`），release 版读不到
- ✅ 设备/模拟器 **API ≥ 26**（你们 `minSdk = 26`，本来就满足）
- ✅ **App 正在运行**。App 被杀掉，面板会显示 "No debuggable processes"

### 能干什么

- **看数据**：双击表，右侧显示全部行，可排序筛选
- **改数据**：直接在格子里双击改（改完立刻同步到正在运行的 App，方便调 UI）
- **跑 SQL**：点 **Open New Query Tab**（或 `Ctrl+Enter` 新开查询页），比如：

```sql
-- 看看种子到底进没进
SELECT COUNT(*) FROM exercises;
SELECT COUNT(*) FROM foods;

-- 检查两类热量模型有没有串（有氧 met>0 且系数=0；力量反过来）
SELECT category, met_value, kcal_per_set_per_kg, name
FROM exercises
WHERE (category = '有氧' AND (met_value <= 0 OR kcal_per_set_per_kg != 0))
   OR (category != '有氧' AND (met_value != 0 OR kcal_per_set_per_kg <= 0));
-- 期望：0 行
```

### 看不到数据时的排查顺序

1. **App 没在跑** → 重新启动 App（种子的插入在 `MainActivity.onCreate`，App 启动后才播种）
2. **表是空的** → 种子只在**表为空时**插入（§6）。可能库已建但播种失败，杀掉 App 重装一次
3. **表名不在列表里** → 你的分支没有最新的 Entity，`git pull` 后再跑
4. **刚改过 Entity 但结构不对** → `AppDatabase.version` 没升，见 §6

### 备选方案（Inspector 卡住时）

**A. 命令行直接查**（走 `run-as`，因为 debug 版可调试）：

```bash
adb shell run-as com.example.healthapp \
  sqlite3 /data/data/com.example.healthapp/databases/health_app.db "SELECT COUNT(*) FROM foods;"
```

**B. 拷出来用桌面工具打开**：

```bash
adb exec-out run-as com.example.healthapp \
  cat databases/health_app.db > health_app.db
```

然后用 DB Browser for SQLite 打开 `health_app.db`。

**C. Android Studio 的 Device Explorer**：`View → Tool Windows → Device Explorer` → `data/data/com.example.healthapp/databases/` → 右键 `health_app.db` → **Save As**。

---

## 3. 代码里怎么调用

### 唯一入口：`HealthRepository`

**所有跨模块读数据都要走 `HealthRepository`**（`database/HealthRepository.java`）。它是异步的，结果回到主线程。

```java
HealthRepository repository = HealthRepository.getInstance(requireContext());

// 拿运动库全量（字典表），自己在内存里按 category 筛
repository.getAllExercises(new RepositoryCallback<List<Exercise>>() {
    @Override
    public void onSuccess(List<Exercise> exercises) {
        if (!isAdded()) return;              // Fragment 可能已经销毁
        // 按部位分组
        for (Exercise exercise : exercises) {
            if (Exercise.CATEGORY_CHEST.equals(exercise.category)) { ... }
        }
    }

    @Override
    public void onError(Throwable error) {
        // 读取失败 ≠ 没有数据，要分开处理，别当成空列表
    }
});
```

```java
// 写一条运动记录（A · 运动）
ExerciseRecord record = new ExerciseRecord();
record.exerciseId = exercise.id;
record.recordDate = DateTimeUtils.today();   // "yyyy-MM-dd"
record.durationMinutes = minutes;            // 有氧填真实分钟；力量填 0
record.sets = 0;                             // 力量填组数；有氧填 0
record.repsPerSet = 0;
record.estimatedCaloriesKcal = kcal;         // 两种都必须写，首页/总结都读它
repository.addExerciseRecord(record, callback);
```

### 常用方法速查

| 方法 | 用途 |
|---|---|
| `getAllExercises` / `getAllFoods` | 拿字典表全量，内存筛 |
| `addExerciseRecord` / `deleteExerciseRecord` | 运动记录增删 |
| `addFoodRecord` / `deleteFoodRecord` | 饮食记录增删 |
| `getExerciseRecordsForDate(date)` | 某天运动记录 |
| `getFoodRecordsForDate(date)` | 某天饮食记录 |
| `getExerciseRecordsInRange(start, end)` | 区间运动记录（总结页用） |
| `getFoodRecordsInRange(start, end)` | 区间饮食记录 |
| `getTotalCaloriesForDate(date)` | 当日摄入 kcal |
| `getTotalExerciseCaloriesForDate(date)` | 当日消耗 kcal |
| `getExerciseMinutesForDate(date)` | 当日运动分钟（**只含有氧**） |
| `getExerciseCountForDate(date)` | 当日运动条数 |
| `getDistinctExerciseDaysInRange(start, end)` | 区间内有运动的天数（周目标用） |
| `hasFoodRecordsForDate(date)` | 当日有没有饮食记录 |
| `getUserProfile` / `saveUserProfile` | 用户资料 |
| `getCurrentWeightKg` | 当前体重（**可能返回 null**，用户没填资料） |
| `getDailyCalorieGoalKcal` | 每日热量目标（**可能 null**） |
| `getDailySummaries(start, end)` | 一次拿区间内每天的汇总 |

### 四条红线

1. **禁止**在主线程调 DAO，**禁止** `allowMainThreadQueries()`
2. **禁止**绕过 `HealthRepository` 去用别人模块的 DAO（跨模块只认 Repository）
3. **禁止**依赖别的模块的 Fragment / Adapter
4. `getCurrentWeightKg` 返回 **null** 时，必须弹「请先填资料」，**不准按默认体重写死**

### DAO 什么时候能用

**自己模块内部**的增删改查可以直接用 DAO（A 用 `ExerciseRecordDao`、B 用 `FoodRecordDao`），但**跨模块读**一律走 `HealthRepository`。

---

## 4. 日期与单位约定

| | 规则 |
|---|---|
| 日期 | `yyyy-MM-dd` 字符串，如 `2026-10-12`（`DateTimeUtils.today()`） |
| 时刻 | epoch milliseconds（`long`） |
| 热量 | kcal（double） |
| 蛋白质/碳水/脂肪 | 克 g（double） |
| 身高 | cm；体重 kg |
| 时长 | 分钟（int） |
| `record_date` | **有索引**，区间查询别用字符串拼日期 |

**食物营养值注意**：`foods` 里存的是**每 100 g**；`food_records` 里存的是**按用户填的克数换算后的绝对值**。所以读记录时直接用 `caloriesKcal`，**不要再乘**。

换算由 `HealthCalculations.calculateNutrientAmount(per100Grams, weightGrams)` 负责。

---

## 5. 两类运动的热量怎么算

全表共用 `exercises`，靠 `category` 区分，**唯一判据**：

```java
boolean isAerobic = Exercise.CATEGORY_AEROBIC.equals(exercise.category);
```

| 类别 | 公式 | 用哪列 |
|---|---|---|
| **有氧**（`有氧`） | `metValue × 体重kg × 分钟 ÷ 60` | `met_value` |
| **力量**（其余 7 类） | `sets × kcalPerSetPerKg × 体重kg` | `kcal_per_set_per_kg` |

**规则**：不参与计算的那一列**恒为 0**。

| | `met_value` | `kcal_per_set_per_kg` | `duration_minutes` | `sets` | `reps_per_set` |
|---|---|---|---|---|---|
| 有氧记录 | >0 | **0.0** | 真实分钟 | **0** | **0** |
| 力量记录 | **0.0** | >0 | **0** | ≥1 | ≥1 |

> ⚠️ **绝不能把两个公式相加** —— 不用的那列是 0，加起来就是把结果抹成 0。
> ⚠️ **力量必须校验 `sets ≥ 1`**，否则会出现「0 组算 0 kcal」的脏数据。
> ⚠️ 力量记录 `duration_minutes` 填 0，导致 `getExerciseMinutesForDate` **不包含力量** —— 首页和总结的「分钟」只有有氧。这是既定口径，不是 bug。

---

## 6. 改种子数据（C 自己改）

所有库内容集中在 **`SeedData.java` 顶部的两张常量表**，改一处即可：

```java
// {name, category, metValue, kcalPerSetPerKg}
private static final Object[][] EXERCISE_SEEDS = { ... };

// {name, category, kcal/100g, protein, carbs, fat, imageResourceName}
private static final Object[][] FOOD_SEEDS = { ... };
```

### ⚠️ 改完必须配套做一件事

`seedIfEmpty()` **只在表为空时插入**，且 `insertAll` 用的是 `OnConflictStrategy.IGNORE`。所以：

- **只改数值 / 新增条目** → 对**已安装的 App 完全无效**（表非空；重名会被 IGNORE 静默跳过）
- **要真正生效** → 改 `SeedData.java` → **把 `AppDatabase.version` 加 1** → 重装

`fallbackToDestructiveMigration()` 会**连用户记录一起清空**（`food_records` / `exercise_records` / `user_profile` / 计划打卡）。开发期可接受，**但要在 PR 里写明**，免得队友把「数据没了」当 bug 报。

### 两个已知坑

1. **重名会被静默丢弃**。`name` 有唯一索引，重复的第二条被 IGNORE 且**不报错**，实际入库数 < 表里写的数。改完跑 `SeedDataTest` 兜底（它会断言去重后数量 == 原始数量）。
2. **`Object[][]` 数字装箱**。读表必须用 `((Number) row[i]).doubleValue()`，**不能**强转 `(Double)` —— `116` 装箱成 `Integer`，强转会抛 `ClassCastException`。

### 发布前

`GUIDELINES.md:109`：**数据库发布后禁止用破坏性迁移代替正式迁移**。正式发布前必须写 `Migration`，建议同时打开 `exportSchema = true`。

### 自检

```bash
gradlew.bat testDebugUnitTest
```

`SeedDataTest` 会检查：运动 ≥20 条、食物 ≥100 条、category 合法、两类热量模型没串、食物营养值非负、名字无重复。

---

## 7. 图片和动图怎么处理的

### 一句话结论：**图片不存进 SQLite，数据库里只存一个「名字」字符串。**

图片本体是 APK 资源文件（`res/drawable/`），打包进 APK。数据库存的只是一个用来查表的键。

| | 数据库字段 | 要什么格式 | 现状 | 谁放文件 |
|---|---|---|---|---|
| **食物图片** | `foods.image_resource_name` | **静态图**：`.png` / `.jpg` / `.webp` | **已填值**（`food_rice`、`food_mantou`…），但**资源文件不存在** | D · UI |
| **运动图片** | `exercises.demo_resource_name` | **动图**：`.gif` | **全 null**（一个字都没填） | D · UI |

> ⚠️ **这两类的格式要求不一样，别搞混**：食物是**不会动的静态图**，运动要**会动的动图**。下面 §7.1 / §7.2 分开讲。

> **归属**：`res/drawable/` 归 **D · UI**。C 只负责数据库里那个字符串，**不碰图片文件**。

### 为什么这么设计（不是偷懒）

- 内置静态图片 → 打包进 APK，数据库只存名字。省用户存储、加载快、能参与资源压缩。
- 只有「用户自己拍的/上传的」图片才适合存数据库（存路径或 BLOB）。
- **为什么不直接存资源 id 数字**：资源 id 每次编译都可能变，存进数据库后跨版本直接失效。**存名字是正确的选择**，代价是运行期才能发现「名字对不上资源」。

### 7.1 食物图片 —— 静态图，格式随便

**要的就是一张普通静态图，不需要动。**

| | |
|---|---|
| 推荐格式 | **`.png`**（可透明背景）或 **`.webp`**（体积最小） |
| 也接受 | `.jpg` |
| 尺寸建议 | 正方形，256×256 或 512×512 就够 |
| 体积 | 单张 ≤ 50 KB |
| 命名 | `food_` 前缀，如 `food_rice.png` |
| 数据库字段 | `foods.image_resource_name` 填 `food_rice`（**不带后缀**） |

静态图**没有任何系统版本限制**，API 26 上也正常显示，放心用。

---

### 7.2 运动图片 —— 动图，用 GIF

**运动动作要「动」的效果，格式用 `.gif`。**

| | |
|---|---|
| 格式 | **`.gif`**（推荐，一个文件搞定） |
| 命名 | `exercise_demo_` 前缀，如 `exercise_demo_incline_press.gif` |
| 尺寸建议 | 宽 240–360 px，10–20 帧，1–2 秒无缝循环 |
| 体积 | 单张 ≤ 500 KB |
| 数据库字段 | `exercises.demo_resource_name` 填 `exercise_demo_incline_press`（**不带后缀**） |

#### ⚠️ GIF 的三条硬约束

1. **需要 API 28+**。GIF 解码依赖 `AnimatedImageDrawable`。你们 `minSdk = 26` → **Android 8.0 / 8.1 上只显示第一帧静止图**（**不崩溃**，只是不动）。你们模拟器是 **API 37**，demo 上没问题。
2. **GIF 不会自己动**。即使 API 28+，`setImageResource()` 之后**必须显式调 `start()`**，否则永远定格第一帧。官方文档写明 `autoStart` 默认是 `false`。
3. **不加版本判断会崩溃**。API 26/27 上 `AnimatedImageDrawable` 这个类根本不存在，直接引用会抛 `NoClassDefFoundError` —— 是**崩溃**，不是退化成静止。

#### 备选：帧动画（要兼容 Android 8.0/8.1 就用这个）

**帧动画是唯一 `minSdk 26` 全版本可用、零依赖的方案**：

- 一串 PNG 序列 + 一个 `xxx_anim.xml`，**全部平铺**在 `res/drawable/`
- 项目里 **`cat_run_anim.xml` + `cat_run_f1..f6.png` 就是现成模板**，照抄即可
- 加载方式和 GIF 不同（`AnimationDrawable`），见 §7.5

**引第三方图片库被 `GUIDELINES.md:33` 禁止**（Glide / Coil 都不行）。

> **「练到的部位变红」是画在动图里面的**，数据库**不需要**额外存「目标肌群」字段。

#### GIF vs 帧动画，怎么选

| | **GIF** | **帧动画（PNG 序列）** |
|---|---|---|
| 文件数 | **1 个** | N+1 个（N 帧 + 1 个 xml） |
| 能放 drawable | ✅ | ✅ |
| 解码门槛 | **API 28+** | **全版本（含 26）** |
| 会不会自动播放 | ❌ 要手写 `start()` | ❌ 要手写 `start()` |
| 体积 | 大 | 中 |
| 兼容 `minSdk 26` | ❌ 26/27 静止 | ✅ |
| 需要第三方库 | 不需要 | 不需要 |

> **结论**：只跑模拟器 / demo → 用 **GIF**，最省事；要真兼容 Android 8.0/8.1 → 用**帧动画**。

---

### 7.3 名字规则（决定数据库里能填什么）

**文件名必须是合法 Android 资源名**，因为数据库字符串要按它去查：

- ✅ 全小写、字母 / 数字 / 下划线、不能数字开头
- ❌ **不能有中文**、❌ **不能有连字符**、❌ 不能有空格
- ✅ `food_rice` → 文件 `food_rice.png`
- ❌ `Food-Rice` / `米饭` / `1_rice` / `food rice`

⚠️ **两个额外注意**：

- **数据库里填的是不带扩展名的名字**：填 `food_rice`，不是 `food_rice.png`
- **同名文件不能同时存在两种格式**：`xxx.png` 和 `xxx.gif` 同时存在会资源冲突，编译失败
- **`res/drawable/` 下不能建子文件夹**。Android 资源目录是**扁平**的，建了直接编译报错。多帧图片只能靠**文件名前缀**区分。

---

### 7.4 完整链路：四环，缺一不可

```
① 文件命名正确，放进 res/drawable/            ← D · UI
        ↓
② 数据库字段真的填了这个名字                    ← C · 数据库
        ↓
③ UI 写加载代码（getIdentifier + start()）      ← A · 运动 / 总结页
        ↓
④ 运行设备 API ≥ 28（走 GIF 时才需要）          ← 环境
```

**四环全对，动图才会出现。** 少任何一环，效果都是「什么都没有」，而且**全程不报错** —— 这是最难查的一类问题。

---

### 7.5 加载逻辑怎么写（A / 总结页负责）

#### 食物静态图

```java
int resId = getResources().getIdentifier(
        food.imageResourceName, "drawable", requireContext().getPackageName());

// 必须判 0！资源不存在时 getIdentifier 返回 0
if (resId != 0) {
    imageView.setImageResource(resId);
} else {
    imageView.setBackgroundColor(0xFFEEEEEE);   // 兜底，别让图片区一片空白
}
```

#### 运动 GIF 动图

```java
int resId = getResources().getIdentifier(
        exercise.demoResourceName, "drawable", requireContext().getPackageName());

if (resId == 0) {
    imageView.setBackgroundColor(0xFFEEEEEE);   // 资源缺失兜底
    return;
}

imageView.setImageResource(resId);

// ⚠️ 版本判断不能省：API 26/27 上这个类不存在，直接引用会崩溃
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {   // P = API 28
    Drawable d = imageView.getDrawable();
    if (d instanceof AnimatedImageDrawable) {
        AnimatedImageDrawable anim = (AnimatedImageDrawable) d;
        anim.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);  // 无限循环
        anim.start();          // ← 不写这句就永远定格第一帧
    }
}
```

#### 想做成「点击才播放」

```java
imageView.setOnClickListener(v -> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Drawable d = imageView.getDrawable();
        if (d instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable anim = (AnimatedImageDrawable) d;
            anim.stop();    // 先复位；已在播放时直接 start() 是无效的
            anim.start();
        }
    }
});
```

#### 帧动画方案（要兼容 API 26 时用这个）

```java
imageView.setImageResource(R.drawable.exercise_demo_incline_press_anim);
AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
anim.setOneShot(false);   // false = 循环播放（小猫用的是 true，只播一次）
anim.start();
```

---

### 7.6 ⚠️ 四个已知的坑

**坑 1：`start()` 的调用时机**

drawable 还没挂到窗口上时调 `start()` 会**静默失效** —— 不报错，就是不动。

- 普通页面里（如 `HomeFragment`）视图已就绪，没问题
- 在 **RecyclerView 列表**里 bind 的瞬间调用，**很可能不生效**，要包一层 `view.post(...)` 延后

**坑 2：食物图片名字填了，文件却不存在**

`foods.image_resource_name` **已经填了值**，但 `res/drawable/` 里**一个对应文件都没有**，而且**目前没有任何代码在读这个字段**。

后果：直接 `setImageResource(getIdentifier(...))` 而不判 0，拿到 `0` 后图片变**空白** —— 不崩溃，但看起来像坏了。**必须判 0 + 兜底。**

**坑 3：运动动图字段全是 null —— 这条最容易被漏**

`exercises.demo_resource_name` **39 条全是 null**，`SeedData.java` 从来没给它赋过值。

所以就算 GIF 放好了、加载代码也写好了，`getIdentifier(null, ...)` 依然返回 `0` → **图片区一片空白**，同样是静默失败。

**补这个字段是 C 的活**：在 `SeedData.java` 里赋值，并**升 `AppDatabase.version`**（3 → 4），否则对已安装的 App 不生效（原因见 §6）。

**坑 4：列表里让 39 个动图同时播放**

运动库列表如果有 39 个动作，每个都 `start()` → **严重卡顿 + 耗电**。

**推荐做法**：

- **列表里**：只显示**静态首帧**（不调 `start()`）
- **点进详情页**：才 `start()` 播放动图

性能最好，体验也对。

---

### 7.7 新增一张图片 / 一个动图的完整流程

| 步 | 谁 | 做什么 |
|---|---|---|
| 1 | **C** | 在 `SeedData.java` 定名字（合法、稳定、**发布后别改**） |
| 2 | **D** | 放文件到 `res/drawable/`，**文件名 = 数据库字符串 + 正确后缀** |
| 3 | **A** | 写 §7.5 的加载代码（判 0 + 兜底 + `start()`） |
| 4 | **C** | 改过种子就要**升 `AppDatabase.version`**，否则不生效 |

**名字一旦发布就别改** —— 改名字等于让已发布的数据库指向不存在的资源。

---

### 7.8 体积

| | 建议上限 |
|---|---|
| 食物静态图 | 单张 ≤ 50 KB |
| 运动 GIF | 单张 ≤ 500 KB |
| 帧动画 | 单帧 ≤ 100 KB，单图总 ≤ 1 MB |

图片全都会打进 APK，**体积直接累加到安装包上**。106 张食物图 + 39 个动图，不控制的话 APK 会很大。

---

## 8. 谁负责什么

| 内容 | 归谁 | 说明 |
|---|---|---|
| `SeedData.java` 两张种子表 | **C** | 改数据只改这里 |
| `model/` 下 Entity、`database/` 下 DAO | **C** | 但改公共接口前要通知框架负责人 |
| `AppDatabase` 的 `version` | **C**（需通知） | 改 Entity 必须同步升 version |
| `HealthRepository` 方法签名 | **框架负责人** | C 禁改；A/B 用的人多，动了全组崩 |
| `MainActivity`、`common/`、Gradle、Manifest | **框架负责人** | 未经确认不得改 |
| `res/drawable/` 图片 / 动图文件 | **D · UI** | C 只在数据库里留名字 |
| 力量热量公式 `estimateStrengthCalories` | **框架负责人** | 需加在 `common/HealthCalculations.java`，C 禁改 |

---

## 9. 常见问题

**Q：数据库是 MySQL 吗？需要连服务器吗？**
不是。是本机 SQLite，跟着 APK 装，不联网、不需要服务器。项目也**不允许**加 `INTERNET` 权限。

**Q：`/data/data/...` 这个路径我在电脑上看不到？**
那是手机内部存储，普通文件管理器进不去。用 §2 的 Database Inspector 或 `run-as` 命令。

**Q：为什么我改了 `SeedData.java` 但 App 里数据没变？**
见 §6 —— 种子只在表为空时插入，改完必须升 `AppDatabase.version`。

**Q：我升级了数据库版本，用户记录没了，是 bug 吗？**
不是。`fallbackToDestructiveMigration()` 会整库重建。开发期如此，发布前会换正式迁移。

**Q：`getCurrentWeightKg` 拿到 null 怎么办？**
说明用户还没填资料。必须走「请先填资料」提示分支，**不准按默认体重算**。

**Q：运动记录里为什么有 `duration_minutes = 0` 的行？**
那是力量训练。力量只记组数，分钟只算有氧（§5）。不是脏数据。

**Q：总结页想显示蛋白质，要加字段吗？**
不用。`food_records` 里 `protein_grams` / `carbs_grams` / `fat_grams` 已经在写了。只需给 `FoodRecordDao` 加一个 `SUM` 查询 —— 但那是改公共 DAO，要走框架负责人。

**Q：食物图片和运动图片分别要什么格式？**
**食物 = 静态图**，`.png` / `.jpg` / `.webp` 都行，简单一张不乱动的图就够。
**运动 = 动图**，用 **`.gif`**（要"动"的效果）。详见 §7.1 / §7.2。

**Q：图片文件是存在数据库里的吗？**
不是。**SQLite 里只存一个名字字符串**，图片本体是 APK 资源文件（`res/drawable/`），跟着 APK 打包。详见 §7。

**Q：运动库的动图，为什么我放了 GIF 却不动？**
按 §7.4 的四环逐条查，常见原因：
1. `exercises.demo_resource_name` 还是 **null**（`SeedData.java` 没赋值）
2. 文件名和数据库字符串**不一致**（大小写、后缀、下划线差一个字符都不行）
3. UI 代码**没写 `start()`** —— GIF 不会自动播放
4. 跑在 **API 26/27** 设备上（GIF 需要 API 28+）

四个原因**全都不会报错**，只是"什么都没有"。用 §2 的 Database Inspector 查字段值，是最快的定位方式。

**Q：多帧动画的图片是一个文件夹吗？**
**不是，`res/drawable/` 下不能建子文件夹**（Android 资源目录是扁平的，建了直接编译报错）。
- 用 **GIF**：就是**一个文件**
- 用**帧动画**：N 张帧 + 1 个 `xxx_anim.xml`，**全部平铺**在 `drawable/` 下，靠**文件名前缀**区分

**Q：我把文件名改成数据库字段的值，就能直接用了吗？**
**不能，光改名有三个断点**：
1. 文件名要对（这一条改名能解决）
2. 走帧动画时，**`xxx_anim.xml` 里要逐帧引用**——必须手写或脚本生成，改名不会自动产生
3. **UI 加载代码必须有人写**——数据库字段只是个索引字符串，它不会让任何东西自动生效

三环缺一，图片都不会出现。见 §7.4。
