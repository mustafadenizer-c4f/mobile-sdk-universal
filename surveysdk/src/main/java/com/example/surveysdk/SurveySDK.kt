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
import android.content.ActivityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper

// ===== PUBLIC CONFIGURATION DATA CLASSES =====
data class Config(
    val baseUrl: String = "",
    val sdkVersion: String = "1.0.0",
    val enableButtonTrigger: Boolean = false,
    val enableScrollTrigger: Boolean = false,
    val enableNavigationTrigger: Boolean = false,
    val enableAppLaunchTrigger: Boolean = false,
    val enableExitTrigger: Boolean = false,
    val enableTabChangeTrigger: Boolean = false,
    val timeDelay: Long = 0L,
    val scrollThreshold: Int = 0,
    val navigationScreens: Set<String> = emptySet(),
    val tabNames: Set<String> = emptySet(),
    val triggerType: String = "instant",
    val modalStyle: String = "full_screen",
    val animationType: String = "slide_up",
    val backgroundColor: String = "#FFFFFF",
    val collectDeviceId: Boolean = false,
    val collectDeviceModel: Boolean = false,
    val collectLocation: Boolean = false,
    val collectAppUsage: Boolean = false,
    val customParams: List<CustomParam> = emptyList(),
    val probability: Double = 1.0,
    val maxSurveysPerSession: Int = 0,
    val cooldownPeriod: Long = 0L,
    val cacheDurationHours: Long = 24L,
    val triggerOnce: Boolean = false,
    val exclusionRules: List<ExclusionRule> = emptyList()
)

data class CustomParam(
    val name: String,
    val source: ParamSource,
    val key: String? = null,
    val value: String? = null,
    val defaultValue: String? = null
)

