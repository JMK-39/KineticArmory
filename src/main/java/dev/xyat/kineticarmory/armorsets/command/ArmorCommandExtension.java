package dev.xyat.kineticarmory.armorsets.command;

import dev.xyat.kineticarmory.util.ColorText;
import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticcore.command.KTCommandApi;
import dev.xyat.kineticcore.command.KTCommandExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;

public final class ArmorCommandExtension implements KTCommandExtension {
    private ArmorCommandExtension() {
    }

    public static void install() {
        KTCommandApi.register(KineticArmory.MODID, new ArmorCommandExtension());
    }

    @Override
    public void reload(CommandSourceStack source) {
        ArmorCommand.executeReload(source);
        if (!ArmorLoader.FAILED_SETS.isEmpty()) {
            String failedList = String.join(", ", ArmorLoader.FAILED_SETS);
            source.sendSuccess(() -> ColorText.translatable(
                    "msg.kineticarmory.armorsets.load_error",
                    failedList
            ).withStyle(ChatFormatting.RED), true);
        }
    }
}
