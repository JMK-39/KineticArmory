package dev.xyat.kineticarmory.armorsets.config;

import dev.xyat.kineticcore.api.config.client.KTClientConfigAdapter;
import dev.xyat.kineticcore.api.config.client.KTClientConfigSpec;

import java.util.Locale;

public final class ArmorClientConfig {
    private static KTClientConfigSpec spec;
    private static KTClientConfigSpec.EnumValue<TipKey> defaultTipKey;

    private ArmorClientConfig() {
    }

    public static void register(String legacyDefault) {
        if (spec != null) return;

        KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
        defaultTipKey = builder
                .comment("Default key held to show armor-set details: shift, ctrl, alt, or none. / 查看套装详情时默认按住的按键：shift、ctrl、alt 或 none。")
                .translation("cfg.kineticarmory.armorsets.tip_key")
                .defineEnum("defaultTipKey", TipKey.fromLegacy(legacyDefault));
        spec = builder.build();
        KTClientConfigAdapter.registerSpec(spec, "kineticcore/armorsets_client.toml");
    }

    public static KTClientConfigSpec spec() {
        if (spec == null) throw new IllegalStateException("Armor client config has not been registered");
        return spec;
    }

    public static String defaultTipKey() {
        return defaultTipKey == null
                ? "shift"
                : defaultTipKey.get().name().toLowerCase(Locale.ROOT);
    }

    public enum TipKey {
        SHIFT,
        CTRL,
        ALT,
        NONE;

        private static TipKey fromLegacy(String value) {
            if (value == null) return SHIFT;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SHIFT;
            }
        }
    }
}
