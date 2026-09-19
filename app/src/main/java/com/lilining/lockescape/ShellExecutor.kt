package com.lilining.lockescape

import android.content.Context
import rikka.shizuku.Shizuku
import java.io.DataOutputStream
import java.io.IOException

/**
 * 逃生命令执行器：
 * 优先走 Shizuku 通道（免 root），否则降级 su（root）。
 * 基础版提供 强停 / 停用 / 卸载 三个动作。
 */
object ShellExecutor {

    fun forceStop(pkg: String): Pair<Boolean, String> =
        run("am force-stop $pkg")

    fun disable(pkg: String): Pair<Boolean, String> =
        run("pm disable-user --user 0 $pkg")

    fun uninstall(pkg: String): Pair<Boolean, String> =
        run("pm uninstall --user 0 $pkg")

    private fun run(cmd: String): Pair<Boolean, String> {
        return try {
            if (Shizuku.pingBinder()) {
                runViaShizuku(cmd)
            } else {
                runViaSu(cmd)
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: e.javaClass.simpleName)
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
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val exit = process.waitFor()
            val out = process.inputStream.bufferedReader().readText().trim()
            val err = process.errorStream.bufferedReader().readText().trim()
            Pair(exit == 0, if (out.isNotEmpty()) out else err)
        } catch (e: IOException) {
            Pair(false, "无 Shizuku 且无 root（su 不可用）")
        }
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
