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
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.FutureTask
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

    fun forceStop(pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildForceStopCommand(pkg)?.let { run(it, mode) } ?: invalidPackage(pkg)

    fun disable(pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildDisableCommand(pkg)?.let { run(it, mode) } ?: unsafePackage(pkg)

    fun uninstall(pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        buildUninstallCommand(pkg)?.let { run(it, mode) } ?: unsafePackage(pkg)

    internal fun forceStopForTest(pkg: String): Pair<Boolean, String> =
        buildForceStopCommand(pkg)?.let { true to it } ?: invalidPackage(pkg)

    internal fun disableForTest(pkg: String): Pair<Boolean, String> =
        buildDisableCommand(pkg)?.let { true to it } ?: unsafePackage(pkg)

    internal fun uninstallForTest(pkg: String): Pair<Boolean, String> =
        buildUninstallCommand(pkg)?.let { true to it } ?: unsafePackage(pkg)

    internal fun forceStopCommandForTest(pkg: String): String? = buildForceStopCommand(pkg)

    internal fun disableCommandForTest(pkg: String): String? = buildDisableCommand(pkg)

    internal fun uninstallCommandForTest(pkg: String): String? = buildUninstallCommand(pkg)

    private fun buildForceStopCommand(pkg: String): String? =
        normalizePackage(pkg)?.let { "am force-stop $it" }

    private fun buildDisableCommand(pkg: String): String? =
        normalizePackage(pkg)?.takeUnless { isCriticalPackage(it) }
            ?.let { "pm disable-user --user 0 $it" }

    private fun buildUninstallCommand(pkg: String): String? =
        normalizePackage(pkg)?.takeUnless { isCriticalPackage(it) }
            ?.let { "pm uninstall --user 0 $it" }

    private fun normalizePackage(pkg: String): String? {
        val normalized = pkg.trim()
        return normalized.takeIf { AppSafety.isValidPackageName(it) }
    }

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
        return try {
            // Shizuku 13.x 将 newProcess 设为私有，这里反射调用（返回 Process 子类）
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            waitForProcess(process)
        } catch (e: Exception) {
            Log.e("LockEscapeShizuku", "Shizuku process invocation failed", e)
            // Shizuku 通道失败时降级 su
            runViaSu(cmd)
        }
    }

    private fun runViaSu(cmd: String): Pair<Boolean, String> {
        return try {
            runSuCommand(cmd) { args ->
                val process = Runtime.getRuntime().exec(args)
                waitForProcess(process)
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
        if (primary.first || !primary.second.contains("invalid uid/gid", ignoreCase = true)) {
            return primary
        }
        return exec(arrayOf("su", "0", "sh", "-c", cmd))
    }

    internal fun waitForProcess(process: Process, timeoutMs: Long = COMMAND_TIMEOUT_MS): Pair<Boolean, String> {
        val output = StringBuilder()
        val error = StringBuilder()
        fun drain(stream: java.io.InputStream, destination: StringBuilder): Thread = Thread {
            try {
                stream.bufferedReader().use { reader ->
                    val buffer = CharArray(1024)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        if (destination.length < MAX_OUTPUT_CHARS) {
                            destination.append(buffer, 0, minOf(count, MAX_OUTPUT_CHARS - destination.length))
                        }
                    }
                }
            } catch (_: IOException) {
                // Destroying a timed-out process can close its streams.
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
                process.destroyForcibly()
                completion.cancel(true)
                return false to "命令超时，请检查 root/Shizuku 授权或重试"
            }
            stdout.join(1000)
            stderr.join(1000)
            val message = output.toString().trim().ifEmpty { error.toString().trim() }
            return (exit == 0) to message.ifBlank {
                if (exit == 0) "命令已完成" else "命令退出码 $exit"
            }
        } finally {
            process.inputStream.close()
            process.errorStream.close()
            process.outputStream.close()
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
    private const val MAX_OUTPUT_CHARS = 4096
}
