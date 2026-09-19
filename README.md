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

| 能力块 | 参考项目 | 许可证 |
|--------|---------|--------|
| 物理按键监听 | F111111shhh/Extinguish（fork 自 Moderpach/Extinguish） | GPL-3.0 |
| 无障碍窗口监控 | hnpdsyn/PhoneGuard | MIT |
| 全屏覆盖 | coderlidy/Fake-Lock-Screen | 待确认 |
| 紧急触发参考 | me.lucky/wasted | GPL-3.0 |
| root 权限通道 | RikkaApps/Shizuku | Apache-2.0 |
| 停用/冻结实现 | aistra0528/Hail | GPL-3.0 |

## 状态

- [ ] 项目骨架（Kotlin + Gradle）
- [ ] 无障碍服务 + 按键监听
- [ ] 全屏 overlay 拦截界面
- [ ] Shizuku/root 删除通道
- [ ] 前台保活
