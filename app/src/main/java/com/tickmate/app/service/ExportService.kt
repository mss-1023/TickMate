package com.tickmate.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.db.entity.RecordEntity
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExportService(private val context: Context) {

    // 导出记录为 CSV 文件并返回文件 URI
    fun exportToCSV(
        records: List<RecordEntity>,
        categories: List<CategoryEntity>
    ): Uri? {
        if (records.isEmpty()) return null

        val categoriesMap = categories.associateBy { it.id }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val fileName = "TickMate_export_$timestamp.csv"

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, fileName)

        csvFile.bufferedWriter().use { writer ->
            // CSV 头部（带 BOM 以支持 Excel 中文）
            writer.write("\uFEFF")
            writer.write("日期,商户名,金额,类目,备注\n")

            records.forEach { record ->
                val categoryName = categoriesMap[record.categoryId]?.name ?: "其他"
                val note = record.note?.replace(",", "，")?.replace("\n", " ") ?: ""
                val merchant = record.merchant.replace(",", "，")
                writer.write("${record.date},$merchant,${record.amount},$categoryName,$note\n")
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )
    }

    // 分享 CSV 文件
    fun shareCSV(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
