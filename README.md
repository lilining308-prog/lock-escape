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


**半成品** Android 应急逃生工具（非成品）：当设备被锁机病毒 / 无障碍恶意锁屏控制时，通过**物理按键组合触发**，强制全屏覆盖拦截，并借助 Shizuku / root 删除或停用锁机组件。

> **本应用是一个半成品。** 功能骨架与主要链路可用，但离打磨完成的产品仍有距离（例如历史包合规缺口、签名与发布流程、部分机型适配、界面与文案打磨等）。**欢迎基于本作品进行修改、扩展和完善**：本项目按 GPL-3.0 发布，你可以自由使用、修改和再分发源码，只需遵守 [LICENSE](LICENSE) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 中的义务（保留版权声明、注明修改、以兼容许可证分发等）。有需要的话，直接从仓库源码改起即可。

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

## APK 下载（按用途分类）

| 分类 | 目录 | 说明 |
|------|------|------|
| **stable（推荐）** | [`apk/stable/`](apk/stable/) | 2.0 正式构建，含 `assets/GPL/` |
| debug | [`apk/debug/`](apk/debug/) | 开发测试用，普通用户勿装 |
| archive | [`apk/archive/`](apk/archive/) | 历史版本，仅归档 |
| 旧布局兼容 | [`apk/_legacy-original-layout/`](apk/_legacy-original-layout/) | 原文件名别名，避免旧链失效 |

**直接下载（GitHub Release）：**

- 推荐 APK：[lock-escape-v2.0-release.apk](https://github.com/lilining308-prog/lock-escape/releases/download/v2.0/lock-escape-v2.0-release.apk)
- 当前交付构建：[lock-escape-current-release.apk](https://github.com/lilining308-prog/lock-escape/releases/download/v2.0/lock-escape-current-release.apk)
- 配套源码包：[lock-escape-current-source.zip](https://github.com/lilining308-prog/lock-escape/releases/download/v2.0/lock-escape-current-source.zip)

分类说明、SHA-256 与分发义务见 [apk/README.md](apk/README.md)；溯源线索见 [docs/APK源码对应表.md](docs/APK源码对应表.md)。历史 `0.1.0` 包缺少内置许可材料，分发时须包外补齐。手机端推广页见 [promo/index.html](promo/index.html)。

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
- [x] 修复音量下长按触发与系统应用误拦截，支持 root 优先模式（版本号仍为 2.0，不代表成品发布）
- [x] 用户自定义白名单 + 系统桌面默认屏蔽，避免桌面触发风控
- [x] 白名单与逃生记录的基础管理（新增/移除/清空、包名格式校验）——仍属半成品能力，未做完整打磨
- [x] 传递依赖许可清单（`gradle generateDependencyLicenseReport` → `docs/DEPENDENCY_LICENSES.md`）
- [x] 修复音量键触发时 SystemUI/输入法窗口误清空目标；su 多路径探测
- [ ] 成品化项：正式签名与发布渠道、机型适配、界面文案打磨、历史包合规缺口收口等

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

## 半成品声明

**本应用是半成品**，不是完成度已达发布的最终产品。若你有需要，**可以基于本作品进行修改**：

- 源码、资源与构建脚本全部开源（GPL-3.0），仓库根目录 `LICENSE` 与 `THIRD_PARTY_NOTICES.md` 说明了可做什么、要满足什么条件；
- 修改时请保留源码中的 GPL 头部与上游版权声明，并注明修改（GPL-3.0 第 5 节要求）；
- 再分发修改版时，须提供对应源码与许可材料，不得移除本项目的合规说明文件。

当前能力边界（半成品的典型表现）见上方「状态」与 [docs/APK源码对应表.md](docs/APK源码对应表.md) 中尚未解除的分发缺口。