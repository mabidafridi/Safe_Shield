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
    
    // =========================================================================
    // SECTION: DUMMY DATA (To be replaced by API FR-3.1 later)
    // =========================================================================
    
    // 1. DUMMY UNSAFE LIST
    private val dummyUnsafeList = listOf("malicious", "bad", "virus", "gamble", "phishing")
    
    // 2. DUMMY SAFE LIST (For FR-4.6 Confirmation)
    private val dummySafeList = listOf("google", "flutter", "youtube")
    
    // 3. SESSION WHITELIST (Links the user manually approved in this session)
    private val sessionAllowedUrls = mutableListOf<String>()

    // =========================================================================

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Toast.makeText(this, "Link Guard Active", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""

        // STRATEGY: BROWSER INTERCEPTION (FR-1.4)
        // We detect if a browser is changing its content (loading a URL)
        val isBrowser = packageName.contains("chrome") || 
                        packageName.contains("browser") || 
                        packageName.contains("internet")

        if (isBrowser && (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
                          event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)) {
            
            val root = rootInActiveWindow ?: return
            val browserUrl = findBrowserUrl(root)

            if (browserUrl.isNotEmpty()) {
                processUrl(browserUrl)
            }
        }
        
        // STRATEGY: CLICK INTERCEPTION (Backup for Apps)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val source = event.source ?: return
            scanAndBlockClick(source)
            scanAndBlockClick(source.parent)
        }
    }

    // --- MAIN LOGIC ---
    private fun processUrl(url: String) {
        // 1. If user already clicked "OPEN" for this link, let it pass.
        if (sessionAllowedUrls.contains(url)) return 

        // 2. CHECK UNSAFE (Dummy List Logic)
        if (isUnsafe(url)) {
            // KILL BROWSER INSTANTLY
            performGlobalAction(GLOBAL_ACTION_BACK)
            showGatekeeperDialog(url, isSafe = false)
            return
        }

        // 3. CHECK SAFE (Dummy List Logic)
        if (isSafe(url)) {
            // KILL BROWSER INSTANTLY (To show the 'Safe' dialog as you requested)
            performGlobalAction(GLOBAL_ACTION_BACK)
            showGatekeeperDialog(url, isSafe = true)
            return
        }
    }

    private fun scanAndBlockClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = (node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")
        val lowerText = text.lowercase()

        if (lowerText.isNotEmpty()) {
            if (isUnsafe(lowerText)) {
                showGatekeeperDialog(lowerText, isSafe = false)
                return true
            }
            if (isSafe(lowerText)) {
                showGatekeeperDialog(lowerText, isSafe = true)
                return true
            }
        }
        
        for (i in 0 until node.childCount) {
            if (scanAndBlockClick(node.getChild(i))) return true
        }
        return false
    }

    // --- DUMMY DETECTION FUNCTIONS (Replace implementation later) ---
    private fun isUnsafe(text: String): Boolean {
        for (bad in dummyUnsafeList) {
            if (text.contains(bad)) return true
        }
        return false
    }

    private fun isSafe(text: String): Boolean {
        for (safe in dummySafeList) {
            if (text.contains(safe)) return true
        }
        return false
    }

    // --- HELPER: FIND URL IN BROWSER ---
    private fun findBrowserUrl(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        
        // Chrome URL bar ID
        if (node.viewIdResourceName != null && node.viewIdResourceName.contains("url_bar")) {
            return node.text?.toString() ?: ""
        }
        
        // Generic Text Check
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

    // --- UI: THE GATEKEEPER DIALOG ---
    private fun showGatekeeperDialog(url: String, isSafe: Boolean) {
        if (floatingView != null) return

        try {
            // 1. BLACKOUT BACKGROUND (Visual Blocking)
            val rootLayout = FrameLayout(this)
            rootLayout.setBackgroundColor(Color.parseColor("#F2000000")) // 95% Black
            rootLayout.setOnClickListener { } // Consume clicks

            // 2. MAIN CARD
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(Color.parseColor("#1E1E1E"))
            card.setPadding(60, 60, 60, 60)
            
            val cardParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            cardParams.gravity = Gravity.CENTER
            cardParams.setMargins(40, 0, 40, 0)
            rootLayout.addView(card, cardParams)

            // 3. HEADER
            val header = TextView(this)
            header.text = if (isSafe) "✅ SAFE LINK" else "🛡️ THREAT DETECTED"
            header.textSize = 22f
            header.setTextColor(if (isSafe) Color.GREEN else Color.RED)
            header.gravity = Gravity.CENTER
            header.setTypeface(null, android.graphics.Typeface.BOLD)
            card.addView(header)

            // 4. URL & SCORE (Dummy Score Logic)
            val info = TextView(this)
            val score = if (isSafe) "99/100 (Safe)" else "95/100 (Critical)"
            info.text = "\nLink: $url\n\nThreat Score: $score"
            info.textSize = 16f
            info.setTextColor(Color.WHITE)
            info.gravity = Gravity.CENTER
            card.addView(info)

            // 5. RISK INDICATORS (Static for now)
            val risks = TextView(this)
            risks.text = if (isSafe) 
                "\n• Verified Domain\n• SSL Valid\n• Known Safe List" 
            else 
                "\n• Phishing Pattern Detected\n• Suspicious Domain\n• User Reported"
                
            risks.textSize = 14f
            risks.setTextColor(if (isSafe) Color.GREEN else Color.YELLOW)
            risks.setPadding(0, 20, 0, 40)
            card.addView(risks)

            // 6. BUTTONS
            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.gravity = Gravity.CENTER
            card.addView(btnLayout)

            // BUTTON 1: BLOCK / IGNORE
            val btnBlock = Button(this)
            btnBlock.text = "⛔ IGNORE"
            btnBlock.setBackgroundColor(Color.RED)
            btnBlock.setTextColor(Color.WHITE)
            btnBlock.setOnClickListener {
                removeAlertBox()
                // Do NOT open browser. Just stay here.
                Toast.makeText(this, "Link Ignored", Toast.LENGTH_SHORT).show()
            }
            val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p1.rightMargin = 15
            btnLayout.addView(btnBlock, p1)

            // BUTTON 2: OPEN / PROCEED
            val btnProceed = Button(this)
            btnProceed.text = if (isSafe) "OPEN" else "⚠️ OPEN ANYWAY"
            btnProceed.setBackgroundColor(if (isSafe) Color.GREEN else Color.DKGRAY)
            btnProceed.setTextColor(if (isSafe) Color.BLACK else Color.WHITE)
            btnProceed.setOnClickListener {
                removeAlertBox()
                
                // IMPORTANT: Add to session list so we don't loop forever
                sessionAllowedUrls.add(url)
                
                // RE-LAUNCH THE BROWSER
                launchBrowser(url)
            }
            val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            btnLayout.addView(btnProceed, p2)

            // WINDOW SETUP
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            windowManager?.addView(rootLayout, params)
            floatingView = rootLayout

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Retry with http prefix if missing
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$url"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeAlertBox() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onInterrupt() {}
}