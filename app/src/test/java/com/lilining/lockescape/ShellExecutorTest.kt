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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellExecutorTest {
    @Test
    fun rootModeUsesSuBeforeShizuku() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest(
            command = "id",
            mode = ShellExecutor.Mode.ROOT,
            shizukuAvailable = true,
            runShizuku = { calls.add("shizuku"); true to "shizuku" },
            runSu = { calls.add("su"); true to "root" }
        )

        assertEquals(true to "root", result)
        assertEquals(listOf("su"), calls)
    }

    @Test
    fun autoModeUsesShizukuBeforeSu() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest(
            command = "id",
            mode = ShellExecutor.Mode.AUTO,
            shizukuAvailable = true,
            runShizuku = { calls.add("shizuku"); true to "shizuku" },
            runSu = { calls.add("su"); true to "root" }
        )

        assertEquals(true to "shizuku", result)
        assertEquals(listOf("shizuku"), calls)
    }

    @Test
    fun suFallbackUsesAndroidEmulatorSyntaxWhenDashCFails() {
        val calls = mutableListOf<List<String>>()
        val result = ShellExecutor.runSuForTest("id") { args: Array<String> ->
            calls.add(args.toList())
            if (args.toList() == listOf("su", "-c", "id")) {
                false to "su: invalid uid/gid '-c'"
            } else {
                true to "uid=0(root)"
            }
        }

        assertEquals(true to "uid=0(root)", result)
        assertEquals(
            listOf(
                listOf("su", "-c", "id"),
                listOf("su", "0", "sh", "-c", "id")
            ),
            calls
        )
    }

    @Test
    fun shellCommandsRejectInvalidPackageNamesBeforeExecution() {
        assertTrue(ShellExecutor.forceStopForTest("com.example.safe").first)
        assertFalse(ShellExecutor.forceStopForTest("com.example.safe; reboot").first)
        assertFalse(ShellExecutor.disableForTest("com.example.safe && reboot").first)
        assertFalse(ShellExecutor.uninstallForTest("../bad").first)
    }

    @Test
    fun shellCommandsUseValidatedPackageNameOnly() {
        assertEquals("am force-stop com.example.safe", ShellExecutor.forceStopCommandForTest("com.example.safe"))
        assertEquals("pm disable-user --user 0 com.example.safe", ShellExecutor.disableCommandForTest("com.example.safe"))
        assertEquals("pm uninstall --user 0 com.example.safe", ShellExecutor.uninstallCommandForTest("com.example.safe"))
    }
}
