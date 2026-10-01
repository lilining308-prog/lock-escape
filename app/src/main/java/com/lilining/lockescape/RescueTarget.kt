/*
 * Copyright (C) 2026 liyan
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.lilining.lockescape

/** Tracks an actionable window without accidentally targeting our own rescue UI. */
class RescueTarget {
    private var packageName = ""
    private var observedAt = 0L

    fun onWindow(pkg: String, ownPackage: String, whitelist: Set<String>,
                 inputMethodPackage: String? = null, nowMs: Long = System.currentTimeMillis()) {
        if (pkg == ownPackage) return
        if (pkg == inputMethodPackage ||
            AppSafety.isProtectedPackage(pkg) || AppSafety.isDefaultWhitelistedPackage(pkg)) {
            clear()
            return
        }
        if (isActionable(pkg, ownPackage, whitelist)) {
            packageName = pkg
            observedAt = nowMs
        } else {
            clear()
        }
    }

    fun current(nowMs: Long = System.currentTimeMillis()): String =
        if (observedAt != 0L && nowMs >= observedAt && nowMs - observedAt <= EXPIRY_MS) packageName else ""

    fun isActionable(pkg: String, ownPackage: String, whitelist: Set<String>): Boolean =
        pkg != ownPackage && AppSafety.isValidPackageName(pkg) &&
            !AppSafety.isProtectedPackage(pkg) && !AppSafety.isWhitelistedPackage(pkg, whitelist)

    fun clear() {
        packageName = ""
        observedAt = 0L
    }

    companion object { const val EXPIRY_MS = 120_000L }
}
