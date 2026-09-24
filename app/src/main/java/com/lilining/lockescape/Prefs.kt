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
    private const val KEY_EXECUTION_MODE = "execution_mode"
    private const val KEY_USER_WHITELIST = "user_whitelist"
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

    fun clearCurrentTopPackage(ctx: Context) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOP_PACKAGE)
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

    fun executionMode(ctx: Context): ShellExecutor.Mode {
        val raw = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_EXECUTION_MODE, ShellExecutor.Mode.AUTO.name)
        return ShellExecutor.Mode.entries.firstOrNull { it.name == raw } ?: ShellExecutor.Mode.AUTO
    }

    fun saveExecutionMode(ctx: Context, mode: ShellExecutor.Mode) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EXECUTION_MODE, mode.name)
            .apply()
    }

    fun userWhitelist(ctx: Context): Set<String> {
        val raw = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_WHITELIST, "") ?: ""
        return raw.split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .let { AppSafety.normalizeUserWhitelist(it.toSet()) }
    }

    fun addUserWhitelistPackage(ctx: Context, pkg: String) {
        val normalized = pkg.trim()
        if (!AppSafety.isValidPackageName(normalized)) return
        val whitelist = userWhitelist(ctx).toMutableSet()
        whitelist.add(normalized)
        saveUserWhitelist(ctx, whitelist)
    }

    fun removeUserWhitelistPackage(ctx: Context, pkg: String) {
        val whitelist = userWhitelist(ctx).toMutableSet()
        whitelist.remove(pkg.trim())
        saveUserWhitelist(ctx, whitelist)
    }

    fun clearUserWhitelist(ctx: Context) {
        saveUserWhitelist(ctx, emptySet())
    }

    fun clearEscapeLogs(ctx: Context) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LOGS)
            .apply()
    }

    private fun saveUserWhitelist(ctx: Context, whitelist: Set<String>) {
        val normalized = AppSafety.normalizeUserWhitelist(whitelist)
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_WHITELIST, normalized.sorted().joinToString("\n"))
            .apply()
    }
}
