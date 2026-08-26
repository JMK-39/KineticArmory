package dev.xyat.kineticarmory.armorsets.client;

import dev.xyat.kineticarmory.util.ColorText;
import com.mojang.datafixers.util.Either;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.config.ArmorClientConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class ArmorTooltip {

    private static final int MAX_DISPLAY_COUNT = 5;
    private static final String ICON_REGEX = "\\[(item|effect):([a-zA-Z0-9_:]+)]";
    private static final Pattern ICON_PATTERN = Pattern.compile(ICON_REGEX);
    private static final String COLOR_CODE_REGEX = "§[0-9a-fk-or]";

    public static void onTooltip(ItemTooltipEvent event) {
        if (!ArmorConfig.enableSets) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        Set<ArmorDataConfig> potentialSets = ArmorClientSnapshot.configsForItem(stack.getItem());
        if (potentialSets == null || potentialSets.isEmpty()) return;

        List<ArmorDataConfig> rejectedBySets = new ArrayList<>();
        Set<String> rejectedSetNames = new HashSet<>();

        for (ArmorDataConfig config : potentialSets) {
            if (isItemIncludedInSet(stack, config) && isItemRejectedBySet(stack, config)) {
                String cleanName = stripColor(config.displayName);
                if (rejectedSetNames.add(cleanName)) rejectedBySets.add(config);
            }
        }

        if (!rejectedBySets.isEmpty()) {
            event.getToolTip().add(Component.empty());
            if (Screen.hasAltDown()) {
                event.getToolTip().add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.conflict_header"));
                int count = 0;
                for (ArmorDataConfig config : rejectedBySets) {
                    if (count >= MAX_DISPLAY_COUNT) break;
                    event.getToolTip().add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.conflict_row", stripColor(config.displayName)));
                    count++;
                }
                if (rejectedBySets.size() > MAX_DISPLAY_COUNT) {
                    event.getToolTip().add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.conflict_more", rejectedBySets.size() - MAX_DISPLAY_COUNT));
                }
            } else {
                event.getToolTip().add(ColorText.translatable("gui.kineticarmory.armorsets.tooltip.hold_alt_conflict"));
            }
        }
    }

    private static boolean isItemIncludedInSet(ItemStack stack, ArmorDataConfig config) {
        config.normalizeEquipmentVariants();
        if (config.equipmentVariants != null) {
            for (List<ArmorDataConfig.ItemReq> variants : config.equipmentVariants.values()) {
                if (ArmorDataConfig.isItemMatchingAny(stack, variants)) return true;
            }
        }
        if (config.curios != null) {
            for (ArmorDataConfig.ItemReq req : config.curios) {
                if (ArmorDataConfig.isItemMatching(stack, req)) return true;
            }
        }
        return false;
    }

    private static boolean isItemRejectedBySet(ItemStack stack, ArmorDataConfig config) {
        if (config.rejectedCurios != null) {
            for (ArmorDataConfig.ItemReq req : config.rejectedCurios) {
                if (ArmorDataConfig.isItemMatching(stack, req)) return true;
            }
        }
        return false;
    }

    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        if (!ArmorConfig.enableSets) return;
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) return;

        Set<ArmorDataConfig> potentialSets = ArmorClientSnapshot.configsForItem(itemStack.getItem());
        if (potentialSets == null || potentialSets.isEmpty()) return;

        List<ArmorDataConfig> matchedSets = new ArrayList<>();
        boolean shouldReplaceOriginalTooltip = false;

        for (ArmorDataConfig config : potentialSets) {
            if (!isItemIncludedInSet(itemStack, config) || isItemRejectedBySet(itemStack, config)) continue;
            matchedSets.add(config);
            String tipKey = getTipKey(config);
            if (isDetailKeyDown(config) && !"none".equalsIgnoreCase(tipKey)) {
                shouldReplaceOriginalTooltip = true;
            }
        }

        if (matchedSets.isEmpty()) return;

        if (shouldReplaceOriginalTooltip) keepOnlyItemName(event);
        addText(event, Component.empty());

        boolean anyActive = false;
        for (ArmorDataConfig config : matchedSets) {
            if (ArmorCache.isSetActive(config.id)) {
                anyActive = true;
                break;
            }
        }

        for (ArmorDataConfig config : matchedSets) {
            TooltipSetState tooltipState = buildClientTooltipSetState(config);
            int pieceCount = tooltipState.pieceCount();
            int total = Math.max(1, config.getTotalPieceCount());
            int shownPieceCount = Math.max(0, Math.min(pieceCount, total));
            addText(event, ColorText.translatable("gui.kineticarmory.armorsets.tooltip.name_with_pieces", stripColor(config.displayName), shownPieceCount, total));
            if (isDetailKeyDown(config)) {
                addSetDetails(event, config, ArmorCache.isSetActive(config.id), anyActive, tooltipState);
            } else if (!"none".equalsIgnoreCase(getTipKey(config))) {
                addText(event, ColorText.translatable("gui.kineticarmory.armorsets.tooltip.hold_key_details", getTipKeyDisplayName(config)));
            }
        }
    }

    private static TooltipSetState buildClientTooltipSetState(ArmorDataConfig config) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return new TooltipSetState(ArmorCache.getSetPieceCount(config.id));
        }
        return calculateClientTooltipSetState(config, minecraft.player);
    }

    private static TooltipSetState calculateClientTooltipSetState(ArmorDataConfig set, LivingEntity entity) {
        Map<String, ItemStack> gear = new HashMap<>();
        gear.put("head", entity.getItemBySlot(EquipmentSlot.HEAD));
        gear.put("chest", entity.getItemBySlot(EquipmentSlot.CHEST));
        gear.put("legs", entity.getItemBySlot(EquipmentSlot.LEGS));
        gear.put("feet", entity.getItemBySlot(EquipmentSlot.FEET));
        gear.put("mainhand", entity.getMainHandItem());
        gear.put("offhand", entity.getOffhandItem());

        int matched = 0;
        set.normalizeEquipmentVariants();
        if (set.equipmentVariants != null) {
            for (Map.Entry<String, List<ArmorDataConfig.ItemReq>> entry : set.equipmentVariants.entrySet()) {
                List<ArmorDataConfig.ItemReq> variants = entry.getValue();
                ItemStack slotStack = gear.getOrDefault(entry.getKey(), ItemStack.EMPTY);
                if (ArmorDataConfig.isExclusiveSlotState(variants, "EMPTY")) {
                    continue;
                }
                if (ArmorDataConfig.countPieceRequirements(variants) > 0 && ArmorDataConfig.isItemMatchingAny(slotStack, variants)) {
                    matched++;
                }
            }
        }

        if (set.curios != null && !set.curios.isEmpty()) {
            List<ItemStack> curios = collectClientCurios(entity);
            boolean[] used = new boolean[curios.size()];
            for (ArmorDataConfig.ItemReq req : set.curios) {
                if (!ArmorDataConfig.isCountedPieceRequirement(req)) continue;
                for (int i = 0; i < curios.size(); i++) {
                    if (!used[i] && ArmorDataConfig.isItemMatching(curios.get(i), req)) {
                        used[i] = true;
                        matched++;
                        break;
                    }
                }
            }
        }

        return new TooltipSetState(matched);
    }

    private static List<ItemStack> collectClientCurios(LivingEntity entity) {
        List<ItemStack> curios = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> handler.getCurios().values().forEach(stackHandler -> {
            IDynamicStackHandler stacks = stackHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) curios.add(stack);
            }
        }));
        return curios;
    }

    private static void keepOnlyItemName(RenderTooltipEvent.GatherComponents event) {
        if (event.getTooltipElements().isEmpty()) return;
        Either<FormattedText, TooltipComponent> firstLine = event.getTooltipElements().get(0);
        event.getTooltipElements().clear();
        event.getTooltipElements().add(firstLine);
    }

    private static void addSetDetails(RenderTooltipEvent.GatherComponents event, ArmorDataConfig config, boolean active, boolean anyActive, TooltipSetState tooltipState) {
        config.preparePieceBonusData();
        int pieceCount = tooltipState.pieceCount();
        int total = Math.max(1, config.getTotalPieceCount());
        List<ArmorTipGenerator.TooltipLine> lines = ArmorTipGenerator.buildTooltipLines(config, pieceCount, false);
        if (lines.isEmpty()) return;

        for (ArmorTipGenerator.TooltipLine line : lines) {
            if (line.text() == null || line.text().isBlank()) continue;
            if (line.iconLine()) {
                addIconTip(event, line.text(), line.isActive(pieceCount, total, active), line.hasAnyActive(pieceCount, anyActive));
            } else {
                addText(event, Component.literal(line.text()));
            }
        }
    }

    private static void addText(RenderTooltipEvent.GatherComponents event, Component component) {
        event.getTooltipElements().add(Either.left(component));
    }

    private static void addIconTip(RenderTooltipEvent.GatherComponents event, String text, boolean active, boolean anyActive) {
        event.getTooltipElements().add(Either.right(new IconTipTooltipData(text, active, anyActive)));
    }

    private static boolean isDetailKeyDown(ArmorDataConfig config) {
        String key = getTipKey(config);
        return switch (key) {
            case "ctrl", "control" -> Screen.hasControlDown();
            case "alt" -> Screen.hasAltDown();
            case "none" -> true;
            default -> Screen.hasShiftDown();
        };
    }

    private static String getTipKey(ArmorDataConfig config) {
        String key = config.tipKey;
        if (key == null || key.isBlank()) key = ArmorClientConfig.defaultTipKey();
        if (key == null || key.isBlank()) key = "shift";
        return key.toLowerCase();
    }

    private static Component getTipKeyDisplayName(ArmorDataConfig config) {
        String key = getTipKey(config);
        return switch (key) {
            case "ctrl", "control" -> ColorText.translatable("gui.kineticarmory.armorsets.key.ctrl");
            case "alt" -> ColorText.translatable("gui.kineticarmory.armorsets.key.alt");
            case "none" -> ColorText.translatable("gui.kineticarmory.armorsets.key.none");
            default -> ColorText.translatable("gui.kineticarmory.armorsets.key.shift");
        };
    }

    private static String stripColor(String text) {
        return text == null ? "" : text.replaceAll(COLOR_CODE_REGEX, "");
    }

    private record TooltipSetState(int pieceCount) {}

    public record IconTipTooltipData(String rawText, boolean isActive, boolean anyActive) implements TooltipComponent {}

    public static class ClientIconTipComponent implements ClientTooltipComponent {
        private final String rawText;
        private final String cleanText;
        private final boolean isActive;
        private final boolean anyActive;

        public ClientIconTipComponent(IconTipTooltipData data) {
            this.rawText = data.rawText();
            this.cleanText = data.rawText().replaceAll(ICON_REGEX, "");
            this.isActive = data.isActive();
            this.anyActive = data.anyActive();
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return font.width(stripColor(cleanText)) + 14;
        }

        @Override
        public void renderText(@NotNull Font font, int x, int y, @NotNull org.joml.Matrix4f matrix, net.minecraft.client.renderer.MultiBufferSource.@NotNull BufferSource bufferSource) {
            String dispText = this.isActive ? this.cleanText : stripColor(this.cleanText);
            String prefix = this.isActive ? "§f" : (this.anyActive ? "§m" : "§f");
            MutableComponent comp = Component.literal(prefix + dispText);
            int color = this.isActive ? -1 : (this.anyActive ? 0xFFBBBBBB : -1);
            font.drawInBatch(comp, x, y, color, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics g) {
            Matcher matcher = ICON_PATTERN.matcher(this.rawText);
            while (matcher.find()) {
                String type = matcher.group(1);
                String id = matcher.group(2);
                int iconX = x + font.width(stripColor(this.rawText.substring(0, matcher.start()).replaceAll(ICON_REGEX, "")));
                if (type.equals("item")) {
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    if (rl != null) {
                        var item = ForgeRegistries.ITEMS.getValue(rl);
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                            g.pose().pushPose();
                            g.pose().translate(iconX, y, 0);
                            g.pose().scale(0.7f, 0.7f, 1.0f);
                            g.renderItem(new ItemStack(item), 0, 0);
                            g.pose().popPose();
                        }
                    }
                }
            }
        }
    }

    public record RejectedTooltipData(List<String> itemIds) implements TooltipComponent {}

    public static class ClientRejectedTooltipComponent implements ClientTooltipComponent {
        private final List<ItemStack> stacks;

        public ClientRejectedTooltipComponent(RejectedTooltipData data) {
            this.stacks = data.itemIds().stream()
                    .map(id -> new ItemStack(Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)))))
                    .collect(Collectors.toList());
        }

        @Override
        public int getHeight() {
            return ((stacks.size() - 1) / 9 + 1) * 18 + 2;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return Math.min(stacks.size(), 9) * 18;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            for (int i = 0; i < stacks.size(); i++) {
                guiGraphics.renderItem(stacks.get(i), x + (i % 9) * 18, y + (i / 9) * 18 + 1);
            }
        }
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RejectedTooltipData.class, ClientRejectedTooltipComponent::new);
        event.register(IconTipTooltipData.class, ClientIconTipComponent::new);
    }
}
