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

import rikka.shizuku.Shizuku
import java.io.DataOutputStream
import java.io.IOException

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
        run("am force-stop $pkg", mode)

    fun disable(pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        run("pm disable-user --user 0 $pkg", mode)

    fun uninstall(pkg: String, mode: Mode = Mode.AUTO): Pair<Boolean, String> =
        run("pm uninstall --user 0 $pkg", mode)

    private fun run(cmd: String, mode: Mode): Pair<Boolean, String> {
        return try {
            runInternal(cmd, mode, Shizuku.pingBinder(), ::runViaShizuku, ::runViaSu)
        } catch (e: Exception) {
            Pair(false, e.message ?: e.javaClass.simpleName)
        }
    }

    internal fun runForTest(
        command: String,
        mode: Mode,
        shizukuAvailable: Boolean,
        runShizuku: (String) -> Pair<Boolean, String>,
        runSu: (String) -> Pair<Boolean, String>
    ): Pair<Boolean, String> = runInternal(command, mode, shizukuAvailable, runShizuku, runSu)

    internal fun runSuForTest(
        command: String,
        exec: (Array<String>) -> Pair<Boolean, String>
    ): Pair<Boolean, String> = runSuCommand(command, exec)

    private fun runInternal(
        cmd: String,
        mode: Mode,
        shizukuAvailable: Boolean,
        runShizuku: (String) -> Pair<Boolean, String>,
        runSu: (String) -> Pair<Boolean, String>
    ): Pair<Boolean, String> {
        return when (mode) {
            Mode.ROOT -> runSu(cmd)
            Mode.AUTO -> if (shizukuAvailable) runShizuku(cmd) else runSu(cmd)
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
            val exit = process.waitFor()
            val out = process.inputStream.bufferedReader().readText().trim()
            val err = process.errorStream.bufferedReader().readText().trim()
            Pair(exit == 0, if (out.isNotEmpty()) out else err)
        } catch (e: Exception) {
            // Shizuku 通道失败时降级 su
            runViaSu(cmd)
        }
    }

    private fun runViaSu(cmd: String): Pair<Boolean, String> {
        return try {
            runSuCommand(cmd) { args ->
                val process = Runtime.getRuntime().exec(args)
                val exit = process.waitFor()
                val out = process.inputStream.bufferedReader().readText().trim()
                val err = process.errorStream.bufferedReader().readText().trim()
                Pair(exit == 0, if (out.isNotEmpty()) out else err)
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

    /**
     * 预留：检测是否有可用的 root 通道
     */
    fun isAnyChannelAvailable(): Boolean {
        if (Shizuku.pingBinder()) return true
        return try {
            val p = Runtime.getRuntime().exec("su")
            DataOutputStream(p.outputStream).use { it.writeBytes("exit\n"); it.flush() }
            p.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
