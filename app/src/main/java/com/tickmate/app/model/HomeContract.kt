package com.tickmate.app.model

import com.tickmate.app.data.db.entity.BudgetEntity
import com.tickmate.app.data.db.entity.RecordEntity

// 首页 MVI 契约
sealed class HomeIntent {
    object LoadData : HomeIntent()
}

data class HomeState(
    val isLoading: Boolean = true,
    val monthTotal: Double = 0.0,
    val budget: BudgetEntity? = null,
    val budgetPercent: Int = 0,
    val recentRecords: List<RecordEntity> = emptyList(),
    val showBudgetWarning: Boolean = false,
    val error: String? = null
)

sealed class HomeEffect {
    data class BudgetAlert(val percent: Int) : HomeEffect()
}
