package com.example.test0.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.R
import com.example.test0.viewmodel.FirstTimeSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: FirstTimeSetupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val shadowsFont = FontFamily(Font(R.font.shadows_into_light_two_regular))
    
    // 帮助对话框状态
    var showHelpDialog by remember { mutableStateOf(false) }
    
    // 处理设置完成
    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题区域
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TransLite",
                fontSize = 40.sp,
                fontFamily = shadowsFont,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "该软件必须开通腾讯云机器翻译服务才能使用。请填写你的信息（稍后可在设置中更改）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 配置表单
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "腾讯云配置信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
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
                
                // 帮助按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { showHelpDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "帮助信息",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("如何获取配置信息？")
                    }
                }
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 操作按钮
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = viewModel::saveConfiguration,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.isFormValid
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (uiState.isLoading) "验证配置中..." else "完成配置"
                )
            }
            
            // 如果有开发者配置，显示导入按钮
            if (uiState.hasDeveloperConfig) {
                OutlinedButton(
                    onClick = viewModel::importDeveloperConfig,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Text("导入开发者配置")
                }
            }
            
            // 跳过按钮
            TextButton(
                onClick = viewModel::showSkipDialog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("跳过配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    
    // 跳过确认对话框
    if (uiState.showSkipDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSkipDialog,
            title = { Text("确认跳过") },
            text = { 
                Text(
                    text = "确认跳过吗？你将无法使用任何翻译服务！\n\n" +
                           "跳过后可以在设置中重新配置腾讯云服务。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::skipConfiguration,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "确认跳过",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSkipDialog) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.onSurface
                        )
                }
            }
        )
    }
    
    // 帮助信息对话框
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("💡 如何获取配置信息？") },
            text = { 
                Text(
                    text = "1. 进入机器翻译控制台(https://console.cloud.tencent.cn/tmt)\n" +
                           "2. 阅读《服务等级协议》后勾选“我已阅读并同意《服务等级协议》”，然后单击开通\n" +
                           "3. 在腾讯云控制台-账号中心-账号信息(https://console.cloud.tencent.cn/developer)中获取AppID\n" +
                           "4. 在腾讯云控制台-账号中心-访问管理-访问密钥-API密钥管理中点击新建密钥，获取SecretID和SecretKey",
                    style = MaterialTheme.typography.bodyMedium
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "知道了",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }
} 