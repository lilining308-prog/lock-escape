# Third-Party Notices

本项目的设计实现参考了以下开源项目。所有上游项目的版权归其各自作者所有。
本声明依据各上游项目许可证要求提供，供本项目使用者与下游分发者遵守。

## 参考项目清单

| 能力块 | 上游项目 | 许可证 | 状态 |
|--------|---------|--------|------|
| 物理按键监听 | [F111111shhh/Extinguish](https://github.com/F111111shhh/Extinguish)（fork 自 [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)） | GPL-3.0 | 已核实（GitHub API） |
| 无障碍窗口监控 | [hnpdsyn/PhoneGuard](https://github.com/hnpdsyn/PhoneGuard) | 无 LICENSE | 仓库无许可证文件，仅参考公开实现思路，未复制代码 |
| 全屏覆盖 | [coderlidy/Fake-Lock-Screen](https://github.com/coderlidy/Fake-Lock-Screen) | MIT | 已核实（GitHub API） |
| 紧急触发参考 | [me.lucky/wasted](https://github.com/me.lucky/wasted) | 待核实 | 仓库当前无法访问（HTTP 404），许可证待进一步核实 |
| root 权限通道 | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) | Apache-2.0 | 已核实（以依赖形式引入） |
| 停用/冻结实现 | [aistra0528/Hail](https://github.com/aistra0528/Hail) | GPL-3.0 | 已核实（GitHub API） |

## 许可证文本位置

- GPL-3.0 全文：见本项目根目录 [LICENSE](LICENSE)
- Apache-2.0 / MIT 全文：见本项目根目录 [NOTICE](NOTICE)（含各上游项目的完整版权信息与许可文本）

## 上游版权声明（按许可证要求保留）

各上游项目的版权归属、NOTICE 说明及完整许可文本见根目录 [NOTICE](NOTICE)，此处不再重复全文，仅保留义务摘要：

### GPL-3.0（Extinguish / Hail）

以上项目为自由软件：您可以依据自由软件基金会发布的 GNU 通用公共许可证第 3 版（或按其选择任何更高版本）重新分发和/或修改它们。
它们的分发旨在提供有用性保证，但**没有任何担保**，甚至没有适销性或特定用途适用性的默示担保。详情见 GNU 通用公共许可证。

### MIT（Fake-Lock-Screen）

完整 MIT 许可文本（含版权声明）见 [NOTICE](NOTICE)。

### Apache-2.0（Shizuku）

Shizuku 仓库无独立 NOTICE 文件；完整 Apache-2.0 许可文本见 [NOTICE](NOTICE)。

## 合规说明

1. 本项目主许可证为 **GPL-3.0**（见 [LICENSE](LICENSE)）。
2. 若本项目后续被判定为 GPL-3.0 上游项目的衍生作品，依据 GPL-3.0 第 5 节，本项目必须以 GPL-3.0 或兼容许可证整体分发，本声明即为此要求的落实。
3. `hnpdsyn/PhoneGuard` 仓库无 LICENSE 文件：本项目**未复制**其代码，仅参考其公开功能描述与通用无障碍实现思路；若后续需要直接采用其实现，请先联系作者确认许可。
4. `me.lucky/wasted` 仓库当前不可访问，若确认该仓库已不存在或无法取得许可证，本项目将不再将其列为参考来源，并移除对应实现影响。

---
本文件由 lock-escape 项目维护，随项目分发。