enum class ParamSource {
    STORAGE, DEVICE, URL, TIMESTAMP, SESSION
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
        @Volatile private var instance: SurveySDK? = null

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
            return instance ?: throw IllegalStateException("SurveySDK not initialized. Call initialize() first.")
        }

    }

    // ===== PROPERTIES =====
    private lateinit var config: Config
    private var apiKey: String = "default_key"
    private var configurationLoaded = false
    private var surveyShownCount = 0
    private var lastSurveyTime: Long = 0
    private val triggeredScreens = mutableSetOf<String>()
    private val triggeredTabs = mutableSetOf<String>()
    private val screenTimers = mutableMapOf<String, Long>()
    private var appStartTime: Long = 0
    private val triggeredExits = mutableSetOf<String>()
    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private val delayExecutors = mutableMapOf<String, ActivitySafeExecutor>()
    private var initialized = false
    private var apiConfigurationFailed = false

    // ===== SERVICES =====
    private val apiService: SurveyApiService by lazy { SurveyApiService(apiKey) }
    private val configCacheManager: ConfigCacheManager by lazy { ConfigCacheManager(context) }

    // ===== CONSTRUCTORS =====
    private constructor(context: Context, apiKey: String) : this(context) {
        this.apiKey = apiKey
        loadFallbackConfig()
    }

    // ===== PUBLIC API METHODS =====

        /**
         * 🔥 ENHANCED: One-stop autoSetup that handles EVERYTHING automatically
         * Clients only need this one line after initialization
         */
        fun autoSetup(activity: Activity): SurveySDK {
            if (!configurationLoaded) {
                Log.e("SurveySDK", "Configuration not loaded.")
                return this
            }

            Log.d("SurveySDK", "🚀 Starting reliable auto-setup...")

            // ✅ SESSION TRACKING - Track app start automatically
            trackAppStart(activity)

            // ✅ BUTTON DETECTION - Auto-find survey buttons (RELIABLE)
            if (config.enableButtonTrigger) {
                setupAutoButtonDetection(activity)
            }

            // ✅ SCROLL TRIGGERS - Auto-detect scrollable views (RELIABLE)
            if (config.enableScrollTrigger) {
                setupScrollTrigger(activity)
            }

            // ✅ APP LAUNCH TRIGGERS (RELIABLE)
            if (config.enableAppLaunchTrigger) {
                setupAppLaunchTrigger(activity)
            }

            // ✅ TAB CHANGE TRIGGERS - Auto-detect ViewPagers, BottomNavigation (RELIABLE)
            if (config.enableTabChangeTrigger) {
                setupTabChangeTrigger(activity)
                autoDetectNavigation(activity) // This uses reflection and works reliably
            }

            // ✅ EXIT TRIGGERS & LIFECYCLE - Automatic activity lifecycle tracking (RELIABLE)
            if (config.enableExitTrigger) {
                setupActivityLifecycle(activity)
            }

            // ✅ SIMPLE SCREEN TRACKING - Track current activity (RELIABLE)
            trackScreenView(activity)

            Log.d("SurveySDK", "✅ Auto-setup completed - All reliable features enabled")
            Log.d("SurveySDK", "💡 For NavigationComponent, call setupNavigationComponent() manually")

            return this
        }

    /**
     * Auto-load configuration during initialization
     */
    private fun loadConfigurationAutomatically() {
        fetchConfiguration(object : ConfigurationCallback {
            override fun onConfigurationLoaded(success: Boolean) {
                configurationLoaded = true
                Log.d("SurveySDK", if (success) "✅ Config loaded automatically" else "⚠️ Using fallback config")
            }
            override fun onError(error: String) {
                configurationLoaded = true
                Log.w("SurveySDK", "⚠️ Auto-config failed: $error - using fallback")
            }
        })
    }

    private fun setupAutoButtonDetection(activity: Activity) {
        if (!config.enableButtonTrigger) return

        // Common survey button IDs that clients should use
        val surveyButtonIds = listOf(
            "survey_button", "take_survey", "feedback_button",
            "rate_app", "customer_feedback", "survey_btn", "btn_survey"
        )

        surveyButtonIds.forEach { buttonId ->
            val resourceId = activity.resources.getIdentifier(buttonId, "id", activity.packageName)
            if (resourceId != 0) {
                val button = activity.findViewById<View>(resourceId)
                if (button != null) {
                    setupButtonTrigger(button, activity)
                    Log.d("SurveySDK", "✅ Auto-detected button: $buttonId")
                } else {
                    //Log.d("SurveySDK", "❌ Button not found: $buttonId")
                }
                setupButtonTrigger(resourceId, activity)
            }
        }
    }

    fun setupNavigationComponent(navController: NavController, activity: Activity) {
        Log.d("SurveySDK", "=== NAVIGATION SETUP ===")

        var previousScreen: String? = null
        var previousScreenLabel: String? = null

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val screenLabel = destination.label?.toString() ?: "no_label"
            val screenId = destination.id
            val currentScreen = "screen_$screenId"

            Log.d("SurveySDK", "📍 Navigation: $previousScreenLabel → $screenLabel")

            // ✅ Track screen view for each navigation
            trackScreenView(activity)

            // Handle exit trigger if enabled
            if (previousScreen != null && previousScreenLabel != null && config.enableExitTrigger) {
                if (config.navigationScreens.isNotEmpty()) {
                    Log.d("SurveySDK", "🔍 Checking exit trigger for: $previousScreenLabel")
                    handleScreenExit(previousScreen!!, previousScreenLabel!!, currentScreen, activity)
                }
            }

            screenTimers[currentScreen] = System.currentTimeMillis()

            // Handle navigation triggers if enabled
            if (config.enableNavigationTrigger) {
                val isConfiguredScreen = config.navigationScreens.isEmpty() ||
                        config.navigationScreens.any { screenLabel.contains(it, ignoreCase = true) }

                Log.d("SurveySDK", "🔍 Navigation check: $screenLabel -> $isConfiguredScreen")

                if (isConfiguredScreen) {
                    if (config.triggerType == "delayed" && config.timeDelay > 0) {
                        Log.d("SurveySDK", "⏰ Setting up delayed trigger for: $screenLabel")
                        setupScreenTimeTrigger(currentScreen, activity)
                    } else {
                        Log.d("SurveySDK", "🎯 Immediate navigation trigger for: $screenLabel")
                        triggerByNavigation(currentScreen, activity)
                    }
                }
            }

            previousScreen = currentScreen
            previousScreenLabel = screenLabel
        }
    }

    fun setupButtonTrigger(button: View, activity: Activity) {
        if (!config.enableButtonTrigger) return

        var lastClickTime = 0L
        val debounceTime = 1000L

        button.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < debounceTime) {
                Log.d("SurveySDK", "🔄 Double click prevented")
                return@setOnClickListener
            }
            lastClickTime = currentTime

            // Visual feedback
            button.isEnabled = false
            button.alpha = 0.5f

            showSurveyIfAllowed(activity)

            // Re-enable after delay
            Handler(Looper.getMainLooper()).postDelayed({
                button.isEnabled = true
                button.alpha = 1.0f
            }, 2000)
        }
    }

    fun setupButtonTrigger(buttonId: Int, activity: Activity) {
        if (!config.enableButtonTrigger) return

        var lastClickTime = 0L
        val debounceTime = 1000L

        val button = activity.findViewById<View>(buttonId)
        button?.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < debounceTime) {
                Log.d("SurveySDK", "🔄 Double click prevented")
                return@setOnClickListener
            }
            lastClickTime = currentTime

            // Visual feedback
            button.isEnabled = false
            button.alpha = 0.5f

            showSurveyIfAllowed(activity)

            // Re-enable after delay
            Handler(Looper.getMainLooper()).postDelayed({
                button.isEnabled = true
                button.alpha = 1.0f
            }, 2000)
        }
    }

    fun showSurvey(activity: Activity) {
        if (isUserExcluded()) {
            Log.d("SurveySDK", "Survey blocked - user excluded")
            return
        }
        showSurveyInternal(activity)
    }

    fun isUserExcluded(): Boolean {
        return if (this::config.isInitialized) {
            Log.d("SurveySDK", "=== EXCLUSION RULES DEBUG ===")
            Log.d("SurveySDK", "Number of rules: ${config.exclusionRules.size}")
            config.exclusionRules.forEach { rule ->
                Log.d("SurveySDK", "Rule: ${rule.name}, Source: ${rule.source}, Key: ${rule.key}, Value: ${rule.value}")
            }
            ExclusionRuleEvaluator.shouldExcludeSurvey(context, config.exclusionRules)
        } else {
            false
        }
    }

    fun resetTriggers() {
        surveyShownCount = 0
        lastSurveyTime = 0
        triggeredScreens.clear()
        triggeredTabs.clear()
        triggeredExits.clear()
        screenTimers.clear()
        appStartTime = 0
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

    // ===== PRIVATE IMPLEMENTATION =====
    private fun showSurveyIfAllowed(activity: Activity) {
        if (isUserExcluded()) {
            Log.d("SurveySDK", "Manual trigger blocked - user excluded")
        } else {
            showSurveyInternal(activity)
        }
    }

    private fun showSurveyInternal(activity: Activity) {
        if (!configurationLoaded) {
            Log.w("SurveySDK", "Configuration not ready, using fallback")
            loadFallbackConfig()
        }

        if (config.baseUrl.isEmpty()) {
            Log.e("SurveySDK", "Base URL empty")
            return
        }

        try {
            val url = buildSurveyUrl()
            val allowedDomain = getSurveyDomain()
            val targetActivity = getTargetActivity()

            val intent = Intent(activity, targetActivity).apply {
                putExtra("SURVEY_URL", url)
                putExtra("BACKGROUND_COLOR", config.backgroundColor)
                putExtra("ANIMATION_TYPE", config.animationType)
                putExtra("ALLOWED_DOMAIN", allowedDomain)
            }

            activity.startActivity(intent)
            applySurveyAnimations(activity)
            recordSurveyShown()

        } catch (e: ActivityNotFoundException) {
            Log.e("SurveySDK", "Survey activity not found")
        } catch (e: Exception) {
            Log.e("SurveySDK", "Error showing survey: ${e.message}")
        }
    }

    private fun getTargetActivity(): Class<*> {
        return when (config.modalStyle) {
            "bottom_sheet" -> SurveyBottomSheetActivity::class.java
            "dialog" -> SurveyDialogActivity::class.java
            else -> SurveyFullScreenActivity::class.java
        }
    }

    private fun applySurveyAnimations(activity: Activity) {
        when (config.animationType) {
            "slide_up", "fade" -> {
                // Animations handled by individual activities
            }
            "none" -> {
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    // ===== TRIGGER IMPLEMENTATIONS =====
    private fun setupScreenTimeTrigger(screenName: String, activity: Activity) {
        // ✅ SIMPLIFIED: Only check triggerType and timeDelay
        if (config.triggerType != "delayed" || config.timeDelay <= 0) return

        val executor = ActivitySafeExecutor(activity)
        delayExecutors["screen_$screenName"] = executor

        executor.executeDelayed(config.timeDelay) { safeActivity ->
            val screenEntryTime = screenTimers[screenName]
            val currentScreen = getCurrentScreen()

            safeActivity?.let {
                if (screenEntryTime != null && currentScreen == screenName && canShowSurvey()) {
                    triggeredScreens.add(screenName)
                    showSurveyInternal(it)
                }
            }
        }
    }

    private fun setupScrollTrigger(activity: Activity) {
        if (!config.enableScrollTrigger || config.scrollThreshold <= 0) return

        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
        findAndSetupScrollListener(contentView, activity)
    }

    private fun setupAppLaunchTrigger(activity: Activity) {
        if (!config.enableAppLaunchTrigger) return

        // ✅ SUPPORT DELAY for app launch
        if (config.triggerType == "delayed" && config.timeDelay > 0) {
            val executor = ActivitySafeExecutor(activity)
            delayExecutors["app_launch"] = executor

            executor.executeDelayed(config.timeDelay) { safeActivity ->
                safeActivity?.let { if (canShowSurvey()) showSurveyInternal(it) }
            }
            Log.d("SurveySDK", "⏰ Delayed app launch trigger: ${config.timeDelay}ms")
        } else {
            // Instant app launch
            if (canShowSurvey()) showSurveyInternal(activity)
        }
    }

    private fun setupTabChangeTrigger(activity: Activity) {
        // Implementation depends on specific tab setup
    }

    // ===== NAVIGATION & SCROLL DETECTION =====
    private fun autoDetectNavigation(activity: Activity) {
        try {
            val bottomNavView = findBottomNavigationView(activity)
            if (bottomNavView != null) {
                Log.d("SurveySDK", "✅ Found BottomNavigationView - setting up listener")
                setupNavigationListener(bottomNavView, activity)
                return
            }
            findAndSetupViewPager(activity)
        } catch (e: Exception) {
            Log.e("SurveySDK", "Auto navigation detection failed: ${e.message}")
        }
    }

    private fun findBottomNavigationView(activity: Activity): View? {
        return findViewByClassName(activity.window.decorView,
            "com.google.android.material.bottomnavigation.BottomNavigationView")
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
        val viewPager = findViewByClassName(activity.window.decorView, "androidx.viewpager.widget.ViewPager")
            ?: findViewByClassName(activity.window.decorView, "androidx.viewpager2.widget.ViewPager2")

        if (viewPager != null) {
            Log.d("SurveySDK", "✅ Found ViewPager - setting up listener")
            setupViewPagerListener(viewPager, activity)
        }
    }

    // ===== NAVIGATION LISTENER SETUP =====
    private fun setupNavigationListener(navView: View, activity: Activity) {
        try {
            val method = navView.javaClass.getMethod("setOnNavigationItemSelectedListener",
                Class.forName("com.google.android.material.bottomnavigation.BottomNavigationView\$OnNavigationItemSelectedListener"))

            val listener = createNavigationListener(activity)
            method.invoke(navView, listener)
            Log.d("SurveySDK", "✅ BottomNavigationView listener setup successful")
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
                val screenName = menuItem.title?.toString()?.toLowerCase() ?: "nav_item_${menuItem.itemId}"
                Log.d("SurveySDK", "🎯 BottomNav item selected: ${menuItem.title} -> $screenName")
                triggerByNavigation(screenName, activity)
                true
            } else {
                false
            }
        }
    }

    private fun setupViewPagerListener(viewPager: Any, activity: Activity) {
        try {
            if (viewPager::class.java.name == "androidx.viewpager.widget.ViewPager") {
                val method = viewPager.javaClass.getMethod("addOnPageChangeListener",
                    Class.forName("androidx.viewpager.widget.ViewPager\$OnPageChangeListener"))

                val listener = createViewPagerListener(activity)
                method.invoke(viewPager, listener)
                Log.d("SurveySDK", "✅ ViewPager listener setup successful")
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
                Log.d("SurveySDK", "🎯 ViewPager page selected: $position -> $screenName")
                triggerByTabChange(screenName, activity)
            }
            false
        }
    }

    fun triggerByNavigation(screenName: String, activity: Activity) {
        if (!config.enableNavigationTrigger) return

        val shouldTrigger = config.navigationScreens.isEmpty() ||
                config.navigationScreens.any { screenName.contains(it, ignoreCase = true) }

        if (!shouldTrigger) return
        if (config.triggerOnce && triggeredScreens.contains(screenName)) return

        screenTimers[screenName] = System.currentTimeMillis()

        // ✅ SIMPLIFIED: Only check triggerType and timeDelay
        if (config.triggerType == "delayed" && config.timeDelay > 0) {
            setupScreenTimeTrigger(screenName, activity)
        } else {
            if (canShowSurvey()) {
                triggeredScreens.add(screenName)
                showSurveyInternal(activity)
            }
        }
    }

    fun triggerByTabChange(tabName: String, activity: Activity) {
        if (!config.enableTabChangeTrigger) return
        if (config.tabNames.isNotEmpty() && !config.tabNames.contains(tabName)) return
        if (config.triggerOnce && triggeredTabs.contains(tabName)) return

        screenTimers[tabName] = System.currentTimeMillis()

        // ✅ UPDATED: Remove enableTimeTrigger check
        if (config.triggerType == "delayed" && config.timeDelay > 0) {
            setupTabTimeTrigger(tabName, activity)
        } else {
            if (canShowSurvey()) {
                triggeredTabs.add(tabName)
                showSurveyInternal(activity)
            }
        }
    }

    private fun handleScreenExit(previousScreen: String, previousScreenLabel: String, newScreen: String, activity: Activity) {
        if (!config.enableExitTrigger) return
        if (config.navigationScreens.isEmpty()) return

        val shouldTrigger = config.navigationScreens.any {
            previousScreenLabel.contains(it, ignoreCase = true)
        }

        if (!shouldTrigger) return
        if (config.triggerOnce && triggeredExits.contains(previousScreenLabel)) return

        if (canShowSurvey()) {
            triggeredExits.add(previousScreenLabel)
            showSurveyInternal(activity)
        }
    }

    private fun setupTabTimeTrigger(tabName: String, activity: Activity) {
        // ✅ UPDATED: Remove enableTimeTrigger check
        if (config.triggerType != "delayed" || config.timeDelay <= 0) return

        val executor = ActivitySafeExecutor(activity)
        delayExecutors["tab_$tabName"] = executor

        executor.executeDelayed(config.timeDelay) { safeActivity ->
            val tabEntryTime = screenTimers[tabName]
            val currentScreen = getCurrentScreen()

            safeActivity?.let {
                if (tabEntryTime != null && currentScreen == tabName && canShowSurvey()) {
                    triggeredTabs.add(tabName)
                    showSurveyInternal(it)
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
                is ViewGroup -> {
                    findAndSetupScrollListener(child, activity)
                }
            }
        }
    }

    private fun setupScrollViewDetection(scrollView: View, activity: Activity) {
        var triggered = false
        var lastScrollTime = 0L
        val scrollDebounceMs = 1000L

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

                if (!triggered && scrollY >= config.scrollThreshold && canShowSurvey()) {
                    showSurveyInternal(activity)
                    triggered = true
                }
            } catch (e: Exception) {
                Log.e("SurveySDK", "ScrollView detection error")
            }
        }
    }

    private fun setupRecyclerViewDetection(recyclerView: RecyclerView, activity: Activity) {
        var triggered = false
        var lastScrollTime = 0L
        val scrollDebounceMs = 1000L

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScrollTime < scrollDebounceMs) return
                lastScrollTime = currentTime

                try {
                    val scrollY = recyclerView.computeVerticalScrollOffset()

                    if (!triggered && scrollY >= config.scrollThreshold && canShowSurvey()) {
                        showSurveyInternal(activity)
                        triggered = true
                    }
                } catch (e: Exception) {
                    Log.e("SurveySDK", "RecyclerView scroll detection error")
                }
            }
        })
    }

    // ===== EXIT TRIGGER IMPLEMENTATION =====
    private fun setupExitTrigger(activity: Activity) {
        if (!config.enableExitTrigger) return
        activityLifecycleCallbacks = createActivityLifecycleCallbacks()
        activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun createActivityLifecycleCallbacks(): Application.ActivityLifecycleCallbacks {
        return object : Application.ActivityLifecycleCallbacks {
            private var activitiesStarted = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: Activity) { activitiesStarted++ }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                activitiesStarted--
                if (activitiesStarted == 0) {
                    handleAppExit(activity)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
    }

    private fun handleAppExit(activity: Activity) {
        if (!config.enableExitTrigger) return
        if (config.triggerOnce && triggeredExits.contains("app_exit")) return

        // ✅ SIMPLIFIED: Only check triggerType and timeDelay
        if (config.triggerType == "delayed" && config.timeDelay > 0) {
            val executor = ActivitySafeExecutor(activity)
            delayExecutors["app_exit"] = executor

            executor.executeDelayed(config.timeDelay) { safeActivity ->
                safeActivity?.let {
                    if (canShowSurvey()) {
                        triggeredExits.add("app_exit")
                        showSurveyInternal(it)
                    }
                }
            }
        } else {
            if (canShowSurvey()) {
                triggeredExits.add("app_exit")
                showSurveyInternal(activity)
            }
        }
    }

    fun setupActivityLifecycle(activity: Activity) {
        if (!config.enableExitTrigger) return
        activity.application.registerActivityLifecycleCallbacks(createActivityLifecycleCallbacks())
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

    // ===== HELPER METHODS =====
    private fun applyConfiguration(config: Config, fromCache: Boolean, callback: ConfigurationCallback?) {
        this.config = config
        configurationLoaded = true
        callback?.onConfigurationLoaded(true)
    }

    private fun loadFallbackConfig() {
        config = DefaultConfig.getFallbackConfig()
        configurationLoaded = true
        Log.d("SurveySDK", "📋 Fallback configuration loaded")
    }

    fun debugSurveyStatus(): String {
        val timeSinceLastSurvey = System.currentTimeMillis() - lastSurveyTime
        val cooldownRemaining = config.cooldownPeriod - timeSinceLastSurvey
        val canShow = canShowSurvey()

        return """
    === SURVEY STATUS ===
    📊 Shown count: $surveyShownCount/${config.maxSurveysPerSession}
    ⏰ Last shown: ${timeSinceLastSurvey/1000}s ago
    ❄️ Cooldown: ${cooldownRemaining/1000}s remaining (${config.cooldownPeriod/1000}s total)
    🎯 Probability: ${config.probability}
    🔘 Button trigger: ${config.enableButtonTrigger}
    🧭 Navigation trigger: ${config.enableNavigationTrigger}
    ✅ Can show survey: $canShow
    ===
    """.trimIndent()
    }

    private fun debugCanShowSurvey(): Boolean {
        if (!this::config.isInitialized) {
            Log.d("SurveySDK", "❌ Config not initialized")
            return false
        }

        if (config.baseUrl.isEmpty()) {
            Log.d("SurveySDK", "❌ Base URL empty")
            return false
        }

        if (ExclusionRuleEvaluator.shouldExcludeSurvey(context, config.exclusionRules)) {
            Log.d("SurveySDK", "❌ User excluded by rules")
            return false
        }

        if (Math.random() > config.probability) {
            Log.d("SurveySDK", "❌ Blocked by probability: ${config.probability}")
            return false
        }

        if (config.maxSurveysPerSession > 0 && surveyShownCount >= config.maxSurveysPerSession) {
            Log.d("SurveySDK", "❌ Max surveys reached: $surveyShownCount/${config.maxSurveysPerSession}")
            return false
        }

        if (config.cooldownPeriod > 0 && (System.currentTimeMillis() - lastSurveyTime) < config.cooldownPeriod) {
            val remainingTime = config.cooldownPeriod - (System.currentTimeMillis() - lastSurveyTime)
            Log.d("SurveySDK", "❌ Cooldown active: ${remainingTime/1000}s remaining")
            return false
        }

        Log.d("SurveySDK", "✅ Survey can be shown")
        return true
    }

    private fun canShowSurvey(): Boolean {
        val result = debugCanShowSurvey()
        Log.d("SurveySDK", "=== CAN SHOW SURVEY: $result ===")
        return result
    }

    private fun canShowSurvey_X(): Boolean {
        if (!this::config.isInitialized) return false

        // Don't show survey if API failed (empty baseUrl)
        if (config.baseUrl.isEmpty()) return false

        if (ExclusionRuleEvaluator.shouldExcludeSurvey(context, config.exclusionRules)) return false
        if (Math.random() > config.probability) return false
        if (config.maxSurveysPerSession > 0 && surveyShownCount >= config.maxSurveysPerSession) return false
        if (config.cooldownPeriod > 0 && (System.currentTimeMillis() - lastSurveyTime) < config.cooldownPeriod) return false

        return true
    }

    private fun recordSurveyShown() {
        surveyShownCount++
        lastSurveyTime = System.currentTimeMillis()
    }

    private fun getCurrentScreen(): String? {
        return screenTimers.entries.lastOrNull {
            System.currentTimeMillis() - it.value < config.timeDelay + 1000
        }?.key
    }

    private fun getSurveyDomain(): String? {
        return try {
            java.net.URI(config.baseUrl).host
        } catch (e: Exception) {
            null
        }
    }

    private fun buildSurveyUrl(): String {
        val params = collectAllData()
        val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
        val finalUrl = "${config.baseUrl}&$queryString"

        Log.d("SurveySDK", "🔗 Building survey URL:")
        Log.d("SurveySDK", "🔗 Base URL: ${config.baseUrl}")
        Log.d("SurveySDK", "🔗 Final URL: $finalUrl")

        return finalUrl

    }

    private fun collectAllData(): Map<String, String> {
        val data = mutableMapOf<String, String>()
        data.putAll(StorageUtils.findUserData(context))

        // Device data collection
        if (config.collectDeviceId || config.collectDeviceModel) {
            data.putAll(DeviceUtils.getDeviceInfo(context))
        }

        // Location data collection
        if (config.collectLocation) {
            data.putAll(DeviceUtils.getLocationData(context))
        }

        // App usage data collection
        if (config.collectAppUsage) {
            data.putAll(AppUsageUtils.getAppUsageData(context))
            data.putAll(AppUsageUtils.getEnhancedSessionData(context))
            data.putAll(AppUsageUtils.getAppInstallInfo(context)) // ← ADD THIS LINE
        }

        // Add battery info (useful for exclusion rules)
        data.putAll(DeviceUtils.getBatteryInfo(context))

        data.putAll(collectCustomParams())
        data["sdkVersion"] = config.sdkVersion
        data["timestamp"] = System.currentTimeMillis().toString()

        Log.d("SurveySDK", "📊 Collected data fields: ${data.keys}")
        return data
    }

    private fun collectCustomParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        config.customParams.forEach { param ->
            val paramValue = when (param.source) {
                ParamSource.STORAGE -> StorageUtils.findSpecificData(context, param.key ?: param.name) ?: param.defaultValue
                ParamSource.DEVICE -> getDeviceParam(param.key ?: param.name) ?: param.defaultValue
                ParamSource.URL -> null
                ParamSource.TIMESTAMP -> System.currentTimeMillis().toString()
                ParamSource.SESSION -> null
            }
            paramValue?.let { params[param.name] = it }
        }
        return params
    }

    private fun getDeviceParam(key: String): String? {
        val deviceInfo = DeviceUtils.getDeviceInfo(context)
        return deviceInfo[key]
    }

    // ===== SESSION TRACKING INTEGRATION =====

    // Call this when the app starts or resumes to track sessions
    fun trackAppStart(activity: Activity) {
        ExclusionRuleEvaluator.trackSessionStart(activity)
        Log.d("SurveySDK", "App start tracked")
    }

    // Call this when a new screen/fragment is displayed
    fun trackScreenView(activity: Activity) {
        ExclusionRuleEvaluator.trackScreenView(activity)
        Log.d("SurveySDK", "Screen view tracked")
    }

    // Reset all session data (for testing or logout scenarios)
    fun resetSessionData() {
        ExclusionRuleEvaluator.resetSessionData(context)
        Log.d("SurveySDK", "Session data reset")
    }

    //Set custom session data for exclusion rules
    fun setSessionData(key: String, value: String) {
        ExclusionRuleEvaluator.setSessionData(key, value)
    }

    // Get current session statistics for debugging
    fun getSessionStats(): Map<String, String> {
        val prefs = context.getSharedPreferences("survey_sdk_session", Context.MODE_PRIVATE)
        return mapOf(
            "session_count" to prefs.getInt("session_count", 0).toString(),
            "screen_view_count" to prefs.getInt("screen_view_count", 0).toString(),
            "last_session_time" to prefs.getLong("last_session_time", 0).toString(),
            "first_session_time" to prefs.getLong("first_session_time", 0).toString()
        )
    }


    // Add this public method for clients to check
    fun isSDKEnabled(): Boolean {
        return configurationLoaded && !apiConfigurationFailed && config.baseUrl.isNotEmpty()
    }
}