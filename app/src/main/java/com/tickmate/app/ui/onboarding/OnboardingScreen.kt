package com.tickmate.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.ui.components.GradientButton
import com.tickmate.app.ui.theme.*

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Receipt,
        title = "轻松记账",
        description = "3秒快速记账，手动录入或拍照识别\n让每一笔消费都有迹可循"
    ),
    OnboardingPage(
        icon = Icons.Default.CameraAlt,
        title = "智能识别",
        description = "拍照即可识别票据信息\n自动提取商户、金额、日期\n告别繁琐手动录入"
    ),
    OnboardingPage(
        icon = Icons.Default.PieChart,
        title = "洞察消费",
        description = "直观的图表统计与月度报表\n预算提醒助你控制开支\n做自己财务的主人"
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 当前页面内容
        Crossfade(
            targetState = currentPage,
            label = "onboarding"
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        // 底部指示器和按钮
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(onboardingPages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (currentPage == index) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentPage == index) TechBlue else TextTertiary
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (currentPage == onboardingPages.size - 1) {
                GradientButton(
                    text = "开始使用",
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GradientButton(
                    text = "下一页",
                    onClick = { currentPage++ },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onFinish) {
                    Text("跳过", color = TextTertiary)
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TechBlue.copy(alpha = 0.3f),
                            TechBlue.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = TechBlue
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
