package com.tickmate.app.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.data.db.entity.RecordEntity
import com.tickmate.app.model.HomeEffect
import com.tickmate.app.model.RecordListIntent
import com.tickmate.app.ui.components.*
import com.tickmate.app.ui.record.RecordListViewModel
import com.tickmate.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddRecord: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToMonthlyReport: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
    recordListViewModel: RecordListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val recordState by recordListViewModel.state.collectAsState()

    // 快速记账状态
    var showQuickAdd by remember { mutableStateOf(false) }
    var quickAmount by remember { mutableStateOf("") }
    var quickCategoryId by remember { mutableLongStateOf(0L) }
    var quickMerchant by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HomeEffect.BudgetAlert -> { /* 通知已在 ViewModel 中处理 */ }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
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
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TechBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                // 标题栏 + 搜索按钮
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TickMate",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechBlue
                            )
                            Text(
                                text = "智能票据管家",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary)
                        }
                    }
                }

                // 预算警告
                if (state.showBudgetWarning) {
                    item {
                        BudgetWarningBanner(percent = state.budgetPercent)
                    }
                }

                // 当月支出概览卡片
                item {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "本月支出",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¥${"%.2f".format(state.monthTotal)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                        // 预算进度条
                        val budget = state.budget
                        if (budget != null && budget.monthlyBudget > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val progress = (state.monthTotal / budget.monthlyBudget)
                                .coerceIn(0.0, 1.0).toFloat()
                            val progressColor = when {
                                state.budgetPercent >= 100 -> WarningRed
                                state.budgetPercent >= 80 -> WarningOrange
                                else -> TechBlue
                            }
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = progressColor,
                                trackColor = SurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "预算 ¥${"%.0f".format(budget.monthlyBudget)}  |  ${state.budgetPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // 查看月度报表入口
                        TextButton(
                            onClick = onNavigateToMonthlyReport,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("查看月度报表", color = TechBlue, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TechBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // 快速记账入口
                item {
                    GlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showQuickAdd = !showQuickAdd }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(TechBlue, NeonPurple))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("快速记账", fontWeight = FontWeight.SemiBold)
                            }
                            Icon(
                                if (showQuickAdd) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextTertiary
                            )
                        }

                        // 展开的快速记账表单
                        AnimatedVisibility(visible = showQuickAdd) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                OutlinedTextField(
                                    value = quickAmount,
                                    onValueChange = { quickAmount = it },
                                    label = { Text("金额") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    prefix = { Text("¥ ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TechBlue,
                                        unfocusedBorderColor = DividerColor,
                                        cursorColor = TechBlue
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = quickMerchant,
                                    onValueChange = { quickMerchant = it },
                                    label = { Text("商户名(可选)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TechBlue,
                                        unfocusedBorderColor = DividerColor,
                                        cursorColor = TechBlue
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // 类目快选
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(recordState.categories) { category ->
                                        FilterChip(
                                            selected = quickCategoryId == category.id,
                                            onClick = { quickCategoryId = category.id },
                                            label = { Text(category.name, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TechBlue.copy(alpha = 0.2f),
                                                selectedLabelColor = TechBlue
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                GradientButton(
                                    text = "记一笔",
                                    onClick = {
                                        val amount = quickAmount.toDoubleOrNull() ?: return@GradientButton
                                        if (amount <= 0) return@GradientButton
                                        val record = RecordEntity(
                                            merchant = quickMerchant.ifBlank { "快速记账" },
                                            amount = amount,
                                            date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                            categoryId = if (quickCategoryId > 0) quickCategoryId
                                                else recordState.categories.firstOrNull()?.id ?: 1
                                        )
                                        recordListViewModel.handleIntent(RecordListIntent.SaveRecord(record))
                                        quickAmount = ""
                                        quickMerchant = ""
                                        showQuickAdd = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = quickAmount.toDoubleOrNull() != null
                                )
                            }
                        }
                    }
                }

                // 最近记录
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "最近记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }

                if (state.recentRecords.isEmpty()) {
                    item {
                        EmptyState(
                            message = "暂无消费记录\n点击 + 开始记录",
                            icon = Icons.Default.Receipt
                        )
                    }
                } else {
                    items(state.recentRecords, key = { it.id }) { record ->
                        val categoryName = recordState.categories
                            .find { it.id == record.categoryId }?.name ?: "其他"
                        ExpenseCard(
                            merchant = record.merchant,
                            amount = record.amount,
                            date = record.date,
                            categoryName = categoryName,
                            note = record.note
                        )
                    }
                }
            }
        }
    }
}
