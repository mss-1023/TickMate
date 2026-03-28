package com.tickmate.app.model

import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.db.entity.RecordEntity

// 记录列表 MVI 契约
sealed class RecordListIntent {
    object LoadRecords : RecordListIntent()
    data class FilterByMonth(val yearMonth: String?) : RecordListIntent()
    data class FilterByCategory(val categoryId: Long?) : RecordListIntent()
    data class DeleteRecord(val record: RecordEntity) : RecordListIntent()
    data class SaveRecord(val record: RecordEntity) : RecordListIntent()
}

data class RecordListState(
    val isLoading: Boolean = true,
    val records: List<RecordEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedMonth: String? = null,      // null 表示全部
    val selectedCategoryId: Long? = null,   // null 表示全部
    val error: String? = null
)

sealed class RecordListEffect {
    data class ShowToast(val message: String) : RecordListEffect()
    object RecordSaved : RecordListEffect()
}
