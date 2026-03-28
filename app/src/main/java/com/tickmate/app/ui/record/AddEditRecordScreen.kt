package com.tickmate.app.ui.record

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.model.RecordListIntent
import com.tickmate.app.ui.components.GradientButton
import com.tickmate.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecordScreen(
    recordId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: RecordListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var note by remember { mutableStateOf("") }
    var isEdit by remember { mutableStateOf(false) }

    // 编辑模式：加载已有记录
    LaunchedEffect(recordId) {
        if (recordId != null && recordId > 0) {
            // 从 ViewModel 的 records 中查找
            val record = state.records.find { it.id == recordId }
            if (record != null) {
                merchant = record.merchant
                amount = "%.2f".format(record.amount)
                date = record.date
                selectedCategoryId = record.categoryId
                note = record.note ?: ""
                isEdit = true
            }
        }
    }

    // 设置默认类目
    LaunchedEffect(state.categories) {
        if (selectedCategoryId == 0L && state.categories.isNotEmpty()) {
            selectedCategoryId = state.categories.first().id
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑记录" else "手动录入") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 商户名
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("商户名 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = TechBlue,
                    cursorColor = TechBlue
                )
            )

            // 金额
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("¥ ") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = TechBlue,
                    cursorColor = TechBlue
                )
            )

            // 日期
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("日期 (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = TechBlue,
                    cursorColor = TechBlue
                )
            )

            // 类目选择
            Text("消费类目", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TechBlue.copy(alpha = 0.2f),
                            selectedLabelColor = TechBlue
                        )
                    )
                }
            }

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechBlue,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = TechBlue,
                    cursorColor = TechBlue
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 保存按钮
            GradientButton(
                text = if (isEdit) "保存修改" else "保存记录",
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (merchant.isBlank()) return@GradientButton
                    if (amountValue == null || amountValue <= 0) return@GradientButton

                    // 校验日期格式
                    val validDate = try {
                        if (date.isNotBlank()) {
                            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            date
                        } else {
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }
                    } catch (e: Exception) {
                        // 日期格式不正确，使用今天
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }

                    val record = RecordEntity(
                        id = if (isEdit && recordId != null) recordId else 0,
                        merchant = merchant.trim().take(50),
                        amount = amountValue,
                        date = validDate,
                        categoryId = if (selectedCategoryId > 0) selectedCategoryId
                            else state.categories.firstOrNull()?.id ?: 1,
                        note = note.trim().take(200).ifBlank { null }
                    )
                    viewModel.handleIntent(RecordListIntent.SaveRecord(record))
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = merchant.isNotBlank() && amount.toDoubleOrNull() != null && selectedCategoryId > 0
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
