# 当前开发版源码目录

这里是 lock-escape 当前开发版的主源码入口。

## 主源码路径

- 应用源码：`app/src/main/java/com/lilining/lockescape/`
- Android Manifest：`app/src/main/AndroidManifest.xml`
- 布局资源：`app/src/main/res/layout/`
- 颜色、主题、样式资源：`app/src/main/res/values/`
- Drawable 视觉资源：`app/src/main/res/drawable/`

## 关键源码文件

- `MainActivity.kt`：主界面、权限状态、安装向导。
- `EscapeActivity.kt`：逃生控制台、危险操作、白名单、日志管理、目标画像展示。
- `EscapeAccessibilityService.kt`：无障碍服务、音量键触发、顶层包名捕获。
- `ShellExecutor.kt`：Shizuku/root 命令执行，含包名校验和 su 兼容。
- `ThreatAssessment.kt`：锁机风险画像模型。
- `PackageThreatInspector.kt`：读取目标应用权限/组件并生成风险信号。
- `AppSafety.kt`：系统关键包、默认白名单、包名校验。
- `Prefs.kt`：本地持久化，包含顶层包名、日志、执行模式、用户白名单。
- `DaemonService.kt`：前台保活服务。
- `RootLauncherActivity.kt`：独立 Root 逃生入口。

## 测试源码

测试源码在：`app/src/test/java/com/lilining/lockescape/`

重点覆盖：

- 音量键触发状态机。
- 系统包保护与白名单。
- 包名校验与命令注入拦截。
- Shizuku/root 执行模式选择。
- 锁机风险画像模型。

## 许可

本项目主许可证为 GPL-3.0。源码文件保留 GPL 头部声明，完整许可证见仓库根目录 `LICENSE`。
