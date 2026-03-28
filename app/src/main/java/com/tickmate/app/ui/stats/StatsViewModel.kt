package com.tickmate.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.model.CategoryAmount
import com.tickmate.app.model.MonthAmount
import com.tickmate.app.model.StatsIntent
import com.tickmate.app.model.StatsState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsViewModel(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    private val currentYearMonth: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    // 缓存类目信息
    private var categoriesMap: Map<Long, CategoryEntity> = emptyMap()

    init {
        handleIntent(StatsIntent.LoadStats)
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            is StatsIntent.LoadStats -> loadStats()
        }
    }

    private fun loadStats() {
        _state.update { it.copy(currentMonth = currentYearMonth) }

        // 先加载类目映射
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                categoriesMap = categories.associateBy { it.id }
            }
        }

        // 折线图：近12个月月度支出
        viewModelScope.launch {
            recordRepository.getMonthlyTotals().collect { totals ->
                // 补全12个月数据
                val monthlyData = buildLast12Months(totals.associate { it.month to it.total })
                _state.update { it.copy(monthlyData = monthlyData, isLoading = false) }
            }
        }

        // 饼图：当月类目占比
        viewModelScope.launch {
            recordRepository.getCategoryTotals(currentYearMonth).collect { totals ->
                val totalAmount = totals.sumOf { it.total }
                val categoryData = totals.mapNotNull { ct ->
                    val category = categoriesMap[ct.categoryId] ?: return@mapNotNull null
                    CategoryAmount(
                        category = category,
                        amount = ct.total,
                        percent = if (totalAmount > 0) (ct.total / totalAmount * 100).toFloat() else 0f
                    )
                }.sortedByDescending { it.amount }
                _state.update { it.copy(categoryData = categoryData) }
            }
        }
    }

    // 构建近12个月的数据，缺失月份补0
    private fun buildLast12Months(dataMap: Map<String, Double>): List<MonthAmount> {
        val today = LocalDate.now()
        return (11 downTo 0).map { i ->
            val month = today.minusMonths(i.toLong())
                .format(DateTimeFormatter.ofPattern("yyyy-MM"))
            MonthAmount(month = month, amount = dataMap[month] ?: 0.0)
        }
    }
}
