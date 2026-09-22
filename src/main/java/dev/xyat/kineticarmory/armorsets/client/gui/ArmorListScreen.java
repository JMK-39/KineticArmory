package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticcore.api.client.input.KineticMouseButtons;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothEntry;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothSelectionList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

public class ArmorListScreen extends KineticScreen {
    private static double lastScrollAmount = 0;
    private static String lastSearch = "";

    private final List<ArmorDataConfig> allEntries;
    private List<ArmorDataConfig> displayEntries;
    private final Set<String> pendingDeletedIds = new LinkedHashSet<>();

    private KineticEditBox searchBox;
    private SetListWidget listWidget;
    private int guiW, guiH, x0, y0;
    private final Screen parent;

    public ArmorListScreen() {
        this(null);
    }

    public ArmorListScreen(Screen parent) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.list.title"));
        setParentScreen(parent);
        this.parent = parent;

        this.allEntries = new ArrayList<>(ArmorClientSnapshot.configs());
        this.allEntries.sort(Comparator.comparing(a -> a.id));
        this.displayEntries = new ArrayList<>(this.allEntries);
        configureStandaloneDraft(
                () -> new LinkedHashSet<>(pendingDeletedIds),
                this::restorePendingDeletes
        );
    }

    @Override
    protected boolean handleCloseRequest() {
        discardDraft();
        return false;
    }

    @Override
    protected void buildUi() {
        int padding = 10;
        this.guiW = this.canvasWidth() - padding * 2;
        this.guiH = this.canvasHeight() - padding * 2;
        this.x0 = padding;
        this.y0 = padding;

        int btnW = 60;
        int filterBtnW = 120;
        int gap = 5;
        int btnBackX = x0 + guiW - padding - btnW;
        int btnSaveX = btnBackX - gap - btnW;
        int btnNewX = btnSaveX - gap - btnW;
        int btnFilterX = btnNewX - gap - filterBtnW;
        this.searchBox = addTextField(x0 + padding, y0 + 20, btnFilterX - gap - (x0 + padding), Component.empty());
        this.searchBox.setPlaceholder(ColorText.translatable("gui.kineticarmory.armorsets.search"));
        this.searchBox.setMaxLength(1024);
        this.searchBox.setValue(lastSearch);
        this.searchBox.setResponder(val -> { updateSearch(val); if (listWidget != null) listWidget.setScrollAmount(0); });
addButtonWithHandler(btnFilterX, y0 + 20, filterBtnW, getEntityFilterButtonText(), getEntityFilterButtonTooltip(), b ->
                ArmorNetwork.requestEntityFilter());
        addButtonWithHandler(btnNewX, y0 + 20, btnW, ColorText.translatable("gui.kineticarmory.armorsets.btn_new"), null, b -> createNewSet());
        addButtonWithHandler(btnSaveX, y0 + 20, btnW, ColorText.translatable("gui.kineticarmory.armorsets.save"), null, b -> savePendingDeletes());
        addButtonWithHandler(btnBackX, y0 + 20, btnW, ColorText.translatable("gui.kineticarmory.common.back"), null, b -> onClose());

        int listTop = y0 + 45;
        int listBottom = y0 + guiH - 2;
        int listLeft = x0 + padding;
        int listRight = x0 + guiW - 2;

        this.listWidget = new SetListWidget(listRight - listLeft, listBottom - listTop, listTop, listBottom, 46);
        this.listWidget.setLeftPos(listLeft);
        addSmoothSelectionList(this.listWidget);

        performSearchFilter(lastSearch);
        this.listWidget.refresh();
        this.listWidget.setScrollAmount(lastScrollAmount);
    }

    private static Component getEntityFilterButtonText() {
        return ColorText.translatable(
                "BLACKLIST".equalsIgnoreCase(ArmorClientSnapshot.entityFilterMode())
                        ? "gui.kineticarmory.armorsets.entity_filter.main_button.blacklist"
                        : "gui.kineticarmory.armorsets.entity_filter.main_button.whitelist"
        );
    }

    private static Component getEntityFilterButtonTooltip() {
        Component mode = ColorText.translatable(
                "BLACKLIST".equalsIgnoreCase(ArmorClientSnapshot.entityFilterMode())
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.state.blacklist"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.state.whitelist"
        );
        return ColorText.translatable("gui.kineticarmory.armorsets.entity_filter.main_button.tooltip", mode);
    }

    private void performSearchFilter(String q) {
        String query = q.toLowerCase(Locale.ROOT).trim();
        displayEntries = allEntries.stream().filter(e -> {
            String dName = e.displayName != null ? e.displayName.toLowerCase(Locale.ROOT) : "";
            String dId = e.id != null ? e.id.toLowerCase(Locale.ROOT) : "";
            String searchStr = dId + " " + dName + " " + KineticSearch.pinyin(dName);
            return KineticSearch.match(searchStr, query);
        }).collect(Collectors.toList());
    }

    private void updateSearch(String q) { performSearchFilter(q); if (listWidget != null) { listWidget.refresh(); } }

    public void refreshFromSnapshot() {
        rebuildEntriesFromSnapshot();
        updateSearch(searchBox == null ? lastSearch : searchBox.getValue());
    }

    private void rebuildEntriesFromSnapshot() {
        allEntries.clear();
        for (ArmorDataConfig config : ArmorClientSnapshot.configs()) {
            if (config != null && config.id != null && !pendingDeletedIds.contains(config.id)) {
                allEntries.add(config);
            }
        }
        allEntries.sort(Comparator.comparing(a -> a.id));
    }

    private void restorePendingDeletes(Set<String> deletedIds) {
        pendingDeletedIds.clear();
        if (deletedIds != null) pendingDeletedIds.addAll(deletedIds);
        rebuildEntriesFromSnapshot();
        updateSearch(searchBox == null ? lastSearch : searchBox.getValue());
    }

    @Override
    protected void canvasTick() {
        if (listWidget != null) lastScrollAmount = listWidget.getScrollAmount();
        if (searchBox != null) lastSearch = searchBox.getValue();
    }

    private void createNewSet() {
        ArmorDataConfig newSet = new ArmorDataConfig();
        newSet.id = "set_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        newSet.displayName = ""; newSet.initNullFields();
        allEntries.add(newSet); ArmorClientSnapshot.put(newSet);
        updateSearch(searchBox.getValue());
        if (listWidget != null) { listWidget.setScrollAmount(listWidget.getMaxScroll()); lastScrollAmount = listWidget.getMaxScroll(); }
        openEditor(newSet);
    }

    private void openEditor(ArmorDataConfig config) { KineticClientRuntime.openScreen(new ArmorEditScreen(this, config)); }

    private void deleteSet(ArmorDataConfig config) {
        if (config == null || config.id == null || config.id.isBlank()) return;
        pendingDeletedIds.add(config.id);
        allEntries.removeIf(entry -> config.id.equals(entry.id));
        updateSearch(searchBox == null ? lastSearch : searchBox.getValue());
    }

    private void savePendingDeletes() {
        if (!pendingDeletedIds.isEmpty()) {
            for (String id : new ArrayList<>(pendingDeletedIds)) {
                ArmorNetwork.deleteArmorSet(id);
                ArmorClientSnapshot.remove(id);
            }
            pendingDeletedIds.clear();
            rebuildEntriesFromSnapshot();
            updateSearch(searchBox == null ? lastSearch : searchBox.getValue());
        }
        commitDraft();
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        GuiTheme.panel(g, x0, y0, guiW, guiH);
        KineticText.drawScrollingCentered(
                g, this.font, this.title, this.canvasWidth() / 2, y0 + 6, Math.max(0, guiW - 20), 0xFFFFFF, false
        );
        renderSmoothSelectionList(listWidget, g, mx, my, pt);
    }

    class SetListWidget extends SmoothSelectionList<SetListWidget.Entry> {
        public SetListWidget(int w, int h, int t, int b, int ih) {
            super(w, h, t, b, ih);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            refresh();
        }

        public void refresh() {
            clearEntries();
            displayEntries.forEach(e -> addEntry(new Entry(e)));
        }

        @Override
        public int getRowWidth() {
            return this.width - 8;
        }

        @Override
        public int getRowLeft() {
            return this.getLeft();
        }

        class Entry extends SmoothEntry<Entry> {
            private static final int DELETE_BUTTON_W = 52;
            private static final int DELETE_BUTTON_H = KineticScreen.STANDARD_CONTROL_HEIGHT;

            private final ArmorDataConfig data;
            private final StateButton deleteButton;
            private boolean deleteConfirm = false;
            private long confirmTime = 0;
            private int lastTop = 0;

            public Entry(ArmorDataConfig data) {
                this.data = data;
                this.deleteButton = KineticWidgets.createCompactButton(0, 0, DELETE_BUTTON_W, ColorText.translatable("gui.kineticarmory.armorsets.btn_delete"), null, this::handleDelete);
            }

            @Override
            public void render(@NotNull GuiGraphics g, int idx, int top, int left, int w, int h, int mx, int my, boolean hv, float pt) {
                this.lastTop = top;
                GuiTheme.stateSurface(
                        g,
                        left,
                        top,
                        w,
                        44,
                        GuiTheme.Surface.PANEL_ALT,
                        false,
                        hv,
                        false
                );

                String dName = (data.displayName != null && !data.displayName.isEmpty()) ? data.displayName : "!!! NO DISPLAY NAME !!!";
                String dId = data.id != null ? data.id : "unknown";

                int textMaxW = 175;
                Component idStr = Component.literal("ID: " + dId);

                KineticText.drawScrollingLeft(
                        g, KineticClientRuntime.font(), Component.literal(dName), left + 5, top + 5, textMaxW, 0xFFFF55, false
                );
                KineticText.drawScrollingLeft(
                        g, KineticClientRuntime.font(), idStr, left + 5, top + 17, textMaxW, 0xAAAAAA, false
                );

                String infoStrRaw = getInfo();
                int maxInfoW = textMaxW + 10;
                KineticText.drawScrollingLeft(
                        g, KineticClientRuntime.font(), Component.literal(infoStrRaw), left + 5, top + 29, maxInfoW, 0x55FF55, false
                );

                int delX = left + w - DELETE_BUTTON_W - 4;
                int delY = top + (44 - DELETE_BUTTON_H) / 2;
                if (deleteConfirm && System.currentTimeMillis() - confirmTime > 3000) {
                    deleteConfirm = false;
                }
                deleteButton.setText(ColorText.translatable(deleteConfirm
                        ? "gui.kineticarmory.armorsets.btn_delete_confirm"
                        : "gui.kineticarmory.armorsets.btn_delete"));
                deleteButton.setX(delX);
                deleteButton.setY(delY);
                deleteButton.render(g, mx, my, pt);

                int labelX = left + 185;
                int labelWidth = 70;
                int curioY = top + 4;
                int equipY = top + 24;

                Component curioLabel = ColorText.translatable("gui.kineticarmory.armorsets.label.curios");
                KineticText.drawScrollingLeft(
                        g, KineticClientRuntime.font(), curioLabel, labelX, curioY + 4, labelWidth, 0xAAAAAA, false
                );
                int curioStartX = labelX + labelWidth + 4;

                int equipLabelX = labelX + 18;
                Component equipLabel = ColorText.translatable("gui.kineticarmory.armorsets.label.equipment");
                KineticText.drawScrollingLeft(
                        g, KineticClientRuntime.font(), equipLabel, equipLabelX, equipY + 4, labelWidth, 0xAAAAAA, false
                );
                int equipStartX = equipLabelX + labelWidth + 4;

                int maxIconsCurio = Math.max(0, (left + w - 4 - curioStartX) / 18);
                int maxIconsEquip = Math.max(0, (delX - 4 - equipStartX) / 18);

                if (data.curios != null) {
                    int drawn = 0;
                    for (ArmorDataConfig.ItemReq req : data.curios) {
                        if (drawn >= maxIconsCurio) break;
                        if (req != null && !req.id.equals("minecraft:air") && !req.id.equals("EMPTY") && !req.id.equals("ANY")) {
                            var rl = KineticResourceIds.tryParse(req.id);
                            if (rl != null) {
                                Item item = KineticRegistries.items().get(rl);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    g.renderItem(new ItemStack(item), curioStartX + drawn * 18, curioY);
                                    drawn++;
                                }
                            }
                        }
                    }
                }

                data.normalizeEquipmentVariants();
                if (data.equipmentVariants != null) {
                    String[] slots = {"head", "chest", "legs", "feet", "mainhand", "offhand"};
                    int drawn = 0;
                    for (String slot : slots) {
                        if (drawn >= maxIconsEquip) break;
                        ArmorDataConfig.ItemReq req = data.getDisplayedEquipmentReq(slot, System.currentTimeMillis());
                        if (req != null && req.id != null && !req.id.equals("minecraft:air") && !req.id.equals("EMPTY") && !req.id.equals("ANY")) {
                            var rl = KineticResourceIds.tryParse(req.id);
                            if (rl != null) {
                                Item item = KineticRegistries.items().get(rl);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    int iconX = equipStartX + drawn * 18;
                                    g.renderItem(new ItemStack(item), iconX, equipY);
                                    if (data.hasMultipleEquipmentVariants(slot)) {
                                        GuiTheme.indicatorFill(g, iconX + 10, equipY + 10, 8, 8, GuiTheme.Indicator.SUCCESS, 0.80F);
                                        g.drawString(KineticClientRuntime.font(), "+", iconX + 12, equipY + 9, 0xFF55FF55, false);
                                    }
                                    drawn++;
                                }
                            }
                        }
                    }
                }
            }

            private @NotNull String getInfo() {
                int attr = data.attributes == null ? 0 : data.attributes.size();
                int pot = data.potionEffects == null ? 0 : data.potionEffects.size();
                int imm = data.damageImmunities == null ? 0 : data.damageImmunities.size();
                int atk = data.attackEffects == null ? 0 : data.attackEffects.size();
                int pImm = data.effectImmunities == null ? 0 : data.effectImmunities.size();
                int conv = data.damageConversions == null ? 0 : data.damageConversions.size();
                int dmgRed = data.damageMultipliers == null ? 0 : data.damageMultipliers.size();
                int atkDmg = data.attackDamageMultipliers == null ? 0 : data.attackDamageMultipliers.size();
                String flightStr = ColorText.translatable(data.allowFlight ? "gui.kineticarmory.armorsets.info.yes" : "gui.kineticarmory.armorsets.info.no").getString();

                return ColorText.translatable("gui.kineticarmory.armorsets.info.summary_detailed",
                        attr, pot, pImm, imm, atk, atkDmg, dmgRed, conv, flightStr).getString();
            }

            private void handleDelete() {
                if (deleteConfirm) {
                    deleteSet(data);
                    return;
                }
                deleteConfirm = true;
                confirmTime = System.currentTimeMillis();
                deleteButton.setText(ColorText.translatable("gui.kineticarmory.armorsets.btn_delete_confirm"));
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (deleteButton.mouseClicked(mx, my, btn)) return true;
                if (KineticMouseButtons.isPrimary(btn) && my >= lastTop && my < lastTop + 44) {
                    openEditor(data);
                    return true;
                }
                return false;
            }
            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
