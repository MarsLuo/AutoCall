package com.marsluo.autocall

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast

class AutoCallAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "无障碍服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName == "com.android.incallui") {
                // 找到挂断按钮并点击
                findAndClickEndCallButton()
            }
        }
    }

    private fun findAndClickEndCallButton() {
        val rootNode = rootInActiveWindow ?: return
        
        // 查找挂断按钮
        val endCallButton = rootNode.findAccessibilityNodeInfosByViewId("com.android.incallui:id/end_call")
        if (endCallButton.isNotEmpty()) {
            val button = endCallButton[0]
            if (button.isClickable) {
                button.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    private fun removeOverlay() {
        if (overlayView != null && windowManager != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }
} 