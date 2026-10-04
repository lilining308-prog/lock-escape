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

    @Test
    fun fourPressesMustFitInOneAndHalfSecondTotal() {
        val trigger = VolumeKeyTrigger()
        repeat(4) { index ->
            assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L + index * 900L).triggerEscape)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
    }

    @Test
    fun resetAfterTriggerDoesNotTreatHeldKeyAsNewPress() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L)
        trigger.resetAfterTrigger()

        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 2_600L).scheduleLongPress)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN)
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 3_000L).scheduleLongPress)
    }

    @Test
    fun clockMovingBackwardsResetsTapWindow() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 900L)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_100L)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)

        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_300L).triggerEscape)
    }

    @Test
    fun unrelatedKeyCannotContributeToEscapeSequence() {
        val trigger = VolumeKeyTrigger()
        assertFalse(trigger.onVolumeDown(42, 1_000L).triggerEscape)
        assertFalse(trigger.onVolumeDown(42, 1_100L).triggerEscape)
        assertFalse(trigger.onVolumeDown(42, 1_200L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_300L).triggerEscape)
    }

    @Test
    fun fourKeyDownsWithNoReleasesAndAlternatingKeysAreNotFourTaps() {
        val trigger = VolumeKeyTrigger()
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_100L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_200L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_300L).triggerEscape)
    }

    @Test
    fun threeTapsThenAKeyboardShortcutCannotTriggerEscape() {
        val trigger = VolumeKeyTrigger()
        repeat(3) { index ->
            assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L + index * 100L).triggerEscape)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        assertFalse(trigger.onVolumeDown(66, 1_300L).triggerEscape)
    }

    @Test
    fun volumeUpReleaseDoesNotCancelVolumeDownLongPress() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L)
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_100L)
        assertFalse(trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP).cancelLongPress)
        assertTrue(trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN).cancelLongPress)
    }

    @Test
    fun startingTimestampAtZeroDoesNotBreakTapCount() {
        val trigger = VolumeKeyTrigger()
        repeat(3) { index ->
            assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, index * 200L).triggerEscape)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 600L).triggerEscape)
    }

    @Test
    fun triggerCanBeArmedAgainAfterHeldKeyIsReleased() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L)
        trigger.resetAfterTrigger()
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN)
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 2_000L).scheduleLongPress)
    }

    @Test
    fun keyRepeatsDoNotRearmALongPressAfterTrigger() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L)
        trigger.resetAfterTrigger()
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_500L, isRepeat = true).scheduleLongPress)
    }

    @Test
    fun secondTapOutsideWindowBeginsFreshSequence() {
        val trigger = VolumeKeyTrigger()
        trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L)
        trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        repeat(3) { index ->
            assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 3_000L + index * 100L).triggerEscape)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 3_300L).triggerEscape)
    }

    @Test
    fun switchingVolumeKeysWithReleasesCountsDistinctTaps() {
        val trigger = VolumeKeyTrigger()
        val keys = listOf(VolumeKeyTrigger.KEY_VOLUME_UP, VolumeKeyTrigger.KEY_VOLUME_DOWN,
            VolumeKeyTrigger.KEY_VOLUME_UP, VolumeKeyTrigger.KEY_VOLUME_DOWN)
        keys.forEachIndexed { index, key ->
            val result = trigger.onVolumeDown(key, 1_000L + index * 100L)
            if (index == 3) assertTrue(result.triggerEscape) else assertFalse(result.triggerEscape)
            trigger.onVolumeUp(key)
        }
    }

    @Test
    fun releasingUnpressedVolumeDownDoesNotCancelLongPress() {
        val trigger = VolumeKeyTrigger()
        assertFalse(trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_DOWN).cancelLongPress)
    }

    @Test
    fun aRepeatedPressCannotCountAsFourthTap() {
        val trigger = VolumeKeyTrigger()
        repeat(3) { index ->
            trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L + index * 100L)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_300L, isRepeat = true).triggerEscape)
    }

    @Test
    fun fourTapsAtTheWindowBoundaryStillTrigger() {
        val trigger = VolumeKeyTrigger()
        listOf(1_000L, 1_300L, 1_700L).forEach { time ->
            trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, time)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 2_500L).triggerEscape)
    }

    @Test
    fun triggerResetsTapCountAfterOpeningEscape() {
        val trigger = VolumeKeyTrigger()
        repeat(4) { index ->
            trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_000L + index * 100L)
            trigger.onVolumeUp(VolumeKeyTrigger.KEY_VOLUME_UP)
        }
        trigger.resetAfterTrigger()
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 2_000L).triggerEscape)
    }

    @Test
    fun longPressSchedulingIsNotDuplicatedByRepeatedDownEvent() {
        val trigger = VolumeKeyTrigger()
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_000L).scheduleLongPress)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_001L).scheduleLongPress)
    }

    @Test
    fun aNonVolumeKeyCannotArmLongPress() {
        val trigger = VolumeKeyTrigger()
        assertFalse(trigger.onVolumeDown(66, 1_000L).scheduleLongPress)
    }
}
