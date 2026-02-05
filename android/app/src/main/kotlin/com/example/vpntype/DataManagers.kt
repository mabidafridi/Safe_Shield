package com.example.vpntype

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// DATA MANAGERS (Shared between Service and UI)
// ============================================================================

object AppConfig {
    var protectionLevel = "Balanced" // Maximum, Balanced, Minimal
    var autoBlockEnabled = false
    var showSafeConfirmations = false
    var loggingEnabled = true
    var threatThreshold = 70
    var isProtectionEnabled = true
}

object WhitelistManager {
    // Initial dummy data
    private val domains = mutableListOf("google.com", "youtube.com", "flutter.dev")

    fun isWhitelisted(url: String): Boolean {
        return domains.any { url.contains(it) }
    }

    fun addDomain(domain: String) {
        if (!domains.contains(domain)) domains.add(domain)
    }

    // --- FIXES 'Unresolved reference: removeDomain' ---
    fun removeDomain(domain: String) {
        domains.remove(domain)
    }

    // --- FIXES 'Unresolved reference: getDomains' ---
    fun getDomains(): List<String> {
        return domains.toList()
    }
}

object ThreatLogger {
    data class LogEntry(val url: String, val score: Int, val action: String, val timestamp: String)
    private val history = mutableListOf<LogEntry>()

    fun log(url: String, score: Int, action: String) {
        if (!AppConfig.loggingEnabled) return
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        history.add(LogEntry(url, score, action, time))
    }

    // --- REQUIRED FOR HISTORY UI ---
    fun getHistory(): List<LogEntry> {
        return history.toList()
    }

    fun clear() {
        history.clear()
    }
}