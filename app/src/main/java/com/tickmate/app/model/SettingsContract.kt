package com.tickmate.app.model

import com.tickmate.app.data.db.entity.BudgetEntity
import com.tickmate.app.data.db.entity.CategoryEntity

// 设置页面 MVI 契约
sealed class SettingsIntent {
    object LoadSettings : SettingsIntent()
    data class AddCategory(val name: String) : SettingsIntent()
    data class EditCategory(val category: CategoryEntity) : SettingsIntent()
    data class DeleteCategory(val category: CategoryEntity) : SettingsIntent()
    data class SaveBudget(val monthlyBudget: Double, val thresholdPercent: Int) : SettingsIntent()
}

data class SettingsState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val budget: BudgetEntity? = null,
    val error: String? = null
)

sealed class SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect()
    object CategoryDeleted : SettingsEffect()
}
