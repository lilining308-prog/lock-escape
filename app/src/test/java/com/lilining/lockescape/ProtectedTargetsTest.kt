/*
 * Copyright (C) 2026 liyan
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.lilining.lockescape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedTargetsTest {
    @Test fun activeDefaultHomeIsBlockedEvenWhenItIsNotOnStaticList() {
        val policy = ProtectedTargets("com.lilining.lockescape", "org.example.customhome", "org.example.keyboard")
        assertFalse(policy.canActOn("org.example.customhome", emptySet()))
        assertFalse(policy.canActOn("org.example.keyboard", emptySet()))
        assertFalse(policy.canActOn("com.lilining.lockescape", emptySet()))
        assertFalse(policy.canActOn("com.android.systemui", emptySet()))
        assertFalse(policy.canActOn("com.example.safe", setOf("com.example.safe")))
        assertTrue(policy.canActOn("com.example.locker", emptySet()))
    }

    @Test fun unresolvedHomeDoesNotBlockOrdinaryPackages() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertTrue(policy.canActOn("com.example.locker", emptySet()))
        assertFalse(policy.canActOn("", emptySet()))
    }

    @Test fun aNewDefaultHomeImmediatelySupersedesTheOldOne() {
        val previous = ProtectedTargets("com.lilining.lockescape", "org.example.oldhome", null)
        val current = ProtectedTargets("com.lilining.lockescape", "org.example.newhome", null)
        assertTrue(previous.canActOn("org.example.newhome", emptySet()))
        assertFalse(current.canActOn("org.example.newhome", emptySet()))
    }

    @Test fun policiesForAnotherPackageStillProtectThemselves() {
        val policy = ProtectedTargets("org.example.escape", "org.example.home", "org.example.ime")
        assertFalse(policy.canActOn("org.example.escape", emptySet()))
        assertFalse(policy.canActOn("org.example.home", emptySet()))
        assertFalse(policy.canActOn("org.example.ime", emptySet()))
    }

    @Test fun resolverPackageIsNotConsideredARealHome() {
        assertTrue(ProtectedTargets.resolvedHomePackage("android") == null)
        assertTrue(ProtectedTargets.resolvedHomePackage("org.example.home") == "org.example.home")
    }

    @Test fun blankResolvedHomeDoesNotBlockEmptyPackageViaNameEquality() {
        assertTrue(ProtectedTargets.resolvedHomePackage("") == null)
        assertTrue(ProtectedTargets.resolvedHomePackage(null) == null)
    }

    @Test fun knownSystemAppsRemainProtectedRegardlessOfHomeResolution() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertFalse(policy.canActOn("com.android.settings", emptySet()))
        assertFalse(policy.canActOn("com.google.android.gms", emptySet()))
    }

    @Test fun userWhitelistOverrideIsAppliedAlongsideDeviceSpecificRules() {
        val policy = ProtectedTargets("com.lilining.lockescape", "org.example.home", "org.example.ime")
        assertFalse(policy.canActOn("com.example.good", setOf("com.example.good")))
        assertTrue(policy.canActOn("com.example.locker", setOf("com.example.good")))
    }

    @Test fun theDefaultLauncherIsRejectedEvenWithoutAResolvedDeviceHome() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertFalse(policy.canActOn("com.android.launcher3", emptySet()))
        assertFalse(policy.canActOn("com.miui.home", emptySet()))
    }

    @Test fun packageMatchingTheActiveInputMethodIsRejectedEvenWhenValid() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, "org.example.keyboard")
        assertFalse(policy.canActOn("org.example.keyboard", emptySet()))
    }

    @Test fun malformedPackageIsRejectedBeforeDynamicProtectionRules() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertFalse(policy.canActOn("com.example.bad;sh", emptySet()))
    }

    @Test fun validOrdinaryTargetDoesNotDependOnHomeBeingInstalled() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertTrue(policy.canActOn("com.example.locker", emptySet()))
    }

    @Test fun currentHomeIsProtectedEvenWhenItIsAnOrdinaryThirdPartyApp() {
        val policy = ProtectedTargets("com.lilining.lockescape", "com.example.customhome", null)
        assertFalse(policy.canActOn("com.example.customhome", emptySet()))
    }
}
