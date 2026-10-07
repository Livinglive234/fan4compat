package dev.fan4.compat.valkyrienskies;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Portal frames on ships use shipyard block addresses, even when unloaded. */
public final class ShipPortalCreation {
    private ShipPortalCreation() {}
    public static boolean blocked(Object world, Object position) {
        if (world == null || position == null || !type("net.minecraft.class_1937").isInstance(world)) return false;
        return (Boolean) exact("org.valkyrienskies.mod.common.VSGameUtilsKt", "isBlockInShipyard",
            new String[]{"net.minecraft.class_1937", "net.minecraft.class_2338"}, world, position);
    }
}
