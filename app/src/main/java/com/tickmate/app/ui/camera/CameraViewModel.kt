package com.tickmate.app.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.model.CameraEffect
import com.tickmate.app.model.CameraIntent
import com.tickmate.app.model.CameraState
import com.tickmate.app.service.CategoryMatcher
import com.tickmate.app.service.OCRResult
import com.tickmate.app.service.OCRService
import com.tickmate.app.service.OnlineOCRService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CameraViewModel(
    private val ocrService: OCRService,
    private val categoryMatcher: CategoryMatcher,
    private val recordRepository: RecordRepository,
    private val onlineOCRService: OnlineOCRService? = null
) : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val _effect = Channel<CameraEffect>(Channel.BUFFERED)
    val effect: Flow<CameraEffect> = _effect.receiveAsFlow()

    // 当前使用的 OCR 引擎
    val useOnlineOCR: Boolean
        get() = onlineOCRService?.isConfigured() == true

    fun handleIntent(intent: CameraIntent) {
        when (intent) {
            is CameraIntent.ProcessImage -> processImage(intent.imageUri)
            is CameraIntent.UpdateMerchant -> _state.update { it.copy(merchant = intent.merchant) }
            is CameraIntent.UpdateAmount -> _state.update { it.copy(amount = intent.amount) }
            is CameraIntent.UpdateDate -> _state.update { it.copy(date = intent.date) }
            is CameraIntent.UpdateCategory -> _state.update { it.copy(categoryId = intent.categoryId) }
            is CameraIntent.UpdateNote -> _state.update { it.copy(note = intent.note) }
            is CameraIntent.SaveRecord -> saveRecord()
            is CameraIntent.Reset -> _state.value = CameraState()
        }
    }

    private fun processImage(imageUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, imageUri = imageUri, error = null) }
            try {
                // 优先百度云 OCR，失败降级 ML Kit
                var engineUsed = ""
                var fallbackReason = ""
                val result: OCRResult = withContext(Dispatchers.IO) {
                    if (onlineOCRService?.isConfigured() == true) {
                        try {
                            engineUsed = "百度云 OCR"
                            onlineOCRService.recognizeReceipt(imageUri)
                        } catch (e: Exception) {
                            fallbackReason = "（百度云失败: ${e.message}，已降级）"
                            engineUsed = "ML Kit 离线$fallbackReason"
                            ocrService.recognizeReceipt(imageUri)
                        }
                    } else {
                        engineUsed = "ML Kit 离线（未配置在线 OCR）"
                        ocrService.recognizeReceipt(imageUri)
                    }
                }

                // 智能匹配类目
                val itemNames = result.itemDetails.map { it.name }
                val category = categoryMatcher.matchCategoryEntity(
                    merchantName = result.merchant,
                    rawText = result.rawText,
                    itemNames = itemNames
                )
                val fallbackCategoryId = category?.id
                    ?: categoryMatcher.matchCategoryEntity("其他")?.id
                    ?: 1

                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                _state.update {
                    it.copy(
                        isProcessing = false,
                        merchant = result.merchant.ifBlank { "未识别到商户名" },
                        amount = result.amount?.let { a -> "%.2f".format(a) } ?: "0.00",
                        date = result.date ?: today,
                        categoryId = fallbackCategoryId,
                        rawText = result.rawText,
                        itemDetails = result.itemDetails,
                        ocrEngine = engineUsed,
                        recognitionDone = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = "识别失败：${e.message}",
                        recognitionDone = false
                    )
                }
                _effect.send(CameraEffect.RecognitionFailed)
            }
        }
    }

    private fun saveRecord() {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull()
        if (s.merchant.isBlank()) {
            viewModelScope.launch { _effect.send(CameraEffect.ShowToast("商户名不能为空")) }
            return
        }
        if (amount == null || amount <= 0) {
            viewModelScope.launch { _effect.send(CameraEffect.ShowToast("请输入有效金额")) }
            return
        }

        viewModelScope.launch {
            val record = RecordEntity(
                merchant = s.merchant,
                amount = amount,
                date = s.date.ifBlank {
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                },
                categoryId = if (s.categoryId > 0) s.categoryId else {
                    categoryMatcher.matchCategoryEntity("未知")?.id ?: 1
                },
                note = s.note.ifBlank { null },
                imageUri = s.imageUri?.toString()
            )
            recordRepository.insertRecord(record)
            _effect.send(CameraEffect.RecordSaved)
            _state.value = CameraState()
        }
    }
}
