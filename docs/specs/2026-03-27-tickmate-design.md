# TickMate 智能票据管家 - 设计文档

> 日期: 2026-03-27
> 状态: 已批准

---

## 1. 项目概述

智能票据管家（TickMate）是一款原生 Android 应用，帮助用户通过拍照或手动录入管理消费票据。利用 Google ML Kit 离线识别票据信息，生成消费记录，提供预算提醒与统计功能。暗黑科技风格 UI。

## 2. 技术选型

| 类型 | 选型 | 理由 |
|------|------|------|
| 语言 | Kotlin | Google 官方推荐，现代、简洁、安全 |
| UI 框架 | Jetpack Compose | 声明式 UI，动画支持好，适合暗黑科技主题 |
| 架构模式 | MVI (单向数据流) | 状态管理严格，调试方便 |
| 依赖注入 | Koin | 轻量级 DI，配置简单 |
| 数据库 | Room (SQLite) | Jetpack 官方封装，支持 Flow 响应式查询 |
| OCR | Google ML Kit Text Recognition | 离线运行，无需 API Key，无网络依赖 |
| 图表 | Vico | Compose 原生图表库，支持折线图 + 饼图 |
| 相机 | CameraX | Jetpack 相机库，兼容性好 |
| 图片加载 | Coil | Kotlin-first 图片加载库，Compose 原生支持 |
| 异步 | Kotlin Coroutines + Flow | 异步处理 + 响应式数据流 |
| 导航 | Jetpack Navigation Compose | 官方导航方案 |
| 本地通知 | NotificationManager | Android 原生通知 |
| 最低兼容 | Android 8.0 (API 26) | |

## 3. 系统架构

```
┌─────────────────────────────────────────────┐
│              UI Layer (Compose)              │
│  Screen → Intent → ViewModel → State → UI   │
│  Home / List / Stats / Settings / Camera     │
└──────────────────┬──────────────────────────┘
                   │ MVI: Intent ↓ State ↑
┌──────────────────▼──────────────────────────┐
│           ViewModel Layer                    │
│  每个 Screen 对应一个 ViewModel              │
│  处理 Intent → 调用 Repository → 发射 State  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Repository Layer                   │
│  RecordRepository / CategoryRepository       │
│  BudgetRepository                            │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Data Layer (Room)                  │
│  RecordDao / CategoryDao / BudgetDao         │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           Services                           │
│  OCRService / NotificationService            │
│  CategoryMatcher / ImageHelper               │
└─────────────────────────────────────────────┘

DI: Koin 管理所有依赖注入
```

### 3.1 MVI 数据流

- Screen 发出 `Intent`（用户操作）
- ViewModel 接收 Intent，调用 Repository 处理业务逻辑
- ViewModel 发射新的 `State`（不可变 data class）
- Compose UI 根据 State 重组渲染
- 副作用（导航、Toast、通知）通过 `Effect` 单独 Channel 处理

### 3.2 MVI 通用模式

```kotlin
// 每个 Screen 统一遵循此模式
sealed class XxxIntent {
    data class LoadData(...) : XxxIntent()
    data class OnAction(...) : XxxIntent()
}

data class XxxState(
    val isLoading: Boolean = false,
    val data: ...,
    val error: String? = null
)

sealed class XxxEffect {
    data class ShowToast(val msg: String) : XxxEffect()
    object NavigateBack : XxxEffect()
}
```

## 4. 数据模型

### 4.1 RecordEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| merchant | String | 商户名 |
| amount | Double | 金额（正数） |
| date | String | 消费日期，YYYY-MM-DD |
| categoryId | Long | 外键 → CategoryEntity |
| note | String? | 备注（可选） |
| imageUri | String? | 原始图片路径（可选） |
| createdAt | Long | 记录创建时间戳 |

### 4.2 CategoryEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 类目名称（唯一） |
| icon | String? | 图标名称 |
| isDefault | Boolean | 内置类目不可删除 |

### 4.3 BudgetEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，固定为 1 |
| monthlyBudget | Double | 月预算总额 |
| thresholdPercent | Int | 提醒阈值百分比（如 80） |
| yearMonth | String | YYYY-MM，所属月份 |

### 4.4 默认类目

应用首次启动时插入：餐饮、交通、购物、娱乐、医疗、其他

### 4.5 关键查询

- 按月份+类目筛选：`WHERE date LIKE 'YYYY-MM%' AND categoryId = ?`
- 月度汇总（折线图）：`SELECT strftime('%Y-%m', date), SUM(amount) GROUP BY 1`
- 类目占比（饼图）：`SELECT categoryId, SUM(amount) WHERE date LIKE 'YYYY-MM%' GROUP BY 1`
- 删除类目时：关联记录的 categoryId 自动改为"其他"的 id

### 4.6 迁移策略

Room Migration 机制，版本号递增，保证升级不丢数据。

## 5. Screen 设计

### 5.1 导航结构

底部导航栏（BottomNavigation）4个 Tab：
- 首页（Home）
- 记录（List）
- 统计（Stats）
- 设置（Settings）

CameraScreen 从首页或记录页通过 FAB 按钮进入，不在底部导航中。

### 5.2 Screen - Intent - State - Effect 映射

| Screen | Intent | State | Effect |
|--------|--------|-------|--------|
| HomeScreen | 查看概览、点击添加 | 当月总支出、预算进度、最近记录 | 预算超限警告横幅 |
| RecordListScreen | 筛选月份/类目、删除、编辑 | 记录列表、筛选条件、加载状态 | 删除确认弹窗 |
| StatsScreen | 切换月份 | 折线图数据(12月)、饼图数据(当月) | 无 |
| CameraScreen | 拍照/选图、确认/修改识别结果 | 图片URI、识别结果、识别状态、商品明细 | 导航到确认页 |
| SettingsScreen | 管理类目、设置预算 | 类目列表、预算设置 | 保存成功 Toast |

