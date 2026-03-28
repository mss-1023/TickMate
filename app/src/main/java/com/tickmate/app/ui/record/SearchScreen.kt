package com.tickmate.app.ui.record

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.ui.components.EmptyState
import com.tickmate.app.ui.components.ExpenseCard
import com.tickmate.app.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    recordRepository: RecordRepository = koinInject(),
    categoryRepository: CategoryRepository = koinInject()
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val records by recordRepository.searchRecords(query).collectAsState(initial = emptyList())
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("搜索商户名或备注...", color = TextTertiary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除", tint = TextTertiary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TechBlue,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = TechBlue,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("输入关键词搜索消费记录", color = TextTertiary)
            }
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(message = "未找到匹配的记录", icon = Icons.Default.Search)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "找到 ${records.size} 条记录",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(records, key = { it.id }) { record ->
                    val categoryName = categories
                        .find { it.id == record.categoryId }?.name ?: "其他"
                    ExpenseCard(
                        merchant = record.merchant,
                        amount = record.amount,
                        date = record.date,
                        categoryName = categoryName,
                        note = record.note,
                        onClick = { onNavigateToDetail(record.id) }
                    )
                }
            }
        }
    }
}
