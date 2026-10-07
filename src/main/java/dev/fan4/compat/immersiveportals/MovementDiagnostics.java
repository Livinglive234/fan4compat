package dev.fan4.compat.immersiveportals;

import static dev.fan4.compat.shared.CompatCalls.*;
import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.shared.ShipPose.Point;
import java.util.*;

/** Temporary bounded observers: no collision, teleport, loading or movement changes. */
public final class MovementDiagnostics {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("fan4compat.movementDebug","false"));
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private static final Map<Object,Budget> BUDGETS=new WeakHashMap<>();
    private static final Set<Object> FAILED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final System.Logger LOG=System.getLogger("Fan4Compat movement");
    private static final class Scope {final Object entity,vector;boolean guarded;Scope(Object e,Object v){entity=e;vector=v;}}
    public static final class Budget {
        private java.lang.ref.WeakReference<Object> world=new java.lang.ref.WeakReference<>(null);private int count;private long next;
        public boolean take(Object value,long now) {
            if(world.get()!=value){world=new java.lang.ref.WeakReference<>(value);count=0;next=0;}
            if(count>=12||(count>0&&now<next))return false;
            count++;next=now+3_000_000_000L;return true;
        }
    }
    public static void move(Object entity,Object kind,Object vector,Object operation) {
        if(!ENABLED||!type("net.minecraft.class_1657").isInstance(entity)){call(operation,"call",(Object)new Object[]{kind,vector});return;}
        Scope previous=CURRENT.get(),scope=new Scope(entity,vector);CURRENT.set(scope);
        Point before=point(call(entity,"method_19538"));boolean success=false;
        try {call(operation,"call",(Object)new Object[]{kind,vector});success=true;}
        finally {
            if(previous==null)CURRENT.remove();else CURRENT.set(previous);
            if(success)try {
                Point requested=point(vector),after=point(call(entity,"method_19538"));
                double request=length(requested),actual=Math.sqrt(Math.pow(after.x()-before.x(),2)+Math.pow(after.y()-before.y(),2)+Math.pow(after.z()-before.z(),2));
                if(request>1e-5&&(scope.guarded||((Math.abs(requested.x())+Math.abs(requested.z())>1e-5||(Boolean)field(entity,"field_5960"))&&actual<request*.1)))report(entity,scope.guarded?"VS_UNLOADED_SHIP_GUARD":"MOVEMENT_CLIPPED",requested,"actualDistance="+actual);
            }catch(RuntimeException e){failure(entity,e);}
        }
    }
    /** Observe the native return value without changing it. */
    public static void guard(Object entity,boolean blocked) {
        ShipLoadRecovery.guard(entity,blocked);
        if(!ENABLED||!blocked)return;
        Scope scope=CURRENT.get();if(scope!=null&&scope.entity==entity)scope.guarded=true;
    }
    public static void terrain(Object client) {
        if(!ENABLED||field(client,"field_1755")!=null)return;
        Object player=field(client,"field_1724"),world=field(client,"field_1687");if(player==null||world==null)return;
        try {
            Object options=field(client,"field_1690");boolean input=false;
            for(String key:new String[]{"field_1894","field_1881","field_1913","field_1849","field_1903"})input|=(Boolean)call(field(options,key),"method_1434");
            if(!input)return;Point pos=point(call(player,"method_19538"));
            if(call(world,"method_22338",chunk(pos.x()),chunk(pos.z()))==null)report(player,"TERRAIN_CHUNK_MISSING",null,"movementKeyPressed=true");
        }catch(RuntimeException e){failure(player,e);}
    }
    private static int chunk(double v){return (int)Math.floor(v/16);}
    private static double length(Point p){return Math.sqrt(p.x()*p.x()+p.y()*p.y()+p.z()*p.z());}
    private static Point point(Object v){return new Point(number(v,"field_1352"),number(v,"field_1351"),number(v,"field_1350"));}
    private static double number(Object v,String name){return ((Number)field(v,name)).doubleValue();}
    private static boolean sample(Object entity,Object world) {synchronized(BUDGETS){return BUDGETS.computeIfAbsent(entity,k->new Budget()).take(world,System.nanoTime());}}
    private static void failure(Object entity,RuntimeException e) {
        synchronized(FAILED){if(FAILED.add(entity))LOG.log(System.Logger.Level.WARNING,"[Fan4Compat movement] diagnostic unavailable: "+e);}
    }
    private static void report(Object entity,String reason,Point requested,String detail) {
        Object world=call(entity,"method_37908");if(!sample(entity,world))return;
        Point pos=point(call(entity,"method_19538"));Object box=call(entity,"method_5829");boolean client=(Boolean)field(world,"field_9236");
        StringBuilder message=new StringBuilder("[Fan4Compat movement] reason=").append(reason).append(" side=").append(client?"client":"server").append(" dimension=").append(call(world,"method_27983")).append(" position=").append(pos).append(" requested=").append(requested).append(' ').append(detail).append(" playerBounds=").append(box).append(" noClip=").append(field(entity,"field_5960")).append(" terrainLoaded=").append(call(world,"method_22338",chunk(pos.x()),chunk(pos.z()))!=null);
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{client?"net.minecraft.class_638":"net.minecraft.class_3218"},world);
        if(client)message.append(" shipWorldSynced=").append(call(ships,"isSyncedWithServer"));
        else message.append(" awaitingTeleport=").append(field(field(entity,"field_13987"),"field_14119"));
        Object dragging=call(entity,"getDraggingInformation");message.append(" standingShip=").append(call(dragging,"getLastShipStoodOn"));
        Object dimension=exact(UTILS,"getDimensionId",new String[]{"net.minecraft.class_1937"},world);int candidates=0;
        for(Object ship:(Iterable<?>)call(ships,"getAllShips")) {
            if(!dimension.equals(call(ship,"getChunkClaimDimension")))continue;
            Object bounds=call(ship,"getWorldAABB");
            if(bounds==null||!near(pos,bounds,64))continue;
            if(candidates++>=8){message.append(" additionalShipsOmitted=true");break;}
            long id=((Number)call(ship,"getId")).longValue();
            message.append(" ship={id=").append(id).append(" known=").append(call(entity,"vs_isKnownShip",id)).append(" loaded=").append(call(call(ships,"getLoadedShips"),"getById",id)!=null).append(" bounds=").append(bounds);
            Object matrix=call(ship,"getWorldToShip");double[] values=new double[16];for(int c=0;c<4;c++)for(int r=0;r<4;r++)values[c*4+r]=((Number)call(matrix,"m"+c+r)).doubleValue();ShipPose pose=new ShipPose(values);
            double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
            for(int corner=0;corner<8;corner++) {
                Point local=pose.position(new Point(number(box,(corner&1)==0?"field_1323":"field_1320"),number(box,(corner&2)==0?"field_1322":"field_1325"),number(box,(corner&4)==0?"field_1321":"field_1324")));
                minX=Math.min(minX,local.x());maxX=Math.max(maxX,local.x());minZ=Math.min(minZ,local.z());maxZ=Math.max(maxZ,local.z());
            }
            Object active=call(ship,"getActiveChunksSet");int missing=0,checked=0;List<String> coordinates=new ArrayList<>();
            for(int x=chunk(minX-1.0000001);x<=chunk(maxX+1.0000001)&&checked<64;x++)for(int z=chunk(minZ-1.0000001);z<=chunk(maxZ+1.0000001)&&checked<64;z++) {
                checked++;if(!(Boolean)call(active,"contains",x,z))continue;
                if(call(world,"method_22338",x,z)==null){missing++;if(coordinates.size()<8)coordinates.add(x+","+z);}
            }
            message.append(" missingActiveChunks=").append(missing).append(" missingChunkPositions=").append(coordinates).append(" checkedCells=").append(checked).append('}');
        }
        LOG.log(System.Logger.Level.WARNING,message.toString());
    }
    private static boolean near(Point p,Object b,double margin) {
        return p.x()>=((Number)call(b,"minX")).doubleValue()-margin&&p.x()<=((Number)call(b,"maxX")).doubleValue()+margin&&p.y()>=((Number)call(b,"minY")).doubleValue()-margin&&p.y()<=((Number)call(b,"maxY")).doubleValue()+margin&&p.z()>=((Number)call(b,"minZ")).doubleValue()-margin&&p.z()<=((Number)call(b,"maxZ")).doubleValue()+margin;
    }
}
