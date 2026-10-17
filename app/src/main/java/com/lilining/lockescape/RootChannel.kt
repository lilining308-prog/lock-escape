/*
 * Copyright (C) 2026 liyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.lilining.lockescape

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/** Root capability probe. It does not claim a device-level listener unless su responds. */
object RootChannel {
    fun probe(): Result {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val exit = process.waitFor()
            if (exit == 0 && output.contains("uid=0")) Result(true, "Root 可用")
            else Result(false, "Root 未授权")
        } catch (_: Exception) {
            Result(false, "未找到可用 su")
        }
    }

    fun startMonitor(context: Context): Result {
        val probe = probe()
        return if (probe.available) {
            // The device-specific monitor is intentionally gated until its event source is verified.
            Result(false, "Root 已授权，监听器待设备适配验证")
        } else probe
    }

    data class Result(val available: Boolean, val message: String)
}
