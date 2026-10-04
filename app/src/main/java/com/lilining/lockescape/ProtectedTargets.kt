/*
 * Copyright (C) 2026 liyan
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.lilining.lockescape

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Resolve device-specific escape targets without relying solely on static launcher/IME lists. */
internal class ProtectedTargets(
    private val ownPackage: String,
    private val homePackage: String?,
    private val inputMethodPackage: String?
) {
    fun canActOn(pkg: String, whitelist: Set<String>): Boolean =
        pkg != ownPackage && pkg != homePackage && pkg != inputMethodPackage &&
            AppSafety.shouldTrackTopPackage(pkg, whitelist, inputMethodPackage)

    companion object {
        fun resolvedHomePackage(pkg: String?): String? =
            pkg?.takeUnless { it.isBlank() || it == "android" }

        fun from(ctx: Context): ProtectedTargets {
            val home = resolvedHomePackage(ctx.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                0
            )?.activityInfo?.packageName)
            val ime = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
            )?.substringBefore('/')
            return ProtectedTargets(ctx.packageName, home, ime)
        }
    }
}
