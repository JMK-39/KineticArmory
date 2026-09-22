package dev.xyat.kineticarmory.armorsets.Network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.config.ArmorConfig;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.event.ArmorManager;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticcore.api.network.KineticCompression;
import dev.xyat.kineticcore.api.network.NetworkBuffer;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.PacketChannel;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.network.ServerPacketContext;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final PacketChannel CHANNEL = PacketChannel.create(
            KineticResourceIds.of(KineticArmory.MODID, "armorsets"),
            PROTOCOL_VERSION,
            NetworkVersionPolicy.ANY
    );
    private static boolean syncActiveSetsRegistered;
    private static boolean syncArmorConfigsRegistered;
    private static boolean syncEntityFilterRegistered;
    private static boolean requestEntityFilterRegistered;
    private static boolean requestReloadRegistered;
    private static boolean saveArmorSetRegistered;
    private static boolean deleteArmorSetRegistered;
    private static boolean saveEntityFilterRegistered;
    private static boolean clientInputRegistered;
    private static boolean requestOpenEditorRegistered;
    private static boolean editorSaveResultRegistered;
    private static final Gson GSON = new Gson();



    public static synchronized void register() {
        PacketRegistrations.runIndependent(
                () -> {
                    if (!syncActiveSetsRegistered) {
                        CHANNEL.registerClientbound(0, SyncActiveSetsPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), SyncActiveSetsPacket::new),
                                message -> ArmorNetworkClient.handleSyncActiveSets(message));
                        syncActiveSetsRegistered = true;
                    }
                },
                () -> {
                    if (!syncArmorConfigsRegistered) {
                        CHANNEL.registerClientbound(1, SyncArmorConfigsPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), SyncArmorConfigsPacket::new),
                                message -> ArmorNetworkClient.handleSyncConfigs(message));
                        syncArmorConfigsRegistered = true;
                    }
                },
                () -> {
                    if (!syncEntityFilterRegistered) {
                        CHANNEL.registerClientbound(2, SyncEntityFilterPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), SyncEntityFilterPacket::new),
                                message -> ArmorNetworkClient.handleSyncEntityFilter(message));
                        syncEntityFilterRegistered = true;
                    }
                },
                () -> {
                    if (!requestEntityFilterRegistered) {
                        CHANNEL.registerServerbound(3, RequestEntityFilterPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), RequestEntityFilterPacket::new),
                                ArmorNetwork::handleRequestEntityFilter);
                        requestEntityFilterRegistered = true;
                    }
                },
                () -> {
                    if (!requestReloadRegistered) {
                        CHANNEL.registerServerbound(4, RequestReloadArmorSetsPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), RequestReloadArmorSetsPacket::new),
                                ArmorNetwork::handleRequestReload);
                        requestReloadRegistered = true;
                    }
                },
                () -> {
                    if (!saveArmorSetRegistered) {
                        CHANNEL.registerServerbound(5, SaveArmorSetPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), SaveArmorSetPacket::new),
                                ArmorNetwork::handleSaveArmorSet);
                        saveArmorSetRegistered = true;
                    }
                },
                () -> {
                    if (!deleteArmorSetRegistered) {
                        CHANNEL.registerServerbound(6, DeleteArmorSetPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), DeleteArmorSetPacket::new),
                                ArmorNetwork::handleDeleteArmorSet);
                        deleteArmorSetRegistered = true;
                    }
                },
                () -> {
                    if (!saveEntityFilterRegistered) {
                        CHANNEL.registerServerbound(7, SaveEntityFilterPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), SaveEntityFilterPacket::new),
                                ArmorNetwork::handleSaveEntityFilter);
                        saveEntityFilterRegistered = true;
                    }
                },
                () -> {
                    if (!clientInputRegistered) {
                        CHANNEL.registerServerbound(8, ClientInputStatePacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), ClientInputStatePacket::new),
                                ArmorNetwork::handleClientInputState);
                        clientInputRegistered = true;
                    }
                },
                () -> {
                    if (!requestOpenEditorRegistered) {
                        CHANNEL.registerServerbound(9, RequestOpenEditorPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), RequestOpenEditorPacket::new),
                                ArmorNetwork::handleRequestOpenEditor);
                        requestOpenEditorRegistered = true;
                    }
                },
                () -> {
                    if (!editorSaveResultRegistered) {
                        CHANNEL.registerClientbound(10, EditorSaveResultPacket.class,
                                NetworkCodec.of((buffer, message) -> message.encode(buffer), EditorSaveResultPacket::new),
                                message -> ArmorNetworkClient.handleEditorSaveResult(message.success()));
                        editorSaveResultRegistered = true;
                    }
                }
        );
    }

    public static void requestEntityFilter() {
        CHANNEL.sendToServer(new RequestEntityFilterPacket());
    }

    public static void requestOpenEditor() {
        CHANNEL.sendToServer(new RequestOpenEditorPacket());
    }

    public static void sendClientInputState(int leftTicks, int rightTicks, boolean leftClick, boolean rightClick) {
        CHANNEL.sendToServer(new ClientInputStatePacket(leftTicks, rightTicks, leftClick, rightClick));
    }

    public static void saveArmorSet(ArmorDataConfig config, String oldId) {
        CHANNEL.sendToServer(new SaveArmorSetPacket(config, oldId));
    }

    public static void deleteArmorSet(String id) {
        CHANNEL.sendToServer(new DeleteArmorSetPacket(id));
    }

    public static void saveEntityFilter(String mode, List<String> rules) {
        CHANNEL.sendToServer(new SaveEntityFilterPacket(mode, rules));
    }

    public static void syncActiveSets(ServerPlayer player, List<String> activeSets, Map<String, Integer> pieceCounts) {
        if (player != null) CHANNEL.sendToPlayer(player, new SyncActiveSetsPacket(activeSets, pieceCounts));
    }

    public static void syncEntityFilter(ServerPlayer player, boolean openGui) {
        if (player != null) CHANNEL.sendToPlayer(player,
                new SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, openGui));
    }

    public static void syncArmorConfigs(ServerPlayer player, boolean openGui) {
        if (player != null) CHANNEL.sendToPlayer(player, new SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, openGui));
    }

    public static void broadcastEntityFilter() {
        CHANNEL.broadcast(new SyncEntityFilterPacket(ArmorConfig.entityFilterMode, ArmorConfig.allowedEntities, false));
    }

    public static void broadcastArmorConfigs() {
        CHANNEL.broadcast(new SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, false));
    }

    private static int readBoundedCount(NetworkBuffer buf, int maximum, String field) {
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

    private static DecodedJson readCompressedJson(NetworkBuffer buf) {
        byte[] compressed = buf.readByteArray(MAX_COMPRESSED_CONFIG_BYTES);
        if (compressed.length == 0) {
            throw new DecoderException("Empty compressed armor config");
        }

        try {
            byte[] raw = KineticCompression.decompressBytes(compressed, MAX_DECOMPRESSED_CONFIG_BYTES);
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
        byte[] compressed = KineticCompression.compressUtf8(json, MAX_COMPRESSED_CONFIG_BYTES, MAX_DECOMPRESSED_CONFIG_BYTES);
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

    private static void handleRequestEntityFilter(RequestEntityFilterPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        if (!player.hasPermissions(2)) return;
        ArmorConfig.reloadFromDisk();
        syncEntityFilter(player, true);
    }

    private static void handleRequestOpenEditor(RequestOpenEditorPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("commands.generic.permission"));
            return;
        }
        ArmorConfig.reloadFromDisk();
        ArmorLoader.load();
        syncEntityFilter(player, false);
        syncArmorConfigs(player, true);
    }

    private static void handleClientInputState(ClientInputStatePacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        ArmorManager.updateClientInputState(
                player,
                message.leftTicks(),
                message.rightTicks(),
                message.leftClick(),
                message.rightClick()
        );
    }

    private static void handleRequestReload(RequestReloadArmorSetsPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        if (player.hasPermissions(2)) {
            ArmorCommand.executeReload(player.createCommandSourceStack());
        }
    }

    private static void handleSaveArmorSet(SaveArmorSetPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        ArmorDataConfig config = message.config();
        String oldId = message.oldId();
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
    }

    private static void handleSaveEntityFilter(SaveEntityFilterPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        if (!player.hasPermissions(2)) {
            sendEditorSaveResult(player, false);
            return;
        }
        ArmorConfig.entityFilterMode = "BLACKLIST".equalsIgnoreCase(message.mode()) ? "BLACKLIST" : "WHITELIST";
        ArmorConfig.allowedEntities = message.rules() == null ? new ArrayList<>() : new ArrayList<>(message.rules());
        ArmorConfig.save();
        ArmorManager.forceRecalculateAll(player.getServer());
        broadcastEntityFilter();
        sendEditorSaveResult(player, true);
    }

    private static void handleDeleteArmorSet(DeleteArmorSetPacket message, ServerPacketContext context) {
        ServerPlayer player = context.sender();
        String id = message.id();
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
    }

    private static void notifyInvalidSetId(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("msg.kineticarmory.armorsets.invalid_id"));
    }

    private static void resyncArmorConfigs(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.sendToPlayer(player, new SyncArmorConfigsPacket(ArmorLoader.LOADED_SETS, false));
    }

    private record DecodedJson(String json, int compressedBytes, int decompressedBytes) {}
    private record EncodedJson(byte[] compressed, int decompressedBytes) {}
    private static final class JsonBudget {
        private int nodes;
    }

    public record SyncActiveSetsPacket(List<String> activeSets, Map<String, Integer> pieceCounts) {
        public SyncActiveSetsPacket(NetworkBuffer buf) {
            this(readList(buf), readPieceCounts(buf));
        }

        private static List<String> readList(NetworkBuffer buf) {
            int size = readBoundedCount(buf, MAX_ACTIVE_SET_COUNT, "active armor set count");
            List<String> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH));
            return list;
        }

        private static Map<String, Integer> readPieceCounts(NetworkBuffer buf) {
            int size = readBoundedCount(buf, MAX_ACTIVE_SET_COUNT, "armor set piece-count entries");
            Map<String, Integer> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                map.put(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH), Math.max(0, buf.readVarInt()));
            }
            return map;
        }

        private void encode(NetworkBuffer buf) {
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

    }

    public record SyncArmorConfigsPacket(Map<String, ArmorDataConfig> configs, boolean openGui) {
        public SyncArmorConfigsPacket(NetworkBuffer buf) { this(readConfigs(buf), buf.readBoolean()); }
        private static Map<String, ArmorDataConfig> readConfigs(NetworkBuffer buf) {
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
        private void encode(NetworkBuffer buf) {
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
    }

    public record SyncEntityFilterPacket(String mode, List<String> rules, boolean openGui) {
        public SyncEntityFilterPacket(NetworkBuffer buf) {
            this(buf.readUtf(MAX_FILTER_MODE_LENGTH), readRules(buf), buf.readBoolean());
        }

        private static List<String> readRules(NetworkBuffer buf) {
            int size = readBoundedCount(buf, MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            List<String> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rules.add(buf.readUtf(MAX_ENTITY_RULE_LENGTH));
            return rules;
        }

        private void encode(NetworkBuffer buf) {
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

    }

    public record RequestEntityFilterPacket() {
        public RequestEntityFilterPacket(NetworkBuffer buf) {
            this();
        }

        private void encode(NetworkBuffer buf) {
        }

    }

    public record RequestOpenEditorPacket() {
        public RequestOpenEditorPacket(NetworkBuffer buf) {
            this();
        }

        private void encode(NetworkBuffer buf) {
        }

    }

    public record ClientInputStatePacket(int leftTicks, int rightTicks, boolean leftClick, boolean rightClick) {
        public ClientInputStatePacket(NetworkBuffer buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
        }

        private void encode(NetworkBuffer buf) {
            buf.writeVarInt(Math.max(0, leftTicks));
            buf.writeVarInt(Math.max(0, rightTicks));
            buf.writeBoolean(leftClick);
            buf.writeBoolean(rightClick);
        }

    }

    public record RequestReloadArmorSetsPacket() {
        public RequestReloadArmorSetsPacket(NetworkBuffer buf) { this(); }
        private void encode(NetworkBuffer buf) {}
    }

    public record SaveArmorSetPacket(ArmorDataConfig config, String oldId) {
        public SaveArmorSetPacket(NetworkBuffer buf) {
            this(readConfig(buf), buf.readBoolean() ? buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH) : null);
        }
        private static ArmorDataConfig readConfig(NetworkBuffer buf) {
            String id = buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH);
            return decodeArmorConfig(id, readCompressedJson(buf));
        }
        private void encode(NetworkBuffer buf) {
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
    }

    public record SaveEntityFilterPacket(String mode, List<String> rules) {
        public SaveEntityFilterPacket(NetworkBuffer buf) {
            this(buf.readUtf(MAX_FILTER_MODE_LENGTH), readRules(buf));
        }

        private static List<String> readRules(NetworkBuffer buf) {
            int size = readBoundedCount(buf, MAX_ENTITY_RULE_COUNT, "entity filter rule count");
            List<String> rules = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rules.add(buf.readUtf(MAX_ENTITY_RULE_LENGTH));
            return rules;
        }

        private void encode(NetworkBuffer buf) {
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

    }

    private static void sendEditorSaveResult(ServerPlayer player, boolean success) {
        if (player == null) return;
        CHANNEL.sendToPlayer(player, new EditorSaveResultPacket(success));
    }

    public record EditorSaveResultPacket(boolean success) {
        public EditorSaveResultPacket(NetworkBuffer buf) {
            this(buf.readBoolean());
        }

        private void encode(NetworkBuffer buf) {
            buf.writeBoolean(success);
        }

    }

    public record DeleteArmorSetPacket(String id) {
        public DeleteArmorSetPacket(NetworkBuffer buf) { this(buf.readUtf(ArmorLoader.MAX_SET_ID_LENGTH)); }
        private void encode(NetworkBuffer buf) {
            requireUtf(id, ArmorLoader.MAX_SET_ID_LENGTH, "armor set id");
            buf.writeUtf(id, ArmorLoader.MAX_SET_ID_LENGTH);
        }
    }
}
