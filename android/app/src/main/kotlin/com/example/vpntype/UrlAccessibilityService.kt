package com.example.vpntype // <--- CHECK YOUR PACKAGE NAME

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class UrlAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    
    // Session cache to prevent looping
    private val sessionAllowedUrls = mutableListOf<String>()

    // DUMMY AI DATA
    private val unsafePatterns = listOf("malicious", "bad", "virus", "gamble", "phishing")

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Toast.makeText(this, "Link Shield Active", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // MASTER SWITCH CHECK
        if (!AppConfig.isProtectionEnabled) return

        val packageName = event.packageName?.toString() ?: ""

        // 1. Browser Detection
        val isBrowser = packageName.contains("chrome") || 
                        packageName.contains("browser") || 
                        packageName.contains("internet")

        if (isBrowser && (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
                          event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)) {
            val root = rootInActiveWindow ?: return
            val url = findBrowserUrl(root)
            if (url.isNotEmpty()) processUrl(url)
        }

        // 2. Click Detection (Messaging Apps)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val source = event.source ?: return
            scanAndBlockClick(source)
            scanAndBlockClick(source.parent)
        }
    }

    private fun processUrl(url: String) {
        if (sessionAllowedUrls.contains(url)) return

        if (WhitelistManager.isWhitelisted(url)) {
            ThreatLogger.log(url, 0, "WHITELISTED")
            return 
        }

        val threatScore = calculateDummyScore(url)

        if (threatScore < AppConfig.threatThreshold) {
            ThreatLogger.log(url, threatScore, "SAFE_ACCESS")
            if (AppConfig.showSafeConfirmations) {
                showSafeToast(url)
            }
            return 
        }

        // INTERCEPT!
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        if (AppConfig.autoBlockEnabled && threatScore >= 90) {
            ThreatLogger.log(url, threatScore, "AUTO_BLOCKED")
            Toast.makeText(this, "Auto-Blocked: $url", Toast.LENGTH_LONG).show()
            return
        }

        showThreatWarning(url, threatScore)
    }

    private fun calculateDummyScore(url: String): Int {
        if (unsafePatterns.any { url.contains(it) }) return 95
        return 10 
    }

    private fun scanAndBlockClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        val lowerText = text.lowercase()

        if (lowerText.isNotEmpty()) {
            if (WhitelistManager.isWhitelisted(lowerText)) return false
            
            val score = calculateDummyScore(lowerText)
            if (score >= AppConfig.threatThreshold) {
                showThreatWarning(lowerText, score)
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            if (scanAndBlockClick(node.getChild(i))) return true
        }
        return false
    }

    private fun showThreatWarning(url: String, score: Int) {
        if (floatingView != null) return

        try {
            val rootLayout = FrameLayout(this)
            rootLayout.setBackgroundColor(Color.parseColor("#F2000000")) 
            rootLayout.setOnClickListener { } 

            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(Color.parseColor("#1E1E1E"))
            card.setPadding(60, 60, 60, 60)
            val cardParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            cardParams.gravity = Gravity.CENTER
            cardParams.setMargins(40, 0, 40, 0)
            rootLayout.addView(card, cardParams)

            val header = TextView(this)
            header.text = "🛡️ THREAT DETECTED"
            header.textSize = 22f
            header.setTextColor(Color.RED)
            header.gravity = Gravity.CENTER
            header.setTypeface(null, android.graphics.Typeface.BOLD)
            card.addView(header)

            val info = TextView(this)
            info.text = "\nLink: $url\n\nThreat Score: $score/100"
            info.textSize = 16f
            info.setTextColor(Color.WHITE)
            info.gravity = Gravity.CENTER
            card.addView(info)

            val risks = TextView(this)
            risks.text = "\nRisk Indicators:\n• Suspicious Pattern\n• Unverified Domain"
            risks.textSize = 14f
            risks.setTextColor(Color.YELLOW)
            risks.setPadding(0, 20, 0, 40)
            card.addView(risks)

            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.gravity = Gravity.CENTER
            card.addView(btnLayout)

            val btnBlock = Button(this)
            btnBlock.text = "⛔ BLOCK"
            btnBlock.setBackgroundColor(Color.RED)
            btnBlock.setTextColor(Color.WHITE)
            btnBlock.setOnClickListener {
                ThreatLogger.log(url, score, "USER_BLOCKED")
                removeAlertBox()
                performGlobalAction(GLOBAL_ACTION_BACK) 
                Toast.makeText(this, "Url Blocked", Toast.LENGTH_SHORT).show()
            }
            val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p1.rightMargin = 15
            btnLayout.addView(btnBlock, p1)

            val btnProceed = Button(this)
            btnProceed.text = "PROCEED"
            btnProceed.setBackgroundColor(Color.DKGRAY)
            btnProceed.setTextColor(Color.WHITE)
            btnProceed.setOnClickListener {
                ThreatLogger.log(url, score, "USER_PROCEEDED")
                removeAlertBox()
                sessionAllowedUrls.add(url) 
                launchBrowser(url)
            }
            val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnLayout.addView(btnProceed, p2)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            windowManager?.addView(rootLayout, params)
            floatingView = rootLayout

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSafeToast(url: String) {
        Toast.makeText(this, "✅ Safe: $url", Toast.LENGTH_SHORT).show()
    }

    private fun launchBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
             try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$url"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {}
        }
    }

    private fun findBrowserUrl(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        if (node.viewIdResourceName != null && node.viewIdResourceName.contains("url_bar")) {
            return node.text?.toString() ?: ""
        }
        if (node.text != null) {
            val t = node.text.toString()
            if (t.startsWith("http") || t.contains("www.")) return t
        }
        for (i in 0 until node.childCount) {
            val res = findBrowserUrl(node.getChild(i))
            if (res.isNotEmpty()) return res
        }
        return ""
    }

    private fun removeAlertBox() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onInterrupt() {}
}