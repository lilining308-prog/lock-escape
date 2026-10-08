---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: c9eff8d25d622adeab9f9582a71074c0_8c278ba0b40411f1a1a1525400393706
    ReservedCode1: ohd7c4LHC/fK+dTaihNZMVOzOs+tFn4d6wX8RadTmvWqUCCH0ufh0zraOmJQOtmZZ4y5olOwC+k1bq5F0WUvYvbmLhejYHAAoD/0L3H81ITbORsxq/vhwkDmbC+OYrnSp7vMbrVSdtvoZagHU9LKyGyBrN6yt95H7X0NUGd0D8wkOUw3nOz2FUCvZSs=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: c9eff8d25d622adeab9f9582a71074c0_8c278ba0b40411f1a1a1525400393706
    ReservedCode2: ohd7c4LHC/fK+dTaihNZMVOzOs+tFn4d6wX8RadTmvWqUCCH0ufh0zraOmJQOtmZZ4y5olOwC+k1bq5F0WUvYvbmLhejYHAAoD/0L3H81ITbORsxq/vhwkDmbC+OYrnSp7vMbrVSdtvoZagHU9LKyGyBrN6yt95H7X0NUGd0D8wkOUw3nOz2FUCvZSs=
---

# lock-escape 锁机逃生


Android 紧急逃生工具：当设备被锁机病毒 / 无障碍恶意锁屏控制时，通过**物理按键组合触发**，强制全屏覆盖拦截，并借助 Shizuku / root 删除或停用锁机组件。

## 核心思路

```
无障碍服务常驻监听音量键组合（如音量+ × 4）
  → 触发全屏 overlay Activity（TYPE_APPLICATION_OVERLAY + 全屏标志）
  → 无障碍窗口事件获取当前顶层包名（锁机 App）
  → Shizuku/root 执行：am force-stop → pm disable-user（或 pm uninstall）
  → overlay 展示删除按钮，一键清掉锁机组件
```

## 技术选型参考

| 能力块 | 参考项目 | 许可证 | 核实状态 |
|--------|---------|--------|---------|
| 物理按键监听 | F111111shhh/Extinguish（fork 自 Moderpach/Extinguish） | GPL-3.0 | 已核实 |
| 全屏覆盖 | coderlidy/Fake-Lock-Screen | MIT | 已核实 |
| root 权限通道 | RikkaApps/Shizuku | Apache-2.0 | 已核实 |
| 停用/冻结实现 | aistra0528/Hail | GPL-3.0 | 已核实 |

## 未采用 / 合规排除来源

| 项目 | 风险原因 | 当前处理 |
|------|----------|----------|
| hnpdsyn/PhoneGuard | 仓库无 LICENSE 文件，无法确认复制、修改、再分发授权 | 已确认排除：不复制、不改写、不复用源码，也不作为实现依据 |
| me.lucky/wasted | 仓库当前 404，许可证无法核实 | 已确认排除：不复制、不改写、不复用源码，也不作为实现依据 |

## 开源许可与合规

- 本项目主许可证为 **GPL-3.0**（见 [LICENSE](LICENSE)）。
- 上游参考项目的版权归属与完整许可文本见 [NOTICE](NOTICE)，项目清单与合规说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
- release 运行时传递依赖逐项清单见 [docs/DEPENDENCY_LICENSES.md](docs/DEPENDENCY_LICENSES.md)，可用 `gradle generateDependencyLicenseReport` 再生；APK 内 `assets/GPL/DEPENDENCY_LICENSES.md` 随包分发。
- `hnpdsyn/PhoneGuard` 与 `me.lucky/wasted` 已列为“未采用 / 合规排除来源”：不得复制、改写或直接复用其源码；当前实现仅使用 Android SDK 公开 API、Shizuku 依赖和本项目原创 GPL-3.0 代码。
- 仓库公开已提交源码：[主源码 app/src/main/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/main/java)，[测试源码 app/src/test/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/test/java)。分发 APK 时须提供与该 APK 对应的源码版本、`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md`；仅提供 `main` 链接不足以证明源码对应关系。
- 所有原创源码按 GPL-3.0 分发，源码文件保留 GPL 头部声明；资源、构建脚本、文档和 APK 分发说明均按 GPL-3.0 随项目发布。
- 当前构建的 APK 在 `assets/GPL/` 中打包许可与源码获取说明；历史 `0.1.0` 包没有该目录，各历史包的材料状态见 [APK源码对应表](docs/APK源码对应表.md)。

