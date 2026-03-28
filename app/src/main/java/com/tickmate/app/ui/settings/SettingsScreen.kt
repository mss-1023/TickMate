package com.tickmate.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.model.SettingsEffect
import com.tickmate.app.model.SettingsIntent
import com.tickmate.app.ui.components.GlowCard
import com.tickmate.app.ui.components.GradientButton
import com.tickmate.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // 预算表单状态
    var budgetAmount by remember { mutableStateOf("") }
    var thresholdPercent by remember { mutableStateOf("80") }

    // 初始化预算表单
    LaunchedEffect(state.budget) {
        state.budget?.let {
            budgetAmount = if (it.monthlyBudget > 0) "%.0f".format(it.monthlyBudget) else ""
            thresholdPercent = it.thresholdPercent.toString()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
                is SettingsEffect.CategoryDeleted -> snackbarHostState.showSnackbar("类目已删除")
            }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium
            )

            // ---- 类目管理（可折叠） ----
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryExpanded = !categoryExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("类目管理", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.categories.size} 个类目（${state.categories.count { it.isDefault }} 个内置）",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    Icon(
                        if (categoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                }

                AnimatedVisibility(
                    visible = categoryExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        // 新增按钮
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TechBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("新增类目")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.categories.isEmpty() && state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TechBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }

                        state.categories.forEachIndexed { index, category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (category.isDefault) {
                                    Text("内置", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                } else {
                                    IconButton(onClick = { showEditCategoryDialog = category }) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑", tint = TechBlue, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { showDeleteConfirm = category }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = WarningRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            if (index < state.categories.size - 1) {
                                Divider(color = DividerColor)
                            }
                        }
                    }
                }
            }

            // ---- 预算设置 ----
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("预算设置", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "设置每月消费预算，超过阈值时提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("月预算金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥ ") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechBlue,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = TechBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    label = { Text("提醒阈值 (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("%") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TechBlue,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = TechBlue
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = "保存预算",
                    onClick = {
                        focusManager.clearFocus()
                        val budget = budgetAmount.toDoubleOrNull() ?: return@GradientButton
                        val threshold = thresholdPercent.toIntOrNull() ?: 80
                        viewModel.handleIntent(SettingsIntent.SaveBudget(budget, threshold))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = budgetAmount.toDoubleOrNull() != null
                )
            }

            // ---- OCR 识别引擎 ----
            OCRConfigCard(focusManager = focusManager)

            // ---- 数据管理 ----
            GlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("数据管理", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "导出消费记录为 CSV 文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val context = androidx.compose.ui.platform.LocalContext.current

                GradientButton(
                    text = "导出 CSV",
                    onClick = {
                        viewModel.handleExport(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- 关于 ----
            GlowCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToAbout
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("关于 TickMate")
                        Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // 新增类目弹窗
    if (showAddCategoryDialog) {
        CategoryDialog(
            title = "新增类目",
            initialName = "",
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.handleIntent(SettingsIntent.AddCategory(name))
                showAddCategoryDialog = false
            }
        )
    }

    // 编辑类目弹窗
    showEditCategoryDialog?.let { category ->
        CategoryDialog(
            title = "编辑类目",
            initialName = category.name,
            onDismiss = { showEditCategoryDialog = null },
            onConfirm = { name ->
                viewModel.handleIntent(SettingsIntent.EditCategory(category.copy(name = name)))
                showEditCategoryDialog = null
            }
        )
    }

    // 删除确认弹窗
    showDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("删除类目「${category.name}」后，关联的消费记录将归入「其他」类目。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.handleIntent(SettingsIntent.DeleteCategory(category))
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = WarningRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
private fun OCRConfigCard(focusManager: androidx.compose.ui.focus.FocusManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ocrService = remember { com.tickmate.app.service.OnlineOCRService(context) }
    var expanded by remember { mutableStateOf(false) }
    val isCustom = ocrService.isCustomConfig()

    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("OCR 识别引擎", style = MaterialTheme.typography.titleMedium)
                Text(
                    "百度云 OCR（在线，高精度）",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
                Text(
                    "无网络时自动切换离线识别",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TextTertiary
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    "已内置百度云 OCR，开箱即用。\n如需更换为自己的 API Key，可在下方修改。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    lineHeight = 18.sp
                )

                if (isCustom) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "当前使用自定义 API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = TechBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            // 清除自定义配置，恢复内置默认
                            val prefs = context.getSharedPreferences("ocr_config", 0)
                            prefs.edit().remove("baidu_api_key").remove("baidu_secret_key")
                                .remove("baidu_access_token").apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("恢复默认 API Key")
                    }
                } else {
                    var apiKey by remember { mutableStateOf("") }
                    var secretKey by remember { mutableStateOf("") }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("自定义 API Key（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = secretKey,
                        onValueChange = { secretKey = it },
                        label = { Text("自定义 Secret Key（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GradientButton(
                        text = "保存自定义配置",
                        onClick = {
                            focusManager.clearFocus()
                            ocrService.saveConfig(apiKey.trim(), secretKey.trim())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank() && secretKey.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("类目名称") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (name.isNotBlank()) onConfirm(name.trim()) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechBlue,
                    unfocusedBorderColor = DividerColor,
                    cursorColor = TechBlue
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("确定", color = TechBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = CardBackground
    )
}
