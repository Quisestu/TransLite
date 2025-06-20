package com.example.test0.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import com.example.test0.viewmodel.SettingsViewModel
import com.example.test0.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel { 
        SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = currentTheme,
            onThemeChange = onThemeChange
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "返回"
                    )
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 配置状态显示
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "腾讯云配置状态",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                uiState.isConfigValid -> "✅ 配置有效"
                                uiState.isConfigSkipped -> "⏭️ 已跳过配置"
                                else -> "❌ 配置无效"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                uiState.isConfigValid -> MaterialTheme.colorScheme.primary
                                uiState.isConfigSkipped -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        
                        OutlinedButton(
                            onClick = viewModel::toggleConfigEditMode,
                            enabled = !uiState.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "修改配置",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.showConfigEdit) "收起" else "修改配置")
                        }
                    }
                    
                    // 配置编辑表单
                    if (uiState.showConfigEdit) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // APP ID
                        OutlinedTextField(
                            value = uiState.appId,
                            onValueChange = viewModel::updateAppId,
                            label = { Text("应用ID (App ID)") },
                            placeholder = { Text("请输入腾讯云应用ID") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = uiState.appIdError != null,
                            supportingText = uiState.appIdError?.let { { Text(it) } }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Secret ID
                        OutlinedTextField(
                            value = uiState.secretId,
                            onValueChange = viewModel::updateSecretId,
                            label = { Text("Secret ID") },
                            placeholder = { Text("请输入腾讯云Secret ID") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.secretIdError != null,
                            supportingText = uiState.secretIdError?.let { { Text(it) } }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Secret Key
                        OutlinedTextField(
                            value = uiState.secretKey,
                            onValueChange = viewModel::updateSecretKey,
                            label = { Text("Secret Key") },
                            placeholder = { Text("请输入腾讯云Secret Key") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (uiState.showSecretKey) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = viewModel::toggleSecretKeyVisibility
                                ) {
                                    Icon(
                                        imageVector = if (uiState.showSecretKey) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (uiState.showSecretKey) {
                                            "隐藏Secret Key"
                                        } else {
                                            "显示Secret Key"
                                        }
                                    )
                                }
                            },
                            isError = uiState.secretKeyError != null,
                            supportingText = uiState.secretKeyError?.let { { Text(it) } }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 地区选择
                        ExposedDropdownMenuBox(
                            expanded = uiState.showRegionDropdown,
                            onExpandedChange = viewModel::toggleRegionDropdown
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedRegionName,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("服务器地区") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.showRegionDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = uiState.showRegionDropdown,
                                onDismissRequest = { viewModel.toggleRegionDropdown(false) }
                            ) {
                                uiState.availableRegions.forEach { (code, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.selectRegion(code, name)
                                            viewModel.toggleRegionDropdown(false)
                                        }
                                    )
                                }
                            }
                        }
                        
                        // 保存和重置按钮
                        if (uiState.hasChanges) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::resetToOriginal,
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    Text("重置")
                                }
                                
                                Button(
                                    onClick = viewModel::saveConfiguration,
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("保存")
                                }
                            }
                        }
                    }
                    
                    // 导入开发者配置按钮
                    if (uiState.hasDeveloperConfig) {
                        Button(
                            onClick = viewModel::importDeveloperConfig,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("导入开发者配置")
                        }
                    }
                    
                    // 清除配置按钮（只在没有开发者配置时显示）
                    if ((uiState.isConfigValid || uiState.isConfigSkipped) && !uiState.hasDeveloperConfig) {
                        OutlinedButton(
                            onClick = viewModel::showClearConfigDialog,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("清除用户配置")
                        }
                    }
                }
            }
            
            // 主题设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "主题设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (currentTheme) {
                                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                                    AppThemeMode.DARK -> Icons.Default.DarkMode
                                    AppThemeMode.SYSTEM -> Icons.Default.Computer
                                },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = when (currentTheme) {
                                        AppThemeMode.LIGHT -> "浅色主题"
                                        AppThemeMode.DARK -> "深色主题"
                                        AppThemeMode.SYSTEM -> "跟随系统"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (currentTheme) {
                                        AppThemeMode.LIGHT -> "始终使用浅色主题"
                                        AppThemeMode.DARK -> "始终使用深色主题"
                                        AppThemeMode.SYSTEM -> "根据系统设置自动切换"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {
                                val nextTheme = when (currentTheme) {
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.SYSTEM
                                    AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                }
                                onThemeChange(nextTheme)
                            },
                            enabled = !uiState.isLoading
                        ) {
                            Text("切换")
                        }
                    }
                }
            }
            
            // 成功提示
            if (uiState.showSuccessMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = uiState.successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // 3秒后自动隐藏
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.hideSuccessMessage()
                }
            }
            
            // 错误提示
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // 确认清除配置对话框
    if (uiState.showClearConfigDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClearConfigDialog,
            title = { Text("确认清除配置") },
            text = { Text("此操作将清除您保存的配置信息，下次启动时将显示初始配置界面。您确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::clearUserConfig,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "确认清除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClearConfigDialog) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }
    
    // 重启应用对话框
    if (uiState.showRestartDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestartDialog,
            title = { Text("配置已保存") },
            text = { Text("新的配置需要重启应用以生效。是否立刻重启？") },
            confirmButton = {
                TextButton(onClick = viewModel::restartApp) {
                    Text(
                        text = "立刻重启",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestartDialog) {
                    Text(
                        text = "稍后重启",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }
}