# 护盾架构方案

## 1. 已确认的产品范围

本项目只实现“护盾”能力，不实现参考工具箱中的跳广告、屏幕滤波、清理加速、FPS、游戏库、插件商城或下载器。

第一版定位为一款面向用户自有设备的应用防护工具，核心目标是：当黑名单应用进入前台时，及时识别、提示并按用户确认执行安全动作。

已确认的范围：

- 黑名单与白名单管理
- 前台应用检测
- 护盾警告界面
- 拦截历史记录
- 无障碍权限引导
- 悬浮窗权限引导
- 前台常驻服务
- 开机恢复
- 电池优化引导
- Shizuku 通道
- Root 通道
- Root/特权模式下的双监听状态
- 强制停止等高风险操作的二次确认
- 崩溃记录与状态恢复

所有高风险操作都必须由用户明确触发，不能因为收到单个后台事件就自动卸载或停用应用。

## 2. 总体运行模式

护盾采用三档降级策略：

```text
Root 可用
  无障碍监听 + Root 监听器

Root 不可用、Shizuku 可用
  无障碍监听 + Shizuku 特权通道

Root 和 Shizuku 都不可用
  无障碍监听单通道
```

模式选择由运行时能力探测决定，不把 Root 或 Shizuku 当作必需条件。无障碍服务未授权时，应用只能展示授权引导，不能伪造“正在保护”的状态。

### 2.1 普通模式

使用 Android 公开 API：

- `AccessibilityService`
- `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`
- `AccessibilityEvent.TYPE_WINDOWS_CHANGED`
- `PackageManager`
- `ActivityManager`
- `WindowManager`
- `ForegroundService`

能力：识别前台窗口、匹配黑名单、展示警告、记录事件、返回安全页面。

### 2.2 Shizuku 模式

使用 Shizuku 的公开 API 建立特权操作通道。Shizuku 只负责授权后的固定操作，不暴露任意 shell 输入框。

计划支持：

- 检查 Shizuku 服务状态
- 请求用户授权
- 执行固定格式的 `am force-stop 包名`
- 在设备和权限允许时执行固定格式的应用管理操作
- 返回结构化结果和错误原因

Shizuku 不被视为 Root，也不承诺具备内核级监听能力。若特权通道不可用，自动回退到无障碍模式。

### 2.3 Root 模式

Root 模式启动一个独立的守护进程，不把长期监听逻辑塞进 Activity。守护进程通过受控的 `su` 通道启动，并使用设备允许的系统信息来源进行观察。

第一阶段只规划以下安全边界：

- 读取进程/窗口变化相关信息
- 发送结构化事件给应用主进程
- 接收停止、暂停和退出指令
- 应用退出或用户关闭护盾时停止守护进程
- 不监听键盘输入、不读取用户内容、不上传设备数据

Root 监听不假设所有 Android 版本和 ROM 都有相同接口。无法可靠读取时必须报告“Root 通道不可用”，不能伪造监听成功。具体的进程监听来源将在技术验证阶段按模拟器和目标设备分别确认。

## 3. 双监听与决策层

无障碍服务和 Root/Shizuku 通道都只负责上报事件，最终是否触发护盾由统一决策层判断。

```text
AccessibilityService ─┐
                       ├─ EventNormalizer
Root Monitor ──────────┤
                       └─ ShieldDecisionEngine
Shizuku status ───────┘
                              │
                              ├─ 黑名单匹配
                              ├─ 白名单匹配
                              ├─ 目标有效期检查
                              ├─ 去重与冷却
                              └─ 输出 ShieldAction
```

决策层必须具备：

- 事件去重：同一个包名短时间重复上报只处理一次
- 白名单优先：白名单命中时不拦截
- 系统关键组件保护：默认桌面、输入法、设置、护盾自身和 Shizuku 不作为可操作目标
- 目标有效期：事件过期后不执行停止或停用
- 当前前台复核：高风险动作前再次确认当前包名
- 用户确认：停止、停用和卸载等动作不能只由后台事件触发
- 失败可解释：区分未授权、目标已退出、命令失败、结果未知

## 4. 模块边界

计划按以下模块拆分，避免把所有逻辑放进 `MainActivity`：

```text
com.lilining.lockescape
├─ ui
│  ├─ dashboard       状态总览
│  ├─ targets         黑名单/白名单
│  ├─ history         拦截记录
│  ├─ settings        设置与权限引导
│  └─ warning         护盾警告界面
├─ accessibility
│  └─ ShieldAccessibilityService
├─ privilege
│  ├─ PrivilegeManager
│  ├─ ShizukuChannel
│  └─ RootChannel
├─ monitor
│  ├─ AccessibilityEventSource
│  ├─ RootMonitorDaemon
│  ├─ EventNormalizer
│  └─ ShieldDecisionEngine
├─ runtime
│  ├─ ShieldForegroundService
│  ├─ BootReceiver
│  └─ CrashReporter
├─ data
│  ├─ ShieldPreferences
│  ├─ ShieldDatabase
│  ├─ TargetRepository
│  └─ BlockHistoryRepository
└─ system
   ├─ PackageCatalog
   ├─ PermissionInspector
   └─ SafetyPolicy
```

目录和类名为本项目重新设计，不复制参考 APK 的命名、结构或实现。

## 5. UI 方案

