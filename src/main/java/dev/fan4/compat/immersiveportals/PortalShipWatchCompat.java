package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Keep VS tracking tied to IP's delivered chunk watches, including remote dimensions. */
public final class PortalShipWatchCompat {
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    public static boolean watched(Object wrapper,Object ship,long chunk) {
        // VS also watches through synthetic ShipObserverPlayer instances without getPlayer().
        if(!type("org.valkyrienskies.mod.common.util.MinecraftPlayer").isInstance(wrapper))return false;
        Object player=call(wrapper,"getPlayer");
        if(player==null||!type("net.minecraft.class_3222").isInstance(player))return false;
        Object dimension=call(type(UTILS),"getResourceKey",call(ship,"getChunkClaimDimension"));
        return (Boolean)exact("qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking","isPlayerWatchingChunk",
            new String[]{"net.minecraft.class_3222","net.minecraft.class_5321","int","int"},
            player,dimension,(int)(chunk>>32),(int)chunk);
    }
    private static final class Scope {
        final Object ship;final long chunk;final Scope previous;boolean watched;
        Scope(Object ship,long chunk,Scope previous){this.ship=ship;this.chunk=chunk;this.previous=previous;}
    }
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    public static void enter(Object ship,int x,int z) {
        CURRENT.set(new Scope(ship,((long)x<<32)|(z&0xffffffffL),CURRENT.get()));
    }
    public static void exit() {
        Scope scope=CURRENT.get();
        if(scope==null||scope.previous==null)CURRENT.remove();else CURRENT.set(scope.previous);
    }
    public static Object playerPosition(Object player,Object destination) {
        Scope scope=CURRENT.get();
        if(scope!=null)scope.watched=watched(player,scope.ship,scope.chunk);
        return call(player,"getPosition",destination);
    }
    public static boolean dimensionEligible(boolean original) {
        Scope scope=CURRENT.get();return original||(scope!=null&&scope.watched);
    }
    public static double watchDistance(double original) {
        Scope scope=CURRENT.get();return scope!=null&&scope.watched?-1d:original;
    }
}
