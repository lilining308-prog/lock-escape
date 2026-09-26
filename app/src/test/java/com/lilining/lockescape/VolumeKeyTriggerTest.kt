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

class VolumeKeyTriggerTest {
    @Test
    fun holdingVolumeDownSchedulesLongPressOnlyOnce() {
        val trigger = VolumeKeyTrigger()

        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L).scheduleLongPress)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_100L, isRepeat = true).scheduleLongPress)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_200L, isRepeat = true).scheduleLongPress)
    }

    @Test
    fun repeatedVolumeDownEventsDoNotCountAsTapTrigger() {
        val trigger = VolumeKeyTrigger()

        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_100L, isRepeat = true).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_200L, isRepeat = true).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_300L, isRepeat = true).triggerEscape)
    }

    @Test
    fun releasingVolumeDownCancelsLongPress() {
        val trigger = VolumeKeyTrigger()

        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L)

        assertTrue(trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN).cancelLongPress)
    }

    @Test
    fun fourVolumePressesStillTriggersEscape() {
        val trigger = VolumeKeyTrigger()

        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L).triggerEscape)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_200L).triggerEscape)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_400L).triggerEscape)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN)
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_600L).triggerEscape)
    }

    @Test
    fun repeatedDownWithoutReleaseDoesNotCountAsMultipleTaps() {
        val trigger = VolumeKeyTrigger()

        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_200L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_400L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_600L).triggerEscape)
    }

    @Test
    fun slowPressesDoNotTriggerEscape() {
        val trigger = VolumeKeyTrigger()

        repeat(4) { index ->
            assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L + index * 2_000L).triggerEscape)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
    }
}
