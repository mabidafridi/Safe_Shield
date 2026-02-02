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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class UrlAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    // UNSAFE LIST
    private val unsafeList = listOf("malicious", "bad", "virus", "gamble")
    
    // SAFE LIST
    private val safeList = listOf("google", "flutter", "youtube")

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Toast.makeText(this, "Precision Shield Active", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // ---------------------------------------------------------
        // FIX: STRICT CLICK MODE
        // We ONLY look at TYPE_VIEW_CLICKED.
        // We ONLY look at event.source (The item you touched).
        // We DO NOT look at the whole screen anymore.
        // ---------------------------------------------------------
        
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val clickedNode = event.source
            if (clickedNode != null) {
                // Only scan the specific thing the user clicked (and its children)
                scanClickedNode(clickedNode)
            }
        }
    }

    private fun scanClickedNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // 1. Check the node itself
        if (node.text != null && node.text.isNotEmpty()) {
            val clickedText = node.text.toString().lowercase()
            
            // LOGIC: Only block if THIS specific text is unsafe
            if (isUnsafe(clickedText)) {
                if (floatingView == null) {
                    showFullScreenBlocker(clickedText)
                }
                return
            }
        }

        // 2. IMPORTANT: Sometimes we click a "Card" that holds the text inside it.
        // So we must check the children of the CLICKED item only.
        for (i in 0 until node.childCount) {
            scanClickedNode(node.getChild(i))
        }
    }

    private fun isUnsafe(text: String): Boolean {
        for (bad in unsafeList) {
            if (text.contains(bad)) return true
        }
        return false
    }

    // --- FULL SCREEN BLOCKER (Previous Code) ---
    private fun showFullScreenBlocker(detectedText: String) {
        try {
            val scrollView = ScrollView(this)
            scrollView.setBackgroundColor(Color.parseColor("#E6000000")) 
            scrollView.isFillViewport = true

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.setPadding(60, 60, 60, 60)
            scrollView.addView(layout)

            val title = TextView(this)
            title.text = "⛔ \nWEBSITE BLOCKED"
            title.setTextColor(Color.RED)
            title.textSize = 26f
            title.gravity = Gravity.CENTER
            title.setTypeface(null, android.graphics.Typeface.BOLD)
            layout.addView(title)

            val warning = TextView(this)
            warning.text = "\nDetected Link:\n$detectedText\n\nThis website is in your Blocklist."
            warning.setTextColor(Color.WHITE)
            warning.textSize = 16f
            warning.gravity = Gravity.CENTER
            warning.setPadding(0, 30, 0, 60)
            layout.addView(warning)

            val btnBlock = Button(this)
            btnBlock.text = "⛔ BLOCK & GO BACK"
            btnBlock.setBackgroundColor(Color.RED)
            btnBlock.setTextColor(Color.WHITE)
            btnBlock.setOnClickListener {
                performGlobalAction(GLOBAL_ACTION_BACK)
                removeAlertBox()
            }
            layout.addView(btnBlock)

            val spacer = TextView(this)
            spacer.height = 40
            layout.addView(spacer)

            val btnProceed = Button(this)
            btnProceed.text = "✅ OPEN (I TAKE THE RISK)"
            btnProceed.setBackgroundColor(Color.GREEN)
            btnProceed.setTextColor(Color.BLACK)
            btnProceed.setOnClickListener {
                removeAlertBox()
            }
            layout.addView(btnProceed)

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

            windowManager?.addView(scrollView, params)
            floatingView = scrollView
            
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