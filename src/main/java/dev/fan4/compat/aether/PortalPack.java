package dev.fan4.compat.aether;

import java.lang.ref.WeakReference;

/**
 * The Aether ships a data pack for Immersive Portals (the Aether portal becomes an Immersive Portals portal) and a method that registers it,
 * setupImmersivePortalsPack, but its packSetup never calls that method, so the portal does not form. The generated mixin calls it at the end of packSetup;
 * this keeps it to one registration per pack event, so an Aether jar that already calls it (a fixed build, or one patched by hand) does not add the pack twice.
 */
public final class PortalPack {
    private static WeakReference<Object> last=new WeakReference<>(null);

    private PortalPack() {}

    /** True the first time the event is seen; the registration runs only then. */
    public static synchronized boolean firstTime(Object event){
        if(last.get()==event)return false;
        last=new WeakReference<>(event);
        return true;
    }
}
