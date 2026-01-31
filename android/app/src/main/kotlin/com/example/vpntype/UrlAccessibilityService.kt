package com.example.vpntype

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
import android.widget.TextView
import android.widget.FrameLayout

class UrlAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null

    // DUMMY LIST (Native Side)
    private val unsafeLinks = listOf("malicious.com", "bad-site.org", "virus.net")

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            val node = event.source ?: return
            scanNode(node)
        }
    }

    private fun scanNode(node: AccessibilityNodeInfo?) {
        if (node == null) return

        if (node.text != null) {
            val url = node.text.toString()
            // Check if it's a URL and if it is in our UNSAFE list
            if ((url.contains("http") || url.contains("www.")) && isUnsafe(url)) {
                // SHOW THE BOX!
                showAlertBox(url)
            }
        }

        for (i in 0 until node.childCount) {
            scanNode(node.getChild(i))
        }
    }

    private fun isUnsafe(url: String): Boolean {
        for (badLink in unsafeLinks) {
            if (url.contains(badLink)) return true
        }
        return false
    }

    // --- THIS CREATES THE POPUP BOX ---
    private fun showAlertBox(url: String) {
        if (floatingView != null) return // Box is already showing

        // 1. Create the Layout Programmatically
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.parseColor("#FF0000")) // RED BACKGROUND
        layout.setPadding(30, 30, 30, 30)

        // 2. Add Text
        val textView = TextView(this)
        textView.text = "⚠️ UNSAFE LINK DETECTED!\n\nLink: $url\n\nProbability: 20% Safe"
        textView.setTextColor(Color.WHITE)
        textView.textSize = 16f
        layout.addView(textView)

        // 3. Add Close Button
        val closeBtn = Button(this)
        closeBtn.text = "CLOSE"
        closeBtn.setOnClickListener {
            removeAlertBox()
        }
        layout.addView(closeBtn)

        // 4. Set Window Parameters (Overlay)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100 // Margin from top

        // 5. Show it
        try {
            windowManager?.addView(layout, params)
            floatingView = layout
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