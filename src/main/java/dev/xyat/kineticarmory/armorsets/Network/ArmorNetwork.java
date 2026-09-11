package dev.xyat.kineticarmory.armorsets.Network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.api.NetworkCompressUtil;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

public class ArmorNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final int MAX_ACTIVE_SET_COUNT = 4096;
    private static final int MAX_CONFIG_COUNT = 512;
    private static final int MAX_ENTITY_RULE_COUNT = 4096;
    private static final int MAX_ENTITY_RULE_LENGTH = 512;
    private static final int MAX_COMPRESSED_CONFIG_BYTES = 256 * 1024;
    private static final int MAX_DECOMPRESSED_CONFIG_BYTES = 1024 * 1024;
    private static final int MAX_TOTAL_COMPRESSED_CONFIG_BYTES = 4 * 1024 * 1024;
    private static final int MAX_TOTAL_DECOMPRESSED_CONFIG_BYTES = 16 * 1024 * 1024;
    private static final int MAX_JSON_DEPTH = 64;
    private static final int MAX_JSON_COLLECTION_SIZE = 4096;
    private static final int MAX_JSON_STRING_LENGTH = 32_767;
    private static final int MAX_JSON_NODES = 65_536;
    private static final int MAX_FILTER_MODE_LENGTH = 16;
    public static SimpleChannel CHANNEL;
    private static final Gson GSON = new Gson();

    public static void register(IEventBus modEventBus) {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(KineticArmory.MODID, "armorsets"),
                () -> PROTOCOL_VERSION,
                KTNetworkProtocol::acceptsAnyVersion,
                KTNetworkProtocol::acceptsAnyVersion
        );

        int id = 0;
        CHANNEL.messageBuilder(SyncActiveSetsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncActiveSetsPacket::new).encoder(SyncActiveSetsPacket::toBytes)
                .consumerNetworkThread(SyncActiveSetsPacket::handle).add();
        CHANNEL.messageBuilder(SyncArmorConfigsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncArmorConfigsPacket::new).encoder(SyncArmorConfigsPacket::toBytes)
                .consumerNetworkThread(SyncArmorConfigsPacket::handle).add();
        CHANNEL.messageBuilder(SyncEntityFilterPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncEntityFilterPacket::new).encoder(SyncEntityFilterPacket::toBytes)
                .consumerNetworkThread(SyncEntityFilterPacket::handle).add();
        CHANNEL.messageBuilder(RequestEntityFilterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestEntityFilterPacket::new).encoder(RequestEntityFilterPacket::toBytes)
                .consumerNetworkThread(RequestEntityFilterPacket::handle).add();
        CHANNEL.messageBuilder(RequestReloadArmorSetsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestReloadArmorSetsPacket::new).encoder(RequestReloadArmorSetsPacket::toBytes)
                .consumerNetworkThread(RequestReloadArmorSetsPacket::handle).add();
        CHANNEL.messageBuilder(SaveArmorSetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveArmorSetPacket::new).encoder(SaveArmorSetPacket::toBytes)
                .consumerNetworkThread(SaveArmorSetPacket::handle).add();
        CHANNEL.messageBuilder(DeleteArmorSetPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeleteArmorSetPacket::new).encoder(DeleteArmorSetPacket::toBytes)
                .consumerNetworkThread(DeleteArmorSetPacket::handle).add();
        CHANNEL.messageBuilder(SaveEntityFilterPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveEntityFilterPacket::new).encoder(SaveEntityFilterPacket::toBytes)
                .consumerNetworkThread(SaveEntityFilterPacket::handle).add();
        CHANNEL.messageBuilder(ClientInputStatePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClientInputStatePacket::new).encoder(ClientInputStatePacket::toBytes)
                .consumerNetworkThread(ClientInputStatePacket::handle).add();
        CHANNEL.messageBuilder(RequestOpenEditorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestOpenEditorPacket::new).encoder(RequestOpenEditorPacket::toBytes)
                .consumerNetworkThread(RequestOpenEditorPacket::handle).add();
        CHANNEL.messageBuilder(EditorSaveResultPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(EditorSaveResultPacket::new).encoder(EditorSaveResultPacket::toBytes)
                .consumerNetworkThread(EditorSaveResultPacket::handle).add();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmorNetworkClient.registerClient(modEventBus));
    }

    private static NetworkEvent.Context contextForSide(
            Supplier<NetworkEvent.Context> supplier,
            LogicalSide expectedSide
    ) {
        NetworkEvent.Context context = supplier.get();
        NetworkDirection direction = context.getDirection();
        if (direction == null || direction.getReceptionSide() != expectedSide) {
            context.setPacketHandled(true);
            return null;
        }
        return context;
    }

    private static int readBoundedCount(FriendlyByteBuf buf, int maximum, String field) {
        int count = buf.readVarInt();
        if (count < 0 || count > maximum) {
            throw new DecoderException(field + " exceeds limit: " + count);
        }
        return count;
    }

    private static void requireCollectionSize(int size, int maximum, String field) {
        if (size < 0 || size > maximum) {
            throw new EncoderException(field + " exceeds limit: " + size);
        }
    }

    private static void requireUtf(String value, int maximum, String field) {
        if (value == null || value.length() > maximum) {
            throw new EncoderException(field + " exceeds limit");
        }
    }

    private static DecodedJson readCompressedJson(FriendlyByteBuf buf) {
        byte[] compressed = buf.readByteArray(MAX_COMPRESSED_CONFIG_BYTES);
        if (compressed.length == 0) {
            throw new DecoderException("Empty compressed armor config");
        }

        try {
            byte[] raw = NetworkCompressUtil.decompressBytes(compressed, MAX_DECOMPRESSED_CONFIG_BYTES);
            return new DecodedJson(new String(raw, StandardCharsets.UTF_8), compressed.length, raw.length);
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid compressed armor config", exception);
        }
    }

    private static EncodedJson encodeCompressedJson(ArmorDataConfig config) {
        if (config == null) throw new EncoderException("Missing armor config");
        String json = GSON.toJson(config);
        int rawBytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (rawBytes == 0 || rawBytes > MAX_DECOMPRESSED_CONFIG_BYTES) {
            throw new EncoderException("Armor config JSON exceeds limit");
        }
        try {
            validateJsonText(json);
            JsonElement element = JsonParser.parseString(json);
            validateJson(element, 0, new JsonBudget());
        } catch (RuntimeException e) {
            throw new EncoderException("Armor config JSON exceeds structural limits", e);
        }
        byte[] compressed = NetworkCompressUtil.compress(json);
        if (compressed.length == 0 || compressed.length > MAX_COMPRESSED_CONFIG_BYTES) {
            throw new EncoderException("Compressed armor config exceeds limit");
        }
        return new EncodedJson(compressed, rawBytes);
    }

    private static ArmorDataConfig decodeArmorConfig(String setId, DecodedJson decoded) {
        try {
            validateJsonText(decoded.json());
            JsonElement element = JsonParser.parseString(decoded.json());
            validateJson(element, 0, new JsonBudget());
            ArmorDataConfig config = GSON.fromJson(element, ArmorDataConfig.class);
            if (config == null) throw new DecoderException("Missing armor config JSON object");
            config.id = setId;
            config.initNullFields();
            config.prepareRuntimeCache();
            return config;
        } catch (DecoderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DecoderException("Invalid armor config JSON", e);
        }
    }

    /** Performs a non-recursive depth/token pass before Gson allocates the JSON tree. */
    private static void validateJsonText(String json) {
        int depth = 0;
        int stringChars = 0;
        int tokenChars = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);
            if (inString) {
                if (!escaped && current == '"') {
                    inString = false;
                    stringChars = 0;
                    continue;
                }
                if (++stringChars > MAX_JSON_STRING_LENGTH * 6) {
                    throw new IllegalArgumentException("JSON string token limit exceeded");
                }
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                continue;
            }

            if (current == '"') {
                inString = true;
                stringChars = 0;
                tokenChars = 0;
            } else if (current == '{' || current == '[') {
                if (++depth > MAX_JSON_DEPTH) {
                    throw new IllegalArgumentException("JSON nesting limit exceeded");
                }
                tokenChars = 0;
            } else if (current == '}' || current == ']') {
                if (--depth < 0) throw new IllegalArgumentException("Unbalanced JSON nesting");
                tokenChars = 0;
            } else if (current == ',' || current == ':' || Character.isWhitespace(current)) {
                tokenChars = 0;
            } else if (++tokenChars > MAX_JSON_STRING_LENGTH) {
                throw new IllegalArgumentException("JSON primitive token limit exceeded");
            }
        }
        if (inString || depth != 0) throw new IllegalArgumentException("Unbalanced JSON document");
    }

    private static void validateJson(JsonElement element, int depth, JsonBudget budget) {
        if (depth > MAX_JSON_DEPTH || ++budget.nodes > MAX_JSON_NODES) {
            throw new IllegalArgumentException("JSON nesting or node limit exceeded");
        }
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.size() > MAX_JSON_COLLECTION_SIZE) {
                throw new IllegalArgumentException("JSON object member limit exceeded");
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getKey().length() > MAX_JSON_STRING_LENGTH) {
                    throw new IllegalArgumentException("JSON object key limit exceeded");
                }
                validateJson(entry.getValue(), depth + 1, budget);
            }
        } else if (element.isJsonArray()) {
            if (element.getAsJsonArray().size() > MAX_JSON_COLLECTION_SIZE) {
                throw new IllegalArgumentException("JSON array element limit exceeded");
            }
            for (JsonElement child : element.getAsJsonArray()) {
                validateJson(child, depth + 1, budget);
            }
        } else if (element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isString()
                && element.getAsString().length() > MAX_JSON_STRING_LENGTH) {
            throw new IllegalArgumentException("JSON string limit exceeded");
        }
    }

    private static void notifyInvalidSetId(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("msg.kineticarmory.armorsets.invalid_id"));
    }

    private static void resyncArmorConfigs(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, false));
    }

    private record DecodedJson(String json, int compressedBytes, int decompressedBytes) {}
    private record EncodedJson(byte[] compressed, int decompressedBytes) {}
    private static final class JsonBudget {
        private int nodes;
    }

    public record SyncActiveSetsPacket(List<String> activeSets, Map<String, Integer> pieceCounts) {
        public SyncActiveSetsPacket(FriendlyByteBuf buf) {
            this(readList(buf), readPieceCounts(buf));
        }

        private static List<String> readList(FriendlyByteBuf buf) {
            int size = readBoundedCount(buf, MAX_ACTIVE_SET_COUNT, "active armor set count");
            List<String> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH));
            return list;
        }

        private static Map<String, Integer> readPieceCounts(FriendlyByteBuf buf) {
            int size = readBoundedCount(buf, MAX_ACTIVE_SET_COUNT, "armor set piece-count entries");
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                map.put(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH), Math.max(0, buf.readVarInt()));
            }
            return map;
        }

        public void toBytes(FriendlyByteBuf buf) {
            requireCollectionSize(activeSets.size(), MAX_ACTIVE_SET_COUNT, "active armor set count");
            buf.writeVarInt(activeSets.size());
            for (String s : activeSets) {
                requireUtf(s, ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
                buf.writeUtf(s, ArmorLoader.MAX_SET_ID_LENGTH);
            }
            requireCollectionSize(pieceCounts.size(), MAX_ACTIVE_SET_COUNT, "armor set piece-count entries");
            buf.writeVarInt(pieceCounts.size());
            for (Map.Entry<String, Integer> entry : pieceCounts.entrySet()) {
                requireUtf(entry.getKey(), ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
                buf.writeUtf(entry.getKey(), ArmorLoader.MAX_SET_ID_LENGTH);
                buf.writeVarInt(Math.max(0, entry.getValue()));
            }
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmorNetworkClient.handleSyncActiveSets(this)));
            context.setPacketHandled(true);
        }
    }

    public record SyncArmorConfigsPacket(Map<String, ArmorDataConfig> configs, boolean openGui) {
        public SyncArmorConfigsPacket(FriendlyByteBuf buf) { this(readConfigs(buf), buf.readBoolean()); }
        private static Map<String, ArmorDataConfig> readConfigs(FriendlyByteBuf buf) {
            Map<String, ArmorDataConfig> map = new LinkedHashMap<>();
            int size = readBoundedCount(buf, MAX_CONFIG_COUNT, "armor config count");
            long totalCompressed = 0;
            long totalDecompressed = 0;
            for (int i = 0; i < size; i++) {
                String setId = buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH);
                DecodedJson decoded = readCompressedJson(buf);
                totalCompressed += decoded.compressedBytes();
                totalDecompressed += decoded.decompressedBytes();
                if (totalCompressed > MAX_TOTAL_COMPRESSED_CONFIG_BYTES
                        || totalDecompressed > MAX_TOTAL_DECOMPRESSED_CONFIG_BYTES) {
                    throw new DecoderException("Armor config packet exceeds aggregate limit");
                }
                map.put(setId, decodeArmorConfig(setId, decoded));
            }
            return map;
        }
        public void toBytes(FriendlyByteBuf buf) {
            requireCollectionSize(configs.size(), MAX_CONFIG_COUNT, "armor config count");
            buf.writeVarInt(configs.size());
            long totalCompressed = 0;
            long totalDecompressed = 0;
            for (Map.Entry<String, ArmorDataConfig> entry : configs.entrySet()) {
                requireUtf(entry.getKey(), ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
                EncodedJson encoded = encodeCompressedJson(entry.getValue());
                totalCompressed += encoded.compressed().length;
                totalDecompressed += encoded.decompressedBytes();
                if (totalCompressed > MAX_TOTAL_COMPRESSED_CONFIG_BYTES
                        || totalDecompressed > MAX_TOTAL_DECOMPRESSED_CONFIG_BYTES) {
                    throw new EncoderException("Armor config packet exceeds aggregate limit");
                }
                buf.writeUtf(entry.getKey(), ArmorLoader.MAX_SET_ID_LENGTH);
                buf.writeByteArray(encoded.compressed());
            }
            buf.writeBoolean(openGui);
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmorNetworkClient.handleSyncConfigs(this)));
            context.setPacketHandled(true);
        }
    }

    public record SyncEntityFilterPacket(String mode, List<String> rules, boolean openGui) {
        public SyncEntityFilterPacket(FriendlyByteBuf buf) {
            this(buf.readUtf(MAX_FILTER_MODE_LENGTH), readRules(buf), buf.readBoolean());
        }

        private static List<String> readRules(FriendlyByteBuf buf) {
            int size = readBoundedCount(buf, MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            List<String> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rules.add(buf.readUtf(MAX_ENTITY_RULE_LENGTH));
            return rules;
        }

        public void toBytes(FriendlyByteBuf buf) {
            String safeMode = mode == null ? "BLACKLIST" : mode;
            requireUtf(safeMode, MAX_FILTER_MODE_LENGTH, "entity filter mode");
            buf.writeUtf(safeMode, MAX_FILTER_MODE_LENGTH);
            requireCollectionSize(rules == null ? 0 : rules.size(), MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            buf.writeVarInt(rules == null ? 0 : rules.size());
            if (rules != null) for (String rule : rules) {
                String safeRule = rule == null ? "" : rule;
                requireUtf(safeRule, MAX_ENTITY_RULE_LENGTH, "entity filter rule");
                buf.writeUtf(safeRule, MAX_ENTITY_RULE_LENGTH);
            }
            buf.writeBoolean(openGui);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ArmorNetworkClient.handleSyncEntityFilter(this)));
            context.setPacketHandled(true);
        }
    }

    public record RequestEntityFilterPacket() {
        public RequestEntityFilterPacket(FriendlyByteBuf buf) {
            this();
        }

        public void toBytes(FriendlyByteBuf buf) {
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) return;
                ArmorConfig.reloadFromDisk();
                CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, true));
            });
            context.setPacketHandled(true);
        }
    }

    public record RequestOpenEditorPacket() {
        public RequestOpenEditorPacket(FriendlyByteBuf buf) {
            this();
        }

        public void toBytes(FriendlyByteBuf buf) {
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.translatable("commands.generic.permission"));
                    return;
                }

                ArmorConfig.reloadFromDisk();
                ArmorLoader.load();
                CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, false));
                CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, true));
            });
            context.setPacketHandled(true);
        }
    }

    public record ClientInputStatePacket(int leftTicks, int rightTicks, boolean leftClick, boolean rightClick) {
        public ClientInputStatePacket(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeVarInt(Math.max(0, leftTicks));
            buf.writeVarInt(Math.max(0, rightTicks));
            buf.writeBoolean(leftClick);
            buf.writeBoolean(rightClick);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    ArmorManager.updateClientInputState(player, leftTicks, rightTicks, leftClick, rightClick);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record RequestReloadArmorSetsPacket() {
        public RequestReloadArmorSetsPacket(FriendlyByteBuf buf) { this(); }
        public void toBytes(FriendlyByteBuf buf) {}
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && player.hasPermissions(2)) {
                    ArmorCommand.executeReload(player.createCommandSourceStack());
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record SaveArmorSetPacket(ArmorDataConfig config, String oldId) {
        public SaveArmorSetPacket(FriendlyByteBuf buf) {
            this(readConfig(buf), buf.readBoolean() ? buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH) : null);
        }
        private static ArmorDataConfig readConfig(FriendlyByteBuf buf) {
            String id = buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH);
            return decodeArmorConfig(id, readCompressedJson(buf));
        }
        public void toBytes(FriendlyByteBuf buf) {
            if (config == null) throw new EncoderException("Missing armor config");
            requireUtf(config.id, ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
            EncodedJson encoded = encodeCompressedJson(config);
            buf.writeUtf(config.id, ArmorLoader.MAX_SET_ID_LENGTH);
            buf.writeByteArray(encoded.compressed());
            buf.writeBoolean(oldId != null);
            if (oldId != null) {
                requireUtf(oldId, ArmorLoader.MAX_SET_ID_LENGTH, "previous armor set id");
                buf.writeUtf(oldId, ArmorLoader.MAX_SET_ID_LENGTH);
            }
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.translatable("commands.generic.permission"));
                    resyncArmorConfigs(player);
                    sendEditorSaveResult(player, false);
                    return;
                }
                if (config == null) {
                    player.sendSystemMessage(Component.translatable("msg.kineticarmory.armorsets.save_failed", ""));
                    resyncArmorConfigs(player);
                    sendEditorSaveResult(player, false);
                    return;
                }
                if (!ArmorLoader.isSafeSetId(config.id)
                        || (oldId != null && !ArmorLoader.isSafeSetId(oldId))) {
                    notifyInvalidSetId(player);
                    resyncArmorConfigs(player);
                    sendEditorSaveResult(player, false);
                    return;
                }

                ArmorLoader.cleanUpConfig(config);
                config.prepareRuntimeCache();
                if (!ArmorLoader.saveChecked(config.id, config)) {
                    player.sendSystemMessage(Component.translatable(
                            "msg.kineticarmory.armorsets.save_failed",
                            config.id
                    ));
                    resyncArmorConfigs(player);
                    sendEditorSaveResult(player, false);
                    return;
                }

                boolean renameCleanupFailed = false;
                if (oldId != null && !oldId.equals(config.id)) {
                    try {
                        Path oldFile = ArmorLoader.resolveConfigPath(oldId);
                        Path newFile = ArmorLoader.resolveConfigPath(config.id);
                        boolean sameTarget = oldFile.equals(newFile)
                                || (Files.exists(oldFile) && Files.exists(newFile) && Files.isSameFile(oldFile, newFile));
                        if (!sameTarget && !Files.deleteIfExists(oldFile)) {
                            KineticArmory.LOGGER.warn("重命名时未找到旧套装文件: {}", oldFile.getFileName());
                        }
                    } catch (IOException | IllegalArgumentException e) {
                        renameCleanupFailed = true;
                        KineticArmory.LOGGER.error("无法删除重命名后的旧套装文件: {}", oldId, e);
                    }
                }

                ArmorCommand.executeReload(player.createCommandSourceStack());
                if (!ArmorConfig.syncOnReload) resyncArmorConfigs(player);
                if (renameCleanupFailed) {
                    player.sendSystemMessage(Component.translatable(
                            "msg.kineticarmory.armorsets.rename_cleanup_failed",
                            oldId
                    ));
                }
                sendEditorSaveResult(player, true);
            });
            context.setPacketHandled(true);
        }
    }

    public record SaveEntityFilterPacket(String mode, List<String> rules) {
        public SaveEntityFilterPacket(FriendlyByteBuf buf) {
            this(buf.readUtf(MAX_FILTER_MODE_LENGTH), readRules(buf));
        }

        private static List<String> readRules(FriendlyByteBuf buf) {
            int size = readBoundedCount(buf, MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            List<String> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rules.add(buf.readUtf(MAX_ENTITY_RULE_LENGTH));
            return rules;
        }

        public void toBytes(FriendlyByteBuf buf) {
            String safeMode = mode == null ? "BLACKLIST" : mode;
            requireUtf(safeMode, MAX_FILTER_MODE_LENGTH, "entity filter mode");
            buf.writeUtf(safeMode, MAX_FILTER_MODE_LENGTH);
            requireCollectionSize(rules == null ? 0 : rules.size(), MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            buf.writeVarInt(rules == null ? 0 : rules.size());
            if (rules != null) for (String rule : rules) {
                String safeRule = rule == null ? "" : rule;
                requireUtf(safeRule, MAX_ENTITY_RULE_LENGTH, "entity filter rule");
                buf.writeUtf(safeRule, MAX_ENTITY_RULE_LENGTH);
            }
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    sendEditorSaveResult(player, false);
                    return;
                }
                ArmorConfig.entityFilterMode = "BLACKLIST".equalsIgnoreCase(mode) ? "BLACKLIST" : "WHITELIST";
                ArmorConfig.allowedEntities = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
                ArmorConfig.save();
                ArmorManager.forceRecalculateAll(player.getServer());
                CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), new SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, false));
                sendEditorSaveResult(player, true);
            });
            context.setPacketHandled(true);
        }
    }

    private static void sendEditorSaveResult(ServerPlayer player, boolean success) {
        if (player == null) return;
        CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new EditorSaveResultPacket(success)
        );
    }

    public record EditorSaveResultPacket(boolean success) {
        public EditorSaveResultPacket(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(success);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.CLIENT);
            if (context == null) return;
            context.enqueueWork(() -> ArmorNetworkClient.handleEditorSaveResult(success));
            context.setPacketHandled(true);
        }
    }

    public record DeleteArmorSetPacket(String id) {
        public DeleteArmorSetPacket(FriendlyByteBuf buf) { this(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH)); }
        public void toBytes(FriendlyByteBuf buf) {
            requireUtf(id, ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
            buf.writeUtf(id, ArmorLoader.MAX_SET_ID_LENGTH);
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context context = contextForSide(ctx, LogicalSide.SERVER);
            if (context == null) return;
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.translatable("commands.generic.permission"));
                    resyncArmorConfigs(player);
                    return;
                }
                if (!ArmorLoader.isSafeSetId(id)) {
                    notifyInvalidSetId(player);
                    resyncArmorConfigs(player);
                    return;
                }
                try {
                    Files.deleteIfExists(ArmorLoader.resolveConfigPath(id));
                    ArmorCommand.executeReload(player.createCommandSourceStack());
                    if (!ArmorConfig.syncOnReload) resyncArmorConfigs(player);
                    player.sendSystemMessage(Component.translatable("msg.kineticarmory.common.deleted"));
                } catch (IOException | IllegalArgumentException e) {
                    KineticArmory.LOGGER.error("ArmorSet 删除失败: {}", id, e);
                    player.sendSystemMessage(Component.translatable(
                            "msg.kineticarmory.armorsets.delete_failed",
                            id
                    ));
                    resyncArmorConfigs(player);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
