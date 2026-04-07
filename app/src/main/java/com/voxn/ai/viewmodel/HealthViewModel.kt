package com.voxn.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.model.DailyHealthEntry
import com.voxn.ai.data.model.HealthData
import com.voxn.ai.manager.HealthConnectManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = HealthConnectManager(app)

    val healthData: StateFlow<HealthData> = manager.healthData
    val weeklyEntries: StateFlow<List<DailyHealthEntry>> = manager.weeklyEntries
    val isAvailable: StateFlow<Boolean> = manager.isAvailable
    val isLoading: StateFlow<Boolean> = manager.isLoading
    val errorMessage: StateFlow<String?> = manager.errorMessage
    val permissions = manager.permissions

    fun refresh() {
        viewModelScope.launch { manager.fetchAllData() }
    }

    fun clearError() = manager.clearError()

    init { refresh() }
}
