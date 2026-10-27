package qouteall.imm_ptl.core.render.context_management;
public final class PortalRendering {
    public static boolean rendering;
    public static int maxLayer=1;
    public static int getMaxPortalLayer(){return maxLayer;}
    public static boolean isRendering(){return rendering;}
    /** For the Sodium portal occlusion test. */
    public static boolean caveCulling;
    public static qouteall.imm_ptl.core.portal.Portal portal;
    public static boolean shouldEnableSodiumCaveCulling(){return caveCulling;}
    public static qouteall.imm_ptl.core.portal.Portal getRenderingPortal(){return portal;}
}
