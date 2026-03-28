package com.tickmate.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.theme.*
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onNavigateBack: () -> Unit,
    recordRepository: RecordRepository = koinInject(),
    categoryRepository: CategoryRepository = koinInject()
) {
    val currentYearMonth = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }
    val daysInMonth = remember {
        LocalDate.now().lengthOfMonth()
    }

    val monthTotal by recordRepository.getMonthTotal(currentYearMonth).collectAsState(initial = 0.0)
    val recordCount by recordRepository.getMonthRecordCount(currentYearMonth).collectAsState(initial = 0)
    val maxAmount by recordRepository.getMonthMaxAmount(currentYearMonth).collectAsState(initial = null)
    val activeDays by recordRepository.getMonthActiveDays(currentYearMonth).collectAsState(initial = 0)
    val categoryTotals by recordRepository.getCategoryTotals(currentYearMonth).collectAsState(initial = emptyList())
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())

    val dailyAvg = if (activeDays > 0) monthTotal / activeDays else 0.0
    val categoriesMap = categories.associateBy { it.id }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("${currentYearMonth.substring(5)}月 消费报表") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总支出大卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(TechBlue.copy(alpha = 0.15f), CardBackground)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text("本月总支出", color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¥${"%.2f".format(monthTotal)}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 统计指标网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "消费笔数",
                    value = "$recordCount 笔",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "日均消费",
                    value = "¥${"%.0f".format(dailyAvg)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "最大单笔",
                    value = "¥${"%.2f".format(maxAmount ?: 0.0)}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "消费天数",
                    value = "$activeDays / $daysInMonth 天",
                    modifier = Modifier.weight(1f)
                )
            }

            // 类目排行
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("类目排行", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                val totalAmount = categoryTotals.sumOf { it.total }
                val sortedCategories = categoryTotals.sortedByDescending { it.total }

                if (sortedCategories.isEmpty()) {
                    Text("暂无数据", color = TextTertiary)
                } else {
                    sortedCategories.forEachIndexed { index, ct ->
                        val category = categoriesMap[ct.categoryId]
                        val percent = if (totalAmount > 0) (ct.total / totalAmount * 100) else 0.0
                        val color = ChartColors[index % ChartColors.size]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 排名
                            Text(
                                text = "#${index + 1}",
                                color = if (index < 3) TechBlue else TextTertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category?.name ?: "其他",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "¥${"%.2f".format(ct.total)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${"%.1f".format(percent)}%",
                                color = color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(50.dp)
                            )
                        }
                        // 进度条
                        LinearProgressIndicator(
                            progress = (percent / 100f).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = SurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    GlowCard(modifier = modifier) {
        Text(title, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
