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

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Root 守护进程：通过 su 启动一个受控 shell 循环。
 *
 * 职责：
 * 1. 周期采集当前前台窗口信息（dumpsys window / activity），写入应用可读状态文件；
 * 2. 可选地“锁定”本应用无障碍服务：若用户开启了 Keeper 且服务被系统或用户关掉，
 *    则重新写回 enabled_accessibility_services，并保留其它应用的无障碍服务条目。
 *
 * 安全边界（硬性）：
 * - 只有 Keeper 总开关打开时才会改写无障碍设置；
 * - 绝不清除、替换其它应用的无障碍服务；
 * - 绝不阻止卸载、绝不修改包管理器安装状态；
 * - 可随时停止守护进程；停止后不再改写任何系统设置。
 */
object RootMonitorDaemon {

    private const val SCRIPT_NAME = "root_monitor.sh"
    private const val STATUS_NAME = "root_monitor_status"
    private const val KEEPER_NAME = "root_keeper_enabled"
    private const val STOP_NAME = "root_monitor_stop"
    private const val LOG_NAME = "root_monitor.log"

    private const val PKG = "com.lilining.lockescape"
    private const val SERVICE_COMPONENT = "com.lilining.lockescape/.EscapeAccessibilityService"

    @Volatile
    private var process: Process? = null

    fun isRunning(): Boolean {
        val p = process ?: return false
        return try {
            p.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    /** 写入 Keeper 开关标记（1=开启无障碍锁定，0=关闭）。 */
    fun setKeeperEnabled(context: Context, enabled: Boolean) {
        file(context, KEEPER_NAME).writeText(if (enabled) "1" else "0")
    }

    fun isKeeperEnabled(context: Context): Boolean =
        runCatching { file(context, KEEPER_NAME).readText().trim() == "1" }.getOrDefault(false)

    /** 启动 root 守护循环。返回是否成功拉起进程。 */
    @Synchronized
    fun start(context: Context): Boolean {
        if (isRunning()) return true
        val script = file(context, SCRIPT_NAME)
        val status = file(context, STATUS_NAME)
        val keeper = file(context, KEEPER_NAME)
        val stop = file(context, STOP_NAME)
        val log = file(context, LOG_NAME)
        stop.delete()
        if (!keeper.exists()) keeper.writeText("0")
        script.writeText(buildScript(status.absolutePath, keeper.absolutePath, stop.absolutePath, log.absolutePath))
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh ${script.absolutePath}"))
            drainAsync(p.inputStream)
            drainAsync(p.errorStream)
            process = p
            true
        } catch (_: Exception) {
            process = null
            false
        }
    }

    /** 停止守护进程。先写停止标记，再结束 su 进程。 */
    @Synchronized
    fun stop(context: Context) {
        runCatching { file(context, STOP_NAME).writeText("1") }
        process?.let { p ->
            runCatching { p.destroy() }
        }
        process = null
    }

    /** 读取守护进程最近一次写出的前台窗口摘要。 */
    fun statusText(context: Context): String =
        runCatching { file(context, STATUS_NAME).readText().trim() }.getOrDefault("")

    fun keeperLog(context: Context): String =
        runCatching {
            file(context, LOG_NAME).readLines().takeLast(8).joinToString("\n")
        }.getOrDefault("")

    fun lastForegroundPackage(context: Context): String {
        val raw = statusText(context)
        val match = Regex("([A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+)/").find(raw)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun file(context: Context, name: String): File =
        File(context.filesDir, name)

    private fun drainAsync(stream: java.io.InputStream) {
        Thread {
            runCatching {
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    while (reader.readLine() != null) { /* discard */ }
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    /**
     * 生成守护脚本。
     * 路径用位置参数传入，避免在 Kotlin 里拼接用户可控内容造成注入；
     * shell 变量使用 \$ 转义，避免被 Kotlin 字符串模板吞掉。
     */
    private fun buildScript(statusPath: String, keeperPath: String, stopPath: String, logPath: String): String {
        val lines = listOf(
            "#!/system/bin/sh",
            "STATUS_FILE=\"$statusPath\"",
            "KEEPER_FILE=\"$keeperPath\"",
            "STOP_FILE=\"$stopPath\"",
            "LOG_FILE=\"$logPath\"",
            "PKG_NAME=\"$PKG\"",
            "SERVICE_COMPONENT=\"$SERVICE_COMPONENT\"",
            "",
            "append_log() {",
            "  echo \"\$(date '+%Y-%m-%d %H:%M:%S') \$1\" >> \"\$LOG_FILE\" 2>/dev/null",
            "}",
            "",
            "while [ ! -f \"\$STOP_FILE\" ]; do",
            "  focus=\$(dumpsys window 2>/dev/null | grep mCurrentFocus | head -n 1)",
            "  if [ -z \"\$focus\" ]; then",
            "    focus=\$(dumpsys activity activities 2>/dev/null | grep topResumedActivity | head -n 1)",
            "  fi",
            "  echo \"ts=\$(date +%s) \$focus\" > \"\$STATUS_FILE.tmp\" 2>/dev/null \\",
            "    && mv \"\$STATUS_FILE.tmp\" \"\$STATUS_FILE\" 2>/dev/null",
            "",
            "  if [ -f \"\$KEEPER_FILE\" ] && [ \"\$(cat \"\$KEEPER_FILE\" 2>/dev/null)\" = \"1\" ]; then",
            "    cur=\$(settings get secure enabled_accessibility_services 2>/dev/null)",
            "    [ \"\$cur\" = \"null\" ] && cur=\"\"",
            "    case \":\$cur:\" in",
            "      *\":\$PKG_NAME/\"* ) : ;;",
            "      *)",
            "        if [ -z \"\$cur\" ]; then",
            "          settings put secure enabled_accessibility_services \"\$SERVICE_COMPONENT\"",
            "        else",
            "          settings put secure enabled_accessibility_services \"\$cur:\$SERVICE_COMPONENT\"",
            "        fi",
            "        settings put secure accessibility_enabled 1",
            "        append_log \"re-enabled accessibility\"",
            "        ;;",
            "    esac",
            "  fi",
            "",
            "  sleep 2",
            "done",
            "append_log \"daemon stopped\""
        )
        return lines.joinToString("\n")
    }
}
