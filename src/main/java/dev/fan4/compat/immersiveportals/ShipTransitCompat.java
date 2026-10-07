package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;
import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.shared.ShipPose.Point;
import java.util.*;

/** Bridge the VS player lifecycle skipped by Immersive Portals' dimension transfer. */
public final class ShipTransitCompat {
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    private static final Map<Object,Set<Long>> ACKNOWLEDGED=new WeakHashMap<>();
    private static final Map<Object,java.lang.ref.WeakReference<Object>> ACK_WORLDS=new WeakHashMap<>();
    public static void acknowledgeLoadedShips(Object client) {
        Object world=field(client,"field_1687"),player=field(client,"field_1724");
        if(world==null||player==null)return;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_638"},world);
        if(!(Boolean)call(ships,"isSyncedWithServer"))return;
        // Native player construction can prepopulate known IDs without sending an ACK.
        // Track actual sends separately, and resend after an IP world transition.
        var previous=ACK_WORLDS.get(player);
        if(previous==null||previous.get()!=world){ACKNOWLEDGED.remove(player);ACK_WORLDS.put(player,new java.lang.ref.WeakReference<>(world));}
        Set<Long> sent=ACKNOWLEDGED.computeIfAbsent(player,k->new HashSet<>()),loaded=new HashSet<>();
        for(Object ship:(Iterable<?>)call(ships,"getLoadedShips")) {
            long id=((Number)call(ship,"getId")).longValue();
            loaded.add(id);
            if(!sent.contains(id)||!(Boolean)call(player,"vs_isKnownShip",id)){call(player,"vs_addKnownShip",id);sent.add(id);}
        }
        sent.retainAll(loaded);
    }
    /** Old C2S ship packets can arrive after the IP teleport packet. */
    public static boolean wrongDimensionMotion(Object motion,Object wrapper) {
        Object player=call(wrapper,"getPlayer");
        if(player==null)return false;
        // Vanilla movement waits for the teleport ACK; VS custom motion must too.
        if(field(field(player,"field_13987"),"field_14119")!=null)return true;
        long id=((Number)call(motion,"getShipID")).longValue();
        if(id==-1)return false;
        Object world=call(player,"method_37908");
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_3218"},world);
        Object ship=call(call(ships,"getAllShips"),"getById",id);
        if(ship==null)return true;
        Object dimension=exact(UTILS,"getDimensionId",new String[]{"net.minecraft.class_1937"},world);
        return !dimension.equals(call(ship,"getChunkClaimDimension"));
    }
    private static ShipPose localPose(Object ship) {
        Object matrix=call(ship,"getWorldToShip");double[] values=new double[16];
        for(int col=0;col<4;col++)for(int row=0;row<4;row++)values[col*4+row]=((Number)call(matrix,"m"+col+row)).doubleValue();
        return new ShipPose(values);
    }
    /** Refresh only the arrived ship's nearby IP watches once per dimension transfer. */
    public static void finishTransfer(Object player) {
        resetShipMotion(player);
        Object world=call(player,"method_37908"),position=call(player,"method_19538");
        Point point=new Point(((Number)field(position,"field_1352")).doubleValue(),((Number)field(position,"field_1351")).doubleValue(),((Number)field(position,"field_1350")).doubleValue());
        Object bounds=createExact("net.minecraft.class_238",new String[]{"double","double","double","double","double","double"},point.x()-2,point.y()-2,point.z()-2,point.x()+2,point.y()+2,point.z()+2);
        List<Object> ships=new ArrayList<>();
        for(Object ship:(Iterable<?>)exact(UTILS,"getShipsIntersecting",new String[]{"net.minecraft.class_1937","net.minecraft.class_238"},world,bounds))ships.add(ship);
        if(ships.isEmpty())return;
        Object tracking=type("qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking");
        call(tracking,"immediatelyUpdateForPlayer",player);
        Object loading=call(tracking,"getPlayerInfo",player),dimension=call(world,"method_27983");int refreshed=0;
        for(Object ship:ships) {
            Point local=localPose(ship).position(point);
            int cx=(int)Math.floor(local.x()/16),cz=(int)Math.floor(local.z()/16);
            Object active=call(ship,"getActiveChunksSet");
            for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++) {
                if(!(Boolean)call(active,"contains",x,z))continue;
                Map<?,?> records=(Map<?,?>)call(tracking,"getWatchRecordForChunk",dimension,x,z);
                Object record=records==null?null:records.get(player);
                if(record==null||!(Boolean)field(record,"isValid")||!(Boolean)field(record,"isLoadedToPlayer"))continue;
                // Requeue through IP's normal batch, light and entity delivery path.
                try {
                    record.getClass().getField("isLoadedToPlayer").setBoolean(record,false);
                }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot refresh IP ship chunk watch",e);}
                call(loading,"markPendingLoading",record);refreshed++;
            }
        }
    }
    public static void resetShipMotion(Object player) {
        Object drag=call(player,"getDraggingInformation");
        call(drag,"setLastShipStoodOn",(Object)null);
        call(drag,"setLastShipStoodOnServerWriteOnly",(Object)null);
        Object zero=create("org.joml.Vector3d");
        call(drag,"setAddedMovementLastTick",zero);call(drag,"setAddedYawRotLastTick",0d);
        for(String name:new String[]{"LerpPositionOnShip","RelativeVelocityOnShip","RelativePositionOnShip","PreviousRelativeVelocityOnShip","CachedLastPosition","ServerRelativePlayerPosition","LerpYawOnShip","LerpHeadYawOnShip","LerpPitchOnShip","RelativeYawOnShip","RelativeHeadYawOnShip","RelativePitchOnShip","DraggedArmorStandRelYaw","ServerRelativePlayerYaw"})call(drag,"set"+name,(Object)null);
        call(drag,"setRestoreCachedLastPosition",false);call(drag,"setShouldImpulseMovement",false);
        call(drag,"setLerpSteps",0);call(drag,"setHeadLerpSteps",0);
        if(type("org.valkyrienskies.mod.mixinducks.world.entity.PlayerDuck").isInstance(player)) {
            call(player,"vs_setQueuedPositionUpdate",(Object)null);
            call(player,"vs_setHandledMovePacket",false);
        }
    }
}
