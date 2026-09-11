# KineticArmory

[English](#english) | [简体中文](#简体中文)

## English

**Project separation:** This mod is an independent gameplay add-on built on KineticCore. KineticCore is the shared base API and infrastructure layer; the actual gameplay content and feature implementation described in this README are provided by this project.

This add-on cannot be safely merged into KineticCore or another Kinetic project. It has its own feature scope, dependencies, configuration, release cycle, and user audience, so combining them would couple unrelated gameplay systems and make installation and maintenance less flexible.

**Required dependency:** KineticCore.

### Overview

**KineticArmory** is the Kinetic armor-set and dynamic-condition mod for modpack authors and server administrators. It provides a complete workflow for armor-set definitions, conditional evaluation, effect execution, visual editing, and player-facing tooltips while using KineticCore's configuration, networking, and GUI APIs.

### Key Features

- Visual armor-set editor.
- Multiple equipment variants per slot with NBT-aware matching.
- Piece-count bonus tiers.
- Dynamic predicates with ANY / ALL / MIN / NOT logic.
- Attributes, potion effects, attack modifiers, immunities and damage conversion.
- Commands triggered by armor-set activation/deactivation.
- Global and per-set entity filtering.
- Armor-set support for non-player living entities.
- Auto-generated tooltips based on the real server configuration.
- Server-authoritative armor evaluation and configuration persistence.
- Optional KubeJS armor-set change events.

### Configuration

```text
config/kineticcore/armorsets.toml
config/kineticcore/armorsets_client.toml
config/kineticcore/armorsets/
```

### Requirements

- Java 17
- KineticCore: required
- Curios: required
- KubeJS: optional

## Feature Reference
### Config Details
| Item | Description |
|---|---|
| **Armor Set Settings** | Server-owned global armor set behavior. This page is available only for the local installation or an integrated server. |
| **general** | Controls armor-set detection, potion refresh frequency, and reload synchronization. |
| **Armor Set Client Preferences** | Local tooltip preferences for this client. These settings do not modify the connected server. |
| **Armor Set Visual Editor** | Manage armor-set JSON files and entity rules through the authenticated server editor. |
| **Open Armor Set Editor** | Requests the current server snapshot and opens the visual editor. Operator permission level 2 is required. |
| **Enable Armor Sets** | Enable or disable custom armor set detection and bonus logic. |
| **Check Interval (Seconds)** | Time between equipment-state checks, with a minimum of 0.05 seconds. |
| **Force Sync** | Whether to sync data to all online players when admin reloads. |
| **Tip Shortcut Key** | Default key held to show armor-set details: shift, ctrl, alt, or none. |

### GUI and Editors
| Item | Description |
|---|---|
| **Edit Set Commands** | Tip: Start with / to execute command silently, without / to send as player chat |
| **mode** | Whitelist: only entities added on the left proceed to armor set checks.<br>Blacklist: entities added on the left skip armor set checks. |
| **set toggle** | When enabled, this armor set only works for mobs added on the left.<br>When disabled, entity type is unrestricted. |
| **Entity Rules** | Global Armor Set Entity Rules<br>Current mode: %s<br>Clicking first reads the current server config, then opens the entity menu.<br>Players are always allowed and bypass the global entity filter. |
| **Blacklist Mode** | Filter Mode<br>Blacklist: entities on the left cannot activate armor set effects; all other entities are allowed.<br>Players are always allowed and ignore this list.<br>Left Click: switch to Whitelist mode.<br>Empty blacklist: no non-player entities are restricted. |
| **Whitelist Mode** | Filter Mode<br>Whitelist: only entities on the left can activate armor set effects.<br>Players are always allowed and do not need to be listed.<br>Left Click: switch to Blacklist mode.<br>Empty whitelist: blocks all non-player entities from activating armor sets. |
| **enabled** | Per-Set Entity Limit<br>Enabled: this armor set only works for entities listed on the left.<br>Players must still satisfy this set's player restriction and equipment requirements.<br>Left Click: disable this set's entity limit.<br>Empty list: this set will not work for any non-player entity. |
| **disabled** | Per-Set Entity Limit<br>Disabled: this set does not perform an extra per-set entity-list check.<br>It is still restricted by the master switch and global entity rules.<br>Left Click: enable the limit and allow only entities listed on the left. |
| **save global** | Save Global Entity Rules<br>Saves the current filter mode and entity list to the server config.<br>The filter cache is rebuilt and synchronized immediately. |
| **save set** | Save Set Entity Rules<br>Saves this set's entity-limit switch and allowed entity list.<br>Per-set entity lists always use whitelist semantics. |
| **back** | Back<br>Does not save unsaved changes made on this screen. |
| **Remove All** | Remove Current Results<br>Removes only entities in the left panel's current search results.<br>Entities not matched by the current search are unaffected. |
| **Add All** | Add Current Results<br>Adds every entity in the right panel's current search results to the rule list.<br>Useful with name, ID, @mod, or #tag searches for bulk selection. |
| **Active Entities** | Armor Set Active Entities<br>Choose which entities this armor set can affect.<br>When entity limiting is enabled, only entities on the left can use this set's effects.<br>When disabled, no extra entity-type restriction is applied, but the master switch and global rules still apply. |
| **Rotation Speed** | Enter the model hover rotation speed percentage, from 0%-500%. 0% disables rotation. |
| **direction** | Click to switch the entity model rotation direction. The numeric field still controls rotation speed from 0%-500%. |
| **mode** | Click to toggle logical combinations:<br>[Match ANY] Activates if at least 1 condition is met.<br>[Match ALL] Activates only when ALL conditions are met.<br>[Match MIN X] Activates when met conditions reach X.<br><br>💡 Advanced: Creating 'Disable Rules' with Inversions<br>Combine with the [NOT] button for logical inversion!<br>E.g. If you want: "Disable flight when Raining AND in Water"<br>1. Set Mode to: [Match ANY]<br>2. Add Condition: [NOT] Raining<br>3. Add Condition: [NOT] In Water<br>(De Morgan's Laws: Flight stays active as long as it's NOT raining OR you are NOT in water) |

### Editable Options
- Curio
- Curio Slots (Extended)
- Rejected Curios (Blocks set if worn)
- Rejected Curio Slot
- Helmet Slot
- Chestplate Slot
- Leggings Slot
- Boots Slot
- Main Hand
- Off Hand
- Match MIN %s
- Match MIN
- Whitelist Mode
- Blacklist Mode
- Blacklist
- Whitelist
- Filter Mode: Blacklist
- Filter Mode: Whitelist
- Match ALL
- Match ANY
- Attribute Range
- Clear Weather
- Climbing
- Daytime
- In Dimension
- Exp Level Range
- Falling
- Food Range
- Health Range
- In Air
- In Water
- Moon Phase
- Left Mouse Click
- Left Mouse Hold
- Right Mouse Click
- Right Mouse Hold
- Nighttime
- On Block
- On Fire
- Potion Level Range
- Raining
- Rising
- Sneaking
- Speed Range
- Game Stage
- Thundering
- Time Range
- %s %s
- Not selected

### Config Defaults
| Key | Default |
|---|---|
| `armorsets.defaultTipKey` | `"shift"` |
| `armorsets.enableSets` | `true` |
| `armorsets.entityFilterMode` | `"BLACKLIST"` |
| `armorsets.potionRefreshInterval` | `20` |
| `armorsets.syncOnReload` | `true` |

### Data Paths
Primary configuration/data paths:

- `config/kineticcore/armorsets.toml`
- `config/kineticcore/armorsets/`
- `config/kineticcore/armorsets_client.toml`

### Dependencies
| Mod ID | Relationship |
|---|---|
| `kineticcore` | Required |
| `curios` | Required |
| `kubejs` | Optional |

## 简体中文

**项目独立性：** 本模组是基于 KineticCore 开发的独立玩法附属。KineticCore 是整个系列共用的基础 API 与底层设施；本 README 所述的实际玩法内容和功能实现均由本项目提供。

本附属不能简单合并进 KineticCore 或其他 Kinetic 项目。它拥有独立的功能范围、依赖、配置、更新周期和适用玩家，强行合并会让互不相关的玩法系统彼此耦合，也会降低安装与维护的灵活性。

### 模组定位

**KineticArmory** 是 Kinetic 系列的装备套装与动态条件模组，面向整合包作者和服务器管理员提供完整的套装定义、条件判定、效果执行、可视化编辑与玩家提示体系。它使用 KineticCore 提供的配置中心、网络和 GUI API，并由服务端负责最终规则判定与配置保存。

### 主要功能

- **可视化套装编辑器**：创建、修改、复制和删除套装，不需要手写完整 JSON。
- **装备槽位与装备变种**：同一部位可配置多个合法装备，并支持 NBT 条件与不同匹配方式。
- **件数档位奖励**：可按 1 件、2 件、3 件、全套等不同件数设置独立效果。
- **动态条件系统**：支持 ANY、ALL、MIN、NOT 等组合逻辑，让属性和效果根据环境、状态、维度等条件动态生效。
- **属性与效果编辑**：支持属性加成、药水效果、攻击伤害、攻击附加效果、免疫、伤害转换等多类套装能力。
- **套装命令**：套装激活或失效时可执行配置好的命令。
- **排斥与限制**：支持玩家专用、实体类型过滤、全局实体规则与单套装实体白名单等多层判定。
- **非玩家实体套装**：允许怪物、女仆、测试假人等 `LivingEntity` 在满足规则时获得套装效果。
- **Auto Tips 自动说明**：根据真实套装配置生成 Tooltip，自动展示装备要求、件数、属性、药水、条件、空槽要求等信息。
- **服务端权威判定**：客户端负责 GUI 和 Tooltip，服务端负责最终套装激活、效果执行与配置保存。
- **KubeJS 兼容**：可选支持套装状态变化事件，供脚本监听联动。

### 配置目录

```text
config/kineticcore/armorsets.toml
config/kineticcore/armorsets_client.toml
config/kineticcore/armorsets/
```

- `armorsets.toml`：套装系统全局设置。
- `armorsets_client.toml`：客户端提示键等本地偏好。
- `armorsets/`：每套装备的独立 JSON 数据。

### 使用建议

优先从 `F6` 打开 KineticArmory 页面，再进入套装列表与专用编辑器。服务端规则由服务器保存；连接服务器时只有具备相应管理权限的玩家才能提交服务端配置修改。

### 运行环境

- Java 17
- KineticCore：必须
- Curios：必须
- KubeJS：可选

## 完整功能参考

### 配置项详细说明

| 项目 | 说明 |
|---|---|
| **套装系统设置** | 由服务端管理的套装全局行为。仅可为本机安装或当前单人服务器编辑。 |
| **general** | 控制套装检测、药水刷新频率和重载同步。 |
| **套装客户端偏好** | 当前客户端的本地物品提示偏好，不会修改所连接的服务器。 |
| **套装可视化编辑器** | 通过带服务端鉴权的编辑器管理套装 JSON 和实体规则。 |
| **打开套装编辑器** | 先读取当前服务器快照，再打开可视化编辑器；需要 2 级管理员权限。 |
| **启用套装系统** | 是否开启自定义套装检测与加成逻辑。 |
| **检查间隔（秒）** | 玩家装备状态检查的时间间隔，最小 0.05 秒。 |
| **强制同步** | 当管理员重载配置时，是否同步数据给所有在线玩家。 |
| **提示快捷键** | 查看套装详情时默认按住的按键：shift、ctrl、alt 或 none。 |

### 界面操作与编辑器说明

| 项目 | 说明 |
|---|---|
| **编辑套装指令** | 提示: 带 / 为静默执行指令，不带 / 为玩家发送聊天消息 |
| **mode** | 白名单：只有左侧已添加实体允许进入套装判断。<br>黑名单：左侧已添加实体直接跳过套装判断。 |
| **set toggle** | 开启后，这个套装只对左侧已添加的生物生效。<br>关闭后不限制生物类型。 |
| **实体规则** | 全局套装实体规则<br>当前模式：%s<br>点击后会先读取服务器当前配置，再打开实体菜单。<br>玩家始终允许，不受全局实体过滤影响。 |
| **黑名单模式** | 过滤模式<br>黑名单：左侧名单中的生物无法触发套装效果，其余生物允许。<br>玩家始终允许，不受此名单影响。<br>左键：切换为白名单模式。<br>空黑名单：不限制任何非玩家实体。 |
| **白名单模式** | 过滤模式<br>白名单：只有左侧名单中的生物可以触发套装效果。<br>玩家始终允许，不需要加入名单。<br>左键：切换为黑名单模式。<br>空白名单：会禁止所有非玩家实体触发套装。 |
| **enabled** | 单套装实体限制<br>已开启：这个套装只会对左侧已添加的生物生效。<br>玩家仍需同时满足该套装自身的玩家限制与装备条件。<br>左键：关闭此套装的实体限制。<br>名单为空时：该套装不会对任何非玩家实体生效。 |
| **disabled** | 单套装实体限制<br>已关闭：这个套装不会额外检查实体名单。<br>仍然会受到套装总开关和全局实体规则限制。<br>左键：开启限制，只允许左侧已添加的生物生效。 |
| **save global** | 保存全局实体规则<br>保存当前过滤模式与实体名单到服务器配置。<br>保存后立即重建过滤缓存并同步。 |
| **save set** | 保存当前套装实体规则<br>保存这个套装的实体限制开关与允许生物名单。<br>单套装名单始终按白名单规则判断。 |
| **back** | 返回上一界面<br>不会保存本界面尚未保存的改动。 |
| **移除结果** | 移除当前结果<br>只移除左侧当前搜索结果中的所有生物。<br>未被当前搜索匹配的生物不会受到影响。 |
| **添加结果** | 添加当前结果<br>把右侧当前搜索结果中的所有生物加入规则名单。<br>适合配合名称、ID、@模组或#标签搜索批量添加。 |
| **生效实体** | 设置这个套装允许生效的生物。开启限制后，仅已添加生物可以激活该套装。 |
| **旋转速度** | 输入模型悬浮旋转速度百分比，范围 0%-500%。0% 表示不旋转。 |
| **direction** | 点击切换实体模型的旋转方向。右侧数字输入框仍用于设置 0%-500% 的旋转速度。 |
| **mode** | 点击切换组合逻辑：<br>[满足任意] 只要列表里有 1 个条件成立，加成即生效。<br>[满足全部] 必须列表里所有条件同时成立，加成才生效。<br>[至少 X 项] 成立条件的数量达到设定值，加成即生效。<br><br>💡 进阶玩法：通过反转条件制作「禁用规则」<br>配合列表项右侧的 [NOT] 按钮，可以实现逻辑反转！<br>例如想做：“当下雨且在水中时，无法激活飞行”<br>1. 模式设为: [满足任意]<br>2. 添加条件: [NOT] 下雨<br>3. 添加条件: [NOT] 在水中<br>(这就是德·摩根定律，只要没下雨 或 不在水中，就会继续激活) |

### 可编辑字段、模式与分类索引

- 饰品
- 饰品栏 (扩展)
- 排斥饰品 (穿戴其一则套装失效)
- 排斥饰品槽位
- 头盔栏
- 胸甲栏
- 护腿栏
- 靴子栏
- 主手
- 副手
- 至少 %s 项
- 至少满足
- 白名单模式
- 黑名单模式
- 黑名单
- 白名单
- 过滤模式：黑名单
- 过滤模式：白名单
- 满足全部
- 满足任意
- 属性值范围
- 晴天
- 正在攀爬
- 白天
- 指定维度
- 经验等级范围
- 正在下落
- 饱食度范围
- 生命值范围
- 在空中
- 在水中
- 月相判断
- 左键点击
- 左键长按
- 右键点击
- 右键长按
- 夜晚
- 站在方块
- 正在燃烧
- 药水等级范围
- 正在下雨
- 正在上升
- 正在潜行
- 移动速度范围
- 拥有阶段 (GameStages)
- 雷雨天气
- 指定时间范围
- %s %s
- 未选择

### 配置键与默认值

| 配置键 | 默认值 |
|---|---|
| `armorsets.defaultTipKey` | `"shift"` |
| `armorsets.enableSets` | `true` |
| `armorsets.entityFilterMode` | `"BLACKLIST"` |
| `armorsets.potionRefreshInterval` | `20` |
| `armorsets.syncOnReload` | `true` |

### 配置与数据路径

主要配置/数据路径：

- `config/kineticcore/armorsets.toml`
- `config/kineticcore/armorsets/`
- `config/kineticcore/armorsets_client.toml`

### 依赖与可选兼容

| 模组 ID | 关系 |
|---|---|
| `kineticcore` | 必须 |
| `curios` | 必须 |
| `kubejs` | 可选 |
