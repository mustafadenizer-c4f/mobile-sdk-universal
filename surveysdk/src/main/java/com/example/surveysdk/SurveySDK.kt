package com.example.surveysdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ===== PUBLIC CONFIGURATION DATA CLASSES =====
data class Config(
    val sdkVersion: String = "2.0.0",
    val cacheDurationHours: Long = 24L,
    val surveys: List<SurveyConfig> = emptyList()
)

data class SurveyConfig(
    // Basic Survey Identity
    val surveyId: String,
    val surveyName: String = "",
    val baseUrl: String = "",
    
    // Trigger Settings
    val enableButtonTrigger: Boolean = false,
    val enableScrollTrigger: Boolean = false,
    val enableNavigationTrigger: Boolean = false,
    val enableAppLaunchTrigger: Boolean = false,
    val enableExitTrigger: Boolean = false,
    val enableTabChangeTrigger: Boolean = false,
    
    // Trigger Configuration
    val triggerScreens: Set<String> = emptySet(),
    val triggerTabs: Set<String> = emptySet(),
    val timeDelay: Long = 0L,
    val scrollThreshold: Int = 0,
    val triggerType: String = "instant",
    
    // Display Settings
    val modalStyle: String = "full_screen",
    val animationType: String = "slide_up",
    val backgroundColor: String = "#FFFFFF",
    
    // Targeting & Limits
    val exclusionRules: List<ExclusionRule> = emptyList(),
    val probability: Double = 1.0,
    val maxShowsPerSession: Int = 0,
    val cooldownPeriod: Long = 0L,
    val triggerOnce: Boolean = false,
    val priority: Int = 1,
    
    // Data Collection
    val collectDeviceId: Boolean = false,
    val collectDeviceModel: Boolean = false,
    val collectLocation: Boolean = false,
    val collectAppUsage: Boolean = false,
    val customParams: List<CustomParam> = emptyList()
)

data class CustomParam(
    val name: String,
    val source: ParamSource,
    val key: String? = null,
    val value: String? = null,
    val defaultValue: String? = null
)

enum class ParamSource {
    STORAGE, DEVICE, URL, TIMESTAMP, SESSION, INSTALL_TIME
}

data class ExclusionRule(
    val name: String,
    val source: ExclusionSource,
    val key: String? = null,
    val value: String? = null,
    val operator: ExclusionOperator,
    val matchValue: String,
    val caseSensitive: Boolean = false
)

enum class ExclusionSource {
    STORAGE, DEVICE, URL, TIMESTAMP, SESSION, INSTALL_TIME, APP_USAGE
}

enum class ExclusionOperator {
    EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS,
    GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL,
    STARTS_WITH, ENDS_WITH, IN, NOT_IN
}

// ===== SURVEY SDK MAIN CLASS =====
class SurveySDK private constructor(private val context: Context) {

    // ===== COMPANION OBJECT =====
    companion object {
        @Volatile
        private var instance: SurveySDK? = null

        fun initialize(context: Context, apiKey: String): SurveySDK {
            return instance ?: synchronized(this) {
                instance ?: SurveySDK(context.applicationContext, apiKey).also { sdk ->
                    instance = sdk
                    sdk.initialized = true
                    sdk.loadConfigurationAutomatically()
                }
            }
        }

        fun getInstance(): SurveySDK {
            return instance
                ?: throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }
    }

    // ===== PROPERTIES =====
    private lateinit var config: Config
    private var apiKey: String = "default_key"
    private var configurationLoaded = false
    private val surveyShownCount = mutableMapOf<String, Int>()
    private val lastSurveyTime = mutableMapOf<String, Long>()
    private val triggeredScreens = mutableMapOf<String, MutableSet<String>>()
    private val triggeredTabs = mutableMapOf<String, MutableSet<String>>()
    private val screenTimers = mutableMapOf<String, Long>()
    private val triggeredExits = mutableMapOf<String, MutableSet<String>>()
    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private val delayExecutors = mutableMapOf<String, ActivitySafeExecutor>()
    private var initialized = false

    // ===== SURVEY QUEUE SYSTEM =====
    private val surveyQueue = mutableListOf<Pair<Activity, SurveyConfig>>()
    private var isShowingSurvey = false
    private val queueLock = Any()

    // ===== SERVICES =====
    private val apiService: SurveyApiService by lazy { SurveyApiService(apiKey) }
    private val configCacheManager: ConfigCacheManager by lazy { ConfigCacheManager(context) }

    // ===== CONSTRUCTORS =====
    private constructor(context: Context, apiKey: String) : this(context) {
        this.apiKey = apiKey
        loadFallbackConfig()
    }

    // ===== PUBLIC API METHODS =====
    fun autoSetup(activity: Activity): SurveySDK {
        if (!configurationLoaded) {
            Log.e("SurveySDK", "Configuration not loaded.")
            return this
        }

        Log.d("SurveySDK", "🚀 Starting multi-survey auto-setup with ${config.surveys.size} surveys")

        // Initialize tracking for each survey
        config.surveys.forEach { survey ->
            surveyShownCount[survey.surveyId] = 0
            lastSurveyTime[survey.surveyId] = 0
            triggeredScreens[survey.surveyId] = mutableSetOf()
            triggeredTabs[survey.surveyId] = mutableSetOf()
            triggeredExits[survey.surveyId] = mutableSetOf()
        }

        // Setup triggers
        trackAppStart(activity)
        setupAutoButtonDetection(activity)
        setupScrollTrigger(activity)
        setupAppLaunchTrigger(activity)

        if (config.surveys.any { it.enableTabChangeTrigger }) {
            setupTabChangeTrigger(activity)
            autoDetectNavigation(activity)
        }

        if (config.surveys.any { it.enableExitTrigger }) {
            setupActivityLifecycle(activity)
        }

        trackScreenView(activity)
        Log.d("SurveySDK", "✅ Multi-survey auto-setup completed")
        return this
    }

