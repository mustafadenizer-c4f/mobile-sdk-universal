import { NativeModules } from 'react-native';

const { SurveySDK } = NativeModules;

class SurveySDKBridge {
  async initialize(apiKey) {
    if (!apiKey) {
      throw new Error('API key is required');
    }
    return await SurveySDK.initialize(apiKey);
  }

  async showSurvey() {
    return await SurveySDK.showSurvey();
  }

  async showSurveyById(surveyId) {
    if (!surveyId) {
      throw new Error('Survey ID is required');
    }
    return await SurveySDK.showSurveyById(surveyId);
  }

  async setUserProperty(key, value) {
    if (!key || !value) {
      throw new Error('Key and value are required');
    }
    return await SurveySDK.setUserProperty(key, value);
  }

  async trackEvent(eventName, properties = {}) {
    if (!eventName) {
      throw new Error('Event name is required');
    }
    return await SurveySDK.trackEvent(eventName, properties);
  }

  async setUserProperties(properties) {
    if (!properties || typeof properties !== 'object') {
      throw new Error('Properties must be an object');
    }

    const promises = Object.entries(properties).map(([key, value]) =>
      this.setUserProperty(key, String(value))
    );

    return Promise.all(promises);
  }

  async isUserExcluded() {
    return await SurveySDK.isUserExcluded();
  }

  async isUserExcludedForSurvey(surveyId) {
    if (!surveyId) {
      throw new Error('Survey ID is required');
    }
    return await SurveySDK.isUserExcludedForSurvey(surveyId);
  }

  async getDebugStatus() {
    return await SurveySDK.getDebugStatus();
  }

  async autoSetup() {
    return await SurveySDK.autoSetup();
  }

  async getSurveyIds() {
    return await SurveySDK.getSurveyIds();
  }

  async isConfigurationLoaded() {
    return await SurveySDK.isConfigurationLoaded();
  }

  async setSessionData(key, value) {
    if (!key || !value) {
      throw new Error('Key and value are required');
    }
    return await SurveySDK.setSessionData(key, value);
  }

  async resetSessionData() {
    return await SurveySDK.resetSessionData();
  }

  async resetTriggers() {
    return await SurveySDK.resetTriggers();
  }

  // ===== NEW METHODS FOR MULTI-SURVEY SUPPORT =====

  async getQueueStatus() {
    return await SurveySDK.getQueueStatus();
  }

  async clearSurveyQueue() {
    return await SurveySDK.clearSurveyQueue();
  }

  async isShowingSurvey() {
    return await SurveySDK.isShowingSurvey();
  }

  async getSetupStatus() {
    return await SurveySDK.getSetupStatus();
  }

  async isSDKEnabled() {
    return await SurveySDK.isSDKEnabled();
  }

  async isReady() {
    return await SurveySDK.isReady();
  }

  async fetchConfiguration() {
    return await SurveySDK.fetchConfiguration();
  }

  async getConfigForDebug() {
    return await SurveySDK.getConfigForDebug();
  }

  async cleanup() {
    return await SurveySDK.cleanup();
  }

  // ===== CONVENIENCE METHODS =====

  /**
   * Get all surveys with their status
   */
  async getAllSurveysStatus() {
    const surveyIds = await this.getSurveyIds();
    const statusPromises = surveyIds.map(async (surveyId) => ({
      surveyId,
      isExcluded: await this.isUserExcludedForSurvey(surveyId).catch(() => false),
    }));
    
    return Promise.all(statusPromises);
  }

  /**
   * Show the first available survey
   */
  async showFirstAvailableSurvey() {
    const surveyIds = await this.getSurveyIds();
    
    for (const surveyId of surveyIds) {
      const isExcluded = await this.isUserExcludedForSurvey(surveyId);
      if (!isExcluded) {
        return await this.showSurveyById(surveyId);
      }
    }
    
    throw new Error('No available surveys to show');
  }

  /**
   * Wait for SDK to be ready with timeout
   */
  async waitForReady(timeoutMs = 10000) {
    const startTime = Date.now();
    
    while (Date.now() - startTime < timeoutMs) {
      const isReady = await this.isReady();
      if (isReady) return true;
      
      // Wait 500ms before checking again
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    throw new Error(`SDK not ready after ${timeoutMs}ms`);
  }
}

export default new SurveySDKBridge();