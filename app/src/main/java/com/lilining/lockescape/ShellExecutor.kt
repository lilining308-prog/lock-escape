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

import android.content.pm.PackageManager
import android.content.Context
import rikka.shizuku.Shizuku
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.FutureTask
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 逃生命令执行器：
 * 优先走 Shizuku 通道（免 root），否则降级 su（root）。
 * 基础版提供 强停 / 停用 / 卸载 三个动作。
 */
object ShellExecutor {

    enum class Mode {
        AUTO,
        ROOT
    }

    fun forceStop(ctx: Context, pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildForceStopCommand(pkg, ProtectedTargets.from(ctx))?.let { run(it, mode) } ?: invalidPackage(pkg)

    fun disable(ctx: Context, pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildDisableCommand(pkg, ProtectedTargets.from(ctx))?.let { run(it, mode) } ?: unsafePackage(pkg)

    fun uninstall(ctx: Context, pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildUninstallCommand(pkg, ProtectedTargets.from(ctx))?.let { run(it, mode) } ?: unsafePackage(pkg)

    internal fun forceStopForTest(pkg: String, policy: ProtectedTargets? = null): Pair<Boolean, String> =
        buildForceStopCommand(pkg, policy)?.let { true to it } ?: invalidPackage(pkg)

    internal fun disableForTest(pkg: String, policy: ProtectedTargets? = null): Pair<Boolean, String> =
        buildDisableCommand(pkg, policy)?.let { true to it } ?: unsafePackage(pkg)

    internal fun uninstallForTest(pkg: String, policy: ProtectedTargets? = null): Pair<Boolean, String> =
        buildUninstallCommand(pkg, policy)?.let { true to it } ?: unsafePackage(pkg)

    internal fun forceStopCommandForTest(pkg: String): String? = buildForceStopCommand(pkg)

    internal fun disableCommandForTest(pkg: String): String? = buildDisableCommand(pkg)

    internal fun uninstallCommandForTest(pkg: String): String? = buildUninstallCommand(pkg)

    private fun buildForceStopCommand(pkg: String, policy: ProtectedTargets? = null): String? =
        normalizePackage(pkg)?.takeUnless { isCriticalPackage(it) || policy?.canActOn(it, emptySet()) == false }
            ?.let { "am force-stop $it" }

    private fun buildDisableCommand(pkg: String, policy: ProtectedTargets? = null): String? =
        normalizePackage(pkg)?.takeUnless { isCriticalPackage(it) || policy?.canActOn(it, emptySet()) == false }
            ?.let { "pm disable-user --user 0 $it" }

    private fun buildUninstallCommand(pkg: String, policy: ProtectedTargets? = null): String? =
        normalizePackage(pkg)?.takeUnless { isCriticalPackage(it) || policy?.canActOn(it, emptySet()) == false }
            ?.let { "pm uninstall --user 0 $it" }

    private fun normalizePackage(pkg: String): String? =
        pkg.takeIf { AppSafety.isValidPackageName(it) }

    private fun invalidPackage(pkg: String): Pair<Boolean, String> =
        false to "包名不合法，已拒绝执行：$pkg"

    private fun isCriticalPackage(pkg: String): Boolean =
        pkg == "com.lilining.lockescape" || AppSafety.isProtectedPackage(pkg) ||
            AppSafety.isDefaultWhitelistedPackage(pkg)

    private fun unsafePackage(pkg: String): Pair<Boolean, String> =
        if (isCriticalPackage(pkg.trim()))
            false to "系统关键应用或默认桌面已受保护：$pkg"
        else invalidPackage(pkg)

    private fun run(cmd: String, mode: Mode): Pair<Boolean, String> {
        return try {
            val available = mode == Mode.AUTO && Shizuku.pingBinder()
            val authorized = available && (Shizuku.isPreV11() ||
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
            runInternal(cmd, mode, available, authorized, ::runViaShizuku, ::runViaSu)
        } catch (e: Exception) {
            Pair(false, e.message ?: e.javaClass.simpleName)
        }
    }

    internal fun runForTest(
        command: String,
        mode: Mode,
        shizukuAvailable: Boolean,
        shizukuAuthorized: Boolean = true,
        runShizuku: (String) -> Pair<Boolean, String>,
        runSu: (String) -> Pair<Boolean, String>
    ): Pair<Boolean, String> = runInternal(command, mode, shizukuAvailable, shizukuAuthorized, runShizuku, runSu)

    internal fun runSuForTest(
        command: String,
        exec: (Array<String>) -> Pair<Boolean, String>
    ): Pair<Boolean, String> = runSuCommand(command, exec)

    private fun runInternal(
        cmd: String,
        mode: Mode,
        shizukuAvailable: Boolean,
        shizukuAuthorized: Boolean,
        runShizuku: (String) -> Pair<Boolean, String>,
        runSu: (String) -> Pair<Boolean, String>
    ): Pair<Boolean, String> {
        return when (mode) {
            Mode.ROOT -> runSu(cmd)
            Mode.AUTO -> if (shizukuAvailable && shizukuAuthorized) runShizuku(cmd) else runSu(cmd)
        }
    }

    private fun runViaShizuku(cmd: String): Pair<Boolean, String> {
        return runShizukuProcess(
            create = {
                // Shizuku 13.x 将 newProcess 设为私有，这里反射调用（返回 Process 子类）
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            },
            await = { process -> waitForProcess(process, command = cmd) },
            fallback = { runViaSu(cmd) }
        )
    }

    internal fun runShizukuProcessForTest(
        create: () -> Process,
        await: (Process) -> Pair<Boolean, String>,
        fallback: () -> Pair<Boolean, String>
    ): Pair<Boolean, String> = runShizukuProcess(create, await, fallback)

    private fun runShizukuProcess(
        create: () -> Process,
        await: (Process) -> Pair<Boolean, String>,
        fallback: () -> Pair<Boolean, String>
    ): Pair<Boolean, String> {
        val process = try {
            create()
        } catch (e: Exception) {
            return fallback()
        }
        return try {
            await(process)
        } catch (e: Exception) {
            false to "Shizuku 命令结果未知，未通过 root 重复执行：${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun runViaSu(cmd: String): Pair<Boolean, String> {
        return try {
            runSuCommand(cmd) { args ->
                val process = Runtime.getRuntime().exec(args)
                waitForProcess(process, command = cmd)
            }
        } catch (e: IOException) {
            Pair(false, "root 不可用（su 不可用）")
        }
    }

    private fun runSuCommand(
        cmd: String,
        exec: (Array<String>) -> Pair<Boolean, String>
    ): Pair<Boolean, String> {
        val primary = exec(arrayOf("su", "-c", cmd))
        if (primary.first || !primary.second.trim().matches(
                Regex("(?i)^su: invalid uid/gid ['\"]?-c['\"]?$"))) {
            return primary
        }
        return exec(arrayOf("su", "0", "sh", "-c", cmd))
    }

    internal fun waitForProcess(
        process: Process,
        timeoutMs: Long = COMMAND_TIMEOUT_MS,
        command: String = ""
    ): Pair<Boolean, String> {
        val output = StringBuilder()
        val error = StringBuilder()
        val truncated = java.util.concurrent.atomic.AtomicBoolean(false)
        val readFailure = java.util.concurrent.atomic.AtomicBoolean(false)
        fun drain(stream: java.io.InputStream, destination: StringBuilder): Thread = Thread {
            try {
                stream.bufferedReader().use { reader ->
                    val buffer = CharArray(1024)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        if (destination.length < MAX_OUTPUT_CHARS) {
                            val saved = minOf(count, MAX_OUTPUT_CHARS - destination.length)
                            destination.append(buffer, 0, saved)
                            if (saved < count) truncated.set(true)
                        } else {
                            truncated.set(true)
                        }
                    }
                }
            } catch (_: IOException) {
                // Destroying a timed-out process can close its streams.
                readFailure.set(true)
            }
        }.apply { isDaemon = true; start() }

        val stdout = drain(process.inputStream, output)
        val stderr = drain(process.errorStream, error)
        // ShizukuRemoteProcess does not override Process.waitFor(timeout, unit).
        // The JDK default polls exitValue(), which its remote process rejects while running.
        val completion = FutureTask { process.waitFor() }
        Thread(completion, "escape-process-wait").apply { isDaemon = true; start() }
        try {
            val exit = try {
                completion.get(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
                try {
                    process.destroyForcibly()
                } catch (_: Exception) {
                    // The remote process may already have exited or disconnected.
                } finally {
                    completion.cancel(true)
                }
                return false to "命令超时，请检查 root/Shizuku 授权或重试"
            } catch (e: ExecutionException) {
                try {
                    process.destroyForcibly()
                } catch (_: Exception) { }
                return false to "命令执行中断，结果未知：${e.cause?.message ?: e.message}"
            }
            stdout.join(OUTPUT_DRAIN_TIMEOUT_MS)
            stderr.join(OUTPUT_DRAIN_TIMEOUT_MS)
            if (stdout.isAlive || stderr.isAlive) {
                return false to "命令输出读取超时，无法确认执行结果"
            }
            val standardOutput = output.toString().trim()
            val standardError = error.toString().trim()
            val message = listOf(standardOutput, standardError).filter { it.isNotEmpty() }.joinToString("\n")
            val confirmed = isCommandSuccessful(exit, standardOutput, command)
            val success = confirmed && isCommandSuccessful(exit, standardError) &&
                !truncated.get() && !readFailure.get()
            return success to when {
                readFailure.get() -> "命令输出读取失败，无法确认执行结果"
                truncated.get() -> "命令输出被截断，无法确认执行结果：${message.take(256)}"
                message.isBlank() && exit != 0 -> "命令退出码 $exit"
                !confirmed && command.startsWith("pm ") -> "无法确认包管理命令结果，请检查应用状态：${message.ifBlank { "没有输出" }}"
                message.isBlank() -> "命令已完成"
                else -> message
            }
        } finally {
            // A remote process can close these itself; cleanup must not mask its actual result.
            try { process.inputStream.close() } catch (_: IOException) { }
            try { process.errorStream.close() } catch (_: IOException) { }
            try { process.outputStream.close() } catch (_: IOException) { }
        }
    }

    internal fun isCommandSuccessful(exit: Int, message: String, command: String = ""): Boolean {
        if (exit != 0) return false
        val confirmed = when {
            command.startsWith("pm uninstall ") -> message.trim() == "Success"
            command.startsWith("pm disable-user --user 0 ") -> {
                val pkg = command.substringAfterLast(' ')
                message.trim() == "Package $pkg new state: disabled-user"
            }
            else -> true
        }
        return confirmed && message.lineSequence().none { line ->
            val text = line.trimStart()
            text.startsWith("Failure", ignoreCase = true) ||
                text.startsWith("Error:", ignoreCase = true) ||
                text.startsWith("Security exception:", ignoreCase = true)
        }
    }

    /**
     * 预留：检测是否有可用的 root 通道
     */
    fun isAnyChannelAvailable(): Boolean {
        if (Shizuku.pingBinder() && (Shizuku.isPreV11() ||
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)) return true
        return try {
            val p = Runtime.getRuntime().exec("su")
            DataOutputStream(p.outputStream).use { it.writeBytes("exit\n"); it.flush() }
            waitForProcess(p).first
        } catch (e: Exception) {
            false
        }
    }

    private const val COMMAND_TIMEOUT_MS = 10_000L
    private const val OUTPUT_DRAIN_TIMEOUT_MS = 3_000L
    private const val MAX_OUTPUT_CHARS = 4096
}
