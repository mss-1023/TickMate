package com.tickmate.app.ui.record

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.model.RecordListEffect
import com.tickmate.app.model.RecordListIntent
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: RecordListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val record = state.records.find { it.id == recordId }
    val categoryName = record?.let { r ->
        state.categories.find { it.id == r.categoryId }?.name ?: "其他"
    } ?: ""
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is RecordListEffect.ShowToast -> { /* handled */ }
                is RecordListEffect.RecordSaved -> { /* handled */ }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("消费详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (record != null) {
                        IconButton(onClick = { onNavigateToEdit(recordId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TechBlue)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = WarningRed)
                        }
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
        if (record == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("记录不存在", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 金额大字
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Text("支出金额", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥${"%.2f".format(record.amount)}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = WarningOrange
                    )
                }

                // 详细信息
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    DetailRow("商户", record.merchant)
                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                    DetailRow("类目", categoryName)
                    Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                    DetailRow("日期", record.date)
                    if (!record.note.isNullOrBlank()) {
                        Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                        DetailRow("备注", record.note)
                    }
                }

                // 原始图片（如果有）
                if (!record.imageUri.isNullOrBlank()) {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Text("票据图片", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = record.imageUri,
                            contentDescription = "票据图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }
    }

    // 删除确认
    if (showDeleteDialog && record != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${record.merchant}」的消费记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(RecordListIntent.DeleteRecord(record))
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("删除", color = WarningRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
    }
}
