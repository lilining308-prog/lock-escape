package com.lilining.lockescape

import android.content.Context

/**
 * 轻量本地存储：记录无障碍服务捕获到的当前顶层包名。
 */
object Prefs {
    private const val NAME = "lock_escape_prefs"
    private const val KEY_TOP_PACKAGE = "top_package"

    fun currentTopPackage(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOP_PACKAGE, "") ?: ""

    fun saveCurrentTopPackage(ctx: Context, pkg: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOP_PACKAGE, pkg)
            .apply()
    }
}
