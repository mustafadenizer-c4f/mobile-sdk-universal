declare module 'surveysdk-react-native' {
  /**
   * Professional Survey SDK for React Native
   */
  interface SurveySDKType {
    /**
     * Initialize the Survey SDK with your API key
     * @param apiKey Your survey API key
     */
    initialize(apiKey: string): Promise<boolean>;
    
    /**
     * Display a survey modal
     */
    showSurvey(): Promise<boolean>;
    
    /**
     * Set user property for targeting and exclusion rules
     * @param key Property key
     * @param value Property value
     */
    setUserProperty(key: string, value: string): Promise<boolean>;
    
    /**
     * Set multiple user properties at once
     * @param properties Key-value pairs of user properties
     */
    setUserProperties(properties: Record<string, any>): Promise<boolean[]>;
    
    /**
     * Track custom events for survey triggers
     * @param eventName Event name
     * @param properties Event properties (optional)
     */
    trackEvent(eventName: string, properties?: Record<string, any>): Promise<boolean>;
    
    /**
     * Check if user is excluded from surveys based on rules
     */
    isUserExcluded(): Promise<boolean>;
    
    /**
     * Get debug status and SDK configuration
     */
    getDebugStatus(): Promise<string>;
    
    /**
     * Auto-setup triggers (buttons, scroll, navigation, etc.)
     */
    autoSetup(): Promise<boolean>;
    
    /**
     * Get session statistics
     */
    getSessionStats(): Promise<Record<string, string>>;
    
    /**
     * Set custom session data for exclusion rules
     */
    setSessionData(key: string, value: string): Promise<boolean>;
    
    /**
     * Reset session data
     */
    resetSessionData(): Promise<boolean>;
    
    /**
     * Reset all triggers
     */
    resetTriggers(): Promise<boolean>;
  }

  const SurveySDK: SurveySDKType;
  export default SurveySDK;
}