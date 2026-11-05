package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTextField;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticEntityGrid;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** One native entity grid; raw group and unknown rules remain editable independently of the model cards. */
public class ArmorEntityFilterPage extends KineticPage {
    private final ArmorDataConfig armorSet;
    private final boolean global;
    private final LinkedHashSet<String> rules = new LinkedHashSet<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final LinkedHashMap<String, EntityEntryData> allEntities = new LinkedHashMap<>();
    private final List<EntityEntryData> displayEntities = new ArrayList<>();
    private KineticTextField searchBox;
    private KineticEntityGrid entityGrid;
    private String globalMode;
    private boolean setFilterEnabled;
    private String selectedEntityId;

    public ArmorEntityFilterPage() {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.global_title"));
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);
        armorSet = null; global = true;
        globalMode = ArmorClientSnapshot.entityFilterMode();
        init(ArmorClientSnapshot.entityFilterRules());
    }

    public ArmorEntityFilterPage(ArmorDataConfig armorSet) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.set_title"));
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);
        this.armorSet = armorSet; global = false; globalMode = "WHITELIST";
        setFilterEnabled = armorSet.entityWhitelistEnabled;
        init(armorSet.allowedEntityTypes);
    }

    private void init(List<String> initial) {
        for (EntityType<?> type : KineticRegistries.entityTypes().values()) {
            if (type.getCategory() != MobCategory.MISC) addEntity(type);
        }
        if (initial != null) for (String raw : initial) {
            if (raw != null && !raw.isBlank()) rules.add(raw.trim());
        }
        ensureConfiguredEntries();
    }

    private void addEntity(EntityType<?> type) {
        var id = KineticRegistries.entityTypes().id(type);
        if (id == null) return;
        Component name = type.getDescription();
        allEntities.putIfAbsent(id.toString(), new EntityEntryData(type, id, name,
                (id + " " + name.getString() + " " + KineticSearch.pinyin(name.getString())).toLowerCase(Locale.ROOT)));
    }

    private void ensureConfiguredEntries() {
        for (String raw : rules) {
            if (raw.startsWith("#") || raw.startsWith("@") || raw.equalsIgnoreCase("ALL")) continue;
            var id = KineticResourceIds.tryParse(raw);
            if (id == null || allEntities.containsKey(id.toString())) continue;
            var type = KineticRegistries.entityTypes().get(id);
            if (type != null) addEntity(type);
            else allEntities.put(id.toString(), new EntityEntryData(null, id, Component.literal(raw), raw.toLowerCase(Locale.ROOT)));
        }
    }

    @Override protected void build(KineticUi ui) {
        ui.button(4, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.common.back"))
                .tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.back.tooltip"))
                .onClick(this::backWithoutSave).build();
        searchBox = ui.textField(48, 4, 416).placeholder(KineticI18n.translatable("gui.kineticarmory.armorsets.search")).build();
        searchBox.limitTextLength(512);
        searchBox.onTextChange(value -> { entityGrid.setScrollOffset(0); refreshLists(); });
        ui.button(468, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rules"))
                .tooltip(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rules.tooltip"))
                .onClick(this::openRules).build();
        ui.button(512, 4, 80).text(modeText()).tooltip(modeTooltip()).onClick(button -> {
            if (global) globalMode = "WHITELIST".equalsIgnoreCase(globalMode) ? "BLACKLIST" : "WHITELIST";
            else setFilterEnabled = !setFilterEnabled;
            button.setText(modeText());button.setTooltip(modeTooltip());
        }).build();
        ui.button(596, 4, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save"))
                .tooltip(KineticI18n.translatable(global ? "gui.kineticarmory.armorsets.entity_filter.save_global.tooltip"
                        : "gui.kineticarmory.armorsets.entity_filter.save_set.tooltip"))
                .onClick(this::save).build();
        entityGrid = ui.add(new KineticEntityGrid(4, 28, 632, 328));
        bindDeleteAction(() -> selectedEntityId != null && selectedIds.contains(selectedEntityId),
                () -> toggleEntity(selectedEntityId));
        entityGrid.onClick((index, input) -> {
            String id = entityGrid.items().get(index).entityId();
            selectedEntityId = id;
            if (input.isLeft()) toggleEntity(id);
            else if (input.isRight() && selectedIds.contains(id)) toggleEntity(id);
        });
        refreshLists();
    }

    private Component modeText() {
        String key = global ? ("WHITELIST".equalsIgnoreCase(globalMode) ? "mode.button.whitelist" : "mode.button.blacklist")
                : (setFilterEnabled ? "set_toggle.button.enabled" : "set_toggle.button.disabled");
        return KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter." + key);
    }
    private Component modeTooltip() {
        String key = global ? ("WHITELIST".equalsIgnoreCase(globalMode) ? "mode.whitelist.tooltip" : "mode.blacklist.tooltip")
                : (setFilterEnabled ? "set_toggle.enabled.tooltip" : "set_toggle.disabled.tooltip");
        return KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter." + key);
    }

    private boolean ruleMatches(String rule, EntityEntryData entry) {
        if (rule.equalsIgnoreCase("ALL")) return true;
        if (rule.startsWith("@")) return entry.id().getNamespace().equalsIgnoreCase(rule.substring(1).trim());
        if (rule.startsWith("#")) {
            var id = KineticResourceIds.tryParse(rule.substring(1).trim());
            return id != null && entry.type() != null && entry.type().builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, id));
        }
        return rule.equals(entry.id().toString());
    }

    private void refreshLists() {
        ensureConfiguredEntries();
        selectedIds.clear(); displayEntities.clear();
        String query = searchBox == null ? "" : searchBox.textValue().trim().toLowerCase(Locale.ROOT);
        for (var entry : allEntities.values()) {
            if (rules.stream().anyMatch(rule -> ruleMatches(rule, entry))) selectedIds.add(entry.id().toString());
            boolean matches;
            if (query.startsWith("@")) matches = entry.id().getNamespace().contains(query.substring(1));
            else if (query.startsWith("#")) matches = entry.type() != null && entry.type().builtInRegistryHolder().tags()
                    .anyMatch(tag -> tag.location().toString().toLowerCase(Locale.ROOT).contains(query.substring(1)));
            else matches = query.isEmpty() || KineticSearch.match(entry.searchData(), query);
            if (matches) displayEntities.add(entry);
        }
        displayEntities.sort(Comparator.comparing((EntityEntryData entry) -> !selectedIds.contains(entry.id().toString()))
                .thenComparing(entry -> entry.id().toString()));
        if (entityGrid != null) entityGrid.setItems(displayEntities.stream().map(entry -> new KineticEntityGrid.Entry(
                entry.id().toString(), entry.name().copy().append("\n").append(entry.id().toString()).append("\n")
                        .append(KineticI18n.translatable(selectedIds.contains(entry.id().toString())
                                ? "gui.kineticarmory.armorsets.entity_filter.click_remove" : "gui.kineticarmory.armorsets.entity_filter.click_add")),
                selectedIds.contains(entry.id().toString()))).toList());
    }

    private void toggleEntity(String id) {
        if (selectedIds.contains(id)) {
            var entry = allEntities.get(id);
            // Removing one member of a group expands that affected group to its remaining members.
            // Other tags, namespaces and unresolved rules stay authored as they were.
            Set<String> remaining = new LinkedHashSet<>(selectedIds); remaining.remove(id);
            rules.removeIf(rule -> ruleMatches(rule, entry));
            for (String selected : remaining) {
                var other = allEntities.get(selected);
                if (rules.stream().noneMatch(rule -> ruleMatches(rule, other))) rules.add(selected);
            }
        } else rules.add(id);
        refreshLists();
    }

    private void addRule(String value) {
        String normalized = normalizedRule(value);
        if (normalized != null) rules.add(normalized);
        refreshLists();
    }
    private static String normalizedRule(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.equalsIgnoreCase("ALL")) return "ALL";
        if (value.startsWith("@")) return value.substring(1).matches("[a-z0-9_.-]+") ? value : null;
        boolean tag = value.startsWith("#");
        var id = KineticResourceIds.tryParse(tag ? value.substring(1) : value);
        return id == null ? null : (tag ? "#" : "") + id;
    }
    private void removeRule(String value) { rules.remove(value); refreshLists(); }
    private void addFiltered() {
        for (var entry : displayEntities) if (!selectedIds.contains(entry.id().toString())) rules.add(entry.id().toString());
        refreshLists();
    }
    private void removeFiltered() {
        var ids = displayEntities.stream().map(entry -> entry.id().toString()).toList();
        for (String id : ids) if (selectedIds.contains(id)) toggleEntity(id);
    }
    private void openRules() {
        List<KineticOverlays.MenuItem> items = new ArrayList<>();
        if (!displayEntities.isEmpty()) {
            items.add(KineticOverlays.MenuItem.action(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.add_filtered"), this::addFiltered));
            items.add(KineticOverlays.MenuItem.action(KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.remove_filtered"), this::removeFiltered));
        }
        String query = searchBox.textValue().trim();
        if (normalizedRule(query) != null) items.add(KineticOverlays.MenuItem.action(
                KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rule.add", query), () -> addRule(query)));
        for (String rule : rules) items.add(KineticOverlays.MenuItem.action(
                KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rule.remove", rule), () -> { removeRule(rule); openRules(); }));
        if (items.isEmpty()) items.add(KineticOverlays.MenuItem.action(
                KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.rules.empty"), () -> {}));
        openContextMenu(402, 26, items, 234);
    }

    private List<String> buildRules() { return new ArrayList<>(rules); }
    private void save() {
        List<String> selected = buildRules();
        if (global) {
            ArmorClientSnapshot.replaceEntityFilter(globalMode, selected);
            ArmorNetwork.saveEntityFilter(globalMode, selected);
        } else if (armorSet != null) {
            armorSet.entityWhitelistEnabled = setFilterEnabled;
            armorSet.allowedEntityTypes = selected;
            armorSet.prepareRuntimeCache();
        }
    }
    private void backWithoutSave() { navigateBack(); }
    @Override protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KineticTheme.panel(graphics, 0, 0, width(), height());
    }
    private record EntityEntryData(EntityType<?> type, ResourceLocation id, Component name, String searchData) {}
}
