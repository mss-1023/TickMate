# TickMate - 智能票据管家

一款原生 Android 消费记录管理应用，支持手动录入和拍照识别票据，帮助你轻松追踪每一笔消费。

## 功能特性

- **手动录入** - 快速记录商户名、金额、日期、类目、备注
- **快速记账** - 首页展开输入金额选类目，3秒完成
- **拍照识别** - 拍照/选图自动识别票据信息（百度云 OCR + ML Kit 离线双引擎）
- **识别纠错** - 识别结果可修改确认后保存
- **记录列表** - 按日期倒序，支持月份/类目筛选，左滑删除
- **统计图表** - 折线图（近12月趋势）+ 饼图（当月类目占比）
- **月度报表** - 总支出、日均、最大单笔、类目排行
- **预算提醒** - 设置月预算和阈值，超限首页警告 + 本地通知
- **类目管理** - 6个内置类目 + 自定义新增/编辑/删除
- **搜索** - 按商户名或备注搜索历史记录
- **数据导出** - 导出 CSV 文件分享
- **暗黑科技风格** - 全局深色主题，科技蓝 + 霓虹紫强调色

## 技术栈

| 类型 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVI（单向数据流） |
| 依赖注入 | Koin |
| 数据库 | Room (SQLite) |
| OCR | 百度云 OCR（在线）+ Google ML Kit（离线降级） |
| 相机 | CameraX |
| 图片 | Coil |
| 异步 | Kotlin Coroutines + Flow |

## 构建运行

**环境要求**：Android Studio + Android SDK (API 26+) + JDK 11

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
com.tickmate.app/
├── di/              # Koin 依赖注入
├── data/db/         # Room 数据库（Entity, DAO, Database）
├── data/repository/ # 数据仓库层
├── model/           # MVI 契约（Intent / State / Effect）
├── service/         # OCR识别、通知、类目匹配、数据导出
├── ui/
│   ├── theme/       # 暗黑科技风格主题
│   ├── navigation/  # 导航图 + 底部导航栏
│   ├── onboarding/  # 首次启动引导页
│   ├── home/        # 首页（概览 + 快速记账）
│   ├── record/      # 记录列表、录入/编辑、详情、搜索
│   ├── stats/       # 统计图表、月度报表
│   ├── camera/      # 拍照识别 + 确认页
│   ├── settings/    # 类目管理、预算、OCR配置、导出、关于
│   └── components/  # 通用组件
└── App.kt
```

## 截图

暗黑科技风格 UI，底部4Tab导航（首页/记录/统计/设置）。

## 兼容性

- 最低支持：Android 8.0 (API 26)
- 目标版本：Android 14 (API 34)

## License

MIT
