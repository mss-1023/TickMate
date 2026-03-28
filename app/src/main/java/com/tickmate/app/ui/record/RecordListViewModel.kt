package com.tickmate.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.model.RecordListEffect
import com.tickmate.app.model.RecordListIntent
import com.tickmate.app.model.RecordListState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecordListViewModel(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordListState())
    val state: StateFlow<RecordListState> = _state.asStateFlow()

    private val _effect = Channel<RecordListEffect>(Channel.BUFFERED)
    val effect: Flow<RecordListEffect> = _effect.receiveAsFlow()

    private var recordsJob: Job? = null

    init {
        handleIntent(RecordListIntent.LoadRecords)
        loadCategories()
    }

    fun handleIntent(intent: RecordListIntent) {
        when (intent) {
            is RecordListIntent.LoadRecords -> loadRecords()
            is RecordListIntent.FilterByMonth -> {
                _state.update { it.copy(selectedMonth = intent.yearMonth) }
                loadRecords()
            }
            is RecordListIntent.FilterByCategory -> {
                _state.update { it.copy(selectedCategoryId = intent.categoryId) }
                loadRecords()
            }
            is RecordListIntent.DeleteRecord -> deleteRecord(intent.record)
            is RecordListIntent.SaveRecord -> saveRecord(intent.record)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadRecords() {
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            val month = _state.value.selectedMonth
            val categoryId = _state.value.selectedCategoryId

            val flow = when {
                month != null && categoryId != null ->
                    recordRepository.getRecordsByMonthAndCategory(month, categoryId)
                month != null ->
                    recordRepository.getRecordsByMonth(month)
                categoryId != null ->
                    recordRepository.getRecordsByCategory(categoryId)
                else ->
                    recordRepository.getAllRecords()
            }

            flow.collect { records ->
                _state.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    private fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch {
            recordRepository.deleteRecord(record)
            _effect.send(RecordListEffect.ShowToast("已删除"))
        }
    }

    private fun saveRecord(record: RecordEntity) {
        viewModelScope.launch {
            if (record.id == 0L) {
                recordRepository.insertRecord(record)
            } else {
                recordRepository.updateRecord(record)
            }
            _effect.send(RecordListEffect.RecordSaved)
        }
    }
}
