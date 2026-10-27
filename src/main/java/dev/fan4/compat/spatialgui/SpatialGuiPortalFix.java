package dev.fan4.compat.spatialgui;

import dev.fan4.compat.shared.Diagnostics;
import dev.fan4.compat.shared.GameLog;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import static dev.fan4.compat.shared.CompatCalls.*;

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

    private static final String RENDER_INFO="qouteall.imm_ptl.core.render.context_management.WorldRenderInfo";
    private static boolean cameraFailed;
    /**
     * Spatial GUI replaces the camera's position and angles at the end of Camera.setup whenever a screen is open, including for the camera of a portal view, and
     * Immersive Portals sets that camera's position from the portal at the same point, so which one wins depends on load order. Runs after both and has Immersive
     * Portals place the camera again (a no-op outside a portal view), so a portal view keeps its position from the portal and Spatial GUI's angles. One failure turns
     * this off for the session.
     */
    public static void keepPortalCamera(Object camera){cameraFailed=!keepPortalCamera(camera,cameraFailed,placed->call(type(RENDER_INFO),"adjustCameraPos",placed));}
    /** Returns false (and logs once) if placing the camera failed; already failed means do nothing. */
    public static boolean keepPortalCamera(Object camera,boolean failed,Consumer<Object> place) {
        if(failed)return false;
        try{place.accept(camera);return true;}
        catch(RuntimeException|LinkageError unplaceable){
            try{GameLog.warn("Spatial GUI portal camera fix disabled after a failure: "+unplaceable.getClass().getSimpleName()+": "+unplaceable.getMessage());}catch(RuntimeException|LinkageError ignored){}
            return false;
        }
    }
}
