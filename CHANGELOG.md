## 26.10.26 — 2026-10-26
- Saving the set list says whether anything was saved.
- 套装列表点击保存后会提示是否有修改被保存。
- 套装生效实体页的过滤模式按钮缩短，搜索框加长；各效果编辑器的“编辑触发条件”按钮缩短为文字所需宽度。

## 26.10.24 — 2026-10-24
- Tip editor: Back, the colour squares with a Reset (§r) button and Add/Save in one header row; no second Back while editing (Back ends the edit); long tips scroll only while hovered. The attack-effect line no longer colours the bracket after the effect name. The armor edit slots use the standard size.
- 文本提示编辑：返回、颜色方块与重置（§r）、添加/保存同在顶栏；编辑时不再出现第二个返回（返回即结束编辑）；长提示仅悬停时滚动。攻击特效一行中效果名后的括号不再被染色。套装编辑物品格改为标准尺寸。

## 26.10.20 — 2026-10-20

- The delete confirmation for an armour set says Back instead of Cancel.
- 删除套装的确认弹窗中“取消”改为“返回”。

- The set-bonus page's top-left Done and the tip editor's Cancel read Back.
- 套装加成页左上角的“完成”与提示编辑器的“取消”改为“返回”。
- The condition list's Add Condition button is wide enough for its label.
- 条件列表的“添加条件”按钮加宽，文字完整显示。
- Equipment and effect editors use one white outer frame, with borderless content regions and gray-white dividers between lists. Item cards, controls, selection outlines and dialogs retain their own interaction states.
- 装备与效果编辑器统一保留一层白色外框，内容区改为无边框底色，列表之间使用灰白分隔线；物品卡片、控件、选中描边及弹窗保留各自的交互状态。
- Set rows open their editor only on left click. Enter and keypad Enter are reserved for active confirmation dialogs, and row tooltips describe the actual controls.
- 套装行仅通过左键进入编辑，回车与小键盘回车仅用于已有二次确认框，悬浮提示与实际操作保持一致。
- Partially visible equipment-variant rows remain clickable during smooth scrolling; their buttons use the same content clip. Piece-bonus lists meet the real content bottom and no longer save values on Enter.
- 装备候选列表平滑滚动时露出的半行仍可点击，行内按钮使用相同的内容裁剪范围；件数加成列表按实际内容末端停止，回车不再直接保存数值。

## 26.10.16 — 2026-10-16
- Entity filtering uses one Core entity grid with standard 50-pixel cards and two-pixel gaps. Selected entities stay green; global whitelist/blacklist and per-set enable controls remain in the top toolbar. Tag, mod and unknown entity rules can be managed from the Rules menu.
- Effect overview actions share one top row, leaving the full width for effect rows and their scrollbar. Effect, command, tip and predicate editors keep their actions in compact headers.
- Potion and effect-immunity selectors are wider so icons, IDs and translated names have more room.

- 实体过滤改为一个 Core 实体网格，使用标准 50 像素卡片与 2 像素间距，已选实体保持绿色；顶部保留全局白黑名单模式和套装启用开关，标签、模组及未知实体规则可在“规则”菜单管理。
- 效果总览的操作集中到顶部同一行，内容区向两侧扩宽并为滚动条留出空间；效果、指令、提示和谓词编辑器的操作均移至紧凑顶栏。
- 药水与药水免疫选择列表加宽，为图标、ID 和本地化名称提供更多空间。

- Large set editors and browsers use the full 640 by 360 canvas with four-pixel insets. Search fields are wider, equipment and accessory previews have more room, and hovering them uses the original item tooltip with its stack data.
- Set rows open deletion confirmation on right click instead of reserving an inline Delete button. Delete follows the page's selected-entry operation, and Enter confirms Core dialogs while text fields retain normal editing.
- 大型套装编辑与浏览页统一使用 640×360 画布和四像素边距；搜索框加宽，装备与饰品预览扩展空间，悬停显示保留原物品数据的原生提示。
- 套装行改为右键弹出删除确认，不再占用右侧删除按钮的位置；Delete 复用页面已有删除操作，Enter 确认核心弹窗，输入框仍按正常文本编辑处理。

## 2026-10-14 — Variant badge and entity cards / 变体标记与生物卡片

- An equipment slot that accepts several items shows a gold # in its upper-left corner, in the set list and the set editor, instead of a green + in the corner.
- The entity rule lists use the same model card size as the other mob lists and show six columns and five rows. Headers, search fields, grids and scrollbars share one margin on both sides of each panel.
- The Accessory and Rejected Accessory sections of the set editor scroll when a set lists more accessories than their two rows hold; one empty slot always follows the list for adding another.
- 可接受多种物品的装备槽位在套装列表与编辑页的左上角显示金色 #，不再在角落显示绿色 +。
- 实体规则列表的生物模型卡片与其他生物列表大小一致，每屏 6 列 5 行；标题、搜索框、网格与滚动条在面板两侧使用相同边距。
- 套装编辑页的饰品与排除饰品区域在超出两行时可以滚动浏览，列表末尾始终保留一个空槽位用于继续添加。

---

