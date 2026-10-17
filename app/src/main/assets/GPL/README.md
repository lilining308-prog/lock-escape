# tiedun 铁盾

Android 设备防护与应急处理工具：当设备被锁机程序或无障碍恶意锁屏控制时，通过物理按键组合快速呼出处理界面，强制全屏覆盖拦截，并借助 Shizuku / root 停止、停用或卸载问题组件。

## 许可

本项目按 GNU General Public License v3.0 发布，完整条款见随包 `LICENSE-GPL-3.0.txt`。

你可以使用、修改和再分发本软件，需：

- 保留版权声明与上游 `NOTICE` 声明；
- 改动后注明来源；
- 以 GPL-3.0 或兼容许可证分发衍生版本；
- 提供与分发包对应的完整源码。

第三方项目清单与合规说明见 `THIRD_PARTY_NOTICES.md`，传递依赖许可清单见 `DEPENDENCY_LICENSES.md`。

## 上游参考

| 上游 | 许可证 | 关系 |
|------|--------|------|
| F111111shhh/Extinguish（fork 自 Moderpach/Extinguish） | GPL-3.0 | 设计参考：物理按键监听 |
| coderlidy/Fake-Lock-Screen | MIT | 设计参考：全屏覆盖 |
| RikkaApps/Shizuku | Apache-2.0 | 依赖引入：特权命令通道 |
| aistra0528/Hail | GPL-3.0 | 设计参考：停用/冻结 |
| hnpdsyn/PhoneGuard | 无 LICENSE | 已排除，不得复用 |
| me.lucky/wasted | 无法核实 | 已排除，不得复用 |

## 源码

源码与文档请从本项目仓库获取。分发本 APK 时，须一并提供对应版本的完整源码、`LICENSE-GPL-3.0.txt`、`NOTICE` 与 `THIRD_PARTY_NOTICES.md`。

完整条款与免责声明以 `LICENSE-GPL-3.0.txt` 为准。
