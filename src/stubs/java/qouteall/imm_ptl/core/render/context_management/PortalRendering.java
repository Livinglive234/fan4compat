package qouteall.imm_ptl.core.render.context_management;

import qouteall.imm_ptl.core.portal.Portal;

/** Stand-in for Immersive Portals' PortalRendering with settable state for tests; never packaged. */
public class PortalRendering {
    public static boolean rendering,caveCulling;
    public static Portal portal;
    public static boolean isRendering(){return rendering;}
    public static boolean shouldEnableSodiumCaveCulling(){return caveCulling;}
    public static Portal getRenderingPortal(){return portal;}
}
