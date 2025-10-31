// DefaultConfig.kt - REPLACE THE ENTIRE FILE
package com.example.surveysdk

object DefaultConfig {

    // ===== EMPTY CONFIGURATION =====
    val EMPTY_CONFIG = Config(
        sdkVersion = "2.0.0",
        surveys = emptyList()
    )

    // ===== TEST CONFIGURATION WITH MULTIPLE SURVEYS =====
    val TEST_CONFIG = Config(
        sdkVersion = "2.0.0",
        surveys = listOf(
           
           SurveyConfig(
    surveyId = "delayed_launch_survey",
    surveyName = "Delayed App Launch Survey",
    baseUrl = "https://jsonplaceholder.typicode.com/posts/2?survey=delayed_launch",
    
    // Trigger Settings  
    //enableAppLaunchTrigger = true,
    enableExitTrigger = true,
    
    // Trigger Configuration
    triggerType = "instant",
    //timeDelay = 5000, // 5 second delay after app launch
    
    // Display Settings
    modalStyle = "full_screen",
    animationType = "fade",
    backgroundColor = "#F5F5F5",
    
    // Targeting & Limits
    probability = 1.0, // 50% chance
    maxShowsPerSession = 99, 
    cooldownPeriod = 1800000L, // 30 minutes
    triggerOnce = true, // Only show once per install
    priority = 2,
    
    // Data Collection
    collectDeviceId = true,
    collectDeviceModel = false, 
    collectLocation = true,
    collectAppUsage = true,
    
    // Custom Parameters - APP LAUNCH CONTEXT
    customParams = listOf(
        // App launch count
        CustomParam("launch_count", ParamSource.STORAGE, key = "app_launch_count"),
        // First launch today
        CustomParam("first_launch_today", ParamSource.SESSION, key = "first_launch"),
        // Time since last launch  
        CustomParam("time_since_last_launch", ParamSource.STORAGE, key = "last_launch_time"),
        // Installation date
        CustomParam("days_since_install", ParamSource.INSTALL_TIME, key = "days"),
        // Current battery level
        CustomParam("battery_at_launch", ParamSource.DEVICE, key = "battery_level"),
        // Network type at launch
        CustomParam("network_at_launch", ParamSource.DEVICE, key = "networkType")
    ),
    
    // Exclusion Rules - LAUNCH BEHAVIOR
    exclusionRules = listOf(
        // Exclude if not first launch today
        ExclusionRule(
            name = "exclude_not_first_launch",
            source = ExclusionSource.SESSION,
            key = "first_launch",
            operator = ExclusionOperator.NOT_EQUALS,
            matchValue = "true"
        ),
        // Exclude if app used recently (< 2 hours ago)
        ExclusionRule(
            name = "exclude_recent_users", 
            source = ExclusionSource.STORAGE,
            key = "last_launch_time",
            operator = ExclusionOperator.GREATER_THAN,
            matchValue = "${System.currentTimeMillis() - 7200000}" // 2 hours ago
        ),
        // Exclude on low battery (< 20%)
        ExclusionRule(
            name = "exclude_low_battery",
            source = ExclusionSource.DEVICE,
            key = "battery_level", 
            operator = ExclusionOperator.LESS_THAN,
            matchValue = "20"
        )
    )
),
            
            SurveyConfig(
                surveyId = "notifications_survey",
                surveyName = "Notifications Feedback", 
                baseUrl = "https://jsonplaceholder.typicode.com/posts/2?survey=notifications",
                
                // Trigger Settings
                enableNavigationTrigger = true,
                
                // Trigger Configuration  
                triggerScreens = setOf("notifications"),
                triggerType = "delayed",
                timeDelay = 1000,
                
                // Display Settings
                modalStyle = "full_screen",
                animationType = "fade",
                backgroundColor = "#F5F5F5",
                
                // Targeting & Limits
                probability = 1.0, // 100% chance
                maxShowsPerSession = 999,
                priority = 2,
                exclusionRules = listOf(
                    ExclusionRule(
            name = "exclude_premium",
            source = ExclusionSource.STORAGE,
            key = "isPremium",
            operator = ExclusionOperator.EQUALS,
            matchValue = "true"
        ),
        // Exclude specific devices
        ExclusionRule(
            name = "exclude_tablets",
            source = ExclusionSource.DEVICE, 
            key = "deviceModel",
            operator = ExclusionOperator.CONTAINS,
            matchValue = "Tab",
            caseSensitive = false
        )
                ),
                // Age targeting example - only show to users 20+

                
                // Data Collection
                collectDeviceId = true,
                customParams = listOf(
                    CustomParam("user_id", ParamSource.STORAGE, key = "userId"),
                    CustomParam("survey_source", ParamSource.STORAGE, value = "notifications")
                )
            ),
            
            SurveyConfig(
                surveyId = "mens_tab_survey",
                surveyName = "Mens Category Feedback",
                baseUrl = "https://jsonplaceholder.typicode.com/posts/3?survey=mens",
                
                // Trigger Settings
                enableTabChangeTrigger = true,
                
                // Trigger Configuration
                triggerTabs = setOf("mens", "men_fashion"),
                
                // Display Settings
                modalStyle = "dialog",
                animationType = "slide_up",
                backgroundColor = "#FFFFFF",
                
                // Targeting & Limits  
                probability = 1.0,
                priority = 3,
                
                // Data Collection
                collectDeviceModel = true,
                customParams = listOf(
                    CustomParam("category", ParamSource.STORAGE, value = "mens")
                )
            )
        )
    )

    // ===== QUICK CONFIG SWITCHER =====
    fun getFallbackConfig(): Config {
        return TEST_CONFIG // Change to EMPTY_CONFIG for production
    }
}