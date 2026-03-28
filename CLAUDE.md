# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TickMate（智能票据管家）是一款原生 Android 消费记录管理应用。支持手动录入、快速记账、拍照识别（Google ML Kit 离线 OCR）、消费统计（折线图+饼图）、月度报表、类目管理、预算提醒、搜索、CSV 导出。暗黑科技风格 UI。

## Build & Run

```bash
# 需要 Android Studio + Android SDK (API 26+)
./gradlew assembleDebug                    # 构建 debug APK
./gradlew assembleRelease                  # 构建 release APK
./gradlew installDebug                     # 安装到设备/模拟器
./gradlew test                             # 运行单元测试
# APK: app/build/outputs/apk/release/app-release.apk
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVI (单向数据流: Intent → ViewModel → State → UI)
- **DI**: Koin
- **Database**: Room (SQLite)
- **OCR**: Google ML Kit Text Recognition (Chinese, 离线)
- **Charts**: Canvas 自绘（折线图+饼图）
- **Camera**: CameraX
- **Image**: Coil
- **Async**: Kotlin Coroutines + Flow
- **Min SDK**: 26 (Android 8.0)

## Architecture

```
UI Layer (Compose Screen)
  ↓ Intent    ↑ State/Effect
ViewModel Layer
  ↓
Repository Layer
  ↓
Data Layer (Room DAO → SQLite)
  +
Service Layer (OCRService, NotificationService, CategoryMatcher, ExportService)

DI: Koin (AppModule.kt)
```

## Package Structure

```
com.tickmate.app/
├── di/             # Koin 模块
├── data/db/        # Room: entity/, dao/, AppDatabase
├── data/repository/# RecordRepository, CategoryRepository, BudgetRepository
├── model/          # MVI 契约 (Intent/State/Effect per screen)
├── service/        # OCRService, NotificationService, CategoryMatcher, ExportService
├── ui/theme/       # 暗黑科技风格主题
├── ui/navigation/  # NavGraph, BottomNavBar, Routes
├── ui/onboarding/  # 首次启动引导页
├── ui/home/        # 首页（快速记账、月度概览）
├── ui/record/      # 记录列表、手动录入/编辑、详情页、搜索
├── ui/stats/       # 统计图表、月度报表
├── ui/camera/      # 拍照识别 + 确认页
├── ui/settings/    # 类目管理、预算设置、CSV导出、关于
└── ui/components/  # 通用组件 (GlowCard, GradientButton, ExpenseCard)
```

## Conventions

- 界面文字、注释、文档：中文
- MVI 模式：每个 Screen 有对应的 Intent/State/Effect sealed class
- 文档保存在 docs/ 目录，不要删除
- 单元测试在 app/src/test/ 目录
