import { NativeModules } from "react-native";

const { SurveySDK } = NativeModules;

class SurveySDKBridge {
  /**
   * Initialize the Survey SDK
   * @param {string} apiKey - Your API key
   * @param {Array} params - Optional parameters (strings or objects)
   *
   * Examples:
   * // 1. Simple initialization
   * await SurveySDK.initialize('your-api-key');
   *
   * // 2. With ONE parameter name (look up from storage)
   * await SurveySDK.initialize('your-api-key', ['userID']);
   *
   * // 3. With MULTIPLE parameter names (look up from storage)
   * await SurveySDK.initialize('your-api-key', ['userID', 'email', 'userTier']);
   *
   * // 4. With DIRECT values (key-value pairs)
   * await SurveySDK.initialize('your-api-key', [
   *   { userId: '12345' },
   *   { userTier: 'premium' }
   * ]);
   *
   * // 5. MIXED parameters (some from storage, some direct values)
   * await SurveySDK.initialize('your-api-key', [
   *   'userID',           // Look up from storage
   *   { email: 'user@example.com' },  // Direct value
   *   'language',         // Look up from storage
   *   { source: 'mobile_app' }        // Direct value
   * ]);
   */
  // async initialize(apiKey) {
  //   if (!apiKey) {
  //     throw new Error('API key is required');
  //   }
  //   return await SurveySDK.initialize(apiKey);
  // }

  // async initializeWithParams(apiKey, params = []) {
  //   if (!apiKey) {
  //     throw new Error('API key is required');
  //   }
  //   if (!Array.isArray(params)) {
  //     throw new Error('Params must be an array');
  //   }
  //   return await SurveySDK.initializeWithParams(apiKey, params);
  // }

  async initialize(apiKey, params = []) {
    if (!apiKey) {
      throw new Error("API key is required");
    }
    if (!Array.isArray(params)) {
      throw new Error("Params must be an array");
    }

    // Call the SAME method for all cases
    // The native side will handle the conversion
    return await SurveySDK.initialize(apiKey, params);
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

  // ===== NEW MULTI-SURVEY METHODS =====

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

  /**
   * Manually trigger a scroll event.
   * Useful to call inside onScroll props if auto-detection misses it.
   */
  async triggerScrollSurvey() {
    return await SurveySDK.triggerScrollSurvey();
  }

  // SurveySDKBridge class:
  async enableNavigationSafety() {
    return await SurveySDK.enableNavigationSafety();
  }

  async autoSetupSafe() {
    return await SurveySDK.autoSetupSafe();
  }

  /**
   * Manually trigger a navigation event.
   * Call this when your screen changes.
   */
  async triggerNavigationSurvey(screenName) {
    if (!screenName) {
      throw new Error("Screen name is required");
    }
    return await SurveySDK.triggerNavigationSurvey(screenName);
  }

  // ===== CONVENIENCE METHODS =====

  /**
   * Get all surveys with their status
   */
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

    throw new Error("No available surveys to show");
  }
}

export default new SurveySDKBridge();
