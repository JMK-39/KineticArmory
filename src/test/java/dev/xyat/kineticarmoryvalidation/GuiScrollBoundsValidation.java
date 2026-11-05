package dev.xyat.kineticarmoryvalidation;

import dev.xyat.kineticarmory.armorsets.client.gui.ArmorEquipmentVariantPage;
import dev.xyat.kineticarmory.armorsets.client.gui.ArmorPieceBonusPage;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;

import java.lang.reflect.Proxy;
import java.util.List;

/** Executes the real private geometry methods without opening Minecraft or allocating a graphics context. */
public final class GuiScrollBoundsValidation {
    public static void main(String[] args) throws Exception {
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        var variants = (ArmorEquipmentVariantPage) unsafe.allocateInstance(ArmorEquipmentVariantPage.class);
        var scroll = new KineticScrollController();
        scroll.update(20, 8);
        set(variants, "listScroll", scroll);
        set(variants, "listX", 4); set(variants, "listY", 28); set(variants, "listH", 328);
        set(variants, "rowW", 616); set(variants, "visibleRows", 8);
        fractionalOffset(scroll, 0.5);
        check((int) invoke(variants, "getRowIndexAt", new Class<?>[]{double.class, double.class}, 12D, 340D) == 8,
                "the visible partial ninth row must accept a click during smooth scrolling");
        check((int) invoke(variants, "getRowIndexAt", new Class<?>[]{double.class, double.class}, 12D, 350D) == -1,
                "the clipped bottom edge must not accept a row click");
        boolean[] visible = {true};
        KineticButton button = (KineticButton) Proxy.newProxyInstance(KineticButton.class.getClassLoader(),
                new Class<?>[]{KineticButton.class}, (proxy, method, values) -> switch (method.getName()) {
                    case "controlVisible" -> visible[0];
                    case "setControlVisible" -> { visible[0] = (boolean) values[0]; yield null; }
                    default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
                });
        for (String field : List.of("modeButtons", "nbtButtons", "replaceButtons", "deleteButtons")) set(variants, field, List.of(button));
        fractionalOffset(scroll, 0.275);
        invoke(variants, "updateRowButtonPositions", new Class<?>[0]);
        check(!visible[0], "a row button crossing the same content clip must be hidden");
        var bonus = (ArmorPieceBonusPage) unsafe.allocateInstance(ArmorPieceBonusPage.class);
        scroll.update(20, 11); scroll.setOffset(Integer.MAX_VALUE);
        int offset = (int) invoke(bonus, "pixelOffset", new Class<?>[]{KineticScrollController.class, int.class, int.class}, scroll, 20, 280);
        check(20 * 25 - offset == 280, "piece bonus rows must meet the final viewport pixel");
        System.out.println("GUI_SCROLL_BOUNDS_PASS: variant partial rows and buttons share their clip; piece bonus ends at the viewport bottom");
    }

    private static void fractionalOffset(KineticScrollController scroll, double value) throws Exception {
        var delegateField = scroll.getClass().getDeclaredField("delegate"); delegateField.setAccessible(true);
        Object delegate = delegateField.get(scroll);
        set(delegate, "currentOffset", value); set(delegate, "targetOffset", value);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... values) throws Exception {
        var method = target.getClass().getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(target, values);
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
}
