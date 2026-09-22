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
            runAction("强停", "am force-stop 后该应用会立即退出，锁机病毒一般会重新拉起", true) { pkg, mode -> ShellExecutor.forceStop(pkg, mode) }
        }
        binding.btnDisable.setOnClickListener {
            runAction("停用", "停用后应用将无法启动，用于永久压制锁机病毒", false) { pkg, mode -> ShellExecutor.disable(pkg, mode) }
        }
        binding.btnUninstall.setOnClickListener {
            runAction("卸载", "卸载后应用将被移除（用户 0 范围）", false) { pkg, mode -> ShellExecutor.uninstall(pkg, mode) }
        }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { refreshTopPackage() }
        binding.btnWhitelistCurrent.setOnClickListener { whitelistCurrentPackage() }
        binding.btnWhitelistInput.setOnClickListener { whitelistInputPackage() }
        binding.switchRootMode.isChecked = Prefs.executionMode(this) == ShellExecutor.Mode.ROOT
        binding.switchRootMode.setOnCheckedChangeListener { _, checked ->
            Prefs.saveExecutionMode(this, if (checked) ShellExecutor.Mode.ROOT else ShellExecutor.Mode.AUTO)
            toast(if (checked) "已切换为 root 优先模式" else "已切换为自动模式")
        }

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
        if (isWhitelisted(pkg)) {
            toast("顶层应用已在白名单，禁止持续压制")
            binding.switchSuppress.isChecked = false
            return
        }
        binding.tvResult.text = "持续压制中：$pkg"
        suppressJob = scope.launch {
            while (isActive) {
                val mode = Prefs.executionMode(this@EscapeActivity)
                val (ok, out) = withContext(Dispatchers.IO) { ShellExecutor.forceStop(pkg, mode) }
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
            sb.append("\n⚠ 警告：这是系统关键应用，禁止停用/卸载！")
        }
        if (isWhitelisted(pkg)) {
            sb.append("\n已加入白名单：不会触发风控或逃生动作")
        }
        binding.tvTopPackage.text = sb.toString()
        binding.tvWhitelist.text = whitelistText()
        binding.tvLogs.text = Prefs.getEscapeLogs(this).joinToString("\n").ifBlank { "暂无逃生记录" }
    }

    /** 只保护会导致设备不可用的关键系统包；普通预装应用允许处理。 */
    private fun isSystemApp(pkg: String): Boolean = AppSafety.isProtectedPackage(pkg)

    private fun isWhitelisted(pkg: String): Boolean =
        AppSafety.isWhitelistedPackage(pkg, Prefs.userWhitelist(this))

    private fun whitelistText(): String {
        val whitelist = Prefs.userWhitelist(this).sorted()
        return if (whitelist.isEmpty()) {
            "用户白名单：暂无。系统桌面已默认屏蔽。"
        } else {
            "用户白名单：\n" + whitelist.joinToString("\n")
        }
    }

    private fun whitelistCurrentPackage() {
        val pkg = Prefs.currentTopPackage(this)
        if (pkg.isBlank()) {
            toast("尚未捕获到顶层应用包名")
            return
        }
        addWhitelistPackage(pkg)
    }

    private fun whitelistInputPackage() {
        addWhitelistPackage(binding.etWhitelistPackage.text?.toString().orEmpty())
        binding.etWhitelistPackage.text?.clear()
    }

    private fun addWhitelistPackage(pkg: String) {
        val normalized = pkg.trim()
        if (normalized.isBlank()) {
            toast("请输入包名")
            return
        }
        Prefs.addUserWhitelistPackage(this, normalized)
        if (Prefs.currentTopPackage(this) == normalized) {
            Prefs.clearCurrentTopPackage(this)
        }
        Prefs.addEscapeLog(this, "加入白名单 $normalized")
        toast("已加入白名单：$normalized")
        refreshTopPackage()
    }

    private fun runAction(
        actionName: String,
        desc: String,
        allowSystem: Boolean,
        action: (String, ShellExecutor.Mode) -> Pair<Boolean, String>
    ) {
        val pkg = Prefs.currentTopPackage(this)
        if (pkg.isBlank()) {
            toast("尚未捕获到顶层应用包名")
            return
        }
        val system = isSystemApp(pkg)
        if (isWhitelisted(pkg)) {
            AlertDialog.Builder(this)
                .setTitle("已拦截：白名单应用")
                .setMessage("$pkg 已在白名单中，不会执行 $actionName。")
                .setPositiveButton("知道了", null)
                .setOnDismissListener { refreshTopPackage() }
                .show()
            return
        }
        if (system && !allowSystem) {
            AlertDialog.Builder(this)
                .setTitle("已拦截：系统关键应用")
                .setMessage("$pkg 是系统关键组件（桌面/系统UI/设置/电话等），停用或卸载可能导致设备不可用。")
                .setPositiveButton("知道了", null)
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

    private fun execute(pkg: String, action: (String, ShellExecutor.Mode) -> Pair<Boolean, String>) {
        binding.tvResult.text = "执行中..."
        scope.launch {
            val mode = Prefs.executionMode(this@EscapeActivity)
            val (ok, out) = withContext(Dispatchers.IO) { action(pkg, mode) }
            binding.tvResult.text = if (ok) "成功：$out" else "失败：$out"
            Prefs.addEscapeLog(this@EscapeActivity, if (ok) "成功[$mode]：$pkg -> $out" else "失败[$mode]：$pkg -> $out")
            refreshTopPackage()
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

    }
}
