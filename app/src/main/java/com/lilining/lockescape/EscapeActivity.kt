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

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.lilining.lockescape.databinding.ActivityEscapeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全屏逃生拦截界面：
 * 展示被锁定的顶层包名，提供 强停 / 停用 / 卸载 三个逃生动作，以及持续压制模式。
 *
 * 安全保护：
 * 1. 系统关键应用（桌面/系统UI/设置/电话等）禁止停用与卸载，强停也会警告；
 * 2. 所有破坏性动作执行前必须二次确认。
 */
class EscapeActivity : Activity() {

    private lateinit var binding: ActivityEscapeBinding

    private val scope = MainScope()
    private var suppressJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEscapeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 保持屏幕常亮，防止锁机病毒熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 设备被锁屏时自动请求解锁（配合 showWhenLocked / turnScreenOn）
        dismissKeyguard()

        refreshTopPackage()

        binding.btnForceStop.setOnClickListener {
            runAction("强停", "am force-stop 后该应用会立即退出，锁机病毒一般会重新拉起", true) { ShellExecutor.forceStop(it) }
        }
        binding.btnDisable.setOnClickListener {
            runAction("停用", "停用后应用将无法启动，用于永久压制锁机病毒", false) { ShellExecutor.disable(it) }
        }
        binding.btnUninstall.setOnClickListener {
            runAction("卸载", "卸载后应用将被移除（用户 0 范围）", false) { ShellExecutor.uninstall(it) }
        }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { refreshTopPackage() }

        // 持续压制模式：每隔 SUPPRESS_INTERVAL_MS 强制停止当前顶层包名，防止病毒重启
        binding.switchSuppress.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                startSuppress()
            } else {
                stopSuppress()
            }
        }
    }

    private fun dismissKeyguard() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, null)
        }
    }

    private fun startSuppress() {
        stopSuppress()
        val pkg = Prefs.currentTopPackage(this)
        if (pkg.isBlank()) {
            toast("尚未捕获到顶层应用包名，无法压制")
            binding.switchSuppress.isChecked = false
            return
        }
        if (isSystemApp(pkg)) {
            toast("顶层是系统应用，禁止持续压制")
            binding.switchSuppress.isChecked = false
            return
        }
        binding.tvResult.text = "持续压制中：$pkg"
        suppressJob = scope.launch {
            while (isActive) {
                val (ok, out) = withContext(Dispatchers.IO) { ShellExecutor.forceStop(pkg) }
                if (!ok) {
                    binding.tvResult.text = "压制失败：$out"
                    break
                }
                delay(SUPPRESS_INTERVAL_MS)
            }
        }
        Prefs.addEscapeLog(this, "开启持续压制 $pkg")
    }

    private fun stopSuppress() {
        suppressJob?.cancel()
        suppressJob = null
    }

    private fun refreshTopPackage() {
        val pkg = Prefs.currentTopPackage(this)
        val sb = StringBuilder("被锁应用：$pkg")
        if (isSystemApp(pkg)) {
            sb.append("\n⚠ 警告：这是系统应用，禁止停用/卸载！")
        }
        binding.tvTopPackage.text = sb.toString()
    }

    /** 判断是否为系统关键应用：系统预装 或 显式黑名单 */
    private fun isSystemApp(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        // 显式黑名单：桌面 / 系统UI / 设置 / 电话 / 系统界面相关
        if (pkg in SYSTEM_BLACKLIST) return true
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun runAction(
        actionName: String,
        desc: String,
        allowSystem: Boolean,
        action: (String) -> Pair<Boolean, String>
    ) {
        val pkg = Prefs.currentTopPackage(this)
        if (pkg.isBlank()) {
            toast("尚未捕获到顶层应用包名")
            return
        }
        val system = isSystemApp(pkg)
        if (system && !allowSystem) {
            AlertDialog.Builder(this)
                .setTitle("已拦截：系统应用")
                .setMessage("$pkg 是系统关键应用（桌面/系统UI/设置等），停用或卸载会导致桌面丢失、无法开机。\n\n如需处理，请先确认该应用确实是锁机病毒。")
                .setPositiveButton("仍然继续", null)
                .setNegativeButton("取消", null)
                .setOnDismissListener { refreshTopPackage() }
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("确认$actionName")
            .setMessage("将对 $pkg 执行：$actionName\n\n$desc" + if (system) "\n\n⚠ 该应用被识别为系统应用！" else "")
            .setPositiveButton("确认$actionName") { _, _ ->
                execute(pkg, action)
            }
            .setNegativeButton("取消", null)
            .setOnDismissListener { refreshTopPackage() }
            .show()
    }

    private fun execute(pkg: String, action: (String) -> Pair<Boolean, String>) {
        binding.tvResult.text = "执行中..."
        scope.launch {
            val (ok, out) = withContext(Dispatchers.IO) { action(pkg) }
            binding.tvResult.text = if (ok) "成功：$out" else "失败：$out"
            Prefs.addEscapeLog(this@EscapeActivity, if (ok) "成功：$pkg → $out" else "失败：$pkg → $out")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        stopSuppress()
        scope.cancel()
    }

    companion object {
        /** 持续压制间隔 */
        private const val SUPPRESS_INTERVAL_MS = 2000L

        private val SYSTEM_BLACKLIST = setOf(
            "com.android.launcher3",                    // 原生桌面
            "com.android.systemui",                     // 系统 UI
            "com.android.settings",                     // 设置
            "com.android.phone",                        // 电话
            "com.google.android.apps.nexuslauncher",    // Pixel 桌面
            "com.google.android.gms",                   // GMS
            "com.android.providers.media",              // 媒体存储
            "com.android.providers.settings"            // 设置存储
        )
    }
}
