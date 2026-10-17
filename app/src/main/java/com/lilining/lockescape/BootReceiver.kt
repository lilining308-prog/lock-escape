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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机后按用户此前的选择恢复护盾运行状态：
 * - 若用户开启过 Root 守护 / 无障碍锁定，则重新拉起守护进程；
 * - 重新启动前台保活服务。
 *
 * 不会在用户从未授权的情况下自行开启任何锁定。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val keeper = Prefs.rootKeeperEnabled(context)
        val daemon = Prefs.rootDaemonEnabled(context)
        if (keeper || daemon) {
            RootMonitorDaemon.setKeeperEnabled(context, keeper)
            RootMonitorDaemon.start(context)
        }

        runCatching {
            context.startForegroundService(Intent(context, DaemonService::class.java))
        }
    }
}
