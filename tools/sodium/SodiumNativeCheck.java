/**
 * Every Sodium 0.8 and Immersive Portals member the portal culling touches must exist with the exact descriptors (args: the Sodium jar and the Immersive Portals jar).
 */
public final class SodiumNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        String viewport="net/caffeinemc/mods/sodium/client/render/viewport/Viewport",frustum=viewport.replace("Viewport","frustum/Frustum");
        // Injection targets: the four box tests and the Frustum calls they redirect.
        XaeroSyncNativeCheck.method(viewport,"isBoxVisible","(III)Z",false);
        XaeroSyncNativeCheck.method(viewport,"isBoxVisibleLooser","(III)Z",false);
        XaeroSyncNativeCheck.method(viewport,"isBoxVisibleDirect","(FFFF)Z",false);
        XaeroSyncNativeCheck.method(viewport,"getBoxIntersectionDirect","(FFFF)I",false);
        XaeroSyncNativeCheck.method(frustum,"testSection","(FFF)Z",false);
        XaeroSyncNativeCheck.method(frustum,"testSectionExpanded","(FFFF)Z",false);
        XaeroSyncNativeCheck.method(frustum,"testAab","(FFFFFF)Z",false);
        XaeroSyncNativeCheck.method(frustum,"intersectAab","(FFFFFF)I",false);
        // Immersive Portals: the culler the helper reads, the box test it calls, and the mixin that is cancelled.
        String ip="qouteall/imm_ptl/core/";
        XaeroSyncNativeCheck.field(ip+"compat/sodium_compatibility/SodiumInterface","frustumCuller",true);
        XaeroSyncNativeCheck.method(ip+"render/FrustumCuller","canDetermineInvisibleWithCameraCoord","(FFFFFF)Z",false);
        XaeroSyncNativeCheck.find(ip+"compat/mixin/sodium/MixinSodiumViewport");
        System.out.println("PASS: every Sodium 0.8 and Immersive Portals member used by the portal culling exists in the real jars with the exact descriptors");
    }
}
