# APK 分类下载

**本应用是半成品**，欢迎基于源码修改扩展（GPL-3.0）。下列安装包按用途分类，便于查找。

## 目录结构

| 目录 | 用途 | 适合谁 |
|------|------|--------|
| [`stable/`](stable/) | 2.0 正式构建（推荐下载） | 普通用户试用 |
| [`debug/`](debug/) | 调试构建 | 开发者 / 测试 |
| [`archive/`](archive/) | 历史版本 | 仅归档，不推荐安装 |
| [`_legacy-original-layout/`](_legacy-original-layout/) | 旧文件名与重复别名 | 保留原路径兼容，不建议新下载 |

## 推荐下载（stable）

| 文件 | 版本 | 说明 |
|------|------|------|
| [`stable/lock-escape-2.0-release.apk`](stable/lock-escape-2.0-release.apk) | 2.0 | 正式构建，含 `assets/GPL/` 许可材料 |

- 包名：`com.lilining.lockescape`
- 版本：`versionName=2.0`，`versionCode=20`
- SHA-256：`3AB9F8BD63A6A936CE513C13F6C648CE866F061659F94F5DDB9E5F045EE86A45`

> 历史包内置 NOTICE 可能含旧表述；分发时请随附仓库根目录更正后的 `NOTICE`、`LICENSE`、`THIRD_PARTY_NOTICES.md` 与对应源码。

## 开发调试（debug）

| 文件 | 版本 | 说明 |
|------|------|------|
| [`debug/lock-escape-2.0-debug.apk`](debug/lock-escape-2.0-debug.apk) | 2.0 debug | 开发测试用 |
| [`debug/app-debug-historical.apk`](debug/app-debug-historical.apk) | 历史 debug | 旧调试包，非当前工作区构建 |

普通用户请勿安装 debug 包。

## 历史归档（archive）

| 文件 | 版本 | 说明 |
|------|------|------|
| [`archive/lock-escape-0.1.0-debug.apk`](archive/lock-escape-0.1.0-debug.apk) | 0.1.0 debug | 历史归档，缺内置 GPL 材料，不推荐安装 |

## 旧布局（`_legacy-original-layout/`）

保留原先根目录散落文件与中文分类文件夹，避免旧链接失效。其中多个文件与上表 **SHA-256 完全相同**（纯别名）：

| 旧文件 | 与之相同的分类版 |
|--------|------------------|
| `lock-escape-v2.0-official.apk` / `lock-escape-v2.0-release.apk` / `lock-escape-v2.0-su-direct-gpl.apk` | `stable/lock-escape-2.0-release.apk` |
| `lock-escape-v2.0-debug.apk` | `debug/lock-escape-2.0-debug.apk` |
| `app-debug.apk` | `debug/app-debug-historical.apk` |
| `lock-escape-v0.1.0-debug.apk` | `archive/lock-escape-0.1.0-debug.apk` |

完整 SHA-256 与源码溯源见 [docs/APK源码对应表.md](../docs/APK源码对应表.md)。

## 完整源码包（推荐与 APK 一起取）

本地交付目录还有当前构建配对包：

- [`../deliverables/lock-escape-current-release.apk`](../deliverables/lock-escape-current-release.apk)
- [`../deliverables/lock-escape-current-source.zip`](../deliverables/lock-escape-current-source.zip)

配对 SHA-256 见 [`../deliverables/交付说明.md`](../deliverables/交付说明.md)。

## 分发时请一并提供

1. 与该 APK 对应的完整源码（或固定 Git 提交）
2. `LICENSE`（GPL-3.0）
3. `NOTICE`、`THIRD_PARTY_NOTICES.md`
4. `docs/DEPENDENCY_LICENSES.md`

## 本地分类副本

`../deliverables/apk/` 下另有 `stable` / `debug` / `archive` 三类副本，与本目录分类一致，方便打包外发。
