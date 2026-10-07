package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;

import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

public class ArmorListPage extends KineticPage {
    private static int lastScrollOffset = 0;
    private static String lastSearch = "";

    private final List<ArmorDataConfig> allEntries;
    private List<ArmorDataConfig> displayEntries;
    private final Set<String> pendingDeletedIds = new LinkedHashSet<>();

    private KineticTextField searchBox;
    private SetListWidget listWidget;
    private int guiW, guiH, x0, y0;
    

    public ArmorListPage() {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.list.title"));

        this.allEntries = new ArrayList<>(ArmorClientSnapshot.configs());
        this.allEntries.sort(Comparator.comparing(a -> a.id));
        this.displayEntries = new ArrayList<>(this.allEntries);
        configureStandaloneDraft(
                () -> new LinkedHashSet<>(pendingDeletedIds),
                this::restorePendingDeletes
        );
    }

    @Override
    protected boolean onCloseRequested() {
        discardDraft();
        return false;
    }

    @Override
    protected void build(KineticUi ui) {
        int padding = 10;
        this.guiW = this.width() - padding * 2;
        this.guiH = this.height() - padding * 2;
        this.x0 = padding;
        this.y0 = padding;

        int btnW = 60;
        int filterBtnW = 120;
        int gap = 5;
        int btnBackX = x0 + guiW - padding - btnW;
        int btnSaveX = btnBackX - gap - btnW;
        int btnNewX = btnSaveX - gap - btnW;
        int btnFilterX = btnNewX - gap - filterBtnW;
        this.searchBox = ui().textField(x0 + padding, y0 + 20, btnFilterX - gap - (x0 + padding)).firstShownTextAsDefault().build();
        this.searchBox.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.search"));
        this.searchBox.limitTextLength(1024);
        this.searchBox.setTextValue(lastSearch);
        this.searchBox.onTextChange(val -> { updateSearch(val); if (listWidget != null) listWidget.setScrollOffset(0); });
ui().button(btnFilterX, y0 + 20, filterBtnW).text(getEntityFilterButtonText()).tooltip(getEntityFilterButtonTooltip()).onClick(b ->
                ArmorNetwork.requestEntityFilter()).build();
        ui().button(btnNewX, y0 + 20, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_new")).onClick(b -> createNewSet()).build();
        ui().button(btnSaveX, y0 + 20, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> savePendingDeletes()).build();
        ui().button(btnBackX, y0 + 20, btnW).text(KineticI18n.translatable("gui.kineticarmory.common.back")).onClick(b -> close()).build();

        int listTop = y0 + 45;
        int listBottom = y0 + guiH - 2;
        int listLeft = x0 + padding;
        int listRight = x0 + guiW - 2;

        this.listWidget = ui.add(new SetListWidget(listLeft, listTop, listRight - listLeft, listBottom - listTop));

        performSearchFilter(lastSearch);
        this.listWidget.refresh();
        this.listWidget.setScrollOffset(lastScrollOffset);
    }

    private static Component getEntityFilterButtonText() {
        return KineticI18n.translatable(
                "BLACKLIST".equalsIgnoreCase(ArmorClientSnapshot.entityFilterMode())
                        ? "gui.kineticarmory.armorsets.entity_filter.main_button.blacklist"
                        : "gui.kineticarmory.armorsets.entity_filter.main_button.whitelist"
        );
    }

    private static Component getEntityFilterButtonTooltip() {
        Component mode = KineticI18n.translatable(
                "BLACKLIST".equalsIgnoreCase(ArmorClientSnapshot.entityFilterMode())
                        ? "gui.kineticarmory.armorsets.entity_filter.mode.state.blacklist"
                        : "gui.kineticarmory.armorsets.entity_filter.mode.state.whitelist"
        );
        return KineticI18n.translatable("gui.kineticarmory.armorsets.entity_filter.main_button.tooltip", mode);
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
        updateSearch(searchBox == null ? lastSearch : searchBox.textValue());
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
        updateSearch(searchBox == null ? lastSearch : searchBox.textValue());
    }

    @Override
    protected void onTick() {
        if (listWidget != null) lastScrollOffset = listWidget.scrollOffset();
        if (searchBox != null) lastSearch = searchBox.textValue();
    }

    private void createNewSet() {
        ArmorDataConfig newSet = new ArmorDataConfig();
        newSet.id = "set_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        newSet.displayName = ""; newSet.initNullFields();
        allEntries.add(newSet); ArmorClientSnapshot.put(newSet);
        updateSearch(searchBox.textValue());
        if (listWidget != null) { listWidget.setScrollOffset(Integer.MAX_VALUE); lastScrollOffset = listWidget.scrollOffset(); }
        openEditor(newSet);
    }

    private void openEditor(ArmorDataConfig config) { openChild(new ArmorEditPage(config)); }

    private void deleteSet(ArmorDataConfig config) {
        if (config == null || config.id == null || config.id.isBlank()) return;
        pendingDeletedIds.add(config.id);
        allEntries.removeIf(entry -> config.id.equals(entry.id));
        updateSearch(searchBox == null ? lastSearch : searchBox.textValue());
    }

    private void savePendingDeletes() {
        if (!pendingDeletedIds.isEmpty()) {
            for (String id : new ArrayList<>(pendingDeletedIds)) {
                ArmorNetwork.deleteArmorSet(id);
                ArmorClientSnapshot.remove(id);
            }
            pendingDeletedIds.clear();
            rebuildEntriesFromSnapshot();
            updateSearch(searchBox == null ? lastSearch : searchBox.textValue());
        }
        commitDraft();
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.panel(g, x0, y0, guiW, guiH);
        g.scrollingTextCentered(title(), this.width() / 2, y0 + 6, Math.max(0, guiW - 20), 0xFFFFFF, false);
    }

    /** 套装列表：每行 46px，右侧内嵌二次确认删除按钮 / Set list: 46px rows with an inline two-step delete button. */
    class SetListWidget extends KineticRowList<ArmorDataConfig> {
        private static final int ROW_H = 46;
        private static final int PREVIEW_SIZE = 18;
        private static final int PREVIEW_PITCH = PREVIEW_SIZE + 2;
        private static final int DELETE_BUTTON_W = 52;
        private static final int DELETE_BUTTON_H = KineticPage.CONTROL_HEIGHT;

        private String deleteConfirmId;
        private long confirmTime;

        SetListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, ROW_H);
        }

