package dev.fan4.compat.jade;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Jade compares/rerays hit vectors in world space; block addresses stay in ship space. */
public final class ShipRaycastCompat {
    private ShipRaycastCompat() {}
    public static Object worldPosition(Object position,Object entity) {
        return exact("org.valkyrienskies.mod.common.VSGameUtilsKt","toWorldCoordinates",
            new String[]{"net.minecraft.class_1937","net.minecraft.class_243"},call(entity,"method_37908"),position);
    }
}
