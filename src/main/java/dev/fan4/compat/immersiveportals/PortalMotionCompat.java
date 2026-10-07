package dev.fan4.compat.immersiveportals;

import dev.fan4.compat.shared.ShipPose;
import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;
import static dev.fan4.compat.shared.ShipPose.*;

/** Small ship-local attachment descriptors; client geometry and crossing history share one pose. */
public final class PortalMotionCompat {
    private static final String KEY="fan4compatShipDoorPose",UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    private static final String SIDE="qouteall.imm_ptl.core.portal.animation.UnilateralPortalState";
    public record Attachment(long ship,boolean destination,Point position,Point right,Point up,double width,double height) {
        public String encode() {
            return ship+","+destination+","+position.x()+","+position.y()+","+position.z()+","+right.x()+","+right.y()+","+right.z()+","+up.x()+","+up.y()+","+up.z()+","+width+","+height;
        }
        public static Attachment decode(String text) {
            String[] s=text.split(",",-1);if(s.length!=13||!(s[1].equals("true")||s[1].equals("false")))throw new IllegalArgumentException("Invalid ship doorway descriptor");
            double[] d=new double[11];for(int i=0;i<11;i++){d[i]=Double.parseDouble(s[i+2]);if(!Double.isFinite(d[i]))throw new IllegalArgumentException("Non-finite doorway pose");}
            if(d[9]<=0||d[10]<=0)throw new IllegalArgumentException("Invalid doorway size");
            Point right=new Point(d[3],d[4],d[5]).normalized(),up=new Point(d[6],d[7],d[8]).normalized();
            if(Math.abs(right.x()*up.x()+right.y()*up.y()+right.z()*up.z())>1e-6)throw new IllegalArgumentException("Non-orthogonal doorway axes");
            return new Attachment(Long.parseLong(s[0]),Boolean.parseBoolean(s[1]),new Point(d[0],d[1],d[2]),right,up,d[9],d[10]);
        }
    }
    private static final Map<Object,Attachment> ATTACHMENTS=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    private static final Map<Object,Attachment> CLIENT=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    private static final Map<Object,FrameHistory> HISTORY=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    public static final class FrameHistory {
        private Object state;private long counter=Long.MIN_VALUE;
        public Object advance(long next,Object value) {Object previous=counter==next-1?state:null;counter=next;state=value;return previous;}
    }
    public static void attach(Object portal,long ship,boolean destination,Point p,Point right,Point up,double width,double height) {
        ATTACHMENTS.put(portal,new Attachment(ship,destination,p,right,up,width,height));
    }
    public static void detach(Object portal) {ATTACHMENTS.remove(portal);CLIENT.remove(portal);clearHistory(portal);}
    public static void write(Object portal,Object tag) {
        Attachment a=ATTACHMENTS.get(portal);
        if(a!=null)call(tag,"method_10582",KEY,a.encode());
    }
    public static void readClient(Object portal,Object tag) {
        Attachment a=null;
        if((Boolean)call(tag,"method_10545",KEY)) {
            try {a=Attachment.decode((String)call(tag,"method_10558",KEY));}
            catch(IllegalArgumentException ignored) { /* Malformed metadata must not drive client geometry. */ }
        }
        if(a==null){CLIENT.remove(portal);clearHistory(portal);return;}
        Attachment old=CLIENT.put(portal,a);if(!a.equals(old))clearHistory(portal);
    }
    /** VS already applies deck motion separately from the player's walking velocity. */
    public static Object pointVelocity(Object portal,Object entity,Object original) {
        Attachment a=CLIENT.get(portal);if(a==null)return original;
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        if(entity!=field(client,"field_1724"))return original;
        Object world=field(client,"field_1687");if(world==null)return original;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_638"},world);
        Object ship=call(call(ships,"getLoadedShips"),"getById",a.ship);if(ship==null)return original;
        Object side=call(call(portal,"getPortalState"),a.destination?"getOtherSideState":"getThisSideState");
        if(!call(type(UTILS),"getResourceKey",call(ship,"getChunkClaimDimension")).equals(call(side,"dimension")))return original;
        Object zero=vec(new Point(0,0,0));
        return createExact("qouteall.imm_ptl.core.teleportation.TeleportationUtil$PortalPointVelocity",new String[]{"net.minecraft.class_243","net.minecraft.class_243"},zero,zero);
    }
    /** Let native ship dragging settle before the moving reverse portal can recross the arrival. */
    public static void crossed(Object teleportation) {
        Object portal=call(teleportation,"portal");Attachment a=CLIENT.get(portal);
        if(a==null)return;
        call(type("qouteall.imm_ptl.core.teleportation.ClientTeleportationManager"),"disableTeleportFor",5);
        for(var entry:entries())clearHistory(entry.getKey());
        if(!a.destination)return;
        Object client=call(type("net.minecraft.class_310"),"method_1551"),world=field(client,"field_1687"),player=field(client,"field_1724");
        if(world==null||player==null)return;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_638"},world),ship=call(call(ships,"getLoadedShips"),"getById",a.ship);
        if(ship==null||!call(type(UTILS),"getResourceKey",call(ship,"getChunkClaimDimension")).equals(call(world,"method_27983")))return;
        Object drag=call(player,"getDraggingInformation");
        call(drag,"setLastShipStoodOn",a.ship);call(drag,"setTicksSinceStoodOnShip",0);
        // Velocity is already in the deck frame; the boarding impulse would subtract motion again.
        call(drag,"setShouldImpulseMovement",false);
    }
    public static void cleanupClient() {
        for(var entry:entries())clearHistory(entry.getKey());
        CLIENT.clear();HISTORY.clear();
    }
    private static void clearHistory(Object portal) {
        if(HISTORY.remove(portal)==null)return;
        Object animation=field(portal,"animation");
        if((Boolean)call(animation,"hasAnimationDriver"))return;
        for(String name:new String[]{"lastTickAnimatedState","thisTickAnimatedState","clientLastFramePortalState","clientCurrentFramePortalState"})writeField(animation,name,null);
        writeField(animation,"clientLastFramePortalStateCounter",-1L);writeField(animation,"clientCurrentFramePortalStateCounter",-1L);
    }
    private static double n(Object value){return ((Number)value).doubleValue();}
    private static Object vec(Point p){return create("net.minecraft.class_243",p.x(),p.y(),p.z());}
    private static ShipPose pose(Object transform) {
        Object m=call(transform,"getShipToWorld");double[] d=new double[16];
        for(int c=0;c<4;c++)for(int r=0;r<4;r++)d[c*4+r]=n(call(m,"m"+c+r));
        return new ShipPose(d);
    }
    public static Object state(Object base,Attachment a,ShipPose pose) {
        Object thisSide=call(base,"getThisSideState"),otherSide=call(base,"getOtherSideState");
        Object side=a.destination?otherSide:thisSide;
        Point right=pose.direction(a.right),up=pose.direction(a.up);
        Object orientation=call(type("qouteall.q_misc_util.my_util.DQuaternion"),"fromFacingVecs",vec(right.normalized()),vec(up.normalized()));
        Object updated=create(SIDE,call(side,"dimension"),vec(pose.position(a.position)),orientation,a.width*right.length(),a.height*up.length(),n(call(side,"thickness")));
        return call(type(SIDE),"combine",a.destination?thisSide:updated,a.destination?updated:otherSide);
    }
    static List<Map.Entry<Object,Attachment>> serverEntries() {synchronized(ATTACHMENTS){return new ArrayList<>(ATTACHMENTS.entrySet());}}
    private record Poses(ShipPose previous,ShipPose current,ShipPose render) {}
    private static List<Map.Entry<Object,Attachment>> entries() {synchronized(CLIENT){return new ArrayList<>(CLIENT.entrySet());}}
    /** Runs inside IP's manageTeleportation after its counter advances, before it samples portals. */
    public static void beforeCrossing(boolean ticking) {
        if(CLIENT.isEmpty())return;
        Object client=call(type("net.minecraft.class_310"),"method_1551");
        Object world=field(client,"field_1687");if(world==null||field(client,"field_1724")==null)return;
        Object ships=exact(UTILS,"getShipObjectWorld",new String[]{"net.minecraft.class_638"},world);
        if(!(Boolean)call(ships,"isSyncedWithServer"))return;
        double partial=ticking?1:n(call(type("qouteall.imm_ptl.core.render.context_management.RenderStates"),"getPartialTick"));
        long counter=((Number)field(type("qouteall.imm_ptl.core.teleportation.ClientTeleportationManager"),"teleportationCounter")).longValue();
        Object loaded=call(ships,"getLoadedShips");Map<Long,Poses> poses=new HashMap<>();
        for(var entry:entries()) {
            Object portal=entry.getKey();Attachment a=entry.getValue();
            if((Boolean)call(portal,"method_31481")){detach(portal);continue;}
            Object ship=call(loaded,"getById",a.ship);
            if(ship==null){clearHistory(portal);continue;}
            if(call(ship,"getTransformProvider")!=null){clearHistory(portal);continue;}
            Object dimension=call(type(UTILS),"getResourceKey",call(ship,"getChunkClaimDimension"));
            Object base=call(portal,"getPortalState"),side=call(base,a.destination?"getOtherSideState":"getThisSideState");
            if(!dimension.equals(call(side,"dimension"))){clearHistory(portal);continue;}
            Poses p=poses.computeIfAbsent(a.ship,id->{
                Object previous=call(ship,"getPrevTickTransform"),current=call(ship,"getTransform");
                // Same default interpolation as ShipObjectClientWorld.updateRenderTransforms.
                Object render=exact("org.valkyrienskies.core.impl.shadow.Eg","a",new String[]{"org.valkyrienskies.core.api.ships.properties.ShipTransform","org.valkyrienskies.core.api.ships.properties.ShipTransform","double"},previous,current,partial);
                return new Poses(pose(previous),pose(current),pose(render));
            });
            Object rendered=state(base,a,ticking?p.current:p.render);
            Object animation=field(portal,"animation");
            // Do not overwrite a genuine IP animation's frame history.
            if((Boolean)call(animation,"hasAnimationDriver")){clearHistory(portal);continue;}
            FrameHistory history=HISTORY.computeIfAbsent(portal,k->new FrameHistory());
            Object last=history.advance(counter,rendered);
            writeField(animation,"lastTickAnimatedState",state(base,a,p.previous));
            writeField(animation,"thisTickAnimatedState",state(base,a,p.current));
            writeField(animation,"clientLastFramePortalState",last);
            writeField(animation,"clientLastFramePortalStateCounter",last==null?-1L:counter-1);
            writeField(animation,"clientCurrentFramePortalState",rendered);
            writeField(animation,"clientCurrentFramePortalStateCounter",counter);
            call(portal,"setPortalState",rendered);call(portal,"updateCache");
        }
    }
    /** Reconcile portal drawing with the exact native render transform, after VS updates it. */
    public static void afterRenderTransforms(Object ships) {
        if(CLIENT.isEmpty())return;
        Object loaded=call(ships,"getLoadedShips");Map<Long,ShipPose> poses=new HashMap<>();
        for(var entry:entries()) {
            Object portal=entry.getKey();Attachment a=entry.getValue();
            if((Boolean)call(portal,"method_31481"))continue;
            Object ship=call(loaded,"getById",a.ship);if(ship==null)continue;
            if(call(ship,"getTransformProvider")!=null||(Boolean)call(field(portal,"animation"),"hasAnimationDriver"))continue;
            Object base=call(portal,"getPortalState"),side=call(base,a.destination?"getOtherSideState":"getThisSideState");
            if(!call(type(UTILS),"getResourceKey",call(ship,"getChunkClaimDimension")).equals(call(side,"dimension")))continue;
            ShipPose p=poses.computeIfAbsent(a.ship,id->pose(call(ship,"getRenderTransform")));
            call(portal,"setPortalState",state(base,a,p));call(portal,"updateCache");
        }
    }
}
