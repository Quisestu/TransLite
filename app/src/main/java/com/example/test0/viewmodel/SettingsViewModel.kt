package com.example.test0.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.config.ConfigManager
import com.example.test0.config.TencentConfig
import com.example.test0.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    private val onThemeChange: (AppThemeMode) -> Unit = {}
) : AndroidViewModel(application) {
    
    private val configManager = ConfigManager(application)
    
    private val _uiState = MutableStateFlow(SettingsUiState(currentTheme = currentTheme))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // 原始配置（用于比较是否有变化）
    private var originalConfig: ConfigManager.TencentConfigData? = null
    
    init {
        // 初始化地区列表
        _uiState.value = _uiState.value.copy(
            availableRegions = configManager.getSupportedRegions()
        )
        
        // 加载当前配置
        loadCurrentConfiguration()
        
        // 检查开发者配置
        checkDeveloperConfig()
    }
    
    /**
     * 加载当前配置
     */
    private fun loadCurrentConfiguration() {
        viewModelScope.launch {
            try {
                val config = configManager.getConfig()
                originalConfig = config
                
                val regionName = configManager.getSupportedRegions()
                    .find { it.first == config.region }?.second ?: "广州（华南）"
                
                _uiState.value = _uiState.value.copy(
                    appId = config.appId,
                    secretId = config.secretId,
                    secretKey = config.secretKey,
                    selectedRegion = config.region,
                    selectedRegionName = regionName,
                    isConfigValid = config.isValid(),
                    isConfigSkipped = config.isSkipped(),
                    hasChanges = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "加载配置失败：${e.message}"
                )
            }
        }
    }
    
    /**
     * 检查开发者配置
     */
    private fun checkDeveloperConfig() {
        viewModelScope.launch {
            try {
                // 临时保存当前用户配置
                val userConfig = configManager.getUserConfig().first()
                
                // 尝试导入开发者配置
                val hasDeveloperConfig = configManager.importFromDeveloperConfig()
                
                // 恢复用户配置
                if (userConfig.isValid()) {
                    configManager.saveUserConfig(userConfig)
                } else if (hasDeveloperConfig) {
                    // 如果没有用户配置但有开发者配置，清除临时导入
                    configManager.clearUserConfig()
                }
                
                _uiState.value = _uiState.value.copy(
                    hasDeveloperConfig = hasDeveloperConfig
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    hasDeveloperConfig = false
                )
            }
        }
    }
    
    /**
     * 更新App ID
     */
    fun updateAppId(appId: String) {
        _uiState.value = _uiState.value.copy(
            appId = appId,
            appIdError = null
        )
        checkForChanges()
    }
    
    /**
     * 更新Secret ID
     */
    fun updateSecretId(secretId: String) {
        _uiState.value = _uiState.value.copy(
            secretId = secretId,
            secretIdError = null
        )
        checkForChanges()
    }
    
    /**
     * 更新Secret Key
     */
    fun updateSecretKey(secretKey: String) {
        _uiState.value = _uiState.value.copy(
            secretKey = secretKey,
            secretKeyError = null
        )
        checkForChanges()
    }
    
    /**
     * 切换Secret Key可见性
     */
    fun toggleSecretKeyVisibility() {
        _uiState.value = _uiState.value.copy(
            showSecretKey = !_uiState.value.showSecretKey
        )
    }
    
    /**
     * 切换地区下拉框
     */
    fun toggleRegionDropdown(show: Boolean = !_uiState.value.showRegionDropdown) {
        _uiState.value = _uiState.value.copy(
            showRegionDropdown = show
        )
    }
    
    /**
     * 选择地区
     */
    fun selectRegion(code: String, name: String) {
        _uiState.value = _uiState.value.copy(
            selectedRegion = code,
            selectedRegionName = name
        )
        checkForChanges()
    }
    
    /**
     * 检查是否有变化
     */
    private fun checkForChanges() {
        val state = _uiState.value
        val original = originalConfig
        
        val hasChanges = if (original != null) {
            state.appId != original.appId ||
            state.secretId != original.secretId ||
            state.secretKey != original.secretKey ||
            state.selectedRegion != original.region
        } else {
            state.appId.isNotBlank() ||
            state.secretId.isNotBlank() ||
            state.secretKey.isNotBlank()
        }
        
        _uiState.value = state.copy(hasChanges = hasChanges)
    }
    
    /**
     * 重置到原始配置
     */
    fun resetToOriginal() {
        val original = originalConfig
        if (original != null) {
            val regionName = configManager.getSupportedRegions()
                .find { it.first == original.region }?.second ?: "广州（华南）"
            
            _uiState.value = _uiState.value.copy(
                appId = original.appId,
                secretId = original.secretId,
                secretKey = original.secretKey,
                selectedRegion = original.region,
                selectedRegionName = regionName,
                hasChanges = false,
                appIdError = null,
                secretIdError = null,
                secretKeyError = null
            )
        }
    }
    
    /**
     * 保存配置
     */
    fun saveConfiguration() {
        val state = _uiState.value
        
        // 验证输入
        if (!validateInputs()) {
            return
        }
        
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val config = ConfigManager.TencentConfigData(
                    appId = state.appId.trim(),
                    secretId = state.secretId.trim(),
                    secretKey = state.secretKey.trim(),
                    region = state.selectedRegion
                )
                
                // 保存配置
                configManager.saveUserConfig(config)
                
                // 更新原始配置
                originalConfig = config
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasChanges = false,
                    isConfigValid = config.isValid(),
                    isConfigSkipped = config.isSkipped(),
                    showConfigEdit = false,  // 保存成功后收起编辑表单
                    showRestartDialog = true,  // 显示重启提示对话框
                    successMessage = "操作成功！"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "保存配置失败：${e.message}"
                )
            }
        }
    }
    
    /**
     * 导入开发者配置
     */
    fun importDeveloperConfig() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val success = configManager.importFromDeveloperConfig()
                if (success) {
                    // 重新加载配置
                    loadCurrentConfiguration()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showRestartDialog = true,
                        successMessage = "操作成功！"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "开发者配置不可用"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "导入开发者配置失败：${e.message}"
                )
            }
        }
    }
    
    /**
     * 显示清除配置对话框
     */
    fun showClearConfigDialog() {
        _uiState.value = _uiState.value.copy(showClearConfigDialog = true)
    }
    
    /**
     * 关闭清除配置对话框
     */
    fun dismissClearConfigDialog() {
        _uiState.value = _uiState.value.copy(showClearConfigDialog = false)
    }
    
    /**
     * 清除用户配置
     */
    fun clearUserConfig() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            showClearConfigDialog = false,
            errorMessage = null
        )
        
        viewModelScope.launch {
            try {
                configManager.clearUserConfig()
                
                // 重新加载配置
                loadCurrentConfiguration()
                
                // 清除配置只在没有开发者配置时可用，所以直接显示固定提示信息
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showSuccessMessage = true,
                    successMessage = "配置已清除，下次启动时将显示初始配置界面"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "清除配置失败：${e.message}"
                )
            }
        }
    }
    
    /**
     * 隐藏成功消息
     */
    fun hideSuccessMessage() {
        _uiState.value = _uiState.value.copy(showSuccessMessage = false)
    }
    
    /**
     * 切换配置编辑模式
     */
    fun toggleConfigEditMode() {
        _uiState.value = _uiState.value.copy(showConfigEdit = !_uiState.value.showConfigEdit)
    }
    
    /**
     * 关闭重启对话框
     */
    fun dismissRestartDialog() {
        _uiState.value = _uiState.value.copy(showRestartDialog = false)
    }
    
    /**
     * 重启应用
     */
    fun restartApp() {
        // Android中重启应用的标准方法
        val context = getApplication<android.app.Application>()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        kotlin.system.exitProcess(0)
    }
    
    /**
     * 切换主题
     */
    fun switchTheme() {
        val nextTheme = when (_uiState.value.currentTheme) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        _uiState.value = _uiState.value.copy(currentTheme = nextTheme)
        onThemeChange(nextTheme)
    }
    
    /**
     * 验证输入
     */
    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var isValid = true
        var appIdError: String? = null
        var secretIdError: String? = null
        var secretKeyError: String? = null
        
        // 验证App ID
        if (state.appId.isBlank()) {
            appIdError = "请输入应用ID"
            isValid = false
        } else if (!state.appId.matches(Regex("\\d+"))) {
            appIdError = "应用ID应该是数字"
            isValid = false
        }
        
        // 验证Secret ID
        if (state.secretId.isBlank()) {
            secretIdError = "请输入Secret ID"
            isValid = false
        } else if (state.secretId.length < 32) {
            secretIdError = "Secret ID长度不正确"
            isValid = false
        }
        
        // 验证Secret Key
        if (state.secretKey.isBlank()) {
            secretKeyError = "请输入Secret Key"
            isValid = false
        } else if (state.secretKey.length < 32) {
            secretKeyError = "Secret Key长度不正确"
            isValid = false
        }
        
        _uiState.value = state.copy(
            appIdError = appIdError,
            secretIdError = secretIdError,
            secretKeyError = secretKeyError
        )
        
        return isValid
    }
}

/**
 * 设置页面的UI状态
 */
data class SettingsUiState(
    val appId: String = "",
    val secretId: String = "",
    val secretKey: String = "",
    val selectedRegion: String = "ap-guangzhou",
    val selectedRegionName: String = "广州（华南）",
    val showSecretKey: Boolean = false,
    val showRegionDropdown: Boolean = false,
    val availableRegions: List<Pair<String, String>> = emptyList(),
    val appIdError: String? = null,
    val secretIdError: String? = null,
    val secretKeyError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val hasChanges: Boolean = false,
    val isConfigValid: Boolean = false,
    val isConfigSkipped: Boolean = false,
    val showClearConfigDialog: Boolean = false,
    val showSuccessMessage: Boolean = false,
    val successMessage: String = "操作成功！",
    val hasDeveloperConfig: Boolean = false,
    val showConfigEdit: Boolean = false,
    val showRestartDialog: Boolean = false,
    // 主题相关
    val currentTheme: AppThemeMode = AppThemeMode.SYSTEM
) 