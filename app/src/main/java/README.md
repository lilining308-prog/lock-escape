# 主源码目录

这里是 tiedun 铁盾的主源码入口。项目按 GPL-3.0 发布，改动时请保留源码文件中的 GPL 头部声明，并遵守根目录 `LICENSE` 与 `THIRD_PARTY_NOTICES.md`。

## 主源码路径

- 应用源码：`app/src/main/java/com/lilining/lockescape/`
- Android Manifest：`app/src/main/AndroidManifest.xml`
- 布局资源：`app/src/main/res/layout/`
- 颜色、主题、样式资源：`app/src/main/res/values/`
- Drawable 视觉资源：`app/src/main/res/drawable/`

## 关键源码文件

- `MainActivity.kt`：主界面、权限状态、Root 守护开关、安装向导。
- `EscapeActivity.kt`：处理控制台、强停/停用/卸载、白名单、日志、持续压制。
- `EscapeAccessibilityService.kt`：无障碍服务、音量键触发、顶层包名捕获、黑名单命中处理。
- `ShellExecutor.kt`：Shizuku/root 命令执行，含包名校验与 su 兼容。
- `RootChannel.kt`：Root 能力探测。
- `RootMonitorDaemon.kt`：root 守护循环、前台窗口采集、无障碍锁定（总开关可控）。
- `ShieldDatabase.kt` / `ShieldRepository.kt`：黑名单、白名单与事件数据。
- `ThreatAssessment.kt` / `PackageThreatInspector.kt`：目标风险画像。
- `AppSafety.kt`：系统关键包、默认白名单、包名校验。
- `Prefs.kt`：本地持久化（目标、日志、执行模式、压制开关、守护开关）。
- `DaemonService.kt`：前台保活服务。
- `BootReceiver.kt`：开机恢复守护与服务。
- `RootLauncherActivity.kt`：Root 直启入口（不在桌面生成快捷方式）。

## 测试源码

测试源码在：`app/src/test/java/com/lilining/lockescape/`

重点覆盖：

- 音量键触发状态机
- 系统包保护与白名单
- 包名校验与命令注入拦截
- Shizuku/root 执行模式选择
- 风险画像模型

## 许可

本项目主许可证为 GPL-3.0。所有原创源码按 GPL-3.0 分发，完整许可证见仓库根目录 `LICENSE`。

APK 内置许可与源码说明位于：`app/src/main/assets/GPL/`。
