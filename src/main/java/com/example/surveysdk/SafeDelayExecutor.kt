package com.example.surveysdk

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class SafeDelayExecutor {

    companion object {
        private val handlers = ConcurrentHashMap<String, WeakReference<Handler>>()

        fun executeDelayed(
            key: String,
            delayMillis: Long,
            action: () -> Unit
        ): Boolean {
            cancelDelayed(key)

            return try {
                val handler = Handler(Looper.getMainLooper())
                handlers[key] = WeakReference(handler)

                handler.postDelayed({
                    handlers.remove(key)
                    action()
                }, delayMillis)
                true
            } catch (e: Exception) {
                Log.e("SafeDelayExecutor", "Failed to execute delayed action: ${e.message}")
                false
            }
        }

        fun cancelDelayed(key: String) {
            handlers[key]?.get()?.removeCallbacksAndMessages(null)
            handlers.remove(key)
        }

        fun cancelAll() {
            handlers.values.forEach { it.get()?.removeCallbacksAndMessages(null) }
            handlers.clear()
        }

        fun cleanup() {
            cancelAll()
        }
    }
}

class ActivitySafeExecutor(activity: Activity) {
    private val weakActivity = WeakReference(activity)
    private val executorKey = "activity_${activity.hashCode()}"

    fun executeDelayed(
        delayMillis: Long,
        action: (activity: Activity?) -> Unit
    ) {
        SafeDelayExecutor.executeDelayed(executorKey, delayMillis) {
            val targetActivity = weakActivity.get()
            if (targetActivity != null && !targetActivity.isFinishing && !targetActivity.isDestroyed) {
                action(targetActivity)
            } else {
                Log.d("ActivitySafeExecutor", "Activity gone, skipping action")
            }
        }
    }

    fun cancel() {
        SafeDelayExecutor.cancelDelayed(executorKey)
    }
}