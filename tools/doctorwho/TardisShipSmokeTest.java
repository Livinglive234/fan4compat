import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Exercise the actual optional-mod helper, replacing only class names with fixtures. */
public class TardisShipSmokeTest implements Opcodes {
    public static class Vec { public final double field_1352,field_1351,field_1350; public Vec(double x,double y,double z){field_1352=x;field_1351=y;field_1350=z;} }
    public static class Pos {
        final int x,y,z;public Pos(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
        public int method_10263(){return x;}public int method_10264(){return y;}public int method_10260(){return z;}
        public int method_19455(Pos p){return Math.abs(x-p.x)+Math.abs(y-p.y)+Math.abs(z-p.z);}
        public Pos method_10084(){return new Pos(x,y+1,z);}
        public long method_10063(){return ((long)x&0x3ffffff)<<38|((long)z&0x3ffffff)<<12|y&0xfff;}
        public static Pos method_10092(long n){return new Pos((int)(n>>38),(int)(n<<52>>52),(int)(n<<26>>38));}
    }
    public enum Dir { NORTH,SOUTH,EAST,WEST,UP;public static Dir method_10168(String n){return valueOf(n.toUpperCase());} }
    public enum Shape { field_17558 }
    public enum Fluid { field_1348 }
    public enum Kind { BLOCK,MISS }
    public static class Context { public final Vec start,end; public Context(Vec start,Vec end,Shape shape,Fluid fluid,Player player){this.start=start;this.end=end;} }
    public static class Key { public String method_29177(){return "minecraft:overworld";} }
    public static class ExteriorBlock {public static final Object OPEN=new Object(),FACING=new Object();public final ExteriorType exteriorType=new ExteriorType();}
    public static class ExteriorType {public double entranceWidth=1;}
    public static class BlockState {public boolean solid,open;public Dir facing=Dir.SOUTH;public final ExteriorBlock block=new ExteriorBlock();BlockState(boolean s){solid=s;}public Comparable<?> method_11654(Object property){return property==ExteriorBlock.OPEN?open:facing;}public ExteriorBlock method_26204(){return block;}}
    public static class World { public boolean field_9236;public Iterable<?> source;public List<Object> collisions=new ArrayList<>();public Iterable<?> method_20812(Player p,Box b){return source==null?collisions:source;}public BlockState method_8320(Pos p){return new BlockState(p.x==-2866585&&p.y==130&&p.z==9999999);}final Key key=new Key(); public Key method_27983(){return key;}public int method_31607(){return -64;}public int method_31600(){return 320;}public Server method_8503(){return new Server(this);} }
    public static class Server {final World world;Server(World w){world=w;}public World method_3847(Key k){return world;} }
    public static class Player {public Vec position=new Vec(10,71,20);public Box method_5829(){return new Box(position.field_1352-.3,position.field_1351,position.field_1350-.3,position.field_1352+.3,position.field_1351+1.8,position.field_1350+.3);}Text message;public void method_7353(Text t,boolean overlay){message=t;}public Vec method_19538(){return position;} }
    public static class Hit {final Pos support;Hit(Pos p){support=p;}public Kind method_17783(){return support==null?Kind.MISS:Kind.BLOCK;}public Pos method_17777(){return support;}public Dir method_17780(){return Dir.UP;} }
    public static class Matrix {
        double tx=-9999989,ty=-60,tz=-2866565; boolean inverse;
        public double m00(){return 0;}public double m01(){return 0;}public double m02(){return inverse?1:-1;}public double m03(){return 0;}
        public double m10(){return 0;}public double m11(){return 1;}public double m12(){return 0;}public double m13(){return 0;}
        public double m20(){return inverse?-1:1;}public double m21(){return 0;}public double m22(){return 0;}public double m23(){return 0;}
        public double m30(){return tx;}public double m31(){return ty;}public double m32(){return tz;}public double m33(){return 1;}
    }
    public static class Ship { final Matrix matrix=new Matrix(),inverse=new Matrix();Ship(){inverse.inverse=true;}public Matrix getShipToWorld(){return matrix;}public Matrix getWorldToShip(){inverse.tx=matrix.tz;inverse.ty=-matrix.ty;inverse.tz=-matrix.tx;return inverse;}public long getId(){return 42;} }
    public static class Utils {static Ship ship;public static Iterable<Ship> getShipsIntersecting(World w,Box b){return ship==null?List.of():List.of(ship);}public static Ship getShipManagingPos(World w,Pos p){return ship!=null && p.z>9000000?ship:null;} }
    public static class Box {public double field_1323,field_1322,field_1321,field_1320,field_1325,field_1324;public Box(double x1,double y1,double z1,double x2,double y2,double z2){field_1323=x1;field_1322=y1;field_1321=z1;field_1320=x2;field_1325=y2;field_1324=z2;}}
    public static class CollisionShape {final Box box;CollisionShape excluded;boolean contains(double x,double y,double z){return x>=box.field_1323&&x<=box.field_1320&&y>=box.field_1322&&y<=box.field_1325&&z>=box.field_1321&&z<=box.field_1324&&(excluded==null||!excluded.contains(x,y,z));}CollisionShape(Box b){box=b;}public List<Box> method_1090(){
            if(excluded==null)return List.of(box);
            Box cut=excluded.box;double x1=Math.max(box.field_1323,cut.field_1323),y1=Math.max(box.field_1322,cut.field_1322),z1=Math.max(box.field_1321,cut.field_1321),x2=Math.min(box.field_1320,cut.field_1320),y2=Math.min(box.field_1325,cut.field_1325),z2=Math.min(box.field_1324,cut.field_1324);
            if(x1>=x2||y1>=y2||z1>=z2)return List.of(box);
            List<Box> result=new ArrayList<>();
            addBox(result,box.field_1323,box.field_1322,box.field_1321,x1,box.field_1325,box.field_1324);addBox(result,x2,box.field_1322,box.field_1321,box.field_1320,box.field_1325,box.field_1324);
            addBox(result,x1,box.field_1322,box.field_1321,x2,y1,box.field_1324);addBox(result,x1,y2,box.field_1321,x2,box.field_1325,box.field_1324);
            addBox(result,x1,y1,box.field_1321,x2,y2,z1);addBox(result,x1,y1,z2,x2,y2,box.field_1324);return result;
        }
        private static void addBox(List<Box> result,double a,double b,double c,double d,double e,double f){if(a<d&&b<e&&c<f)result.add(new Box(a,b,c,d,e,f));}public boolean method_1110(){return false;}public Box method_1107(){return box;}}
    public static class BooleanFunction {public static final Object field_16886=new Object();}
    public static class Shapes {public static CollisionShape method_1081(double a,double b,double c,double d,double e,double f){return new CollisionShape(new Box(a,b,c,d,e,f));}public static CollisionShape method_1072(CollisionShape original,CollisionShape cut,Object function){check(function==BooleanFunction.field_16886,"only subtract the doorway");CollisionShape result=new CollisionShape(original.box);result.excluded=cut;return result;}}
    public enum Formatting { field_1054 }
    public static class Text {String value;Text(String v){value=v;}public static Text method_43470(String v){return new Text(v);}public static Text method_43471(String v){return new Text(v);}public Text method_27692(Formatting f){return this;}public Text method_27693(String v){value+=v;return this;}public String toString(){return value;}}
    public static class Texts {public static java.util.function.Function<List<Object>,Object> SONIC_DEVICE_TARDIS_RELOCATED=coords->new Text("Destination: "+coords.get(0)+", "+coords.get(1)+", "+coords.get(2));}
    public static class Blocks {public static boolean checkBlockIsSolid(BlockState s){return s.solid;}}
    public static class Rays {static Pos support;public static Hit clipIncludeShips(World w,Context c,boolean transformed,Long skip,boolean skipWorld){check(!transformed,"preserve ship-local hit");if(!skipWorld)check(c.start.field_1351>71&&c.end.field_1351<71,"recall scans below feet");else check(Math.abs(c.end.field_1351-c.start.field_1351)>1,"coordinate scan spans world height");return new Hit(support);} }
    public enum Scanning { TOP,BOTTOM,DIRECT,NONE }
    public record LandingSpot(Pos pos,Dir facing) {}
    public static class Materialization {public State tardis;Scanning scan=Scanning.TOP;boolean safe=true;int validations;public void setVerticalScanning(Scanning s){scan=s;}public LandingSpot findLandingSpot(World w,Pos p,Dir dir,Scanning scanning){check(scanning==Scanning.DIRECT,"native clearance validation must be direct");validations++;return safe?new LandingSpot(p,dir):null;}}
    public static class Entry {public float entranceWidth=1,entranceHeight=2;}
    public static class State {
        final World world=new World();final Materialization mat=new Materialization();Pos curr=new Pos(-2866585,130,9999999),dest=curr;Dir destFace=Dir.EAST;boolean flyover=true;int consoleUpdates;
        State(){mat.tardis=this;}
        public World getWorld(){return world;}public World getExteriorWorld(){return world;}public World getDestinationExteriorWorld(){return world;}public Key getPreviousExteriorDimension(){return world.key;}
        public Pos getCurrentExteriorPosition(){return curr;}public Pos getPreviousExteriorPosition(){return curr;}public Pos getDestinationExteriorPosition(){return dest;}
        public Dir getCurrentExteriorFacing(){return Dir.SOUTH;}public Dir getPreviousExteriorFacing(){return Dir.SOUTH;}public Dir getDestinationExteriorFacing(){return destFace;}
        public void setDestinationPosition(Pos p){dest=p;}public void setDestinationFacing(Dir d){destFace=d;}
        public Object getSystem(Class<?> c){return mat;}public Entry getExteriorType(){return new Entry();}public boolean isFlyoverEnabled(){return flyover;}
        public void markConsoleTilesUpdated(){consoleUpdates++;}
    }
    public static class Tag {
        final Map<String,Object> values=new HashMap<>();public void method_10544(String k,long v){values.put(k,v);}public void method_10582(String k,String v){values.put(k,v);}
        public long method_10537(String k){return ((Number)values.getOrDefault(k,0L)).longValue();}public String method_10558(String k){return (String)values.getOrDefault(k,"");}public boolean method_10545(String k){return values.containsKey(k);}
    }
    public static class Portal {
        static int nextId;int entityId=++nextId;
        public boolean equals(Object other){return other instanceof Portal p&&entityId==p.entityId;}
        public int hashCode(){return entityId;}
        Vec origin,dest,w,h;double width,height,scale;int updates;World world;boolean removed,allowed=true,animation=true;
        public World method_37908(){return world;}public boolean method_31481(){return removed;}public boolean isPortalValid(){return true;}public boolean isTeleportable(){return allowed;}public boolean canTeleportEntity(Player p){return allowed;}public void disableDefaultAnimation(){animation=false;updates++;}
        public Vec getNormal(){return new Vec(1,0,0);}public Vec getOriginPos(){return origin;}public void setOriginPos(Vec v){origin=v;}public void setDestination(Vec v){dest=v;}
        public void setOrientationAndSize(Vec w,Vec h,double width,double height){this.w=w;this.h=h;this.width=width;this.height=height;}
        public void setScaling(double s){scale=s;}public void reloadAndSyncToClientNextTick(){updates++;}
    }
    public static class PortalState {private final State tardis;private final Portal portalFromTardis=new Portal(),portalToTardis=new Portal();PortalState(State t){tardis=t;portalFromTardis.origin=new Vec(0,1,0);portalToTardis.world=t.world;portalFromTardis.world=new World();} }
    public static class Manipulation {public static void adjustRotationToConnect(Portal a,Portal b){at(a.dest,b.origin.field_1352,b.origin.field_1351,b.origin.field_1350);at(b.dest,a.origin.field_1352,a.origin.field_1351,a.origin.field_1350);} }
    public interface Original {Object call(Object... args);}
    public static class Settings {boolean dynamic;public Settings method_9624(){dynamic=true;return this;}}
    static Class<?> generatedHandler(String mixin,String method,String name,String superclass,Map<String,String> types,ClassLoader parent)throws Exception {
        ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/doctorwho/common",mixin+".class"))).accept(node,0);
        types=new HashMap<>(types);types.put(node.name,name);node.superName=superclass;
        node.methods.removeIf(m->!m.name.equals(method));node.methods.forEach(m->m.access=(m.access&~ACC_PRIVATE)|ACC_PUBLIC);
        ClassWriter writer=new ClassWriter(0);final Map<String,String> mapping=types;node.accept(new ClassRemapper(writer,new Remapper(){public String map(String n){return mapping.getOrDefault(n,n);}}));
        if(!superclass.equals("java/lang/Object")) {MethodVisitor ctor=writer.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,superclass,"<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();}
        byte[] bytes=writer.toByteArray();return new ClassLoader(parent){Class<?> define(){return defineClass(name,bytes,0,bytes.length);}}.define();
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void at(Vec p,double x,double y,double z){check(Math.abs(p.field_1352-x)<1e-6&&Math.abs(p.field_1351-y)<1e-6&&Math.abs(p.field_1350-z)<1e-6,"portal pose");}
    public static void main(String[] args)throws Exception {
        String[][] names={{"net.minecraft.class_2561","Text"},{"net.minecraft.class_124","Formatting"},{"net.drgmes.dwm.DWM$TEXTS","Texts"},{"net.minecraft.class_238","Box"},{"net.drgmes.dwm.utils.helpers.WorldHelper","Blocks"},{"net.minecraft.class_243","Vec"},{"net.minecraft.class_2338","Pos"},{"net.minecraft.class_2350","Dir"},{"net.minecraft.class_1657","Player"},{"net.minecraft.class_1937","World"},{"net.minecraft.class_3959","Context"},{"net.minecraft.class_3959$class_3960","Shape"},{"net.minecraft.class_3959$class_242","Fluid"},{"org.valkyrienskies.mod.common.VSGameUtilsKt","Utils"},{"org.valkyrienskies.mod.common.world.RaycastUtilsKt","Rays"},{"net.drgmes.dwm.common.tardis.systems.TardisSystemMaterialization","Materialization"},{"net.drgmes.dwm.enums.TardisVerticalScanning","Scanning"},{"qouteall.imm_ptl.core.portal.PortalManipulation","Manipulation"}};
        Map<String,String> mapping=new HashMap<>();mapping.put("net.minecraft.class_2688","TardisShipSmokeTest$BlockState");mapping.put("net.minecraft.class_4970$class_4971","TardisShipSmokeTest$BlockState");mapping.put("net.minecraft.class_2769","java.lang.Object");mapping.put("net.minecraft.class_2248","TardisShipSmokeTest$ExteriorBlock");mapping.put("net.minecraft.class_1297","TardisShipSmokeTest$Player");mapping.put("net.minecraft.class_259","TardisShipSmokeTest$Shapes");mapping.put("net.minecraft.class_247","TardisShipSmokeTest$BooleanFunction");mapping.put("net.drgmes.dwm.blocks.tardis.exteriors.BaseTardisExteriorBlock","TardisShipSmokeTest$ExteriorBlock");for(String[] p:names)mapping.put(p[0],"TardisShipSmokeTest$"+p[1]);
        String helperName="dev.fan4.compat.doctorwho.TardisShipCompat";
        ClassLoader loader=new ClassLoader(TardisShipSmokeTest.class.getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(!name.equals(helperName)&&!name.startsWith(helperName+"$")) return super.loadClass(name,resolve);
                Class<?> loaded=findLoadedClass(name);if(loaded!=null)return loaded;
                try {
                    ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],name.replace('.','/')+".class"))).accept(new ClassRemapper(node,new Remapper(){public Object mapValue(Object value){return value instanceof String s?mapping.getOrDefault(s,s):super.mapValue(value);}}),0);
                    ClassWriter writer=new ClassWriter(0);node.accept(writer);byte[] bytes=writer.toByteArray();
                    Class<?> result=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(result);return result;
                } catch(java.io.IOException e) {throw new ClassNotFoundException(name,e);}
            }
        };
        Class<?> helper=loader.loadClass(helperName);
        var recall=helper.getMethod("prepareRecall",Object.class,Object.class,Object.class);var invalid=helper.getMethod("invalidRecall",Object.class,Object.class,Object.class);var portals=helper.getMethod("updatePortals",Object.class);var write=helper.getMethod("write",Object.class,Object.class);
        Utils.ship=new Ship();State state=new State();Rays.support=state.curr;
        recall.invoke(null,state,new Player(),state.world);
        long target=state.dest.method_10063();check(state.dest.y==131&&state.destFace==Dir.SOUTH&&state.mat.scan==Scanning.TOP,"recall target and facing local to ship");
        // Simulate another mod returning a MISS instead of the deck hit.
        Rays.support=null;State missedRay=new State();recall.invoke(null,missedRay,new Player(),missedRay.world);
        check(missedRay.dest.method_10063()==target&&missedRay.mat.scan==Scanning.TOP,"direct deck lookup rescues missed raycast");
        // Also handle a raycast that incorrectly reports ordinary terrain.
        Rays.support=new Pos(10,60,20);State terrainRay=new State();recall.invoke(null,terrainRay,new Player(),terrainRay.world);
        check(terrainRay.dest.method_10063()==target,"deck lookup rescues terrain-only raycast");
        Utils.ship.matrix.tx+=100;Utils.ship.matrix.tz-=80;
        check(state.dest.method_10063()==target,"moving ship retains exact local target");
        check(!(Boolean)invalid.invoke(null,state.mat,state.world,state.dest),"original ship still owns destination");
        Tag tag=new Tag();tag.method_10544("currExteriorPosition",state.curr.method_10063());write.invoke(null,state,tag);
        check(tag.method_10537("currExteriorPosition")==state.curr.method_10063(),"persistence keeps raw block address");
        Pos display=Pos.method_10092(tag.method_10537("fan4compatWorld_currPosition"));check(display.x==110&&display.y==70&&display.z==-61,"console world coordinates");
        check(tag.method_10558("fan4compatWorld_currFacing").equals("east"),"console world facing");
        PortalState ps=new PortalState(state);portals.invoke(null,ps);at(ps.portalToTardis.origin,111,71,-60.5);check(ps.portalToTardis.w.field_1350==-1&&ps.portalToTardis.h.field_1351==1,"portal axes rotate");
        int updates=ps.portalToTardis.updates;portals.invoke(null,ps);check(ps.portalToTardis.updates==updates,"stationary portals don't spam updates");
        Utils.ship.matrix.tx+=4;portals.invoke(null,ps);at(ps.portalToTardis.origin,115,71,-60.5);check(ps.portalToTardis.updates==updates+1,"portal follows ship each tick");
        check(!ps.portalFromTardis.animation&&!ps.portalToTardis.animation,"ship portals disable trailing default animation");
        CollisionShape lower=new CollisionShape(new Box(-2866585,130,9999999,-2866584,131,10000000));
        CollisionShape upper=new CollisionShape(new Box(-2866585,131,9999999,-2866584,132,10000000));
        CollisionShape deck=new CollisionShape(new Box(-2866585,129,9999999,-2866584,130,10000000));
        CollisionShape neighbor=new CollisionShape(new Box(-2866584,130,9999999,-2866583,131,10000000));
        state.world.collisions.addAll(List.of(lower,upper,deck,neighbor));
        var collisions=helper.getMethod("shipCollisions",Object.class,Object.class,Object.class);
        Object query=new Box(-2866586,129,9999998,-2866583,133,10000001);Player player=new Player();
        check(collisions.invoke(null,state.world,player,query)==state.world.collisions,"source-clipped walls are not discarded by the VS query bridge");
        var sourceShape=helper.getMethod("openExteriorShape",Object.class,Object.class,Object.class);
        BlockState openState=new BlockState(true);openState.open=true;
        check((Boolean)sourceShape.invoke(null,openState,state.world,state.curr),"open attached shell lower shape is eligible for doorway clipping");
        check((Boolean)sourceShape.invoke(null,openState,state.world,state.curr.method_10084()),"open attached shell upper shape is eligible for doorway clipping");
        check(!(Boolean)sourceShape.invoke(null,openState,state.world,new Pos(state.curr.x,state.curr.y-1,state.curr.z)),"deck collision retained at source");
        check(!(Boolean)sourceShape.invoke(null,openState,new World(),state.curr),"foreign world retains shell");
        openState.open=false;check(!(Boolean)sourceShape.invoke(null,openState,state.world,state.curr),"closed doors retain shell shape");openState.open=true;
        ps.portalToTardis.allowed=false;check(!(Boolean)sourceShape.invoke(null,openState,state.world,state.curr),"nonteleportable portal retains shell shape");ps.portalToTardis.allowed=true;
        var cut=helper.getMethod("doorwayCut",dev.fan4.compat.shared.ShipPose.Point.class,double.class);
        for(var face:List.of(new dev.fan4.compat.shared.ShipPose.Point(1,0,0),new dev.fan4.compat.shared.ShipPose.Point(-1,0,0),new dev.fan4.compat.shared.ShipPose.Point(0,0,1),new dev.fan4.compat.shared.ShipPose.Point(0,0,-1))) {
            double[] box=(double[])cut.invoke(null,face,1d);
            boolean x=face.x()!=0;int axis=x?0:2,side=x?2:0;double sign=x?face.x():face.z();
            check(box[axis]==(sign>0?.5:0)&&box[axis+3]==(sign>0?1:.5),"opening cuts only the facing half; back remains solid");
            check(box[side]==.0625&&box[side+3]==.9375,"both side walls remain solid");
        }
        var sourceClip=helper.getMethod("doorwayShape",Object.class,Object.class,Object.class,Object.class);
        CollisionShape solid=Shapes.method_1081(0,0,0,1,1,1);
        for(Dir facing:List.of(Dir.NORTH,Dir.SOUTH,Dir.EAST,Dir.WEST)) {
            openState.facing=facing;
            CollisionShape clipped=(CollisionShape)sourceClip.invoke(null,openState,state.world,state.curr,solid);
            double fx=facing==Dir.EAST?1:facing==Dir.WEST?-1:0,fz=facing==Dir.SOUTH?1:facing==Dir.NORTH?-1:0;
            check(!clipped.contains(.5+fx*.49,.5,.5+fz*.49),"door face permits entry and targeting");
            check(clipped.contains(.5-fx*.49,.5,.5-fz*.49),"opposite face stays hard");
            check(clipped.contains(.5+fz*.49,.5,.5+fx*.49)&&clipped.contains(.5-fz*.49,.5,.5-fx*.49),"both side faces stay hard");
        }
        openState.open=false;check(sourceClip.invoke(null,openState,state.world,state.curr,solid)==solid,"closed shell is unchanged");openState.open=true;
        state.world.source=null;
        ps.portalToTardis.removed=true;check(collisions.invoke(null,state.world,player,query)==state.world.collisions,"closed/discarded portal keeps collisions");ps.portalToTardis.removed=false;
        World other=new World();other.collisions.addAll(state.world.collisions);check(collisions.invoke(null,other,player,query)==other.collisions,"other worlds keep collisions");
        check(collisions.invoke(null,state.world,null,query)==state.world.collisions,"non-entity collision queries remain intact");
        Tag portalTag=new Tag();helper.getMethod("writeExteriorPortal",Object.class,Object.class).invoke(null,ps.portalToTardis,portalTag);
        World clientWorld=new World();Portal clientPortal=new Portal();clientPortal.entityId=ps.portalToTardis.entityId;clientPortal.world=clientWorld;
        helper.getMethod("readExteriorPortal",Object.class,Object.class).invoke(null,clientPortal,portalTag);
        check((Boolean)sourceShape.invoke(null,openState,clientWorld,state.curr),"equal-ID client portal opens client doorway");
        check((Boolean)sourceShape.invoke(null,openState,state.world,state.curr),"equal-ID server portal still opens server doorway");
        helper.getMethod("readExteriorPortal",Object.class,Object.class).invoke(null,clientPortal,new Tag());
        check((Boolean)sourceShape.invoke(null,openState,state.world,state.curr),"removing client metadata preserves server doorway");
        ps.portalToTardis.removed=true;Portal synced=new Portal();synced.world=state.world;helper.getMethod("readExteriorPortal",Object.class,Object.class).invoke(null,synced,portalTag);
        check(collisions.invoke(null,state.world,player,query)==state.world.collisions,"synced client metadata preserves source-clipped walls");
        helper.getMethod("readExteriorPortal",Object.class,Object.class).invoke(null,synced,new Tag());check(collisions.invoke(null,state.world,player,query)==state.world.collisions,"ordinary portals do not erase ship collisions");
        var sonic=helper.getMethod("sonicMessage",Object.class,Object.class,boolean.class,Object.class,Object.class);
        Text original=new Text("raw coordinates");sonic.invoke(null,player,original,true,state.world,new Hit(state.curr));
        check(player.message.value.equals("Destination: 114, 71, -61 (On ship)"),"sonic displays transformed destination with ship marker");
        check(state.curr.method_10063()==tag.method_10537("currExteriorPosition"),"sonic display does not modify raw block addresses");
        sonic.invoke(null,player,original,true,state.world,new Hit(new Pos(0,70,0)));check(player.message==original,"terrain sonic message unchanged");
        check(!(Boolean)helper.getMethod("flyoverEnabled",Object.class).invoke(null,state),"avoid raw shipyard flyover animation");
        State loaded=new State();helper.getMethod("read",Object.class,Object.class).invoke(null,loaded,tag);loaded.dest=state.dest;
        var coordinateScan=helper.getMethod("scanShipLanding",Object.class,Object.class,Object.class,Object.class,Object.class,Object.class);
        Rays.support=state.curr;
        LandingSpot ground=new LandingSpot(new Pos(114,10,-61),Dir.EAST);
        Map<String,String> generatedTypes=new HashMap<>();mapping.forEach((k,v)->generatedTypes.put(k.replace('.','/'),v.replace('.','/')));generatedTypes.put("net/minecraft/class_3218","TardisShipSmokeTest$World");
        generatedTypes.put("com/llamalad7/mixinextras/injector/wrapoperation/Operation","TardisShipSmokeTest$Original");generatedTypes.put("net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization$LandingSpot","TardisShipSmokeTest$LandingSpot");
        Class<?> wrapper=generatedHandler("TardisShipCoordinateScanMixin","fan4$scanShipSurface","GeneratedScan","TardisShipSmokeTest$Materialization",generatedTypes,loader);
        Materialization wrapperInstance=(Materialization)wrapper.getConstructor().newInstance();wrapperInstance.tardis=state;
        Pos originalRequest=new Pos(114,200,-61);int[] originalCalls={0};
        Object wrappedLanding=wrapper.getMethod("fan4$scanShipSurface",World.class,Pos.class,Dir.class,Scanning.class,Original.class).invoke(wrapperInstance,state.world,originalRequest,Dir.EAST,Scanning.BOTTOM,(Original)callArgs->{
            check(callArgs[1]==originalRequest,"forward the original scan position");originalCalls[0]++;callArgs[1]=ground.pos();return ground;
        });
        check(originalCalls[0]==1&&wrappedLanding instanceof LandingSpot&&((LandingSpot)wrappedLanding).pos().y==131,"generated wrapper keeps high-Y request when native scan advances to ground");
        generatedTypes.put("net/minecraft/class_4970$class_2251","TardisShipSmokeTest$Settings");
        Class<?> dynamic=generatedHandler("TardisShipExteriorShapeMixin","fan4$liveDoorwayShapes","GeneratedDynamic","java/lang/Object",generatedTypes,loader);
        Settings settings=new Settings();check(dynamic.getMethod("fan4$liveDoorwayShapes",Settings.class).invoke(null,settings)==settings&&settings.dynamic,"shell settings disable cached collision shapes");
        Object onDeck=coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.BOTTOM,ground);
        check(onDeck instanceof LandingSpot&&((LandingSpot)onDeck).pos().y==131&&((LandingSpot)onDeck).pos().z>9000000,"high coordinate scan selects ship over ground and preserves local address");
        check(((LandingSpot)onDeck).facing()==Dir.SOUTH,"coordinate scan converts facing into ship frame");
        LandingSpot closerTerrain=new LandingSpot(new Pos(114,100,-61),Dir.EAST);
        check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.BOTTOM,closerTerrain)==closerTerrain,"closer terrain wins downward scan");
        check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,20,-61),Dir.EAST,Scanning.TOP,null) instanceof LandingSpot,"upward scan finds deck above request");
        check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.TOP,null)==null,"upward scan cannot choose ship below request");
        state.mat.safe=false;check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.BOTTOM,ground)==ground,"blocked native clearance falls back to terrain");state.mat.safe=true;
        int validations=state.mat.validations;
        check(coordinateScan.invoke(null,state.mat,state.world,state.curr,Dir.EAST,Scanning.BOTTOM,ground)==ground,"already-local destinations retain native behavior");
        check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.DIRECT,ground)==ground,"DIRECT does not recursively scan");
        check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.NONE,ground)==ground,"NONE does not scan ships");
        check(validations==state.mat.validations,"local and non-scanning destinations avoid ray validation");
        Rays.support=null;check(coordinateScan.invoke(null,state.mat,state.world,new Pos(114,200,-61),Dir.EAST,Scanning.BOTTOM,ground)==ground,"missed ship ray retains terrain");
        Utils.ship=null;check((Boolean)invalid.invoke(null,loaded.mat,loaded.world,loaded.dest),"saved recall rejects removed ship");
        State terrain=new State();Rays.support=new Pos(0,70,0);terrain.dest=new Pos(0,71,0);recall.invoke(null,terrain,new Player(),terrain.world);check(terrain.mat.scan==Scanning.TOP&&terrain.dest.x==0,"terrain recall remains unchanged");
        if(args.length>3) {
            VerifyAddon.readJar(args[1]);VerifyAddon.readJar(args[2]);VerifyAddon.readJar(args[3]);
            for(String mixin:List.of("TardisShipCoordinateScanMixin","TardisShipExteriorShapeMixin","TardisShipKeyRangeMixin")) {
                ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/doctorwho/common",mixin+".class"))).accept(c,0);VerifyAddon.injectionTargets(c);
            }
            check(VerifyAddon.exists("qouteall/imm_ptl/core/portal/Portal","isTeleportable","()Z",new HashSet<>()),"source shell guard matches IP API");
        }
        System.out.println("PASS: recall attachment, moving/rotating portal pair, world-coordinate display, raw-address preservation, removed-ship rejection, persistence and terrain fallback");
    }
}
