# HealthApp — Personal Health Management

**City University of Hong Kong (CityUHK) · EE5415: Mobile Applications Design and Development · Group 11 · 2026**

A lightweight, offline-first Android course project for tracking exercise, food intake, and everyday health goals.

一款面向日常运动、饮食记录与健康目标管理的轻量级、离线优先 Android 课程项目。

**[English](#english) | [简体中文](#简体中文)**

> **Project status / 项目状态:** Under active development and integration / 正在开发与联调。This README describes the publicly visible `main` branch reviewed on **12 October 2026**; it is not a claim that all features have passed final acceptance tests.

---

## English

### About the project

HealthApp is developed by **Group 11** for **EE5415 — Mobile Applications Design and Development** at the **City University of Hong Kong**. Our goal is to build a straightforward personal health tracker rather than a professional fitness-training or medical application.

The app combines **exercise logging, food tracking, a daily/weekly summary, a personal profile, and an interactive cat mascot**. Core records are stored on the device with **Room (SQLite)**; no account, cloud backend, or internet connection is required for normal use.

### Main features and progress

| Module | Features currently present in source | Status |
| --- | --- | --- |
| **Home** | Today's exercise and calorie overview, goal-related feedback, interactive cat mascot reacting to logged activities | Implemented; integration and polish ongoing |
| **Exercise** | Searchable library of **22 exercise types** (including three strength-training intensities), static activity icons, time-based logging, MET calorie estimates, current-week date selection and record deletion | Core recording available; editing and plan/check-in workflows remain follow-up work |
| **Diet** | Food search, breakfast/lunch/dinner/snack records, portion input in grams, calorie and nutrient estimates, daily intake display and date-based records | Core features available; further testing ongoing |
| **Summary** | Daily exercise minutes, calorie intake and exercise expenditure, weekly exercise chart, and recent exercise/food history | Code integrated; final verification and refinement pending |
| **Profile** | Basic profile, avatar, body measurements and exercise/calorie goals | Implemented; integration testing ongoing |

**Scope note:** Sleep tracking was removed from the current product scope. Animated exercise tutorials, AI recommendations, wearable-device integration, cloud sync, and multi-user accounts are not part of this version.

### How calorie estimates work

For a recorded exercise, the app uses a simplified metabolic equivalent (MET) model:

`Estimated exercise energy (kcal) = MET × body weight (kg) × duration (minutes) / 60`

Food intake is estimated from portion size and the food database's nutrient values, typically per 100 g. **“Net calories” in this project means recorded intake minus recorded exercise expenditure; it is not a complete daily energy-balance calculation** and does not account for all other energy expenditure.

### Technology stack

| Area | Technology |
| --- | --- |
| Platform | Native Android (minimum Android API 26) |
| Programming | Java 11 |
| User interface | XML layouts, Android Views, Fragments, Material Components |
| Local database | Room / SQLite |
| Asynchronous data access | `DatabaseExecutor`, `HealthRepository`, `RepositoryCallback` |
| Build | Gradle Kotlin DSL (build scripts only) |
| Development and collaboration | Android Studio, Git, GitHub, feature branches and pull requests |

**Architecture in brief:** Exercise and diet records are saved in Room. Screens read shared data through `HealthRepository`, rather than passing totals directly between Fragments. `CatInteractionHub` provides a separate, narrow event channel for mascot reactions.

### Getting started

1. Install **Android Studio**, the Android SDK, and Git.
2. Clone the repository: `git clone https://github.com/wuyuxuanvv/android-group11.git`
3. Open the cloned **`android-group11`** directory as an Android Studio project.
4. Let **Gradle Sync** finish and install the requested SDK components if prompted.
5. Run the **`app`** configuration on an emulator or Android device with **API 26+**.

On Windows PowerShell, run a local build and unit tests from the repository root:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Build or test success must be checked in your own environment; a public source snapshot alone does not verify runtime behavior.

### Repository guide

```text
android-group11/
├── app/src/main/java/com/example/healthapp/
│   ├── common/       # Shared calculations, dates, and mascot events
│   ├── database/     # Room database, DAOs, repository, seed data
│   ├── model/        # Persistent entities and shared models
│   ├── home/         # Home screen and interactive mascot
│   ├── exercise/     # Exercise selection and logging
│   ├── diet/         # Food and nutrition logging
│   ├── summary/      # Daily/weekly summaries and history
│   └── profile/      # User profile and goals
├── app/src/main/res/ # Layouts, drawables, strings, themes
├── docs/             # Requirements and development documents
└── README.md
```

Useful references:

- [`docs/GUIDELINES.md`](docs/GUIDELINES.md) — team conventions and module responsibilities.
- [`docs/DATABASE_USAGE.md`](docs/DATABASE_USAGE.md) — database and repository usage notes.
- [`AGENTS.md`](AGENTS.md) — repository-specific AI-assisted development instructions.
- [`docs/PRD.md`](docs/PRD.md), [`docs/DEVELOPMENT_GUIDE.md`](docs/DEVELOPMENT_GUIDE.md) — historical planning references; these explicitly identify some older content as superseded.

**Documentation caveat:** Some planning documents reflect an older navigation or exercise-library design. Check the latest `main` branch and coordinate shared-interface changes with the team before implementing them.

### Teamwork and academic context

This is a **six-person course project**. Development work is divided across the shared framework/profile, Exercise, Diet, database/data contracts, UI/assets, and home/summary visualization. Team members develop on feature branches and submit changes through pull requests.

We hope this repository helps future EE5415 students understand the organization of a collaborative Android project. Please follow your own course's academic-integrity requirements when using it for reference.

**Disclaimer:** HealthApp is an educational prototype, not a medical device or a source of medical advice. Energy and nutritional estimates are approximate. Some visual assets may have separate third-party licenses; check their sources before reuse. Public repository visibility does not by itself grant an open-source license for all code or assets.

---

## 简体中文

### 项目简介

**HealthApp** 是香港城市大学（CityUHK）**EE5415 — Mobile Applications Design and Development（移动应用设计与开发）**课程第 **11 小组（Group 11）**于 2026 年开发的 Android 课程项目。

我们的目标是实现一款**简单、直观、适合日常使用**的个人健康管理 App，而不是专业健身训练日志或医疗软件。应用将**运动记录、饮食管理、每日/每周总结、个人资料与健康目标、小猫吉祥物互动**整合在一起。

核心数据通过 **Room（SQLite）保存在设备本地**，正常使用不依赖云服务器、在线账号或网络连接。

### 主要功能与当前进度

| 模块 | 当前代码中已有的功能 | 进度说明 |
| --- | --- | --- |
| **首页 Home** | 今日运动与卡路里概览、目标状态反馈、随记录发生互动的小猫吉祥物 | 已有实现，持续联调与优化 |
| **运动 Exercise** | **22 种运动**（含低/中/高三档力量训练）、动作搜索、静态运动图标、按时长记录、MET 热量估算、本自然周日期选择与历史记录删除 | 核心记录功能已接入；编辑、运动计划及打卡尚待完善 |
| **饮食 Diet** | 食物搜索、早/午/晚/加餐分类、按克数记录、热量及营养素估算、当日摄入统计和按日期查看 | 核心功能已有代码，仍需测试 |
| **总结 Summary** | 今日运动时长、饮食摄入、运动消耗、每周运动图表及近期记录查询 | 页面代码已接入，仍需最终联调验收 |
| **个人资料 Profile** | 个人基本信息、头像、身高体重与运动/热量目标 | 已有实现，仍需整体测试 |

**功能范围说明：** 当前版本已取消睡眠记录模块。运动演示动图、AI 推荐、穿戴设备接入、云同步和多用户账号不属于本阶段交付内容。

### 热量计算与数据交互

运动消耗采用简化的 MET（代谢当量）估算公式：

`运动估算热量（kcal）= MET × 体重（kg）× 运动时长（分钟）/ 60`

饮食热量和主要营养素则根据食物营养数据及实际摄入克数估算。项目中展示的**“净热量”仅指记录的饮食摄入减去记录的运动消耗**，并不是考虑基础代谢及全部能量支出后的真实每日热量盈余。

运动和饮食模块将记录保存至本地 Room 数据库；首页和总结模块通过统一的 `HealthRepository` 异步读取数据，不依赖模块之间直接传递页面状态。`CatInteractionHub` 则专门用于运动和饮食记录触发小猫互动。

### 技术栈

| 项目 | 技术方案 |
| --- | --- |
| 运行平台 | 原生 Android（最低 API 26） |
| 开发语言 | Java 11 |
| 用户界面 | XML、Android Views、Fragment、Material Components |
| 本地存储 | Room / SQLite |
| 异步数据访问 | `DatabaseExecutor`、`HealthRepository`、`RepositoryCallback` |
| 构建配置 | Gradle Kotlin DSL（仅构建脚本使用 Kotlin） |
| 开发与协作 | Android Studio、Git、GitHub、功能分支与 Pull Request |

### 如何运行

1. 安装 **Android Studio**、Android SDK 和 Git。
2. 克隆本仓库：`git clone https://github.com/wuyuxuanvv/android-group11.git`。
3. 使用 Android Studio 打开克隆得到的 **`android-group11`** 项目目录。
4. 等待 Gradle Sync 完成，根据提示安装缺少的 SDK。
5. 选择 **API 26 或以上**的 Android 模拟器或真机，运行 `app`。

在仓库根目录使用 Windows PowerShell 执行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

请以自己环境中的实际编译、测试和模拟器结果为准；README 不代表所有功能已通过最终验收。

### 代码结构与开发文档

主要源码位于 `app/src/main/java/com/example/healthapp/`：

- `common/`：日期、热量计算、公共工具和小猫事件通知。
- `database/`、`model/`：Room、DAO、Repository、Entity 和内置运动/食物数据。
- `exercise/`、`diet/`：运动和饮食记录功能。
- `home/`、`summary/`：首页小猫互动、健康概览、统计与历史记录。
- `profile/`：个人资料与目标。
- `app/src/main/res/`：XML 布局、静态图标和其他界面资源。
- `docs/`：团队分工、开发规范和需求资料。

建议先阅读 [`docs/GUIDELINES.md`](docs/GUIDELINES.md) 和 [`docs/DATABASE_USAGE.md`](docs/DATABASE_USAGE.md)。部分较早的规划文档已经过时；若文档与当前代码不一致，应核实最新实现并与模块负责人协调，不要直接修改公共数据库和接口。

### 小组协作与项目声明

本项目由 **Group 11 六名成员**协作开发，涉及公共框架及资料页、运动、饮食、数据库、界面资源，以及首页与总结可视化等工作。团队通过 Git 功能分支、Pull Request 和统一的数据接口协同开发。

我们希望本仓库不仅记录本组的开发过程，也能帮助未来修读 **EE5415** 的同学了解 Android 课程项目的设计、实现与多人协作方式。参考项目时，请遵守所在课程的学术诚信要求。

**免责声明：** 本项目仅用于课程学习和一般健康习惯记录，不提供医疗诊断或专业医疗建议。热量与营养数据均为估算。部分 SVG 等视觉资源可能具有独立的第三方许可，复用前应核对来源与授权；仓库公开不等于全部内容自动采用开源许可证。

---

**CityUHK · EE5415 · Group 11 · 2026**