### 5.1 技术

- 全原生 Jetpack Compose
- 不使用 WebView
- 不使用参考 APK 的图片、布局、颜色、文案或动效
- 系统权限页和悬浮层使用 Android 原生 API

### 5.2 视觉方向：工业终端风

设计语言采用原创的“工业终端”方向：

- 背景：深石墨灰，不使用纯黑
- 主文字：冷白色
- 状态色：低饱和青绿色
- 风险色：琥珀色和高亮红色
- 辅助色：灰蓝色
- 字体：系统无衬线字体，关键数据使用等宽数字
- 布局：模块化面板、明显的状态标签、紧凑但可读的间距
- 动效：短促的状态切换和扫描线提示，不使用参考样本的动画形式

主界面规划：

1. **状态总览**：护盾开关、当前运行模式、权限状态、受保护数量
2. **保护目标**：黑名单、白名单、临时放行
3. **事件记录**：按时间查看拦截记录和动作结果
4. **运行设置**：开机恢复、通知、震动、持续压制、监听模式
5. **权限中心**：无障碍、悬浮窗、电池优化、Shizuku、Root 状态

## 6. 数据存储

采用 `DataStore + Room`：

- DataStore：开关、监听模式、通知偏好、临时设置
- Room：黑名单、白名单、应用快照、拦截历史、动作结果

建议的核心数据表：

```text
ProtectedTarget
- id
- packageName
- labelSnapshot
- mode
- enabled
- createdAt
- updatedAt

BlockHistory
- id
- packageName
- labelSnapshot
- eventSource
- action
- result
- occurredAt
- detail
```

数据库只保存实现护盾所需的信息，不保存应用内容、键盘输入或屏幕截图。

## 7. 权限矩阵

| 能力 | Android API/权限 | 是否必需 |
|---|---|---|
| 前台应用识别 | `AccessibilityService` | 核心必需 |
| 警告悬浮层 | `SYSTEM_ALERT_WINDOW` | 需要悬浮警告时必需 |
| 常驻运行 | `FOREGROUND_SERVICE`、通知权限 | 建议开启 |
| 开机恢复 | `RECEIVE_BOOT_COMPLETED` | 可选 |
| 电池优化引导 | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 可选 |
| Root 通道 | 设备已有 Root / `su` | 可选 |
| Shizuku 通道 | Shizuku API | 可选 |
| 应用信息 | `PackageManager` | 核心需要 |
| 强制停止 | Shizuku/root 或受限公开 API | 可选且需确认 |

权限不足时必须展示清楚的状态和原因，不能静默绕过 Android 权限模型。

## 8. 高风险操作策略

默认只启用：

- 检测
- 警告
- 返回安全页面
- 记录

以下功能默认关闭，并要求用户二次确认：

- 强制停止
- 停用应用
- 卸载用户应用
- 持续压制

系统关键组件、默认桌面、输入法、系统设置、Shizuku 和护盾自身进入保护名单，不允许通过护盾执行停用或卸载。

## 9. 崩溃和恢复

计划加入：

- `Thread.setDefaultUncaughtExceptionHandler`
- 本地崩溃报告
- 下次启动显示上次异常摘要
- 前台服务状态恢复
- 数据库事务保护
- Root 守护进程断开检测
- Shizuku 服务断开回退
- 无障碍被关闭时明确提示

崩溃捕获用于记录和恢复，不能保证 Android 进程在崩溃后继续运行。

## 10. 第三方依赖和许可证计划

当前仓库已经使用并登记的依赖：

| 库 | 用途 | 许可证 |
|---|---|---|
| AndroidX Core KTX | Android 基础扩展 | Apache-2.0 |
| AndroidX AppCompat | 兼容组件 | Apache-2.0 |
| Material Components | 原生 Material 组件 | Apache-2.0 |
| ConstraintLayout | 现有布局支持 | Apache-2.0 |
| Shizuku API / Provider | 特权通道 | Apache-2.0 |
| Kotlin Coroutines | 异步任务 | Apache-2.0 |
| JUnit | 测试 | EPL-1.0 |

后续新增 Compose、Room、DataStore 依赖前，会先确认版本和许可证，并更新：

- `docs/DEPENDENCY_LICENSES.md`
- `THIRD_PARTY_NOTICES.md`
- `NOTICE`
- `third_party/<库名>/LICENSE`
- `third_party/<库名>/NOTICE`（上游提供时）

本项目继续使用 GPL-3.0。当前阶段不复制参考 APK 的代码、资源、布局、类名、方法名或视觉素材，也不把许可证不明的代码作为实现来源。

## 11. 实施顺序

1. 技术验证：模拟器上确认无障碍、悬浮窗、前台服务、Shizuku 状态检测
2. 数据层：DataStore、Room、黑名单/白名单/历史记录
3. 无障碍监听：前台包名事件归一化
4. 护盾决策层：匹配、去重、冷却、白名单保护
5. Compose 主界面：原创工业终端风
6. 警告界面和权限引导
7. 前台服务、开机恢复、电池优化
8. Shizuku 固定操作通道
9. Root 守护进程技术验证与可用性检测
10. Root/无障碍双监听融合
11. 崩溃捕获、日志和完整测试

Root 监听器不会在没有经过模拟器和目标设备验证前宣称“内核级全兼容”。
