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
import rikka.shizuku.Shizuku

/**
 * ADB 式无障碍锁定：执行与 `adb shell settings put` 完全相同的命令，
 * 通过 Shizuku 特权通道运行，无需 root。
 *
 * 也可由用户手动在电脑上执行 [manualLockCommand] 完成同样的锁定。
 *
 * 安全边界：只追加/移除本应用自己的无障碍服务条目，不改动其它应用的服务。
 */
object AdbAccessibilityLock {

    private const val PKG = "com.lilining.lockescape"
    private const val SERVICE_COMPONENT = "com.lilining.lockescape/.EscapeAccessibilityService"

    /** 电脑端等价命令（用户可自行 adb 执行）。 */
    val manualLockCommand: String =
        "adb shell settings put secure enabled_accessibility_services " +
            "\"$SERVICE_COMPONENT\"\n" +
            "adb shell settings put secure accessibility_enabled 1"

    fun isShizukuReady(): Boolean = try {
        Shizuku.pingBinder() && (Shizuku.isPreV11() ||
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
    } catch (_: Throwable) {
        false
    }

    /** 锁定：把本应用无障碍服务写入 enabled_accessibility_services。 */
    fun lock(): Result {
        if (!isShizukuReady()) return Result(false, "Shizuku 未就绪，可手动执行 adb 命令")
        val get = run("settings get secure enabled_accessibility_services")
        if (!get.ok) return Result(false, "读取无障碍设置失败：${get.output}")
        val current = get.output.trim().let { if (it == "null") "" else it }
        if (current.contains(PKG)) return Result(true, "无障碍已处于锁定状态")
        val next = if (current.isEmpty()) SERVICE_COMPONENT else "$current:$SERVICE_COMPONENT"
        val put = run("settings put secure enabled_accessibility_services \"$next\"")
        if (!put.ok) return Result(false, "写入无障碍列表失败：${put.output}")
        val en = run("settings put secure accessibility_enabled 1")
        return if (en.ok) Result(true, "ADB 式无障碍锁定成功")
        else Result(false, "已写入服务但启用失败：${en.output}")
    }

    /** 解除锁定：仅移除本应用的条目，保留其它应用无障碍服务。 */
    fun unlock(): Result {
        if (!isShizukuReady()) return Result(false, "Shizuku 未就绪")
        val get = run("settings get secure enabled_accessibility_services")
        if (!get.ok) return Result(false, "读取无障碍设置失败：${get.output}")
        val current = get.output.trim().let { if (it == "null") "" else it }
        if (!current.contains(PKG)) return Result(true, "未处于锁定状态")
        val remaining = current.split(':')
            .filter { it.isNotBlank() && !it.startsWith("$PKG/") && it != PKG }
            .joinToString(":")
        val put = run("settings put secure enabled_accessibility_services \"$remaining\"")
        return if (put.ok) Result(true, "已解除 ADB 式无障碍锁定")
        else Result(false, "解除失败：${put.output}")
    }

    fun isLocked(): Boolean {
        val get = run("settings get secure enabled_accessibility_services")
        return get.ok && get.output.contains(PKG)
    }

    private fun run(command: String): CmdResult {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val out = process.inputStream.bufferedReader().readText().trim()
            val err = process.errorStream.bufferedReader().readText().trim()
            val code = process.waitFor()
            CmdResult(code == 0, listOf(out, err).filter { it.isNotEmpty() }.joinToString("\n"))
        } catch (e: Exception) {
            CmdResult(false, e.message ?: e.javaClass.simpleName)
        }
    }

    data class Result(val ok: Boolean, val message: String)
    private data class CmdResult(val ok: Boolean, val output: String)
}
