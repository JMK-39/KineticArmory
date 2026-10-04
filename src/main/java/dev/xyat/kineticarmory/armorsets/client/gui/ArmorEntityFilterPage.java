package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ArmorEntityFilterPage extends KineticPage {
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
    // Panel headings end before the same-row filtered-action button.
    private static final int FILTER_ACTION_RIGHT_OFFSET = 80;
    private static final int TEXT_GAP = 4;
    private static int rotationSpeedPercent = 100;
    private static boolean clockwiseRotation = true;

    
    private final ArmorDataConfig armorSet;
    private final boolean global;
    private final Set<String> selectedIds = new HashSet<>();
    private final List<String> originalRules = new ArrayList<>();
    private final List<EntityEntryData> allEntities = new ArrayList<>();
    private final Set<String> knownEntityIds = new HashSet<>();
    private final List<EntityEntryData> leftEntities = new ArrayList<>();
    private final List<EntityEntryData> rightEntities = new ArrayList<>();
    private final KineticEntityPreview entityPreviewRenderer = KineticEntityPreview.create();
    private final KineticScrollController leftBar = new KineticScrollController();
    private final KineticScrollController rightBar = new KineticScrollController();

    private KineticTextField leftSearchBox;
    private KineticTextField rotationSpeedBox;
    private KineticTextField rightSearchBox;
    private String globalMode;
    private boolean setFilterEnabled;
    private boolean rulesDirty;
    private int leftScroll;
    private int rightScroll;
    private boolean draggingLeftScroll;
    private boolean draggingRightScroll;
    private List<Component> deferredTooltip;

    public ArmorEntityFilterPage() {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.global_title"));
        this.armorSet = null;
        this.global = true;
        this.globalMode = ArmorClientSnapshot.entityFilterMode();
        initEntities();
        loadRules(ArmorClientSnapshot.entityFilterRules());
        sortEntities();
        refreshLists();
        setupScale();
    }

    public ArmorEntityFilterPage(ArmorDataConfig armorSet) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_title"));
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
entityPreviewRenderer.setRotationSpeedPercent(rotationSpeedPercent);
        entityPreviewRenderer.setClockwise(clockwiseRotation);
    }

    private void initEntities() {
        for (EntityType<?> type : KineticRegistries.entityTypes().values()) {
            if (type.getCategory() == MobCategory.MISC) continue;
            addEntityEntry(type);
        }
    }

    private void addEntityEntry(EntityType<?> type) {
        ResourceLocation id = KineticRegistries.entityTypes().id(type);
        if (id == null || !knownEntityIds.add(id.toString())) return;

        Component name = KineticI18n.translatable(type.getDescriptionId());
        String displayName = name.getString();
        String searchData = id + " " + displayName + " " + KineticSearch.pinyin(displayName);

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

                for (EntityType<?> type : KineticRegistries.entityTypes().values()) {
                    ResourceLocation id = KineticRegistries.entityTypes().id(type);

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

                ResourceLocation tagId = KineticResourceIds.tryParse(rule.substring(1).trim());
                if (tagId == null) continue;

                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);

                for (EntityType<?> type : KineticRegistries.entityTypes().valuesInTag(tag)) {
                    if (type.getCategory() == MobCategory.MISC) continue;
                    addEntityEntry(type);

                    ResourceLocation id = KineticRegistries.entityTypes().id(type);
                    if (id != null) {
                        selectedIds.add(id.toString());
                    }
                }

                continue;
            }

            ResourceLocation configuredId = KineticResourceIds.tryParse(rule);
            if (configuredId == null) continue;

            EntityType<?> type = KineticRegistries.entityTypes().get(configuredId);
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

            ResourceLocation id = KineticResourceIds.tryParse(rawId);
            if (id == null) continue;

            EntityType<?> type = KineticRegistries.entityTypes().get(id);

            if (type != null) {
                addEntityEntry(type);
            } else {
                addMissingConfiguredEntry(id);
            }
        }
    }

    @Override
    protected void build(KineticUi ui) {
        int topY = 18;
        int modeW = 142;
        int speedButtonW = 72;
        int speedInputW = 52;
        int saveW = 64;
        int backW = 64;

        if (global) {
            ui().button(8, topY, modeW).text(getGlobalModeText()).tooltip(getGlobalModeTooltip()).onClick(b -> {
                        globalMode = "WHITELIST".equalsIgnoreCase(globalMode)
                                ? "BLACKLIST"
                                : "WHITELIST";

                        b.setText(getGlobalModeText());
                        b.setTooltip(getGlobalModeTooltip());
                    }).build();
} else {
            ui().button(8, topY, modeW).text(getSetFilterText()).tooltip(getSetFilterTooltip()).onClick(b -> {
                        setFilterEnabled = !setFilterEnabled;
                        b.setText(getSetFilterText());
                        b.setTooltip(getSetFilterTooltip());
                    }).build();
}

        rotationSpeedBox = ui().textField(432, topY, speedInputW).label(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rotation_speed")).firstShownTextAsDefault().build();
        rotationSpeedBox.limitTextLength(3);
        rotationSpeedBox.filterText(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        rotationSpeedBox.setTextValue(Integer.toString(rotationSpeedPercent));
        rotationSpeedBox.onTextChange(value -> applyRotationSpeedInput(false));
        rotationSpeedBox.setTooltip(KineticI18n.translatable(
                "gui.kineticarmory.armorsets.entity_filter.rotation_speed.tooltip"
        ));
ui().button(356, topY, speedButtonW).text(getRotationDirectionText()).tooltip(KineticI18n.translatable(
                                "gui.kineticarmory.common.rotation.direction.tooltip"
                        )).onClick(b -> {
                                    clockwiseRotation = !clockwiseRotation;
                                    entityPreviewRenderer.setClockwise(clockwiseRotation);
                                    b.setText(getRotationDirectionText());
                                }).build();

        ui().button(500, topY, saveW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).tooltip(KineticI18n.translatable(
                                global
                                        ? "gui.kineticarmory.armorsets.entity_filter.save_global.tooltip"
                                        : "gui.kineticarmory.armorsets.entity_filter.save_set.tooltip"
                        )).onClick(b -> save()).build();

        ui().button(568, topY, backW).text(KineticI18n.translatable("gui.kineticarmory.common.back")).tooltip(KineticI18n.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.back.tooltip"
                        )).onClick(b -> backWithoutSave()).build();

        ui().button(LEFT_X + PANEL_W - FILTER_ACTION_RIGHT_OFFSET, PANEL_Y + 4, 72).text(KineticI18n.translatable(
                                        "gui.kineticarmory.armorsets.entity_filter.remove_filtered"
                                )).tooltip(KineticI18n.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.remove_filtered.tooltip"
                        )).onClick(b -> removeFiltered()).build();

        ui().button(RIGHT_X + PANEL_W - FILTER_ACTION_RIGHT_OFFSET, PANEL_Y + 4, 72).text(KineticI18n.translatable(
                                        "gui.kineticarmory.armorsets.entity_filter.add_filtered"
                                )).tooltip(KineticI18n.translatable(
                                "gui.kineticarmory.armorsets.entity_filter.add_filtered.tooltip"
                        )).onClick(b -> addFiltered()).build();

        leftSearchBox = ui().textField(LEFT_X + 8, SEARCH_Y, PANEL_W - 16).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.search_available")).firstShownTextAsDefault().build();

        leftSearchBox.limitTextLength(256);
        leftSearchBox.onTextChange(value -> {
            leftScroll = 0;
            refreshLists();
        });
rightSearchBox = ui().textField(RIGHT_X + 8, SEARCH_Y, PANEL_W - 16).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.search_added")).firstShownTextAsDefault().build();

        rightSearchBox.limitTextLength(256);
        rightSearchBox.onTextChange(value -> {
            rightScroll = 0;
            refreshLists();
        });
refreshLists();
    }

    private Component getRotationDirectionText() {
        return KineticI18n.translatable(
                clockwiseRotation
                        ? "gui.kineticarmory.common.rotation.clockwise"
                        : "gui.kineticarmory.common.rotation.counterclockwise"
        );
    }

    private boolean applyRotationSpeedInput(boolean notifyInvalid) {
        if (rotationSpeedBox == null) return true;

        String raw = rotationSpeedBox.textValue().trim();
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
            KineticOverlays.toast(
                    "armorsets_rotation_speed_invalid",
                    KineticI18n.translatable("msg.kineticarmory.armorsets.rotation_speed.invalid")
            );
        }
        return false;
    }

    private Component getGlobalModeText() {
        return KineticI18n.translatable(
                "WHITELIST".equalsIgnoreCase(globalMode)
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.button.whitelist"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.button.blacklist"
        );
    }

    private Component getGlobalModeTooltip() {
        return KineticI18n.translatable(
                "WHITELIST".equalsIgnoreCase(globalMode)
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.whitelist.tooltip"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.blacklist.tooltip"
        );
    }

    private Component getSetFilterText() {
        return KineticI18n.translatable(
                setFilterEnabled
                        ? "gui.kineticarmory.armorsets.entity_filter.set_toggle.button.enabled"
                        : "gui.kineticarmory.armorsets.entity_filter.set_toggle.button.disabled"
        );
    }

    private Component getSetFilterTooltip() {
        return KineticI18n.translatable(
                setFilterEnabled
                        ? "gui.kineticarmory.armorsets.entity_filter.set_toggle.enabled.tooltip"
                        : "gui.kineticarmory.armorsets.entity_filter.set_toggle.disabled.tooltip"
        );
    }

    private Component getLeftTitle() {
        if (!global) {
            return KineticI18n.translatable(
                    "gui.kineticarmory.armorsets.entity_filter.left_set",
                    selectedIds.size()
            );
        }

        return KineticI18n.translatable(
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
                : leftSearchBox.textValue();

        String rightQuery = rightSearchBox == null
                ? ""
                : rightSearchBox.textValue();

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

        return KineticSearch.match(data.searchData(), query);
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

    private void save() {
        if (!applyRotationSpeedInput(true)) return;

        List<String> rules = buildRules();

        if (global) {
            ArmorClientSnapshot.replaceEntityFilter(globalMode, rules);

            ArmorNetwork.saveEntityFilter(globalMode, rules);
        } else if (armorSet != null) {
            armorSet.entityWhitelistEnabled = setFilterEnabled;
            armorSet.allowedEntityTypes = rules;
            armorSet.prepareRuntimeCache();
        }

        originalRules.clear();
        originalRules.addAll(rules);
        rulesDirty = false;
    }

    private void backWithoutSave() {
        navigateBack();
    }

    @Override
    protected void renderBackground(KineticGraphics g,
            int mx,
            int my,
            float pt
    ) {
        deferredTooltip = null;

        KineticTheme.panel(g, 0, 0, V_WIDTH, V_HEIGHT);

        drawCenteredNoShadow(
                g,
                title(),
                V_WIDTH / 2,
                6,
                V_WIDTH - LEFT_X * 2,
                0xFFFFFFFF
        );

        drawPanel(g, LEFT_X, getLeftTitle());

        drawPanel(
                g,
                RIGHT_X,
                KineticI18n.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.right_available",
                        availableCount()
                )
        );
    }

    private void drawPanel(
            KineticGraphics g,
            int x,
            Component title
    ) {
        KineticTheme.panelAlt(g, x, PANEL_Y, PANEL_W, PANEL_H);

        g.scrollingText(title, x + 8, PANEL_Y + 9, PANEL_W - FILTER_ACTION_RIGHT_OFFSET - 8 - TEXT_GAP, 0xFFFFFFFF, false);

        KineticTheme.panel(g, x + 8, GRID_Y - 2, GRID_W, GRID_H + 4);
    }

    @Override
    protected void renderForeground(KineticGraphics g,
            int mx,
            int my,
            float pt
    ) {
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

        renderScrollbar(g, mx, my, LEFT_X + PANEL_W - 7, true);

        renderScrollbar(g, mx, my, RIGHT_X + PANEL_W - 7, false);

        Component status = global
                ? KineticI18n.translatable(
                "gui.kineticarmory.armorsets.entity_filter.global_status",
                selectedIds.size()
        )
                : KineticI18n.translatable(
                "gui.kineticarmory.armorsets.entity_filter.set_status",
                selectedIds.size()
        );

        drawCenteredNoShadow(
                g,
                status,
                V_WIDTH / 2,
                344,
                V_WIDTH - LEFT_X * 2,
                0xFFFFFFFF
        );

        if (deferredTooltip != null
                && !deferredTooltip.isEmpty()) {
            showTooltip(deferredTooltip);
        }
    }

    private void renderGrid(
            KineticGraphics g,
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

            KineticTheme.surface(
                    g,
                    x + 2,
                    y + 2,
                    CELL_SIZE - 4,
                    CELL_SIZE - 4,
                    addedSide ? KineticTheme.Surface.PANEL : KineticTheme.Surface.PANEL_ALT
            );
            if (hovered) {
                KineticTheme.stateOutline(g, x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2, false, true, false);
            } else {
                KineticTheme.indicatorOutline(
                        g,
                        x + 1,
                        y + 1,
                        CELL_SIZE - 2,
                        CELL_SIZE - 2,
                        addedSide ? KineticTheme.Indicator.SUCCESS : KineticTheme.Indicator.MUTED
                );
            }

            KineticEntityPreview.drawCheckerboard(
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

                tooltip.add(KineticI18n.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.name",
                        data.name()
                ));

                tooltip.add(KineticI18n.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.id",
                        data.id().toString()
                ));

                tooltip.add(KineticI18n.translatable(
                        "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.zoom",
                        getModelZoomPercent(data.id().toString())
                ));

                if (data.type() == null) {
                    tooltip.add(KineticI18n.translatable(
                            "gui.kineticarmory.armorsets.entity_filter.entity.tooltip.invalid"
                    ));
                }

                tooltip.add(KineticI18n.translatable(
                        addedSide
                                ? "gui.kineticarmory.armorsets.entity_filter.click_remove"
                                : "gui.kineticarmory.armorsets.entity_filter.click_add"
                ));

                deferredTooltip = tooltip;
            }
        }
    }

    private void renderScrollbar(
            KineticGraphics g,
            int mouseX,
            int mouseY,
            int x,
            boolean left
    ) {
        syncBar(left).render(g, mouseX, mouseY, x, GRID_Y, 4, GRID_H, 20);
    }

    /**
     * 行偏移仍以 leftScroll/rightScroll 为准，滚动条控制器只负责绘制与拖拽。
     * leftScroll/rightScroll stay the source of truth; the controllers only draw and drag the scrollbar.
     */
    private KineticScrollController syncBar(boolean left) {
        KineticScrollController bar = left ? leftBar : rightBar;
        bar.update(rowCount(left ? leftEntities.size() : rightEntities.size()), VISIBLE_ROWS);
        int value = left ? leftScroll : rightScroll;
        if (bar.offset() != value) bar.setOffset(value);
        return bar;
    }

    private void renderAdaptiveEntity(
            KineticGraphics g,
            int boxX,
            int boxY,
            EntityEntryData data,
            boolean hovered
    ) {
        final int boxW = CELL_SIZE - 8;
        final int boxH = CELL_SIZE - 8;

        boolean rendered = entityPreviewRenderer.render(
                g,
                data.id().toString(),
                data.id().toString(),
                boxX,
                boxY,
                boxW,
                boxH,
                hovered
        );

        if (!rendered) {
            drawCenteredNoShadow(
                    g,
                    KineticI18n.translatable(
                            "gui.kineticarmory.armorsets.entity_filter.preview_unavailable"
                    ),
                    boxX + boxW / 2,
                    boxY + boxH / 2 - 4,
                    boxW - TEXT_GAP * 2,
                    0xFFFF7777
            );
        }
    }

    private int getModelZoomPercent(String id) {
        return entityPreviewRenderer.zoomPercent(id);
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
            KineticGraphics g,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color
    ) {
        g.scrollingTextCentered(text, centerX, y, maxWidth, color, false);
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        boolean handled = false;

        if (handled) return true;
        if (!KineticMouseButtons.isPrimary(btn)) return false;

        if (handleScrollbarClick(mx, my, input.button(), true)) {
            return true;
        }

        if (handleScrollbarClick(mx, my, input.button(), false)) {
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
            clearFocus();
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
            clearFocus();
            return true;
        }

        return false;
    }

    private boolean handleScrollbarClick(
            double mx,
            double my,
            MouseButton button,
            boolean left
    ) {
        int x = (left ? LEFT_X : RIGHT_X) + PANEL_W - 7;
        KineticScrollController bar = syncBar(left);
        if (!bar.beginDrag(mx, my, button, x, GRID_Y, 4, GRID_H, 20, 0)) {
            return false;
        }
        if (left) {
            draggingLeftScroll = true;
            leftScroll = bar.offset();
        } else {
            draggingRightScroll = true;
            rightScroll = bar.offset();
        }
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
    protected boolean onMouseDrag(MouseDragInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton(); double dx = input.deltaX(), dy = input.deltaY();
        if (KineticMouseButtons.isPrimary(btn) && draggingLeftScroll) {
            updateScrollFromMouse(
                    my,
                    true
            );
            return true;
        }

        if (KineticMouseButtons.isPrimary(btn) && draggingRightScroll) {
            updateScrollFromMouse(
                    my,
                    false
            );
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        double mx = input.x(), my = input.y(); int btn = input.rawButton();
        if (KineticMouseButtons.isPrimary(btn)
                && (draggingLeftScroll
                || draggingRightScroll)) {
            draggingLeftScroll = false;
            draggingRightScroll = false;
            leftBar.release(input.button());
            rightBar.release(input.button());
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        double mx = input.x(), my = input.y(), delta = input.deltaY();
        if (KineticClientRuntime.controlModifierDown()) {
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

        return false;
    }

    private void updateScrollFromMouse(
            double my,
            boolean left
    ) {
        KineticScrollController bar = left ? leftBar : rightBar;
        bar.drag(my, GRID_Y, GRID_H, 20);
        if (left) {
            leftScroll = bar.offset();
        } else {
            rightScroll = bar.offset();
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
    protected void onRemoved() {
        entityPreviewRenderer.clear();
    }

    private record EntityEntryData(
            EntityType<?> type,
            ResourceLocation id,
            Component name,
            String searchData
    ) {
    }
}
