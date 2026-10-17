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
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 无障碍服务：
 * 1. onKeyEvent 监听物理按键（长按音量下键强制弹出逃生界面，连按4次为备用触发）
 * 2. onAccessibilityEvent 记录当前顶层包名
 */
class EscapeAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val volumeKeyTrigger = VolumeKeyTrigger()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var shieldRepository: ShieldRepository
    private var longPressTask: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        shieldRepository = ShieldRepository(this)
        // 启动前台保活服务
        startForegroundService(Intent(this, DaemonService::class.java))
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return false
        // 只处理音量键事件
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                val decision = volumeKeyTrigger.onVolumeDown(
                    event.keyCode,
                    event.eventTime,
                    isRepeat = event.repeatCount > 0
                )
                if (decision.scheduleLongPress) {
                    cancelLongPress()
                    longPressTask = Runnable { triggerEscape() }
                    handler.postDelayed(longPressTask!!, VolumeKeyTrigger.LONG_PRESS_MS)
                }
                if (decision.triggerEscape) {
                    triggerEscape()
                }
            }
            KeyEvent.ACTION_UP -> {
                val decision = volumeKeyTrigger.onVolumeUp(event.keyCode)
                if (decision.cancelLongPress) {
                    cancelLongPress()
                }
            }
        }
        // 只观察按键；普通调音量仍交给系统处理。
        return false
    }

    private fun cancelLongPress() {
        longPressTask?.let { handler.removeCallbacks(it) }
        longPressTask = null
    }

    private fun triggerEscape() {
        cancelLongPress()
        volumeKeyTrigger.resetAfterTrigger()
        val intent = Intent(this, EscapeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (!pkg.isNullOrBlank()) {
                // The rescue console temporarily covers the suspect app. Keep the target
                // until it expires; other windows revoke it to prevent stale targeting.
                if (pkg == packageName) return
                val policy = ProtectedTargets.from(this)
                val whitelist = Prefs.userWhitelist(this)
                // Returning to a launcher means the user left the suspect app — revoke.
                if (AppSafety.isDefaultWhitelistedPackage(pkg)) {
                    Prefs.clearCurrentTopPackage(this)
                    return
                }
                // Keyboard / volume dialog / other system UI must not become a new target,
                // but they must not wipe a still-valid rescue target either — otherwise
                // the volume-key trigger itself erases the package the user just captured.
                if (pkg == policy.inputMethodPackage || AppSafety.isProtectedPackage(pkg)) {
                    return
                }
                if (!policy.canActOn(pkg, whitelist)) {
                    Prefs.clearCurrentTopPackage(this)
                    return
                }
                Prefs.saveCurrentTopPackage(this, pkg)
                if (::shieldRepository.isInitialized) {
                    serviceScope.launch {
                        if (shieldRepository.isBlacklisted(pkg)) {
                            val label = runCatching {
                                packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                            }.getOrDefault(pkg)
                            shieldRepository.addEvent(pkg, label, "accessibility", "warning", "blacklist-hit")
                            launchShieldWarning(pkg)
                        }
                    }
                }
            }
        }
    }

    private fun launchShieldWarning(pkg: String) {
        val intent = Intent(this, EscapeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("shield_target_package", pkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        cancelLongPress()
        serviceScope.cancel()
        super.onDestroy()
    }
}
