package com.example.surveysdk

object DefaultConfig {

    // ===== TEST CONFIGURATION =====
    // Use this when you want to test with real surveys
    val TEST_CONFIG = Config(

        /*
        // DEFAULT EMPTY CONFIG
           baseUrl = "", // ← Empty URL = no survey
           enableButtonTrigger = false,
           triggerType = "instant",
           enableScrollTrigger = false,
           enableNavigationTrigger = false,
           enableAppLaunchTrigger = false,
           enableExitTrigger = false,
           enableTabChangeTrigger = false,
           probability = 0.0, // ← 0% chance to show
           exclusionRules = emptyList()
        */
        // ===== BASIC SETTINGS FOR TEST PURPOSES ONLY, PROD WILL USE EMPTY =====
        baseUrl = "https://jsonplaceholder.typicode.com/posts/1?a=1",
        sdkVersion = "1.0.0",

        // ===== TRIGGER CONFIGURATION =====
        enableButtonTrigger = true,
        enableScrollTrigger = false,
        enableNavigationTrigger = true,
        enableAppLaunchTrigger = false,
        enableExitTrigger = false,
        enableTabChangeTrigger = true,

        // ===== TRIGGER TIMING & BEHAVIOR =====
        timeDelay = 5000L, // 5 seconds delay for time triggers
        scrollThreshold = 500, // pixels scrolled
        triggerType = "instant", // or "delayed"

        // ===== NAVIGATION & SCREENS =====
        navigationScreens = setOf("dashboard", "notifications"),
        tabNames = setOf("tab1", "tab2", "tab3"),

        // ===== UI/UX SETTINGS =====
        modalStyle = "dialog", // "full_screen", "bottom_sheet", "dialog"
        animationType = "slide_up", // "slide_up", "fade", "slide_down", "none"
        backgroundColor = "#FFFFFF",

        // ===== DATA COLLECTION =====
        collectDeviceId = true,
        collectDeviceModel = true,
        collectLocation = false,
        collectAppUsage = false,

        // ===== SURVEY FREQUENCY & LIMITS =====
        probability = 1.0, // 100% chance to show
        maxSurveysPerSession = 999,
        cooldownPeriod = 0L, // 300000L for 5 minutes cooldown
        triggerOnce = false,
        cacheDurationHours = 24L,

        // ===== CUSTOM PARAMETERS =====
        customParams = listOf(
            CustomParam("user_id", ParamSource.STORAGE, key = "userId"),
            CustomParam("user_email", ParamSource.STORAGE, key = "userEmail"),
            CustomParam("device_type", ParamSource.DEVICE, key = "deviceModel"),
            CustomParam("survey_time", ParamSource.TIMESTAMP),
            CustomParam("app_version", ParamSource.DEVICE, key = "appVersion"),
        ),
        // ===== EXCLUSION RULES =====
        exclusionRules = listOf(

/*
            ExclusionRule(
                name = "block_test_accounts",
                source = ExclusionSource.STORAGE,
                key = "userEmail",                  // Look for email in storage
                operator = ExclusionOperator.CONTAINS,
                matchValue = "test",                // Block emails with "test"
                caseSensitive = false
            ),
// Use case: Exclude QA/test accounts
            ExclusionRule(
                name = "exclude_test_accounts",
                source = ExclusionSource.STORAGE,
                key = "userEmail",
                operator = ExclusionOperator.CONTAINS,
                matchValue = "google.com",
                caseSensitive = false
            ),
            ExclusionRule(
                name = "block_free_users",
                source = ExclusionSource.STORAGE,
                key = "userTier",                   // Look for subscription tier
                operator = ExclusionOperator.EQUALS,
                matchValue = "free",                // Block free tier users
                caseSensitive = false
            ),
// Use case: Only show surveys to premium users
            ExclusionRule(
                name = "block_enterprise_users",
                source = ExclusionSource.STORAGE,
                key = "userSegment",                // Look for user segment
                operator = ExclusionOperator.EQUALS,
                matchValue = "enterprise",          // Block enterprise customers
                caseSensitive = false
            ),
// Use case: Different survey strategies for different segments
            ExclusionRule(
                name = "block_old_versions",
                source = ExclusionSource.DEVICE,
                key = "appVersion",                 // Get app version from device
                operator = ExclusionOperator.LESS_THAN,
                matchValue = "2.0.0",               // Block versions below 2.0.0
                caseSensitive = false
            ),
// Use case: Only survey users with latest features
            ExclusionRule(
                name = "block_test_devices",
                source = ExclusionSource.DEVICE,
                key = "deviceModel",                // Get device model
                operator = ExclusionOperator.CONTAINS,
                matchValue = "Emulator",            // Block emulators
                caseSensitive = false
            ),
// Use case: Exclude test devices/emulators
            ExclusionRule(
                name = "block_old_android",
                source = ExclusionSource.DEVICE,
                key = "osVersion",                  // Get Android version
                operator = ExclusionOperator.LESS_THAN,
                matchValue = "10",                  // Block Android 9 and below
                caseSensitive = false
            ),
// Use case: Target modern OS users only
            ExclusionRule(
                name = "low_battery_exclusion",        // ← UNIQUE NAME
                source = ExclusionSource.DEVICE,
                key = "battery_level",
                operator = ExclusionOperator.LESS_THAN,
                matchValue = "20", // Block if battery < 20%
                caseSensitive = false
            ),
            ExclusionRule(
                name = "low_battery_exclusion",        // ← UNIQUE NAME
                source = ExclusionSource.DEVICE,
                key = "battery_level",
                operator = ExclusionOperator.LESS_THAN,
                matchValue = "20", // Block if battery < 20%
                caseSensitive = false
            ),
            ExclusionRule(
                name = "late_night_exclusion",         // ← UNIQUE NAME
                source = ExclusionSource.TIMESTAMP,
                key = "hour",
                operator = ExclusionOperator.GREATER_THAN,
                matchValue = "22", // Block after 10 PM
                caseSensitive = false
            ),
            ExclusionRule(
                name = "weekend_exclusion",            // ← UNIQUE NAME
                source = ExclusionSource.TIMESTAMP,
                key = "day_of_week",
                operator = ExclusionOperator.IN,       // Assuming you add IN operator
                matchValue = "1,7", // Block on Sunday (1) and Saturday (7)
                caseSensitive = false
            ),
            ExclusionRule(
                name = "charging_required",            // ← UNIQUE NAME
                source = ExclusionSource.DEVICE,
                key = "is_charging",
                operator = ExclusionOperator.EQUALS,
                matchValue = "true", // Block if device charging
                caseSensitive = false
            ),
            ExclusionRule(
                name = "wifi_required",                // ← UNIQUE NAME
                source = ExclusionSource.DEVICE,
                key = "is_wifi",
                operator = ExclusionOperator.EQUALS,
                matchValue = "false", // Block if not on WiFi
                caseSensitive = false
            ),
            ExclusionRule(
                name = "exclude_specific_roles",
                source = ExclusionSource.STORAGE,
                key = "userRole",
                operator = ExclusionOperator.IN,
                matchValue = "admin,moderator,test", // Block multiple roles
                caseSensitive = false
            ),
            ExclusionRule(
                name = "exclude_first_time_flow",      // ← UNIQUE NAME
                source = ExclusionSource.SESSION,
                key = "onboarding_complete",
                operator = ExclusionOperator.EQUALS,
                matchValue = "false", // Block during onboarding
                caseSensitive = false
            )*/

        )
    )

    // ===== EMPTY CONFIGURATION =====
    // Use this when you want API-only behavior (no fallback surveys)
    val EMPTY_CONFIG = Config(
        baseUrl = "", // Empty URL = no survey when API fails
        enableButtonTrigger = false,
        enableScrollTrigger = false,
        enableNavigationTrigger = false,
        enableAppLaunchTrigger = false,
        enableExitTrigger = false,
        enableTabChangeTrigger = false,
        navigationScreens = emptySet(),
        probability = 0.0,
        exclusionRules = emptyList()
    )

    // ===== QUICK CONFIG SWITCHER =====
    // Change this to easily switch between test and empty config
    fun getFallbackConfig(): Config {
        return TEST_CONFIG    // ← Change to EMPTY_CONFIG for API-only behavior
        // return EMPTY_CONFIG // ← Use this for final client delivery
    }
}