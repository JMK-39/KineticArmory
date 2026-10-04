//? if >=1.21 {
/*package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticarmory.armorsets.data.ArmorItemData;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.text.KineticI18n;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class ArmorComponentsEditorPage extends KineticPage {
    // Share the editor's content column with its title, hint, and validation message.
    private static final int EDITOR_MARGIN = 15;
    private static final int EDITOR_MAX_WIDTH = 700;
    private final String itemId;
    private final Consumer<String> onSave;
    private String data;
    private KineticButton save;
    private boolean valid;

    public ArmorComponentsEditorPage(String itemId, String initial, Consumer<String> onSave) {
        super(KineticI18n.translatable("gui.kineticarmory.armorsets.components.title"));
        this.itemId = itemId;
        this.data = initial == null || initial.isBlank() ? ArmorItemData.emptyData() : initial;
        this.onSave = onSave;
    }

    @Override
    protected void build(KineticUi ui) {
        int editorWidth = Math.min(width() - EDITOR_MARGIN * 2, EDITOR_MAX_WIDTH);
        int x = (width() - editorWidth) / 2;
        var input = ui.textArea(x, 45, editorWidth, Math.max(30, height() - 110))
                .label(title()).maxLength(32767).value(data).onChange(text -> {
                    data = text;
                    validate();
                }).build();
        save = ui.button(width() / 2 - 85, height() - 30, 80)
                .text(KineticI18n.translatable("gui.kineticarmory.armorsets.save"))
                .onClick(button -> {
                    validate();
                    if (!valid) return;
                    onSave.accept(data.isBlank() ? ArmorItemData.emptyData() : data.trim());
                    navigateBack();
                }).build();
        ui.button(width() / 2 + 5, height() - 30, 80)
                .text(KineticI18n.translatable("gui.kineticarmory.armorsets.back"))
                .onClick(button -> navigateBack()).build();
        validate();
        focus(input);
    }

    private void validate() {
        try {
            ArmorItemData.compile(itemId, data);
            valid = true;
            if (save != null) save.setTooltip((Component) null);
        } catch (RuntimeException invalid) {
            valid = false;
            if (save != null) save.setTooltip(Component.literal(invalid.getMessage() == null ? "" : invalid.getMessage()));
        }
        if (save != null) save.setEnabled(valid);
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int textWidth = Math.min(width() - EDITOR_MARGIN * 2, EDITOR_MAX_WIDTH);
        graphics.scrollingTextCentered(title(), width() / 2, 8, textWidth, 0xFFFFFF, true);
        graphics.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.components.hint"), width() / 2, 27, textWidth, 0xAAAAAA, false);
        if (!valid) graphics.scrollingTextCentered(KineticI18n.translatable("gui.kineticarmory.armorsets.components.invalid"),
                width() / 2, height() - 53, textWidth, 0xFF5555, false);
    }
}
*///?}
