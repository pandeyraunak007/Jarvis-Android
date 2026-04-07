package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserProfileManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voxn_profile", Context.MODE_PRIVATE)

    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName: StateFlow<String> = _userName

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean("onboarding_complete", false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    fun setUserName(name: String) {
        _userName.value = name
        prefs.edit().putString("user_name", name).apply()
    }

    fun completeOnboarding() {
        _onboardingComplete.value = true
        prefs.edit().putBoolean("onboarding_complete", true).apply()
    }
}
