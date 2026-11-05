package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** A slot that accepts several equipment variants shows a small gold # in its upper-left corner, like a tag ingredient. */
final class ArmorVariantMarker {
    private ArmorVariantMarker() { }

    static void draw(KineticGraphics graphics, int x, int y) {
        graphics.push();
        try {
            graphics.raise(1);
            graphics.translate(x + 2, y + 2);
            graphics.scale(0.75F, 0.75F);
            graphics.text(Component.literal("#").withStyle(ChatFormatting.GOLD), 0, 0, 0xFFFFFFFF, true);
        } finally {
            graphics.pop();
        }
    }
}
