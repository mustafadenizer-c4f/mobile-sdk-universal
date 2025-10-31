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
            try {
                // Safe way to check if key exists and get its value
                if (prefs.contains(key)) {
                    val value = prefs.getAll()[key]
                    val stringValue = when (value) {
                        is String -> value
                        is Boolean -> value.toString()
                        is Int -> value.toString()
                        is Long -> value.toString()
                        is Float -> value.toString()
                        else -> null
                    }

                    stringValue?.let {
                        if (it.isNotBlank()) {
                            // Use standardized key names (your existing logic)
                            when {
                                key.contains("id", ignoreCase = true) -> data["userId"] = it
                                key.contains("email", ignoreCase = true) -> data["userEmail"] = it
                                key.contains("name", ignoreCase = true) -> data["userName"] = it
                                key.contains("tier", ignoreCase = true) -> data["userTier"] = it
                                key.contains("role", ignoreCase = true) -> data["userRole"] = it
                                key.contains("premium", ignoreCase = true) -> data["isPremium"] = it
                                key.contains("logged", ignoreCase = true) -> data["isLoggedIn"] = it
                                key.contains("session", ignoreCase = true) -> data["sessionId"] = it
                                key.contains("install", ignoreCase = true) -> data["installSource"] = it
                                key.contains("version", ignoreCase = true) -> data["appVersion"] = it
                                key.contains("language", ignoreCase = true) -> data["language"] = it
                                else -> data[key] = it
                            }
                            data["${key}Source"] = source
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StorageUtils", "Error reading key $key: ${e.message}")
            }
        }
    }

    // New method to find specific key
    fun findSpecificData(context: Context, key: String): String? {
        return try {
            // Check regular SharedPreferences first
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            if (sharedPrefs.contains(key)) {
                when {
                    sharedPrefs.getAll()[key] is String -> sharedPrefs.getString(key, null)
                    sharedPrefs.getAll()[key] is Boolean -> sharedPrefs.getBoolean(key, false).toString()
                    sharedPrefs.getAll()[key] is Int -> sharedPrefs.getInt(key, 0).toString()
                    sharedPrefs.getAll()[key] is Long -> sharedPrefs.getLong(key, 0L).toString()
                    sharedPrefs.getAll()[key] is Float -> sharedPrefs.getFloat(key, 0f).toString()
                    else -> null
                }
            } else {
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

                    if (encryptedPrefs.contains(key)) {
                        when {
                            encryptedPrefs.getAll()[key] is String -> encryptedPrefs.getString(key, null)
                            encryptedPrefs.getAll()[key] is Boolean -> encryptedPrefs.getBoolean(key, false).toString()
                            encryptedPrefs.getAll()[key] is Int -> encryptedPrefs.getInt(key, 0).toString()
                            encryptedPrefs.getAll()[key] is Long -> encryptedPrefs.getLong(key, 0L).toString()
                            encryptedPrefs.getAll()[key] is Float -> encryptedPrefs.getFloat(key, 0f).toString()
                            else -> null
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}