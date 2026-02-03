package com.example.vpntype // <--- CHECK YOUR PACKAGE NAME

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
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

    // DATA LISTS
    private val unsafeList = listOf("malicious", "bad", "virus", "gamble")
    private val safeList = listOf("google", "flutter", "youtube")

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Toast.makeText(this, "Shield Active", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: ""

        // STRATEGY 1: PRECISION MODE (For your App, SMS, WhatsApp)
        // We only look at what you CLICKED to avoid false alarms.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val source = event.source
            if (source != null) {
                // Check the clicked item and its parents (the container)
                if (scanNodeHierarchy(source)) return
                if (scanNodeHierarchy(source.parent)) return
            }
        }

        // STRATEGY 2: BROWSER CATCHER (For Chrome, Edge, Samsung Internet)
        // If the window changed (Browser opened), we scan the WHOLE screen.
        // This catches the link even if the 'Click' was missed.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            // Only do full scan if it looks like a browser or we are desperate
            // Common browser packages: Chrome, Samsung, Firefox, Edge
            val isBrowser = packageName.contains("chrome") || 
                            packageName.contains("browser") || 
                            packageName.contains("internet")

            if (isBrowser) {
                val root = rootInActiveWindow
                if (root != null) {
                    scanNodeHierarchy(root)
                }
            }
        }
    }

    // Recursive Scanner
    private fun scanNodeHierarchy(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // 1. Get Text
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val combinedText = "$text $desc"

        if (combinedText.isNotEmpty()) {
            // CHECK UNSAFE
            if (isUnsafe(combinedText)) {
                showGatekeeperDialog(combinedText, isSafe = false)
                return true
            }
            // CHECK SAFE
            if (isSafe(combinedText)) {
                showGatekeeperDialog(combinedText, isSafe = true)
                return true
            }
        }

        // 2. Check Children (Recursion)
        for (i in 0 until node.childCount) {
            if (scanNodeHierarchy(node.getChild(i))) return true
        }
        
        return false
    }

    private fun isUnsafe(text: String): Boolean {
        for (bad in unsafeList) {
            if (text.contains(bad)) return true
        }
        return false
    }

    private fun isSafe(text: String): Boolean {
        for (safe in safeList) {
            if (text.contains(safe)) return true
        }
        return false
    }

    // --- GATEKEEPER UI ---
    private fun showGatekeeperDialog(url: String, isSafe: Boolean) {
        if (floatingView != null) return // Already showing

        try {
            // 1. WALL (Black Background)
            val rootLayout = FrameLayout(this)
            rootLayout.setBackgroundColor(Color.parseColor("#E6000000")) 
            rootLayout.setOnClickListener { } // Block touches

            // 2. DIALOG BOX
            val dialogBox = LinearLayout(this)
            dialogBox.orientation = LinearLayout.VERTICAL
            dialogBox.setBackgroundColor(Color.parseColor("#202020"))
            dialogBox.setPadding(50, 50, 50, 50)
            
            val boxParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            boxParams.gravity = Gravity.CENTER
            boxParams.leftMargin = 50
            boxParams.rightMargin = 50
            rootLayout.addView(dialogBox, boxParams)

            // 3. TEXT
            val title = TextView(this)
            title.text = if (isSafe) "VERIFIED LINK" else "⚠️ THREAT DETECTED"
            title.setTextColor(if (isSafe) Color.GREEN else Color.RED)
            title.textSize = 22f
            title.gravity = Gravity.CENTER
            title.setTypeface(null, android.graphics.Typeface.BOLD)
            dialogBox.addView(title)

            val message = TextView(this)
            message.text = "\nLink detected:\n$url\n\nChoose Action:"
            message.setTextColor(Color.WHITE)
            message.textSize = 16f
            message.gravity = Gravity.CENTER
            message.setPadding(0, 20, 0, 40)
            dialogBox.addView(message)

            // 4. BUTTONS
            val btnLayout = LinearLayout(this)
            btnLayout.orientation = LinearLayout.HORIZONTAL
            btnLayout.gravity = Gravity.CENTER
            dialogBox.addView(btnLayout)

            // BLOCK BUTTON
            val btnIgnore = Button(this)
            btnIgnore.text = "BLOCK"
            btnIgnore.setBackgroundColor(Color.RED)
            btnIgnore.setTextColor(Color.WHITE)
            btnIgnore.setOnClickListener {
                removeAlertBox()
                performGlobalAction(GLOBAL_ACTION_BACK) // KILL BROWSER
                Toast.makeText(this, "Blocked", Toast.LENGTH_SHORT).show()
            }
            val p1 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p1.rightMargin = 10
            btnLayout.addView(btnIgnore, p1)

            // OPEN BUTTON
            val btnOpen = Button(this)
            btnOpen.text = "OPEN"
            btnOpen.setBackgroundColor(Color.GREEN)
            btnOpen.setTextColor(Color.BLACK)
            btnOpen.setOnClickListener {
                removeAlertBox() // LET BROWSER SHOW
            }
            val p2 = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            p2.leftMargin = 10
            btnLayout.addView(btnOpen, p2)

            // 5. PARAMS
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

    private fun removeAlertBox() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }

    override fun onInterrupt() {}
}