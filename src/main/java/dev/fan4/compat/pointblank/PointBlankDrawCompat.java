package dev.fan4.compat.pointblank;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Never start a draw transition with a different item or unresolved fire mode. */
public final class PointBlankDrawCompat {
    private PointBlankDrawCompat() {}
    public static boolean validDraw(Object state,Object stack) {
        if(stack==null)return false;
        Object item=call(stack,"method_7909");
        Class<?> gun=type("com.vicmatskiv.pointblank.item.GunItem");
        return gun.isInstance(item) && item==call(state,"getGunItem")
            && call(gun,"getFireModeInstance",stack)!=null;
    }
}
