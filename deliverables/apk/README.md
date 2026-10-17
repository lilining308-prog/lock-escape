# 产物 APK 分类副本

本目录是 `apk/` 分类结果的本地副本，方便打包外发：

| 子目录 | 对应 |
|--------|------|
| `stable/` | [`../../apk/stable/`](../../apk/stable/) |
| `debug/` | [`../../apk/debug/`](../../apk/debug/) |
| `archive/` | [`../../apk/archive/`](../../apk/archive/) |

仓库只跟踪 `apk/` 下的唯一分类包，本目录 `.apk` 为本地副本（见 `.gitignore`），不重复入库。

推荐与 APK 成对的当前交付物：

- [`../tiedun-current-release.apk`](../tiedun-current-release.apk)
- [`../tiedun-current-source.zip`](../tiedun-current-source.zip)
- [`../交付说明.md`](../交付说明.md)
