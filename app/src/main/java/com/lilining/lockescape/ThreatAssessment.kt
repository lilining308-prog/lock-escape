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

object ThreatAssessment {
    enum class Risk {
        IGNORED,
        LOW,
        MEDIUM,
        HIGH,
        PROTECTED
    }

    data class Signals(
        val packageName: String,
        val appLabel: String = "",
        val hasAccessibilityService: Boolean = false,
        val requestsOverlay: Boolean = false,
        val hasDeviceAdminReceiver: Boolean = false,
        val isWhitelisted: Boolean = false,
        val isProtectedSystem: Boolean = false
    )

    data class Profile(
        val risk: Risk,
        val score: Int,
        val title: String,
        val reasons: List<String>,
        val recommendation: String
    )

    fun assess(signals: Signals): Profile {
        if (signals.isWhitelisted) {
            return Profile(
                risk = Risk.IGNORED,
                score = 0,
                title = "已加入白名单",
                reasons = listOf("白名单应用不会触发逃生动作"),
                recommendation = "无需处理；如误加，可从白名单移除。"
            )
        }
        if (signals.isProtectedSystem) {
            return Profile(
                risk = Risk.PROTECTED,
                score = 0,
                title = "系统关键组件",
                reasons = listOf("该包属于系统关键保护列表"),
                recommendation = "禁止停用或卸载，避免设备不可用。"
            )
        }

        var score = 0
        val reasons = mutableListOf<String>()
        if (signals.hasAccessibilityService) {
            score += 3
            reasons += "声明无障碍服务，可能拦截按键或覆盖窗口"
        }
        if (signals.requestsOverlay) {
            score += 2
            reasons += "请求悬浮窗权限，可能显示锁屏覆盖层"
        }
        if (signals.hasDeviceAdminReceiver) {
            score += 3
            reasons += "声明设备管理器组件，可能阻止卸载或锁屏"
        }

        val risk = when {
            score >= 5 -> Risk.HIGH
            score >= 3 -> Risk.MEDIUM
            else -> Risk.LOW
        }
        return Profile(
            risk = risk,
            score = score,
            title = when (risk) {
                Risk.HIGH -> "高风险目标"
                Risk.MEDIUM -> "可疑目标"
                Risk.LOW -> "低风险或信息不足"
                else -> "已处理"
            },
            reasons = reasons.ifEmpty { listOf("未发现典型锁机组件声明，仍需结合当前界面判断") },
            recommendation = when (risk) {
                Risk.HIGH -> "建议先强制停止；若反复拉起，再停用或卸载。"
                Risk.MEDIUM -> "建议先强制停止并观察；确认恶意后再执行破坏性动作。"
                Risk.LOW -> "建议谨慎处理，避免误伤正常应用。"
                else -> "无需处理。"
            }
        )
    }
}
