package com.example.surveysdk

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

object StorageUtils {

    private val userKeys = listOf(
        "userId", "user_id", "id",
        "userEmail", "email", "user_email",
        "userName", "username", "user_name",
        "userTier", "user_tier", "tier",
        "userRole", "user_role", "role",
        "isPremium", "is_premium", "premium",
        "isLoggedIn", "is_logged_in", "logged_in",
        "sessionId", "session_id",
        "installSource", "install_source",
        "appVersion", "app_version",
        "language", "locale"
    )

    fun findUserData(context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()

        // Check SharedPreferences
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        findDataInPreferences(sharedPrefs, data, "SharedPreferences")

        // Check EncryptedSharedPreferences
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            findDataInPreferences(encryptedPrefs, data, "EncryptedSharedPreferences")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return data
    }

    private fun findDataInPreferences(prefs: SharedPreferences, data: MutableMap<String, String>, source: String) {
        userKeys.forEach { key ->
            prefs.getString(key, null)?.let { value ->
                if (value.isNotBlank()) {
                    // Use standardized key names
                    when {
                        key.contains("id", ignoreCase = true) -> data["userId"] = value
                        key.contains("email", ignoreCase = true) -> data["userEmail"] = value
                        key.contains("name", ignoreCase = true) -> data["userName"] = value
                        key.contains("tier", ignoreCase = true) -> data["userTier"] = value
                        key.contains("role", ignoreCase = true) -> data["userRole"] = value
                        key.contains("premium", ignoreCase = true) -> data["isPremium"] = value
                        key.contains("logged", ignoreCase = true) -> data["isLoggedIn"] = value
                        key.contains("session", ignoreCase = true) -> data["sessionId"] = value
                        key.contains("install", ignoreCase = true) -> data["installSource"] = value
                        key.contains("version", ignoreCase = true) -> data["appVersion"] = value
                        key.contains("language", ignoreCase = true) -> data["language"] = value
                        else -> data[key] = value
                    }
                    data["${key}Source"] = source
                }
            }
        }
    }

    // New method to find specific key
    fun findSpecificData(context: Context, key: String): String? {
        return try {
            // Check regular SharedPreferences first
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPrefs.getString(key, null) ?: run {
                // Check encrypted if not found
                try {
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val encryptedPrefs = EncryptedSharedPreferences.create(
                        context,
                        "secure_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    encryptedPrefs.getString(key, null)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

}