## 2026-10-13 — Editor layout fixes / 编辑器排版修复

- Dragging a hover tip keeps the row over the list it reorders, with an opaque background, and the insertion line starts at the row edge.
- The set command editor frames its header and Save button, and its title lines up with Go Back.
- The entity rule search fields are labelled for the list they filter: added mobs on the left, available mobs on the right.
- The set list keeps a two-pixel gap to the window frame.
- Generated hover tips no longer wrap a few leftover characters, such as a closing bracket, onto a line of their own.
- 拖动悬停提示时，被拖动的行停留在所排序的列表上方并使用不透明背景，插入线从行边缘开始。
- 套装指令编辑页的顶部与保存按钮位于边框内，标题与返回按钮对齐。
- 实体规则的搜索框提示与所筛选的列表对应：左侧为已添加生物，右侧为可添加生物。
- 套装列表与窗口边框保持两像素间距。
- 自动生成的悬停提示不再把右括号等少量剩余字符单独换成一行。

---

## 2026-10-11 — Item preview size / 物品预览大小

- Set editors and set-list equipment previews render item icons at their normal 1.0 scale.
- 套装编辑器及套装列表的装备预览统一使用 1.0 大小的物品图标。

---

## 2026-10-10 — Editor navigation / 编辑器返回位置

### English

- The set list, set editor, equipment variants, piece bonuses, tips, commands, effects, entity filters, and condition dialogs now place their return control at the upper left.
- Titles and instructions scroll within the header space beside navigation. Equipment grids, lists, input rows, and save actions keep their existing layout and behavior.
- Back labels use white text in English and Chinese, consistent with the other return controls.
- Selecting a piece bonus or piece-count row changes its frame without changing the label's white text.

### 简体中文

- 套装列表、套装编辑器、装备候选、件数加成、提示、命令、效果、实体过滤与条件界面的返回控件统一放在左上角。
- 标题与说明在返回按钮旁的顶部区域内滚动；装备网格、列表、输入行及保存操作保留既有布局与行为。
- 英文与中文的返回按钮统一使用白色文字，与其他返回控件保持一致。
- 件数加成与件数列表选中时通过边框区分，名称文字保持白色。

## 2026-10-08 — Item preview slots / 物品预览格

### English

- Equipment and accessory previews in the set list, small item icons inside detail and tip rows, and item previews in hover tips all use the standard item-slot background. Slots stay clear of neighbouring icons and controls, and inline icons have their own space beside the tip text.
- Set equipment icons and their matching-mode badges stay inside their slots without changing the equipment grid layout. Colored and bold text continues correctly after inline item icons in the set details.

### 简体中文

- 套装列表中的装备与饰品预览、详情和提示行中的小物品图标，以及悬浮提示中的物品预览统一使用物品格背景。格子避开相邻图标和控件，行内图标在提示文字旁有独立空间。
- 套装装备图标及其匹配模式标记保持在格子内部，装备网格布局保持不变；套装详情中，行内物品图标后的颜色和粗体文字正确延续。

## 26.10.6 — 2026-10-06

### English

- The piece bonus list and the set effects overview show each bonus on one clean line. They reused the tooltip text, whose line breaks showed up as a wide gap in the bonus list and joined words together in the overview ("chance to applySlowness"). Saved tips are unchanged.
- The condition editor's title has its own row, so the "Select Predicate Type" label is shown in full instead of being cut to "Select…" beside the title.
- The set editor's effect dialogs (attribute bonus, potion effect, damage and potion immunity, attack effect, damage conversion, attack damage multiplier) and the condition editor are centred in the window both ways. Their height follows their content, so each one sits in the middle whatever its size, instead of some sitting high and some low.
- In the set commands, hover tips and set effects lists, the Conds and Delete buttons sat on the row frame lines. Rows are now taller and their buttons, icons and text are centred, 3 px clear of the frame on every side.
- In the time-range condition, an empty minimum or maximum means "no limit" and is no longer marked red.

### 简体中文

- 套装件数加成列表和套装效果总览中，每条加成显示为一整行。之前它们复用了提示文本，其中的换行在加成列表里显示为一大段空白，在总览中则把单词连在一起（"chance to applySlowness"）。已保存的提示不受影响。
- 条件编辑器的标题单独占一行，"选择谓词类型"标签可以完整显示，不再在标题旁被截成"Select…"。
- 套装编辑器的效果子界面（属性加成、药水效果、伤害与药水免疫、攻击效果、伤害转换、攻击伤害倍率）以及条件编辑器在窗口中水平、垂直居中。界面高度随内容计算，无论大小都位于正中，不再有的偏上、有的偏下。
- 套装指令、悬浮提示和套装效果列表中，"条件"与"删除"按钮压在行边框线上。现在行高增加，按钮、图标和文字垂直居中，四周与边框保持 3 像素。
- 时间范围条件中，最小值或最大值留空表示"不限"，不再标红。

## 26.10.5 — 2026-10-05

### English

