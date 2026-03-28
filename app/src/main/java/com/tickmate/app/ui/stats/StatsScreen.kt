package com.tickmate.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.ui.components.EmptyState
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "消费统计",
            style = MaterialTheme.typography.headlineMedium
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TechBlue)
            }
        } else if (state.monthlyData.all { it.amount == 0.0 } && state.categoryData.isEmpty()) {
            EmptyState(message = "暂无消费数据", icon = Icons.Default.BarChart)
        } else {
            // 折线图：近12个月消费趋势
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "月度消费趋势",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (state.monthlyData.any { it.amount > 0 }) {
                    // 自绘折线图
                    val maxAmount = state.monthlyData.maxOf { it.amount }.coerceAtLeast(1.0)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val padding = 8.dp.toPx()
                        val chartWidth = width - padding * 2
                        val chartHeight = height - padding * 2
                        val points = state.monthlyData.mapIndexed { index, data ->
                            val x = padding + chartWidth * index / (state.monthlyData.size - 1).coerceAtLeast(1)
                            val y = padding + chartHeight * (1 - data.amount / maxAmount).toFloat()
                            Offset(x, y)
                        }

                        // 绘制网格线
                        for (i in 0..4) {
                            val y = padding + chartHeight * i / 4
                            drawLine(
                                color = DividerColor,
                                start = Offset(padding, y),
                                end = Offset(width - padding, y),
                                strokeWidth = 0.5f
                            )
                        }

                        // 绘制折线
                        if (points.size >= 2) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = TechBlue,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // 绘制数据点
                        points.forEach { point ->
                            drawCircle(
                                color = TechBlue,
                                radius = 3.dp.toPx(),
                                center = point
                            )
                        }
                    }

                    // 月份标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        state.monthlyData.forEachIndexed { index, data ->
                            if (index % 3 == 0 || index == state.monthlyData.size - 1) {
                                Text(
                                    text = if (data.month.length >= 7) data.month.substring(5) + "月" else data.month,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextTertiary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                } else {
                    Text("暂无数据", color = TextTertiary)
                }
            }

            // 饼图：当月类目占比
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                val monthLabel = if (state.currentMonth.length >= 7)
                    state.currentMonth.substring(5) + "月"
                else state.currentMonth
                Text(
                    text = "$monthLabel 类目占比",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (state.categoryData.isNotEmpty()) {
                    // 自绘饼图
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier.size(160.dp)
                        ) {
                            var startAngle = -90f
                            state.categoryData.forEachIndexed { index, data ->
                                val sweepAngle = data.percent / 100f * 360f
                                val color = ChartColors[index % ChartColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true,
                                    size = Size(size.width, size.height)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 图例列表
                    state.categoryData.forEachIndexed { index, data ->
                        val color = ChartColors[index % ChartColors.size]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = data.category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "¥${"%.2f".format(data.amount)}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${"%.1f".format(data.percent)}%",
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = data.percent / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = SurfaceVariant,
                        )
                    }
                } else {
                    Text("当月暂无消费数据", color = TextTertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
