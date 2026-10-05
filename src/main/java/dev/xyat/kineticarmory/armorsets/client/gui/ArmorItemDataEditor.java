package dev.xyat.kineticarmory.armorsets.client.gui;

import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;
import java.util.function.Consumer;

/** Edits an armor item's data in Core's NBT editor, the same screen on every version. */
final class ArmorItemDataEditor {
    private ArmorItemDataEditor() {}

    static void open(KineticPage parent, String itemId, String initial, Consumer<String> onSave) {
        //? if >=1.21 {
        /*// Item component text ([damage=5]) is checked against the item instead of as NBT.
        KineticSelectors.openNbtEditor(initial, text -> {
            try {
                dev.xyat.kineticarmory.armorsets.data.ArmorItemData.compile(itemId, text);
                return null;
            } catch (RuntimeException invalid) {
                return invalid.getMessage() == null ? invalid.toString() : invalid.getMessage();
            }
        }, saved -> onSave.accept(saved.isBlank() ? dev.xyat.kineticarmory.armorsets.data.ArmorItemData.emptyData() : saved));
        *///?} else {
        KineticSelectors.openNbtEditor(initial, onSave);
        //?}
    }
}
