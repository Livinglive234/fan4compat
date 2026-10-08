package dev.fan4.compat.freecam;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Let only the active client camera tick beyond full terrain chunks. */
public final class FreecamTickCompat {
    private static final String PORTAL="qouteall.imm_ptl.core.render.context_management.PortalRendering";
    private static final boolean HAS_PORTALS=FreecamTickCompat.class.getClassLoader().getResource(PORTAL.replace('.','/')+".class")!=null;
    private FreecamTickCompat() {}
    private static boolean active(Object entity) {
        if(entity==null||!entity.getClass().getName().equals("net.xolt.freecam.util.FreeCamera"))return false;
        Class<?> freecam=type("net.xolt.freecam.Freecam");
        return (Boolean)call(freecam,"isEnabled")&&call(freecam,"getFreeCamera")==entity;
    }
    public static boolean allowTick(boolean loaded,Object entity) {
        if(loaded)return true;
        return active(entity);
    }
    /** No full terrain exists here to cover DH's normal near-camera exclusion. */
    private static boolean unloadedCamera() {
        Class<?> freecam=type("net.xolt.freecam.Freecam");
        if(!(Boolean)call(freecam,"isEnabled"))return false;
        Object camera=call(freecam,"getFreeCamera");if(!active(camera))return false;
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        if(call(client,"method_1560")!=camera)return false;
        if(HAS_PORTALS&&(Boolean)call(type(PORTAL),"isRendering"))return false;
        Object world=call(camera,"method_37908");
        if(field(client,"field_1687")!=world)return false;
        return !(Boolean)call(world,"method_33598",call(camera,"method_31477"),call(camera,"method_31479"));
    }
    public static float nearClip(float original) {
        return unloadedCamera()?Math.min(original,.5f):original;
    }
    public static void lodClip(Object shader) {
        if(unloadedCamera())call(shader,"setUniform",((Number)field(shader,"uClipDistance")).intValue(),0f);
    }
}
