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
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
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
        useCanvas(CANVAS_WIDTH, CANVAS_HEIGHT, 4);

        this.allEntries = new ArrayList<>(ArmorClientSnapshot.configs());
        this.allEntries.sort(Comparator.comparing(a -> a.id));
        this.displayEntries = new ArrayList<>(this.allEntries);
        configureStandaloneDraft(
                () -> new LinkedHashSet<>(pendingDeletedIds),
                this::restorePendingDeletes
        );
        bindDeleteAction(() -> listWidget != null && listWidget.selectedItem() != null,
                () -> confirmDelete(listWidget.selectedItem()));
    }

    @Override
    protected boolean onCloseRequested() {
        discardDraft();
        return false;
    }

    /**
     * The pages and effect editors of the first set, for screenshot tools; the effect editors open in "new" mode and
     * nothing is saved unless the player saves. None while there are no sets.
     */
    @Override
    public List<ReviewView> reviewViews() {
        if (allEntries.isEmpty()) return List.of();
        ArmorDataConfig first = allEntries.get(0);
        return List.of(
                new ReviewView("set", () -> openEditor(first)),
                new ReviewView("tips", () -> openChild(new ArmorTipEditorPage(first))),
                new ReviewView("detail", () -> openChild(new ArmorDetailPage(first))),
                new ReviewView("commands", () -> openChild(new ArmorCommandEditorPage(first))),
                new ReviewView("bonus", () -> openChild(new ArmorPieceBonusPage(first))),
                new ReviewView("entity-filter", () -> openChild(new ArmorEntityFilterPage(first))),
                new ReviewView("variants", () -> openChild(new ArmorEquipmentVariantPage(first, "head",
                        KineticI18n.translatable("gui.kineticarmory.armorsets.slot.head")))),
                new ReviewView("attribute", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.AttributeEditor(first, null))),
                new ReviewView("potion", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.PotionEditor(first, null))),
                new ReviewView("immunity", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.ImmunityEditor(first, null))),
                new ReviewView("attack-effect", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.AttackEffectEditor(first, null))),
                new ReviewView("effect-immunity", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.PotionImmunityEditor(first, null))),
                new ReviewView("damage-conversion", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.DamageConversionEditor(first, null))),
                new ReviewView("attack-damage", () -> openChild(new dev.xyat.kineticarmory.armorsets.client.gui.editor.AttackDamageEditor(first, null))));
    }

    @Override
    protected void build(KineticUi ui) {
        int padding = 4;
        this.guiW = this.width();
        this.guiH = this.height();
        this.x0 = 0;
        this.y0 = 0;

        int btnW = 40;
        int filterBtnW = 120;
        int gap = 4;
        int toolbarY = padding;
        int btnSaveX = guiW - padding - 40;
        int btnNewX = btnSaveX - gap - btnW;
        int btnFilterX = btnNewX - gap - filterBtnW;
        int searchX = padding + 40 + gap;
        this.searchBox = ui().textField(searchX, toolbarY, 368).firstShownTextAsDefault().build();
        this.searchBox.setPlaceholder(KineticI18n.translatable("gui.kineticarmory.armorsets.search"));
        this.searchBox.limitTextLength(1024);
        this.searchBox.setTextValue(lastSearch);
        this.searchBox.onTextChange(val -> { updateSearch(val); if (listWidget != null) listWidget.setScrollOffset(0); });
        ui().button(btnFilterX, toolbarY, filterBtnW).text(getEntityFilterButtonText()).tooltip(getEntityFilterButtonTooltip()).onClick(b ->
                ArmorNetwork.requestEntityFilter()).build();
        ui().button(btnNewX, toolbarY, btnW).text(KineticI18n.translatable("gui.kineticarmory.armorsets.btn_new")).onClick(b -> createNewSet()).build();
        ui().button(btnSaveX, toolbarY, 40).text(KineticI18n.translatable("gui.kineticarmory.armorsets.save")).onClick(b -> savePendingDeletes()).build();
        ui().button(padding, toolbarY, 40).text(KineticI18n.translatable("gui.kineticarmory.common.back")).onClick(b -> close()).build();

        int listTop = toolbarY + CONTROL_HEIGHT + 4;
        int listBottom = guiH - padding;
        int listLeft = padding;
        int listRight = guiW - padding;

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

    private void confirmDelete(ArmorDataConfig config) {
        if (config == null) return;
        openDialog(KineticI18n.translatable("gui.kineticarmory.armorsets.delete"),
                KineticI18n.translatable("gui.kineticarmory.confirm_delete", config.displayName == null || config.displayName.isBlank() ? config.id : config.displayName),
                KineticI18n.translatable("gui.kineticarmory.armorsets.delete"), KineticI18n.translatable("gui.back"),
                () -> deleteSet(config), () -> {});
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
            dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.saved"));
        } else {
            // Edits inside a set are saved on its own page; say so instead of staying silent.
            dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays.toast(KineticI18n.translatable("msg.kineticarmory.common.nothing_to_save"));
        }
        commitDraft();
    }

    @Override
    protected void renderBackground(KineticGraphics g, int mx, int my, float pt) {
        KineticTheme.panel(g, x0, y0, guiW, guiH);
    }

    @Override
    protected void renderTooltips(int mouseX, int mouseY) {
        if (listWidget == null) return;
        ItemStack stack = listWidget.previewAt(mouseX, mouseY);
        if (!stack.isEmpty()) showItemTooltip(stack);
    }

    private record ItemPreview(ItemStack stack, int x, int y, boolean variants) { }

    /** Set rows expose editing and confirmed deletion without reserving a trailing button. */
    class SetListWidget extends KineticRowList<ArmorDataConfig> {
        private static final int ROW_H = 46;
        private static final int PREVIEW_SIZE = 18;
        private static final int PREVIEW_PITCH = PREVIEW_SIZE + 2;
        SetListWidget(int x, int y, int width, int height) {
            super(x, y, width, height, ROW_H);
        }

        public void refresh() {
            setItems(displayEntries);
        }

        @Override
        protected void renderRowBackground(KineticGraphics g, int index, int x, int y, int width, int height,
                                           boolean hovered, boolean selected) {
            KineticTheme.stateSurface(g, x, y, width, 44, KineticTheme.Surface.PANEL_ALT, false, hovered, false);
        }

        @Override
        protected boolean onRowClick(ArmorDataConfig data, int index, MouseInput input) {
            int top = rowTop(index);
            if (input.y() < top || input.y() >= top + 44) return false;
            setSelectedIndex(index);
            if (input.isRight()) {confirmDelete(data);return true;}
            if (input.isLeft()) {
                openEditor(data);
                return true;
            }
            return false;
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

                int labelX = left + 185;
                int labelWidth = 70;
                g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.label.curios"),
                        labelX, top + 7, labelWidth, 0xAAAAAA, false);
                g.scrollingText(KineticI18n.translatable("gui.kineticarmory.armorsets.label.equipment"),
                        labelX + 18, top + 27, labelWidth, 0xAAAAAA, false);
                for (ItemPreview preview : previews(data, left, top, w)) {
                    KineticTheme.itemSlot(g, preview.x(), preview.y(), PREVIEW_SIZE, false);
                    KineticTheme.item(g, preview.stack(), preview.x(), preview.y(), PREVIEW_SIZE, 1.0F, false);
                    if (preview.variants()) ArmorVariantMarker.draw(g, preview.x(), preview.y());
                }
        }

        @Override
        protected Component rowTooltip(ArmorDataConfig data, int index) {
            return previewAt(mouseX(), mouseY()).isEmpty()
                    ? KineticI18n.translatable("gui.kineticarmory.armorsets.list.row.tooltip") : null;
        }

        private List<ItemPreview> previews(ArmorDataConfig data, int left, int top, int width) {
            List<ItemPreview> result = new ArrayList<>();
            int curioStart = left + 259, equipmentStart = left + 277;
            int right = left + width - 4;
            if (data.curios != null) {
                int x = curioStart;
                for (ArmorDataConfig.ItemReq req : data.curios) {
                    if (x + PREVIEW_SIZE > right) break;
                    ItemStack stack = req == null ? ItemStack.EMPTY : req.createDisplayStack();
                    if (stack.isEmpty()) continue;
                    result.add(new ItemPreview(stack, x, top + 3, false));x += PREVIEW_PITCH;
                }
            }
            data.normalizeEquipmentVariants();
            int x = equipmentStart;
            for (String slot : new String[]{"head", "chest", "legs", "feet", "mainhand", "offhand"}) {
                if (x + PREVIEW_SIZE > right) break;
                ArmorDataConfig.ItemReq req = data.getDisplayedEquipmentReq(slot, System.currentTimeMillis());
                ItemStack stack = req == null ? ItemStack.EMPTY : req.createDisplayStack();
                if (stack.isEmpty()) continue;
                result.add(new ItemPreview(stack, x, top + 23, data.hasMultipleEquipmentVariants(slot)));x += PREVIEW_PITCH;
            }
            return result;
        }

        private ItemStack previewAt(double mouseX, double mouseY) {
            int index = rowAt(mouseX, mouseY);
            if (index < 0) return ItemStack.EMPTY;
            for (ItemPreview preview : previews(items().get(index), controlX(), rowTop(index), rowsWidth())) {
                if (KineticTheme.hovering(mouseX, mouseY, preview.x(), preview.y(), PREVIEW_SIZE, PREVIEW_SIZE)) return preview.stack();
            }
            return ItemStack.EMPTY;
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
