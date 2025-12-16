import 'dart:async';
import 'package:flutter/services.dart';

class SurveySdkFlutter {
  static const MethodChannel _channel = MethodChannel('surveysdk_flutter');

  static Future<bool> initialize(String apiKey) async {
    try {
      final bool result = await _channel.invokeMethod('initialize', {
        'apiKey': apiKey,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to initialize SDK: ${e.message}');
    }
  }

  static Future<bool> showSurvey() async {
    try {
      final bool result = await _channel.invokeMethod('showSurvey');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to show survey: ${e.message}');
    }
  }

  static Future<bool> showSurveyById(String surveyId) async {
    try {
      final bool result = await _channel.invokeMethod('showSurveyById', {
        'surveyId': surveyId,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to show survey $surveyId: ${e.message}');
    }
  }

  static Future<bool> autoSetup() async {
    try {
      final bool result = await _channel.invokeMethod('autoSetup');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to auto setup: ${e.message}');
    }
  }

  static Future<bool> isUserExcluded() async {
    try {
      final bool result = await _channel.invokeMethod('isUserExcluded');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to check exclusion: ${e.message}');
    }
  }

  static Future<bool> isUserExcludedForSurvey(String surveyId) async {
    try {
      final bool result = await _channel.invokeMethod('isUserExcludedForSurvey', {
        'surveyId': surveyId,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to check exclusion for $surveyId: ${e.message}');
    }
  }

  static Future<String> getDebugStatus() async {
    try {
      final String result = await _channel.invokeMethod('getDebugStatus');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to get debug status: ${e.message}');
    }
  }

  static Future<List<dynamic>> getSurveyIds() async {
    try {
      final List<dynamic> result = await _channel.invokeMethod('getSurveyIds');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to get survey IDs: ${e.message}');
    }
  }

  static Future<bool> isConfigurationLoaded() async {
    try {
      final bool result = await _channel.invokeMethod('isConfigurationLoaded');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to check config status: ${e.message}');
    }
  }

  static Future<bool> setUserProperty(String key, String value) async {
    try {
      final bool result = await _channel.invokeMethod('setUserProperty', {
        'key': key,
        'value': value,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to set user property: ${e.message}');
    }
  }

  static Future<bool> trackEvent(String eventName, [Map<String, dynamic>? properties]) async {
    try {
      final bool result = await _channel.invokeMethod('trackEvent', {
        'eventName': eventName,
        'properties': properties ?? {},
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to track event: ${e.message}');
    }
  }

  static Future<bool> setSessionData(String key, String value) async {
    try {
      final bool result = await _channel.invokeMethod('setSessionData', {
        'key': key,
        'value': value,
      });
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to set session data: ${e.message}');
    }
  }

  static Future<bool> resetSessionData() async {
    try {
      final bool result = await _channel.invokeMethod('resetSessionData');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to reset session data: ${e.message}');
    }
  }

  static Future<bool> resetTriggers() async {
    try {
      final bool result = await _channel.invokeMethod('resetTriggers');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to reset triggers: ${e.message}');
    }
  }

  // ===== NEW MULTI-SURVEY METHODS =====

  static Future<String> getQueueStatus() async {
    try {
      final String result = await _channel.invokeMethod('getQueueStatus');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to get queue status: ${e.message}');
    }
  }

  static Future<bool> clearSurveyQueue() async {
    try {
      final bool result = await _channel.invokeMethod('clearSurveyQueue');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to clear survey queue: ${e.message}');
    }
  }

  static Future<bool> isShowingSurvey() async {
    try {
      final bool result = await _channel.invokeMethod('isShowingSurvey');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to check if survey is showing: ${e.message}');
    }
  }

  static Future<bool> isSDKEnabled() async {
    try {
      final bool result = await _channel.invokeMethod('isSDKEnabled');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to check if SDK is enabled: ${e.message}');
    }
  }

  static Future<bool> fetchConfiguration() async {
    try {
      final bool result = await _channel.invokeMethod('fetchConfiguration');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to fetch configuration: ${e.message}');
    }
  }

  static Future<String> getConfigForDebug() async {
    try {
      final String result = await _channel.invokeMethod('getConfigForDebug');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to get config debug info: ${e.message}');
    }
  }

  static Future<bool> cleanup() async {
    try {
      final bool result = await _channel.invokeMethod('cleanup');
      return result;
    } on PlatformException catch (e) {
      throw Exception('Failed to cleanup SDK: ${e.message}');
    }
  }

  // ===== CONVENIENCE METHODS =====

  /// Get all surveys with their exclusion status
  static Future<Map<String, bool>> getAllSurveysStatus() async {
    try {
      final List<dynamic> surveyIds = await getSurveyIds();
      final Map<String, bool> statusMap = {};
      
      for (var surveyId in surveyIds) {
        try {
          final isExcluded = await isUserExcludedForSurvey(surveyId.toString());
          statusMap[surveyId.toString()] = isExcluded;
        } catch (e) {
          statusMap[surveyId.toString()] = false;
        }
      }
      
      return statusMap;
    } catch (e) {
      throw Exception('Failed to get all surveys status');
    }
  }

  /// Show the first available survey
  static Future<bool> showFirstAvailableSurvey() async {
    try {
      final List<dynamic> surveyIds = await getSurveyIds();
      
      for (var surveyId in surveyIds) {
        final isExcluded = await isUserExcludedForSurvey(surveyId.toString());
        if (!isExcluded) {
          return await showSurveyById(surveyId.toString());
        }
      }
      
      throw Exception('No available surveys to show');
    } catch (e) {
      throw Exception('Failed to show first available survey');
    }
  }

  /// Set multiple user properties at once
  static Future<bool> setUserProperties(Map<String, String> properties) async {
    try {
      final List<Future<bool>> futures = [];
      
      properties.forEach((key, value) {
        futures.add(setUserProperty(key, value));
      });
      
      final results = await Future.wait(futures);
      return results.every((result) => result == true);
    } catch (e) {
      throw Exception('Failed to set user properties');
    }
  }
}