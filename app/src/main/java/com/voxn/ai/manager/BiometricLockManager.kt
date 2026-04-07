package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BiometricLockManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voxn_security", Context.MODE_PRIVATE)

    private val _lockEnabled = MutableStateFlow(prefs.getBoolean("biometric_lock", false))
    val lockEnabled: StateFlow<Boolean> = _lockEnabled

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    fun setLockEnabled(enabled: Boolean) {
        _lockEnabled.value = enabled
        prefs.edit().putBoolean("biometric_lock", enabled).apply()
    }

    fun setUnlocked() {
        _isUnlocked.value = true
    }

    fun needsUnlock(): Boolean {
        return _lockEnabled.value && !_isUnlocked.value
    }

    companion object {
        fun isBiometricAvailable(context: Context): Boolean {
            val manager = BiometricManager.from(context)
            return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        }

        fun showPrompt(
            activity: FragmentActivity,
            onSuccess: () -> Unit,
            onError: (String) -> Unit,
        ) {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError(errString.toString())
                    }
                }
            }
            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Voxn AI")
                .setSubtitle("Verify your identity to access your data")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        }
    }
}
