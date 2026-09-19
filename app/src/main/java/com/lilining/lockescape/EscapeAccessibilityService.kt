package com.lilining.lockescape

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务：
 * 1. onKeyEvent 监听物理按键（音量键连击触发逃生）
 * 2. onAccessibilityEvent 记录当前顶层包名
 */
class EscapeAccessibilityService : AccessibilityService() {

    companion object {
        const val TRIGGER_COUNT = 4          // 连击次数
        const val TRIGGER_WINDOW_MS = 1500L  // 连击时间窗口
    }

    private var lastKeyTime = 0L
    private var consecutiveCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 启动前台保活服务
        startForegroundService(Intent(this, DaemonService::class.java))
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        // 只处理音量键按下事件
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true

        val now = System.currentTimeMillis()
        if (now - lastKeyTime > TRIGGER_WINDOW_MS) {
            consecutiveCount = 0
        }
        lastKeyTime = now
        consecutiveCount++

        if (consecutiveCount >= TRIGGER_COUNT) {
            consecutiveCount = 0
            triggerEscape()
        }
        // 拦截音量键事件，避免误调音量
        return true
    }

    private fun triggerEscape() {
        val intent = Intent(this, EscapeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank() && pkg != packageName) {
                Prefs.saveCurrentTopPackage(this, pkg)
            }
        }
    }

    override fun onInterrupt() = Unit
}