    fun showSurvey(activity: Activity) {
        showSurveyInternal(activity, null)
    }

    fun showSurveyById(activity: Activity, surveyId: String) {
        val survey = config.surveys.find { it.surveyId == surveyId }
        if (survey != null) {
            showSurveyInternal(activity, survey)
        } else {
            Log.e("SurveySDK", "Survey not found: $surveyId")
        }
    }

    fun isUserExcluded(surveyId: String? = null): Boolean {
        if (!this::config.isInitialized) return false

        if (surveyId != null) {
            val survey = config.surveys.find { it.surveyId == surveyId }
            if (survey != null) {
                return ExclusionRuleEvaluator.shouldExcludeSurvey(context, survey.exclusionRules)
            }
            return false
        }
        return false
    }

    fun resetTriggers() {
        surveyShownCount.clear()
        lastSurveyTime.clear()
        triggeredScreens.clear()
        triggeredTabs.clear()
        triggeredExits.clear()
        screenTimers.clear()
        SafeDelayExecutor.cancelAll()
        delayExecutors.clear()
    }

    fun cleanup() {
        SafeDelayExecutor.cancelAll()
        delayExecutors.clear()
        apiService.cleanup()
        cleanupExitTrigger()
    }

    // ===== CONFIGURATION MANAGEMENT =====
    interface ConfigurationCallback {
        fun onConfigurationLoaded(success: Boolean)
        fun onError(error: String)
    }

    fun fetchConfiguration(callback: ConfigurationCallback? = null) {
        val cachedConfig = configCacheManager.getCachedConfig()
        if (cachedConfig != null) {
            applyConfiguration(cachedConfig, true, callback)
            return
        }

        apiService.fetchConfiguration(object : SurveyApiService.ConfigCallback {
            override fun onConfigLoaded(config: Config?) {
                if (config != null) {
                    configCacheManager.saveConfig(config)
                    applyConfiguration(config, true, callback)
                } else {
                    callback?.onConfigurationLoaded(false)
                }
            }

            override fun onError(error: String) {
                callback?.onError(error)
                callback?.onConfigurationLoaded(false)
            }
        })
    }

    fun isConfigurationLoaded(): Boolean = configurationLoaded

