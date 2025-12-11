declare module 'surveysdk-react-native' {
  interface SurveySDKType {
    // Core Methods
    initialize(apiKey: string): Promise<boolean>;
    showSurvey(): Promise<boolean>;
    
    // Multi-Survey Methods
    showSurveyById(surveyId: string): Promise<boolean>;
    getSurveyIds(): Promise<string[]>;
    isUserExcludedForSurvey(surveyId: string): Promise<boolean>;
    isConfigurationLoaded(): Promise<boolean>;
    
    // User Data & Events
    setUserProperty(key: string, value: string): Promise<boolean>;
    setUserProperties(properties: Record<string, string>): Promise<boolean[]>;
    trackEvent(eventName: string, properties?: Record<string, any>): Promise<boolean>;
    
    // Session & Triggers
    setSessionData(key: string, value: string): Promise<boolean>;
    resetSessionData(): Promise<boolean>;
    resetTriggers(): Promise<boolean>;
    
    // Debug & Status
    isUserExcluded(): Promise<boolean>;
    getDebugStatus(): Promise<string>;
    autoSetup(): Promise<boolean>;
  }

  const SurveySDK: SurveySDKType;
  export default SurveySDK;
}