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
        for(var entry:PortalMotionCompat.serverEntries()) {
            var anchor=entry.getValue();Object portal=entry.getKey();
            if(!anchor.destination()||call(portal,"getOriginWorld")!=world||(Boolean)call(portal,"method_31481")||!(Boolean)call(portal,"isPortalValid")||!(Boolean)call(portal,"canTeleportEntity",player))continue;
            if(((Number)call(portal,"getDistanceToNearestPointInPortal",position)).doubleValue()>64||!visited.add(anchor.ship()))continue;
            Object destination=call(portal,"getDestWorld");
            if(destination==null)continue;
            Object ships=exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getShipObjectWorld",new String[]{"net.minecraft.class_3218"},destination);
            Object ship=call(call(ships,"getAllShips"),"getById",anchor.ship());
            if(ship==null||!call(ship,"getChunkClaimDimension").equals(exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getDimensionId",new String[]{"net.minecraft.class_1937"},destination)))continue;
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
}
