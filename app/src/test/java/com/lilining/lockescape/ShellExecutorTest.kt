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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
    fun autoModeDoesNotRepeatFailedCommandWithRoot() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest(
            command = "id",
            mode = ShellExecutor.Mode.AUTO,
            shizukuAvailable = true,
            runShizuku = { calls.add("shizuku"); false to "binder unavailable" },
            runSu = { calls.add("su"); true to "uid=0(root)" }
        )

        assertEquals(false to "binder unavailable", result)
        assertEquals(listOf("shizuku"), calls)
    }

    @Test
    fun shizukuCreationFailureCanFallbackButWaitFailureCannotRepeatCommand() {
        var fallbackCount = 0
        val fallback: () -> Pair<Boolean, String> = {
            fallbackCount++
            true to "root"
        }
        val creationFailure = ShellExecutor.runShizukuProcessForTest(
            create = { throw IllegalStateException("binder unavailable") },
            await = { true to "completed" },
            fallback = fallback
        )
        assertEquals(true to "root", creationFailure)

        val process = completedProcess(0, "", "")
        val waitFailure = ShellExecutor.runShizukuProcessForTest(
            create = { process },
            await = { throw IllegalStateException("response lost") },
            fallback = fallback
        )
        assertFalse(waitFailure.first)
        assertTrue(waitFailure.second.contains("response lost"))
        assertEquals(1, fallbackCount)
    }

    @Test
    fun autoModeDoesNotUseShizukuWithoutPermission() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest(
            command = "id",
            mode = ShellExecutor.Mode.AUTO,
            shizukuAvailable = true,
            shizukuAuthorized = false,
            runShizuku = { calls.add("shizuku"); true to "shizuku" },
            runSu = { calls.add("su"); true to "root" }
        )

        assertEquals(true to "root", result)
        assertEquals(listOf("su"), calls)
    }

    @Test
    fun protectedPackagesCannotBeDisabledOrUninstalledEvenThroughExecutor() {
        assertFalse(ShellExecutor.disableForTest("com.android.systemui").first)
        assertFalse(ShellExecutor.uninstallForTest("com.miui.home").first)
        assertFalse(ShellExecutor.disableForTest("com.lilining.lockescape").first)
        assertFalse(ShellExecutor.uninstallForTest("com.lilining.lockescape").first)
    }

    @Test
    fun executorRejectsDeviceSpecificHomeAndInputMethodBeforeCommandExecution() {
        val policy = ProtectedTargets("com.lilining.lockescape", "org.example.customhome", "org.example.keyboard")
        assertFalse(ShellExecutor.forceStopForTest("org.example.customhome", policy).first)
        assertFalse(ShellExecutor.disableForTest("org.example.keyboard", policy).first)
        assertFalse(ShellExecutor.uninstallForTest("org.example.customhome", policy).first)
        assertTrue(ShellExecutor.forceStopForTest("com.example.locker", policy).first)
    }

    @Test
    fun commandTimeoutTerminatesProcess() {
        val process = if (System.getProperty("os.name")?.startsWith("Windows") == true) {
            ProcessBuilder("cmd", "/c", "ping -n 8 127.0.0.1 > nul").start()
        } else {
            ProcessBuilder("sh", "-c", "sleep 8").start()
        }
        val result = ShellExecutor.waitForProcess(process, 100L)

        assertFalse(result.first)
        assertTrue(result.second?.contains("超时") == true)
        assertFalse(process.isAlive)
    }

    @Test
    fun processWithBrokenTimedWaitStillReturnsItsActualResult() {
        val actual = ProcessBuilder("java", "-version").start()
        val process = object : Process() {
            override fun getOutputStream() = actual.outputStream
            override fun getInputStream() = actual.inputStream
            override fun getErrorStream() = actual.errorStream
            override fun waitFor() = actual.waitFor()
            override fun exitValue() = actual.exitValue()
            override fun destroy() = actual.destroy()
            override fun waitFor(timeout: Long, unit: java.util.concurrent.TimeUnit): Boolean =
                throw IllegalArgumentException("process hasn't exited")
        }

        val result = ShellExecutor.waitForProcess(process, 5_000L)

        assertTrue(result.first)
        assertTrue(result.second.isNotBlank())
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
        assertFalse(ShellExecutor.forceStopForTest(" com.example.safe ").first)
        assertFalse(ShellExecutor.uninstallForTest("com.example.safe\n").first)
    }

    @Test
    fun shellCommandsUseValidatedPackageNameOnly() {
        assertEquals("am force-stop com.example.safe", ShellExecutor.forceStopCommandForTest("com.example.safe"))
        assertEquals("pm disable-user --user 0 com.example.safe", ShellExecutor.disableCommandForTest("com.example.safe"))
        assertEquals("pm uninstall --user 0 com.example.safe", ShellExecutor.uninstallCommandForTest("com.example.safe"))
    }

    @Test
    fun forceStopNeverTargetsTheRescueAppOrCriticalSystemPackages() {
        assertFalse(ShellExecutor.forceStopForTest("com.lilining.lockescape").first)
        assertFalse(ShellExecutor.forceStopForTest("com.android.systemui").first)
        assertFalse(ShellExecutor.forceStopForTest("com.android.settings").first)
        assertFalse(ShellExecutor.forceStopForTest("com.miui.home").first)
        assertTrue(ShellExecutor.forceStopForTest("com.example.locker").first)
    }

    @Test
    fun zeroExitWithPackageManagerFailureIsNotReportedAsSuccess() {
        val process = ProcessBuilder("java", "-version").start()
        assertTrue(ShellExecutor.waitForProcess(process, 5_000L).first)
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Failure [not installed]"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Error: package not found"))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "Success"))
        assertFalse(ShellExecutor.isCommandSuccessful(1, "Success"))
    }

    @Test
    fun stderrFailureCannotBeHiddenBySuccessfulStdout() {
        val process = completedProcess(0, "checking package\n", "Failure [package not installed]\n")

        val result = ShellExecutor.waitForProcess(process)

        assertFalse(result.first)
        assertTrue(result.second.contains("Failure"))
    }

    @Test
    fun stderrWarningDoesNotTurnSuccessfulCommandIntoFailure() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "Success\n", "Warning: retry\n"))

        assertTrue(result.first)
    }

    @Test
    fun diagnosticLineBeforePackageManagerFailureDoesNotMaskFailure() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "checking package\nFailure [not installed]"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "checking package\nError: package missing"))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "checking package\nSuccess"))
    }

    @Test
    fun leadingWhitespaceAndMixedCaseStillSignalFailure() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "\n  failure [DELETE_FAILED_INTERNAL_ERROR]"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "  ERROR: permission denied"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Security exception: not allowed"))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "No errors found"))
    }

    @Test
    fun zeroExitWithoutOutputCannotConfirmDestructiveAction() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "", "pm uninstall --user 0 com.example.locker"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "unexpected response", "pm uninstall --user 0 com.example.locker"))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "Success", "pm uninstall --user 0 com.example.locker"))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "", "am force-stop com.example.locker"))
    }

    @Test
    fun uninstallProcessWithZeroExitAndEmptyOutputIsNotReportedSuccessful() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", ""), command = "pm uninstall --user 0 com.example.locker"
        )

        assertFalse(result.first)
        assertTrue(result.second.contains("无法确认"))
    }

    @Test
    fun failingStderrIsDetectedEvenIfStdoutFillsItsOutputLimit() {
        val longOutput = "a".repeat(4_096)
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, longOutput, "Failure [not installed]"),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("Failure"))
    }

    @Test
    fun truncatedOutputCannotConfirmAnUninstall() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success" + "x".repeat(4_096), ""),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("无法确认"))
    }

    @Test
    fun streamCleanupFailureDoesNotTurnFinishedCommandIntoAnException() {
        val process = object : Process() {
            override fun getOutputStream() = object : ByteArrayOutputStream() {
                override fun close() { throw java.io.IOException("stream already closed") }
            }
            override fun getInputStream() = ByteArrayInputStream("Success".toByteArray())
            override fun getErrorStream() = ByteArrayInputStream(ByteArray(0))
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() = Unit
        }

        assertEquals(true to "Success", ShellExecutor.waitForProcess(process))
    }

    @Test
    fun suSyntaxFallbackMustNotRerunACommandThatWasAlreadySent() {
        var calls = 0
        val result = ShellExecutor.runSuForTest("pm uninstall --user 0 com.example.locker") {
            calls++
            false to "command started; invalid uid/gid while processing target"
        }
        assertFalse(result.first)
        assertEquals(1, calls)
    }

    @Test
    fun disableRequiresPackageManagerConfirmationNotJustExitCode() {
        val cmd = "pm disable-user --user 0 com.example.locker"
        assertFalse(ShellExecutor.isCommandSuccessful(0, "", cmd))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Unexpected output", cmd))
        assertTrue(ShellExecutor.isCommandSuccessful(0, "Package com.example.locker new state: disabled-user", cmd))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Package com.other.app new state: disabled-user", cmd))
    }

    @Test
    fun suSyntaxFallbackRequiresACommandThatHasNotStarted() {
        val calls = mutableListOf<List<String>>()
        ShellExecutor.runSuForTest("pm uninstall --user 0 com.example.locker") { args ->
            calls += args.toList()
            false to "su: invalid uid/gid '-c'\nFailure [DELETE_FAILED_INTERNAL_ERROR]"
        }
        assertEquals(1, calls.size)
    }

    @Test
    fun processTimeoutStillReturnsAnErrorIfDestroyThrows() {
        val actual = if (System.getProperty("os.name")?.startsWith("Windows") == true) {
            ProcessBuilder("cmd", "/c", "ping -n 8 127.0.0.1 > nul").start()
        } else {
            ProcessBuilder("sh", "-c", "sleep 8").start()
        }
        val remote = object : Process() {
            override fun getOutputStream() = actual.outputStream
            override fun getInputStream() = actual.inputStream
            override fun getErrorStream() = actual.errorStream
            override fun waitFor() = actual.waitFor()
            override fun exitValue() = actual.exitValue()
            override fun destroy() = actual.destroy()
            override fun destroyForcibly(): Process {
                actual.destroyForcibly()
                throw java.io.IOException("remote process already exited")
            }
        }

        val result = ShellExecutor.waitForProcess(remote, 100L)
        assertFalse(result.first)
        assertTrue(result.second.contains("超时"))
    }

    @Test
    fun interruptionDoesNotLeaveAnUntrackedProcessRunning() {
        val actual = if (System.getProperty("os.name")?.startsWith("Windows") == true) {
            ProcessBuilder("cmd", "/c", "ping -n 8 127.0.0.1 > nul").start()
        } else {
            ProcessBuilder("sh", "-c", "sleep 8").start()
        }
        val remote = object : Process() {
            override fun getOutputStream() = actual.outputStream
            override fun getInputStream() = actual.inputStream
            override fun getErrorStream() = actual.errorStream
            override fun waitFor(): Int { throw InterruptedException("remote wait interrupted") }
            override fun exitValue() = actual.exitValue()
            override fun destroy() = actual.destroy()
            override fun destroyForcibly(): Process = actual.destroyForcibly()
        }

        try {
            val result = ShellExecutor.waitForProcess(remote, 1_000L)
            assertFalse(result.first)
            assertFalse(actual.isAlive)
        } finally {
            actual.destroyForcibly()
        }
    }

    @Test
    fun truncatedUninstallOutputCannotReportSuccessEvenWithSuccessPrefix() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success\n" + "noise".repeat(1_000), ""),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun truncatedCommandOutputCannotHideLateFailure() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "normal".repeat(1_000) + "\nFailure [late error]", ""),
            command = "am force-stop com.example.locker"
        )

        assertFalse(result.first)
        assertTrue(result.second.contains("截断"))
    }

    @Test
    fun completedProcessDoesNotReturnBeforeSlowErrorStreamIsRead() {
        val process = object : Process() {
            override fun getOutputStream() = ByteArrayOutputStream()
            override fun getInputStream() = ByteArrayInputStream("checking\n".toByteArray())
            override fun getErrorStream() = object : ByteArrayInputStream("Failure [late]\n".toByteArray()) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    Thread.sleep(1_200L)
                    return super.read(b, off, len)
                }
                override fun read(): Int {
                    Thread.sleep(1_200L)
                    return super.read()
                }
            }
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() = Unit
        }

        assertFalse(ShellExecutor.waitForProcess(process, 4_000L).first)
    }

    @Test
    fun completedProcessWithStalledStreamReportsUncertainResult() {
        val process = object : Process() {
            override fun getOutputStream() = ByteArrayOutputStream()
            override fun getInputStream() = ByteArrayInputStream("Success".toByteArray())
            override fun getErrorStream() = object : ByteArrayInputStream(ByteArray(0)) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    Thread.sleep(4_000L)
                    return super.read(b, off, len)
                }
            }
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() = Unit
        }

        val result = ShellExecutor.waitForProcess(process, 5_000L)
        assertFalse(result.first)
        assertTrue(result.second.contains("读取超时"))
    }

    @Test
    fun shizukuCreationFailurePropagatesRootFailure() {
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { throw IllegalStateException("no binder") },
            await = { true to "unreachable" },
            fallback = { false to "root 不可用" }
        )
        assertEquals(false to "root 不可用", result)
    }

    @Test
    fun failedCommandWithOnlyStderrPreservesItsReason() {
        val result = ShellExecutor.waitForProcess(completedProcess(1, "", "permission denied"))
        assertEquals(false to "permission denied", result)
    }

    @Test
    fun commandWithZeroExitAndSecurityExceptionIsNotSuccessful() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", "Security exception: permission denied"),
            command = "am force-stop com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("permission denied"))
    }

    @Test
    fun pmConfirmationRequiresMatchingTargetPackage() {
        val command = "pm disable-user --user 0 com.example.locker"
        val output = "Package com.example.locker.evil new state: disabled-user"
        assertFalse(ShellExecutor.isCommandSuccessful(0, output, command))
    }

    @Test
    fun processExitFailureKeepsCodeWhenNoOutputExists() {
        assertEquals(false to "命令退出码 127", ShellExecutor.waitForProcess(completedProcess(127, "", "")))
    }

    @Test
    fun suFallbackDoesNotRunWhenRootCommandExplicitlyFailed() {
        var attempts = 0
        val result = ShellExecutor.runSuForTest("id") {
            attempts++
            false to "su: permission denied"
        }
        assertEquals(false to "su: permission denied", result)
        assertEquals(1, attempts)
    }

    @Test
    fun confirmedDisableMayIncludeOnlyWhitespaceAroundResponse() {
        val command = "pm disable-user --user 0 com.example.locker"
        assertTrue(ShellExecutor.isCommandSuccessful(0, "\nPackage com.example.locker new state: disabled-user\n", command))
    }

    @Test
    fun commandWithStdoutSuccessAndStderrFailureNeverReportsSuccess() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success", "Error: permission denied"),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun shizukuWaitFailureDoesNotRetryEvenIfExceptionLooksLikePermissionError() {
        var fallbackCalled = false
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { completedProcess(0, "", "") },
            await = { throw SecurityException("permission revoked after start") },
            fallback = { fallbackCalled = true; true to "root" }
        )
        assertFalse(result.first)
        assertFalse(fallbackCalled)
    }

    @Test
    fun shizukuProcessCreationFallbackNeverCallsWait() {
        var waited = false
        ShellExecutor.runShizukuProcessForTest(
            create = { throw IllegalArgumentException("cannot create") },
            await = { waited = true; true to "unexpected" },
            fallback = { false to "no root" }
        )
        assertFalse(waited)
    }

    @Test
    fun zeroExitPackageManagerResponseForOtherCommandCannotConfirmUninstall() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Package com.example.locker new state: disabled-user", ""),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun documentedForceStopCommandStillSucceedsOnSilentZeroExit() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", ""), command = "am force-stop com.example.locker"
        )
        assertEquals(true to "命令已完成", result)
    }

    @Test
    fun failedStreamReadNeverReportsSuccessFromIncompleteOutput() {
        val process = object : Process() {
            override fun getOutputStream() = ByteArrayOutputStream()
            override fun getInputStream() = ByteArrayInputStream("Success".toByteArray())
            override fun getErrorStream() = object : ByteArrayInputStream(ByteArray(0)) {
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    throw java.io.IOException("remote stream disconnected")
                }
            }
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() = Unit
        }
        val result = ShellExecutor.waitForProcess(process, command = "pm uninstall --user 0 com.example.locker")
        assertFalse(result.first)
        assertTrue(result.second.contains("读取失败"))
    }

    @Test
    fun commandTimeoutDoesNotReportSuccessEvenIfOutputSaysSuccess() {
        val process = if (System.getProperty("os.name")?.startsWith("Windows") == true) {
            ProcessBuilder("cmd", "/c", "echo Success & ping -n 8 127.0.0.1 > nul").start()
        } else {
            ProcessBuilder("sh", "-c", "echo Success; sleep 8").start()
        }
        val result = ShellExecutor.waitForProcess(process, 150L, "pm uninstall --user 0 com.example.locker")
        assertFalse(result.first)
        assertTrue(result.second.contains("超时"))
    }

    @Test
    fun processWithExactOutputLimitRemainsConfirmable() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "x".repeat(4_096), ""))
        assertTrue(result.first)
    }

    @Test
    fun aQuietProcessCannotPretendToHaveUninstalledAnApplication() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", ""), command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("没有输出"))
    }

    @Test
    fun stderrWarningsDoNotOverrideValidUninstallConfirmation() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success", "Warning: device busy"),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertTrue(result.first)
    }

    @Test
    fun errorLineAfterSuccessIsNeverAccepted() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Success\nError: failed"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Success\nFailure [failed]"))
    }

    @Test
    fun successIsNotAcceptedWhenTheStreamStartsWithAHiddenFailureLine() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "  Failure [permission denied]\nSuccess"))
    }

    @Test
    fun unexpectedAndroidPackageManagerFailureCaseIsRejected() {
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Failure [DELETE_FAILED_DEVICE_POLICY_MANAGER]", "pm uninstall --user 0 com.example.locker"))
        assertFalse(ShellExecutor.isCommandSuccessful(0, "Error: unknown package", "pm disable-user --user 0 com.example.locker"))
    }

    @Test
    fun shellCommandWithTruncatedStderrIsNeverReportedSuccessful() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", "warning".repeat(1_000)), command = "am force-stop com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("截断"))
    }

    @Test
    fun suUidSyntaxFallbackOnlyHappensForExactUnsupportedSyntaxMessage() {
        var calls = 0
        ShellExecutor.runSuForTest("id") {
            calls++
            false to "su: invalid uid/gid '-c' followed by unexpected output"
        }
        assertEquals(1, calls)
    }

    @Test
    fun disableOutputCannotBeConfirmedWhenStderrIsUnavailable() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Package com.example.locker new state: disabled-user", "Failure [permission]"),
            command = "pm disable-user --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun repeatedShizukuFailureDoesNotInvokeRootWhenProcessWasCreated() {
        var calls = 0
        repeat(2) {
            val result = ShellExecutor.runShizukuProcessForTest(
                create = { completedProcess(1, "", "") },
                await = { false to "binder error" },
                fallback = { calls++; true to "root" }
            )
            assertFalse(result.first)
        }
        assertEquals(0, calls)
    }

    @Test
    fun reportedUninstallFailureIncludesPackageManagerReason() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Failure [DELETE_FAILED_DEVICE_POLICY_MANAGER]", ""),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("DELETE_FAILED_DEVICE_POLICY_MANAGER"))
    }

    @Test
    fun successfulSuFallbackUsesOnlyOneCommandPerSyntax() {
        val commands = mutableListOf<List<String>>()
        val result = ShellExecutor.runSuForTest("id") { args ->
            commands += args.toList()
            if (commands.size == 1) false to "su: invalid uid/gid '-c'" else true to "uid=0(root)"
        }
        assertTrue(result.first)
        assertEquals(2, commands.size)
    }

    @Test
    fun zeroExitUninstallWithSuccessOnlyInStderrNeedsConfirmationOnStdout() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", "Success"), command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun outputReadFailureOverridesZeroExitAndSuccessPrefix() {
        val stream = object : ByteArrayInputStream("Success".toByteArray()) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (available() == 0) throw java.io.IOException("stream gone")
                return super.read(b, off, len)
            }
        }
        val process = object : Process() {
            override fun getOutputStream() = ByteArrayOutputStream()
            override fun getInputStream() = stream
            override fun getErrorStream() = ByteArrayInputStream(ByteArray(0))
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() = Unit
        }
        val result = ShellExecutor.waitForProcess(process, command = "pm uninstall --user 0 com.example.locker")
        assertFalse(result.first)
    }

    @Test
    fun commandFailureDoesNotBecomeSuccessBecauseOfAWarningOnOtherStream() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(1, "Success", "Warning: retry"), command = "pm uninstall --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun aTimeoutPreservesTheNoRetryBoundaryAfterShizukuProcessCreation() {
        var retries = 0
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { completedProcess(0, "", "") },
            await = { false to "命令超时" },
            fallback = { retries++; true to "root" }
        )
        assertFalse(result.first)
        assertEquals(0, retries)
    }

    @Test
    fun outputWithNoFailureMarkerAndNormalExitIsNotAccidentallyRejected() {
        assertTrue(ShellExecutor.isCommandSuccessful(0, "Process completed", "am force-stop com.example.locker"))
        assertFalse(ShellExecutor.isCommandSuccessful(2, "Process completed", "am force-stop com.example.locker"))
    }

    @Test
    fun commandConfirmationIsIndependentOfUserVisibleMessageFormatting() {
        val output = "Package com.example.locker new state: disabled-user"
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, output, ""), command = "pm disable-user --user 0 com.example.locker"
        )
        assertEquals(true to output, result)
    }

    @Test
    fun noRootAndNoShizukuReturnsRootFailureWithoutPretendingToSucceed() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest("id", ShellExecutor.Mode.AUTO, false,
            runShizuku = { calls += "shizuku"; true to "wrong" },
            runSu = { calls += "su"; false to "root 不可用" }
        )
        assertEquals(false to "root 不可用", result)
        assertEquals(listOf("su"), calls)
    }

    @Test
    fun shizukuAuthorizedFalseDoesNotAttemptBinderExecution() {
        val calls = mutableListOf<String>()
        ShellExecutor.runForTest("id", ShellExecutor.Mode.AUTO, true, false,
            runShizuku = { calls += "shizuku"; true to "unavailable" },
            runSu = { calls += "su"; false to "root denied" }
        )
        assertEquals(listOf("su"), calls)
    }

    @Test
    fun shizukuAvailableButFailingDoesNotRetryWithRoot() {
        val calls = mutableListOf<String>()
        val result = ShellExecutor.runForTest("pm disable-user --user 0 com.example.locker",
            ShellExecutor.Mode.AUTO, true, true,
            runShizuku = { calls += "shizuku"; false to "Failure" },
            runSu = { calls += "root"; true to "done" })
        assertFalse(result.first)
        assertEquals(listOf("shizuku"), calls)
    }

    @Test
    fun emptyCommandOutputWithExitZeroIsNotEnoughForDisable() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", ""), command = "pm disable-user --user 0 com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun commandCleanupFailureDoesNotHideARealError() {
        val process = object : Process() {
            override fun getOutputStream() = object : ByteArrayOutputStream() {
                override fun close() { throw java.io.IOException("cleanup failed") }
            }
            override fun getInputStream() = ByteArrayInputStream(ByteArray(0))
            override fun getErrorStream() = ByteArrayInputStream("Failure [not installed]".toByteArray())
            override fun waitFor() = 1
            override fun exitValue() = 1
            override fun destroy() = Unit
        }
        val result = ShellExecutor.waitForProcess(process)
        assertFalse(result.first)
        assertTrue(result.second.contains("Failure"))
    }

    @Test
    fun malformedPackageCannotBecomeACommandEvenWithPolicyPresent() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertFalse(ShellExecutor.disableForTest("com.example.bad;reboot", policy).first)
        assertFalse(ShellExecutor.uninstallForTest("com.example.bad\n", policy).first)
    }

    @Test
    fun allThreeOperationsRejectTheCurrentDeviceHome() {
        val policy = ProtectedTargets("com.lilining.lockescape", "org.example.home", null)
        assertFalse(ShellExecutor.forceStopForTest("org.example.home", policy).first)
        assertFalse(ShellExecutor.disableForTest("org.example.home", policy).first)
        assertFalse(ShellExecutor.uninstallForTest("org.example.home", policy).first)
    }

    @Test
    fun allThreeOperationsRejectTheCurrentInputMethod() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, "org.example.keyboard")
        assertFalse(ShellExecutor.forceStopForTest("org.example.keyboard", policy).first)
        assertFalse(ShellExecutor.disableForTest("org.example.keyboard", policy).first)
        assertFalse(ShellExecutor.uninstallForTest("org.example.keyboard", policy).first)
    }

    @Test
    fun aRootOnlyRequestDoesNotInspectShizukuResult() {
        val result = ShellExecutor.runForTest("id", ShellExecutor.Mode.ROOT, true, true,
            runShizuku = { throw IllegalStateException("not used") },
            runSu = { true to "root" }
        )
        assertEquals(true to "root", result)
    }

    @Test
    fun existingProtectedPackagesStillCannotBeUninstalled() {
        assertFalse(ShellExecutor.uninstallForTest("com.android.systemui").first)
        assertFalse(ShellExecutor.uninstallForTest("com.google.android.gms").first)
    }

    @Test
    fun resultOfUninstallWithExactSuccessAndNoErrorsIsSuccess() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success\n", ""), command = "pm uninstall --user 0 com.example.locker"
        )
        assertTrue(result.first)
    }

    @Test
    fun rootSyntaxFallbackDoesNotTreatExitCodeZeroAsFailure() {
        var calls = 0
        val result = ShellExecutor.runSuForTest("id") {
            calls++
            true to "su: invalid uid/gid '-c'"
        }
        assertTrue(result.first)
        assertEquals(1, calls)
    }

    @Test
    fun shizukuCreationFailureUsesSuOnlyOnce() {
        var calls = 0
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { throw IllegalStateException("binder disconnected") },
            await = { true to "not reached" },
            fallback = { calls++; true to "root" }
        )
        assertTrue(result.first)
        assertEquals(1, calls)
    }

    @Test
    fun aQuietPackageManagerFailureIncludesItsExitCode() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(3, "", ""), command = "pm disable-user --user 0 com.example.locker"
        )
        assertFalse(result.first)
        assertTrue(result.second.contains("退出码 3"))
    }

    @Test
    fun stderrWarningIsIncludedForDiagnosticsWithoutChangingSuccess() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "Success", "Warning: nonfatal"),
            command = "pm uninstall --user 0 com.example.locker"
        )
        assertTrue(result.first)
        assertTrue(result.second.contains("Warning: nonfatal"))
    }

    @Test
    fun commandLineIsBuiltOnlyFromAcceptedPackageCharacters() {
        assertEquals("pm disable-user --user 0 com.example.safe_2", ShellExecutor.disableCommandForTest("com.example.safe_2"))
        assertEquals(null, ShellExecutor.disableCommandForTest("com.example.safe_2; id"))
    }

    @Test
    fun allShellOperationsAreUnavailableForTheRescuePackage() {
        val pkg = "com.lilining.lockescape"
        assertFalse(ShellExecutor.forceStopForTest(pkg).first)
        assertFalse(ShellExecutor.disableForTest(pkg).first)
        assertFalse(ShellExecutor.uninstallForTest(pkg).first)
    }

    @Test
    fun resultWithOutputLimitedToMaximumCharactersIsNotTruncated() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "a".repeat(4_096), ""))
        assertTrue(result.first)
    }

    @Test
    fun bothStreamsAreCollectedBeforePresentingFailureReason() {
        val result = ShellExecutor.waitForProcess(completedProcess(1, "started", "Failure [permission]"))
        assertFalse(result.first)
        assertTrue(result.second.contains("started"))
        assertTrue(result.second.contains("Failure"))
    }

    @Test
    fun malformedInputCannotBeUsedForUninstallEvenWithoutPolicy() {
        assertFalse(ShellExecutor.uninstallForTest("com.example.locker && reboot").first)
    }

    @Test
    fun processExitZeroCanBeRejectedByStderrEvenWithForceStop() {
        val result = ShellExecutor.waitForProcess(
            completedProcess(0, "", "Error: unknown package"), command = "am force-stop com.example.locker"
        )
        assertFalse(result.first)
    }

    @Test
    fun shellCommandRejectsUnknownExitFailureEvenIfSuccessWordIsPresent() {
        val result = ShellExecutor.waitForProcess(completedProcess(9, "Success", ""))
        assertFalse(result.first)
    }

    @Test
    fun stdoutFailureCannotBeHiddenByStderrWarning() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "Failure [not installed]", "Warning: retry"))
        assertFalse(result.first)
    }

    @Test
    fun processWithNoOutputAndNormalExitMayForceStopButCannotUninstall() {
        val forceStop = ShellExecutor.waitForProcess(completedProcess(0, "", ""), command = "am force-stop com.example.locker")
        val uninstall = ShellExecutor.waitForProcess(completedProcess(0, "", ""), command = "pm uninstall --user 0 com.example.locker")
        assertTrue(forceStop.first)
        assertFalse(uninstall.first)
    }

    @Test
    fun noOutputForDisableIsMarkedUnconfirmedNotCompleted() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "", ""),
            command = "pm disable-user --user 0 com.example.locker")
        assertFalse(result.first)
        assertTrue(result.second.contains("无法确认"))
    }

    @Test
    fun knownShizukuPermissionFailureDoesNotRetryACommandAfterStart() {
        var rootCalls = 0
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { completedProcess(0, "", "") },
            await = { false to "permission denied" },
            fallback = { rootCalls++; true to "root" }
        )
        assertFalse(result.first)
        assertEquals(0, rootCalls)
    }

    @Test
    fun malformedProtectedTargetDoesNotReachCommandConstruction() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertEquals(null, ShellExecutor.forceStopCommandForTest("com.example.safe && reboot"))
        assertFalse(ShellExecutor.forceStopForTest("com.example.safe && reboot", policy).first)
    }

    @Test
    fun processWithZeroExitAndNoOutputDoesNotClaimDisableSuccess() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "", ""),
            command = "pm disable-user --user 0 com.example.locker")
        assertFalse(result.first)
    }

    @Test
    fun stderrSecurityErrorIsCheckedEvenAfterStdoutSuccess() {
        val result = ShellExecutor.waitForProcess(completedProcess(0, "Success", "Security exception: denied"),
            command = "pm uninstall --user 0 com.example.locker")
        assertFalse(result.first)
    }

    @Test
    fun shizukuCreationExceptionDoesNotDuplicateAnUninstallAttempt() {
        var attempts = 0
        val result = ShellExecutor.runShizukuProcessForTest(
            create = { throw NoSuchMethodException("newProcess unavailable") },
            await = { true to "not started" },
            fallback = { attempts++; false to "root unavailable" }
        )
        assertEquals(false to "root unavailable", result)
        assertEquals(1, attempts)
    }

    @Test
    fun emptyInputNeverCreatesForceStopCommand() {
        assertFalse(ShellExecutor.forceStopForTest("").first)
        assertEquals(null, ShellExecutor.forceStopCommandForTest(""))
    }

    @Test
    fun forceStopCommandCannotTargetItsOwnApplication() {
        assertEquals(null, ShellExecutor.forceStopCommandForTest("com.lilining.lockescape"))
    }

    @Test
    fun userDefinedWhitelistIsNotTheShellExecutorsResponsibility() {
        val policy = ProtectedTargets("com.lilining.lockescape", null, null)
        assertTrue(ShellExecutor.forceStopForTest("com.example.locker", policy).first)
        assertFalse(policy.canActOn("com.example.locker", setOf("com.example.locker")))
    }

    private fun completedProcess(exit: Int, stdout: String, stderr: String): Process = object : Process() {
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun getInputStream() = ByteArrayInputStream(stdout.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(stderr.toByteArray())
        override fun waitFor() = exit
        override fun exitValue() = exit
        override fun destroy() = Unit
    }
}
