package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;
import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.shared.ShipPose.Point;
import java.util.*;

/** Recover authenticated native acknowledgements and nearby IP chunk delivery. */
public final class ShipLoadRecovery {
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    private static final Map<Object,long[]> CLIENT=new WeakHashMap<>();
    private static final Map<Object,Map<Long,Long>> PENDING=new WeakHashMap<>();
    private static final Map<Object,Map<Long,Long>> LAST_REPAIR=new WeakHashMap<>();
    public static void guard(Object entity,boolean blocked) {
        Object world=call(entity,"method_37908");
        if(!(Boolean)field(world,"field_9236"))return;
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        if(field(client,"field_1724")!=entity)return;
        long[] state=CLIENT.computeIfAbsent(entity,k->new long[3]);
        if(blocked)state[0]=1;else {state[0]=0;state[2]=0;}
    }
    public static void tick(Object client) {
        Object player=field(client,"field_1724"),world=field(client,"field_1687");if(player==null||world==null)return;
        long[] state=CLIENT.get(player);long now=System.nanoTime();
        if(state==null||state[0]==0||state[2]>=8||(state[2]>0&&now<state[1]))return;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_638"},world);
        if(!(Boolean)call(ships,"isSyncedWithServer"))return;
        state[0]=0;state[1]=now+2_000_000_000L;state[2]++;
        Object dimension=exact(UTILS,"getDimensionId",new String[]{"net.minecraft.class_1937"},world);int sent=0;
        for(Object ship:(Iterable<?>)call(ships,"getLoadedShips")) {
            if(!dimension.equals(call(ship,"getChunkClaimDimension"))||!near(player,ship))continue;
            // A native authenticated C2S ACK also requests recovery on the server.
            call(player,"vs_addKnownShip",((Number)call(ship,"getId")).longValue());if(++sent>=8)break;
        }
    }
    public static void acknowledged(Object packet,Object wrapper) {
        Object player=call(wrapper,"getPlayer");if(player==null)return;
        long id=((Number)call(packet,"getShipID")).longValue();
        if(!(Boolean)call(packet,"getAdd")){Map<Long,Long> pending=PENDING.get(player);if(pending!=null)pending.remove(id);return;}
        Object world=call(player,"method_37908"),ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_3218"},world);
        Object loaded=call(call(ships,"getLoadedShips"),"getById",id);
        if(loaded!=null){Map<Long,Long> pending=PENDING.get(player);if(pending!=null)pending.remove(id);repair(player,loaded);return;}
        // Native ACK handling discards an early acknowledgement. Retain only IDs
        // that already exist on the authoritative server, until they finish loading.
        if(call(call(ships,"getAllShips"),"getById",id)==null)return;
        Map<Long,Long> pending=PENDING.computeIfAbsent(player,k->new HashMap<>());
        if(pending.size()<64&&!pending.containsKey(id))pending.put(id,System.nanoTime()+10_000_000_000L);
    }
    public static void flush(Object server) {
        if(PENDING.isEmpty())return;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.server.MinecraftServer"},server);long now=System.nanoTime();
        Iterator<Map.Entry<Object,Map<Long,Long>>> players=PENDING.entrySet().iterator();
        while(players.hasNext()) {
            var entry=players.next();Object player=entry.getKey();
            if(call(call(player,"method_37908"),"method_8503")!=server||(Boolean)call(player,"method_31481")){players.remove();continue;}
            Iterator<Map.Entry<Long,Long>> ids=entry.getValue().entrySet().iterator();
            while(ids.hasNext()) {
                var request=ids.next();Object ship=call(call(ships,"getLoadedShips"),"getById",request.getKey());
                if(ship!=null){call(player,"vs_addKnownShip",request.getKey());ids.remove();repair(player,ship);}
                else if(now>request.getValue()||call(call(ships,"getAllShips"),"getById",request.getKey())==null)ids.remove();
            }
            if(entry.getValue().isEmpty())players.remove();
        }
    }
    private static void repair(Object player,Object ship) {
        Object world=call(player,"method_37908");
        if(!call(ship,"getChunkClaimDimension").equals(exact(UTILS,"getDimensionId",new String[]{"net.minecraft.class_1937"},world))||!near(player,ship))return;
        long id=((Number)call(ship,"getId")).longValue(),now=System.nanoTime();Map<Long,Long> last=LAST_REPAIR.computeIfAbsent(player,k->new HashMap<>());
        Long previous=last.get(id);if(previous!=null&&now-previous<2_000_000_000L)return;
        last.entrySet().removeIf(e->now-e.getValue()>30_000_000_000L);if(last.size()>=64)return;last.put(id,now);
        Object tracking=type("qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking");
        call(tracking,"immediatelyUpdateForPlayer",player);
        Object loading=call(tracking,"getPlayerInfo",player),dimension=call(world,"method_27983"),active=call(ship,"getActiveChunksSet");
        for(int[] cell:cells(player,ship)) {
            if(!(Boolean)call(active,"contains",cell[0],cell[1]))continue;
            Map<?,?> records=(Map<?,?>)call(tracking,"getWatchRecordForChunk",dimension,cell[0],cell[1]);Object record=records==null?null:records.get(player);
            // Pending watches are already queued. Invalid/other-player watches
            // cannot authorize loading. Preserve IP's ownership and batch protocol.
            if(record==null||!(Boolean)field(record,"isValid")||!(Boolean)field(record,"isLoadedToPlayer"))continue;
            writeField(record,"isLoadedToPlayer",false);call(loading,"markPendingLoading",record);
        }
    }
    private static boolean near(Object player,Object ship) {
        Object p=call(player,"method_19538"),b=call(ship,"getWorldAABB");if(b==null)return false;
        double[] coordinates={number(p,"field_1352"),number(p,"field_1351"),number(p,"field_1350")};String[] axes={"X","Y","Z"};double distance=0;
        for(int i=0;i<3;i++){double min=((Number)call(b,"min"+axes[i])).doubleValue(),max=((Number)call(b,"max"+axes[i])).doubleValue(),gap=Math.max(0,Math.max(min-coordinates[i],coordinates[i]-max));distance+=gap*gap;}
        return Double.isFinite(distance)&&distance<=64*64;
    }
    private static double number(Object v,String name){return ((Number)field(v,name)).doubleValue();}
    private static List<int[]> cells(Object player,Object ship) {
        Object matrix=call(ship,"getWorldToShip"),box=call(player,"method_5829");double[] values=new double[16];
        for(int c=0;c<4;c++)for(int r=0;r<4;r++)values[c*4+r]=((Number)call(matrix,"m"+c+r)).doubleValue();ShipPose pose=new ShipPose(values);
        double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
        for(int corner=0;corner<8;corner++) {
            Point point=pose.position(new Point(number(box,(corner&1)==0?"field_1323":"field_1320"),number(box,(corner&2)==0?"field_1322":"field_1325"),number(box,(corner&4)==0?"field_1321":"field_1324")));
            minX=Math.min(minX,point.x());maxX=Math.max(maxX,point.x());minZ=Math.min(minZ,point.z());maxZ=Math.max(maxZ,point.z());
        }
        return region(minX,minZ,maxX,maxZ);
    }
    public static List<int[]> region(double minX,double minZ,double maxX,double maxZ) {
        if(!Double.isFinite(minX+minZ+maxX+maxZ)||minX>maxX||minZ>maxZ)return List.of();
        int x0=(int)Math.floor((minX-1.0000001)/16),z0=(int)Math.floor((minZ-1.0000001)/16),x1=(int)Math.floor((maxX+1.0000001)/16),z1=(int)Math.floor((maxZ+1.0000001)/16);
        if((long)x1-x0>7||(long)z1-z0>7)return List.of();List<int[]> result=new ArrayList<>();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)result.add(new int[]{x,z});return result;
    }
}
