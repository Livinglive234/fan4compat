package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;

/** VS chunk tasks must deliver packets to their owning world, including portal views. */
public final class ShipChunkPackets {
    private static final String REDIRECT="qouteall.imm_ptl.core.network.PacketRedirection";
    private record Scope(Object world,Object pos,Scope previous) {}
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    public static void watch(Object task,Object world,boolean shipyard,Object pos,Object operation) {
        Scope previous=CURRENT.get();CURRENT.set(new Scope(world,pos,previous));
        try {redirect(world,()->call(operation,"call",(Object)new Object[]{task,world,shipyard,pos}));}
        finally {if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    private static void redirect(Object world,Runnable body) {
        exact(REDIRECT,"withForceRedirect",new String[]{"net.minecraft.class_3218","java.lang.Runnable"},world,body);
    }
    private static boolean watched(Object player,Object world,Object pos) {
        return (Boolean)exact("qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking","isPlayerWatchingChunk",
            new String[]{"net.minecraft.class_3222","net.minecraft.class_5321","int","int"},player,call(world,"method_27983"),field(pos,"field_9181"),field(pos,"field_9180"));
    }
    /** The warning's same-world assumption does not cover legitimate IP watchers. */
    public static String watchDimension(Object wrapper) {
        String original=(String)call(wrapper,"getDimension");Scope scope=CURRENT.get();
        if(scope!=null) {
            Object player=call(wrapper,"getPlayer");
            if(player!=null&&watched(player,scope.world,scope.pos))return (String)exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getDimensionId",new String[]{"net.minecraft.class_1937"},scope.world);
        }
        return original;
    }
    public static void drop(Object manager,Object player,Object pos,Object operation) {
        Object world=field(manager,"field_17214");
        // IP still owns a delivered watch: a VS unwatch must not erase that cache.
        if(watched(player,world,pos))return;
        redirect(world,()->call(operation,"call",(Object)new Object[]{manager,player,pos}));
    }
}
