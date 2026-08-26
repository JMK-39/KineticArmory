# KineticArmory

[简体中文](#简体中文) | [English](#english)

## 简体中文

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

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17
- KineticCore：必须
- Curios：必须
- KubeJS：可选

## English

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

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17
- KineticCore: required
- Curios: required
- KubeJS: optional


## 开源协议与版权 (License)

Copyright (C) 2024-2026 XYAT.

本项目基于 **GNU Lesser General Public License v3.0 (LGPLv3)** 协议开源。

This project is open-sourced under the **GNU Lesser General Public License v3.0 (LGPLv3)**.
