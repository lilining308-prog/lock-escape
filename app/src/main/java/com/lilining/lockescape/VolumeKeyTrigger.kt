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

class VolumeKeyTrigger {
    private var lastKeyTime = 0L
    private var consecutiveCount = 0
    private var volumeDownPressed = false

    fun onVolumeDown(keyCode: Int, now: Long, isRepeat: Boolean = false): Decision {
        if (isRepeat) {
            return Decision(scheduleLongPress = false, cancelLongPress = false, triggerEscape = false)
        }

        val scheduleLongPress = keyCode == KEY_VOLUME_DOWN && !volumeDownPressed
        if (keyCode == KEY_VOLUME_DOWN) {
            volumeDownPressed = true
        }

        if (now - lastKeyTime > TRIGGER_WINDOW_MS) {
            consecutiveCount = 0
        }
        lastKeyTime = now
        consecutiveCount++

        val triggerEscape = consecutiveCount >= TRIGGER_COUNT
        if (triggerEscape) {
            consecutiveCount = 0
        }

        return Decision(
            scheduleLongPress = scheduleLongPress,
            cancelLongPress = false,
            triggerEscape = triggerEscape
        )
    }

    fun onVolumeUp(keyCode: Int): Decision {
        val cancelLongPress = keyCode == KEY_VOLUME_DOWN && volumeDownPressed
        if (keyCode == KEY_VOLUME_DOWN) {
            volumeDownPressed = false
        }
        return Decision(
            scheduleLongPress = false,
            cancelLongPress = cancelLongPress,
            triggerEscape = false
        )
    }

    fun resetAfterTrigger() {
        consecutiveCount = 0
        volumeDownPressed = false
    }

    data class Decision(
        val scheduleLongPress: Boolean,
        val cancelLongPress: Boolean,
        val triggerEscape: Boolean
    )

    companion object {
        const val KEY_VOLUME_UP = 24
        const val KEY_VOLUME_DOWN = 25
        const val TRIGGER_COUNT = 4
        const val TRIGGER_WINDOW_MS = 1500L
        const val LONG_PRESS_MS = 1500L
    }
}
