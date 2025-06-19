package com.example.test0.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.model.TranslationRecord
import com.example.test0.model.TranslationType
import com.example.test0.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun FilterDropdownMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        colors = if (isSelected) {
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.primary
            )
        } else {
            MenuDefaults.itemColors()
        },
        modifier = if (isSelected) {
            Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 4.dp)
        } else {
            Modifier.padding(horizontal = 4.dp)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    initialFilterType: TranslationType? = null,
    viewModel: HistoryViewModel = viewModel()
) {
    val selectedFilterType by viewModel.selectedFilterType.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showDeleteAllDialog by viewModel.showDeleteAllDialog.collectAsState()
    
    var showFilterMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    // 设置初始过滤器
    LaunchedEffect(initialFilterType) {
        viewModel.setInitialFilter(initialFilterType)
    }
    
    // 错误对话框
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 删除所有记录确认对话框
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteAllDialog() },
            title = { Text("确认删除") },
            text = { Text("确定要清除所有历史记录吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAllRecords() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteAllDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "历史记录",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "返回")
                    }
                },
                actions = {
                    // 过滤器下拉菜单
                    Box {
                        // 获取当前选择的过滤器显示名称
                        val currentFilterName = when (selectedFilterType) {
                            null -> "所有"
                            TranslationType.TEXT -> "文本"
                            TranslationType.SPEECH -> "语音"
                            TranslationType.IMAGE -> "图片"
                        }
                        
                        TextButton(
                            onClick = { showFilterMenu = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentFilterName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "展开过滤器",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            FilterDropdownMenuItem(
                                text = "所有",
                                isSelected = selectedFilterType == null,
                                onClick = {
                                    viewModel.updateFilter(null)
                                    showFilterMenu = false
                                }
                            )
                            FilterDropdownMenuItem(
                                text = "文本翻译",
                                isSelected = selectedFilterType == TranslationType.TEXT,
                                onClick = {
                                    viewModel.updateFilter(TranslationType.TEXT)
                                    showFilterMenu = false
                                }
                            )
                            FilterDropdownMenuItem(
                                text = "语音翻译",
                                isSelected = selectedFilterType == TranslationType.SPEECH,
                                onClick = {
                                    viewModel.updateFilter(TranslationType.SPEECH)
                                    showFilterMenu = false
                                }
                            )
                            FilterDropdownMenuItem(
                                text = "图片翻译",
                                isSelected = selectedFilterType == TranslationType.IMAGE,
                                onClick = {
                                    viewModel.updateFilter(TranslationType.IMAGE)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                    
                    // 删除所有按钮
                    IconButton(onClick = { viewModel.showDeleteAllDialog() }) {
                        Icon(
                            Icons.Outlined.Delete, 
                            contentDescription = "清除所有",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (historyRecords.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无历史记录",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                }
            } else {
                // 历史记录列表
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyRecords) { record ->
                        HistoryRecordItem(
                            record = record,
                            onCopy = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            },
                            onDelete = { viewModel.deleteRecord(record) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRecordItem(
    record: TranslationRecord,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    // 检测文本是否可能超过3行（大约每行20-25个字符）
    val sourceNeedsExpansion = record.sourceText.length > 120 || record.sourceText.count { it == '\n' } >= 3
    val translatedNeedsExpansion = record.translatedText.length > 120 || record.translatedText.count { it == '\n' } >= 3
    val hasLongText = sourceNeedsExpansion || translatedNeedsExpansion
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            )
            .clickable(enabled = hasLongText && !isExpanded) { 
                isExpanded = true 
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 头部：类型、语言、时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 类型标签
                    val typeInfo = TranslationType.fromValue(record.type)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = typeInfo?.displayName ?: record.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 语言信息
                    Text(
                        text = "${record.sourceLanguage} → ${record.targetLanguage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // 时间和删除按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 原文
            Text(
                text = "原文:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = record.sourceText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = { onCopy(record.sourceText) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制原文",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 译文
            Text(
                text = "译文:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = record.translatedText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = { onCopy(record.translatedText) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制译文",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 展开状态下的收起按钮，放在卡片右下角
            if (isExpanded && hasLongText) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { isExpanded = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "收起",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // 未展开时的提示（仅在有长文本时显示）
            if (!isExpanded && hasLongText) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "点击查看完整内容",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

        }
    }
} 