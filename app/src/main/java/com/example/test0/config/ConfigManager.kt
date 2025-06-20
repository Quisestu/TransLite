package com.example.test0.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test0.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore扩展
private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(name = "tencent_config")

class ConfigManager(private val context: Context) {
    
    companion object {
        // DataStore keys
        private val APP_ID_KEY = stringPreferencesKey("tencent_app_id")
        private val SECRET_ID_KEY = stringPreferencesKey("tencent_secret_id")
        private val SECRET_KEY_KEY = stringPreferencesKey("tencent_secret_key")
        private val REGION_KEY = stringPreferencesKey("tencent_region")
        
        // 默认配置
        private const val DEFAULT_REGION = "ap-guangzhou"
    }
    
    /**
     * 配置数据类
     */
    data class TencentConfigData(
        val appId: String = "",
        val secretId: String = "",
        val secretKey: String = "",
        val region: String = DEFAULT_REGION
    ) {
        fun isValid(): Boolean {
            return appId.isNotBlank() && secretId.isNotBlank() && secretKey.isNotBlank() && !isSkipped()
        }
        
        fun isEmpty(): Boolean {
            return appId.isBlank() && secretId.isBlank() && secretKey.isBlank()
        }
        
        fun isSkipped(): Boolean {
            return appId == "SKIPPED" && secretId == "SKIPPED" && secretKey == "SKIPPED"
        }
    }
    
    /**
     * 获取当前有效配置（多层优先级）
     * 1. 用户运行时配置(DataStore) - 最高优先级
     * 2. 编译时配置(BuildConfig) - 调试时使用  
     * 3. 默认配置 - 兜底方案
     */
    suspend fun getConfig(): TencentConfigData {
        val userConfig = getUserConfig().first()
        
        // 如果用户配置有效或者是跳过状态，直接使用
        if (userConfig.isValid() || userConfig.isSkipped()) {
            return userConfig
        }
        
        // 否则尝试使用BuildConfig配置
        val buildConfig = getBuildConfig()
        if (buildConfig.isValid()) {
            return buildConfig
        }
        
        // 最后使用默认配置（空配置）
        return TencentConfigData()
    }
    
    /**
     * 获取用户配置流
     */
    fun getUserConfig(): Flow<TencentConfigData> {
        return context.configDataStore.data.map { preferences ->
            TencentConfigData(
                appId = preferences[APP_ID_KEY] ?: "",
                secretId = preferences[SECRET_ID_KEY] ?: "",
                secretKey = preferences[SECRET_KEY_KEY] ?: "",
                region = preferences[REGION_KEY] ?: DEFAULT_REGION
            )
        }
    }
    
    /**
     * 获取BuildConfig配置
     */
    private fun getBuildConfig(): TencentConfigData {
        return TencentConfigData(
            appId = BuildConfig.TENCENT_APP_ID ?: "",
            secretId = BuildConfig.TENCENT_SECRET_ID ?: "",
            secretKey = BuildConfig.TENCENT_SECRET_KEY ?: "",
            region = BuildConfig.TENCENT_REGION ?: DEFAULT_REGION
        )
    }
    
    /**
     * 保存用户配置
     */
    suspend fun saveUserConfig(config: TencentConfigData) {
        context.configDataStore.edit { preferences ->
            preferences[APP_ID_KEY] = config.appId
            preferences[SECRET_ID_KEY] = config.secretId
            preferences[SECRET_KEY_KEY] = config.secretKey
            preferences[REGION_KEY] = config.region
        }
    }
    
    /**
     * 清除用户配置
     */
    suspend fun clearUserConfig() {
        context.configDataStore.edit { preferences ->
            preferences.remove(APP_ID_KEY)
            preferences.remove(SECRET_ID_KEY)
            preferences.remove(SECRET_KEY_KEY) 
            preferences.remove(REGION_KEY)
        }
    }
    
    /**
     * 检查是否需要显示首次配置界面
     * 如果没有任何有效配置且用户没有跳过配置，则需要显示
     */
    suspend fun needsFirstTimeSetup(): Boolean {
        val config = getConfig()
        // 如果配置有效，不需要首次配置
        if (config.isValid()) {
            return false
        }
        // 如果用户已经跳过配置，也不需要首次配置
        if (config.isSkipped()) {
            return false
        }
        // 只有在没有有效配置且未跳过的情况下，才需要首次配置
        return true
    }
    
    /**
     * 从开发者配置导入到用户配置
     * 用于开发者一键将BuildConfig配置导入到用户配置
     */
    suspend fun importFromDeveloperConfig(): Boolean {
        val buildConfig = getBuildConfig()
        return if (buildConfig.isValid()) {
            saveUserConfig(buildConfig)
            true
        } else {
            false
        }
    }
    
    /**
     * 获取支持的地区列表
     */
    fun getSupportedRegions(): List<Pair<String, String>> {
        return listOf(
            "ap-guangzhou" to "广州（华南）",
            "ap-shanghai" to "上海（华东）",
            "ap-beijing" to "北京（华北）",
            "ap-chengdu" to "成都（西南）",
            "ap-singapore" to "新加坡",
            "ap-hongkong" to "香港", 
            "ap-tokyo" to "东京",
            "na-siliconvalley" to "硅谷",
            "na-ashburn" to "弗吉尼亚"
        )
    }
} 