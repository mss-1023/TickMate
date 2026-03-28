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

    // 记录是否已发送过预算提醒（本次 ViewModel 生命周期内）
    private var budgetAlertSent = false

    init {
        handleIntent(HomeIntent.LoadData)
    }

    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadData -> loadData()
        }
    }

    private fun loadData() {
        // 用 combine 同时监听月支出和预算，任何一个变化都重新计算
        viewModelScope.launch {
            combine(
                recordRepository.getMonthTotal(currentYearMonth),
                budgetRepository.getBudget(currentYearMonth)
            ) { total, budget ->
                val percent = if (budget != null && budget.monthlyBudget > 0) {
                    (total / budget.monthlyBudget * 100).toInt()
                } else 0
                val showWarning = budget != null &&
                    budget.monthlyBudget > 0 &&
                    percent >= budget.thresholdPercent

                // 发送预算提醒通知（每次 ViewModel 生命周期内只发一次）
                if (showWarning && !budgetAlertSent) {
                    budgetAlertSent = true
                    _effect.send(HomeEffect.BudgetAlert(percent))
                    notificationService.sendBudgetAlert(total, budget!!.monthlyBudget, percent)
                }

                HomeState(
                    isLoading = false,
                    monthTotal = total,
                    budget = budget,
                    budgetPercent = percent,
                    showBudgetWarning = showWarning,
                    recentRecords = _state.value.recentRecords
                )
            }.collect { newState ->
                _state.value = newState
            }
        }

        // 监听最近记录
        viewModelScope.launch {
            recordRepository.getRecentRecords(5).collect { records ->
                _state.update { it.copy(recentRecords = records) }
            }
        }
    }
}
