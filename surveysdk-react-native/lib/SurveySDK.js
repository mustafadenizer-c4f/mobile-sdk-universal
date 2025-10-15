import { NativeModules } from 'react-native';

const { SurveySDK } = NativeModules;

class SurveySDKBridge {
  /**
   * Initialize the Survey SDK
   * @param {string} apiKey - Your Survey SDK API key
   * @returns {Promise<void>}
   */
  async initialize(apiKey) {
    if (!apiKey) {
      throw new Error('API key is required');
    }
    return await SurveySDK.initialize(apiKey);
  }

  /**
   * Show the survey modal
   * @returns {Promise<void>}
   */
  async showSurvey() {
    return await SurveySDK.showSurvey();
  }

  /**
   * Set user property for targeting
   * @param {string} key - Property key
   * @param {string} value - Property value
   * @returns {Promise<void>}
   */
  async setUserProperty(key, value) {
    if (!key || !value) {
      throw new Error('Key and value are required');
    }
    return await SurveySDK.setUserProperty(key, value);
  }

  /**
   * Track custom event
   * @param {string} eventName - Event name
   * @param {Object} properties - Event properties
   * @returns {Promise<void>}
   */
  async trackEvent(eventName, properties = {}) {
    if (!eventName) {
      throw new Error('Event name is required');
    }
    return await SurveySDK.trackEvent(eventName, properties);
  }

  /**
   * Set multiple user properties at once
   * @param {Object} properties - Key-value pairs of user properties
   * @returns {Promise<void>}
   */
  async setUserProperties(properties) {
    if (!properties || typeof properties !== 'object') {
      throw new Error('Properties must be an object');
    }

    const promises = Object.entries(properties).map(([key, value]) =>
      this.setUserProperty(key, String(value))
    );

    return Promise.all(promises);
  }
}

export default new SurveySDKBridge();