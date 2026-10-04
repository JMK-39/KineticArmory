# GUI verification / 界面验证

## English

2026-10-04: bounded headings, warnings, captions and row text in 17 GUI files; matching Core 26.10.4 required. Mixed tip rows reserve icon slots, retain legacy colors/styles across icons, and clamp the drag preview inside the page. No new production loader branches, configuration syntax or language keys.

- Offline buildAll and runtimeValidationJar passed for both enabled nodes; architecture/reference checks passed, zero Mixin targets/problems. Source EN/CN each389 keys, NeoForge overrides each11, packaged Forge389/NeoForge392. Validation classes are absent from production JARs.
- Existing NeoForge21.1.252 client:31 actual states, English/Chinese at854×480 and1536×864, automatic GUI scale; extended translations photographed twice. Coverage: armor edit, bonus empty/populated/warning, four variant modes, tips add/edit/drag, detail, commands add/edit, global/set entity filters, conditions empty/populated/four parameter schemas, seven bonus editors, valid/invalid components.
- Full run186 captures/zero failures. The original empty-variant fixture was hydrated from legacy equipment; clearing that local slot supplied the actual empty state (6-capture follow-up). Final affected-state run60 captures/zero failures; colored tip run18 captures/zero failures. Execution markers were checked separately from screenshot review.
- Screens show text moving inside its assigned regions with adjacent controls clear. Bonus warning and input captions are separated; tier values have their own column. Tip icons no longer cover following words; green text stays green after icons. Drag preview remains on screen.
- Coverage limits: native hover tooltips obscure parts of large variant rows, the stress armor-edit Curios heading and ATTR_RANGE fields; corresponding other-size captures and visible bounds were reviewed, but hidden pixels are not certified. Missing-entity preview fallback was not visibly exercised (the invalid ID is counted but not rendered). Drag preview clamps to the full page canvas, not the inset painted border (about3 physical pixels beyond that border, still on screen). The sample detail override did not force every generated mixed-icon row; its existing sequential renderer was reviewed in source.
- Draft configurations only; no save callbacks or inventory-changing actions. Original options.txt restored byte-for-byte (SHA25650498E5F02C94CAADB7B7752AE4F4C304CF3463FD27382E950FA6348F70AD919). Own clients stopped normally, temporary validation JAR removed from mods, memory settings unchanged. Production SHA2568C2E4558BB6DF7201DF6ACBFBCC839B59F6390D22CBC4F8CBED24298F5F04360 matches D:/NEWMODS.
- ArmorList already uses bounded Core scrolling; its only direct text is a short plus symbol. HUD/world text unchanged. No Forge game run, per user instruction. Core tall-tooltip limitation remains tracked in ContentStudio/docs/tooltip-height-follow-up.md. Custom item tooltip components also have only width()/height()/render() in the current public contract: arbitrary long custom-icon rows do not receive a wrapping budget; this path has not been claimed fixed.

Evidence: .gradle/gui-long-text-20261004/build.log, build-colors.log, client-full.log, client-empty.log, client-final.log, client-colors.log and phase-numbered PNGs.

## 简体中文

2026-10-04：17个界面文件的标题、警告、标签与列表文字限制在各自区域，最低匹配核心26.10.4。图文提示为图标预留位置，图标前后保留旧颜色/字体样式；拖动预览限制在页面内。未新增加载器分支、配置语法或语言键。

- 两个启用节点通过离线buildAll与验证JAR构建，架构/引用检查通过，Mixin目标及问题均为0。中英文源码各389键，NeoForge覆盖各11键，打包Forge各389键/NeoForge各392键；正式产物不含验证类。
- 使用现有NeoForge21.1.252客户端检查31种真实状态，中英文分别854×480及1536×864，GUI自动缩放，超长翻译拍摄两帧。覆盖套装编辑、空/已有/警告件数奖励、四种候选模式、提示新增/编辑/拖动、效果明细、命令新增/编辑、全局/套装实体过滤、空/已有条件、四种参数类型、七种加成编辑器及有效/错误组件。
- 完整运行186张截图零失败；空候选用例原被旧装备字段自动填充，清空本地字段后补测6张，已覆盖真实空状态。最终相关界面补测60张，带色提示补测18张，均零运行失败；截图另行检查。
- 滚动文字保持在区域内，警告避开输入标签，件数名称与数值分列。图标不再压住后续文字，绿色跨图标延续，拖动预览不越界。
- 局限：部分大窗口候选行、套装长文本饰品标题及属性条件字段被正常悬浮提示遮挡；其余尺寸与可见边界已检查，不宣称隐藏像素已验证。缺失实体预览分支未实际显示（无效ID计数但不绘制）；拖动预览限制在完整画布内，比内缩背景边框约多3个物理像素，仍在屏幕内。示例明细覆盖未强制生成每种图标行，其现有顺序绘制方法已检查源码。
- 只操作本地草稿，不调用保存或库存操作；options.txt逐字节恢复，测试客户端正常退出，验证JAR已移出mods，未修改内存。正式JAR与D:/NEWMODS哈希一致。
- 套装列表原已使用核心边界滚动，唯一直接文字为加号；HUD/世界文字不变，按用户要求未启动Forge游戏。核心过高悬浮提示仍单独交接；当前自定义物品提示组件API没有传递换行预算，任意长图标提示行不算已修复。

本机证据见.gradle/gui-long-text-20261004。
