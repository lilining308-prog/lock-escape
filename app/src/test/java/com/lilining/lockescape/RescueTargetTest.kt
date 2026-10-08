/*
 * Copyright (C) 2026 liyan
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.lilining.lockescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RescueTargetTest {
    @Test fun switchingToOwnWindowKeepsLastExternalTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet())
        target.onWindow("com.lilining.lockescape", "com.lilining.lockescape", emptySet())
        assertEquals("com.example.locker", target.current())
    }

    @Test fun switchingToKeyboardOrSystemWindowKeepsLastExternalTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet())
        target.onWindow("com.google.android.inputmethod.latin", "com.lilining.lockescape", emptySet(), "com.google.android.inputmethod.latin")
        target.onWindow("com.android.systemui", "com.lilining.lockescape", emptySet())
        assertEquals("com.example.locker", target.current())
    }

    @Test fun switchingToAnotherAppReplacesTargetAndWhitelistRevokesIt() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet())
        target.onWindow("com.example.other", "com.lilining.lockescape", emptySet())
        assertEquals("com.example.other", target.current())
        assertFalse(target.isActionable("com.example.other", "com.lilining.lockescape", setOf("com.example.other")))
        assertTrue(target.isActionable("com.example.other", "com.lilining.lockescape", emptySet()))
    }

    @Test fun oldTargetExpiresInsteadOfActingOnUnrelatedApp() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        assertEquals("", target.current(nowMs = 120_101L))
    }

    @Test fun invalidAndProtectedTargetsAreNotActionable() {
        val target = RescueTarget()
        assertFalse(target.isActionable("com.example.locker; reboot", "com.lilining.lockescape", emptySet()))
        assertFalse(target.isActionable("com.android.settings", "com.lilining.lockescape", emptySet()))
        assertFalse(target.isActionable("com.lilining.lockescape", "com.lilining.lockescape", emptySet()))
    }

    @Test fun suppressionRejectsChangedExpiredAndWhitelistedTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        assertTrue(target.canActOn("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 101L))
        assertFalse(target.canActOn("com.example.other", "com.lilining.lockescape", emptySet(), nowMs = 101L))
        assertFalse(target.canActOn("com.example.locker", "com.lilining.lockescape", setOf("com.example.locker"), nowMs = 101L))
        assertFalse(target.canActOn("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 120_101L))
    }

    @Test fun inputMethodWindowCannotBecomeActionableEvenIfNotPredefined() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("org.example.keyboard", "com.lilining.lockescape", emptySet(),
            inputMethodPackage = "org.example.keyboard", nowMs = 200L)

        // Keyboard windows must not steal or wipe the rescue target.
        assertEquals("com.example.locker", target.current(nowMs = 201L))
        assertFalse(target.isActionable("org.example.keyboard", "com.lilining.lockescape", emptySet(),
            inputMethodPackage = "org.example.keyboard"))
    }

    @Test fun previouslyCapturedPackageIsRejectedWhenItBecomesActiveKeyboard() {
        val target = RescueTarget()
        target.onWindow("org.example.keyboard", "com.lilining.lockescape", emptySet(), nowMs = 100L)

        assertFalse(target.canActOn("org.example.keyboard", "com.lilining.lockescape", emptySet(),
            inputMethodPackage = "org.example.keyboard", nowMs = 101L))
    }

    @Test fun switchingAwayFromSuspectRevokesTargetEvenIfNewAppIsValidButWhitelisted() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("com.example.trusted", "com.lilining.lockescape", setOf("com.example.trusted"), nowMs = 200L)
        assertEquals("", target.current(nowMs = 201L))
    }

    @Test fun targetAtExpiryBoundaryIsStillValidThenImmediatelyExpires() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        assertEquals("com.example.locker", target.current(nowMs = 120_100L))
        assertEquals("", target.current(nowMs = 120_101L))
    }

    @Test fun futureObservationIsNotValidUntilItsTimestamp() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 1_000L)
        assertEquals("", target.current(nowMs = 999L))
        assertEquals("com.example.locker", target.current(nowMs = 1_000L))
    }

    @Test fun changingToMalformedWindowClearsOldTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("bad package", "com.lilining.lockescape", emptySet(), nowMs = 200L)
        assertEquals("", target.current(nowMs = 201L))
    }

    @Test fun ownWindowDoesNotExtendTargetLifetime() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("com.lilining.lockescape", "com.lilining.lockescape", emptySet(), nowMs = 120_000L)
        assertEquals("", target.current(nowMs = 120_101L))
    }

    @Test fun clearingTargetInvalidatesPendingActions() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.clear()
        assertFalse(target.canActOn("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 101L))
    }

    @Test fun inputMethodChangingAfterCaptureInvalidatesTarget() {
        val target = RescueTarget()
        target.onWindow("org.example.keyboard", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        assertFalse(target.canActOn("org.example.keyboard", "com.lilining.lockescape", emptySet(),
            inputMethodPackage = "org.example.keyboard", nowMs = 200L))
    }

    @Test fun switchingToKnownLauncherRevokesEarlierTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("com.android.launcher3", "com.lilining.lockescape", emptySet(), nowMs = 200L)
        assertEquals("", target.current(nowMs = 201L))
    }

    @Test fun sameTargetObservationRefreshesItsLifetimeOnlyWhenActuallyForeground() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 120_000L)
        assertEquals("com.example.locker", target.current(nowMs = 120_101L))
    }

    @Test fun validTargetCannotBeActedOnIfItWasNeverObserved() {
        val target = RescueTarget()
        assertFalse(target.canActOn("com.example.locker", "com.lilining.lockescape", emptySet(), nowMs = 100L))
    }

    @Test fun protectedSystemWindowCannotBecomeANewTarget() {
        val target = RescueTarget()
        target.onWindow("com.android.systemui", "com.lilining.lockescape", emptySet(), nowMs = 100L)
        assertEquals("", target.current(nowMs = 101L))
    }
}
