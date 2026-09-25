# KineticArmory

[English](#english) | [简体中文](#chinese) | [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kineticarmory)

<a id="english"></a>

## English

KineticArmory turns existing equipment into configurable sets with bonuses that respond to worn pieces, conditions, and combat events. It provides an in-game editor, server-side activation checks, and item tooltips.

### Requirements

- Minecraft **1.20.1**, Forge **47.4.2+**, Java **17**.
- Required: **KineticCore 26.9.20+**, **Curios 5.10.0+**.
- Optional: **KubeJS 2001.6.5+** for set activation/deactivation scripts.
- Install the mod and required dependencies on the server and connecting clients.

Pack authors define which existing items form a set and what that set does. Installation alone does not give every armor combination predefined bonuses.

### Create a first set

1. Enter a world with permission level **2** and press **F6** to open KineticCore's shared configuration hub.
2. Select the KineticArmory set editor page, then open the editor.
3. Create a unique set ID and display name. Import currently equipped armor and Curios, or choose requirements manually.
4. Add a simple attribute or potion bonus. Configure a minimum piece count if the set should activate before all pieces are worn.
5. Save, equip the required items, and check their tooltips. The server determines active sets and matching piece counts.
6. Add conditions, piece tiers, entity rules, and combat effects after the basic set works.

F6 opens the shared hub rather than directly opening the set list. Shared editing requires server authorization.

### Equipment matching

| Rule | Purpose |
| --- | --- |
| Equipment slots | Match standard slots, including armor and held items. |
| Equipment variants | Accept alternative items for the same slot. |
| Curios requirements | Include equipped accessories in set matching. |
| Rejected Curios | Prevent activation when specified accessories are worn. |
| Item NBT | Distinguish items sharing an item ID using NBT requirements. |
| `ANY` / `EMPTY` | Require a nonempty or empty slot; empty-slot requirements do not count as worn pieces. |
| Flexible pieces | Activate after a minimum number of pieces match. |
| Piece bonus groups | Associate effects and values with different piece-count tiers. |

Use the editor to construct variant and NBT rules before maintaining their JSON. The set ID determines its filename; its display name is separate.

### Available bonuses

- **Attributes:** additive, base-multiplier, and total-multiplier modifiers.
- **Potion effects:** wearer effects and chance-based effects on attack.
- **Damage:** damage-type immunities, incoming and outgoing multipliers, and damage conversions.
- **Effect immunities:** protection against configured status effects.
- **Flight:** grant flight when piece and condition requirements are met.
- **Commands:** execute actions when the set activates or deactivates.

Effects can have their own piece requirements and conditions. An active set does not necessarily have every configured effect active simultaneously.

### Dynamic conditions

Combine checks using any, all, or minimum-count matching:

| Family | Examples |
| --- | --- |
| Posture and movement | Sneaking, airborne, climbing, falling, rising, speed. |
| Environment | Water, fire, rain, thunder, clear weather, dimension, block underfoot. |
| Time | Day, night, moon phase, time range. |
| Entity values | Health, food, experience, attributes, potion-effect levels. |
| Input | Left/right mouse click and hold duration. |
| Progression | Named stage condition. |

Choose conditions appropriate to the target: player input, food, and experience are not general-purpose mob triggers.

### Entities and tooltips

Sets default to **player-only**. To support other living entities, disable that restriction and configure the set's entity whitelist as needed. The global entity filter also controls eligible non-player entities.

Tooltips show requirements and bonuses using server-synchronized activation and piece counts. Authors can generate descriptions automatically or customize line layout, hide entries, override text, and write manual tips.

The default tooltip modifier is **Shift**. A client configuration page controls the local default; individual sets can also specify their tooltip key.

### Configuration and applying changes

| Path relative to the game/server directory | Contents |
| --- | --- |
| `config/kineticcore/armorsets/<set-id>.json` | One set per file. |
| `config/kineticcore/armorsets.toml` | Enable switch, potion refresh interval, synchronization, entity filter. |
| `config/kineticcore/armorsets_client.toml` | Local tooltip preferences. |

Editor saves go to the server, persist to disk, and trigger recalculation. The reload operation reloads set definitions and recalculates active sets; `syncOnReload` controls broadcasting updated definitions to clients.

Opening the editor also reads server files. After external edits, reopen/reload through the editor and check the log for failed filenames. Invalid JSON and unsafe set IDs are skipped.

The old standalone armor command registration is a compatibility no-op; use the shared hub for editing. Global configuration saves in a running single-player world recalculate sets; changes outside a world apply at its next load.

### KubeJS integration

With KubeJS installed, use this server event:

```js
kineticarmoryEvents.armorSetChange(event => {
  console.info(`${event.getSetId()}: ${event.isActivated()}`)
})
```

The event exposes `getEntity()`, `getSetId()`, `isActivated()`, coordinates, and `getDimension()`. It reports set activation changes; normal bonus definitions remain editable through the GUI.

### Troubleshooting and boundaries

- **Editor unavailable:** confirm permission level 2 and select the editor action page in the F6 hub.
- **Set inactive:** check slots, variants, NBT, rejected Curios, minimum pieces, and entity filters.
- **Only some bonuses work:** check each effect's conditions and piece group.
- **Tooltip mismatch:** check reload synchronization and the local tooltip modifier.
- **Command actions:** run from a permission-level-2 server command source; literal `@p` is replaced with the wearer's scoreboard name. These are administrator-authored actions.

<a id="chinese"></a>

## 简体中文

KineticArmory 将已有装备组织成可配置套装，让加成随穿戴件数、条件和战斗事件变化。它提供游戏内编辑器、服务端生效判定和物品提示。

### 环境要求

- Minecraft **1.20.1**、Forge **47.4.2+**、Java **17**。
- 必需：**KineticCore 26.9.20+**、**Curios 5.10.0+**。
- 可选：**KubeJS 2001.6.5+**，用于套装激活与失效脚本。
- 服务端和连接的客户端均安装本模组及必需依赖。

由整合包作者指定哪些已有物品组成套装、具有什么效果。安装后并不会自动为所有护甲组合添加预设加成。

### 创建第一套装备

1. 以拥有 **2 级权限**的身份进入世界，按 **F6** 打开 KineticCore 共用配置中心。
2. 选择 KineticArmory 套装编辑器页面，再打开编辑器。
3. 创建唯一 ID 和显示名称；导入当前穿戴的护甲与 Curios，或手动选择需求物品。
4. 先添加简单的属性或药水加成；需要未穿齐就激活时，设置最少件数。
5. 保存并穿戴对应物品，检查物品提示。激活状态和匹配件数由服务端决定。
6. 基础效果正常后，再添加条件、件数档位、实体规则和战斗效果。

F6 打开的是共用配置中心，并非直接进入套装列表。公共套装编辑需要服务端授权。

### 装备匹配

| 规则 | 用途 |
| --- | --- |
| 标准装备槽 | 匹配护甲、手持物品等标准槽位。 |
| 装备变种 | 同一槽位接受不同备选物品。 |
| Curios 需求 | 将已佩戴饰品纳入套装匹配。 |
| 排斥 Curios | 穿戴指定饰品时阻止激活。 |
| 物品 NBT | 通过 NBT 要求区分同一 ID 的不同装备。 |
| `ANY` / `EMPTY` | 要求槽位非空或为空；空槽要求不计入穿戴件数。 |
| 灵活件数 | 达到最少匹配件数即可激活。 |
| 件数加成组 | 为不同件数档位关联效果和数值。 |

建议先用编辑器构造变种和 NBT 规则，再维护对应 JSON。套装 ID 决定文件名，与显示名称独立。

### 可配置加成

- **属性：** 加算、基础倍率和最终倍率修改。
- **药水效果：** 持有者效果，以及攻击时按概率施加的效果。
- **伤害：** 伤害类型免疫、受伤和攻击倍率、伤害转换。
- **效果免疫：** 免疫指定状态效果。
- **飞行：** 满足件数和条件时授予飞行能力。
- **命令：** 套装激活或失效时执行动作。

各项效果可以拥有独立件数要求和条件，因此套装激活不代表所有效果同时生效。

### 动态条件

可使用任一满足、全部满足或至少满足指定数量的组合方式：

| 类别 | 示例 |
| --- | --- |
| 姿态与运动 | 潜行、滞空、攀爬、下落、上升、速度。 |
| 环境 | 水中、着火、下雨、雷暴、晴天、维度、脚下方块。 |
| 时间 | 白天、夜晚、月相、时间范围。 |
| 实体数值 | 生命、饥饿、经验、属性、药水效果等级。 |
| 输入 | 鼠标左/右键点击、持续按住时间。 |
| 进度 | 指定名称的阶段条件。 |

条件应与目标实体相符：玩家输入、饥饿和经验判断不是通用生物触发器。

### 实体与物品提示

套装默认**仅玩家生效**。要用于其他活体实体，需关闭此限制，并按需设置该套装的实体白名单。全局实体过滤规则也会限制非玩家实体的参与范围。

物品提示显示需求和加成，使用服务端同步的激活状态与件数。作者可以自动生成说明，也可以调整行顺序、隐藏条目、覆盖文本或编写手动提示。

默认提示修饰键为 **Shift**。客户端配置页控制本地默认值；单个套装也可以指定提示按键。

### 配置与生效方式

| 相对于游戏或服务器目录的路径 | 内容 |
| --- | --- |
| `config/kineticcore/armorsets/<set-id>.json` | 每个文件定义一套装备。 |
| `config/kineticcore/armorsets.toml` | 总开关、药水刷新间隔、同步、实体过滤。 |
| `config/kineticcore/armorsets_client.toml` | 本地提示偏好。 |

编辑器保存会提交服务端、写入文件并重新计算。重载操作重新读取套装定义并计算激活状态；`syncOnReload` 控制是否向客户端广播更新后的定义。

打开编辑器也会读取服务器文件。外部修改后，通过编辑器重新打开或重载，检查日志中的失败文件名。无效 JSON 和不安全的套装 ID 会被跳过。

旧独立护甲命令注册入口只保留兼容空实现，编辑请使用共用配置中心。单人世界内保存全局配置会重新计算套装；世界外修改在下次加载时生效。

### KubeJS 联动

安装 KubeJS 后可使用服务端事件：

```js
kineticarmoryEvents.armorSetChange(event => {
  console.info(`${event.getSetId()}: ${event.isActivated()}`)
})
```

事件提供 `getEntity()`、`getSetId()`、`isActivated()`、坐标和 `getDimension()`，用于报告套装激活变化；常规加成仍可直接在 GUI 中配置。

### 排查与边界

- **编辑器不可用：** 确认拥有 2 级权限，并在 F6 中选择编辑器动作页面。
- **套装不激活：** 检查槽位、变种、NBT、排斥饰品、最少件数和实体过滤。
- **部分加成不生效：** 检查各效果自己的条件与件数组。
- **提示不一致：** 检查重载同步和本地提示修饰键。
- **命令动作：** 以 2 级权限服务器命令源执行，文本中的 `@p` 替换为穿戴者的计分板名称；应由管理员编写配置。