        public void refresh() {
            setItems(displayEntries);
        }

        private int deleteX(int left, int w) {
            return left + w - DELETE_BUTTON_W - 4;
        }

        private int deleteY(int top) {
            return top + (44 - DELETE_BUTTON_H) / 2;
        }

        @Override
        protected void renderRowBackground(KineticGraphics g, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(g, x, y, width, 44, KineticTheme.Surface.PANEL_ALT, false, hovered, false);
        }

        @Override
        protected boolean onRowClick(ArmorDataConfig data, int index, MouseInput input) {
            int top = rowTop(index);
            int delX = deleteX(controlX(), rowsWidth());
            if (input.isLeft() && input.inside(delX, deleteY(top), DELETE_BUTTON_W, DELETE_BUTTON_H)) {
                handleDelete(data);
                return true;
            }
            if (input.isLeft() && input.y() >= top && input.y() < top + 44) {
                openEditor(data);
                return true;
            }
            return false;
        }

        private void handleDelete(ArmorDataConfig data) {
            if (data.id != null && data.id.equals(deleteConfirmId)) {
                deleteConfirmId = null;
                deleteSet(data);
                return;
            }
            deleteConfirmId = data.id;
            confirmTime = System.currentTimeMillis();
        }

        @Override
        protected void renderRow(KineticGraphics g, ArmorDataConfig data, int idx, int left, int top, int w, int h,
                                 boolean hv, boolean selected) {
                String dName = (data.displayName != null && !data.displayName.isEmpty()) ? data.displayName : "!!! NO DISPLAY NAME !!!";
                String dId = data.id != null ? data.id : "unknown";

                int textMaxW = 175;
                Component idStr = Component.literal("ID: " + dId);

                g.scrollingText(Component.literal(dName), left + 5, top + 5, textMaxW, 0xFFFF55, false);
                g.scrollingText(idStr, left + 5, top + 17, textMaxW, 0xAAAAAA, false);

                String infoStrRaw = getInfo(data);
                int maxInfoW = textMaxW + 10;
                g.scrollingText(Component.literal(infoStrRaw), left + 5, top + 29, maxInfoW, 0x55FF55, false);

                int delX = deleteX(left, w);
                int delY = deleteY(top);
                if (deleteConfirmId != null && System.currentTimeMillis() - confirmTime > 3000) {
                    deleteConfirmId = null;
                }
                boolean confirming = data.id != null && data.id.equals(deleteConfirmId);
                boolean delHovered = mouseX() >= delX && mouseX() < delX + DELETE_BUTTON_W
                        && mouseY() >= delY && mouseY() < delY + DELETE_BUTTON_H;
                KineticTheme.button(g, delX, delY, DELETE_BUTTON_W, DELETE_BUTTON_H, KineticI18n.translatable(confirming
                        ? "gui.kineticarmory.armorsets.btn_delete_confirm"
                        : "gui.kineticarmory.armorsets.btn_delete"), delHovered, true, false);

                int labelX = left + 185;
                int labelWidth = 70;
                int curioY = top + 3;
                int equipY = top + 23;

                Component curioLabel = KineticI18n.translatable("gui.kineticarmory.armorsets.label.curios");
                g.scrollingText(curioLabel, labelX, curioY + 4, labelWidth, 0xAAAAAA, false);
                int curioStartX = labelX + labelWidth + 4;

                int equipLabelX = labelX + 18;
                Component equipLabel = KineticI18n.translatable("gui.kineticarmory.armorsets.label.equipment");
                g.scrollingText(equipLabel, equipLabelX, equipY + 4, labelWidth, 0xAAAAAA, false);
                int equipStartX = equipLabelX + labelWidth + 4;

                int maxIconsCurio = Math.max(0, (delX - 4 - curioStartX + 2) / PREVIEW_PITCH);
                int maxIconsEquip = Math.max(0, (delX - 4 - equipStartX + 2) / PREVIEW_PITCH);

                if (data.curios != null) {
                    int drawn = 0;
                    for (ArmorDataConfig.ItemReq req : data.curios) {
                        if (drawn >= maxIconsCurio) break;
                        if (req != null && !req.id.equals("minecraft:air") && !req.id.equals("EMPTY") && !req.id.equals("ANY")) {
                            var rl = KineticResourceIds.tryParse(req.id);
                            if (rl != null) {
                                Item item = KineticRegistries.items().get(rl);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    int iconX = curioStartX + drawn * PREVIEW_PITCH;
                                    KineticTheme.itemSlot(g, iconX, curioY, PREVIEW_SIZE, false);
                                    KineticTheme.item(g, new ItemStack(item), iconX, curioY, PREVIEW_SIZE, 0.75F, false);
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
                                    int iconX = equipStartX + drawn * PREVIEW_PITCH;
                                    KineticTheme.itemSlot(g, iconX, equipY, PREVIEW_SIZE, false);
                                    KineticTheme.item(g, new ItemStack(item), iconX, equipY, PREVIEW_SIZE, 0.75F, false);
                                    if (data.hasMultipleEquipmentVariants(slot)) {
                                        KineticTheme.indicatorFill(g, iconX + 9, equipY + 9, 6, 6, KineticTheme.Indicator.SUCCESS, 0.80F);
                                        g.text("+", iconX + 9, equipY + 6, 0xFF55FF55, false);
                                    }
                                    drawn++;
                                }
                            }
                        }
                    }
                }
            }

        private String getInfo(ArmorDataConfig data) {
                int attr = data.attributes == null ? 0 : data.attributes.size();
                int pot = data.potionEffects == null ? 0 : data.potionEffects.size();
                int imm = data.damageImmunities == null ? 0 : data.damageImmunities.size();
                int atk = data.attackEffects == null ? 0 : data.attackEffects.size();
                int pImm = data.effectImmunities == null ? 0 : data.effectImmunities.size();
                int conv = data.damageConversions == null ? 0 : data.damageConversions.size();
                int dmgRed = data.damageMultipliers == null ? 0 : data.damageMultipliers.size();
                int atkDmg = data.attackDamageMultipliers == null ? 0 : data.attackDamageMultipliers.size();
                String flightStr = KineticI18n.translatable(data.allowFlight ? "gui.kineticarmory.armorsets.info.yes" : "gui.kineticarmory.armorsets.info.no").getString();

                return KineticI18n.translatable("gui.kineticarmory.armorsets.info.summary_detailed",
                        attr, pot, pImm, imm, atk, atkDmg, dmgRed, conv, flightStr).getString();
            }
    }
}
