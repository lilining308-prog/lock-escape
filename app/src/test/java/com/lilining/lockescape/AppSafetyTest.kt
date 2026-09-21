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
}
