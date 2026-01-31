package com.example.vpntype  // <--- MAKE SURE THIS IS CORRECT

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent

class UrlAccessibilityService : AccessibilityService() {

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
            val text = node.text.toString()
            if (text.contains("http") || text.contains("www.")) {
                sendUrlToFlutter(text)
            }
        }

        for (i in 0 until node.childCount) {
            scanNode(node.getChild(i))
        }
    }

    private fun sendUrlToFlutter(url: String) {
        // MAKE SURE THIS STRING MATCHES YOUR PACKAGE NAME
        val intent = Intent("com.example.vpntype.URL_DETECTED") 
        intent.putExtra("url", url)
        sendBroadcast(intent)
    }

    override fun onInterrupt() {}
}