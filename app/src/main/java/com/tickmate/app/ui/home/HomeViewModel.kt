package com.tickmate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tickmate.app.data.repository.BudgetRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.model.HomeEffect
import com.tickmate.app.model.HomeIntent
import com.tickmate.app.model.HomeState
import com.tickmate.app.service.NotificationService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeViewModel(
    private val recordRepository: RecordRepository,
    private val budgetRepository: BudgetRepository,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    private val currentYearMonth: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    init {
        handleIntent(HomeIntent.LoadData)
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
        }
    }

    private fun loadData() {
        // 监听当月总支出
        viewModelScope.launch {
            recordRepository.getMonthTotal(currentYearMonth).collect { total ->
                _state.update { it.copy(monthTotal = total, isLoading = false) }
                checkBudget(total)
            }
        }

        // 监听预算设置
        viewModelScope.launch {
            budgetRepository.getBudget(currentYearMonth).collect { budget ->
                _state.update { state ->
                    val percent = if (budget != null && budget.monthlyBudget > 0) {
                        (state.monthTotal / budget.monthlyBudget * 100).toInt()
                    } else 0
                    state.copy(
                        budget = budget,
                        budgetPercent = percent,
                        showBudgetWarning = budget != null &&
                            budget.monthlyBudget > 0 &&
                            percent >= (budget.thresholdPercent)
                    )
                }
            }
        }

        // 监听最近记录
        viewModelScope.launch {
            recordRepository.getRecentRecords(5).collect { records ->
                _state.update { it.copy(recentRecords = records) }
            }
        }
    }

    private fun checkBudget(total: Double) {
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetSync(currentYearMonth)
            if (budget != null && budget.monthlyBudget > 0) {
                val percent = (total / budget.monthlyBudget * 100).toInt()
                if (percent >= budget.thresholdPercent) {
                    _effect.send(HomeEffect.BudgetAlert(percent))
                    notificationService.sendBudgetAlert(total, budget.monthlyBudget, percent)
                }
            }
        }
    }
}
