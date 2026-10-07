package dev.fan4.compat.soundphysics;
import java.util.*;
import java.lang.reflect.Proxy;
import static dev.fan4.compat.shared.CompatCalls.*;
import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.shared.ShipPose.Point;

/** Trace every segment in one coordinate frame and return world-space acoustic hits. */
public final class ShipSoundRaycast {
    private record Frame(ShipPose local,ShipPose global,Set<Long> chunks,AcousticBounds bounds) {}
    private record Snapshot(List<Frame> frames,AcousticBounds terrain) {}
    private static final Map<Object,Snapshot> SNAPSHOTS=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    private static long chunk(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private static long chunk(Point p){return chunk((int)Math.floor(p.x()/16),(int)Math.floor(p.z()/16));}
    /** Called during SPR's main-thread clone, before publishing it to the sound engine. */
    @SuppressWarnings("unchecked")
    public static void capture(Object proxy,Object world,Object origin,int radius) {
        double x=((Number)call(origin,"method_10263")).doubleValue(),y=((Number)call(origin,"method_10264")).doubleValue(),z=((Number)call(origin,"method_10260")).doubleValue(),range=Math.max(16,radius*16);
        Object originShip=exact(UTILS,"getShipManagingPos",new String[]{MC+"class_1937",MC+"class_2338"},world,origin);
        if(originShip!=null){Point moved=pose(originShip,false).position(new Point(x,y,z));x=moved.x();y=moved.y();z=moved.z();}
        Object bounds=createExact(MC+"class_238",new String[]{"double","double","double","double","double","double"},x-range,y-range,z-range,x+range,y+range,z+range);
        List<Frame> frames=new ArrayList<>();Map<Object,Object> cache=(Map<Object,Object>)field(proxy,"clonedLevelChunks");Object source=call(world,"method_2935");
        if(originShip!=null) {
            int centerX=(int)Math.floor(x/16),centerZ=(int)Math.floor(z/16);
            for(int cx=centerX-radius;cx<centerX+radius;cx++)for(int cz=centerZ-radius;cz<centerZ+radius;cz++)cloneChunk(cache,source,world,cx,cz);
        }
        double bottom=((Number)call(world,"method_31607")).doubleValue(),top=bottom+((Number)call(world,"method_31605")).doubleValue();
        Set<Object> nearby=new LinkedHashSet<>();for(Object ship:(Iterable<?>)exact(UTILS,"getShipsIntersecting",new String[]{MC+"class_1937",MC+"class_238"},world,bounds))nearby.add(ship);if(originShip!=null)nearby.add(originShip);
        for(Object ship:nearby) {
            Set<Long> chunks=new HashSet<>();Class<?> callback=type("org.valkyrienskies.core.api.util.functions.IntBinaryConsumer");
            Object collect=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},(self,method,args)->{
                if(method.getName().equals("accept")) {
                    int cx=(Integer)args[0],cz=(Integer)args[1];chunks.add(chunk(cx,cz));
                    cloneChunk(cache,source,world,cx,cz);
                }else if(method.getName().equals("toString"))return "Fan4Compat acoustic snapshot";
                else if(method.getName().equals("hashCode"))return System.identityHashCode(self);
                else if(method.getName().equals("equals"))return self==args[0];
                return null;
            });call(call(ship,"getActiveChunksSet"),"forEach",collect);AcousticBounds extent=null;
            for(Object pos:cache.keySet()){int cx=((Number)field(pos,"field_9181")).intValue(),cz=((Number)field(pos,"field_9180")).intValue();if(chunks.contains(chunk(cx,cz)))extent=include(extent,cx,cz,bottom,top);}
            frames.add(new Frame(pose(ship,true),pose(ship,false),Set.copyOf(chunks),extent));
        }
        Set<Long> shipChunks=new HashSet<>();for(Frame frame:frames)shipChunks.addAll(frame.chunks);
        AcousticBounds terrain=null;for(Object pos:cache.keySet()){int cx=((Number)field(pos,"field_9181")).intValue(),cz=((Number)field(pos,"field_9180")).intValue();if(!shipChunks.contains(chunk(cx,cz))&&!(Boolean)exact(UTILS,"isChunkInShipyard",new String[]{MC+"class_1937","int","int"},world,cx,cz))terrain=include(terrain,cx,cz,bottom,top);}
        SNAPSHOTS.put(proxy,new Snapshot(List.copyOf(frames),terrain));
    }
    private static AcousticBounds include(AcousticBounds bounds,int x,int z,double bottom,double top){return bounds==null?new AcousticBounds(x*16d,bottom,z*16d,(x+1)*16d,top,(z+1)*16d):bounds.include(x,z);}
    private static void cloneChunk(Map<Object,Object> cache,Object source,Object world,int x,int z) {
        Object pos=createExact(MC+"class_1923",new String[]{"int","int"},x,z);if(cache.containsKey(pos))return;
        Object original=call(source,"method_12126",x,z,false);if(original==null)return;
        cache.put(pos,createExact("com.sonicether.soundphysics.world.ClonedLevelChunk",new String[]{MC+"class_1937",MC+"class_1923","[Lnet.minecraft.class_2826;"},world,pos,call(original,"method_12006")));
    }
    private static Object worldPosition(Object point,List<Frame> frames){Point p=point(point);for(Frame frame:frames)if(frame.chunks.contains(chunk(p)))return vec(frame.global.position(p));return point;}
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt",MC="net.minecraft.";
    private static Point point(Object vector){return new Point(((Number)field(vector,"field_1352")).doubleValue(),((Number)field(vector,"field_1351")).doubleValue(),((Number)field(vector,"field_1350")).doubleValue());}
    private static Object vec(Point p){return createExact(MC+"class_243",new String[]{"double","double","double"},p.x(),p.y(),p.z());}
    private static ShipPose pose(Object ship,boolean local){Object matrix=call(ship,local?"getWorldToShip":"getShipToWorld");double[] m=new double[16];for(int c=0;c<4;c++)for(int r=0;r<4;r++)m[c*4+r]=((Number)call(matrix,"m"+c+r)).doubleValue();return new ShipPose(m);}
    private static Object original(Object operation,Object world,Object from,Object to,Object ignore){return call(operation,"call",(Object)new Object[]{world,from,to,ignore});}
    private static boolean hit(Object hit){return hit!=null&&((Enum<?>)call(hit,"method_17783")).name().equals("BLOCK");}
    private static double distance(Object from,Object hit){return ((Number)call(from,"method_1025",call(hit,"method_17784"))).doubleValue();}
    private static Object miss(Object from,Object to) {
        Point p=point(to),a=point(from);Object facing=call(type(MC+"class_2350"),"method_10142",p.x()-a.x(),p.y()-a.y(),p.z()-a.z());
        Object block=createExact(MC+"class_2338",new String[]{"int","int","int"},(int)Math.floor(p.x()),(int)Math.floor(p.y()),(int)Math.floor(p.z()));
        return exact(MC+"class_3965","method_17778",new String[]{MC+"class_243",MC+"class_2350",MC+"class_2338"},to,facing,block);
    }
    /** Trace only cached space, subdividing long cached segments below IP's 512-block limit. */
    private static Object trace(Object operation,Object world,Object from,Object to,Object ignore,AcousticBounds bounds) {
        if(bounds==null)return miss(from,to);Point a=point(from),b=point(to);double[] interval=bounds.interval(a,b);if(interval==null)return miss(from,to);
        Point first=AcousticBounds.lerp(a,b,interval[0]),last=AcousticBounds.lerp(a,b,interval[1]);
        double length=Math.sqrt(Math.pow(last.x()-first.x(),2)+Math.pow(last.y()-first.y(),2)+Math.pow(last.z()-first.z(),2));
        int steps=Math.max(1,(int)Math.ceil(length/256));Object result=null;
        for(int i=0;i<steps;i++) {result=original(operation,world,vec(AcousticBounds.lerp(first,last,(double)i/steps)),vec(AcousticBounds.lerp(first,last,(double)(i+1)/steps)),ignore);if(hit(result))return result;}
        return result==null?miss(from,to):result;
    }
    public static Object rayCast(Object world,Object from,Object to,Object ignore,Object operation) {
        Snapshot snapshot=SNAPSHOTS.get(world);
        if(snapshot==null)return original(operation,world,from,to,ignore);
        List<Frame> frames=snapshot.frames;
        Object start=worldPosition(from,frames),end=worldPosition(to,frames);
        Object best=trace(operation,world,start,end,ignore,snapshot.terrain);double closest=hit(best)?distance(start,best):Double.POSITIVE_INFINITY;
        Point a=point(start),b=point(end);
        for(Frame frame:frames) {
            ShipPose local=frame.local,global=frame.global;
            Object candidate=trace(operation,world,vec(local.position(a)),vec(local.position(b)),ignore,frame.bounds);
            if(!hit(candidate))continue;
            Object block=call(candidate,"method_17777");
            if(!frame.chunks.contains(chunk(Math.floorDiv(((Number)call(block,"method_10263")).intValue(),16),Math.floorDiv(((Number)call(block,"method_10260")).intValue(),16))))continue;
            Object position=vec(global.position(point(call(candidate,"method_17784"))));double d=((Number)call(start,"method_1025",position)).doubleValue();
            if(d>=closest)continue;
            Object directionVector=call(call(candidate,"method_17780"),"method_10163");
            Point direction=global.direction(new Point(((Number)call(directionVector,"method_10263")).doubleValue(),((Number)call(directionVector,"method_10264")).doubleValue(),((Number)call(directionVector,"method_10260")).doubleValue()));
            Object facing=call(type(MC+"class_2350"),"method_10142",direction.x(),direction.y(),direction.z());
            best=createExact(MC+"class_3965",new String[]{MC+"class_243",MC+"class_2350",MC+"class_2338","boolean"},position,facing,call(candidate,"method_17777"),call(candidate,"method_17781"));closest=d;
        }
        return best;
    }
}
