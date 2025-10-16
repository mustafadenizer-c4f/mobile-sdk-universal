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

  async getDebugStatus() {
    return await SurveySDK.getDebugStatus();
  }

  async autoSetup() {
    return await SurveySDK.autoSetup();
  }
}

export default new SurveySDKBridge();