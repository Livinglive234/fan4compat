package dev.fan4.compat.sodium;

import dev.fan4.compat.shared.FailureLatch;
import dev.fan4.compat.shared.GameLog;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import static dev.fan4.compat.shared.CompatCalls.type;

/**
 * Immersive Portals narrows Sodium's culling while a portal view is rendering: a box inside Sodium's frustum is still skipped when IP's own culler can prove the
 * portal hides it. IP did this by redirecting the frustum test in Sodium 0.6's Viewport.isBoxVisible; Sodium 0.8 split that into four Viewport methods that each
 * call a different Frustum method, so the generated hooks redirect those four calls and pass the result and the camera-relative box through here. Outside a portal view
 * IP's culler is null and every answer is returned unchanged. One failure turns this off for the session and leaves Sodium's own answers.
 */
public final class PortalCulling {
    /** Plane index JOML reports for a box that is outside the frustum; any non-negative value means outside. */
    static final int OUTSIDE=0;

    /** True when the box is provably hidden, in camera-relative coordinates. */
    public interface BoxCuller {boolean invisible(float minX,float minY,float minZ,float maxX,float maxY,float maxZ);}

    private PortalCulling() {}

    public static boolean section(boolean visible,BoxCuller culler,float x,float y,float z,float radius){
        return visible&&(culler==null||!culler.invisible(x-radius,y-radius,z-radius,x+radius,y+radius,z+radius));
    }
    public static boolean box(boolean visible,BoxCuller culler,float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
        return visible&&(culler==null||!culler.invisible(minX,minY,minZ,maxX,maxY,maxZ));
    }
    /** JOML's intersectAab: -1 inside, -2 intersecting, a plane index when outside. Only a box that is not already outside can be culled here. */
    public static int intersect(int result,BoxCuller culler,float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
        return result<0&&culler!=null&&culler.invisible(minX,minY,minZ,maxX,maxY,maxZ)?OUTSIDE:result;
    }

    // Entry points for the generated hooks.
    public static boolean section(boolean visible,float x,float y,float z,float radius){
        if(!visible)return false;
        try{return section(true,current(),x,y,z,radius);}catch(Throwable failure){return fail(failure,true);}
    }
    public static boolean box(boolean visible,float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
        if(!visible)return false;
        try{return box(true,current(),minX,minY,minZ,maxX,maxY,maxZ);}catch(Throwable failure){return fail(failure,true);}
    }
    public static int intersect(int result,float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
        if(result>=0)return result;
        try{return intersect(result,current(),minX,minY,minZ,maxX,maxY,maxZ);}catch(Throwable failure){fail(failure,true);return result;}
    }

    private static final String SODIUM_INTERFACE="qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface",FRUSTUM_CULLER="qouteall.imm_ptl.core.render.FrustumCuller";
    private static final FailureLatch LATCH=new FailureLatch("Sodium portal culling");
    private static IpCuller last;

    /** The culler IP has set for the portal view being rendered, or null outside one. */
    private static BoxCuller current()throws Throwable {
        if(LATCH.tripped()||!Ip.OK)return null;
        Object culler=(Object)Ip.CULLER.invokeExact();
        if(culler==null)return null;
        IpCuller cached=last;
        if(cached==null||cached.culler!=culler)last=cached=new IpCuller(culler);
        return cached;
    }
    private static boolean fail(Throwable failure,boolean visible){LATCH.trip(failure);return visible;}

    private record IpCuller(Object culler) implements BoxCuller {
        public boolean invisible(float minX,float minY,float minZ,float maxX,float maxY,float maxZ){
            try{return (boolean)Ip.INVISIBLE.invokeExact(culler,minX,minY,minZ,maxX,maxY,maxZ);}
            catch(RuntimeException|Error unchecked){throw unchecked;}
            catch(Throwable checked){throw new IllegalStateException(checked);}
        }
    }
    /** Immersive Portals' static culler field and its box test, resolved once (static final, so the JIT can inline them). */
    private static final class Ip {
        static final MethodHandle CULLER,INVISIBLE;static final boolean OK;
        static {
            MethodHandle culler=null,invisible=null;boolean ok=false;
            try {
                MethodHandles.Lookup lookup=MethodHandles.publicLookup();
                Class<?> holder=type(SODIUM_INTERFACE),cullerType=type(FRUSTUM_CULLER);
                culler=lookup.findStaticGetter(holder,"frustumCuller",cullerType).asType(MethodType.methodType(Object.class));
                invisible=lookup.findVirtual(cullerType,"canDetermineInvisibleWithCameraCoord",MethodType.methodType(boolean.class,float.class,float.class,float.class,float.class,float.class,float.class))
                    .asType(MethodType.methodType(boolean.class,Object.class,float.class,float.class,float.class,float.class,float.class,float.class));
                ok=true;
            } catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable) {
                try{GameLog.warn("Sodium portal culling unavailable: "+unavailable.getClass().getSimpleName()+": "+unavailable.getMessage());}catch(RuntimeException|LinkageError ignored){}
            }
            CULLER=culler;INVISIBLE=invisible;OK=ok;
        }
    }
}
