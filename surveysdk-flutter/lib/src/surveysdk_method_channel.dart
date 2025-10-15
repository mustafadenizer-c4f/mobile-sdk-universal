import 'package:flutter/services.dart';

/// Survey SDK for Flutter
class SurveySDK {
  static const MethodChannel _channel = MethodChannel('surveysdk');

  /// Initialize the Survey SDK
  ///
  /// [apiKey] - Your Survey SDK API key
  static Future<void> initialize({required String apiKey}) async {
    try {
      await _channel.invokeMethod('initialize', {'apiKey': apiKey});
    } on PlatformException catch (e) {
      throw Exception('Failed to initialize Survey SDK: ${e.message}');
    }
  }

  /// Show the survey modal
  static Future<void> showSurvey() async {
    try {
      await _channel.invokeMethod('showSurvey');
    } on PlatformException catch (e) {
      throw Exception('Failed to show survey: ${e.message}');
    }
  }

  /// Set user property for targeting
  ///
  /// [key] - Property key
  /// [value] - Property value
  static Future<void> setUserProperty({
    required String key,
    required String value,
  }) async {
    try {
      await _channel.invokeMethod('setUserProperty', {
        'key': key,
        'value': value,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to set user property: ${e.message}');
    }
  }

  /// Set multiple user properties at once
  ///
  /// [properties] - Map of key-value pairs
  static Future<void> setUserProperties(Map<String, String> properties) async {
    try {
      final List<Future<void>> futures = properties.entries.map((entry) {
        return setUserProperty(key: entry.key, value: entry.value);
      }).toList();

      await Future.wait(futures);
    } on PlatformException catch (e) {
      throw Exception('Failed to set user properties: ${e.message}');
    }
  }

  /// Track custom event
  ///
  /// [eventName] - Name of the event
  /// [properties] - Optional event properties
  static Future<void> trackEvent({
    required String eventName,
    Map<String, dynamic> properties = const {},
  }) async {
    try {
      await _channel.invokeMethod('trackEvent', {
        'eventName': eventName,
        'properties': properties,
      });
    } on PlatformException catch (e) {
      throw Exception('Failed to track event: ${e.message}');
    }
  }
}