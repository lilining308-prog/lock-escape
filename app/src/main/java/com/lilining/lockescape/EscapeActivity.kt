package com.lilining.lockescape

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.lilining.lockescape.databinding.ActivityEscapeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全屏逃生拦截界面：
 * 展示被锁定的顶层包名，提供 强停 / 停用 / 卸载 三个逃生动作。
 */
class EscapeActivity : Activity() {

    private lateinit var binding: ActivityEscapeBinding

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEscapeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 保持屏幕常亮，防止锁机病毒熄屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        refreshTopPackage()

        binding.btnForceStop.setOnClickListener { runAction { ShellExecutor.forceStop(it) } }
        binding.btnDisable.setOnClickListener { runAction { ShellExecutor.disable(it) } }
        binding.btnUninstall.setOnClickListener { runAction { ShellExecutor.uninstall(it) } }
        binding.btnExit.setOnClickListener { finish() }
        binding.btnRefresh.setOnClickListener { refreshTopPackage() }
    }

    private fun refreshTopPackage() {
        binding.tvTopPackage.text = "被锁应用：" + Prefs.currentTopPackage(this)
    }

    private fun runAction(action: (String) -> Pair<Boolean, String>) {
        val pkg = Prefs.currentTopPackage(this)
        if (pkg.isBlank()) {
            toast("尚未捕获到顶层应用包名")
            return
        }
        binding.tvResult.text = "执行中..."
        scope.launch {
            val (ok, out) = withContext(Dispatchers.IO) { action(pkg) }
            binding.tvResult.text = if (ok) "成功：$out" else "失败：$out"
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
