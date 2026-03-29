package com.tickmate.app.ui.record

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.model.RecordListEffect
import com.tickmate.app.model.RecordListIntent
import com.tickmate.app.ui.components.*
import com.tickmate.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordListScreen(
    onNavigateToAddRecord: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToEditRecord: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: RecordListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<RecordEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is RecordListEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is RecordListEffect.RecordSaved -> snackbarHostState.showSnackbar("已保存")
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column {
                SmallFloatingActionButton(
                    onClick = onNavigateToCamera,
                    containerColor = NeonPurple,
                    contentColor = TextPrimary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "拍照识别")
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = onNavigateToAddRecord,
                    containerColor = TechBlue,
                    contentColor = TextPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "手动录入")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 筛选栏
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "消费记录",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 月份筛选
                val months = remember {
                    val today = LocalDate.now()
                    (0..11).map {
                        today.minusMonths(it.toLong())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedMonth == null,
                            onClick = { viewModel.handleIntent(RecordListIntent.FilterByMonth(null)) },
                            label = { Text("全部") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TechBlue.copy(alpha = 0.2f),
                                selectedLabelColor = TechBlue
                            )
                        )
                    }
                    items(months) { month ->
                        FilterChip(
                            selected = state.selectedMonth == month,
                            onClick = { viewModel.handleIntent(RecordListIntent.FilterByMonth(month)) },
                            label = { Text(month.substring(5) + "月") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TechBlue.copy(alpha = 0.2f),
                                selectedLabelColor = TechBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 类目筛选
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.handleIntent(RecordListIntent.FilterByCategory(null)) },
                            label = { Text("全部类目") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple.copy(alpha = 0.2f),
                                selectedLabelColor = NeonPurple
                            )
                        )
                    }
                    items(state.categories) { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.handleIntent(RecordListIntent.FilterByCategory(category.id)) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple.copy(alpha = 0.2f),
                                selectedLabelColor = NeonPurple
                            )
                        )
                    }
                }
            }

            // 记录列表
            if (state.records.isEmpty() && !state.isLoading) {
                EmptyState(message = "暂无消费记录", icon = Icons.Default.Receipt)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(state.records, key = { it.id }) { record ->
                        val categoryName = state.categories
                            .find { it.id == record.categoryId }?.name ?: "其他"

                        // 滑动删除
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    showDeleteDialog = record
                                }
                                false // 不自动消失，等用户确认弹窗
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                // 滑动背景：红色 + 删除图标
                                val color = when (dismissState.targetValue) {
                                    DismissValue.DismissedToStart -> WarningRed.copy(alpha = 0.3f)
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = WarningRed
                                    )
                                }
                            },
                            dismissContent = {
                                ExpenseCard(
                                    merchant = record.merchant,
                                    amount = record.amount,
                                    date = record.date,
                                    categoryName = categoryName,
                                    note = record.note,
                                    onClick = { onNavigateToDetail(record.id) },
                                    onDelete = { showDeleteDialog = record }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    showDeleteDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${record.merchant}」的消费记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(RecordListIntent.DeleteRecord(record))
                    showDeleteDialog = null
                }) {
                    Text("删除", color = WarningRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            },
            containerColor = CardBackground
        )
    }
}
