package qouteall.imm_ptl.core.portal.shape;

import net.minecraft.class_243;
import net.minecraft.class_4076;
import qouteall.imm_ptl.core.portal.Portal;

/** Stand-in for Immersive Portals' PortalShape (a default method there); never packaged. */
public interface PortalShape {
    class_4076 getModifiedVisibleSectionIterationOrigin(Portal portal,class_243 cameraPos);
}
