# 第三方许可材料

本仓库当前实际构建依赖及许可证见：

- `docs/DEPENDENCY_LICENSES.md`
- `THIRD_PARTY_NOTICES.md`
- `NOTICE`

当前实际使用的新增代码依赖仍是仓库原有依赖：AndroidX Core/AppCompat/ConstraintLayout、Material Components、Shizuku API/Provider、Kotlin Coroutines 和 JUnit，许可证清单已在上述文件中登记。

`androidx-compose/` 与 `androidx-room-datastore/` 是下一阶段升级的材料占位目录。本轮由于构建环境无法解析新增依赖，未将 Compose、Room 或 DataStore 打进 APK，也没有把它们写进实际构建依赖图。
