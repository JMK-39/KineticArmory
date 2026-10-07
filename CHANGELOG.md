## 26.10.6 — 2026-10-06

### English

- The piece bonus list and the set effects overview show each bonus on one clean line. They reused the tooltip text, whose line breaks showed up as a wide gap in the bonus list and joined words together in the overview ("chance to applySlowness"). Saved tips are unchanged.
- The condition editor's title has its own row, so the "Select Predicate Type" label is shown in full instead of being cut to "Select…" beside the title.
- The set editor's effect dialogs (attribute bonus, potion effect, damage and potion immunity, attack effect, damage conversion, attack damage multiplier) and the condition editor are centred in the window both ways. Their height follows their content, so each one sits in the middle whatever its size, instead of some sitting high and some low.
- In the set commands, hover tips and set effects lists, the Conds and Delete buttons sat on the row frame lines. Rows are now taller and their buttons, icons and text are centred, 3 px clear of the frame on every side.
- In the time-range condition, an empty minimum or maximum means "no limit" and is no longer marked red.
- Checked with screenshots of all 31 screens on 1.21.1 and 26.1.2, in English and Chinese, at two window sizes and with extra-long text; both versions look the same. On 1.20.1 the same screens were captured inside an installed modpack at 1920×1080 and 854×480 with an automatic check that no text or button touches a frame line.

### 简体中文

- 套装件数加成列表和套装效果总览中，每条加成显示为一整行。之前它们复用了提示文本，其中的换行在加成列表里显示为一大段空白，在总览中则把单词连在一起（"chance to applySlowness"）。已保存的提示不受影响。
- 条件编辑器的标题单独占一行，"选择谓词类型"标签可以完整显示，不再在标题旁被截成"Select…"。
- 套装编辑器的效果子界面（属性加成、药水效果、伤害与药水免疫、攻击效果、伤害转换、攻击伤害倍率）以及条件编辑器在窗口中水平、垂直居中。界面高度随内容计算，无论大小都位于正中，不再有的偏上、有的偏下。
- 套装指令、悬浮提示和套装效果列表中，"条件"与"删除"按钮压在行边框线上。现在行高增加，按钮、图标和文字垂直居中，四周与边框保持 3 像素。
- 时间范围条件中，最小值或最大值留空表示"不限"，不再标红。
- 已在 1.21.1 与 26.1.2 上对全部 31 个界面截图检查，涵盖英文和中文、两种窗口尺寸以及超长文本；两个版本外观一致。1.20.1 上则在已安装的整合包中以 1920×1080 与 854×480 截图，并自动检查文字和按钮都不碰到边框线。

## 26.10.5 — 2026-10-05

### English

- Saving, renaming or deleting an armor set, or saving the entity filter, answers only the admin who saved; the sets are no longer sent to every online player on each save. Other players receive them when they log in. The reload command and the reload button still refresh everyone when "Sync on reload" is on.
- Enabled Minecraft 26.1.2 / NeoForge 26.1.2.112 (Java 25); releases now cover Forge 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2. Requires Curios 15 on 26.1.2; KubeJS integration compiles against KubeJS 26.1.2-8 but its 26.1 build does not start on 26.1.2 yet.
- Every version uses the same screens. On 1.21.1 and 26.1.2, item component data (`[damage=5]`) is now edited in Core's NBT editor, the same editor Forge uses for NBT, instead of a separate page. Requires KineticCore 26.10.5+.
- Armor sets written for 1.21.1 keep their attribute ids on 26.1.2 (`minecraft:generic.attack_damage` matches `minecraft:attack_damage`) in attribute bonuses, attribute conditions and generated tips.
- On 26.1.2 a set reacts to both kinds of Curios change: a different item and a changed item state. Time conditions read the overworld clock and moon-phase conditions read the moon phase at the entity, the 26.1 sources of both values.
- Verified: all three versions build; the server runtime checks pass on 1.21.1 and 26.1.2; 186 English/Chinese 26.1.2 client captures at 854×480 and 1536×864 match the 1.21.1 layouts.

### 简体中文

- 保存、重命名或删除套装，以及保存实体过滤，只回复保存的管理员，不再每次保存都发送给所有在线玩家；其他玩家在登录时获得。开启"重载时同步"时，重载命令与重载按钮仍会刷新所有人。
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

- Added NeoForge 1.21.1 support alongside Forge 1.20.1, using matching KineticCore 26.10.3+.
- The 1.21.1 set editor and matching rules use native item components (`componentMode` / `components`); legacy item-NBT syntax is rejected and is not converted. Forge retains its NBT rules.
- Adapted attribute/potion bonuses, Curios equipment, and configuration synchronization for 1.21.1.
- Both builds, Forge startup, and targeted NeoForge world/editor checks passed; all set combinations, script callbacks, and multiplayer servers have not been tested.
- The 26.1.2 node is reserved and disabled; it is not a supported release.

- 新增 NeoForge 1.21.1 支持，同时保留 Forge 1.20.1；使用对应版本的 KineticCore 26.10.3+。
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
