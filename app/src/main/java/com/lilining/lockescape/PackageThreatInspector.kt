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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

object PackageThreatInspector {
    fun inspect(ctx: Context, pkg: String): ThreatAssessment.Profile {
        val userWhitelist = Prefs.userWhitelist(ctx)
        val signals = ThreatAssessment.Signals(
            packageName = pkg,
            appLabel = appLabel(ctx, pkg),
            hasAccessibilityService = hasServiceWithPermission(ctx, pkg, Manifest.permission.BIND_ACCESSIBILITY_SERVICE),
            requestsOverlay = requestsPermission(ctx, pkg, Manifest.permission.SYSTEM_ALERT_WINDOW),
            hasDeviceAdminReceiver = hasReceiverWithPermission(ctx, pkg, Manifest.permission.BIND_DEVICE_ADMIN),
            isWhitelisted = AppSafety.isWhitelistedPackage(pkg, userWhitelist),
            isProtectedSystem = AppSafety.isProtectedPackage(pkg)
        )
        return ThreatAssessment.assess(signals)
    }

    private fun appLabel(ctx: Context, pkg: String): String = try {
        val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
        ctx.packageManager.getApplicationLabel(ai).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }

    private fun requestsPermission(ctx: Context, pkg: String, permission: String): Boolean = try {
        val info = ctx.packageManager.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.contains(permission) == true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun hasServiceWithPermission(ctx: Context, pkg: String, permission: String): Boolean = try {
        val info = ctx.packageManager.getPackageInfo(pkg, PackageManager.GET_SERVICES)
        info.services?.any { it.packageName == pkg && it.permission == permission } == true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun hasReceiverWithPermission(ctx: Context, pkg: String, permission: String): Boolean = try {
        val info = ctx.packageManager.getPackageInfo(pkg, PackageManager.GET_RECEIVERS)
        info.receivers?.any { it.packageName == pkg && it.permission == permission } == true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
