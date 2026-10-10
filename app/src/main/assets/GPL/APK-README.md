# APK 下载说明

**本应用是半成品**，可基于公开源码修改后再构建；本 APK 不代表已完成的最终产品。

**许可核查状态：暂停推荐。** 本文件随 APK 打包，但历史文件名不能证明完整对应源码与第三方依赖许可材料已经逐包核实。以下“推荐”均为历史命名。

## 历史“推荐下载”目录（当前暂停推荐）

| 文件 | 版本 | 用途 | 建议 |
|------|------|------|------|
| `推荐下载-2.0正式版/锁机逃生-2.0正式版-推荐下载.apk` | 2.0 历史构建 | 历史留档 | 暂停推荐 |
| `推荐下载-2.0正式版/锁机逃生-2.0-SU直接启动-GPL版.apk` | 相同 SHA-256 的历史别名 | 历史留档 | 暂停推荐 |

## 开发测试

| 文件 | 版本 | 用途 | 建议 |
|------|------|------|------|
| `调试版-开发测试/锁机逃生-2.0调试版-开发测试.apk` | 2.0 调试版 | ADB 调试、开发测试 | 普通用户不要下 |
| `调试版-开发测试/app-debug-当前调试版.apk` | 当前调试版 | 保留原调试包路径 | 普通用户不要下 |

## 历史版本

| 文件 | 版本 | 用途 | 建议 |
|------|------|------|------|
| `历史版本-不推荐/锁机逃生-0.1.0历史调试版-不推荐.apk` | 0.1.0 调试版 | 历史归档 | 不推荐安装 |

## 根目录 APK 说明

根目录仍保留原文件名，避免旧链接失效：

- `lock-escape-v2.0-official.apk`：2.0 历史构建，暂停推荐。
- `lock-escape-v2.0-su-direct-gpl.apk`：历史正式包的相同字节别名，暂停推荐。
- `lock-escape-v2.0-release.apk`：与历史正式包 SHA-256 相同，非新版构建。
- `lock-escape-v2.0-debug.apk`：2.0 调试版，仅开发测试。
- `app-debug.apk`：历史调试版，保留原路径，并非当前工作区构建。
- `lock-escape-v0.1.0-debug.apk`：历史调试版，不推荐。

## 源码公开与许可

本文件随不同批次 APK 一起打包，文件名/版本号不能用于确定源码版本。下载页应同时列明该 APK 的 SHA-256、对应源码提交或源码压缩包及构建脚本；`main` 链接仅指向当前已提交代码。

- 已提交源码可从 [主源码 app/src/main/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/main/java) 和 [测试源码 app/src/test/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/test/java) 查阅；这些不断变化的 `main` 链接不等于历史 APK 的对应源码版本。
- 当前开发版本地源码路径：`app/src/main/java/com/lilining/lockescape/`。
- 当前开发版本地测试源码路径：`app/src/test/java/com/lilining/lockescape/`。
- 主许可证为 GPL-3.0，全文见仓库根目录 `LICENSE`。
- 第三方声明见仓库根目录 `THIRD_PARTY_NOTICES.md` 与 `NOTICE`。
- 分发 APK 时应同时提供源码仓库地址、`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md`。
- 所有原创源码、资源、构建脚本、文档和 APK 分发说明均按 GPL-3.0 随项目公开。
- 包含本文件的 APK 在 `assets/GPL/` 中内置许可材料，但这不代表所有历史 APK 均已包含，也不代表其中的说明与当前源码一一对应。

## APK 声明核查

已核查 `锁机逃生-2.0正式版-推荐下载.apk`：

- 包名：`com.lilining.lockescape`
- 版本：`versionName=2.0`，`versionCode=20`
- 权限声明包含：前台服务、通知、唤醒锁、悬浮窗、忽略电池优化、查询应用列表、Shizuku API 权限。
- 这些权限与锁机逃生功能直接相关：监听触发、前台保活、覆盖逃生界面、定位顶层包、执行 Shizuku/root 逃生动作。
