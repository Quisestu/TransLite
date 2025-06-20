package com.example.test0.config

import android.content.Context
import kotlinx.coroutines.runBlocking

object TencentConfig {
    private var _configManager: ConfigManager? = null
    private var _configCache: ConfigManager.TencentConfigData? = null
    
    /**
     * 初始化配置管理器
     */
    fun initialize(context: Context) {
        _configManager = ConfigManager(context)
    }
    
    /**
     * 获取当前配置（带缓存）
     */
    private fun getConfig(): ConfigManager.TencentConfigData {
        return _configCache ?: run {
            val configManager = _configManager ?: throw IllegalStateException("TencentConfig not initialized")
            val config = runBlocking { configManager.getConfig() }
            _configCache = config
            config
        }
    }
    
    /**
     * 刷新配置缓存
     */
    fun refreshConfig() {
        _configCache = null
    }
    
    // 兼容原有接口
    val SECRET_ID: String get() = getConfig().secretId
    val SECRET_KEY: String get() = getConfig().secretKey
    val APP_ID: String get() = getConfig().appId
    val REGION: String get() = getConfig().region
    const val ENDPOINT = "tmt.tencentcloudapi.com"
    
    /**
     * 检查配置是否有效
     */
    fun isConfigValid(): Boolean = getConfig().isValid()
    
    /**
     * 检查配置是否被跳过
     */
    fun isConfigSkipped(): Boolean = getConfig().isSkipped()
    
    /**
     * 获取配置状态描述
     */
    fun getConfigStatus(): String {
        val config = getConfig()
        return when {
            config.isValid() -> "配置有效"
            config.isSkipped() -> "已跳过配置"
            else -> "配置无效"
        }
    }
    
    /**
     * 获取ConfigManager实例
     */
    fun getConfigManager(): ConfigManager {
        return _configManager ?: throw IllegalStateException("TencentConfig not initialized")
    }
} 