package com.tickmate.app.model

import android.net.Uri
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.service.ItemDetail

// 拍照识别 MVI 契约
sealed class CameraIntent {
    data class ProcessImage(val imageUri: Uri) : CameraIntent()
    data class UpdateMerchant(val merchant: String) : CameraIntent()
    data class UpdateAmount(val amount: String) : CameraIntent()
    data class UpdateDate(val date: String) : CameraIntent()
    data class UpdateCategory(val categoryId: Long) : CameraIntent()
    data class UpdateNote(val note: String) : CameraIntent()
    object SaveRecord : CameraIntent()
    object Reset : CameraIntent()
}

data class CameraState(
    val isProcessing: Boolean = false,
    val imageUri: Uri? = null,
    val merchant: String = "",
    val amount: String = "",
    val date: String = "",
    val categoryId: Long = 0,
    val note: String = "",
    val rawText: String = "",
    val itemDetails: List<ItemDetail> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val recognitionDone: Boolean = false,
    val ocrEngine: String = "",     // 使用的 OCR 引擎名称
    val error: String? = null
)

sealed class CameraEffect {
    data class ShowToast(val message: String) : CameraEffect()
    object RecordSaved : CameraEffect()
    object RecognitionFailed : CameraEffect()
}
