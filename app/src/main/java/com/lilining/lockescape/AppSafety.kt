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

object AppSafety {
    private val PROTECTED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.google.android.gms"
    )

    private val DEFAULT_WHITELIST_PACKAGES = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.vivo.launcher",
        "com.sec.android.app.launcher",
        "com.microsoft.launcher",
        "com.teslacoilsw.launcher"
    )

    fun isProtectedPackage(pkg: String): Boolean = pkg in PROTECTED_PACKAGES

    fun isWhitelistedPackage(pkg: String, userWhitelist: Set<String>): Boolean =
        pkg in DEFAULT_WHITELIST_PACKAGES || pkg in userWhitelist

    fun shouldTrackTopPackage(pkg: String, userWhitelist: Set<String>): Boolean =
        pkg.isNotBlank() && !isWhitelistedPackage(pkg, userWhitelist)
}
