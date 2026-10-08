package dev.fan4.compat.pointblank;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Never start a draw transition with a different item or unresolved fire mode. */
public final class PointBlankDrawCompat {
    private PointBlankDrawCompat() {}
    public static Object drawStack(Object state,Object player,Object original) {
        if(player==null || !type("net.minecraft.class_1657").isInstance(player))return original;
        Class<?> gun=type("com.vicmatskiv.pointblank.item.GunItem");
        Object context=call(gun,"resolveOperableGunContext",player);
        if(context==null)return original;
        Object candidate=call(context,"itemStack");
        if(candidate==null || call(candidate,"method_7909")!=call(state,"getGunItem"))return original;
        Object id=call(gun,"getItemStackId",candidate);
        return id!=null && id.equals(call(state,"getId")) ? candidate : original;
    }
    public static boolean validDraw(Object state,Object stack) {
        if(stack==null)return false;
        Object item=call(stack,"method_7909");
        Class<?> gun=type("com.vicmatskiv.pointblank.item.GunItem");
        return gun.isInstance(item) && item==call(state,"getGunItem")
            && call(gun,"getFireModeInstance",stack)!=null;
    }
}
