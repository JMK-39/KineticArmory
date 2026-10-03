# KineticArmory 多版本迁移验收（2026-10-03）

## 支持范围

| 节点 | 状态 | Java 字节码 | 产物 |
|---|---|---|---|
| 1.20.1-forge | 构建与现有整合包主菜单验证通过 | 21 / 65 | kineticarmory-forge-1.20.1-26.10.3.jar |
| 1.21.1-neoforge | 构建、实际世界回归及组件编辑器验证通过 | 21 / 65 | kineticarmory-neoforge-1.21.1-26.10.3.jar |
| 26.1.2-neoforge | 仅预留，未启用、未构建、未宣称支持 | 预留 25 | 无 |

当前核心本地输出只有 Forge 1.20.1 和 NeoForge 1.21.1 的 26.10.3，缺少 26.1.2 对应核心。26 节点的 Curios/KubeJS 等依赖也须在启用时重新核实，当前依赖坐标只针对 1.21.1。

## 构建与代码修改

- Gradle 9.8.0、Stonecutter 0.9.8、MDG 2.0.148；删除原根 build.gradle，增加两个加载器脚本和共用节点脚本，根 buildAll 一次构建所有启用节点。
- 保留用户已修改的 Java 21、核心最低版本和强制版本 26.10.3。原先五个构建文件的未提交改动已保存到本地 .gradle/migration/preexisting.patch；未丢弃用户修改。
- 固定模组依赖优先读取 Gradle 用户目录上一级的 libs；核心按加载器、Minecraft 和版本严格匹配，兼容旧名核心仅限 Forge 1.20.1。支持显式版本、联网自动比较官方版本、断网使用本地和错误节点拒绝。
- 节点共享自动日期版本与命名规则，产物输出 D:/NEWMODS；两个工作流改为 Java 21 / Gradle 9.8 / buildAll 和多版本产物。没有执行远程 CI、推送或发布。
- Holder 效果/属性、UUID 对应稳定 ResourceLocation 修饰符、物品组件缓存与 KubeJS 2101 接口采用 Stonecutter 条件代码；Forge 保留原有 UUID、NBT 和事件接口行为。
- 本模组没有 Mixin。最终核心引用、架构、Mixin 目标检查接入 build；没有虚构 refmap 或 MixinConfigs。
- 运行夹具位于独立测试包，仅显式 runtimeValidationJar 任务生成，未进入发布 JAR。

## 各版本物品数据写法

按用户最新要求，不在 NeoForge 上兼容或转换旧物品 NBT。

1.20.1 示例：

```json
{"id":"minecraft:diamond_sword","nbtMode":"WEAK","nbtTag":"{Damage:5}"}
```

1.21.1 示例：

```json
{"id":"minecraft:diamond_sword","componentMode":"WEAK","components":"[damage=5]"}
```

Neo 空组件使用 []。NONE 仅检查物品 ID；WEAK 检查指定组件，custom_data 使用字段子集匹配，附魔等其他组件按整项值比较；STRONG 检查完整组件补丁。显式默认值、组件移除保留为约束，错误写法在匹配模式下拒绝。组件约束缓存随配置和世界注册表上下文刷新。

Neo 新编辑页使用核心多行文本控件并通过原版 ItemParser 校验；不会打开核心的 NBT 编辑器。两个节点的配置写法分开，不自动升级旧配置。Neo 的版本语言覆盖只进入 Neo 产物，Forge 语言资源保持原样。

## 验收结果

- gradlew buildAll :1.21.1-neoforge:runtimeValidationJar --offline：通过。包括两个节点的架构检查、最终核心方法引用检查以及 Mixin 目标检查（0 配置/0 注入）。构建依赖来自已有缓存/本地库，没有单独下载游戏。
- Verify-ReleaseJar.ps1：Forge 104 个类、Neo 106 个类，全部 class version 65；正确加载器 TOML、核心范围 [26.10.3,)、pack JSON、无测试夹具。
- 用 D:/NEWMODS 当前两个核心发布 JAR 再次检查对应附属 JAR，KineticReobfCheck addon 均为 0 problem(s)。强制给 Neo 节点传入 Forge 核心 JAR 的负例已拒绝。
- 迁移前 Forge 101 个类全部保留，只新增 ArmorItemData、ArmorVersionCompat、ArmorItemDataEditor 三个适配辅助类。语言及其他资源一致；元数据差异限于清单、TOML 和 pack.mcmeta。
- 1.21.1 于 16:29:48 在用户既有 PCL2 / NeoForge 21.1.252 世界输出 KINETICARMORY_RUNTIME_VALIDATION_PASS。覆盖组件字段序列化、空默认值、旧 NBT 拒绝、子集/完整匹配、显式默认/移除、名称/附魔组件、编辑数据缓存、启动到世界的注册表缓存刷新、装备组件哈希变化、属性添加/刷新/移除/SET、药水与属性 Holder 条件以及压缩网络配置往返。
- 新字段回归在改动前实测失败 Neo config uses component fields only；改动后同一用例通过。更早的启动缓存失败包含夹具输入额外附魔，属于用例误判，不作为缓存问题的运行红测证据。
- 1.21.1 实际从 F6 模块菜单打开套装列表（网络同步）并创建未保存草稿。组件编辑器对 {Damage:5} 显示错误并禁用保存，对 [custom_data={sample:1}] 接受并写回草稿，模式变为弱匹配；页面按钮/说明使用数据组件术语。没有向服务端保存此草稿，配置目录没有测试套装文件。
- 1.20.1 于 16:35:11 在 PCL2 原设置下进入 OTHERWORLD CLASH 主菜单（Forge 47.4.23）。日志确认 kineticarmory 26.10.3 加载，KubeJS 找到插件并完成 7/7 启动脚本，0 errors / 0 warnings。
- Neo KubeJS 同样找到插件，1/1 启动脚本，0 errors / 0 warnings。没有专门验证套装变化后的用户 JS 回调、多人服务器、完整套装玩法以及所有效果组合。

运行证据保存在本地 .gradle/migration/runtime-evidence/；构建日志在 .gradle/migration/component-build.log。测试客户端正常关闭，没有结束用户游戏进程、修改 PCL 内存或下载独立客户端。

## 本地依赖与安装

Neo Curios 原本缺失，从官方 Modrinth 的 [9.5.1+1.21.1 发布页](https://modrinth.com/mod/curios/version/9.5.1%2B1.21.1) 下载并校验 SHA-512，存入 D:/IDEA_Caches/libs/curios-yohfFbgD.jar，同时安装到既有 Neo 实例。其余编译依赖已在本地。

两个发布 JAR 已放入对应游戏版本 mods。Forge 原 JAR 备份在 OTHERWORLD CLASH/codex-migration-backup/kineticarmory-20261003。Neo 测试夹具已移至 1.21.1-NeoForge_21.1.252/codex-migration-backup/kineticarmory-validation-20261003，mods 中只保留正式产物。

只读代码审查未发现 Critical / Important。整合包其他模组现有的资源、网络和 Mixin 警告不等于 KineticArmory 错误；Neo 实测另外发现 RealmControl CommandsMixin 的回调描述符警告，另行记录处理，不混入本模组验收结果。