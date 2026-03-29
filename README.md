# TickMate - 智能票据管家

一款原生 Android 消费记录管理应用，支持手动录入和拍照识别票据，帮助你轻松追踪每一笔消费。

## 功能特性

- **手动录入** - 快速记录商户名、金额、日期、类目、备注
- **快速记账** - 首页展开输入金额选类目，3秒完成
- **拍照识别** - 拍照/选图自动识别票据信息（百度云 OCR 在线 + ML Kit 离线双引擎，无网自动降级）
- **智能类目** - 根据发票类目字段、商品内容、商户名自动推荐消费类目
- **识别纠错** - 识别结果可修改确认后保存
- **记录列表** - 按日期倒序，支持月份/类目筛选，左滑删除
- **记录详情** - 查看完整消费详情，支持编辑和删除
- **统计图表** - 折线图（近12月趋势）+ 饼图（当月类目占比）
- **月度报表** - 总支出、日均消费、最大单笔、消费天数、类目排行
- **预算提醒** - 设置月预算和阈值，超限首页警告横幅 + 本地推送通知
- **类目管理** - 6个内置类目 + 自定义新增/编辑/删除（可折叠面板）
- **搜索** - 按商户名或备注模糊搜索历史记录
- **数据导出** - 导出 CSV 文件（兼容 Excel 中文），通过系统分享发送
- **欢迎引导** - 首次启动 3 页引导，介绍核心功能
- **暗黑科技风格** - 全局深色主题，科技蓝 + 霓虹紫强调色，发光卡片 + 渐变按钮

## 技术栈

| 类型 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVI（单向数据流：Intent → ViewModel → State → UI） |
| 依赖注入 | Koin |
| 数据库 | Room (SQLite) |
| OCR | 百度云 OCR（在线，高精度）+ Google ML Kit（离线降级） |
| 相机 | CameraX + ActivityResultContracts |
| 图片 | Coil |
| 图表 | Canvas 自绘（折线图 + 饼图） |
| 异步 | Kotlin Coroutines + Flow |
| 构建 | Gradle 8.9 + AGP 8.7.3 + Kotlin 1.9.20 |

## 构建运行

**环境要求**：Android Studio Panda (2025.3+) + Android SDK (API 26+) + JDK 17+

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行单元测试
./gradlew test
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
com.tickmate.app/
├── di/              # Koin 依赖注入模块
├── data/
│   ├── db/          # Room 数据库（Entity, DAO, Database）
│   └── repository/  # 数据仓库层
├── model/           # MVI 契约（Intent / State / Effect per screen）
├── service/
│   ├── OCRService        # ML Kit 离线 OCR
│   ├── OnlineOCRService  # 百度云在线 OCR
│   ├── CategoryMatcher   # 智能类目匹配（4级优先级）
│   ├── NotificationService # 预算提醒通知
│   └── ExportService     # CSV 数据导出
├── ui/
│   ├── theme/       # 暗黑科技风格主题（Color, Type, Theme）
│   ├── navigation/  # NavGraph（Tab无动画瞬切）+ 自定义底部导航栏
│   ├── onboarding/  # 首次启动引导页
│   ├── home/        # 首页（月度概览 + 快速记账 + 最近记录）
│   ├── record/      # 记录列表、手动录入/编辑、详情页、搜索
│   ├── stats/       # 统计图表（Canvas自绘）、月度报表
│   ├── camera/      # 拍照识别（双引擎 + 确认纠错）
│   ├── settings/    # 类目管理、预算设置、OCR引擎配置、数据导出、关于
│   └── components/  # 通用组件（GlowCard, GradientButton, ExpenseCard等）
└── App.kt           # Application（Koin初始化 + 缓存清理）
```

## 兼容性

- 最低支持：Android 8.0 (API 26)
- 目标版本：Android 14 (API 34)
- 开发环境：Android Studio Panda 2025.3 + JDK 17

## License

MIT
