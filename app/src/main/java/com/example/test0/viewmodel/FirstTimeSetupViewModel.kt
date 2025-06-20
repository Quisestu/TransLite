package com.example.test0.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.config.ConfigManager
import com.example.test0.config.TencentConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FirstTimeSetupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val configManager = ConfigManager(application)
    
    private val _uiState = MutableStateFlow(FirstTimeSetupUiState())
    val uiState: StateFlow<FirstTimeSetupUiState> = _uiState.asStateFlow()
    
    init {
        // 初始化支持的地区列表
        _uiState.value = _uiState.value.copy(
            availableRegions = configManager.getSupportedRegions(),
            selectedRegionName = "广州（华南）"
        )
        
        // 检查是否有开发者配置
        checkDeveloperConfig()
    }
    
    /**
     * 检查是否有开发者配置
     */
    private fun checkDeveloperConfig() {
        viewModelScope.launch {
            try {
                val hasDeveloperConfig = configManager.importFromDeveloperConfig()
                if (hasDeveloperConfig) {
                    // 有开发者配置，恢复原状态（不保存）
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
        validateForm()
    }
    
    /**
     * 更新Secret ID
     */
    fun updateSecretId(secretId: String) {
        _uiState.value = _uiState.value.copy(
            secretId = secretId,
            secretIdError = null
        )
        validateForm()
    }
    
    /**
     * 更新Secret Key
     */
    fun updateSecretKey(secretKey: String) {
        _uiState.value = _uiState.value.copy(
            secretKey = secretKey,
            secretKeyError = null
        )
        validateForm()
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
    }
    
    /**
     * 验证表单
     */
    private fun validateForm() {
        val state = _uiState.value
        val isValid = state.appId.isNotBlank() && 
                     state.secretId.isNotBlank() && 
                     state.secretKey.isNotBlank()
        
        _uiState.value = state.copy(isFormValid = isValid)
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
                
                // 刷新TencentConfig缓存，让配置立即生效
                TencentConfig.refreshConfig()
                
                // 简单验证（这里可以添加更复杂的验证逻辑）
                if (config.isValid()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSetupComplete = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "配置信息无效，请检查输入"
                    )
                }
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
                    // 刷新TencentConfig缓存，让配置立即生效
                    TencentConfig.refreshConfig()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSetupComplete = true
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
     * 显示跳过确认对话框
     */
    fun showSkipDialog() {
        _uiState.value = _uiState.value.copy(showSkipDialog = true)
    }
    
    /**
     * 关闭跳过确认对话框
     */
    fun dismissSkipDialog() {
        _uiState.value = _uiState.value.copy(showSkipDialog = false)
    }
    
    /**
     * 跳过配置
     */
    fun skipConfiguration() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            showSkipDialog = false,
            errorMessage = null
        )
        
        viewModelScope.launch {
            try {
                // 保存一个特殊的"跳过"标记配置
                // 这样系统知道用户选择了跳过，不会再显示首次配置界面
                val skipConfig = ConfigManager.TencentConfigData(
                    appId = "SKIPPED",
                    secretId = "SKIPPED", 
                    secretKey = "SKIPPED",
                    region = "ap-guangzhou"
                )
                
                configManager.saveUserConfig(skipConfig)
                
                // 刷新TencentConfig缓存，让跳过状态立即生效
                TencentConfig.refreshConfig()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "跳过配置失败：${e.message}"
                )
            }
        }
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
 * 首次配置界面的UI状态
 */
data class FirstTimeSetupUiState(
    val appId: String = "",
    val secretId: String = "",
    val secretKey: String = "",
    val selectedRegion: String = "ap-guangzhou",
    val selectedRegionName: String = "",
    val showSecretKey: Boolean = false,
    val showRegionDropdown: Boolean = false,
    val availableRegions: List<Pair<String, String>> = emptyList(),
    val appIdError: String? = null,
    val secretIdError: String? = null,
    val secretKeyError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val isSetupComplete: Boolean = false,
    val hasDeveloperConfig: Boolean = false,
    val showSkipDialog: Boolean = false
) 