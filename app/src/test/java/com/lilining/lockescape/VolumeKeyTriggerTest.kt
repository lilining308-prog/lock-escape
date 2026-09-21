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
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_200L).triggerEscape)
        assertFalse(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_DOWN, 1_400L).triggerEscape)
        assertTrue(trigger.onVolumeDown(VolumeKeyTrigger.KEY_VOLUME_UP, 1_600L).triggerEscape)
    }
}
