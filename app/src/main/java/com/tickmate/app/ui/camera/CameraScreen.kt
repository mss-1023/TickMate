package com.tickmate.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.model.CameraEffect
import com.tickmate.app.model.CameraIntent
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.components.GradientButton
import com.tickmate.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = koinViewModel(),
    categoryRepository: CategoryRepository = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 加载类目列表
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    fun createPhotoUri(): Uri {
        val photoFile = File(context.cacheDir, "photos").apply { mkdirs() }
            .let { File(it, "photo_${System.currentTimeMillis()}.jpg") }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            viewModel.handleIntent(CameraIntent.ProcessImage(photoUri!!))
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.handleIntent(CameraIntent.ProcessImage(it)) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createPhotoUri()
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("需要相机权限才能拍照识别")
            }
        }
    }

    fun onTakePhoto() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val uri = createPhotoUri()
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CameraEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is CameraEffect.RecordSaved -> {
                    snackbarHostState.showSnackbar("记录已保存")
                    onNavigateBack()
                }
                is CameraEffect.RecognitionFailed -> {
                    snackbarHostState.showSnackbar("识别失败，请重试或手动录入")
                }
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("拍照识别") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ========== 未识别状态：引导页面 ==========
            if (!state.recognitionDone && !state.isProcessing) {

                Spacer(modifier = Modifier.height(8.dp))

                // 顶部图标区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(TechBlue.copy(alpha = 0.12f), NeonPurple.copy(alpha = 0.06f))
                            )
                        )
                        .border(1.dp, GlowBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(TechBlue.copy(alpha = 0.3f), TechBlue.copy(alpha = 0.05f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = TechBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "拍摄或选择票据图片",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "AI 自动识别商户、金额、日期",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 两个大按钮：拍照 / 相册
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        icon = Icons.Default.CameraAlt,
                        title = "拍照",
                        subtitle = "打开相机拍摄票据",
                        gradientColors = listOf(TechBlue, TechBlue.copy(alpha = 0.7f)),
                        onClick = { onTakePhoto() },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        icon = Icons.Default.Photo,
                        title = "相册",
                        subtitle = "从相册选择图片",
                        gradientColors = listOf(NeonPurple, NeonPurple.copy(alpha = 0.7f)),
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 使用提示
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "识别小贴士",
                        style = MaterialTheme.typography.titleMedium,
                        color = TechBlue
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TipRow(Icons.Default.LightMode, "光线充足", "确保票据在明亮环境下拍摄")
                    Spacer(modifier = Modifier.height(8.dp))
                    TipRow(Icons.Default.CenterFocusStrong, "对焦清晰", "将票据平放，镜头对准后再拍摄")
                    Spacer(modifier = Modifier.height(8.dp))
                    TipRow(Icons.Default.Crop, "完整拍摄", "确保票据上的文字完整出现在画面中")
                    Spacer(modifier = Modifier.height(8.dp))
                    TipRow(Icons.Default.Edit, "可修改", "识别结果可手动修正，确认后保存")
                }

                // 支持的票据类型
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "支持的票据类型",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReceiptTypeChip("购物小票")
                        ReceiptTypeChip("餐饮账单")
                        ReceiptTypeChip("发票")
                        ReceiptTypeChip("外卖单")
                    }
                }
            }

            // ========== 识别中 ==========
            if (state.isProcessing) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, GlowBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = TechBlue,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "正在识别票据信息...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "AI 正在分析图片中的文字",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ========== 识别完成：确认页 ==========
            if (state.recognitionDone) {
                // 图片预览
                state.imageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "票据图片",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 半透明遮罩标签
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(
                                    SuccessGreen.copy(alpha = 0.85f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("识别成功", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 引擎信息
                if (state.ocrEngine.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isBaidu = state.ocrEngine.startsWith("百度")
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isBaidu) SuccessGreen else WarningOrange)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "引擎: ${state.ocrEngine}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }

                // 识别结果表单
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = TechBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("识别结果", style = MaterialTheme.typography.titleMedium, color = TechBlue)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("可修改", fontSize = 12.sp, color = TextTertiary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 商户名
                    OutlinedTextField(
                        value = state.merchant,
                        onValueChange = { viewModel.handleIntent(CameraIntent.UpdateMerchant(it)) },
                        label = { Text("商户名") },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 金额
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.handleIntent(CameraIntent.UpdateAmount(it)) },
                        label = { Text("金额") },
                        leadingIcon = { Text("¥", color = WarningOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 日期
                    OutlinedTextField(
                        value = state.date,
                        onValueChange = { viewModel.handleIntent(CameraIntent.UpdateDate(it)) },
                        label = { Text("日期") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 类目选择
                    Text("消费类目", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { category ->
                            FilterChip(
                                selected = state.categoryId == category.id,
                                onClick = { viewModel.handleIntent(CameraIntent.UpdateCategory(category.id)) },
                                label = { Text(category.name, fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TechBlue.copy(alpha = 0.2f),
                                    selectedLabelColor = TechBlue
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 备注
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.handleIntent(CameraIntent.UpdateNote(it)) },
                        label = { Text("备注（可选）") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )
                }

                // 商品明细（P3）
                if (state.itemDetails.isNotEmpty()) {
                    GlowCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("商品明细", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 表头
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        ) {
                            Text("商品", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("数量", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                            Text("金额", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                        }
                        Divider(color = DividerColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        state.itemDetails.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.name, color = TextPrimary, modifier = Modifier.weight(1f))
                                Text("x${item.quantity}", color = TextSecondary, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                                Text("¥${"%.2f".format(item.price)}", color = WarningOrange, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }

                // 原始识别文字
                var showRawText by remember { mutableStateOf(false) }
                TextButton(onClick = { showRawText = !showRawText }) {
                    Icon(
                        if (showRawText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (showRawText) "隐藏原始文字" else "查看原始识别文字",
                        color = TextTertiary,
                        fontSize = 13.sp
                    )
                }
                if (showRawText) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Text(
                            state.rawText,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.handleIntent(CameraIntent.Reset) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新识别")
                    }
                    GradientButton(
                        text = "确认保存",
                        onClick = { viewModel.handleIntent(CameraIntent.SaveRecord) },
                        modifier = Modifier.weight(1f),
                        enabled = state.merchant.isNotBlank() && state.amount.toDoubleOrNull() != null
                    )
                }
            }

            // 错误信息
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = WarningRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = WarningRed, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// 操作卡片（拍照/相册）
@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(Modifier.clickable { onClick() }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.15f) }
                    )
                )
                .border(1.dp, gradientColors[0].copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(gradientColors[0].copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = gradientColors[0], modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// 提示行
@Composable
private fun TipRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = TechBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(description, fontSize = 12.sp, color = TextTertiary)
        }
    }
}

// 票据类型标签
@Composable
private fun ReceiptTypeChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}
