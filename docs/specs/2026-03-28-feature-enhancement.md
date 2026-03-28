# TickMate 功能增强 - 设计文档

> 日期: 2026-03-28
> 状态: 已实现

## 新增功能

### 1. 欢迎引导页
- 首次启动展示 3 页引导（轻松记账、智能识别、洞察消费）
- SharedPreferences 记录是否完成引导
- 支持"跳过"和"下一页"，最后一页显示"开始使用"
- 文件: `ui/onboarding/OnboardingScreen.kt`

### 2. 快速记账入口
- 首页可展开的快速记账面板
- 输入金额 + 选类目即可完成记账，商户名可选
- 参考鲨鱼记账"3秒记账"体验
- 集成在 `ui/home/HomeScreen.kt`

### 3. 记录详情页
- 点击任意消费记录进入详情页
- 展示金额大字、商户名、类目、日期、备注、票据图片
- 支持编辑和删除操作
- 文件: `ui/record/RecordDetailScreen.kt`

### 4. 月度报表
- 独立的月度消费报告页面
- 统计指标：总支出、消费笔数、日均消费、最大单笔、消费天数
- 类目排行：按金额降序排列，显示百分比和进度条
- 文件: `ui/stats/MonthlyReportScreen.kt`

### 5. 搜索功能
- 独立搜索页面，自动聚焦输入框
- 按商户名和备注模糊搜索
- 实时搜索结果展示
- 文件: `ui/record/SearchScreen.kt`

### 6. 数据导出
- 设置页"导出 CSV"按钮
- 生成带 BOM 的 CSV 文件（兼容 Excel 中文）
- 通过系统分享菜单发送文件
- 文件: `service/ExportService.kt`

### 7. 关于页面
- 应用介绍、版本号、技术栈信息
- 暗黑科技风格一致
- 文件: `ui/settings/AboutScreen.kt`

## 新增路由

| 路由 | 页面 |
|------|------|
| onboarding | 欢迎引导页 |
| search | 搜索页 |
| record_detail/{recordId} | 记录详情页 |
| monthly_report | 月度报表页 |
| about | 关于页面 |

## 新增 DAO 查询

- `searchRecords(query)` - 模糊搜索
- `getMonthRecordCount(yearMonth)` - 月度记录数
- `getMonthMaxAmount(yearMonth)` - 月度最大单笔
- `getMonthActiveDays(yearMonth)` - 月度消费天数
