package dev.fan4.compat.immersiveportals;
import java.util.*;
import java.util.function.Consumer;
import java.lang.reflect.Proxy;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Bootstrap ship metadata from stable doorway anchors, independently of world bounds. */
public final class DoorwayShipLoading {
    public static void loaders(Object player,Consumer<Object> consumer) {
        Object world=call(player,"method_37908"),position=call(player,"method_19538");
        Set<Long> visited=new HashSet<>();
        // Keep the native unloaded-ship margin loaded around the actual player,
        // including empty active shipyard chunks outside the visible hull.
        double x=((Number)field(position,"field_1352")).doubleValue(),y=((Number)field(position,"field_1351")).doubleValue(),z=((Number)field(position,"field_1350")).doubleValue();
        Object bounds=createExact("net.minecraft.class_238",new String[]{"double","double","double","double","double","double"},x-48,y-48,z-48,x+48,y+48,z+48);
        Object currentDimension=exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getDimensionId",new String[]{"net.minecraft.class_1937"},world);
        for(Object ship:(Iterable<?>)exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getShipsIntersecting",new String[]{"net.minecraft.class_1937","net.minecraft.class_238"},world,bounds)) {
            long id=((Number)call(ship,"getId")).longValue();
            if(currentDimension.equals(call(ship,"getChunkClaimDimension"))&&visited.add(id))collect(world,ship,consumer);
        }
        for(var entry:PortalMotionCompat.serverEntries()) {
            var anchor=entry.getValue();Object portal=entry.getKey();
            if(!anchor.destination()||call(portal,"getOriginWorld")!=world||(Boolean)call(portal,"method_31481")||!(Boolean)call(portal,"isPortalValid")||!(Boolean)call(portal,"canTeleportEntity",player))continue;
            if(((Number)call(portal,"getDistanceToNearestPointInPortal",position)).doubleValue()>64||!visited.add(anchor.ship()))continue;
            Object destination=call(portal,"getDestWorld");
            if(destination==null)continue;
            Object ships=exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getShipObjectWorld",new String[]{"net.minecraft.class_3218"},destination);
            Object ship=call(call(ships,"getAllShips"),"getById",anchor.ship());
            if(ship==null||!call(ship,"getChunkClaimDimension").equals(exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getDimensionId",new String[]{"net.minecraft.class_1937"},destination)))continue;
            collect(destination,ship,consumer);
        }
    }
    private static void collect(Object destination,Object ship,Consumer<Object> consumer) {
            Object dimension=call(destination,"method_27983");
            Class<?> callback=type("org.valkyrienskies.core.api.util.functions.IntBinaryConsumer");
            Object collect=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(proxy,method,args)->{
                if(method.getName().equals("accept"))consumer.accept(createExact("qouteall.imm_ptl.core.chunk_loading.ChunkLoader",new String[]{"net.minecraft.class_5321","int","int","int"},dimension,args[0],args[1],0));
                else if(method.getName().equals("toString"))return "Fan4Compat ship chunks";
                else if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                else if(method.getName().equals("equals"))return proxy==args[0];
                return null;
            });
            call(call(ship,"getActiveChunksSet"),"forEach",collect);
    }
}
