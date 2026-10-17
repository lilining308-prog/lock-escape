# tiedun 铁盾

Android 设备防护与应急处理工具：当设备被锁机程序或无障碍恶意锁屏控制时，通过**物理按键组合**快速呼出处理界面，强制全屏覆盖拦截，并借助 Shizuku / root 停止、停用或卸载问题组件。内置黑名单护盾、Root 守护监听与无障碍锁定，可在受控设备上长期驻留防护。

## 核心思路

```
无障碍服务常驻监听前台切换 + 音量键组合（长按音量下 / 连按 4 次）
  → 触发全屏处理界面
  → 无障碍 / Root 通道获取当前顶层包名
  → 安全校验（黑名单、白名单、系统关键包）
  → Shizuku/root 执行：am force-stop → pm disable-user（或 pm uninstall）
  → 持续压制可选：2s 循环强停，防止问题组件自动重启
```

## 技术选型参考

| 能力块 | 参考项目 | 许可证 | 核实状态 |
|--------|---------|--------|---------|
| 物理按键监听 | [F111111shhh/Extinguish](https://github.com/F111111shhh/Extinguish)（fork 自 [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)） | GPL-3.0 | 已核实 |
| 全屏覆盖 | [coderlidy/Fake-Lock-Screen](https://github.com/coderlidy/Fake-Lock-Screen) | MIT | 已核实 |
| root 权限通道 | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) | Apache-2.0 | 已核实 |
| 停用/冻结实现 | [aistra0528/Hail](https://github.com/aistra0528/Hail) | GPL-3.0 | 已核实 |

## 未采用 / 合规排除来源

| 项目 | 风险原因 | 当前处理 |
|------|----------|----------|
| [hnpdsyn/PhoneGuard](https://github.com/hnpdsyn/PhoneGuard) | 仓库无 LICENSE 文件，无法确认复制、修改、再分发授权 | 已确认排除：不复制、不改写、不复用源码，也不作为实现依据 |
| [me.lucky/wasted](https://github.com/me.lucky/wasted) | 仓库当前无法访问（HTTP 404），许可证无法核实 | 已确认排除：不复制、不改写、不复用源码，也不作为实现依据 |

## 上游清单一览

本项目涉及的全部上游来源如下，完整版权与许可文本见 [NOTICE](NOTICE)，逐项合规说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)：

| # | 上游 | 许可证 | 与本项目关系 |
|---|------|--------|--------------|
| 1 | F111111shhh/Extinguish | GPL-3.0 | 设计参考：物理按键监听 |
| 2 | Moderpach/Extinguish | GPL-3.0 | Extinguish 的上游 fork 源 |
| 3 | coderlidy/Fake-Lock-Screen | MIT | 设计参考：全屏覆盖 |
| 4 | RikkaApps/Shizuku | Apache-2.0 | 依赖引入：特权命令通道 |
| 5 | aistra0528/Hail | GPL-3.0 | 设计参考：停用/冻结 |
| 6 | hnpdsyn/PhoneGuard | 无 LICENSE | **排除**，不得作为实现依据 |
| 7 | me.lucky/wasted | 无法核实 | **排除**，不得作为实现依据 |

传递依赖（AndroidX / Material / Kotlin / Coroutines 等）逐项清单见 [docs/DEPENDENCY_LICENSES.md](docs/DEPENDENCY_LICENSES.md)，可用 `gradle generateDependencyLicenseReport` 再生。

## 开源许可与合规

- 本项目主许可证为 **GPL-3.0**（见 [LICENSE](LICENSE)）。
- 上游参考项目的版权归属与完整许可文本见 [NOTICE](NOTICE)。
- 所有原创源码按 GPL-3.0 分发，源码文件保留 GPL 头部声明；资源、构建脚本、文档与 APK 分发材料均随项目发布。
- 分发 APK 时须提供与该 APK 对应的源码版本、`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md`；仅提供分支链接不足以证明源码对应关系。
- `hnpdsyn/PhoneGuard` 与 `me.lucky/wasted` 不得复制、改写或直接复用；当前实现仅使用 Android SDK 公开 API、Shizuku 依赖与本项目原创 GPL-3.0 代码。
- APK 内 `assets/GPL/` 打包许可与源码获取说明；第三方许可材料目录见 [third_party/](third_party/)。

## 功能

| 能力 | 说明 |
|------|------|
| 黑名单护盾 | 监测前台应用，命中黑名单弹出警告并可处理 |
| 白名单保护 | 系统桌面、输入法、关键组件与用户白名单禁止操作 |
| 应急处理 | 强停 / 停用 / 卸载，执行前二次确认 |
| 持续压制 | 常开可选，控制台打开即压制，2s 循环强停 |
| Root 守护 | root 守护进程采集前台窗口，可选锁定本应用无障碍 |
| Shizuku 通道 | 无 root 时经 Shizuku 执行固定命令 |
| 逃生触发 | 长按音量下 1.5s 或连按音量键 4 次 |
| 运行保障 | 前台服务、开机恢复、电池优化引导、崩溃记录 |

## 触发方式

目标识别依赖用户开启无障碍服务；只有在目标应用成为前台窗口后触发，才会保留其包名。系统设置、桌面、系统组件和输入法窗口会清除之前的目标；处理界面自身不会覆盖目标。记录超过 2 分钟自动失效，防止误操作旧目标。按键触发只是打开人工确认界面，不会自动卸载或停用任何应用。

执行前会再次校验目标包名、最近捕获时间、用户白名单、当前默认桌面和当前输入法；目标变更或失效则停止操作。命令输出中出现失败提示、输出读取不完整或超时，都不会报告为成功；停用和卸载还需收到包管理器对应的成功回执。Shizuku 命令已经发出但结果未知时，不会通过 root 重复执行。

| 触发 | 操作 |
|------|------|
| 长按音量下键 | 按住 1.5 秒弹出处理界面 |
| 连按音量键 | 1.5 秒内连按 4 次弹出处理界面 |

## 处理动作

| 动作 | 命令 | 系统应用保护 |
|------|------|--------------|
| 强制停止 | am force-stop | 可执行但警告 |
| 停用 | pm disable-user --user 0 | 黑名单禁止 |
| 卸载 | pm uninstall --user 0 | 黑名单禁止，成功后返回桌面 |
| 持续压制 | 2s 循环 force-stop | 黑名单禁止 |

卸载成功会提示并返回桌面；失败时提示「可能删除失败」，可选择返回桌面或留在本页重试。

## 构建

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## 状态

- ✅ 项目骨架（Kotlin + Gradle）
- ✅ 无障碍服务 + 按键监听
- ✅ 全屏处理界面
- ✅ Shizuku/root 处理通道
- ✅ 前台保活
- ✅ 系统关键应用黑名单 + 二次确认
- ✅ 持续压制（常开可选，打开即压制）
- ✅ 用户白名单 + 系统桌面默认屏蔽
- ✅ Root 守护监听 + 无障碍锁定（总开关可关）
- ✅ 开机恢复
- ✅ 卸载成功返回桌面 / 失败提示
- ✅ 传递依赖许可清单
- ✅ 单一桌面快捷方式、原创图标与终端风配色

## 设计说明

本项目为原创实现：界面、图标、文案与代码结构均独立设计，不复制任何参考项目的源码、布局或视觉资源。上表中的上游仅作为能力思路与许可证义务的来源记录。

安全边界：

- 系统关键组件、默认桌面、输入法、Shizuku 与本工具自身禁止处理。
- Root 无障碍锁定仅在用户显式开启时生效，随时可关，不阻止卸载。
- 目标画像与评分仅作参考，不构成病毒鉴定结论。

---

本项目按 GPL-3.0 发布，欢迎修改与扩展，请保留版权声明与上游声明，并按 [LICENSE](LICENSE) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 履行相应义务。
