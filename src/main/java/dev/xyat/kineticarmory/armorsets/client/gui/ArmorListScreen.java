package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.client.ArmorClientSnapshot;
import dev.xyat.kineticarmory.armorsets.Network.ArmorNetwork;
import dev.xyat.kineticcore.api.client.search.KineticSearch;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.SmoothSelectionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
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

    private EditBox searchBox;
    private SetListWidget listWidget;
    private int guiW, guiH, x0, y0;
    private final Screen parent;

    public ArmorListScreen() {
        this(null);
    }

    public ArmorListScreen(Screen parent) {
        super(ColorText.translatable("gui.kineticarmory.armorsets.list.title"));
        this.parent = parent;
        useCanvas(
                640f,
                360f,
                6
        );

        this.allEntries = new ArrayList<>(ArmorClientSnapshot.configs());
        this.allEntries.sort(Comparator.comparing(a -> a.id));
        this.displayEntries = new ArrayList<>(this.allEntries);
        configureStandaloneDraft(
                () -> new LinkedHashSet<>(pendingDeletedIds),
                this::restorePendingDeletes
        );
    }

    @Override
    public void onClose() {
        discardDraft();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    protected void buildUi() {
        int padding = 10;
        this.guiW = this.canvasWidth - padding * 2;
        this.guiH = this.canvasHeight - padding * 2;
        this.x0 = padding;
        this.y0 = padding;

        int btnW = 60;
        int filterBtnW = 120;
        int gap = 5;
        int btnBackX = x0 + guiW - padding - btnW;
        int btnSaveX = btnBackX - gap - btnW;
        int btnNewX = btnSaveX - gap - btnW;
        int btnFilterX = btnNewX - gap - filterBtnW;
        this.searchBox = new EditBox(this.font, x0 + padding, y0 + 20, btnFilterX - gap - (x0 + padding), 20, Component.empty());
        this.searchBox.setMaxLength(1024);
        this.searchBox.setValue(lastSearch);
        this.searchBox.setResponder(val -> { updateSearch(val); if (listWidget != null) listWidget.setScrollAmount(0); });
        this.addRenderableWidget(searchBox);

        this.addRenderableWidget(Button.builder(getEntityFilterButtonText(), b ->
                ArmorNetwork.CHANNEL.sendToServer(new ArmorNetwork.RequestEntityFilterPacket()))
                .bounds(btnFilterX, y0 + 20, filterBtnW, 20)
                .tooltip(Tooltip.create(getEntityFilterButtonTooltip()))
                .build());
        this.addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.btn_new"), b -> createNewSet()).bounds(btnNewX, y0 + 20, btnW, 20).build());
        this.addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.save"), b -> savePendingDeletes()).bounds(btnSaveX, y0 + 20, btnW, 20).build());
        this.addRenderableWidget(Button.builder(ColorText.translatable("gui.kineticarmory.common.back"), b -> onClose()).bounds(btnBackX, y0 + 20, btnW, 20).build());

        int listTop = y0 + 45;
        int listBottom = y0 + guiH - padding;

        this.listWidget = new SetListWidget(this.minecraft, guiW - 16, listBottom - listTop, listTop, listBottom, 46);
        this.listWidget.setLeftPos(x0 + 8);
        this.addWidget(this.listWidget);

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
    public void tick() {
        super.tick();
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

    private void openEditor(ArmorDataConfig config) { if (this.minecraft != null) this.minecraft.setScreen(new ArmorEditScreen(this, config)); }

    private void deleteSet(ArmorDataConfig config) {
        if (config == null || config.id == null || config.id.isBlank()) return;
        pendingDeletedIds.add(config.id);
        allEntries.removeIf(entry -> config.id.equals(entry.id));
        updateSearch(searchBox == null ? lastSearch : searchBox.getValue());
    }

    private void savePendingDeletes() {
        if (!pendingDeletedIds.isEmpty()) {
            for (String id : new ArrayList<>(pendingDeletedIds)) {
                ArmorNetwork.CHANNEL.sendToServer(new ArmorNetwork.DeleteArmorSetPacket(id));
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
        g.fill(x0, y0, x0 + guiW, y0 + guiH, 0xDD000000);
        g.renderOutline(x0, y0, guiW, guiH, 0xFFFFFFFF);
        g.drawCenteredString(this.font, this.title, this.canvasWidth / 2, y0 + 6, 0xFFFFFF);
        renderScaledList(listWidget, g, mx, my, pt);
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics g, int mx, int my, float pt) {
        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            String text = ColorText.translatable("gui.kineticarmory.armorsets.search").getString();
            g.drawString(this.font, this.font.plainSubstrByWidth(text, searchBox.getWidth() - 8), searchBox.getX() + 4, searchBox.getY() + 6, 0x888888, false);
        }
    }

    @Override
    protected boolean canvasMouseScrolled(double mx, double my, double delta) {
        if (listWidget != null && listWidget.isMouseOver(mx, my) && listWidget.mouseScrolled(mx, my, delta)) {
            return true;
        }
        return super.canvasMouseScrolled(mx, my, delta);
    }

    @Override
    protected boolean canvasMouseClicked(double mx, double my, int btn) {
        if (this.searchBox != null) {
            if (!this.searchBox.isMouseOver(mx, my)) {
                this.searchBox.setFocused(false);
                if (this.getFocused() == this.searchBox) {
                    this.setFocused(null);
                }
            } else {
                this.setFocused(this.searchBox);
            }
        }
        if (this.listWidget != null && this.listWidget.handleScrollbarMouseClicked(mx, my, btn)) {
            return true;
        }
        return super.canvasMouseClicked(mx, my, btn);
    }

    @Override
    protected boolean canvasMouseDragged(double mx, double my, int btn, double dragX, double dragY) {
        if (this.listWidget != null && this.listWidget.handleScrollbarMouseDragged(my, btn)) {
            return true;
        }
        return super.canvasMouseDragged(mx, my, btn, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mx, double my, int btn) {
        if (this.listWidget != null && this.listWidget.handleScrollbarMouseReleased(btn)) {
            return true;
        }
        return super.canvasMouseReleased(mx, my, btn);
    }

    class SetListWidget extends SmoothSelectionList<SetListWidget.Entry> {
        private static final int SCROLLBAR_WIDTH = 4;
        private static final int SCROLLBAR_HIT_PADDING = 3;

        private final int listTop, listBottom;
        private boolean scrollbarDragging;
        private int scrollbarGrabOffset;

        public SetListWidget(Minecraft mc, int w, int h, int t, int b, int ih) {
            super(mc, w, h, t, b, ih);
            this.listTop = t; this.listBottom = b;
            setRenderBackground(false); setRenderTopAndBottom(false); refresh();
        }

        public void refresh() { clearEntries(); displayEntries.forEach(e -> addEntry(new Entry(e))); }
        @Override public int getRowWidth() { return this.width - 12; }
        @Override protected int getScrollbarPosition() { return ArmorListScreen.this.x0 + ArmorListScreen.this.guiW - 6; }

        private int scrollbarTrackHeight() {
            return Math.max(1, listBottom - listTop);
        }

        private int scrollbarThumbHeight() {
            int trackHeight = scrollbarTrackHeight();
            int contentHeight = Math.max(trackHeight, getMaxPosition());
            int minThumb = Math.min(20, trackHeight);
            return Math.max(minThumb, Math.min(trackHeight,
                    (int) Math.round((double) trackHeight * trackHeight / Math.max(1, contentHeight))));
        }

        private int scrollbarThumbY() {
            int trackHeight = scrollbarTrackHeight();
            int thumbHeight = scrollbarThumbHeight();
            int travel = Math.max(0, trackHeight - thumbHeight);
            double maxScroll = Math.max(0.0D, getMaxScroll());
            if (travel <= 0 || maxScroll <= 0.0D) return listTop;
            double ratio = Math.max(0.0D, Math.min(1.0D, getScrollAmount() / maxScroll));
            return listTop + (int) Math.round(ratio * travel);
        }

        private void setScrollFromThumbTop(double thumbTop) {
            int trackHeight = scrollbarTrackHeight();
            int thumbHeight = scrollbarThumbHeight();
            int travel = Math.max(0, trackHeight - thumbHeight);
            double maxScroll = Math.max(0.0D, getMaxScroll());
            if (travel <= 0 || maxScroll <= 0.0D) {
                snapScrollAmount(0.0D);
                return;
            }
            double clampedTop = Math.max(listTop, Math.min(listTop + travel, thumbTop));
            double ratio = (clampedTop - listTop) / travel;
            snapScrollAmount(ratio * maxScroll);
        }

        public boolean handleScrollbarMouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || getMaxScroll() <= 0.0D) return false;

            int barX = getScrollbarPosition();
            if (mouseX < barX - SCROLLBAR_HIT_PADDING
                    || mouseX >= barX + SCROLLBAR_WIDTH + SCROLLBAR_HIT_PADDING
                    || mouseY < listTop
                    || mouseY >= listBottom) {
                return false;
            }

            int thumbY = scrollbarThumbY();
            int thumbHeight = scrollbarThumbHeight();
            scrollbarDragging = true;

            if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                scrollbarGrabOffset = (int) Math.floor(mouseY - thumbY);
            } else {
                scrollbarGrabOffset = thumbHeight / 2;
                setScrollFromThumbTop(mouseY - scrollbarGrabOffset);
            }
            return true;
        }

        public boolean handleScrollbarMouseDragged(double mouseY, int button) {
            if (!scrollbarDragging || button != 0) return false;
            setScrollFromThumbTop(mouseY - scrollbarGrabOffset);
            return true;
        }

        public boolean handleScrollbarMouseReleased(int button) {
            if (!scrollbarDragging || button != 0) return false;
            scrollbarDragging = false;
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            if (handled && button == 0) snapScrollAmount(getScrollAmount());
            return handled;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            if (handled && button == 0) snapScrollAmount(getScrollAmount());
            return handled;
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private static final int DELETE_BUTTON_W = 52;
            private static final int DELETE_BUTTON_H = 18;

            private final ArmorDataConfig data;
            private final Button deleteButton;
            private boolean deleteConfirm = false;
            private long confirmTime = 0;
            private int lastTop = 0;

            public Entry(ArmorDataConfig data) {
                this.data = data;
                this.deleteButton = Button.builder(ColorText.translatable("gui.kineticarmory.armorsets.btn_delete"), b -> handleDelete())
                        .bounds(0, 0, DELETE_BUTTON_W, DELETE_BUTTON_H)
                        .build();
            }

            @Override
            public void render(@NotNull GuiGraphics g, int idx, int top, int left, int w, int h, int mx, int my, boolean hv, float pt) {
                this.lastTop = top;
                int bgColor = hv ? 0x88777777 : ((idx % 2 == 0) ? 0x88444444 : 0x88222222);

                g.fill(left, top, left + w, top + 44, bgColor);
                g.renderOutline(left, top, w, 44, 0xFF555555);

                String dName = (data.displayName != null && !data.displayName.isEmpty()) ? data.displayName : "!!! NO DISPLAY NAME !!!";
                String dId = data.id != null ? data.id : "unknown";

                int textMaxW = 175;
                dName = Minecraft.getInstance().font.plainSubstrByWidth(dName, textMaxW);
                String idStr = Minecraft.getInstance().font.plainSubstrByWidth("ID: " + dId, textMaxW);

                g.drawString(Minecraft.getInstance().font, dName, left + 5, top + 5, 0xFFFF55);
                g.drawString(Minecraft.getInstance().font, idStr, left + 5, top + 17, 0xAAAAAA);

                String infoStrRaw = getInfo();
                int infoW = Minecraft.getInstance().font.width(infoStrRaw);
                int maxInfoW = textMaxW + 10;

                g.pose().pushPose();
                if (infoW > maxInfoW) {
                    float scale = Math.max(0.6f, (float) maxInfoW / infoW);
                    g.pose().scale(scale, scale, 1.0f);
                    String renderStr = Minecraft.getInstance().font.plainSubstrByWidth(infoStrRaw, (int)(maxInfoW / scale));
                    g.drawString(Minecraft.getInstance().font, renderStr, (int)((left + 5) / scale), (int)((top + 29) / scale), 0x55FF55);
                } else {
                    g.drawString(Minecraft.getInstance().font, infoStrRaw, left + 5, top + 29, 0x55FF55);
                }
                g.pose().popPose();

                int delX = left + w - DELETE_BUTTON_W - 4;
                int delY = top + 23;
                if (deleteConfirm && System.currentTimeMillis() - confirmTime > 3000) {
                    deleteConfirm = false;
                }
                deleteButton.setMessage(ColorText.translatable(deleteConfirm
                        ? "gui.kineticarmory.armorsets.btn_delete_confirm"
                        : "gui.kineticarmory.armorsets.btn_delete"));
                deleteButton.setX(delX);
                deleteButton.setY(delY);
                deleteButton.render(g, mx, my, pt);

                int labelX = left + 185;
                int curioY = top + 4;
                int equipY = top + 24;

                Component curioLabel = ColorText.translatable("gui.kineticarmory.armorsets.label.curios");
                g.drawString(Minecraft.getInstance().font, curioLabel, labelX, curioY + 4, 0xAAAAAA);
                int curioStartX = labelX + Minecraft.getInstance().font.width(curioLabel) + 4;

                int equipLabelX = labelX + 18;
                Component equipLabel = ColorText.translatable("gui.kineticarmory.armorsets.label.equipment");
                g.drawString(Minecraft.getInstance().font, equipLabel, equipLabelX, equipY + 4, 0xAAAAAA);
                int equipStartX = equipLabelX + Minecraft.getInstance().font.width(equipLabel) + 4;

                int maxIconsCurio = Math.max(0, (left + w - 4 - curioStartX) / 18);
                int maxIconsEquip = Math.max(0, (delX - 4 - equipStartX) / 18);

                if (data.curios != null) {
                    int drawn = 0;
                    for (ArmorDataConfig.ItemReq req : data.curios) {
                        if (drawn >= maxIconsCurio) break;
                        if (req != null && !req.id.equals("minecraft:air") && !req.id.equals("EMPTY") && !req.id.equals("ANY")) {
                            ResourceLocation rl = ResourceLocation.tryParse(req.id);
                            if (rl != null) {
                                Item item = ForgeRegistries.ITEMS.getValue(rl);
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
                            ResourceLocation rl = ResourceLocation.tryParse(req.id);
                            if (rl != null) {
                                Item item = ForgeRegistries.ITEMS.getValue(rl);
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    int iconX = equipStartX + drawn * 18;
                                    g.renderItem(new ItemStack(item), iconX, equipY);
                                    if (data.hasMultipleEquipmentVariants(slot)) {
                                        g.fill(iconX + 10, equipY + 10, iconX + 18, equipY + 18, 0xCC003300);
                                        g.drawString(Minecraft.getInstance().font, "+", iconX + 12, equipY + 9, 0xFF55FF55, false);
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
                deleteButton.setMessage(ColorText.translatable("gui.kineticarmory.armorsets.btn_delete_confirm"));
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                if (deleteButton.mouseClicked(mx, my, btn)) return true;
                if (btn == 0 && my >= lastTop && my < lastTop + 44) {
                    openEditor(data);
                    return true;
                }
                return false;
            }
            @Override public @NotNull Component getNarration() { return Component.empty(); }
        }
    }
}
