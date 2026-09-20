/*
 * Copyright (C) 2026 liyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.lilining.lockescape

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务：
 * 1. onKeyEvent 监听物理按键（长按音量下键强制弹出逃生界面，连按4次为备用触发）
 * 2. onAccessibilityEvent 记录当前顶层包名
 */
class EscapeAccessibilityService : AccessibilityService() {

    companion object {
        const val TRIGGER_COUNT = 4          // 备用触发：连击次数
        const val TRIGGER_WINDOW_MS = 1500L  // 连击时间窗口
        const val LONG_PRESS_MS = 1500L      // 长按音量下键触发阈值
    }

    private var lastKeyTime = 0L
    private var consecutiveCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var longPressTask: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("EscapeA11y", "serviceInfo.flags=${serviceInfo.flags} FLAG_REQUEST_FILTER_KEY_EVENTS=${android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS}")
        // 启动前台保活服务
        startForegroundService(Intent(this, DaemonService::class.java))
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        Log.i("EscapeA11y", "onKeyEvent: keyCode=${event.keyCode} action=${event.action} source=${event.source}")
        // 只处理音量键事件
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // 长按音量下键：按住不放持续 LONG_PRESS_MS 触发强制逃生
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    cancelLongPress()
                    longPressTask = Runnable { triggerEscape() }
                    handler.postDelayed(longPressTask!!, LONG_PRESS_MS)
                }
                // 保留连按4次备用触发（音量上/下都算）
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
            }
            KeyEvent.ACTION_UP -> {
                // 松开音量下键：取消长按计时
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    cancelLongPress()
                }
            }
        }
        // 拦截音量键事件，避免误调音量
        return true
    }

    private fun cancelLongPress() {
        longPressTask?.let { handler.removeCallbacks(it) }
        longPressTask = null
    }

    private fun triggerEscape() {
        cancelLongPress()
        consecutiveCount = 0
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
