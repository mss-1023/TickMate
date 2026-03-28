package com.tickmate.app.model

import com.tickmate.app.data.db.entity.CategoryEntity

// 统计页面 MVI 契约
sealed class StatsIntent {
    object LoadStats : StatsIntent()
}

data class StatsState(
    val isLoading: Boolean = true,
    // 折线图：近12个月月度支出
    val monthlyData: List<MonthAmount> = emptyList(),
    // 饼图：当月各类目占比
    val categoryData: List<CategoryAmount> = emptyList(),
    val currentMonth: String = "",
    val error: String? = null
)

data class MonthAmount(
    val month: String,      // YYYY-MM
    val amount: Double
)

data class CategoryAmount(
    val category: CategoryEntity,
    val amount: Double,
    val percent: Float       // 百分比 0-100
)
