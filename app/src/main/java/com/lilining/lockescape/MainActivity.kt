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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lilining.lockescape.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            toast("Shizuku 权限已授予")
        } else {
            toast("Shizuku 权限被拒绝")
        }
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnShizuku.setOnClickListener { requestShizukuPermission() }
        binding.btnBatteryOptimization.setOnClickListener { requestIgnoreBatteryOptimizations() }
        binding.btnTestEscape.setOnClickListener {
            startActivity(Intent(this, EscapeActivity::class.java))
        }
        binding.btnRootEscape.setOnClickListener {
            startActivity(Intent(this, RootLauncherActivity::class.java))
        }

        binding.switchRootKeeper.isChecked = Prefs.rootKeeperEnabled(this)
        binding.switchRootKeeper.setOnCheckedChangeListener { _, checked ->
            Prefs.saveRootKeeperEnabled(this, checked)
            RootMonitorDaemon.setKeeperEnabled(this, checked)
            if (checked) {
                val started = RootMonitorDaemon.start(this)
                Prefs.saveRootDaemonEnabled(this, started)
                if (started) {
                    Prefs.addEscapeLog(this, "开启 Root 无障碍锁定")
                    toast("Root 无障碍锁定已开启")
                } else {
                    toast("无法启动 root 守护，请检查 su 授权")
                    binding.switchRootKeeper.isChecked = false
                    Prefs.saveRootKeeperEnabled(this, false)
                    RootMonitorDaemon.setKeeperEnabled(this, false)
                }
            } else {
                RootMonitorDaemon.setKeeperEnabled(this, false)
                Prefs.addEscapeLog(this, "关闭 Root 无障碍锁定")
                toast("已关闭无障碍锁定，可正常在系统设置中关闭服务")
            }
            refreshStatus()
        }

        binding.btnRootDaemon.setOnClickListener {
            if (RootMonitorDaemon.isRunning()) {
                RootMonitorDaemon.stop(this)
                Prefs.saveRootDaemonEnabled(this, false)
                Prefs.addEscapeLog(this, "停止 Root 守护监听")
                toast("Root 守护已停止")
            } else {
                val started = RootMonitorDaemon.start(this)
                Prefs.saveRootDaemonEnabled(this, started)
                toast(if (started) "Root 守护已启动" else "Root 守护启动失败")
            }
            refreshStatus()
        }

        requestNotificationPermissionIfNeeded()

        // 若用户此前开启过锁定，进程重建后自动拉起守护
        if (Prefs.rootKeeperEnabled(this) || Prefs.rootDaemonEnabled(this)) {
            RootMonitorDaemon.setKeeperEnabled(this, Prefs.rootKeeperEnabled(this))
            RootMonitorDaemon.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onStart() {
        super.onStart()
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onStop() {
        super.onStop()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun refreshStatus() {
        binding.tvAccessibilityStatus.text =
            if (isAccessibilityEnabled()) "无障碍服务：已开启" else "无障碍服务：未开启"
        binding.tvShizukuStatus.text = shizukuStatusText()
        binding.tvBatteryStatus.text = batteryOptimizationText()
        binding.tvCurrentTop.text = "最近目标应用：" + Prefs.currentTopPackage(this).ifBlank { "无（等待无障碍事件）" }
        val logs = Prefs.getEscapeLogs(this)
        binding.tvLogs.text = if (logs.isEmpty()) "暂无逃生记录" else logs.joinToString("\n")
        binding.tvChannelStatus.text = channelStatusText()

        val rootRunning = RootMonitorDaemon.isRunning()
        binding.tvRootDaemonStatus.text = buildString {
            append("Root 守护：").append(if (rootRunning) "运行中" else "未运行")
            append(" · 无障碍锁定：").append(if (Prefs.rootKeeperEnabled(this@MainActivity)) "开启" else "关闭")
            val fg = RootMonitorDaemon.lastForegroundPackage(this@MainActivity)
            if (fg.isNotBlank()) append("\nRoot 观测前台：").append(fg)
            val keeperLog = RootMonitorDaemon.keeperLog(this@MainActivity)
            if (keeperLog.isNotBlank()) append("\n").append(keeperLog)
        }
        binding.btnRootDaemon.text = if (rootRunning) "停止 Root 守护监听" else "启动 Root 守护监听"
    }

    private fun channelStatusText(): String {
        val mode = Prefs.executionMode(this)
        val shizukuReady = Shizuku.pingBinder() && (Shizuku.isPreV11() ||
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
        val root = RootChannel.probe()
        return buildString {
            append("执行模式：")
            append(if (mode == ShellExecutor.Mode.ROOT) "root 优先" else "自动（Shizuku 优先）")
            append(" · Root：").append(root.message)
            append(if (shizukuReady) " · Shizuku 就绪" else " · 需要 Shizuku 或 root")
        }
    }

    private fun batteryOptimizationText(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            "电池优化：已忽略（后台监听稳定）"
        } else {
            "电池优化：未忽略（系统可能杀掉后台监听）"
        }
    } else {
        "电池优化：无需处理（Android 6 以下）"
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toast("当前系统无需忽略电池优化")
            return
        }
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast("已处于忽略电池优化状态")
            return
        }
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabled.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun shizukuStatusText(): String = when {
        Shizuku.pingBinder() && Shizuku.isPreV11() -> "Shizuku：已连接（系统授予）"
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
            "Shizuku：已连接 + 已授权"
        Shizuku.pingBinder() -> "Shizuku：已连接，未授权"
        else -> "Shizuku：未连接（需安装 Shizuku 并以 adb/root 启动）"
    }

    private fun requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            toast("Shizuku 未运行，请先通过 adb 或 root 启动 Shizuku")
            return
        }
        if (Shizuku.isPreV11()) {
            toast("Shizuku 为系统授予模式，无需额外授权")
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
        } else {
            toast("Shizuku 已有权限")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
