# 测试源码目录

这里是 tiedun 铁盾的单元测试源码入口。

## 测试源码路径

- `app/src/test/java/com/lilining/lockescape/`

## 当前重点测试

- `VolumeKeyTriggerTest.kt`：长按音量下键、连按音量键、重复按键事件过滤。
- `AppSafetyTest.kt`：系统关键包、默认桌面白名单、用户白名单、包名校验。
- `ShellExecutorTest.kt`：Root/Shizuku 执行模式、su 兼容、命令注入拦截。
- `ThreatAssessmentTest.kt`：风险画像与分级。
- `RootLauncherTest.kt`：Root 直启入口默认进入 Root 模式。
- `DaemonServiceTest.kt`：前台服务类型与启动行为。
- `ProtectedTargetsTest.kt` / `RescueTargetTest.kt`：目标解析与有效期。

## 运行测试

```text
gradle testDebugUnitTest
```

## 许可

测试源码同样按 GPL-3.0 分发，路径为 `app/src/test/java/com/lilining/lockescape/`。
