package dev.fan4.compat.spatialgui;

import dev.fan4.compat.shared.Diagnostics;
import java.util.function.IntSupplier;

/**
 * Spatial GUI saves the camera's projection and view matrices after every world render so it can place its screen in the world, and it overwrites that saved
 * state each time. Immersive Portals renders each portal view as a further world render after the main one, so by the time the screen is drawn the saved state
 * belongs to the portal's camera. The generated hook skips the save while a portal view is being rendered, so only the main view sets it.
 */
public final class SpatialGuiPortalFix {
    private SpatialGuiPortalFix() {}
    /** True while Immersive Portals is rendering a portal view (portal layer above zero); false if the layer cannot be read. */
    public static boolean skipCapture(IntSupplier portalLayer) {
        try{return portalLayer.getAsInt()>0;}
        catch(RuntimeException|LinkageError unreadable){return false;}
    }
    /** Called from the generated hook. */
    public static boolean skipCapture(){return skipCapture(Diagnostics::portalLayer);}
}
