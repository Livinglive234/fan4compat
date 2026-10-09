package dev.fan4.compat.soundphysics;

import java.util.*;
import dev.fan4.compat.shared.ShipPose.Point;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Hold one published acoustic snapshot for each sound evaluation. */
public final class PortalSoundCompat {
    private static final ThreadLocal<Object> SCOPE=new ThreadLocal<>();
    private static final Map<Object,AcousticBounds> BOUNDS=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    public static Object snapshot(){return SCOPE.get();}
    public static boolean scoped(){return SCOPE.get()!=null;}
    public static Object evaluate(Object operation,int source,double x,double y,double z,Object category,Object id,boolean auxiliary) {
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        Object proxy=call(type("com.sonicether.soundphysics.utils.LevelAccessUtils"),"getClientLevelProxy",client);
        if(proxy==null){call(type("com.sonicether.soundphysics.SoundPhysics"),"setDefaultEnvironment",source,auxiliary);return null;}
        Object previous=SCOPE.get();SCOPE.set(proxy);
        try{return call(operation,"call",(Object)new Object[]{source,x,y,z,category,id,auxiliary});}
        finally{if(previous==null)SCOPE.remove();else SCOPE.set(previous);}
    }
    private static Point point(Object vec){return new Point(((Number)field(vec,"field_1352")).doubleValue(),((Number)field(vec,"field_1351")).doubleValue(),((Number)field(vec,"field_1350")).doubleValue());}
    private static Object vec(Point p){return createExact("net.minecraft.class_243",new String[]{"double","double","double"},p.x(),p.y(),p.z());}
    private static Object miss(Object from,Object to){
        Point a=point(from),b=point(to);Object direction=call(type("net.minecraft.class_2350"),"method_10142",b.x()-a.x(),b.y()-a.y(),b.z()-a.z());
        Object block=createExact("net.minecraft.class_2338",new String[]{"int","int","int"},(int)Math.floor(b.x()),(int)Math.floor(b.y()),(int)Math.floor(b.z()));
        return exact("net.minecraft.class_3965","method_17778",new String[]{"net.minecraft.class_243","net.minecraft.class_2350","net.minecraft.class_2338"},to,direction,block);
    }
    private static AcousticBounds bounds(Object proxy){
        synchronized(BOUNDS){if(BOUNDS.containsKey(proxy))return BOUNDS.get(proxy);}
        double bottom=((Number)call(proxy,"method_31607")).doubleValue(),top=bottom+((Number)call(proxy,"method_31605")).doubleValue();
        AcousticBounds result=null;
        for(Object pos:((Map<?,?>)field(proxy,"clonedLevelChunks")).keySet()){
            int x=((Number)field(pos,"field_9181")).intValue(),z=((Number)field(pos,"field_9180")).intValue();
            result=result==null?new AcousticBounds(x*16d,bottom,z*16d,(x+1)*16d,top,(z+1)*16d):result.include(x,z);
        }
        synchronized(BOUNDS){BOUNDS.put(proxy,result);}return result;
    }
    /** Only used without VS; ship frames are handled by ShipSoundRaycast. */
    public static Object rayCast(Object proxy,Object from,Object to,Object ignore,Object operation){
        if(proxy==null||!type("com.sonicether.soundphysics.world.ClonedClientLevel").isInstance(proxy))
            return call(operation,"call",(Object)new Object[]{proxy,from,to,ignore});
        AcousticBounds bounds=bounds(proxy);if(bounds==null)return miss(from,to);
        Point a=point(from),b=point(to);double[] interval=bounds.interval(a,b);if(interval==null)return miss(from,to);
        Point first=AcousticBounds.lerp(a,b,interval[0]),last=AcousticBounds.lerp(a,b,interval[1]);
        double length=new Point(last.x()-first.x(),last.y()-first.y(),last.z()-first.z()).length();
        int steps=Math.max(1,(int)Math.ceil(length/256));
        for(int i=0;i<steps;i++){
            Object hit=call(operation,"call",(Object)new Object[]{proxy,vec(AcousticBounds.lerp(first,last,(double)i/steps)),vec(AcousticBounds.lerp(first,last,(double)(i+1)/steps)),ignore});
            if(hit!=null&&((Enum<?>)call(hit,"method_17783")).name().equals("BLOCK"))return hit;
        }
        return miss(from,to);
    }
}
