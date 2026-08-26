package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticcore.api.client.AdvancedSearchUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.PinyinUtil;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.ScrollUtil;
import dev.xyat.kineticcore.api.client.entity.EntityPreviewRenderer;
import dev.xyat.kineticcore.api.client.gui.ConfigScrollbarTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ArmorEntityFilterScreen extends ScaledScreen {
    private static final int V_WIDTH = 640;
    private static final int V_HEIGHT = 360;
    private static final int LEFT_X = 8;
    private static final int RIGHT_X = 324;
    private static final int PANEL_Y = 42;
    private static final int PANEL_W = 308;
    private static final int PANEL_H = 292;
    private static final int SEARCH_Y = 68;
    private static final int GRID_Y = 94;
    private static final int COLS = 4;
    private static final int CELL_SIZE = 72;
    private static final int VISIBLE_ROWS = 3;
    private static final int GRID_W = COLS * CELL_SIZE;
    private static final int GRID_H = VISIBLE_ROWS * CELL_SIZE;
    private static int rotationSpeedPercent = 100;
    private static boolean clockwiseRotation = true;

    private final ScaledScreen parent;
    private final ArmorDataConfig armorSet;
    private final boolean global;
    private final Set<String> selectedIds = new HashSet<>();
    private final List<String> originalRules = new ArrayList<>();
    private final List<EntityEntryData> allEntities = new ArrayList<>();
    private final Set<String> knownEntityIds = new HashSet<>();
    private final List<EntityEntryData> leftEntities = new ArrayList<>();
    private final List<EntityEntryData> rightEntities = new ArrayList<>();
    private final EntityPreviewRenderer entityPreviewRenderer =
            new EntityPreviewRenderer();

    private EditBox leftSearchBox;
    private EditBox rotationSpeedBox;
    private EditBox rightSearchBox;
    private String globalMode;
    private boolean setFilterEnabled;
    private boolean rulesDirty;
    private int leftScroll;
    private int rightScroll;
    private boolean draggingLeftScroll;
    private boolean draggingRightScroll;
    private List<Component> deferredTooltip;

    public ArmorEntityFilterScreen(ScaledScreen parent) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.global_title"));
        this.parent = parent;
        this.armorSet = null;
        this.global = true;
        this.globalMode = ArmorClientSnapshot.entityFilterMode();
        initEntities();
        loadRules(ArmorClientSnapshot.entityFilterRules());
        sortEntities();
        refreshLists();
        setupScale();
    }

    public ArmorEntityFilterScreen(ScaledScreen parent, ArmorDataConfig armorSet) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.set_title"));
        this.parent = parent;
        this.armorSet = armorSet;
        this.global = false;
        this.globalMode = "WHITELIST";
        this.setFilterEnabled = armorSet.entityWhitelistEnabled;
        initEntities();
        loadRules(armorSet.allowedEntityTypes);
        sortEntities();
        refreshLists();
        setupScale();
    }

    private void setupScale() {
        configureResponsiveCanvas(
                V_WIDTH,
                V_HEIGHT,
                6
        );
        this.scaleMultiplier = 1f;
        entityPreviewRenderer.setRotationSpeedPercent(rotationSpeedPercent);
        entityPreviewRenderer.setClockwise(clockwiseRotation);
    }

    private void initEntities() {
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (type.getCategory() == MobCategory.MISC) continue;
            addEntityEntry(type);
        }
    }

    private void addEntityEntry(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null || !knownEntityIds.add(id.toString())) return;

        Component name = ColorText.translatable(type.getDescriptionId());
        String displayName = name.getString();
        String searchData = id + " " + displayName + " " + PinyinUtil.getSearchData(displayName);

        allEntities.add(new EntityEntryData(
                type,
                id,
                name,
                searchData.toLowerCase(Locale.ROOT)
        ));
    }

    private void addMissingConfiguredEntry(ResourceLocation id) {
        if (id == null || !knownEntityIds.add(id.toString())) return;

        Component name = Component.literal(id.toString());
        allEntities.add(new EntityEntryData(
                null,
                id,
                name,
                id.toString().toLowerCase(Locale.ROOT)
        ));
    }

    private void sortEntities() {
        allEntities.sort(Comparator.comparing(data -> data.id().toString()));
    }

    private void loadRules(List<String> rules) {
        if (rules == null || rules.isEmpty()) return;

        for (String raw : rules) {
            if (raw == null) continue;

            String rule = raw.trim();
            if (rule.isEmpty()) continue;

            if (rule.equalsIgnoreCase("ALL")) {
                originalRules.add(rule);
                for (EntityEntryData data : allEntities) {
                    selectedIds.add(data.id().toString());
                }
                continue;
            }

            if (rule.startsWith("@")) {
                originalRules.add(rule);

                String namespace = rule.substring(1).trim();
                if (namespace.isEmpty()) continue;

                for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);

                    if (id != null
                            && namespace.equalsIgnoreCase(id.getNamespace())
                            && type.getCategory() != MobCategory.MISC) {
                        addEntityEntry(type);
                        selectedIds.add(id.toString());
                    }
                }

                continue;
            }

            if (rule.startsWith("#")) {
                originalRules.add(rule);

                ResourceLocation tagId = ResourceLocation.tryParse(rule.substring(1).trim());
                var manager = ForgeRegistries.ENTITY_TYPES.tags();

                if (tagId == null || manager == null) continue;

                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);

                for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                    if (type.getCategory() == MobCategory.MISC) continue;
                    if (!manager.getTag(tag).contains(type)) continue;

                    addEntityEntry(type);

                    ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
                    if (id != null) {
                        selectedIds.add(id.toString());
                    }
                }

                continue;
            }

            ResourceLocation configuredId = ResourceLocation.tryParse(rule);
            if (configuredId == null) continue;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(configuredId);
            if (type != null) {
                addEntityEntry(type);
            } else {
                addMissingConfiguredEntry(configuredId);
            }

            selectedIds.add(configuredId.toString());
            originalRules.add(rule);
        }

        ensureSelectedEntries();
    }

    private void ensureSelectedEntries() {
        for (String rawId : selectedIds) {
            if (knownEntityIds.contains(rawId)) continue;

            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) continue;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);

            if (type != null) {
                addEntityEntry(type);
            } else {
                addMissingConfiguredEntry(id);
            }
        }
    }

    @Override
    protected void initScaled() {
        int topY = 18;
        int modeW = 142;
        int speedButtonW = 72;
        int speedInputW = 52;
        int saveW = 64;
        int backW = 64;

        if (global) {
            Button modeButton = Button.builder(getGlobalModeText(), b -> {
                        globalMode = "WHITELIST".equalsIgnoreCase(globalMode)
                                ? "BLACKLIST"
                                : "WHITELIST";

                        b.setMessage(getGlobalModeText());
                        b.setTooltip(Tooltip.create(getGlobalModeTooltip()));
                    })
                    .bounds(8, topY, modeW, 20)
                    .tooltip(Tooltip.create(getGlobalModeTooltip()))
                    .build();

            this.addRenderableWidget(modeButton);
        } else {
            Button setToggleButton = Button.builder(getSetFilterText(), b -> {
                        setFilterEnabled = !setFilterEnabled;
                        b.setMessage(getSetFilterText());
                        b.setTooltip(Tooltip.create(getSetFilterTooltip()));
                    })
                    .bounds(8, topY, modeW, 20)
                    .tooltip(Tooltip.create(getSetFilterTooltip()))
                    .build();

            this.addRenderableWidget(setToggleButton);
        }

        rotationSpeedBox = new EditBox(
                this.font,
                432,
                topY,
                speedInputW,
                20,
                ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.rotation_speed")
        );
        rotationSpeedBox.setMaxLength(3);
        rotationSpeedBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        rotationSpeedBox.setValue(Integer.toString(rotationSpeedPercent));
        rotationSpeedBox.setResponder(value -> applyRotationSpeedInput(false));
        rotationSpeedBox.setTooltip(Tooltip.create(ColorText.translatable(
                "gui.kineticarmory.armorsets.entity_filter.rotation_speed.tooltip"
        )));
        this.addRenderableWidget(rotationSpeedBox);

        this.addRenderableWidget(
                Button.builder(
                                getRotationDirectionText(),
                                b -> {
                                    clockwiseRotation = !clockwiseRotation;
                                    entityPreviewRenderer.setClockwise(clockwiseRotation);
                                    b.setMessage(getRotationDirectionText());
                                }
                        )
                        .bounds(356, topY, speedButtonW, 20)
                        .tooltip(Tooltip.create(ColorText.translatable(
                                "gui.kineticarmory.common.rotation.direction.tooltip"
                        )))
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                ColorText.translatable("gui.kineticarmory.armorsets.save"),
                                b -> saveAndBack()
                        )
                        .bounds(500, topY, saveW, 20)
                        .tooltip(Tooltip.create(ColorText.translatable(
                                global
                                        ? "gui.kineticarmory.armorsets.entity_filter.save_global.tooltip"
                                        : "gui.kineticarmory.armorsets.entity_filter.save_set.tooltip"
                        )))
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                ColorText.translatable("gui.kineticarmory.common.back"),
                                b -> backWithoutSave()
                        )
                        .bounds(568, topY, backW, 20)
                        .tooltip(Tooltip.create(ColorText.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.back.tooltip"
                        )))
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                ColorText.translatable(
                                        "gui.kineticarmory.armorsets.entity_filter.remove_filtered"
                                ),
                                b -> removeFiltered()
                        )
                        .bounds(LEFT_X + PANEL_W - 80, PANEL_Y + 4, 72, 18)
                        .tooltip(Tooltip.create(ColorText.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.remove_filtered.tooltip"
                        )))
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                ColorText.translatable(
                                        "gui.kineticarmory.armorsets.entity_filter.add_filtered"
                                ),
                                b -> addFiltered()
                        )
                        .bounds(RIGHT_X + PANEL_W - 80, PANEL_Y + 4, 72, 18)
                        .tooltip(Tooltip.create(ColorText.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.add_filtered.tooltip"
                        )))
                        .build()
        );

        leftSearchBox = new EditBox(
                this.font,
                LEFT_X + 8,
                SEARCH_Y,
                PANEL_W - 16,
                20,
                Component.empty()
        );

        leftSearchBox.setMaxLength(256);
        leftSearchBox.setResponder(value -> {
            leftScroll = 0;
            refreshLists();
        });

        this.addRenderableWidget(leftSearchBox);

        rightSearchBox = new EditBox(
                this.font,
                RIGHT_X + 8,
                SEARCH_Y,
                PANEL_W - 16,
                20,
                Component.empty()
        );

        rightSearchBox.setMaxLength(256);
        rightSearchBox.setResponder(value -> {
            rightScroll = 0;
            refreshLists();
        });

        this.addRenderableWidget(rightSearchBox);

        refreshLists();
    }

    private Component getRotationDirectionText() {
        return ColorText.translatable(
                clockwiseRotation
                        ? "gui.kineticarmory.common.rotation.clockwise"
                        : "gui.kineticarmory.common.rotation.counterclockwise"
        );
    }

    private boolean applyRotationSpeedInput(boolean notifyInvalid) {
        if (rotationSpeedBox == null) return true;

        String raw = rotationSpeedBox.getValue().trim();
        if (!raw.isEmpty()) {
            try {
                int value = Integer.parseInt(raw);
                if (value >= 0 && value <= 500) {
                    rotationSpeedPercent = value;
                    entityPreviewRenderer.setRotationSpeedPercent(rotationSpeedPercent);
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (notifyInvalid) {
            GuiToastUtil.showToast(
                    "armorsets_rotation_speed_invalid",
                    ColorText.translatable("msg.kineticarmory.armorsets.rotation_speed.invalid")
            );
        }
        return false;
    }

    private Component getGlobalModeText() {
        return ColorText.translatable(
                "WHITELIST".equalsIgnoreCase(globalMode)
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.button.whitelist"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.button.blacklist"
        );
    }

    private Component getGlobalModeTooltip() {
        return ColorText.translatable(
                "WHITELIST".equalsIgnoreCase(globalMode)
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.whitelist.tooltip"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.blacklist.tooltip"
        );
    }

    private Component getSetFilterText() {
        return ColorText.translatable(
                setFilterEnabled
                        ? "gui.kineticarmory.armorsets.entity_filter.set_toggle.button.enabled"
                        : "gui.kineticarmory.armorsets.entity_filter.set_toggle.button.disabled"
        );
    }

    private Component getSetFilterTooltip() {
        return ColorText.translatable(
                setFilterEnabled
                        ? "gui.kineticarmory.armorsets.entity_filter.set_toggle.enabled.tooltip"
                        : "gui.kineticarmory.armorsets.entity_filter.set_toggle.disabled.tooltip"
        );
    }

    private Component getLeftTitle() {
        if (!global) {
            return ColorText.translatable(
                    "gui.kineticarmory.armorsets.entity_filter.left_set",
                    selectedIds.size()
            );
        }

        return ColorText.translatable(
                "WHITELIST".equalsIgnoreCase(globalMode)
                        ? "gui.kineticarmory.armorsets.entity_filter.left_whitelist"
                        : "gui.kineticarmory.armorsets.entity_filter.left_blacklist",
                selectedIds.size()
        );
    }

    private int availableCount() {
        return Math.max(0, allEntities.size() - selectedIds.size());
    }

    private void refreshLists() {
        String leftQuery = leftSearchBox == null
                ? ""
                : leftSearchBox.getValue();

        String rightQuery = rightSearchBox == null
                ? ""
                : rightSearchBox.getValue();

        leftEntities.clear();
        rightEntities.clear();

        for (EntityEntryData data : allEntities) {
            boolean selected = selectedIds.contains(data.id().toString());

            if (selected) {
                if (matchesSearch(data, leftQuery)) {
                    leftEntities.add(data);
                }
            } else if (matchesSearch(data, rightQuery)) {
                rightEntities.add(data);
            }
        }

        leftScroll = clampScroll(leftScroll, leftEntities.size());
        rightScroll = clampScroll(rightScroll, rightEntities.size());
    }

    private boolean matchesSearch(EntityEntryData data, String rawQuery) {
        String query = rawQuery == null
                ? ""
                : rawQuery.trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty()) return true;

        if (query.startsWith("@")) {
            String namespace = query.substring(1).trim();
            return namespace.isEmpty()
                    || data.id().getNamespace().contains(namespace);
        }

        if (query.startsWith("#")) {
            String tagQuery = query.substring(1).trim();

            if (tagQuery.isEmpty()) return true;

            return data.type() != null
                    && data.type().builtInRegistryHolder().tags()
                    .anyMatch(tag -> tag.location()
                            .toString()
                            .toLowerCase(Locale.ROOT)
                            .contains(tagQuery));
        }

        return AdvancedSearchUtil.match(data.searchData(), query);
    }

    private void addFiltered() {
        if (rightEntities.isEmpty()) return;

        for (EntityEntryData data : rightEntities) {
            selectedIds.add(data.id().toString());
        }

        markRulesDirty();
    }

    private void removeFiltered() {
        if (leftEntities.isEmpty()) return;

        for (EntityEntryData data : new ArrayList<>(leftEntities)) {
            selectedIds.remove(data.id().toString());
        }

        markRulesDirty();
    }

    private void addEntity(EntityEntryData data) {
        if (data != null && selectedIds.add(data.id().toString())) {
            markRulesDirty();
        }
    }

    private void removeEntity(EntityEntryData data) {
        if (data != null && selectedIds.remove(data.id().toString())) {
            markRulesDirty();
        }
    }

    private void markRulesDirty() {
        rulesDirty = true;
        leftScroll = 0;
        rightScroll = 0;
        refreshLists();
    }

    private List<String> buildRules() {
        if (!rulesDirty) {
            return new ArrayList<>(new LinkedHashSet<>(originalRules));
        }

        if (!allEntities.isEmpty()
                && selectedIds.size() == allEntities.size()) {
            return new ArrayList<>(List.of("ALL"));
        }

        List<String> rules = new ArrayList<>(selectedIds);
        rules.sort(String::compareTo);
        return rules;
    }

    private void saveAndBack() {
        if (!applyRotationSpeedInput(true)) return;

        List<String> rules = buildRules();

        if (global) {
            ArmorClientSnapshot.replaceEntityFilter(globalMode, rules);

            ArmorNetwork.CHANNEL.sendToServer(
                    new ArmorNetwork.SaveEntityFilterPacket(globalMode, rules)
            );
        } else if (armorSet != null) {
            armorSet.entityWhitelistEnabled = setFilterEnabled;
            armorSet.allowedEntityTypes = rules;
            armorSet.prepareRuntimeCache();
        }

        backWithoutSave();
    }

    private void backWithoutSave() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        backWithoutSave();
    }

    @Override
    protected void renderScaledBackground(
            @NotNull GuiGraphics g,
            int mx,
            int my,
            float pt
    ) {
        deferredTooltip = null;

        g.fill(
                0,
                0,
                V_WIDTH,
                V_HEIGHT,
                0xFA1E1E1E
        );

        g.renderOutline(
                0,
                0,
                V_WIDTH,
                V_HEIGHT,
                0xFF444444
        );

        drawCenteredNoShadow(
                g,
                this.title,
                V_WIDTH / 2,
                6,
                0xFFFFFFFF
        );

        drawPanel(g, LEFT_X, getLeftTitle());

        drawPanel(
                g,
                RIGHT_X,
                ColorText.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.right_available",
                        availableCount()
                )
        );
    }

    private void drawPanel(
            GuiGraphics g,
            int x,
            Component title
    ) {
        g.fill(
                x,
                PANEL_Y,
                x + PANEL_W,
                PANEL_Y + PANEL_H,
                0xEE111111
        );

        g.renderOutline(
                x,
                PANEL_Y,
                PANEL_W,
                PANEL_H,
                0xFF555555
        );

        g.drawString(
                this.font,
                title,
                x + 8,
                PANEL_Y + 9,
                0xFFFFFFFF,
                false
        );

        g.fill(
                x + 8,
                GRID_Y - 2,
                x + 8 + GRID_W,
                GRID_Y + GRID_H + 2,
                0xFF090909
        );

        g.renderOutline(
                x + 8,
                GRID_Y - 2,
                GRID_W,
                GRID_H + 4,
                0xFF444444
        );
    }

    @Override
    protected void renderScaledForeground(
            @NotNull GuiGraphics g,
            int mx,
            int my,
            float pt
    ) {
        renderSearchHint(
                g,
                leftSearchBox,
                "gui.kineticarmory.armorsets.entity_filter.search_available"
        );

        renderSearchHint(
                g,
                rightSearchBox,
                "gui.kineticarmory.armorsets.entity_filter.search_added"
        );

        renderGrid(
                g,
                leftEntities,
                LEFT_X + 8,
                leftScroll,
                true,
                mx,
                my
        );

        renderGrid(
                g,
                rightEntities,
                RIGHT_X + 8,
                rightScroll,
                false,
                mx,
                my
        );

        renderScrollbar(
                g,
                mx,
                my,
                LEFT_X + PANEL_W - 7,
                leftEntities.size(),
                leftScroll,
                draggingLeftScroll
        );

        renderScrollbar(
                g,
                mx,
                my,
                RIGHT_X + PANEL_W - 7,
                rightEntities.size(),
                rightScroll,
                draggingRightScroll
        );

        Component status = global
                ? ColorText.translatable(
                "gui.kineticarmory.armorsets.entity_filter.global_status",
                selectedIds.size()
        )
                : ColorText.translatable(
                "gui.kineticarmory.armorsets.entity_filter.set_status",
                selectedIds.size()
        );

        drawCenteredNoShadow(
                g,
                status,
                V_WIDTH / 2,
                344,
                0xFFFFFFFF
        );

        if (deferredTooltip != null
                && !deferredTooltip.isEmpty()) {
            g.renderComponentTooltip(
                    this.font,
                    deferredTooltip,
                    mx,
                    my
            );
        }
    }

    private void renderSearchHint(
            GuiGraphics g,
            EditBox box,
            String key
    ) {
        if (box == null
                || !box.visible
                || !box.getValue().isEmpty()
                || box.isFocused()) {
            return;
        }

        String text = this.font.plainSubstrByWidth(
                ColorText.translatable(key).getString(),
                box.getWidth() - 10
        );

        g.drawString(
                this.font,
                text,
                box.getX() + 5,
                box.getY() + 6,
                0xFFAAAAAA,
                false
        );
    }

    private void renderGrid(
            GuiGraphics g,
            List<EntityEntryData> dataList,
            int gridX,
            int scroll,
            boolean addedSide,
            int mx,
            int my
    ) {
        int start = scroll * COLS;
        int end = Math.min(
                start + VISIBLE_ROWS * COLS,
                dataList.size()
        );

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % COLS;
            int row = local / COLS;

            int x = gridX + col * CELL_SIZE;
            int y = GRID_Y + row * CELL_SIZE;

            EntityEntryData data = dataList.get(i);

            boolean hovered = mx >= x
                    && mx < x + CELL_SIZE
                    && my >= y
                    && my < y + CELL_SIZE;

            int bg = addedSide
                    ? 0xFF17352D
                    : 0xFF252525;

            int outline = hovered
                    ? 0xFF88DDFF
                    : addedSide
                    ? 0xFF55DD88
                    : 0xFF555555;

            g.fill(
                    x + 2,
                    y + 2,
                    x + CELL_SIZE - 2,
                    y + CELL_SIZE - 2,
                    bg
            );

            g.renderOutline(
                    x + 1,
                    y + 1,
                    CELL_SIZE - 2,
                    CELL_SIZE - 2,
                    outline
            );

            EntityPreviewRenderer.drawCheckerboard(
                    g,
                    x + 4,
                    y + 4,
                    CELL_SIZE - 8,
                    CELL_SIZE - 8
            );

            renderAdaptiveEntity(
                    g,
                    x + 4,
                    y + 4,
                    data,
                    hovered
            );

            if (hovered) {
                List<Component> tooltip = new ArrayList<>();

                tooltip.add(ColorText.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.name",
                        data.name()
                ));

                tooltip.add(ColorText.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.id",
                        data.id().toString()
                ));

                tooltip.add(ColorText.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.zoom",
                        getModelZoomPercent(data.id().toString())
                ));

                if (data.type() == null) {
                    tooltip.add(ColorText.translatable(
                            "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.invalid"
                    ));
                }

                tooltip.add(ColorText.translatable(
                        addedSide
                                ? "gui.kineticarmory.armorsets.entity_filter.click_remove"
                                : "gui.kineticarmory.armorsets.entity_filter.click_add"
                ));

                deferredTooltip = tooltip;
            }
        }
    }

    private void renderScrollbar(
            GuiGraphics g,
            int mouseX,
            int mouseY,
            int x,
            int totalItems,
            int scroll,
            boolean dragging
    ) {
        int totalRows = rowCount(totalItems);
        int maxScroll = Math.max(
                0,
                totalRows - VISIBLE_ROWS
        );

        if (maxScroll > 0) {
            int thumbH = ScrollUtil.calculateThumbHeight(
                    GRID_H,
                    VISIBLE_ROWS,
                    totalRows,
                    20
            );

            ConfigScrollbarTheme.render(
                    g,
                    mouseX,
                    mouseY,
                    x,
                    GRID_Y,
                    4,
                    GRID_H,
                    thumbH,
                    maxScroll,
                    scroll,
                    dragging
            );
        }
    }

    private void renderAdaptiveEntity(
            GuiGraphics g,
            int boxX,
            int boxY,
            EntityEntryData data,
            boolean hovered
    ) {
        final int boxW = CELL_SIZE - 8;
        final int boxH = CELL_SIZE - 8;

        boolean rendered = entityPreviewRenderer.render(
                g,
                data.type(),
                data.id(),
                data.id().toString(),
                boxX,
                boxY,
                boxW,
                boxH,
                this.guiScale,
                this.offsetX,
                this.offsetY,
                hovered
        );

        if (!rendered) {
            drawCenteredNoShadow(
                    g,
                    ColorText.translatable(
                            "gui.kineticarmory.armorsets.entity_filter.preview_unavailable"
                    ),
                    boxX + boxW / 2,
                    boxY + boxH / 2 - 4,
                    0xFFFF7777
            );
        }
    }

    private int getModelZoomPercent(String id) {
        return entityPreviewRenderer.getZoomPercent(id);
    }

    private void adjustHoveredModelZoom(double mx, double my, double delta) {
        EntityEntryData hovered = getClickedEntity(
                leftEntities,
                LEFT_X + 8,
                leftScroll,
                mx,
                my
        );

        if (hovered == null) {
            hovered = getClickedEntity(
                    rightEntities,
                    RIGHT_X + 8,
                    rightScroll,
                    mx,
                    my
            );
        }

        if (hovered == null || delta == 0D) {
            return;
        }

        entityPreviewRenderer.adjustZoom(
                hovered.id().toString(),
                delta
        );
    }

    private void drawCenteredNoShadow(
            GuiGraphics g,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        String value = text.getString();

        g.drawString(
                this.font,
                value,
                centerX - this.font.width(value) / 2,
                y,
                color,
                false
        );
    }

    @Override
    protected boolean universalMouseClicked(
            double mx,
            double my,
            int btn
    ) {
        boolean handled = super.universalMouseClicked(
                mx,
                my,
                btn
        );

        if (handled) return true;
        if (btn != 0) return false;

        if (handleScrollbarClick(mx, my, true)) {
            return true;
        }

        if (handleScrollbarClick(mx, my, false)) {
            return true;
        }

        EntityEntryData left = getClickedEntity(
                leftEntities,
                LEFT_X + 8,
                leftScroll,
                mx,
                my
        );

        if (left != null) {
            removeEntity(left);
            this.setFocused(null);
            return true;
        }

        EntityEntryData right = getClickedEntity(
                rightEntities,
                RIGHT_X + 8,
                rightScroll,
                mx,
                my
        );

        if (right != null) {
            addEntity(right);
            this.setFocused(null);
            return true;
        }

        return false;
    }

    private boolean handleScrollbarClick(
            double mx,
            double my,
            boolean left
    ) {
        int panelX = left
                ? LEFT_X
                : RIGHT_X;

        int totalItems = left
                ? leftEntities.size()
                : rightEntities.size();

        int totalRows = rowCount(totalItems);

        int maxScroll = Math.max(
                0,
                totalRows - VISIBLE_ROWS
        );

        int x = panelX + PANEL_W - 7;

        boolean onScrollbar =
                maxScroll > 0
                        && mx >= x
                        && mx <= x + 4
                        && my >= GRID_Y
                        && my <= GRID_Y + GRID_H;

        if (!onScrollbar) {
            return false;
        }

        if (left) {
            draggingLeftScroll = true;
        } else {
            draggingRightScroll = true;
        }

        updateScrollFromMouse(
                my,
                left
        );

        return true;
    }

    private EntityEntryData getClickedEntity(
            List<EntityEntryData> list,
            int gridX,
            int scroll,
            double mx,
            double my
    ) {
        if (mx < gridX
                || mx >= gridX + GRID_W
                || my < GRID_Y
                || my >= GRID_Y + GRID_H) {
            return null;
        }

        int col = (int) (
                (mx - gridX) / CELL_SIZE
        );

        int row = (int) (
                (my - GRID_Y) / CELL_SIZE
        );

        int index =
                scroll * COLS
                        + row * COLS
                        + col;

        return index >= 0
                && index < list.size()
                ? list.get(index)
                : null;
    }

    @Override
    protected boolean universalMouseDragged(
            double mx,
            double my,
            int btn,
            double dx,
            double dy
    ) {
        if (btn == 0 && draggingLeftScroll) {
            updateScrollFromMouse(
                    my,
                    true
            );
            return true;
        }

        if (btn == 0 && draggingRightScroll) {
            updateScrollFromMouse(
                    my,
                    false
            );
            return true;
        }

        return super.universalMouseDragged(
                mx,
                my,
                btn,
                dx,
                dy
        );
    }

    @Override
    protected boolean universalMouseReleased(
            double mx,
            double my,
            int btn
    ) {
        if (btn == 0
                && (draggingLeftScroll
                || draggingRightScroll)) {
            draggingLeftScroll = false;
            draggingRightScroll = false;
            return true;
        }

        return super.universalMouseReleased(
                mx,
                my,
                btn
        );
    }

    @Override
    protected boolean universalMouseScrolled(
            double mx,
            double my,
            double delta
    ) {
        if (Screen.hasControlDown()) {
            EntityEntryData hoveredLeft = getClickedEntity(
                    leftEntities,
                    LEFT_X + 8,
                    leftScroll,
                    mx,
                    my
            );
            EntityEntryData hoveredRight = getClickedEntity(
                    rightEntities,
                    RIGHT_X + 8,
                    rightScroll,
                    mx,
                    my
            );

            if (hoveredLeft != null || hoveredRight != null) {
                adjustHoveredModelZoom(mx, my, delta);
                return true;
            }
        }

        if (my >= GRID_Y
                && my < GRID_Y + GRID_H) {
            int direction = delta > 0
                    ? -1
                    : delta < 0
                    ? 1
                    : 0;

            if (direction == 0) {
                return false;
            }

            if (mx >= LEFT_X
                    && mx < LEFT_X + PANEL_W) {
                leftScroll = clampScroll(
                        leftScroll + direction,
                        leftEntities.size()
                );
                return true;
            }

            if (mx >= RIGHT_X
                    && mx < RIGHT_X + PANEL_W) {
                rightScroll = clampScroll(
                        rightScroll + direction,
                        rightEntities.size()
                );
                return true;
            }
        }

        return super.universalMouseScrolled(
                mx,
                my,
                delta
        );
    }

    private void updateScrollFromMouse(
            double my,
            boolean left
    ) {
        int totalItems = left
                ? leftEntities.size()
                : rightEntities.size();

        int totalRows = rowCount(totalItems);

        int maxScroll = Math.max(
                0,
                totalRows - VISIBLE_ROWS
        );

        if (maxScroll > 0) {
            int thumbH = ScrollUtil.calculateThumbHeight(
                    GRID_H,
                    VISIBLE_ROWS,
                    totalRows,
                    20
            );

            int value = ScrollUtil.calculateScrollOffset(
                    my,
                    GRID_Y,
                    GRID_H,
                    thumbH,
                    maxScroll
            );

            if (left) {
                leftScroll = value;
            } else {
                rightScroll = value;
            }
        }
    }

    private int clampScroll(
            int value,
            int totalItems
    ) {
        int max = Math.max(
                0,
                rowCount(totalItems) - VISIBLE_ROWS
        );

        return Math.max(
                0,
                Math.min(value, max)
        );
    }

    private int rowCount(int itemCount) {
        return (itemCount + COLS - 1) / COLS;
    }

    @Override
    public void removed() {
        entityPreviewRenderer.clear();
        super.removed();
    }

    private record EntityEntryData(
            EntityType<?> type,
            ResourceLocation id,
            Component name,
            String searchData
    ) {
    }
}
