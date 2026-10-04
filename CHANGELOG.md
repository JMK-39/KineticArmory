## 26.10.4 — 2026-10-04

### English

- Keep long headings, warnings, parameter labels and list text inside their own regions with shared scrolling APIs. Covers equipment sets, piece bonuses, variants, tips, effects, commands, entity filters and condition/bonus editors.
- Keep titles clear of Type labels and tip action buttons, bonus warnings clear of input captions, and row names clear of value columns or action buttons.
- Reserve icon space in mixed tip rows and keep the drag preview inside the page.
- Require matching KineticCore 26.10.4+. No gameplay, configuration syntax or language keys changed.

### 简体中文

- 装备套装、件数奖励、候选装备、提示、效果、命令、实体过滤及条件/加成编辑器的长标题、警告、参数标签和列表文字通过核心API在各自范围内滚动。
- 标题避开类型标签与提示操作按钮；件数警告避开输入框标签；行名称避开数值列和操作按钮。
- 图文提示行给图标预留空间，拖动预览限制在页面内。
- 要求匹配的KineticCore26.10.4+；未修改玩法、配置语法或语言键。

---

2026年10月04日 — Language key validation / 语言键一致性检查

- Require identical authored English/Chinese keys and string values in source, version overrides and packaged resources; generated formatting keys are rejected during builds.

- 强制检查源码、版本覆盖与最终资源的中英文完整键名一致、值为字符串；构建禁止派生格式语言键。

---

2026年10月04日 — 26.10.4

- Completed the Chinese 1.21.1 component matching labels so both version-specific language files contain the same authored keys.

- 补齐 1.21.1 中文数据组件匹配文案，确保两个版本专用语言文件的人工语言键一致。

---

2026年10月03日 16时42分 — 26.10.3

- Added NeoForge 1.21.1 support alongside Forge 1.20.1, using Java 21 and matching KineticCore 26.10.3+.
- The 1.21.1 set editor and matching rules use native item components (`componentMode` / `components`); legacy item-NBT syntax is rejected and is not converted. Forge retains its NBT rules.
- Adapted attribute/potion bonuses, Curios equipment, and configuration synchronization for 1.21.1.
- Both builds, Forge startup, and targeted NeoForge world/editor checks passed; all set combinations, script callbacks, and multiplayer servers have not been tested.
- The 26.1.2 node is reserved and disabled; it is not a supported release.

- 新增 NeoForge 1.21.1 支持，同时保留 Forge 1.20.1；使用 Java 21 和对应版本的 KineticCore 26.10.3+。
- 1.21.1 套装编辑与匹配使用原生物品组件（`componentMode` / `components`），拒绝且不转换旧物品 NBT 写法；Forge 保留 NBT 规则。
- 适配 1.21.1 属性与药水加成、Curios 装备和配置同步。
- 两个版本构建、Forge 启动及针对性的 NeoForge 世界和编辑器检查通过；未测试所有套装组合、脚本回调和多人服务器。
- 26.1.2 节点仅预留、未启用，不代表已支持。

---

2026年10月02日 13时53分

- Removed 24 genuinely unused imports without changing runtime behavior.
- Enabled addon architecture validation and corrected development-run Mixin refmap remapping for Architectury/KubeJS.
- Full build, final-JAR API verification, and real development-client startup passed. Existing build suppressions were preserved; no source suppressions were added.

- 删除 24 个确实未使用的 import，不改变运行功能。
- 接入附属架构检查，补全开发运行的 Mixin 引用映射，修复 Architectury/KubeJS 启动失败。
- 完整构建、最终 JAR API 检查及真实开发客户端启动验证通过。保留已有 build 抑制，未添加源码抑制。

---

2026年09月29日（原记录未标注小时、分钟）

- Updated the armor set, condition, entity filter, equipment variant, reward, command, and tips editors.
- Improved optional Curios compatibility for armor slot changes.
- Updated compatibility with KineticCore 26.9.29.

- 更新护甲套装、条件、实体筛选、装备变体、奖励、命令和 Tips 编辑器。
- 改进 Curios 可选兼容模组的护甲槽位变更处理。
- 更新对 KineticCore 26.9.29 的兼容。
