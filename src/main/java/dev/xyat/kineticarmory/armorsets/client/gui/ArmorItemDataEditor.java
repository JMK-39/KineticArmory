package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import java.util.function.Consumer;

final class ArmorItemDataEditor {
    private ArmorItemDataEditor() {}

    static void open(KineticPage parent, String itemId, String initial, Consumer<String> onSave) {
        //? if >=1.21 {
        /*parent.openChild(new ArmorComponentsEditorPage(itemId, initial, onSave));
        *///?} else {
        KineticSelectors.openNbtEditor(initial, onSave);
        //?}
    }
}