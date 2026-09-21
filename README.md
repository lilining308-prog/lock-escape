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

> **本应用还在开发中，请先不要使用。**

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
| 无障碍窗口监控 | hnpdsyn/PhoneGuard | 无 LICENSE | 仓库无许可证文件，仅参考思路未复制代码 |
| 全屏覆盖 | coderlidy/Fake-Lock-Screen | MIT | 已核实 |
| 紧急触发参考 | me.lucky/wasted | 待核实 | 仓库当前 404 不可访问 |
| root 权限通道 | RikkaApps/Shizuku | Apache-2.0 | 已核实 |
| 停用/冻结实现 | aistra0528/Hail | GPL-3.0 | 已核实 |

## 开源许可与合规

- 本项目主许可证为 **GPL-3.0**（见 [LICENSE](LICENSE)）。
- 上游参考项目的版权声明与许可文本见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
- `hnpdsyn/PhoneGuard` 无 LICENSE 文件，本项目未复制其代码；`me.lucky/wasted` 仓库不可访问，许可证待核实。

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

## 触发方式

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
