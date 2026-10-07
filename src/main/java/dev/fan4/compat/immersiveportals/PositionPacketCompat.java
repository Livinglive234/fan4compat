package dev.fan4.compat.immersiveportals;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Stamp missing IP metadata while the authoritative sender is still available. */
public final class PositionPacketCompat {
    public static void prepare(Object handler,Object packet) {
        if(!type("net.minecraft.class_3244").isInstance(handler)||!type("net.minecraft.class_2708").isInstance(packet))return;
        if(call(packet,"ip_getPlayerDimension")!=null)return;
        Object player=field(handler,"field_14140");
        Object dimension=call(call(player,"method_37908"),"method_27983");
        call(packet,"ip_setPlayerDimension",dimension);
    }
    /** Source-world correction retries must not survive a committed IP transfer. */
    public static void clearOldAwaiting(Object player) {
        Object handler=field(player,"field_13987"),current=call(call(player,"method_37908"),"method_27983");
        Object awaiting=field(handler,"ip_dimOfAwaitingPosition");
        if(awaiting!=null&&!awaiting.equals(current)) {
            writeField(handler,"field_14119",null);
            writeField(handler,"ip_dimOfAwaitingPosition",null);
        }
    }
}
