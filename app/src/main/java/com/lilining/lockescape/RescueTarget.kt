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

/** Tracks an actionable window without accidentally targeting our own rescue UI. */
class RescueTarget {
    private var packageName = ""
    private var observedAt = 0L

    fun onWindow(pkg: String, ownPackage: String, whitelist: Set<String>,
                 inputMethodPackage: String? = null, nowMs: Long = System.currentTimeMillis()) {
        if (pkg == ownPackage) return
        // Returning to a launcher means the user left the suspect app — revoke.
        if (AppSafety.isDefaultWhitelistedPackage(pkg)) {
            clear()
            return
        }
        // Keyboard and other system UI windows must not become the rescue target, but
        // they also must not wipe a still-valid capture — otherwise the volume-key
        // trigger (which raises SystemUI) erases the package the user just selected.
        if (pkg == inputMethodPackage || AppSafety.isProtectedPackage(pkg)) {
            return
        }
        if (isActionable(pkg, ownPackage, whitelist, inputMethodPackage)) {
            packageName = pkg
            observedAt = nowMs
        } else {
            clear()
        }
    }

    fun current(nowMs: Long = System.currentTimeMillis()): String =
        if (observedAt != 0L && nowMs >= observedAt && nowMs - observedAt <= EXPIRY_MS) packageName else ""

    fun isActionable(pkg: String, ownPackage: String, whitelist: Set<String>,
                     inputMethodPackage: String? = null): Boolean =
        pkg != ownPackage && pkg != inputMethodPackage && AppSafety.isValidPackageName(pkg) &&
            !AppSafety.isProtectedPackage(pkg) && !AppSafety.isWhitelistedPackage(pkg, whitelist)

    fun canActOn(pkg: String, ownPackage: String, whitelist: Set<String>,
                 inputMethodPackage: String? = null, nowMs: Long = System.currentTimeMillis()): Boolean =
        current(nowMs) == pkg && isActionable(pkg, ownPackage, whitelist, inputMethodPackage)

    fun clear() {
        packageName = ""
        observedAt = 0L
    }

    companion object { const val EXPIRY_MS = 120_000L }
}
