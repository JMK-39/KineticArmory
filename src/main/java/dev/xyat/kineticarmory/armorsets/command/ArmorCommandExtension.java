package dev.xyat.kineticarmory.armorsets.command;

import dev.xyat.kineticarmory.KineticArmory;
import dev.xyat.kineticarmory.armorsets.ArmorCommand;
import dev.xyat.kineticarmory.armorsets.json.ArmorLoader;
import dev.xyat.kineticcore.api.command.CommandExtension;
import dev.xyat.kineticcore.api.command.KineticCommands;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.commands.CommandSourceStack;

public final class ArmorCommandExtension implements CommandExtension {
    private ArmorCommandExtension() {
    }

    public static void install() {
        KineticCommands.registerExtension(KineticArmory.MODID, new ArmorCommandExtension());
    }

    @Override
    public void reload(CommandSourceStack source) {
        ArmorCommand.executeReload(source);
        if (!ArmorLoader.FAILED_SETS.isEmpty()) {
            String failedList = String.join(", ", ArmorLoader.FAILED_SETS);
            source.sendSuccess(() -> KineticI18n.translatable(
                    "msg.kineticarmory.armorsets.load_error",
                    failedList
            ), true);
        }
    }
}
