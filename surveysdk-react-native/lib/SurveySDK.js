import { NativeModules } from "react-native";

const { SurveySDK } = NativeModules;

class SurveySDKBridge {
  /**
   * Initialize the Survey SDK
   * @param {string} apiKey - Your API key
   * @param {Array} params - Optional parameters (strings or objects)
   *
   * Examples:
   * // Simple initialization
   * await SurveySDK.initialize('your-api-key');
   *
   * // With parameter name (SDK will look up from storage)
   * await SurveySDK.initialize('your-api-key', ['userID']);
   *
   * // With multiple parameter names
   * await SurveySDK.initialize('your-api-key', ['userID', 'email', 'userTier']);
   *
   * // With direct values
   * await SurveySDK.initialize('your-api-key', [
   *   { userId: '12345' },
   *   { userTier: 'premium' }
   * ]);
   *
   * // Mixed parameters
   * await SurveySDK.initialize('your-api-key', [
   *   'userID',                    // Look up from storage
   *   { email: 'user@example.com' }, // Direct value
   *   'language',                  // Look up from storage
   *   { source: 'mobile_app' }     // Direct value
   * ]);
   */
  // SurveySDK.js - MAKE SURE THIS MATCHES
  async initialize(apiKey, params = []) {
    if (!apiKey) {
      throw new Error("API key is required");
    }

    // ✅ CRITICAL FIX: Handle undefined/null parameters
    // React Native bridge converts default params to undefined!
    const safeParams = params || [];

    // Ensure it's an array
    const paramsArray = Array.isArray(safeParams) ? safeParams : [safeParams];

    Log.d(
      "SurveySDK_RN_JS",
      `Initialize called with ${paramsArray.length} params`
    );
    return await SurveySDK.initializeNew(apiKey, paramsArray);
  }

  async showSurvey() {
    return await SurveySDK.showSurvey();
  }

  async showSurveyById(surveyId) {
    if (!surveyId) {
      throw new Error("Survey ID is required");
    }
    return await SurveySDK.showSurveyById(surveyId);
  }

  async setUserProperty(key, value) {
    if (!key || !value) {
      throw new Error("Key and value are required");
    }
    return await SurveySDK.setUserProperty(key, value);
  }

  async setUserProperties(properties) {
    if (!properties || typeof properties !== "object") {
      throw new Error("Properties must be an object");
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
      throw new Error("Survey ID is required");
    }
    return await SurveySDK.isUserExcludedForSurvey(surveyId);
  }

  async getDebugStatus() {
    return await SurveySDK.getDebugStatus();
  }

  async autoSetup() {
    return await SurveySDK.autoSetup();
  }

  async enableNavigationSafety() {
    return await SurveySDK.enableNavigationSafety();
  }

  async autoSetupSafe() {
    return await SurveySDK.autoSetupSafe();
  }

  async getSurveyIds() {
    return await SurveySDK.getSurveyIds();
  }

  async isConfigurationLoaded() {
    return await SurveySDK.isConfigurationLoaded();
  }

  async setSessionData(key, value) {
    if (!key || !value) {
      throw new Error("Key and value are required");
    }
    return await SurveySDK.setSessionData(key, value);
  }

  async resetSessionData() {
    return await SurveySDK.resetSessionData();
  }

  async resetTriggers() {
    return await SurveySDK.resetTriggers();
  }

  async getQueueStatus() {
    return await SurveySDK.getQueueStatus();
  }

  async clearSurveyQueue() {
    return await SurveySDK.clearSurveyQueue();
  }

  async isShowingSurvey() {
    return await SurveySDK.isShowingSurvey();
  }

  async isSDKEnabled() {
    return await SurveySDK.isSDKEnabled();
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

  async triggerButtonSurvey(buttonId) {
    if (!buttonId) {
      throw new Error("Button ID is required");
    }
    return await SurveySDK.triggerButtonSurvey(buttonId);
  }

  async triggerScrollSurvey() {
    return await SurveySDK.triggerScrollSurvey();
  }

  async triggerNavigationSurvey(screenName) {
    if (!screenName) {
      throw new Error("Screen name is required");
    }
    return await SurveySDK.triggerNavigationSurvey(screenName);
  }

  // ===== CONVENIENCE METHODS =====
  async getAllSurveysStatus() {
    const surveyIds = await this.getSurveyIds();
    const statusPromises = surveyIds.map(async (surveyId) => ({
      surveyId,
      isExcluded: await this.isUserExcludedForSurvey(surveyId).catch(
        () => false
      ),
    }));
    return Promise.all(statusPromises);
  }

  async showFirstAvailableSurvey() {
    const surveyIds = await this.getSurveyIds();
    for (const surveyId of surveyIds) {
      const isExcluded = await this.isUserExcludedForSurvey(surveyId);
      if (!isExcluded) {
        return await this.showSurveyById(surveyId);
      }
    }
    throw new Error("No available surveys to show");
  }
}

export default new SurveySDKBridge();
