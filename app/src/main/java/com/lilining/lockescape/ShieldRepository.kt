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

class ShieldRepository(context: Context) {
    private val prefs = context.getSharedPreferences("shield_targets", Context.MODE_PRIVATE)

    fun isBlacklisted(packageName: String): Boolean = blacklist().any { it.packageName == packageName }

    fun blacklist(): List<ProtectedTargetEntity> = readTargets("blacklist")

    fun addBlacklist(packageName: String, label: String) {
        saveTarget(ProtectedTargetEntity(packageName, "blacklist", label))
    }

    fun addWhitelist(packageName: String, label: String) {
        saveTarget(ProtectedTargetEntity(packageName, "whitelist", label))
    }

    fun remove(packageName: String, kind: String) {
        val remaining = readTargets(kind).filterNot { it.packageName == packageName }
        prefs.edit().putString(kind, remaining.joinToString("\n") { "${it.packageName}|${it.label}" }).apply()
    }

    fun addEvent(packageName: String, label: String, source: String, action: String, result: String) {
        val line = "${System.currentTimeMillis()}|$packageName|$label|$source|$action|$result"
        val events = prefs.getStringSet("events", emptySet()).orEmpty().toMutableSet()
        events.add(line)
        prefs.edit().putStringSet("events", events.toList().sorted().takeLast(50).toSet()).apply()
    }

    private fun saveTarget(target: ProtectedTargetEntity) {
        val targets = readTargets(target.kind).filterNot { it.packageName == target.packageName } + target
        prefs.edit().putString(target.kind, targets.joinToString("\n") { "${it.packageName}|${it.label}" }).apply()
    }

    private fun readTargets(kind: String): List<ProtectedTargetEntity> = prefs.getString(kind, "").orEmpty()
        .lineSequence()
        .mapNotNull { line ->
            val pieces = line.split('|', limit = 2)
            if (pieces.size == 2 && AppSafety.isValidPackageName(pieces[0])) ProtectedTargetEntity(pieces[0], kind, pieces[1]) else null
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
