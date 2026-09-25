# Third-Party Notices

本项目的设计实现参考了以下开源项目。所有上游项目的版权归其各自作者所有。
本声明依据各上游项目许可证要求提供，供本项目使用者与下游分发者遵守。

## 参考项目清单

| 能力块 | 上游项目 | 许可证 | 状态 |
|--------|---------|--------|------|
| 物理按键监听 | [F111111shhh/Extinguish](https://github.com/F111111shhh/Extinguish)（fork 自 [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)） | GPL-3.0 | 已核实（GitHub API） |
| 全屏覆盖 | [coderlidy/Fake-Lock-Screen](https://github.com/coderlidy/Fake-Lock-Screen) | MIT | 已核实（GitHub API） |
| root 权限通道 | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) | Apache-2.0 | 已核实（以依赖形式引入） |
| 停用/冻结实现 | [aistra0528/Hail](https://github.com/aistra0528/Hail) | GPL-3.0 | 已核实（GitHub API） |

## 未采用 / 合规排除来源

以下项目不作为本项目实现依据，不复制、不改写、不复用源码；仅作为历史调研记录保留，避免后续误用：

| 项目 | 风险原因 | 当前处理 |
|------|----------|----------|
| [hnpdsyn/PhoneGuard](https://github.com/hnpdsyn/PhoneGuard) | 仓库无 LICENSE 文件，无法确认复制、修改、再分发授权 | 已确认排除；已从参考实现清单移除，不得直接采用其实现 |
| [me.lucky/wasted](https://github.com/me.lucky/wasted) | 仓库当前无法访问（HTTP 404），许可证无法核实 | 已确认排除；已从参考实现清单移除，不得直接采用其实现 |

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
3. `hnpdsyn/PhoneGuard` 仓库无 LICENSE 文件：本项目已确认排除，**不得复制、改写或直接复用**其代码。
4. `me.lucky/wasted` 仓库当前不可访问且许可证无法核实：本项目已确认排除，**不得复制、改写或直接复用**其代码。
5. 当前实现基于 Android SDK 公开 API、Shizuku 依赖和本项目原创 GPL-3.0 代码；若后续发现任何来自未授权来源的复制实现，必须删除并以原创实现替换。

---
本文件由 lock-escape 项目维护，随项目分发。
