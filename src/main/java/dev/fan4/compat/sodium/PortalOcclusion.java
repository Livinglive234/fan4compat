package dev.fan4.compat.sodium;

import dev.fan4.compat.shared.FailureLatch;
import net.minecraft.class_243;
import net.minecraft.class_4076;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * While a portal view is rendered, Immersive Portals makes Sodium's occlusion search start from the section the portal shows instead of the camera's own, turns cave
 * culling off for that search, and tolerates the first frustum test of the start section failing. IP did this with one mixin on Sodium 0.6's OcclusionCuller; that
 * mixin cannot apply to Sodium 0.8 (findVisible takes a RenderSectionVisitor now), so it is cancelled and the generated mixin on the OcclusionCuller hands the same
 * four steps to this class. One failure turns portal-aware occlusion off for the session (Sodium searches from the camera) and logs a warning.
 */
public final class PortalOcclusion {
    /** The OcclusionCuller's private members this needs, implemented by the generated mixin. */
    public interface Host {
        Object fan4$section(int x,int y,int z);
        boolean fan4$withinFrustum(Object viewport,Object section);
        void fan4$startPoint(Object sectionPos);
    }
    private static final FailureLatch LATCH=new FailureLatch("Sodium portal occlusion");
    private static boolean tolerateFailedFrustumTest;

    private PortalOcclusion() {}

    /** The value for findVisible's useOcclusionCulling argument; also sets the search's start section (null outside a portal view) on the host. */
    public static boolean begin(Host host,Object viewport,boolean original) {
        if(LATCH.tripped())return original;
        try {
            boolean useOcclusionCulling=PortalRendering.shouldEnableSodiumCaveCulling();
            host.fan4$startPoint(null);
            tolerateFailedFrustumTest=false;
            if(PortalRendering.isRendering()) {
                Portal portal=PortalRendering.getRenderingPortal();
                class_243 cameraPos=CHelper.getCurrentCameraPos();
                class_4076 start=portal.getPortalShape().getModifiedVisibleSectionIterationOrigin(portal,cameraPos);
                host.fan4$startPoint(start);
                if(start!=null) {
                    useOcclusionCulling=false;
                    Object section=host.fan4$section(start.method_18674(),start.method_18683(),start.method_18687());
                    if(section!=null&&!host.fan4$withinFrustum(viewport,section))tolerateFailedFrustumTest=true;
                }
            }
            return useOcclusionCulling;
        } catch(Throwable failure) {
            LATCH.trip(failure);
            try{host.fan4$startPoint(null);}catch(Throwable ignored){}
            tolerateFailedFrustumTest=false;
            return original;
        }
    }
    /** The answer of Sodium's isWithinFrustum: while the start section's test is being tolerated every answer is yes, until Sodium itself says yes once. */
    public static boolean tolerate(boolean withinFrustum) {
        if(!tolerateFailedFrustumTest)return withinFrustum;
        if(withinFrustum)tolerateFailedFrustumTest=false;
        return true;
    }
    /** For tests: clears the failure latch and the tolerance flag. */
    public static void reset(){LATCH.reset();tolerateFailedFrustumTest=false;}
}
