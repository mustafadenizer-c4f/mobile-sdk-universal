import 'dart:async';
import 'package:flutter/services.dart';

class SurveySDK {
  static const MethodChannel _channel = MethodChannel('surveysdk_flutter');

  static Future<bool> initialize(String apiKey) async {
    try {
      final bool result = await _channel.invokeMethod('initialize', {'apiKey': apiKey});
      return result;
    } on PlatformException catch (e) {
      print("Failed to initialize Survey SDK: '${e.message}'");
      return false;
    }
  }

  static Future<bool> showSurvey() async {
    try {
      final bool result = await _channel.invokeMethod('showSurvey');
      return result;
    } on PlatformException catch (e) {
      print("Failed to show survey: '${e.message}'");
      return false;
    }
  }

  static Future<bool> setUserProperty(String key, String value) async {
    try {
      final bool result = await _channel.invokeMethod(
        'setUserProperty', 
        {'key': key, 'value': value}
      );
      return result;
    } on PlatformException catch (e) {
      print("Failed to set user property: '${e.message}'");
      return false;
    }
  }

  static Future<bool> isUserExcluded() async {
    try {
      final bool result = await _channel.invokeMethod('isUserExcluded');
      return result;
    } on PlatformException catch (e) {
      print("Failed to check user exclusion: '${e.message}'");
      return false;
    }
  }

  static Future<String> getDebugStatus() async {
    try {
      final String result = await _channel.invokeMethod('getDebugStatus');
      return result;
    } on PlatformException catch (e) {
      return "Error: ${e.message}";
    }
  }
}