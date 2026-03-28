package com.tickmate.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tickmate.app.data.db.entity.BudgetEntity
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.repository.BudgetRepository
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.service.ExportService
import com.tickmate.app.model.SettingsEffect
import com.tickmate.app.model.SettingsIntent
import com.tickmate.app.model.SettingsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SettingsViewModel(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    private val currentYearMonth: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    init {
        handleIntent(SettingsIntent.LoadSettings)
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.LoadSettings -> loadSettings()
            is SettingsIntent.AddCategory -> addCategory(intent.name)
            is SettingsIntent.EditCategory -> editCategory(intent.category)
            is SettingsIntent.DeleteCategory -> deleteCategory(intent.category)
            is SettingsIntent.SaveBudget -> saveBudget(intent.monthlyBudget, intent.thresholdPercent)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _state.update { it.copy(categories = categories, isLoading = false) }
            }
        }
        viewModelScope.launch {
            budgetRepository.getBudget(currentYearMonth).collect { budget ->
                _state.update { it.copy(budget = budget) }
            }
        }
    }

    private fun addCategory(name: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _effect.send(SettingsEffect.ShowToast("类目名称不能为空")) }
            return
        }
        viewModelScope.launch {
            val existing = categoryRepository.getCategoryByName(name)
            if (existing != null) {
                _effect.send(SettingsEffect.ShowToast("类目已存在"))
                return@launch
            }
            categoryRepository.insertCategory(CategoryEntity(name = name))
            _effect.send(SettingsEffect.ShowToast("已添加"))
        }
    }

    private fun editCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            _effect.send(SettingsEffect.ShowToast("已更新"))
        }
    }

    private fun deleteCategory(category: CategoryEntity) {
        if (category.isDefault) {
            viewModelScope.launch { _effect.send(SettingsEffect.ShowToast("内置类目不可删除")) }
            return
        }
        viewModelScope.launch {
            // 关联记录的 categoryId 改为"其他"
            val otherCategory = categoryRepository.getCategoryByName("其他")
            if (otherCategory != null) {
                recordRepository.updateRecordsCategory(category.id, otherCategory.id)
            }
            categoryRepository.deleteCategory(category)
            _effect.send(SettingsEffect.CategoryDeleted)
        }
    }

    private fun saveBudget(monthlyBudget: Double, thresholdPercent: Int) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                monthlyBudget = monthlyBudget,
                thresholdPercent = thresholdPercent,
                yearMonth = currentYearMonth
            )
            budgetRepository.saveBudget(budget)
            _effect.send(SettingsEffect.ShowToast("预算已保存"))
        }
    }

    // 导出全部记录为 CSV
    fun handleExport(context: Context) {
        viewModelScope.launch {
            val exportService = ExportService(context)
            recordRepository.getAllRecords().first().let { records ->
                val categories = _state.value.categories
                val uri = exportService.exportToCSV(records, categories)
                if (uri != null) {
                    val intent = exportService.shareCSV(uri)
                    context.startActivity(Intent.createChooser(intent, "导出消费记录"))
                } else {
                    _effect.send(SettingsEffect.ShowToast("暂无记录可导出"))
                }
            }
        }
    }
}
