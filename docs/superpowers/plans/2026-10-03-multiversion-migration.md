# KineticArmory 多版本迁移

沿用已经验收的 TextStudio / RealmControl 和 KineticCore 架构。用户已授权执行迁移及使用现有 PCL2 版本测试；不修改启动内存，不下载独立游戏，不推送、不发布。

1. 保存用户已有构建修改和 Forge 基线产物，保留最低核心 26.10.3、Java 21 意图。
2. 使用 Gradle 9.8、Stonecutter 0.9.8、MDG 2.0.148，启用 Forge 1.20.1 / NeoForge 1.21.1；26.1.2 预留，匹配核心尚缺。
3. 固定依赖本地优先，核心精确匹配节点；发布名称 kineticarmory-<loader>-<mc>-<version>.jar，输出 D:/NEWMODS。
4. 移植 Holder、属性修饰符 ID、物品组件与 KubeJS 接口，保留 UUID 和套装功能；按用户最新要求，Forge 使用 NBT 字段，Neo 使用组件字段、组件编辑器和提示，不兼容旧物品 NBT。
5. 完整离线构建、最终核心引用与架构检查、JAR 验证及基线资源比较。没有 Mixin，不凭空引入 refmap。
6. 重点验证属性添加/刷新/移除、药水 Holder、Forge NBT / Neo 纯组件物品约束、装备缓存失效和网络配置往返；在已有 PCL2 实例运行。
7. 请求只读代码审查，修复问题，完成验收报告并单独本地提交。
