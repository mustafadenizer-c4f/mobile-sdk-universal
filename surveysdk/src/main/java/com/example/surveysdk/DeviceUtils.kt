package com.example.surveysdk

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.location.Location
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log

object DeviceUtils {

    fun getDeviceInfo(context: Context): Map<String, String> {
        return mapOf(
            "deviceID" to getDeviceId(context),
            "deviceModel" to Build.MODEL,
            "platform" to "Android",
            "osVersion" to Build.VERSION.RELEASE,
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "sdkVersion" to Build.VERSION.SDK_INT.toString(),
            "screenResolution" to getScreenResolution(context),
            "networkType" to getNetworkType(context),
            "timezone" to java.util.TimeZone.getDefault().id,
            "locale" to java.util.Locale.getDefault().toString(),
            "installTime" to getInstallTime(context),
            "appVersion" to getAppVersion(context)
        )
    }

    private fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getScreenResolution(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }

    private fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.typeName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getInstallTime(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.firstInstallTime.toString()
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun getLocationData(context: Context): Map<String, String> {
        return try {
            // Check location permissions without ContextCompat
            val hasFineLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                return mapOf("location_permission" to "denied")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Try to get last known location from different providers
            var bestLocation: Location? = null
            var bestProvider: String = "unknown"

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                try {
                    if (locationManager.isProviderEnabled(provider)) {
                        val location = locationManager.getLastKnownLocation(provider)
                        if (location != null && (bestLocation == null ||
                                    location.accuracy > bestLocation!!.accuracy)) {
                            bestLocation = location
                            bestProvider = provider
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("DeviceUtils", "Location permission denied for provider: $provider")
                } catch (e: Exception) {
                    Log.w("DeviceUtils", "Error getting location from $provider: ${e.message}")
                }
            }

            if (bestLocation != null) {
                mapOf(
                    "latitude" to bestLocation.latitude.toString(),
                    "longitude" to bestLocation.longitude.toString(),
                    "location_accuracy" to bestLocation.accuracy.toString(),
                    "location_time" to bestLocation.time.toString(),
                    "location_provider" to bestProvider
                )
            } else {
                mapOf("location_available" to "false")
            }
        } catch (e: Exception) {
            Log.e("DeviceUtils", "Error getting location: ${e.message}")
            mapOf("location_error" to (e.message ?: "unknown"))
        }
    }

    fun getBatteryInfo(context: Context): Map<String, String> {
            return try {
                val batteryIntent = context.registerReceiver(null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))

                val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                val plugged = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1

                val batteryPercent = if (level != -1 && scale != -1) {
                    (level * 100 / scale.toFloat()).toInt().toString()
                } else {
                    "unknown"
                }

                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL

                val chargeType = when (plugged) {
                    android.os.BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                    android.os.BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                    android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                    else -> "none"
                }

                mapOf(
                    "battery_level" to batteryPercent,
                    "is_charging" to isCharging.toString(),
                    "charge_type" to chargeType
                )
            } catch (e: Exception) {
                Log.e("DeviceUtils", "Error getting battery info: ${e.message}")
                emptyMap()
            }
        }

}