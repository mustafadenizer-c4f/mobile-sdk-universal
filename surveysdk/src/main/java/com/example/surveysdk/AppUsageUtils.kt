package com.example.surveysdk

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppUsageUtils {

    fun getAppUsageData(context: Context): Map<String, String> {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                return mapOf("usage_stats_available" to "false")
            }

            // Check if permission is granted
            val hasUsageStatsPermission = context.checkSelfPermission(
                android.Manifest.permission.PACKAGE_USAGE_STATS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasUsageStatsPermission) {
                return mapOf("usage_stats_permission" to "denied")
            }

            // Get usage stats for last 24 hours
            val calendar = java.util.Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(java.util.Calendar.HOUR, -24)
            val startTime = calendar.timeInMillis

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val currentAppStats = usageStats?.find { it.packageName == context.packageName }
            val appUsage = currentAppStats?.totalTimeInForeground ?: 0L

            mapOf(
                "app_usage_time_ms" to appUsage.toString(),
                "last_used_time" to (currentAppStats?.lastTimeUsed?.toString() ?: "0"),
                "usage_stats_available" to "true"
            )
        } catch (e: Exception) {
            Log.e("AppUsageUtils", "Error getting app usage: ${e.message}")
            mapOf("usage_stats_error" to "api_error") // Fixed string value
        }
    }

    fun getEnhancedSessionData(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences("survey_sdk_session", Context.MODE_PRIVATE)

        val sessionCount = prefs.getInt("session_count", 0)
        val screenViewCount = prefs.getInt("screen_view_count", 0)
        val lastSessionTime = prefs.getLong("last_session_time", 0)

        val currentTime = System.currentTimeMillis()
        val timeSinceLastSession = if (lastSessionTime > 0) currentTime - lastSessionTime else 0

        return mapOf(
            "session_count" to sessionCount.toString(),
            "screen_view_count" to screenViewCount.toString(),
            "time_since_last_session_ms" to timeSinceLastSession.toString(),
            "avg_screens_per_session" to if (sessionCount > 0) {
                (screenViewCount / sessionCount).toString()
            } else "0"
        )
    }

    fun getAppInstallInfo(context: Context): Map<String, String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            mapOf(
                "app_version" to (packageInfo.versionName ?: "unknown")
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}