## APK 下载

`apk/` 内保存的安装包为历史构建。对应源码和第三方依赖许可尚未逐包核实，当前不推荐作为已完成许可核查的发布版本下载或再分发。原“推荐下载”目录名仅为历史文件名，不代表当前核查结论。

历史 `0.1.0` 包缺少内置许可材料，已发布的 `2.0` 包仍包含过时 NOTICE；详细差异与待核项见 [APK源码对应表](docs/APK源码对应表.md)。

更多 APK 分类、历史版本和调试版说明见 [apk/README.md](apk/README.md)。
每个已跟踪 APK 的 SHA-256 与源码溯源线索见 [docs/APK源码对应表.md](docs/APK源码对应表.md)；不同 APK 不得仅凭相同版本号视为同一构建。

## 当前开发版源码

工作区中未提交的改动不会出现在以下 `main` 链接，也不会自动进入 `apk/` 中已发布的安装包；发布新版 APK 时，应提供与该 APK 构建版本对应的完整源码、构建脚本和许可材料。

- 当前开发版主源码目录：[app/src/main/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/main/java)
- 当前开发版测试源码目录：[app/src/test/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/test/java)
- 本地源码路径：`app/src/main/java/com/lilining/lockescape/`
- 本地测试源码路径：`app/src/test/java/com/lilining/lockescape/`
- 源码目录说明文件：`app/src/main/java/README.md`
- APK 内置源码公开说明：`app/src/main/assets/GPL/SOURCE_PATHS.md`，打包后位于 APK 的 `assets/GPL/SOURCE_PATHS.md`。

## 状态

- [x] 项目骨架（Kotlin + Gradle）
- [x] 无障碍服务 + 按键监听
- [x] 全屏 overlay 拦截界面
- [x] Shizuku/root 删除通道
- [x] 前台保活
- [x] 长按音量下键 1.5s 强制弹出（备用：连按 4 次）
- [x] 系统关键应用黑名单 + 二次确认 + 红字警告
- [x] 持续压制模式（2s 循环强停，防病毒重启）
- [x] 逃生日志（最近 20 条，主界面可查）
- [x] 锁屏自动解锁辅助（requestDismissKeyguard）
- [x] 2.0 正式版：修复音量下长按触发、系统应用误拦截，新增 root 优先模式
- [x] 用户自定义白名单 + 系统桌面默认屏蔽，避免桌面触发风控
- [x] 成熟工具化管理：白名单可新增/移除/清空，逃生记录可清空，包名格式自动校验
- [x] 传递依赖许可清单（`gradle generateDependencyLicenseReport` → `docs/DEPENDENCY_LICENSES.md`）
- [x] 修复音量键触发时 SystemUI/输入法窗口误清空目标；su 多路径探测

## 触发方式

目标识别依赖用户开启无障碍服务；只有在目标应用成为前台窗口后触发逃生，才会保留其包名。系统设置、桌面、系统组件和输入法窗口会清除之前的目标；逃生界面自身不会覆盖目标。记录超过 2 分钟自动失效，防止误操作旧目标。按键触发只是打开人工确认界面，不会自动卸载或停用任何应用。

执行前会再次校验目标包名、最近捕获时间、用户白名单、当前默认桌面和当前输入法；目标变更或失效则停止操作。命令输出中出现失败提示、输出读取不完整或超时，都不会报告为成功；停用和卸载还需收到包管理器对应的成功回执。Shizuku 命令已经发出但结果未知时，不会通过 root 重复执行。

守护服务在 Android 14 及以上使用 `specialUse` 前台服务类型；在旧版本使用系统支持的普通前台服务启动方式。

执行强停、停用、卸载需要已授权的 Shizuku 或 root；未授权时不会绕过 Android 权限限制。系统关键组件、默认桌面、输入法、Shizuku 和本工具都禁止通过工具操作。请勿将目标画像评分视作病毒鉴定结果。

| 触发 | 操作 |
|------|------|
| 长按音量下键 | 按住 1.5 秒弹出逃生界面 |
| 连按音量键 | 1.5 秒内连按 4 次弹出逃生界面 |

## 逃生动作

| 动作 | 命令 | 系统应用保护 |
|------|------|--------------|
| 强制停止 | am force-stop | 可执行但警告 |
| 停用 | pm disable-user --user 0 | 黑名单禁止 |
| 卸载 | pm uninstall --user 0 | 黑名单禁止 |
| 持续压制 | 2s 循环 force-stop | 黑名单禁止 |
