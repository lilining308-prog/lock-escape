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

    @Test fun switchingToKeyboardOrSystemWindowRevokesOldTarget() {
        val target = RescueTarget()
        target.onWindow("com.example.locker", "com.lilining.lockescape", emptySet())
        target.onWindow("com.google.android.inputmethod.latin", "com.lilining.lockescape", emptySet(), "com.google.android.inputmethod.latin")
        target.onWindow("com.android.systemui", "com.lilining.lockescape", emptySet())
        assertEquals("", target.current())
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
}
