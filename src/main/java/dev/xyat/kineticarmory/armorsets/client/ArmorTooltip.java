package dev.xyat.kineticarmory.armorsets.client;

import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.config.ArmorClientConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorTipGenerator;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.tooltip.KineticItemTooltips;
import dev.xyat.kineticcore.api.client.tooltip.KineticTooltipComponent;
import dev.xyat.kineticcore.api.registry.KineticRegistries;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

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

    public static void onTooltip(ItemStack stack, List<Component> tooltip) {
        if (!ArmorConfig.enableSets) return;
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
            tooltip.add(Component.empty());
            if (KineticClientRuntime.altModifierDown()) {
                tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.conflict_header"));
                int count = 0;
                for (ArmorDataConfig config : rejectedBySets) {
                    if (count >= MAX_DISPLAY_COUNT) break;
                    tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.conflict_row", stripColor(config.displayName)));
                    count++;
                }
                if (rejectedBySets.size() > MAX_DISPLAY_COUNT) {
                    tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.conflict_more", rejectedBySets.size() - MAX_DISPLAY_COUNT));
                }
            } else {
                tooltip.add(KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.hold_alt_conflict"));
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

    public static void onGatherTooltipComponents(KineticItemTooltips.GatherContext context) {
        if (!ArmorConfig.enableSets) return;
        ItemStack itemStack = context.stack();
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

        if (shouldReplaceOriginalTooltip) keepOnlyItemName(context);
        addText(context, Component.empty());

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
            addText(context, KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.name_with_pieces", stripColor(config.displayName), shownPieceCount, total));
            if (isDetailKeyDown(config)) {
                addSetDetails(context, config, ArmorCache.isSetActive(config.id), anyActive, tooltipState);
            } else if (!"none".equalsIgnoreCase(getTipKey(config))) {
                addText(context, KineticI18n.translatable("gui.kineticarmory.armorsets.tooltip.hold_key_details", getTipKeyDisplayName(config)));
            }
        }
    }

    private static TooltipSetState buildClientTooltipSetState(ArmorDataConfig config) {
        var player = KineticClientRuntime.localPlayer();
        if (player == null) {
            return new TooltipSetState(ArmorCache.getSetPieceCount(config.id));
        }
        return calculateClientTooltipSetState(config, player);
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

    private static void keepOnlyItemName(KineticItemTooltips.GatherContext context) {
        context.keepOnlyFirst();
    }

    private static void addSetDetails(KineticItemTooltips.GatherContext context, ArmorDataConfig config, boolean active, boolean anyActive, TooltipSetState tooltipState) {
        config.preparePieceBonusData();
        int pieceCount = tooltipState.pieceCount();
        int total = Math.max(1, config.getTotalPieceCount());
        List<ArmorTipGenerator.TooltipLine> lines = ArmorTipGenerator.buildTooltipLines(config, pieceCount, false);
        if (lines.isEmpty()) return;

        for (ArmorTipGenerator.TooltipLine line : lines) {
            if (line.text() == null || line.text().isBlank()) continue;
            if (line.iconLine()) {
                addIconTip(context, line.text(), line.isActive(pieceCount, total, active), line.hasAnyActive(pieceCount, anyActive));
            } else {
                addText(context, Component.literal(line.text()));
            }
        }
    }

    private static void addText(KineticItemTooltips.GatherContext context, Component component) {
        context.addText(component);
    }

    private static void addIconTip(KineticItemTooltips.GatherContext context, String text, boolean active, boolean anyActive) {
        context.addComponent(new IconTipTooltipData(text, active, anyActive));
    }

    private static boolean isDetailKeyDown(ArmorDataConfig config) {
        String key = getTipKey(config);
        return switch (key) {
            case "ctrl", "control" -> KineticClientRuntime.controlModifierDown();
            case "alt" -> KineticClientRuntime.altModifierDown();
            case "none" -> true;
            default -> KineticClientRuntime.shiftModifierDown();
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
            case "ctrl", "control" -> KineticI18n.translatable("gui.kineticarmory.armorsets.key.ctrl");
            case "alt" -> KineticI18n.translatable("gui.kineticarmory.armorsets.key.alt");
            case "none" -> KineticI18n.translatable("gui.kineticarmory.armorsets.key.none");
            default -> KineticI18n.translatable("gui.kineticarmory.armorsets.key.shift");
        };
    }

    private static String stripColor(String text) {
        return text == null ? "" : text.replaceAll(COLOR_CODE_REGEX, "");
    }

    private record TooltipSetState(int pieceCount) {}

    public record IconTipTooltipData(String rawText, boolean isActive, boolean anyActive) implements TooltipComponent {}

    public static class ClientIconTipComponent implements KineticTooltipComponent {
        private static final int ICON_SLOT_SIZE = 16;
        private static final int ICON_ADVANCE = ICON_SLOT_SIZE + 4;
        private final String rawText;
        private final boolean isActive;
        private final boolean anyActive;

        public ClientIconTipComponent(IconTipTooltipData data) {
            this.rawText = data.rawText();
            this.isActive = data.isActive();
            this.anyActive = data.anyActive();
        }

        @Override
        public int height() {
            Matcher matcher = ICON_PATTERN.matcher(rawText);
            while (matcher.find()) if (matcher.group(1).equals("item")) return ICON_SLOT_SIZE + 4;
            return 10;
        }

        @Override
        public int width() {
            int width = 0;
            int lastEnd = 0;
            Matcher matcher = ICON_PATTERN.matcher(rawText);
            while (matcher.find()) {
                String plain = rawText.substring(lastEnd, matcher.start());
                width += KineticText.width(activeFormatting(lastEnd) + (isActive ? plain : stripColor(plain)));
                if (matcher.group(1).equals("item")) width += ICON_ADVANCE;
                lastEnd = matcher.end();
            }
            String tail = rawText.substring(lastEnd);
            width += KineticText.width(activeFormatting(lastEnd) + (isActive ? tail : stripColor(tail)));
            return width;
        }

        @Override
        public void render(KineticGraphics g, int x, int y) {
            String prefix = this.isActive ? "§f" : (this.anyActive ? "§m" : "§f");
            int color = this.isActive ? -1 : (this.anyActive ? 0xFFBBBBBB : -1);
            Matcher matcher = ICON_PATTERN.matcher(this.rawText);
            int currentX = x;
            int textY = y + (height() - KineticText.lineHeight()) / 2;
            int lastEnd = 0;
            while (matcher.find()) {
                String plain = rawText.substring(lastEnd, matcher.start());
                String formatting = activeFormatting(lastEnd);
                g.text(Component.literal(prefix + formatting + (isActive ? plain : stripColor(plain))), currentX, textY, color, true);
                currentX += KineticText.width(formatting + (isActive ? plain : stripColor(plain)));
                String type = matcher.group(1);
                String id = matcher.group(2);
                if (type.equals("item")) {
                    ResourceLocation rl = KineticResourceIds.tryParse(id);
                    if (rl != null) {
                        var item = KineticRegistries.items().get(rl);
                        if (item != null && item != net.minecraft.world.item.Items.AIR) {
                            KineticTheme.itemSlot(g, currentX + 2, y + 2, ICON_SLOT_SIZE, false);
                            KineticTheme.item(g, new ItemStack(item), currentX + 2, y + 2, ICON_SLOT_SIZE, 0.625F, false);
                        }
                    }
                }
                if (type.equals("item")) currentX += ICON_ADVANCE;
                lastEnd = matcher.end();
            }
            String tail = rawText.substring(lastEnd);
            g.text(Component.literal(prefix + activeFormatting(lastEnd) + (isActive ? tail : stripColor(tail))), currentX, textY, color, true);
        }

        private String activeFormatting(int end) {
            if (!isActive) return "";
            Matcher matcher = Pattern.compile(COLOR_CODE_REGEX).matcher(rawText.substring(0, end));
            StringBuilder formatting = new StringBuilder();
            while (matcher.find()) formatting.append(matcher.group());
            return formatting.toString();
        }
    }

    public record RejectedTooltipData(List<String> itemIds) implements TooltipComponent {}

    public static class ClientRejectedTooltipComponent implements KineticTooltipComponent {
        private static final int COLUMNS = 9;
        private static final int SLOT_SIZE = 22;
        private static final int SLOT_GAP = 2;
        private static final int SLOT_PITCH = SLOT_SIZE + SLOT_GAP;
        private final List<ItemStack> stacks;

        public ClientRejectedTooltipComponent(RejectedTooltipData data) {
            this.stacks = data.itemIds().stream()
                    .map(id -> new ItemStack(Objects.requireNonNull(KineticRegistries.items().get(KineticResourceIds.tryParse(id)))))
                    .collect(Collectors.toList());
        }

        @Override
        public int height() {
            int rows = (stacks.size() + COLUMNS - 1) / COLUMNS;
            return rows == 0 ? 0 : rows * SLOT_PITCH - SLOT_GAP;
        }

        @Override
        public int width() {
            int columns = Math.min(stacks.size(), COLUMNS);
            return columns == 0 ? 0 : columns * SLOT_PITCH - SLOT_GAP;
        }

        @Override
        public void render(KineticGraphics g, int x, int y) {
            for (int i = 0; i < stacks.size(); i++) {
                int slotX = x + (i % COLUMNS) * SLOT_PITCH;
                int slotY = y + (i / COLUMNS) * SLOT_PITCH;
                KineticTheme.itemSlot(g, slotX, slotY, SLOT_SIZE, false);
                KineticTheme.item(g, stacks.get(i), slotX, slotY, SLOT_SIZE, 1.0F, false);
            }
        }
    }

    public static void registerTooltipComponents() {
        KineticItemTooltips.registerComponentFactory(RejectedTooltipData.class, ClientRejectedTooltipComponent::new);
        KineticItemTooltips.registerComponentFactory(IconTipTooltipData.class, ClientIconTipComponent::new);
    }
}
