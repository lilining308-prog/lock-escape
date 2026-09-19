package com.lilining.lockescape

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
import com.lilining.lockescape.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

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
        binding.btnTestEscape.setOnClickListener {
            startActivity(Intent(this, EscapeActivity::class.java))
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
        binding.tvCurrentTop.text = "当前顶层应用：" + Prefs.currentTopPackage(this).ifBlank { "未知（等待无障碍事件）" }
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