- Saving, renaming or deleting an armor set, or saving the entity filter, answers only the admin who saved; the sets are no longer sent to every online player on each save. Other players receive them when they log in. The reload command and the reload button still refresh everyone when "Sync on reload" is on.
- Added Minecraft 26.1.2 / NeoForge 26.1.2.112 support (Java 25); releases now cover Forge 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2. Requires Curios 15 on 26.1.2; the current KubeJS 26.1 release cannot start on 26.1.2.
- Every version uses the same screens. On 1.21.1 and 26.1.2, item component data (`[damage=5]`) is now edited in Core's NBT editor, the same editor Forge uses for NBT, instead of a separate page. Requires KineticCore 26.10.5+.
- Armor sets written for 1.21.1 keep their attribute ids on 26.1.2 (`minecraft:generic.attack_damage` matches `minecraft:attack_damage`) in attribute bonuses, attribute conditions and generated tips.
- On 26.1.2 a set reacts to both kinds of Curios change: a different item and a changed item state. Time conditions use the overworld clock and moon-phase conditions use the moon phase at the entity.

### 简体中文

- 保存、重命名或删除套装，以及保存实体过滤，只回复保存的管理员，不再每次保存都发送给所有在线玩家；其他玩家在登录时获得。开启"重载时同步"时，重载命令与重载按钮仍会刷新所有人。
- 新增 Minecraft 26.1.2 / NeoForge 26.1.2.112 支持（Java 25）；发布版本覆盖 Forge 1.20.1、NeoForge 1.21.1 与 NeoForge 26.1.2。26.1.2 需要 Curios 15；KubeJS 当前的 26.1 版本无法在 26.1.2 上启动。
- 所有版本使用相同界面。1.21.1 与 26.1.2 的物品数据组件（`[damage=5]`）改用核心 NBT 编辑器编辑，与 Forge 编辑 NBT 的界面一致，不再使用单独页面。要求 KineticCore 26.10.5+。
- 1.21.1 写下的套装在 26.1.2 上保留属性 ID（`minecraft:generic.attack_damage` 对应 `minecraft:attack_damage`），适用于属性加成、属性条件与自动生成的提示。
- 26.1.2 上套装会响应两类饰品变化：更换物品与物品状态改变。时间条件使用主世界时钟，月相条件使用实体所在位置的月相。

## 2026-10-05 — Potion list icons / 药水列表图标

### English

- Add leading effect icons and effect IDs to potion, attack-effect and effect-immunity rows in the detail and piece-bonus lists.
- Icons have their own space within each row, preserving the existing controls, row heights, colors and interactions.

### 简体中文

- 为详情和件数加成列表中的药水、攻击效果、药水免疫行添加行首效果图标与效果 ID。
- 行内为图标预留空间，保留既有控件、行高、颜色与交互。

## 26.10.4 — 2026-10-04

### English

- Keep long headings, warnings, parameter labels and list text scrolling inside their own regions. Covers equipment sets, piece bonuses, variants, tips, effects, commands, entity filters and condition/bonus editors.
- Keep titles clear of Type labels and tip action buttons, bonus warnings clear of input captions, and row names clear of value columns or action buttons.
- Reserve icon space in mixed tip rows and keep the drag preview inside the page.
- Requires matching KineticCore 26.10.4+.

### 简体中文

- 装备套装、件数奖励、候选装备、提示、效果、命令、实体过滤及条件/加成编辑器的长标题、警告、参数标签和列表文字在各自范围内滚动。
- 标题避开类型标签与提示操作按钮；件数警告避开输入框标签；行名称避开数值列和操作按钮。
- 图文提示行给图标预留空间，拖动预览限制在页面内。
- 要求匹配的 KineticCore 26.10.4+。

---

2026年10月04日 — 26.10.4

- Added missing Chinese labels for 1.21.1 item-component matching.

- 补齐 1.21.1 数据组件匹配缺失的中文文案。

---

2026年10月03日 16时42分 — 26.10.3

- Added NeoForge 1.21.1 support alongside Forge 1.20.1, using matching KineticCore 26.10.3+.
- The 1.21.1 set editor and matching rules use native item components (`componentMode` / `components`); legacy item-NBT syntax is rejected and is not converted. Forge retains its NBT rules.
- Adapted attribute/potion bonuses, Curios equipment, and configuration synchronization for 1.21.1.
- Minecraft 26.1.2 is not yet supported.

- 新增 NeoForge 1.21.1 支持，同时保留 Forge 1.20.1；使用对应版本的 KineticCore 26.10.3+。
- 1.21.1 套装编辑与匹配使用原生物品组件（`componentMode` / `components`），拒绝且不转换旧物品 NBT 写法；Forge 保留 NBT 规则。
- 适配 1.21.1 属性与药水加成、Curios 装备和配置同步。
- 尚不支持 Minecraft 26.1.2。

---

2026年09月29日（原记录未标注小时、分钟）

- Updated the armor set, condition, entity filter, equipment variant, reward, command, and tips editors.
- Improved optional Curios compatibility for armor slot changes.
- Updated compatibility with KineticCore 26.9.29.

- 更新护甲套装、条件、实体筛选、装备变体、奖励、命令和 Tips 编辑器。
- 改进 Curios 可选兼容模组的护甲槽位变更处理。
- 更新对 KineticCore 26.9.29 的兼容。
