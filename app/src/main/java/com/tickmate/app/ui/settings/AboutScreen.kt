package com.tickmate.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("关于") },
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App 图标
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(TechBlue.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TechBlue
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TickMate",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TechBlue
            )
            Text(
                text = "智能票据管家",
                color = TextSecondary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "v1.0.0",
                color = TextTertiary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TickMate 是一款智能消费记录管理工具，支持手动录入与拍照识别票据信息，" +
                        "帮助你轻松追踪每一笔消费，配合直观的统计图表和预算提醒，" +
                        "让你成为自己财务的主人。",
                    color = TextSecondary,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlowCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow("技术栈", "Kotlin + Jetpack Compose")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("架构", "MVI + Koin + Room")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("OCR", "Google ML Kit (离线)")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("最低版本", "Android 8.0 (API 26)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Made with Kotlin & Jetpack Compose",
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = TextPrimary, fontSize = 14.sp)
    }
}
