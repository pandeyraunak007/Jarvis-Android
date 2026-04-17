package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object KeystoreManager {
    private const val FILE_NAME = "voxn_secure_prefs"
    private const val KEY_GROQ_API = "groq_api_key"

    private fun prefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveApiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_GROQ_API, key).apply()
    }

    fun loadApiKey(context: Context): String? {
        return prefs(context).getString(KEY_GROQ_API, null)?.takeIf { it.isNotBlank() }
    }

    fun deleteApiKey(context: Context) {
        prefs(context).edit().remove(KEY_GROQ_API).apply()
    }

    fun hasApiKey(context: Context): Boolean = loadApiKey(context) != null
}
