import 'package:flutter/services.dart';

class NativeApi {
  static const MethodChannel _channel = MethodChannel('com.example.vpntype/settings');

  // MASTER SWITCH
  static Future<void> setProtectionState(bool isEnabled) async {
    await _channel.invokeMethod('setProtectionState', {'isEnabled': isEnabled});
  }

  static Future<bool> getProtectionState() async {
    final bool result = await _channel.invokeMethod('getProtectionState');
    return result;
  }

  // THREAT HISTORY
  static Future<List<Map<String, dynamic>>> getHistory() async {
    final List<dynamic> result = await _channel.invokeMethod('getThreatHistory');
    return result.cast<Map<String, dynamic>>();
  }

  static Future<void> clearHistory() async {
    await _channel.invokeMethod('clearHistory');
  }

  // WHITELIST
  static Future<List<String>> getWhitelist() async {
    final List<dynamic> result = await _channel.invokeMethod('getWhitelist');
    return result.cast<String>();
  }

  static Future<void> addToWhitelist(String domain) async {
    await _channel.invokeMethod('addToWhitelist', {'domain': domain});
  }

  static Future<void> removeFromWhitelist(String domain) async {
    await _channel.invokeMethod('removeFromWhitelist', {'domain': domain});
  }

  // SETTINGS
  static Future<void> updateSettings({
    required bool autoBlock,
    required bool showSafe,
    required int threshold,
  }) async {
    await _channel.invokeMethod('updateSettings', {
      'autoBlock': autoBlock,
      'showSafe': showSafe,
      'threshold': threshold,
    });
  }

  static Future<Map<String, dynamic>> getSettings() async {
    final Map<dynamic, dynamic> result = await _channel.invokeMethod('getSettings');
    return Map<String, dynamic>.from(result);
  }
}