    // ===== NAVIGATION INTEGRATION =====
    fun setupNavigationComponent(navController: NavController, activity: Activity) {
        Log.d("SurveySDK", "=== NAVIGATION SETUP ===")

        var previousScreen: String? = null
        var previousScreenLabel: String? = null

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val screenLabel = destination.label?.toString() ?: "no_label"
            val screenId = destination.id
            val currentScreen = "screen_$screenId"

            Log.d("SurveySDK", "📍 Navigation: $previousScreenLabel → $screenLabel")
            trackScreenView(activity)

            // Handle exit trigger
            if (previousScreen != null && previousScreenLabel != null) {
                handleScreenExit(previousScreen!!, previousScreenLabel!!, currentScreen, activity)
            }

            screenTimers[currentScreen] = System.currentTimeMillis()
            triggerByNavigation(currentScreen, activity)

            previousScreen = currentScreen
            previousScreenLabel = screenLabel
        }
    }

    // ===== TRIGGER SETUP METHODS =====
    fun setupButtonTrigger(button: View, activity: Activity) {
        val surveysWithButtonTrigger = config.surveys.filter { it.enableButtonTrigger }
        if (surveysWithButtonTrigger.isEmpty()) return

        var lastClickTime = 0L
        val debounceTime = 1000L

        button.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < debounceTime) return@setOnClickListener
            lastClickTime = currentTime

            button.isEnabled = false
            button.alpha = 0.5f

            showSurveyInternal(activity)

            Handler(Looper.getMainLooper()).postDelayed({
                button.isEnabled = true
                button.alpha = 1.0f
            }, 2000)
        }
    }

    fun setupButtonTrigger(buttonId: Int, activity: Activity) {
        val button = activity.findViewById<View>(buttonId)
        button?.let { setupButtonTrigger(it, activity) }
    }

    fun triggerByNavigation(screenName: String, activity: Activity) {
        val matchingSurveys = config.surveys.filter { survey ->
            survey.enableNavigationTrigger &&
                    (survey.triggerScreens.isEmpty() || survey.triggerScreens.any {
                        screenName.contains(
                            it,
                            ignoreCase = true
                        )
                    }) &&
                    !(survey.triggerOnce && triggeredScreens[survey.surveyId]?.contains(screenName) == true)
        }

        if (matchingSurveys.isEmpty()) return

        screenTimers[screenName] = System.currentTimeMillis()

        matchingSurveys.forEach { survey ->
            if (survey.triggerType == "delayed" && survey.timeDelay > 0) {
                setupScreenTimeTrigger(screenName, activity, survey)
            } else {
                if (canShowSurvey(survey)) {
                    triggeredScreens[survey.surveyId]?.add(screenName)
                    showSingleSurvey(activity, survey)
                }
            }
        }
    }

    fun triggerByTabChange(tabName: String, activity: Activity) {
        Log.d("SurveySDK", "📍 Tab change triggered: $tabName")

        val matchingSurveys = config.surveys.filter { survey ->
            survey.enableTabChangeTrigger &&
                    (survey.triggerTabs.isEmpty() || survey.triggerTabs.any {
                        tabName.contains(it, ignoreCase = true)
                    }) &&
                    !(survey.triggerOnce && triggeredTabs[survey.surveyId]?.contains(tabName) == true)
        }

        Log.d("SurveySDK", "📊 Found ${matchingSurveys.size} matching surveys for tab: $tabName")

        if (matchingSurveys.isEmpty()) return

        screenTimers[tabName] = System.currentTimeMillis()

        matchingSurveys.forEach { survey ->
            if (survey.triggerType == "delayed" && survey.timeDelay > 0) {
                Log.d("SurveySDK", "⏰ Setting up delayed trigger for ${survey.surveyId}")
                setupTabTimeTrigger(tabName, activity, survey)
            } else {
                if (canShowSurvey(survey)) {
                    Log.d("SurveySDK", "🎯 Immediately showing survey: ${survey.surveyId}")
                    triggeredTabs[survey.surveyId]?.add(tabName)
                    showSingleSurvey(activity, survey)
                } else {
                    Log.d("SurveySDK", "❌ Cannot show survey ${survey.surveyId} - conditions not met")
                }
            }
        }
    }

    // ===== SESSION TRACKING =====
    fun trackAppStart(activity: Activity) {
        ExclusionRuleEvaluator.trackSessionStart(activity)
        Log.d("SurveySDK", "App start tracked")
    }

    fun trackScreenView(activity: Activity) {
        ExclusionRuleEvaluator.trackScreenView(activity)
        Log.d("SurveySDK", "Screen view tracked")
    }

    fun resetSessionData() {
        ExclusionRuleEvaluator.resetSessionData(context)
        Log.d("SurveySDK", "Session data reset")
    }

    fun setSessionData(key: String, value: String) {
        ExclusionRuleEvaluator.setSessionData(key, value)
    }

    // ===== DEBUG METHODS =====
    fun debugSurveyStatus(): String {
        val builder = StringBuilder()
        builder.append("=== MULTI-SURVEY STATUS ===\n")

        // Add queue status
        builder.append("📋 ${getQueueStatus()}\n")
        builder.append("📱 Currently showing survey: $isShowingSurvey\n\n")

        config.surveys.forEach { survey ->
            val shownCount = surveyShownCount[survey.surveyId] ?: 0
            val lastTime = lastSurveyTime[survey.surveyId] ?: 0
            val timeSinceLastSurvey = System.currentTimeMillis() - lastTime
            val cooldownRemaining = survey.cooldownPeriod - timeSinceLastSurvey
            val canShow = canShowSurvey(survey)

            builder.append(
                """
        📊 Survey: ${survey.surveyName} (${survey.surveyId})
        🎯 Priority: ${survey.priority}
        📈 Shown count: $shownCount/${survey.maxShowsPerSession}
        ⏰ Last shown: ${timeSinceLastSurvey / 1000}s ago
        ❄️ Cooldown: ${cooldownRemaining / 1000}s remaining
        🎲 Probability: ${survey.probability}
        ✅ Can show: $canShow
        ---
        """.trimIndent()
            )
        }

        return builder.toString()
    }

    fun getBaseUrlForDebug(): String {
        return if (this::config.isInitialized) {
            "Multi-survey config with ${config.surveys.size} surveys"
        } else {
            "Config not initialized"
        }
    }

    fun getConfigForDebug(): String {
        return if (this::config.isInitialized) {
            "Multi-survey config: ${config.surveys.size} surveys"
        } else {
            "Config not initialized"
        }
    }

    fun isSDKEnabled(): Boolean {
        return configurationLoaded && config.surveys.isNotEmpty()
    }

    // ===== PRIVATE IMPLEMENTATION =====
    private fun loadConfigurationAutomatically() {
        fetchConfiguration(object : ConfigurationCallback {
            override fun onConfigurationLoaded(success: Boolean) {
                configurationLoaded = true
                Log.d(
                    "SurveySDK",
                    if (success) "✅ Config loaded automatically" else "⚠️ Using fallback config"
                )
            }

            override fun onError(error: String) {
                configurationLoaded = true
                Log.w("SurveySDK", "⚠️ Auto-config failed: $error - using fallback")
            }
        })
    }

    private fun showSurveyInternal(activity: Activity, specificSurvey: SurveyConfig? = null) {
        if (!configurationLoaded) {
            Log.w("SurveySDK", "Configuration not ready, using fallback")
            loadFallbackConfig()
        }

        try {
            if (specificSurvey != null) {
                if (canShowSurvey(specificSurvey)) {
                    showSingleSurvey(activity, specificSurvey)
                }
            } else {
                val matchingSurveys = findMatchingSurveys(activity)
                if (matchingSurveys.isNotEmpty()) {
                    val bestSurvey = findHighestPrioritySurvey(matchingSurveys)
                    showSingleSurvey(activity, bestSurvey)
                } else {
                    Log.d("SurveySDK", "No matching surveys found for current context")
                }
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Error showing survey: ${e.message}")
        }
    }

    private fun findMatchingSurveys(activity: Activity): List<SurveyConfig> {
        return config.surveys.filter { survey -> canShowSurvey(survey) }
    }

    private fun findHighestPrioritySurvey(surveys: List<SurveyConfig>): SurveyConfig {
        return surveys.sortedByDescending { it.priority }.first()
    }

    private fun showSingleSurvey(activity: Activity, survey: SurveyConfig) {
        // Add to queue or show immediately
        synchronized(queueLock) {
            if (isShowingSurvey) {
                // Survey already showing - add to priority queue
                surveyQueue.add(activity to survey)
                // Keep queue sorted by priority (highest first)
                surveyQueue.sortByDescending { it.second.priority }
                Log.d("SurveySDK", "📋 Survey queued: ${survey.surveyName} (Priority: ${survey.priority})")
                Log.d("SurveySDK", "📋 Queue size: ${surveyQueue.size}")
                Log.d("SurveySDK", "📋 Current queue: ${surveyQueue.map { it.second.surveyName }}")
                return
            }
            isShowingSurvey = true
        }

        if (survey.baseUrl.isEmpty()) {
            Log.e("SurveySDK", "Base URL empty for survey: ${survey.surveyId}")
            surveyCompleted() // Important: Mark as completed even if failed
            return
        }

        try {
            val url = buildSurveyUrl(survey)
            val allowedDomain = getSurveyDomain(survey.baseUrl)

            Log.d("SurveySDK", "🎯 Showing survey: ${survey.surveyName} (${survey.surveyId})")
            Log.d("SurveySDK", "🎯 Priority: ${survey.priority}, Queue: ${surveyQueue.size} waiting")
            Log.d("SurveySDK", "🎯 Modal style: ${survey.modalStyle}")

            when (survey.modalStyle) {
                "dialog" -> {
                    Log.d("SurveySDK", "🚀 Launching SurveyDialogFragment")
                    showDialogSurvey(activity, url, allowedDomain, survey.animationType)
                }
                "bottom_sheet" -> {
                    Log.d("SurveySDK", "🚀 Launching SurveyBottomSheetFragment")
                    showBottomSheetSurvey(activity, url, allowedDomain, survey.animationType, survey.backgroundColor)
                }
                else -> {
                    Log.d("SurveySDK", "🚀 Launching SurveyFullScreenActivity")
                    showFullScreenSurvey(activity, url, allowedDomain, survey.animationType, survey.backgroundColor)
                }
            }

            recordSurveyShown(survey.surveyId)

        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Error showing survey ${survey.surveyId}: ${e.message}")
            surveyCompleted() // Important: Mark as completed even if failed
        }
    }

    /**
     * Called when a survey is completed/dismissed to show the next one in queue
     */
    fun surveyCompleted() {
        synchronized(queueLock) {
            isShowingSurvey = false
            Log.d("SurveySDK", "✅ Survey completed, checking queue...")

            if (surveyQueue.isNotEmpty()) {
                val (activity, nextSurvey) = surveyQueue.removeAt(0)
                Log.d("SurveySDK", "🔄 Showing next survey from queue: ${nextSurvey.surveyName}")
                Log.d("SurveySDK", "🔄 Remaining queue size: ${surveyQueue.size}")

                // Small delay to ensure previous survey is fully closed
                Handler(Looper.getMainLooper()).postDelayed({
                    showSingleSurvey(activity, nextSurvey)
                }, 500) // 500ms delay between surveys
            } else {
                Log.d("SurveySDK", "📭 Survey queue is empty")
            }
        }
    }

    private fun canShowSurvey(survey: SurveyConfig): Boolean {
        if (!this::config.isInitialized) return false
        if (survey.baseUrl.isEmpty()) return false
        if (ExclusionRuleEvaluator.shouldExcludeSurvey(context, survey.exclusionRules)) return false
        if (Math.random() > survey.probability) return false

        val shownCount = surveyShownCount[survey.surveyId] ?: 0
        if (survey.maxShowsPerSession > 0 && shownCount >= survey.maxShowsPerSession) return false

        val lastTime = lastSurveyTime[survey.surveyId] ?: 0
        if (survey.cooldownPeriod > 0 && (System.currentTimeMillis() - lastTime) < survey.cooldownPeriod) return false

        return true
    }

    private fun recordSurveyShown(surveyId: String) {
        val currentCount = surveyShownCount[surveyId] ?: 0
        surveyShownCount[surveyId] = currentCount + 1
        lastSurveyTime[surveyId] = System.currentTimeMillis()
        Log.d("SurveySDK", "📊 Survey shown: $surveyId, count: ${surveyShownCount[surveyId]}")
    }

    private fun buildSurveyUrl(survey: SurveyConfig): String {
        val params = collectAllData(survey)
        val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "${survey.baseUrl}&$queryString"
    }

    private fun collectAllData(survey: SurveyConfig): Map<String, String> {
        val data = mutableMapOf<String, String>()
        data.putAll(StorageUtils.findUserData(context))

        if (survey.collectDeviceId || survey.collectDeviceModel) {
            data.putAll(DeviceUtils.getDeviceInfo(context))
        }
        if (survey.collectLocation) {
            data.putAll(DeviceUtils.getLocationData(context))
        }
        if (survey.collectAppUsage) {
            data.putAll(AppUsageUtils.getAppUsageData(context))
            data.putAll(AppUsageUtils.getEnhancedSessionData(context))
            data.putAll(AppUsageUtils.getAppInstallInfo(context))
        }

        data.putAll(DeviceUtils.getBatteryInfo(context))
        data.putAll(collectCustomParams(survey))
        data["sdkVersion"] = config.sdkVersion
        data["surveyId"] = survey.surveyId
        data["surveyName"] = survey.surveyName
        data["timestamp"] = System.currentTimeMillis().toString()

        return data
    }

    private fun collectCustomParams(survey: SurveyConfig): Map<String, String> {
        val params = mutableMapOf<String, String>()
        survey.customParams.forEach { param ->
            val paramValue = when (param.source) {
                ParamSource.STORAGE -> StorageUtils.findSpecificData(
                    context,
                    param.key ?: param.name
                ) ?: param.defaultValue

                ParamSource.DEVICE -> getDeviceParam(param.key ?: param.name) ?: param.defaultValue
                ParamSource.TIMESTAMP -> System.currentTimeMillis().toString()
                else -> null
            }
            paramValue?.let { params[param.name] = it }
        }
        return params
    }

    private fun getDeviceParam(key: String): String? {
        return DeviceUtils.getDeviceInfo(context)[key]
    }

    private fun getSurveyDomain(baseUrl: String): String? {
        return try {
            java.net.URI(baseUrl).host
        } catch (e: Exception) {
            null
        }
    }

    private fun applyConfiguration(
        config: Config,
        fromCache: Boolean,
        callback: ConfigurationCallback?
    ) {
        this.config = config
        configurationLoaded = true
        callback?.onConfigurationLoaded(true)
        Log.d("SurveySDK", "✅ Configuration loaded with ${config.surveys.size} surveys")
    }

    private fun loadFallbackConfig() {
        config = DefaultConfig.getFallbackConfig()
        configurationLoaded = true
        Log.d("SurveySDK", "📋 Fallback configuration loaded with ${config.surveys.size} surveys")
    }

    /**
     * Get current queue status for debugging
     */
    fun getQueueStatus(): String {
        synchronized(queueLock) {
            return if (surveyQueue.isEmpty()) {
                "Queue: Empty"
            } else {
                "Queue: ${surveyQueue.size} surveys - " +
                        surveyQueue.joinToString { "${it.second.surveyName} (P:${it.second.priority})" }
            }
        }
    }

    /**
     * Clear all pending surveys from queue
     */
    fun clearSurveyQueue() {
        synchronized(queueLock) {
            surveyQueue.clear()
            Log.d("SurveySDK", "🧹 Survey queue cleared")
        }
    }

    /**
     * Check if currently showing a survey
     */
    fun isShowingSurvey(): Boolean {
        synchronized(queueLock) {
            return isShowingSurvey
        }
    }

    // ===== TRIGGER IMPLEMENTATIONS =====
    private fun setupAutoButtonDetection(activity: Activity) {
        val surveysWithButtonTrigger = config.surveys.filter { it.enableButtonTrigger }
        if (surveysWithButtonTrigger.isEmpty()) return

        val surveyButtonIds = listOf("survey_button", "take_survey", "feedback_button", "rate_app")
        surveyButtonIds.forEach { buttonId ->
            val resourceId = activity.resources.getIdentifier(buttonId, "id", activity.packageName)
            if (resourceId != 0) {
                setupButtonTrigger(resourceId, activity)
            }
        }
    }

    private fun setupScrollTrigger(activity: Activity) {
        val surveysWithScrollTrigger =
            config.surveys.filter { it.enableScrollTrigger && it.scrollThreshold > 0 }
        if (surveysWithScrollTrigger.isEmpty()) return

        val contentView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        findAndSetupScrollListener(contentView, activity)
    }

    private fun setupAppLaunchTrigger(activity: Activity) {
        val surveysWithAppLaunch = config.surveys.filter { it.enableAppLaunchTrigger }
        surveysWithAppLaunch.forEach { survey ->
            if (survey.triggerType == "delayed" && survey.timeDelay > 0) {
                val executor = ActivitySafeExecutor(activity)
                delayExecutors["app_launch_${survey.surveyId}"] = executor
                executor.executeDelayed(survey.timeDelay) { safeActivity ->
                    safeActivity?.let { if (canShowSurvey(survey)) showSingleSurvey(it, survey) }
                }
            } else if (canShowSurvey(survey)) {
                showSingleSurvey(activity, survey)
            }
        }
    }

    private fun setupTabChangeTrigger(activity: Activity) {
        // Implementation for tab change detection
    }

    private fun setupActivityLifecycle(activity: Activity) {
        activityLifecycleCallbacks = createActivityLifecycleCallbacks()
        activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun setupScreenTimeTrigger(
        screenName: String,
        activity: Activity,
        survey: SurveyConfig
    ) {
        if (survey.triggerType != "delayed" || survey.timeDelay <= 0) return

        val executorKey = "screen_${screenName}_${survey.surveyId}"
        val executor = ActivitySafeExecutor(activity)
        delayExecutors[executorKey] = executor

        executor.executeDelayed(survey.timeDelay) { safeActivity ->
            val screenEntryTime = screenTimers[screenName]
            safeActivity?.let {
                if (screenEntryTime != null && canShowSurvey(survey)) {
                    triggeredScreens[survey.surveyId]?.add(screenName)
                    showSingleSurvey(it, survey)
                }
            }
        }
    }

    private fun setupTabTimeTrigger(tabName: String, activity: Activity, survey: SurveyConfig) {
        if (survey.triggerType != "delayed" || survey.timeDelay <= 0) return

        val executorKey = "tab_${tabName}_${survey.surveyId}"
        val executor = ActivitySafeExecutor(activity)
        delayExecutors[executorKey] = executor

        executor.executeDelayed(survey.timeDelay) { safeActivity ->
            val tabEntryTime = screenTimers[tabName]
            safeActivity?.let {
                if (tabEntryTime != null && canShowSurvey(survey)) {
                    triggeredTabs[survey.surveyId]?.add(tabName)
                    showSingleSurvey(it, survey)
                }
            }
        }
    }

    private fun handleScreenExit(
        previousScreen: String,
        previousScreenLabel: String,
        newScreen: String,
        activity: Activity
    ) {
        val matchingSurveys = config.surveys.filter { survey ->
            survey.enableExitTrigger &&
                    (survey.triggerScreens.isEmpty() || survey.triggerScreens.any {
                        previousScreenLabel.contains(
                            it,
                            ignoreCase = true
                        )
                    }) &&
                    !(survey.triggerOnce && triggeredExits[survey.surveyId]?.contains(
                        previousScreenLabel
                    ) == true)
        }

        matchingSurveys.forEach { survey ->
            if (survey.triggerType == "delayed" && survey.timeDelay > 0) {
                val executor = ActivitySafeExecutor(activity)
                delayExecutors["exit_${previousScreen}_${survey.surveyId}"] = executor
                executor.executeDelayed(survey.timeDelay) { safeActivity ->
                    safeActivity?.let {
                        if (canShowSurvey(survey)) {
                            triggeredExits[survey.surveyId]?.add(previousScreenLabel)
                            showSingleSurvey(it, survey)
                        }
                    }
                }
            } else {
                if (canShowSurvey(survey)) {
                    triggeredExits[survey.surveyId]?.add(previousScreenLabel)
                    showSingleSurvey(activity, survey)
                }
            }
        }
    }

    private fun findAndSetupScrollListener(viewGroup: ViewGroup, activity: Activity) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is ScrollView, is NestedScrollView -> {
                    setupScrollViewDetection(child as View, activity)
                    return
                }

                is RecyclerView -> {
                    setupRecyclerViewDetection(child, activity)
                    return
                }

                is ViewGroup -> findAndSetupScrollListener(child, activity)
            }
        }
    }

    private fun setupScrollViewDetection(scrollView: View, activity: Activity) {
        var triggered = false
        var lastScrollTime = 0L
        val scrollDebounceMs = 1000L

        Log.d("SurveySDK", "🔄 Setting up ScrollView detection")

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastScrollTime < scrollDebounceMs) return@addOnScrollChangedListener
            lastScrollTime = currentTime

            try {
                val scrollY = when (scrollView) {
                    is ScrollView -> scrollView.scrollY
                    is NestedScrollView -> scrollView.scrollY
                    else -> 0
                }

                Log.d("SurveySDK", "📜 ScrollView - ScrollY: $scrollY")

                val matchingSurveys = config.surveys.filter { survey ->
                    val meetsThreshold = scrollY >= survey.scrollThreshold
                    val canShow = canShowSurvey(survey)

                    Log.d("SurveySDK", "📊 Survey ${survey.surveyId} - Threshold: ${survey.scrollThreshold}, Meets: $meetsThreshold, CanShow: $canShow")

                    survey.enableScrollTrigger && !triggered && meetsThreshold && canShow
                }

                if (matchingSurveys.isNotEmpty() && !triggered) {
                    Log.d("SurveySDK", "🎯 Scroll trigger FIRED at $scrollY")
                    val bestSurvey = findHighestPrioritySurvey(matchingSurveys)
                    showSingleSurvey(activity, bestSurvey)
                    triggered = true
                }
            } catch (e: Exception) {
                Log.e("SurveySDK", "❌ ScrollView detection error: ${e.message}")
            }
        }
    }

    private fun setupRecyclerViewDetection(recyclerView: RecyclerView, activity: Activity) {
        var triggered = false
        var lastScrollTime = 0L
        val scrollDebounceMs = 1000L

        Log.d("SurveySDK", "🔄 Setting up RecyclerView detection")

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScrollTime < scrollDebounceMs) return
                lastScrollTime = currentTime

                try {
                    val scrollY = recyclerView.computeVerticalScrollOffset()
                    Log.d("SurveySDK", "📜 RecyclerView - ScrollY: $scrollY, dy: $dy")

                    val matchingSurveys = config.surveys.filter { survey ->
                        val meetsThreshold = scrollY >= survey.scrollThreshold
                        val canShow = canShowSurvey(survey)

                        Log.d("SurveySDK", "📊 Survey ${survey.surveyId} - Threshold: ${survey.scrollThreshold}, Meets: $meetsThreshold, CanShow: $canShow")

                        survey.enableScrollTrigger && !triggered && meetsThreshold && canShow
                    }

                    if (matchingSurveys.isNotEmpty() && !triggered) {
                        Log.d("SurveySDK", "🎯 RecyclerView scroll trigger FIRED at $scrollY")
                        val bestSurvey = findHighestPrioritySurvey(matchingSurveys)
                        showSingleSurvey(activity, bestSurvey)
                        triggered = true
                    }
                } catch (e: Exception) {
                    Log.e("SurveySDK", "❌ RecyclerView scroll detection error: ${e.message}")
                }
            }
        })
    }

    private fun autoDetectNavigation(activity: Activity) {
        try {
            Log.d("SurveySDK", "🔄 Starting auto navigation detection...")

            // Method 1: Try BottomNavigationView detection
            val bottomNavView = findBottomNavigationView(activity)
            if (bottomNavView != null) {
                Log.d("SurveySDK", "✅ Found BottomNavigationView")
                setupSimpleBottomNavListener(bottomNavView, activity)
                return
            }

            // Method 2: Try ViewPager detection
            findAndSetupViewPager(activity)

            // Method 3: Try Navigation Component detection
            setupNavigationComponentDetection(activity)

            Log.d("SurveySDK", "✅ Auto navigation detection completed")

        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Auto navigation detection failed: ${e.message}")
        }
    }

    // ADD THIS SIMPLE BOTTOM NAV LISTENER (no reflection):
    private fun setupSimpleBottomNavListener(navView: View, activity: Activity) {
        try {
            Log.d("SurveySDK", "🔄 Setting up simple bottom nav listener...")

            // Cast to BottomNavigationView directly
            if (navView is com.google.android.material.bottomnavigation.BottomNavigationView) {
                navView.setOnNavigationItemSelectedListener { menuItem ->
                    val screenName = when (menuItem.itemId) {
                        // Common BottomNavigationView IDs
                        android.R.id.home -> "home"
                        else -> {
                            // Use title as screen name
                            val title = menuItem.title?.toString()?.toLowerCase() ?: "tab_${menuItem.itemId}"
                            Log.d("SurveySDK", "📍 Bottom nav clicked: $title")
                            title
                        }
                    }

                    Log.d("SurveySDK", "📍 BottomNavigationView: $screenName clicked")

                    // Trigger both navigation and tab change
                    triggerByNavigation(screenName, activity)
                    triggerByTabChange(screenName, activity)

                    // Don't consume the event - let normal navigation happen
                    false
                }
                Log.d("SurveySDK", "✅ Simple bottom nav listener setup complete")
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Simple bottom nav setup failed: ${e.message}")
        }
    }

    // ADD NAVIGATION COMPONENT DETECTION:
    private fun setupNavigationComponentDetection(activity: Activity) {
        try {
            if (activity is androidx.fragment.app.FragmentActivity) {
                // Look for NavHostFragment
                val navHostFragment = activity.supportFragmentManager.fragments.find {
                    it is androidx.navigation.fragment.NavHostFragment
                } as? androidx.navigation.fragment.NavHostFragment

                navHostFragment?.let { navHost ->
                    val navController = navHost.navController
                    Log.d("SurveySDK", "✅ Found Navigation Component")

                    navController.addOnDestinationChangedListener { _, destination, _ ->
                        val screenName = destination.label?.toString() ?: "screen_${destination.id}"
                        Log.d("SurveySDK", "📍 Navigation Component: $screenName")

                        triggerByNavigation(screenName, activity)
                        triggerByTabChange(screenName, activity)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Navigation component setup failed: ${e.message}")
        }
    }

    private fun findBottomNavigationView(activity: Activity): View? {
        return findViewByClassName(
            activity.window.decorView,
            "com.google.android.material.bottomnavigation.BottomNavigationView"
        )
    }

    private fun findViewByClassName(root: View, className: String): View? {
        if (root::class.java.name == className) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findViewByClassName(root.getChildAt(i), className)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findAndSetupViewPager(activity: Activity) {
        val viewPager =
            findViewByClassName(activity.window.decorView, "androidx.viewpager.widget.ViewPager")
                ?: findViewByClassName(
                    activity.window.decorView,
                    "androidx.viewpager2.widget.ViewPager2"
                )

        if (viewPager != null) {
            setupViewPagerListener(viewPager, activity)
        }
    }

    private fun setupNavigationListener(navView: View, activity: Activity) {
        try {
            val method = navView.javaClass.getMethod(
                "setOnNavigationItemSelectedListener",
                Class.forName("com.google.android.material.bottomnavigation.BottomNavigationView\$OnNavigationItemSelectedListener")
            )

            val listener = createNavigationListener(activity)
            method.invoke(navView, listener)
        } catch (e: Exception) {
            Log.e("SurveySDK", "Failed to setup navigation listener: ${e.message}")
        }
    }

    private fun createNavigationListener(activity: Activity): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            activity.classLoader,
            arrayOf(Class.forName("com.google.android.material.bottomnavigation.BottomNavigationView\$OnNavigationItemSelectedListener"))
        ) { _, method, args ->
            if (method.name == "onNavigationItemSelected") {
                val menuItem = args!![0] as android.view.MenuItem
                val screenName =
                    menuItem.title?.toString()?.toLowerCase() ?: "nav_item_${menuItem.itemId}"
                triggerByTabChange(screenName, activity)
                true
            } else {
                false
            }
        }
    }

    private fun setupViewPagerListener(viewPager: Any, activity: Activity) {
        try {
            if (viewPager::class.java.name == "androidx.viewpager.widget.ViewPager") {
                val method = viewPager.javaClass.getMethod(
                    "addOnPageChangeListener",
                    Class.forName("androidx.viewpager.widget.ViewPager\$OnPageChangeListener")
                )

                val listener = createViewPagerListener(activity)
                method.invoke(viewPager, listener)
            }
        } catch (e: Exception) {
            Log.e("SurveySDK", "Failed to setup ViewPager listener: ${e.message}")
        }
    }

    private fun createViewPagerListener(activity: Activity): Any {
        return java.lang.reflect.Proxy.newProxyInstance(
            activity.classLoader,
            arrayOf(Class.forName("androidx.viewpager.widget.ViewPager\$OnPageChangeListener"))
        ) { _, method, args ->
            if (method.name == "onPageSelected") {
                val position = args!![0] as Int
                val screenName = "tab_$position"
                triggerByTabChange(screenName, activity)
            }
            false
        }
    }

    private fun createActivityLifecycleCallbacks(): Application.ActivityLifecycleCallbacks {
        return object : Application.ActivityLifecycleCallbacks {
            private var activitiesStarted = 0

            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: android.os.Bundle?
            ) {
            }

            override fun onActivityStarted(activity: Activity) {
                activitiesStarted++
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: android.os.Bundle
            ) {
            }

            override fun onActivityDestroyed(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                activitiesStarted--
                if (activitiesStarted == 0) {
                    handleAppExit(activity)
                }
            }
        }
    }

    private fun handleAppExit(activity: Activity) {
        val matchingSurveys = config.surveys.filter { survey ->
            survey.enableExitTrigger &&
                    !(survey.triggerOnce && triggeredExits[survey.surveyId]?.contains("app_exit") == true)
        }

        matchingSurveys.forEach { survey ->
            if (survey.triggerType == "delayed" && survey.timeDelay > 0) {
                val executor = ActivitySafeExecutor(activity)
                delayExecutors["app_exit_${survey.surveyId}"] = executor
                executor.executeDelayed(survey.timeDelay) { safeActivity ->
                    safeActivity?.let {
                        if (canShowSurvey(survey)) {
                            triggeredExits[survey.surveyId]?.add("app_exit")
                            showSingleSurvey(it, survey)
                        }
                    }
                }
            } else {
                if (canShowSurvey(survey)) {
                    triggeredExits[survey.surveyId]?.add("app_exit")
                    showSingleSurvey(activity, survey)
                }
            }
        }
    }

    private fun cleanupExitTrigger() {
        activityLifecycleCallbacks?.let {
            try {
                val application = context.applicationContext as? Application
                application?.unregisterActivityLifecycleCallbacks(it)
                activityLifecycleCallbacks = null
            } catch (e: Exception) {
                Log.e("SurveySDK", "Error cleaning up exit trigger")
            }
        }
    }

    private fun getCurrentScreen(): String? {
        return screenTimers.entries.lastOrNull {
            System.currentTimeMillis() - it.value < 30000 // 30 seconds
        }?.key
    }

    // ===== SURVEY DISPLAY METHODS =====
    // In SurveySDK.kt - UPDATE these methods:

    // In SurveySDK.kt - ADD these methods:

    private fun showDialogSurvey(
        activity: Activity,
        url: String,
        allowedDomain: String?,
        animationType: String
    ) {
        try {
            Log.d("SurveySDK", "🚀 Launching SurveyDialogFragment")

            // Use DialogFragment for true transparency
            val dialogFragment = SurveyDialogFragment.newInstance(
                surveyUrl = url,
                backgroundColor = "#FFFFFF",
                animationType = animationType,
                allowedDomain = allowedDomain
            )

            // Show as fragment - this will be truly transparent
            if (activity is androidx.fragment.app.FragmentActivity) {
                dialogFragment.show(activity.supportFragmentManager, "SurveyDialogFragment")
            } else {
                Log.e(
                    "SurveySDK",
                    "❌ Activity is not FragmentActivity, cannot show dialog fragment"
                )
                // Fallback to activity approach
                showDialogActivityFallback(activity, url, allowedDomain, animationType)
            }

        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Dialog fragment failed: ${e.message}")
            // Fallback to activity approach
            showDialogActivityFallback(activity, url, allowedDomain, animationType)
        }
    }

    private fun showDialogActivityFallback(
        activity: Activity,
        url: String,
        allowedDomain: String?,
        animationType: String
    ) {
        try {
            Log.d("SurveySDK", "🔄 Using dialog activity fallback")
            val intent = Intent(activity, SurveyDialogActivity::class.java).apply {
                putExtra("SURVEY_URL", url)
                putExtra("BACKGROUND_COLOR", "#FFFFFF")
                putExtra("ANIMATION_TYPE", animationType)
                putExtra("ALLOWED_DOMAIN", allowedDomain)
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(0, 0)
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Dialog activity fallback also failed: ${e.message}")
        }
    }

    private fun showBottomSheetSurvey(
        activity: Activity,
        url: String,
        allowedDomain: String?,
        animationType: String,
        backgroundColor: String
    ) {
        try {
            Log.d("SurveySDK", "🚀 Launching SurveyBottomSheetFragment")

            if (activity is androidx.fragment.app.FragmentActivity) {
                val bottomSheetFragment = SurveyBottomSheetFragment.newInstance(
                    surveyUrl = url,
                    backgroundColor = backgroundColor,
                    allowedDomain = allowedDomain
                )
                bottomSheetFragment.show(
                    activity.supportFragmentManager,
                    "SurveyBottomSheetFragment"
                )
            } else {
                Log.e("SurveySDK", "❌ Activity is not FragmentActivity, using activity fallback")
                showBottomSheetActivityFallback(
                    activity,
                    url,
                    allowedDomain,
                    animationType,
                    backgroundColor
                )
            }

        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Bottom sheet fragment failed: ${e.message}")
            showBottomSheetActivityFallback(
                activity,
                url,
                allowedDomain,
                animationType,
                backgroundColor
            )
        }
    }

    private fun showBottomSheetActivityFallback(
        activity: Activity,
        url: String,
        allowedDomain: String?,
        animationType: String,
        backgroundColor: String
    ) {
        try {
            val intent = Intent(activity, SurveyBottomSheetActivity::class.java).apply {
                putExtra("SURVEY_URL", url)
                putExtra("BACKGROUND_COLOR", backgroundColor)
                putExtra("ANIMATION_TYPE", animationType)
                putExtra("ALLOWED_DOMAIN", allowedDomain)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Bottom sheet activity fallback failed: ${e.message}")
        }
    }

    private fun showFullScreenSurvey(
        activity: Activity,
        url: String,
        allowedDomain: String?,
        animationType: String,
        backgroundColor: String
    ) {
        try {
            Log.d("SurveySDK", "🚀 Launching SurveyFullScreenActivity")
            val intent = Intent(activity, SurveyFullScreenActivity::class.java).apply {
                putExtra("SURVEY_URL", url)
                putExtra("BACKGROUND_COLOR", backgroundColor)
                putExtra("ANIMATION_TYPE", animationType)
                putExtra("ALLOWED_DOMAIN", allowedDomain)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SurveySDK", "❌ Full screen survey failed: ${e.message}")
        }
    }
}
