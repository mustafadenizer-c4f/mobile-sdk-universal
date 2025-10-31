package com.example.surveysdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi

object AppUsageUtils {
    
    fun getAppUsageData(context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Check if we have usage stats permission (API 23+)
        val hasUsageStatsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkUsageStatsPermission(context)
        } else {
            // For API < 23, permissions are granted at installation time
            true
        }
        
        data["has_usage_stats_permission"] = hasUsageStatsPermission.toString()
        
        if (hasUsageStatsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Add your usage stats collection code here with proper API checks
            data["usage_stats_available"] = "true"
        } else {
            data["usage_stats_available"] = "false"
        }
        
        return data
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkUsageStatsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == 
               PackageManager.PERMISSION_GRANTED
    }
    
    // Update other methods to include API level checks
    fun getEnhancedSessionData(context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Add API level checks for all methods that use newer APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Your enhanced session data collection
            data["session_enhanced"] = "true"
        } else {
            data["session_enhanced"] = "false"
        }
        
        return data
    }
    
    fun getAppInstallInfo(context: Context): Map<String, String> {
        val data = mutableMapOf<String, String>()
        
        // Safe to use on all API levels
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        data["app_version"] = packageInfo.versionName ?: "unknown"
        data["app_version_code"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
        
        return data
    }
}