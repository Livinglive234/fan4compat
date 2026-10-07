package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Scope VS's source-world crosshair override around IP's remote placement. */
public final class PortalInteractionCompat {
    private record Saved(Object client,Object hit) {}
    public static Object begin(boolean placing) {
        if(!placing)return null;
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        Object saved=call(client,"vs$getOriginalCrosshairTarget");
        call(client,"vs$setOriginalCrosshairTarget",(Object)null);
        return new Saved(client,saved);
    }
    public static void end(Object token) {
        if(token instanceof Saved saved)call(saved.client,"vs$setOriginalCrosshairTarget",saved.hit);
    }
}
