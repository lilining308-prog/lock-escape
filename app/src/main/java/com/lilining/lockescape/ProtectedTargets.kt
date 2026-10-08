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
import android.content.Intent
import android.provider.Settings

/** Resolve device-specific escape targets without relying solely on static launcher/IME lists. */
internal class ProtectedTargets(
    private val ownPackage: String,
    private val homePackage: String?,
    val inputMethodPackage: String?
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
