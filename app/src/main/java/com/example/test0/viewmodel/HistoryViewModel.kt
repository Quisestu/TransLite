package com.example.test0.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.model.TranslationRecord
import com.example.test0.model.TranslationType
import com.example.test0.repository.TranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val translationRepository = TranslationRepository.getInstance(application.applicationContext)
    
    // 过滤类型状态
    private val _selectedFilterType = MutableStateFlow<TranslationType?>(null)
    val selectedFilterType = _selectedFilterType.asStateFlow()
    
    // 历史记录列表
    private val _historyRecords = MutableStateFlow<List<TranslationRecord>>(emptyList())
    val historyRecords = _historyRecords.asStateFlow()
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    // 显示删除确认对话框
    private val _showDeleteAllDialog = MutableStateFlow(false)
    val showDeleteAllDialog = _showDeleteAllDialog.asStateFlow()
    
    init {
        loadAllRecords()
    }
    
    fun setInitialFilter(type: TranslationType?) {
        _selectedFilterType.value = type
        if (type == null) {
            loadAllRecords()
        } else {
            loadRecordsByType(type)
        }
    }
    
    fun updateFilter(type: TranslationType?) {
        _selectedFilterType.value = type
        if (type == null) {
            loadAllRecords()
        } else {
            loadRecordsByType(type)
        }
    }
    
    private fun loadAllRecords() {
        viewModelScope.launch {
            try {
                translationRepository.getAllRecords().collect { records ->
                    _historyRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to load all records: ${e.message}", e)
                _errorMessage.value = "加载历史记录失败: ${e.message}"
            }
        }
    }
    
    private fun loadRecordsByType(type: TranslationType) {
        viewModelScope.launch {
            try {
                translationRepository.getRecordsByType(type).collect { records ->
                    _historyRecords.value = records
                }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to load records by type: ${e.message}", e)
                _errorMessage.value = "加载历史记录失败: ${e.message}"
            }
        }
    }
    
    fun deleteRecord(record: TranslationRecord) {
        viewModelScope.launch {
            try {
                translationRepository.deleteRecord(record)
                Log.i("HistoryViewModel", "Record deleted successfully")
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to delete record: ${e.message}", e)
                _errorMessage.value = "删除记录失败: ${e.message}"
            }
        }
    }
    
    fun showDeleteAllDialog() {
        _showDeleteAllDialog.value = true
    }
    
    fun hideDeleteAllDialog() {
        _showDeleteAllDialog.value = false
    }
    
    fun deleteAllRecords() {
        viewModelScope.launch {
            try {
                translationRepository.deleteAllRecords()
                _showDeleteAllDialog.value = false
                Log.i("HistoryViewModel", "All records deleted successfully")
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Failed to delete all records: ${e.message}", e)
                _errorMessage.value = "清除所有记录失败: ${e.message}"
            }
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
} 