package com.example.vpntype // <--- CHECK YOUR PACKAGE NAME

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.vpntype/settings"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // MASTER SWITCH
                "setProtectionState" -> {
                    val isEnabled = call.argument<Boolean>("isEnabled") ?: true
                    AppConfig.isProtectionEnabled = isEnabled
                    result.success(true)
                }
                "getProtectionState" -> {
                    result.success(AppConfig.isProtectionEnabled)
                }

                // HISTORY
                "getThreatHistory" -> {
                    val history = ThreatLogger.getHistory().map { 
                        mapOf(
                            "url" to it.url,
                            "score" to it.score,
                            "action" to it.action,
                            "timestamp" to it.timestamp
                        )
                    }
                    result.success(history)
                }
                "clearHistory" -> {
                    ThreatLogger.clear()
                    result.success(true)
                }

                // WHITELIST
                "getWhitelist" -> {
                    result.success(WhitelistManager.getDomains())
                }
                "addToWhitelist" -> {
                    val domain = call.argument<String>("domain")
                    if (domain != null) {
                        WhitelistManager.addDomain(domain)
                        result.success(true)
                    } else {
                        result.error("INVALID", "Domain is null", null)
                    }
                }
                "removeFromWhitelist" -> {
                    val domain = call.argument<String>("domain")
                    if (domain != null) {
                        WhitelistManager.removeDomain(domain)
                        result.success(true)
                    } else {
                        result.error("INVALID", "Domain is null", null)
                    }
                }

                // SETTINGS
                "updateSettings" -> {
                    val autoBlock = call.argument<Boolean>("autoBlock")
                    val showSafe = call.argument<Boolean>("showSafe")
                    val threshold = call.argument<Int>("threshold")

                    if (autoBlock != null) AppConfig.autoBlockEnabled = autoBlock
                    if (showSafe != null) AppConfig.showSafeConfirmations = showSafe
                    if (threshold != null) AppConfig.threatThreshold = threshold

                    result.success(true)
                }
                "getSettings" -> {
                    val settings = mapOf(
                        "autoBlock" to AppConfig.autoBlockEnabled,
                        "showSafe" to AppConfig.showSafeConfirmations,
                        "threshold" to AppConfig.threatThreshold
                    )
                    result.success(settings)
                }

                else -> result.notImplemented()
            }
        }
    }
}