## 6. OCR 识别方案

### 6.1 技术方案

Google ML Kit Text Recognition，离线运行，无需网络。

### 6.2 识别流程

1. 用户点击"拍照识别"，调用 CameraX 拍照或系统相册选图
2. ML Kit 识别图片中的文字
3. 本地解析逻辑提取字段：
   - **金额**：正则匹配 `\d+\.\d{2}`，优先取"合计/总计"附近数字，否则取最大值
   - **日期**：匹配日期格式（YYYY-MM-DD / YYYY/MM/DD / MM月DD日 等）
   - **商户名**：提取首行或"商户/店名"关键词附近文字
   - **商品明细**（P3）：按行解析"名称 x数量 单价"模式
4. 跳转确认页，展示识别结果，用户可修改任意字段
5. 确认后保存为正式记录

### 6.3 类目推荐

维护关键词→类目映射表：
- "星巴克/麦当劳/餐厅/食堂" → 餐饮
- "滴滴/地铁/公交/加油" → 交通
- "超市/商场/淘宝" → 购物
- "电影/KTV/游戏" → 娱乐
- "医院/药店/诊所" → 医疗
- 无匹配 → 其他

## 7. 图表方案

使用 Vico 图表库：

- **折线图**：X轴 12 个月份，Y轴金额，查询 `SUM(amount) GROUP BY strftime('%Y-%m', date)`，无数据月份显示 0
- **饼图**：当月各类目占比，图例显示类目名 + 金额 + 百分比
- **样式**：蓝色/紫色渐变线条，暗色背景适配，绘制动画

## 8. 预算提醒方案

- 每次增删改记录后，ViewModel 重新计算当月总支出
- 若 `总支出 / 预算 * 100 >= 阈值`：
  - 首页显示警告横幅（State 驱动）
  - 通过 NotificationManager 发本地通知（每天最多一次，用 SharedPreferences 记录）
- 首次设置预算时引导请求通知权限（Android 13+ 需 POST_NOTIFICATIONS 运行时权限）

## 9. UI 主题 - 暗黑科技风格

### 9.1 色彩

| 用途 | 色值 |
|------|------|
| 主背景 | #0A0A0A (深黑) |
| 卡片背景 | #1C1C1E (碳黑) |
| 强调色主 | #00A6FF (科技蓝) |
| 强调色副 | #A020F0 (霓虹紫) |
| 文字主色 | #FFFFFF |
| 文字副色 | #8E8E93 |
| 成功色 | #30D158 |
| 警告色 | #FF453A |

### 9.2 组件风格

- 边框：1dp 半透明白边 + 微发光效果
- 卡片：12dp 圆角，轻微 elevation
- 按钮：蓝紫渐变背景 或 发光边框
- 字体：系统无衬线，金额等数字用等宽字体

### 9.3 动画

Compose Animation API：页面淡入淡出、卡片加载渐显、图表绘制动画、按钮点击缩放

## 10. 项目包结构

```
com.tickmate.app/
├── di/                      # Koin 模块定义
│   └── AppModule.kt
├── data/
│   ├── db/                  # Room 数据库
│   │   ├── AppDatabase.kt
│   │   ├── dao/             # RecordDao, CategoryDao, BudgetDao
│   │   └── entity/          # RecordEntity, CategoryEntity, BudgetEntity
│   └── repository/          # RecordRepository, CategoryRepository, BudgetRepository
├── model/                   # UI 层数据模型 & Intent/State/Effect
├── service/
│   ├── OCRService.kt        # ML Kit 识别 + 文字解析
│   ├── NotificationService.kt
│   └── CategoryMatcher.kt   # 关键词→类目匹配
├── ui/
│   ├── theme/               # Theme.kt, Color.kt, Type.kt
│   ├── navigation/          # NavGraph.kt, BottomNavBar.kt
│   ├── home/                # HomeScreen, HomeViewModel
│   ├── record/              # RecordListScreen, RecordListViewModel
│   ├── stats/               # StatsScreen, StatsViewModel
│   ├── camera/              # CameraScreen, ConfirmScreen, CameraViewModel
│   ├── settings/            # SettingsScreen, CategoryManageScreen, BudgetScreen, SettingsViewModel
│   └── components/          # 通用组件：ExpenseCard, FilterBar, GlowButton 等
└── App.kt                   # Application 类，初始化 Koin
```

## 11. 功能清单与优先级

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 手动录入 | 表单输入商户名/金额/日期/类目/备注，保存记录 |
| P0 | 记录列表 | 按日期倒序，支持月份和类目筛选，支持删除/编辑 |
| P1 | 统计图表 | 折线图(近12月趋势) + 饼图(当月类目占比) |
| P1 | 类目管理 | 新增/编辑/删除自定义类目，内置类目不可删 |
| P1 | 数据持久化 | Room 数据库，关闭重开数据不丢失 |
| P2 | 拍照识别 | ML Kit 离线识别票据，提取商户名/金额/日期 |
| P2 | 识别纠错 | 确认页展示识别结果，用户可修改后保存 |
| P2 | 预算提醒 | 设置月预算和阈值，超限时本地通知+首页横幅 |
| P3 | 明细解析 | 解析小票商品行明细，解析失败不影响主流程 |

全部功能一次性开发完成。
