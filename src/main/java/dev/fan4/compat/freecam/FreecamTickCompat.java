package dev.fan4.compat.freecam;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Let only the active client camera tick beyond full terrain chunks. */
public final class FreecamTickCompat {
    private FreecamTickCompat() {}
    public static boolean allowTick(boolean loaded,Object entity) {
        if(loaded)return true;
        if(entity==null||!entity.getClass().getName().equals("net.xolt.freecam.util.FreeCamera"))return false;
        Class<?> freecam=type("net.xolt.freecam.Freecam");
        return (Boolean)call(freecam,"isEnabled")&&call(freecam,"getFreeCamera")==entity;
    }
}
