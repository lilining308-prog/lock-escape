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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSafetyTest {
    @Test
    fun criticalSystemPackagesAreProtected() {
        assertTrue(AppSafety.isProtectedPackage("com.android.systemui"))
        assertTrue(AppSafety.isProtectedPackage("com.android.settings"))
        assertTrue(AppSafety.isProtectedPackage("com.android.launcher3"))
    }

    @Test
    fun ordinaryPreinstalledPackagesAreNotBlockedByPackageFlagAlone() {
        assertFalse(AppSafety.isProtectedPackage("com.android.chrome"))
        assertFalse(AppSafety.isProtectedPackage("com.google.android.youtube"))
    }

    @Test
    fun launchersAreWhitelistedByDefault() {
        assertTrue(AppSafety.isWhitelistedPackage("com.android.launcher3", emptySet()))
        assertTrue(AppSafety.isWhitelistedPackage("com.google.android.apps.nexuslauncher", emptySet()))
        assertTrue(AppSafety.isWhitelistedPackage("com.miui.home", emptySet()))
    }

    @Test
    fun userWhitelistPackagesAreWhitelisted() {
        val userWhitelist = setOf("com.example.safe")

        assertTrue(AppSafety.isWhitelistedPackage("com.example.safe", userWhitelist))
        assertFalse(AppSafety.isWhitelistedPackage("com.example.risk", userWhitelist))
    }

    @Test
    fun whitelistedPackagesAreNotTrackedAsTopRiskPackage() {
        assertFalse(AppSafety.shouldTrackTopPackage("com.android.launcher3", emptySet()))
        assertFalse(AppSafety.shouldTrackTopPackage("com.example.safe", setOf("com.example.safe")))
        assertTrue(AppSafety.shouldTrackTopPackage("com.example.risk", setOf("com.example.safe")))
    }

    @Test
    fun packageNameValidationRejectsInvalidInput() {
        assertTrue(AppSafety.isValidPackageName("com.example.safe"))
        assertTrue(AppSafety.isValidPackageName("a.b_1.c2"))
        assertFalse(AppSafety.isValidPackageName(""))
        assertFalse(AppSafety.isValidPackageName("com example safe"))
        assertFalse(AppSafety.isValidPackageName("com.example."))
        assertFalse(AppSafety.isValidPackageName(".com.example"))
    }

    @Test
    fun normalizeWhitelistTrimsBlanksAndDropsInvalidPackages() {
        val normalized = AppSafety.normalizeUserWhitelist(
            setOf(" com.example.safe ", "", "bad package", "com.example.safe", "com.other.app")
        )

        assertTrue("com.example.safe" in normalized)
        assertTrue("com.other.app" in normalized)
        assertFalse("bad package" in normalized)
        assertFalse("" in normalized)
    }
}
