# APK 下载说明

## 推荐下载

| 文件 | 版本 | 用途 | 建议 |
|------|------|------|------|
| `推荐下载-2.0正式版/锁机逃生-2.0正式版-推荐下载.apk` | 2.0 正式版 | 普通用户安装 | 推荐下载这个 |
| `推荐下载-2.0正式版/锁机逃生-2.0-SU直接启动-GPL版.apk` | 2.0 SU 直启 GPL 版 | 安装后使用“锁机逃生 SU直启”入口，直接进入 Root/SU 优先逃生控制台 | root 用户推荐 |

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

- `lock-escape-v2.0-official.apk`：2.0 正式版，推荐。
- `lock-escape-v2.0-su-direct-gpl.apk`：2.0 SU 直接启动 GPL 版，安装后使用“锁机逃生 SU直启”入口。
- `lock-escape-v2.0-release.apk`：2.0 release 构建，等同正式版。
- `lock-escape-v2.0-debug.apk`：2.0 调试版，仅开发测试。
- `app-debug.apk`：当前调试版，保留原路径。
- `lock-escape-v0.1.0-debug.apk`：历史调试版，不推荐。

## 源码公开与许可

- 本项目源码随仓库公开，点击可直接查看源码目录：[主源码 app/src/main/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/main/java)，[测试源码 app/src/test/java](https://github.com/lilining308-prog/lock-escape/tree/main/app/src/test/java)。
- 当前开发版本地源码路径：`app/src/main/java/com/lilining/lockescape/`。
- 当前开发版本地测试源码路径：`app/src/test/java/com/lilining/lockescape/`。
- 主许可证为 GPL-3.0，全文见仓库根目录 `LICENSE`。
- 第三方声明见仓库根目录 `THIRD_PARTY_NOTICES.md` 与 `NOTICE`。
- 分发 APK 时应同时提供源码仓库地址、`LICENSE`、`NOTICE`、`THIRD_PARTY_NOTICES.md`。
- 所有原创源码、资源、构建脚本、文档和 APK 分发说明均按 GPL-3.0 随项目公开。
- APK 内置 GPL 与源码公开说明，路径为 `assets/GPL/`。打包内容包括：`LICENSE-GPL-3.0.txt`、`NOTICE.txt`、`THIRD_PARTY_NOTICES.md`、`SOURCE_PATHS.md`、`README.md`、`APK-README.md`。

## APK 声明核查

已核查 `锁机逃生-2.0正式版-推荐下载.apk`：

- 包名：`com.lilining.lockescape`
- 版本：`versionName=2.0`，`versionCode=20`
- 权限声明包含：前台服务、通知、唤醒锁、悬浮窗、忽略电池优化、查询应用列表、Shizuku API 权限。
- 这些权限与锁机逃生功能直接相关：监听触发、前台保活、覆盖逃生界面、定位顶层包、执行 Shizuku/root 逃生动作。
