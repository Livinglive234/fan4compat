import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.immersiveportals.PortalMotionCompat;

public class PortalMotionSmokeTest {
    public static class Vec {public double field_1352,field_1351,field_1350;public Vec(double x,double y,double z){field_1352=x;field_1351=y;field_1350=z;}}
    public record Orientation(Vec right,Vec up){public static Orientation fromFacingVecs(Vec r,Vec u){return new Orientation(r,u);}}
    public record Side(String dimension,Vec position,Orientation orientation,double width,double height,double thickness){
        public static State combine(Side from,Side to){return new State(from,to);}
    }
    public record State(Side from,Side to){public Side getThisSideState(){return from;}public Side getOtherSideState(){return to;}}
    public static class Animation {
        public Object lastTickAnimatedState,thisTickAnimatedState,clientLastFramePortalState,clientCurrentFramePortalState;
        public long clientLastFramePortalStateCounter=-1,clientCurrentFramePortalStateCounter=-1;boolean driver;
        public boolean hasAnimationDriver(){return driver;}
    }
    public static class Portal {
        public Animation animation=new Animation();State state;boolean removed;int updates;
        Portal(State s){state=s;}public boolean method_31481(){return removed;}public State getPortalState(){return state;}
        public void setPortalState(State s){state=s;updates++;}public void updateCache(){}
    }
    public static class Matrix {
        double scale,tx;boolean rotate;
        Matrix(double x,double s,boolean r){tx=x;scale=s;rotate=r;}
        public double m00(){return rotate?0:scale;}public double m01(){return 0;}public double m02(){return rotate?-scale:0;}public double m03(){return 0;}
        public double m10(){return 0;}public double m11(){return scale;}public double m12(){return 0;}public double m13(){return 0;}
        public double m20(){return rotate?scale:0;}public double m21(){return 0;}public double m22(){return rotate?0:scale;}public double m23(){return 0;}
        public double m30(){return tx;}public double m31(){return 0;}public double m32(){return 0;}public double m33(){return 1;}
    }
    public record Transform(Matrix matrix){public Matrix getShipToWorld(){return matrix;}}
    public static class Ship {
        Transform previous=new Transform(new Matrix(10,1,false)),current=new Transform(new Matrix(30,1,false)),render=new Transform(new Matrix(20,1,false));Object provider;
        public String getChunkClaimDimension(){return "overworld";}public Transform getPrevTickTransform(){return previous;}public Transform getTransform(){return current;}
        public Object getTransformProvider(){return provider;}public Transform getRenderTransform(){return render;}
    }
    public static class Ships {Ship ship=new Ship();boolean synced=true;public boolean isSyncedWithServer(){return synced;}public Ships getLoadedShips(){return this;}public Ship getById(long id){return id==42?ship:null;}}
    public static class World {public String method_27983(){return "overworld";}}
    public static class Client {public World field_1687=new World();public Object field_1724=new Player();static Client client=new Client();public static Client method_1551(){return client;}}
    public static class Utils {static Ships ships=new Ships();public static Ships getShipObjectWorld(World w){return ships;}public static String getResourceKey(String d){return d;}}
    public static class Native {static int calls;public static Transform a(Transform previous,Transform current,double partial){calls++;return new Transform(new Matrix(previous.matrix.tx+(current.matrix.tx-previous.matrix.tx)*partial,current.matrix.scale,current.matrix.rotate));}}
    public static class Render {public static float getPartialTick(){return .5f;}}
    public static class Crossing {public static long teleportationCounter=1;static int cooldown;public static void disableTeleportFor(int ticks){cooldown=ticks;}}
    public static class Player {final Drag drag=new Drag();public Drag getDraggingInformation(){return drag;}}
    public record Velocity(Vec thisSidePointVelocity,Vec otherSidePointVelocity) {}
    public static class Drag {Long ship;int ticks=100;boolean impulse;public void setLastShipStoodOn(Long id){ship=id;impulse=true;}public void setTicksSinceStoodOnShip(int value){ticks=value;}public void setShouldImpulseMovement(boolean value){impulse=value;}}
    public record Teleportation(Portal portal) {}
    public static class Tag {Map<String,String> values=new HashMap<>();public void method_10582(String k,String v){values.put(k,v);}public boolean method_10545(String k){return values.containsKey(k);}public String method_10558(String k){return values.get(k);}}
    static Side side(String dimension,double x){return new Side(dimension,new Vec(x,0,0),new Orientation(new Vec(1,0,0),new Vec(0,1,0)),1,2,0);}
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void at(Vec v,double x,double y,double z){check(Math.abs(v.field_1352-x)<1e-8&&Math.abs(v.field_1351-y)<1e-8&&Math.abs(v.field_1350-z)<1e-8,"incorrect doorway pose");}
    static void contracts(String vs,String ip,String generated)throws Exception {
        Map<String,ClassNode> classes=new HashMap<>();
        for(String jar:List.of(vs,ip))try(ZipFile z=new ZipFile(jar)){for(var e:Collections.list(z.entries()))if(e.getName().endsWith(".class")){ClassNode c=new ClassNode();new ClassReader(z.getInputStream(e)).accept(c,0);classes.put(c.name,c);}}
        VerifyAddon.classes.putAll(classes);
        for(String mixin:List.of("Data","Crossing","Render","Cleanup")) {
            ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(generated,"dev/fan4/compat/mixin/immersiveportals/client/MixinShipPortalMotion"+mixin+".class"))).accept(c,0);VerifyAddon.injectionTargets(c);
        }
        String[][] calls={{"org/valkyrienskies/core/impl/shadow/Eg","a","(Lorg/valkyrienskies/core/api/ships/properties/ShipTransform;Lorg/valkyrienskies/core/api/ships/properties/ShipTransform;D)Lorg/valkyrienskies/core/api/ships/properties/ShipTransform;"},
            {"qouteall/q_misc_util/my_util/DQuaternion","fromFacingVecs","(Lnet/minecraft/class_243;Lnet/minecraft/class_243;)Lqouteall/q_misc_util/my_util/DQuaternion;"},
            {"qouteall/imm_ptl/core/portal/animation/UnilateralPortalState","<init>","(Lnet/minecraft/class_5321;Lnet/minecraft/class_243;Lqouteall/q_misc_util/my_util/DQuaternion;DDD)V"},
            {"qouteall/imm_ptl/core/portal/animation/UnilateralPortalState","combine","(Lqouteall/imm_ptl/core/portal/animation/UnilateralPortalState;Lqouteall/imm_ptl/core/portal/animation/UnilateralPortalState;)Lqouteall/imm_ptl/core/portal/PortalState;"}};
        for(String[] c:calls)check(VerifyAddon.exists(c[0],c[1],c[2],new HashSet<>()),"missing native motion contract "+Arrays.toString(c));
        for(String f:List.of("lastTickAnimatedState","thisTickAnimatedState","clientLastFramePortalState","clientCurrentFramePortalState","clientLastFramePortalStateCounter","clientCurrentFramePortalStateCounter"))check(VerifyAddon.fieldExists("qouteall/imm_ptl/core/portal/animation/PortalAnimation",f),"missing IP motion history field "+f);
        check(VerifyAddon.fieldExists("qouteall/imm_ptl/core/teleportation/ClientTeleportationManager","teleportationCounter"),"missing crossing counter");
        System.out.println("PASS: exact VS interpolation, IP geometry/history contracts and all motion injection selectors");
    }
    public static void main(String[] args)throws Exception {
        var a=new PortalMotionCompat.Attachment(42,false,new ShipPose.Point(1,2,3),new ShipPose.Point(1,0,0),new ShipPose.Point(0,1,0),1,2);
        check(PortalMotionCompat.Attachment.decode(a.encode()).equals(a),"attachment metadata round trip");
        try{PortalMotionCompat.Attachment.decode(a.encode().replace(",1.0,2.0",",NaN,2.0"));throw new AssertionError("non-finite metadata accepted");}catch(IllegalArgumentException expected){}
        PortalMotionCompat.FrameHistory h=new PortalMotionCompat.FrameHistory();check(h.advance(1,"first")==null,"first frame safe");check(h.advance(2,"second").equals("first"),"consecutive frame history");check(h.advance(4,"gap")==null,"gap does not reuse old crossing state");
        Map<String,String> names=new HashMap<>();String[][] mappings={{"net.minecraft.class_243","Vec"},{"net.minecraft.class_310","Client"},{"net.minecraft.class_638","World"},{"org.valkyrienskies.mod.common.VSGameUtilsKt","Utils"},{"org.valkyrienskies.core.api.ships.properties.ShipTransform","Transform"},{"org.valkyrienskies.core.impl.shadow.Eg","Native"},{"qouteall.q_misc_util.my_util.DQuaternion","Orientation"},{"qouteall.imm_ptl.core.portal.animation.UnilateralPortalState","Side"},{"qouteall.imm_ptl.core.render.context_management.RenderStates","Render"},{"qouteall.imm_ptl.core.teleportation.ClientTeleportationManager","Crossing"}};
        names.put("qouteall.imm_ptl.core.teleportation.TeleportationUtil$PortalPointVelocity","PortalMotionSmokeTest$Velocity");
        for(String[] n:mappings)names.put(n[0],"PortalMotionSmokeTest$"+n[1]);
        String helperName="dev.fan4.compat.immersiveportals.PortalMotionCompat";
        ClassLoader loader=new ClassLoader(PortalMotionSmokeTest.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException {
                if(!n.startsWith(helperName))return super.loadClass(n,resolve);
                Class<?> cached=findLoadedClass(n);if(cached!=null)return cached;
                try{ClassWriter w=new ClassWriter(0);new ClassReader(Files.readAllBytes(Path.of(args[0],n.replace('.','/')+".class"))).accept(new ClassRemapper(w,new Remapper(){public Object mapValue(Object value){return value instanceof String s?names.getOrDefault(s,s):super.mapValue(value);}}),0);byte[] b=w.toByteArray();Class<?> c=defineClass(n,b,0,b.length);if(resolve)resolveClass(c);return c;}catch(Exception e){throw new ClassNotFoundException(n,e);}
            }
        };
        Class<?> helper=loader.loadClass(helperName);var read=helper.getMethod("readClient",Object.class,Object.class);var frame=helper.getMethod("beforeCrossing",boolean.class);var render=helper.getMethod("afterRenderTransforms",Object.class);
        Portal outer=new Portal(new State(side("overworld",0),side("tardis",100))),inner=new Portal(new State(side("tardis",100),side("overworld",0)));
        Tag outTag=new Tag();outTag.method_10582("fan4compatShipDoorPose",a.encode());read.invoke(null,outer,outTag);
        var inward=new PortalMotionCompat.Attachment(42,true,a.position(),a.right(),a.up(),1,2);Tag inTag=new Tag();inTag.method_10582("fan4compatShipDoorPose",inward.encode());read.invoke(null,inner,inTag);
        frame.invoke(null,false);at(outer.state.from.position,21,2,3);at(inner.state.to.position,21,2,3);at(outer.state.to.position,100,0,0);at(inner.state.from.position,100,0,0);
        check(Native.calls==1,"one interpolation per ship, shared by both portal endpoints");
        check(outer.animation.clientLastFramePortalState==null,"new portal starts with static crossing fallback");
        Crossing.teleportationCounter++;Utils.ships.ship.previous=new Transform(new Matrix(30,2,true));Utils.ships.ship.current=new Transform(new Matrix(50,2,true));
        frame.invoke(null,false);at(outer.state.from.position,46,4,-2);at(inner.state.to.position,46,4,-2);
        check(outer.state.from.width==2&&outer.state.from.height==4,"scaled doorway geometry");
        at(outer.state.from.orientation.right,0,0,-1);at(outer.state.from.orientation.up,0,1,0);
        check(outer.animation.clientLastFramePortalStateCounter==1&&outer.animation.clientCurrentFramePortalStateCounter==2,"IP counter-aligned frame history");
        at(((State)outer.animation.lastTickAnimatedState).from.position,36,4,-2);at(((State)outer.animation.thisTickAnimatedState).from.position,56,4,-2);
        Utils.ships.ship.render=new Transform(new Matrix(60,2,true));render.invoke(null,Utils.ships);at(outer.state.from.position,66,4,-2);at(inner.state.to.position,66,4,-2);
        outer.animation.driver=true;outer.animation.lastTickAnimatedState="native animation";int updates=outer.updates;Crossing.teleportationCounter++;frame.invoke(null,false);check(outer.updates==updates&&outer.animation.lastTickAnimatedState.equals("native animation"),"existing animation and history left alone");outer.animation.driver=false;
        Utils.ships.ship.provider=new Object();updates=outer.updates;int calls=Native.calls;Crossing.teleportationCounter++;frame.invoke(null,false);render.invoke(null,Utils.ships);
        check(outer.updates==updates&&Native.calls==calls,"custom transform provider uses original portal behavior without double sampling");Utils.ships.ship.provider=null;
        Crossing.teleportationCounter++;frame.invoke(null,false);
        Utils.ships.ship=null;Crossing.teleportationCounter++;frame.invoke(null,false);check(outer.animation.clientLastFramePortalState==null,"unloaded ship clears crossing history");
        read.invoke(null,inner,new Tag());Utils.ships.ship=new Ship();int innerUpdates=inner.updates;frame.invoke(null,false);check(inner.updates==innerUpdates,"detached portal no longer follows ship");
        read.invoke(null,inner,inTag);
        var velocity=helper.getMethod("pointVelocity",Object.class,Object.class,Object.class);
        Velocity moving=new Velocity(new Vec(.5,.02,-.2),new Vec(.5,.02,-.2));
        Player local=(Player)Client.client.field_1724;
        Velocity deck=(Velocity)velocity.invoke(null,inner,local,moving);at(deck.thisSidePointVelocity(),0,0,0);at(deck.otherSidePointVelocity(),0,0,0);
        deck=(Velocity)velocity.invoke(null,outer,local,moving);at(deck.thisSidePointVelocity(),0,0,0);at(deck.otherSidePointVelocity(),0,0,0);
        check(velocity.invoke(null,inner,new Player(),moving)==moving,"other entities retain native portal velocity");
        Portal ordinary=new Portal(inner.state);check(velocity.invoke(null,ordinary,local,moving)==moving,"ordinary portals retain native motion transfer");
        Ship loaded=Utils.ships.ship;Utils.ships.ship=null;check(velocity.invoke(null,inner,local,moving)==moving,"missing ship retains native velocity");Utils.ships.ship=loaded;
        var crossed=helper.getMethod("crossed",Object.class);crossed.invoke(null,new Teleportation(inner));
        check(Crossing.cooldown==5,"ship arrival uses bounded native teleport settling interval");
        Player arrived=(Player)Client.client.field_1724;check(Long.valueOf(42).equals(arrived.drag.ship)&&arrived.drag.ticks==0,"arrival resumes native ship dragging");
        check(!arrived.drag.impulse,"deck-relative arrival does not receive a second boarding impulse");
        check(outer.animation.clientLastFramePortalState==null,"crossing clears pre-transfer portal history");
        arrived.drag.ship=null;crossed.invoke(null,new Teleportation(outer));check(arrived.drag.ship==null,"interior arrival is not attached to exterior ship");
        Crossing.cooldown=0;crossed.invoke(null,new Teleportation(new Portal(inner.state)));check(Crossing.cooldown==0,"ordinary portals keep native crossing behavior");
        helper.getMethod("cleanupClient").invoke(null);updates=outer.updates;frame.invoke(null,false);check(outer.updates==updates,"disconnect cleanup clears registrations");
        System.out.println("PASS: paired endpoint attachment, fast movement, rotation/scale, frame/tick history, unload, detach and animation isolation");
        if(args.length>1)contracts(args[1],args[2],args[3]);
    }
}
