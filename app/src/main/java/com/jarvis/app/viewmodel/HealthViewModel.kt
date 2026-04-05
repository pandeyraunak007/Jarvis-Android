package com.jarvis.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.app.data.model.DailyHealthEntry
import com.jarvis.app.data.model.HealthData
import com.jarvis.app.manager.HealthConnectManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    val manager = HealthConnectManager(app)

    val healthData: StateFlow<HealthData> = manager.healthData
    val weeklyEntries: StateFlow<List<DailyHealthEntry>> = manager.weeklyEntries
    val isAvailable: StateFlow<Boolean> = manager.isAvailable
    val permissions = manager.permissions

    fun refresh() {
        viewModelScope.launch { manager.fetchAllData() }
    }

    init { refresh() }
}
