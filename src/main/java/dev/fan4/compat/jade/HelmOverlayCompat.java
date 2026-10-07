package dev.fan4.compat.jade;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Only suppress the overlay during an active helm ride; never change Jade settings. */
public final class HelmOverlayCompat {
    public static boolean piloting(){
        Object client=call(type("net.minecraft.class_310"),"method_1551"),player=field(client,"field_1724");
        if(player==null)return false;
        Object mount=call(player,"method_5854");
        return mount!=null&&type("org.valkyrienskies.mod.common.entity.ShipMountingEntity").isInstance(mount)
            &&!(Boolean)call(mount,"vs$isPassengerSeat");
    }
}
