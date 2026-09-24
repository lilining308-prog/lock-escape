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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatAssessmentTest {
    @Test
    fun accessibilityOverlayAndAdminProduceHighRiskProfile() {
        val profile = ThreatAssessment.assess(
            ThreatAssessment.Signals(
                packageName = "com.bad.locker",
                hasAccessibilityService = true,
                requestsOverlay = true,
                hasDeviceAdminReceiver = true,
                isWhitelisted = false,
                isProtectedSystem = false
            )
        )

        assertEquals(ThreatAssessment.Risk.HIGH, profile.risk)
        assertTrue(profile.reasons.any { it.contains("无障碍") })
        assertTrue(profile.reasons.any { it.contains("悬浮窗") })
        assertTrue(profile.reasons.any { it.contains("设备管理器") })
    }

    @Test
    fun whitelistedPackageIsIgnoredEvenWithDangerousSignals() {
        val profile = ThreatAssessment.assess(
            ThreatAssessment.Signals(
                packageName = "com.safe.app",
                hasAccessibilityService = true,
                requestsOverlay = true,
                hasDeviceAdminReceiver = true,
                isWhitelisted = true,
                isProtectedSystem = false
            )
        )

        assertEquals(ThreatAssessment.Risk.IGNORED, profile.risk)
    }
}
