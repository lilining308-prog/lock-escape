/*
 * Copyright (C) 2026 liyan
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.lilining.lockescape

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DaemonServiceTest {
    @Test fun specialUseForegroundTypeOnlyAvailableOnAndroid14AndLater() {
        assertFalse(DaemonService.supportsSpecialUseForegroundType(26))
        assertFalse(DaemonService.supportsSpecialUseForegroundType(29))
        assertFalse(DaemonService.supportsSpecialUseForegroundType(33))
        assertTrue(DaemonService.supportsSpecialUseForegroundType(34))
    }

    @Test fun android14UsesSpecialUseForegroundTypeWhileOlderDevicesUseLegacyCall() {
        assertFalse(DaemonService.supportsSpecialUseForegroundType(27))
        assertFalse(DaemonService.supportsSpecialUseForegroundType(30))
        assertTrue(DaemonService.supportsSpecialUseForegroundType(35))
    }
}
