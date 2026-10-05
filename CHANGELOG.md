## 26.10.5 — 2026-10-05

### English

- Enabled Minecraft 26.1.2 / NeoForge 26.1.2.112 (Java 25); releases now cover Forge 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2. Requires Curios 15 on 26.1.2; KubeJS integration compiles against KubeJS 26.1.2-8 but its 26.1 build does not start on 26.1.2 yet.
- Every version uses the same screens. On 1.21.1 and 26.1.2, item component data (`[damage=5]`) is now edited in Core's NBT editor, the same editor Forge uses for NBT, instead of a separate page. Requires KineticCore 26.10.5+.
- Armor sets written for 1.21.1 keep their attribute ids on 26.1.2 (`minecraft:generic.attack_damage` matches `minecraft:attack_damage`) in attribute bonuses, attribute conditions and generated tips.
- On 26.1.2 a set reacts to both kinds of Curios change: a different item and a changed item state. Time conditions read the overworld clock and moon-phase conditions read the moon phase at the entity, the 26.1 sources of both values.
- Verified: all three versions build; the server runtime checks pass on 1.21.1 and 26.1.2; 186 English/Chinese 26.1.2 client captures at 854×480 and 1536×864 match the 1.21.1 layouts.

### 简体中文

- 启用 Minecraft 26.1.2 / NeoForge 26.1.2.112（Java 25）；发布版本覆盖 Forge 1.20.1、NeoForge 1.21.1 与 NeoForge 26.1.2。26.1.2 需要 Curios 15；KubeJS 联动按 KubeJS 26.1.2-8 编译，但其 26.1 版本目前无法在 26.1.2 上启动。
- 所有版本使用相同界面。1.21.1 与 26.1.2 的物品数据组件（`[damage=5]`）改用核心 NBT 编辑器编辑，与 Forge 编辑 NBT 的界面一致，不再使用单独页面。要求 KineticCore 26.10.5+。
- 1.21.1 写下的套装在 26.1.2 上保留属性 ID（`minecraft:generic.attack_damage` 对应 `minecraft:attack_damage`），适用于属性加成、属性条件与自动生成的提示。
- 26.1.2 上套装会响应两类饰品变化：更换物品与物品状态改变。时间条件读取主世界时钟，月相条件读取实体所在位置的月相，均为 26.1 中这两个数值的来源。
- 验证：三个版本均可构建；服务端运行时检查在 1.21.1 与 26.1.2 通过；26.1.2 客户端中英文 854×480 与 1536×864 共 186 张截图，与 1.21.1 布局一致。

## 2026-10-05 — Potion list icons / 药水列表图标

### English

- Add leading effect icons and effect IDs to potion, attack-effect and effect-immunity rows in the detail and piece-bonus lists through shared KineticCore APIs.
- Preserve existing controls, row heights, colors and interactions; reserve icon space only within each row's text bounds. Autocomplete popup icons require a shared Core API extension.

### 简体中文

- 通过 KineticCore 公共 API，为详情和件数加成列表中的药水、攻击效果、药水免疫行添加行首效果图标与效果 ID。
- 保留既有控件、行高、颜色与交互，仅在行内文字范围中预留图标空间；自动补全弹窗图标仍需核心公共 API 扩展。

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
