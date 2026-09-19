package com.lilining.lockescape

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 轻量本地存储：记录无障碍服务捕获到的当前顶层包名、逃生日志。
 */
object Prefs {
    private const val NAME = "lock_escape_prefs"
    private const val KEY_TOP_PACKAGE = "top_package"
    private const val KEY_LOGS = "escape_logs"
    private const val MAX_LOGS = 20

    fun currentTopPackage(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOP_PACKAGE, "") ?: ""

    fun saveCurrentTopPackage(ctx: Context, pkg: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOP_PACKAGE, pkg)
            .apply()
    }

    /** 追加一条逃生日志（环形保留最近 MAX_LOGS 条，新日志在前） */
    fun addEscapeLog(ctx: Context, msg: String) {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logs = getEscapeLogs(ctx).toMutableList()
        logs.add(0, "[$time] $msg")
        if (logs.size > MAX_LOGS) {
            logs.subList(MAX_LOGS, logs.size).clear()
        }
        sp.edit().putString(KEY_LOGS, logs.joinToString("\n")).apply()
    }

    fun getEscapeLogs(ctx: Context): List<String> {
        val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_LOGS, "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("\n")
    }
}
