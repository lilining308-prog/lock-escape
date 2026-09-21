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
