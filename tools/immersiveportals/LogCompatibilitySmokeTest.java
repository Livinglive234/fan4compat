import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import java.util.zip.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import dev.fan4.compat.bclib.RecipeDataCompat;
import dev.fan4.compat.iris.ShaderCompileCompat;
import dev.fan4.compat.shared.ShipPose.Point;

/** Reproduce log failures at optional mod boundaries without booting Minecraft. */
public class LogCompatibilitySmokeTest {
    public static class Json {
        final Map<String,Json> values=new LinkedHashMap<>();String text;
        public Json(){}public Json(String s){text=s;}
        public boolean isJsonObject(){return text==null;}public Json getAsJsonObject(){return this;}
        public String getAsString(){return text;}public boolean has(String k){return values.containsKey(k);}
        public Json get(String k){return values.get(k);}public Json remove(String k){return values.remove(k);}
        public String toString(){return text==null?values.toString():"\""+text+"\"";}
        public void add(String k,Json v){values.put(k,v);}Json put(String k,Json v){add(k,v);return this;}
    }
    public static class World {
        public boolean field_9236;final Map<Integer,Object> entities=new HashMap<>();String dimension="overworld";
        public Object method_8469(int id){return entities.get(id);}public String method_27983(){return dimension;}
        public int method_31607(){return -64;}public int method_31605(){return 384;}
        public ChunkManager method_2935(){return chunks;}final ChunkManager chunks=new ChunkManager();
    }
    public static class Player {
        public Handler field_13987;World world;Player(World w){world=w;field_13987=new Handler(this);}
        public World method_37908(){return world;}public Vec method_19538(){return new Vec(0,64,0);}
    }
    public static class Handler {
        public Player field_14140;public Object field_14119,ip_dimOfAwaitingPosition;
        Handler(Player p){field_14140=p;}
    }
    public static class PositionPacket {
        Object dimension;public Object ip_getPlayerDimension(){return dimension;}public void ip_setPlayerDimension(Object d){dimension=d;}
    }
    public static class Worlds {
        static List<World> worlds=new ArrayList<>();static boolean initialized=true;
        public static boolean getIsInitialized(){return initialized;}public static List<World> getClientWorlds(){return worlds;}
    }
    public record Packet(int entityId,String payload) {
        static final List<String> applied=new ArrayList<>();
        public static void handlePacket(Packet p,Player player){applied.add(p.payload);}
    }
    public static class Client {
        public Player field_1724;static Client instance=new Client();public ModelLoader models=new ModelLoader();
        public static Client method_1551(){return instance;}public ModelLoader method_31974(){return models;}
    }
    public static class Part {
        public boolean field_3665=true,field_38456=false;int transform;List<Part> children=new ArrayList<>();
        public void method_41923(){transform=0;}
        public Stream<Part> method_32088(){return Stream.concat(Stream.of(this),children.stream().flatMap(Part::method_32088));}
    }
    public static class ModelLoader {
        public Map<Object,Object> field_27542=new HashMap<>();int bakes;
        public Part method_32072(Object layer){bakes++;Part p=new Part();p.children.add(new Part());return p;}
    }
    public static class Shader {
        public static final ThreadLocal<Object> ip_programType=new ThreadLocal<>(),ip_programName=new ThreadLocal<>();
    }
    public static class Vec {
        public final double field_1352,field_1351,field_1350;
        public Vec(double x,double y,double z){field_1352=x;field_1351=y;field_1350=z;}
        public double method_1025(Vec v){return Math.pow(field_1352-v.field_1352,2)+Math.pow(field_1351-v.field_1351,2)+Math.pow(field_1350-v.field_1350,2);}
    }
    public record Pos(int x,int y,int z) {public int method_10263(){return x;}public int method_10264(){return y;}public int method_10260(){return z;}}
    public static class ChunkPos {
        public final int field_9181,field_9180;public ChunkPos(int x,int z){field_9181=x;field_9180=z;}
        public boolean equals(Object o){return o instanceof ChunkPos p&&p.field_9181==field_9181&&p.field_9180==field_9180;}public int hashCode(){return Objects.hash(field_9181,field_9180);}
    }
    public record Bounds(double x1,double y1,double z1,double x2,double y2,double z2) {}
    public static class Section {}
    public static class Chunk {public Section[] method_12006(){return new Section[]{new Section()};}}
    public static class ChunkManager {
        public Object method_12126(int x,int z,boolean create){check(!create,"snapshot never loads chunks on audio thread");return new Chunk();}
    }
    public static class ProxyWorld {public Map<ChunkPos,Object> clonedLevelChunks=new HashMap<>();public ProxyWorld(){clonedLevelChunks.put(new ChunkPos(6,0),new Object());}}
    public static class ClonedChunk {public ClonedChunk(World world,ChunkPos pos,Section[] sections){}}
    public interface ChunkConsumer {void accept(int x,int z);}
    public static class Chunks {public void forEach(ChunkConsumer c){c.accept(-62500,0);}}
    public static class Ship {
        String dimension="overworld";double worldX=100;
        public long getId(){return 42;}public String getChunkClaimDimension(){return dimension;}public Chunks getActiveChunksSet(){return new Chunks();}
        public PortalMotionSmokeTest.Matrix getShipToWorld(){return new PortalMotionSmokeTest.Matrix(worldX+1000000,1,false);}
        public PortalMotionSmokeTest.Matrix getWorldToShip(){return new PortalMotionSmokeTest.Matrix(-worldX-1000000,1,false);}
    }
    public static class Ships {
        public Ships getAllShips(){return this;}public Ship getById(long id){return id==42?Utils.ship:null;}
    }
    public static class Utils {
        static Ship ship=new Ship();public static Ships getShipObjectWorld(World world){return new Ships();}
        public static String getDimensionId(World w){return w.dimension;}
        static Bounds lastBounds;
        public static boolean isChunkInShipyard(World w,int x,int z){return x<-60000;}
        public static Ship getShipManagingPos(World w,Pos pos){return pos.x()<-999000?ship:null;}
        public static Iterable<Ship> getShipsIntersecting(World w,Bounds bounds){lastBounds=bounds;return List.of(ship);}
    }
    public record ChunkLoader(String dimension,int x,int z,int radius) {}
    public static class Portal {
        World origin,destination;boolean removed,valid=true,allowed=true;double distance=1;
        public World getOriginWorld(){return origin;}public World getDestWorld(){return destination;}
        public boolean method_31481(){return removed;}public boolean isPortalValid(){return valid;}public boolean canTeleportEntity(Player p){return allowed;}
        public double getDistanceToNearestPointInPortal(Vec v){return distance;}
    }
    public enum HitType {BLOCK,MISS}
    public enum Direction {EAST;
        public Pos method_10163(){return new Pos(1,0,0);}public static Direction method_10142(double x,double y,double z){return EAST;}
    }
    public static class Hit {
        Vec position;Direction direction;Pos block;boolean inside;HitType type;
        public Hit(Vec p,Direction d,Pos b,boolean i){position=p;direction=d;block=b;inside=i;type=HitType.BLOCK;}
        public static Hit method_17778(Vec p,Direction d,Pos b){Hit h=new Hit(p,d,b,false);h.type=HitType.MISS;return h;}
        public HitType method_17783(){return type;}public Vec method_17784(){return position;}public Direction method_17780(){return direction;}public Pos method_17777(){return block;}public boolean method_17781(){return inside;}
    }
    public interface OperationAPI {Object call(Object...args);}
    public static class Operation implements OperationAPI {
        final List<Vec> starts=new ArrayList<>();boolean terrain,foreign;Object ignored;
        public Object call(Object...args){
            Vec from=(Vec)args[1],to=(Vec)args[2];check(from.method_1025(to)<=512*512,"no ray may reach IP with a segment longer than 512 blocks");ignored=args[3];starts.add(from);
            if(from.field_1352< -999000){return new Hit(new Vec(-999997,64,0),Direction.EAST,new Pos(foreign?0:-999997,64,0),false);}
            Hit h=new Hit(terrain?new Vec(102,64,0):to,Direction.EAST,new Pos(102,64,0),false);if(!terrain)h.type=HitType.MISS;return h;
        }
    }
    public static class Redirect {
        static World destination;
        public static void withForceRedirect(World w,Runnable body){World old=destination;destination=w;try{body.run();}finally{destination=old;}}
    }
    public static class Tracking {
        static boolean watching;
        public static boolean isPlayerWatchingChunk(Player p,String dimension,int x,int z){return watching;}
    }
    public static class Wrapper {
        Player player;Wrapper(Player p){player=p;}public Player getPlayer(){return player;}public String getDimension(){return player.world.dimension;}
    }
    public static class ChunkMap {public World field_17214;ChunkMap(World w){field_17214=w;}}
    public interface Like {Object get(Object key);Object get(String key);Stream<?> entries();}
    public interface Ops {Result getStringValue(Object key);}
    public record Result(Object value) {public Optional<?> result(){return Optional.ofNullable(value);}}
    public static class Builder {}
    public abstract static class MapCodec {
        public abstract Result decode(Ops ops,Like input);public abstract Builder encode(Object value,Ops ops,Builder output);public abstract Stream<?> keys(Ops ops);
    }
    public static class ItemCodec extends MapCodec {
        Like last;
        public Result decode(Ops ops,Like input){last=input;Object item=input.get("item");if(item==null)throw new IllegalArgumentException("No key item");check(item==input.get((Object)"item"),"string and encoded-key lookup agree");return new Result(item);}
        public Builder encode(Object value,Ops ops,Builder output){return output;}public Stream<?> keys(Ops ops){return Stream.of("item","count","nbt");}
    }
    public static class Input implements Like {
        final Map<String,Object> values=new HashMap<>();public Object get(Object key){return values.get(key);}public Object get(String key){return values.get(key);}public Stream<?> entries(){return values.entrySet().stream();}
    }
    static final Set<String> HELPERS=Set.of("dev.fan4.compat.doctorwho.ModelBakeCompat","dev.fan4.compat.immersiveportals.PositionPacketCompat","dev.fan4.compat.accessories.PortalEntitySyncCompat","dev.fan4.compat.iris.ShaderCompileCompat","dev.fan4.compat.immersiveportals.PortalMotionCompat","dev.fan4.compat.immersiveportals.DoorwayShipLoading","dev.fan4.compat.soundphysics.ShipSoundRaycast","dev.fan4.compat.immersiveportals.ShipChunkPackets","dev.fan4.compat.bclib.StackCodecCompat","dev.fan4.compat.bclib.AliasedStackCodec");
    static final Map<String,String> TYPES=new HashMap<>();
    static {
        Object[][] mappings={
            {"com/mojang/serialization/MapCodec",MapCodec.class},{"com/mojang/serialization/DynamicOps",Ops.class},{"com/mojang/serialization/MapLike",Like.class},{"com/mojang/serialization/DataResult",Result.class},{"com/mojang/serialization/RecordBuilder",Builder.class},
            {"net/minecraft/class_281$class_282",Object.class},{"net/minecraft/class_5913",Object.class},{"com/llamalad7/mixinextras/injector/wrapoperation/Operation",OperationAPI.class},
            {"qouteall/imm_ptl/core/network/PacketRedirection",Redirect.class},{"qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking",Tracking.class},{"net/minecraft/class_3222",Player.class},{"net/minecraft/class_1657",Player.class},
            {"net/minecraft/class_310",Client.class},{"net/minecraft/class_3244",Handler.class},{"net/minecraft/class_2708",PositionPacket.class},{"net/minecraft/class_281",Shader.class},
            {"qouteall/imm_ptl/core/ClientWorldLoader",Worlds.class},{"net/minecraft/class_1937",World.class},{"net/minecraft/class_3218",World.class},
            {"net/minecraft/class_243",Vec.class},{"net/minecraft/class_238",Bounds.class},{"net/minecraft/class_1923",ChunkPos.class},{"net/minecraft/class_2338",Pos.class},
            {"net/minecraft/class_2826",Section.class},{"net/minecraft/class_2350",Direction.class},{"net/minecraft/class_3965",Hit.class},
            {"org/valkyrienskies/mod/common/VSGameUtilsKt",Utils.class},{"org/valkyrienskies/core/api/util/functions/IntBinaryConsumer",ChunkConsumer.class},
            {"com/sonicether/soundphysics/world/ClonedLevelChunk",ClonedChunk.class},{"qouteall/imm_ptl/core/chunk_loading/ChunkLoader",ChunkLoader.class},{"net/minecraft/class_5321",String.class}};
        for(Object[] mapping:mappings)TYPES.put((String)mapping[0],((Class<?>)mapping[1]).getName().replace('.','/'));
    }
    static class Loader extends ClassLoader {
        Loader(){super(LogCompatibilitySmokeTest.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            if(!name.equals("dev.fan4.compat.mixin.iris.client.ShaderCompileScopeMixin")&&HELPERS.stream().noneMatch(s->name.equals(s)||name.startsWith(s+"$")))return super.loadClass(name,resolve);
            Class<?> found=findLoadedClass(name);if(found!=null)return found;
            try {
                Path source=Path.of("build/classes/java/main",name.replace('.','/')+".class");if(!Files.exists(source))source=Path.of("build/generated/classes",name.replace('.','/')+".class");
                ClassWriter out=new ClassWriter(0);new ClassReader(Files.readAllBytes(source)).accept(new ClassRemapper(out,new Remapper(){
                    public String map(String n){return TYPES.getOrDefault(n,n);}
                    public Object mapValue(Object v){if(v instanceof String s){if(s.startsWith("[L"))return mapDesc(s.replace('.','/')).replace('/','.');if(TYPES.containsKey(s.replace('.','/')))return TYPES.get(s.replace('.','/')).replace('/','.');}return super.mapValue(v);}
                }),0);byte[] b=out.toByteArray();return defineClass(name,b,0,b.length);
            }catch(Exception e){throw new ClassNotFoundException(name,e);}
        }
    }
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static Method method(Loader l,String helper,String method,Class<?>...args)throws Exception{return l.loadClass("dev.fan4.compat."+helper).getMethod(method,args);}
    static void shader(Loader loader)throws Exception {
        String text="#version 150\n#extension GL_ARB_shader_texture_lod : enable\nvoid main(){float d=dhFarPlane;}\n";
        String fixed=String.join("",ShaderCompileCompat.declarations(List.of(text.substring(0,15),text.substring(15))));
        check(fixed.indexOf("#extension")<fixed.indexOf("uniform float dhFarPlane;"),"extensions must precede GLSL declarations");
        check(fixed.indexOf("uniform float dhFarPlane;")<fixed.indexOf("void main"),"declare before use");
        var declared=List.of("#version 150\nuniform float dhFarPlane;\nvoid main(){float d=dhFarPlane;}");
        check(ShaderCompileCompat.declarations(declared)==declared,"preserve existing declaration");
        var inactive=List.of("// dhFarPlane\n/* dhFarPlane */\nvoid main(){}");check(ShaderCompileCompat.declarations(inactive)==inactive,"comments must not activate fix");
        var macro=List.of("#define dhFarPlane 128.0\nvoid main(){float d=dhFarPlane;}");check(ShaderCompileCompat.declarations(macro)==macro,"preserve pack macro");
        Shader.ip_programName.set("basic");Shader.ip_programType.set(new Object());method(loader,"iris.ShaderCompileCompat","clearContext").invoke(null);
        check(Shader.ip_programName.get()==null&&Shader.ip_programType.get()==null,"clear both compiler thread locals");
    }
    static void compileScope(Loader loader)throws Exception {
        Method compile=loader.loadClass("dev.fan4.compat.mixin.iris.client.ShaderCompileScopeMixin").getDeclaredMethod("fan4$compileScope",Object.class,String.class,InputStream.class,String.class,Object.class,OperationAPI.class);compile.setAccessible(true);
        OperationAPI success=args->{Shader.ip_programType.set("vertex");Shader.ip_programName.set("basic");return 9;};
        check(compile.invoke(null,new Object(),"basic",new ByteArrayInputStream(new byte[0]),"",new Object(),success).equals(9),"preserve native compilation result");check(Shader.ip_programType.get()==null&&Shader.ip_programName.get()==null,"successful compilation clears context");
        RuntimeException failure=new RuntimeException("original shader error");OperationAPI broken=args->{Shader.ip_programType.set("fragment");Shader.ip_programName.set("entities_translucent");throw failure;};
        try {compile.invoke(null,new Object(),"basic",new ByteArrayInputStream(new byte[0]),"",new Object(),broken);throw new AssertionError("compile failure swallowed");}catch(java.lang.reflect.InvocationTargetException e){check(e.getCause()==failure,"preserve original shader error");}
        check(Shader.ip_programType.get()==null&&Shader.ip_programName.get()==null,"failed compile cannot poison next compile");
    }
    static void recipes() {
        Json patch=new Json().put("minecraft:stored_enchantments",new Json("fortune:1"));Json output=new Json().put("id",new Json("minecraft:enchanted_book")).put("count",new Json("2")).put("nbt",patch);
        Json recipe=new Json().put("type",new Json("betterend:infusion")).put("result",output);
        RecipeDataCompat.repair("betterend:fortune_book",recipe);
        check(output.has("item")&&!output.has("id")&&output.get("nbt")==patch&&output.get("count").getAsString().equals("2"),"repair preserves enchanted output and count");
        Json vanillaResult=new Json().put("id",new Json("minecraft:iron_ingot"));Json vanilla=new Json().put("type",new Json("minecraft:crafting_shaped")).put("result",vanillaResult);RecipeDataCompat.repair("betterend:vanilla",vanilla);check(vanillaResult.has("id"),"leave native item codec alone");
        Map<String,Json> input=new LinkedHashMap<>();input.put("betterend:fortune_book",recipe);input.put("jeed:remedy",new Json().put("type",new Json("jeed:effect_provider")));input.put("jeed:inebriation",new Json().put("type",new Json("jeed:effect_provider")));input.put("jeed:other",new Json());input.put("supplementaries:copper_lantern_conversion",new Json().put("block",new Json("suppsquared:copper_lantern")));
        var filtered=RecipeDataCompat.available(input,false,false);check(filtered.size()==2&&filtered.containsKey("jeed:other"),"only known recipes with absent dependencies filtered");check(input.size()==5,"source resource map retained");check(RecipeDataCompat.available(input,true,true).size()==5,"installed dependencies retain recipes");
        input.put("jeed:remedy",vanilla);input.put("jeed:inebriation",vanilla);input.put("supplementaries:copper_lantern_conversion",vanilla);check(RecipeDataCompat.available(input,false,false).size()==5,"valid data-pack overrides retained without optional mods");
    }
    static void position(Loader loader)throws Exception {
        Method prepare=method(loader,"immersiveportals.PositionPacketCompat","prepare",Object.class,Object.class),clear=method(loader,"immersiveportals.PositionPacketCompat","clearOldAwaiting",Object.class);
        Player p=new Player(new World());PositionPacket packet=new PositionPacket();prepare.invoke(null,p.field_13987,packet);check(packet.dimension.equals("overworld"),"stamp missing authoritative dimension");packet.dimension="tardis";prepare.invoke(null,p.field_13987,packet);check(packet.dimension.equals("tardis"),"retain explicit teleport destination");prepare.invoke(null,new Object(),packet);prepare.invoke(null,p.field_13987,new Object());
        p.field_13987.field_14119=new Object();p.field_13987.ip_dimOfAwaitingPosition="tardis";clear.invoke(null,p);check(p.field_13987.field_14119==null&&p.field_13987.ip_dimOfAwaitingPosition==null,"obsolete source correction cancelled");
        Object pending=new Object();p.field_13987.field_14119=pending;p.field_13987.ip_dimOfAwaitingPosition="overworld";clear.invoke(null,p);check(p.field_13987.field_14119==pending,"current dimension correction retained");
    }
    static void models(Loader loader)throws Exception {
        Method root=method(loader,"doctorwho.ModelBakeCompat","root",Object.class,Object.class,Object.class,String.class);Object owner=new Object(),layer=new Object();ModelLoader models=Client.instance.models;
        Part first=(Part)root.invoke(null,owner,models,layer,"method_32072");first.transform=8;first.children.get(0).transform=7;first.field_3665=false;first.children.get(0).field_38456=true;
        check(root.invoke(null,owner,models,layer,"method_32072")==first&&models.bakes==1,"root baked once per renderer/layer");check(first.transform==0&&first.children.get(0).transform==0&&first.field_3665&&!first.children.get(0).field_38456,"reset every part transform and flags between entities");
        check(root.invoke(null,new Object(),models,layer,"method_32072")!=first,"renderer roots independent");models.field_27542=new HashMap<>();check(root.invoke(null,owner,models,layer,"method_32072")!=first,"resource reload invalidates cached roots");
    }
    static void accessories(Loader loader)throws Exception {
        Method entity=method(loader,"accessories.PortalEntitySyncCompat","entity",Object.class,int.class),defer=method(loader,"accessories.PortalEntitySyncCompat","defer",Object.class,Object.class),tick=method(loader,"accessories.PortalEntitySyncCompat","tick",Object.class);
        World local=new World(),remote=new World();Worlds.worlds=List.of(local,remote);Player p=new Player(local);Client c=new Client();c.field_1724=p;Object target=new Object();remote.entities.put(42,target);check(entity.invoke(null,local,42)==target,"remote portal entity resolved");Object localTarget=new Object();local.entities.put(42,localTarget);check(entity.invoke(null,local,42)==localTarget,"active world entity preferred");
        check(!(Boolean)defer.invoke(null,new Packet(42,"live"),p),"ready entity stays on native handler");check((Boolean)defer.invoke(null,new Packet(43,"full"),p),"queue missing entity");check((Boolean)defer.invoke(null,new Packet(43,"delta"),p),"queue ordered delta");tick.invoke(null,c);check(Packet.applied.isEmpty(),"wait for spawn");remote.entities.put(43,new Object());tick.invoke(null,c);check(Packet.applied.equals(List.of("full","delta")),"replay in order when remote entity spawns");
        defer.invoke(null,new Packet(44,"expired"),p);for(int i=0;i<101;i++)tick.invoke(null,c);remote.entities.put(44,new Object());tick.invoke(null,c);check(Packet.applied.size()==2,"expired data dropped");defer.invoke(null,new Packet(45,"disconnected"),p);c.field_1724=null;tick.invoke(null,c);c.field_1724=p;remote.entities.put(45,new Object());tick.invoke(null,c);check(Packet.applied.size()==2,"disconnect clears pending state");
    }
    static void doorway(Loader loader)throws Exception {
        Method attach=method(loader,"immersiveportals.PortalMotionCompat","attach",Object.class,long.class,boolean.class,Point.class,Point.class,Point.class,double.class,double.class),loaders=method(loader,"immersiveportals.DoorwayShipLoading","loaders",Object.class,java.util.function.Consumer.class);
        World interior=new World(),exterior=new World();interior.dimension="tardis";Player p=new Player(interior);Portal portal=new Portal();portal.origin=interior;portal.destination=exterior;
        attach.invoke(null,portal,42L,true,new Point(0,0,0),new Point(1,0,0),new Point(0,1,0),1d,2d);
        List<ChunkLoader> loads=new ArrayList<>();java.util.function.Consumer<Object> consumer=o->loads.add((ChunkLoader)o);loaders.invoke(null,p,consumer);
        check(loads.equals(List.of(new ChunkLoader("overworld",-62500,0,0))),"request ship chunks directly from anchor without loaded client metadata");
        for(int kind=0;kind<5;kind++) {
            loads.clear();portal.allowed=true;portal.removed=false;portal.valid=true;portal.distance=1;Utils.ship.dimension="overworld";
            if(kind==0)portal.allowed=false;if(kind==1)portal.removed=true;if(kind==2)portal.valid=false;if(kind==3)portal.distance=65;if(kind==4)Utils.ship.dimension="aether";
            loaders.invoke(null,p,consumer);check(loads.isEmpty(),"portal access/removal/range and ship dimension enforced");
        }
        Utils.ship.dimension="overworld";portal.valid=true;portal.distance=1;loads.clear();loaders.invoke(null,new Player(exterior),consumer);check(loads.equals(List.of(new ChunkLoader("overworld",-62500,0,0))),"near-player chunks stay watched independently of the portal origin");
    }
    static void sound(Loader loader)throws Exception {
        Method capture=method(loader,"soundphysics.ShipSoundRaycast","capture",Object.class,Object.class,Object.class,int.class),ray=method(loader,"soundphysics.ShipSoundRaycast","rayCast",Object.class,Object.class,Object.class,Object.class,Object.class);
        World live=new World();ProxyWorld snapshot=new ProxyWorld();Utils.ship.worldX=100;capture.invoke(null,snapshot,live,new Pos(100,64,0),4);
        check(snapshot.clonedLevelChunks.containsKey(new ChunkPos(-62500,0)),"snapshot includes loaded ship chunks");
        Pos ignored=new Pos(-999990,64,0);Operation op=new Operation();Hit hit=(Hit)ray.invoke(null,snapshot,new Vec(-1000000,64,0),new Vec(110,64,0),ignored,op);
        check(hit.position.field_1352==103&&hit.block.x()==-999997,"ship hit returns world position and ship-local material address");check(op.starts.size()==2&&op.starts.get(0).field_1352==100&&op.starts.get(1).field_1352==-1000000,"convert mixed-frame endpoints before either trace");check(op.ignored==ignored,"ignored block forwarded unchanged");
        Utils.ship.worldX=200;op=new Operation();hit=(Hit)ray.invoke(null,snapshot,new Vec(-1000000,64,0),new Vec(110,64,0),ignored,op);check(hit.position.field_1352==103,"audio trace uses immutable captured pose");
        op=new Operation();op.terrain=true;hit=(Hit)ray.invoke(null,snapshot,new Vec(-1000000,64,0),new Vec(110,64,0),ignored,op);check(hit.position.field_1352==102,"nearest ordinary terrain wins");
        op=new Operation();op.foreign=true;hit=(Hit)ray.invoke(null,snapshot,new Vec(-1000000,64,0),new Vec(110,64,0),ignored,op);check(hit.type==HitType.MISS,"ship trace rejects blocks outside its captured chunks");
        op=new Operation();Vec source=new Vec(5,64,0);ray.invoke(null,new Object(),source,new Vec(6,64,0),ignored,op);check(op.starts.size()==1&&op.starts.get(0)==source,"unknown proxy retains native raycast");
        Utils.ship.worldX=100;snapshot=new ProxyWorld();capture.invoke(null,snapshot,live,new Pos(-1000000,64,0),4);check(Utils.lastBounds.x1()==36,"shipyard listener origin converted before ship query");check(snapshot.clonedLevelChunks.containsKey(new ChunkPos(5,0)),"shipyard listener snapshot includes nearby ordinary terrain");
        op=new Operation();ray.invoke(null,snapshot,new Vec(-30000000,64,0),new Vec(110,64,0),ignored,op);check(!op.starts.isEmpty()&&op.starts.size()<8,"unresolved distant origin traces only bounded cached space");
        snapshot=new ProxyWorld();snapshot.clonedLevelChunks.put(new ChunkPos(70,0),new Object());capture.invoke(null,snapshot,live,new Pos(100,64,0),4);op=new Operation();ray.invoke(null,snapshot,new Vec(100,64,0),new Vec(1100,64,0),ignored,op);check(op.starts.size()>=5,"long cached segments subdivided while retaining ship/terrain tracing");
    }
    static void chunkPackets(Loader loader)throws Exception {
        Method watch=method(loader,"immersiveportals.ShipChunkPackets","watch",Object.class,Object.class,boolean.class,Object.class,Object.class),dimension=method(loader,"immersiveportals.ShipChunkPackets","watchDimension",Object.class),drop=method(loader,"immersiveportals.ShipChunkPackets","drop",Object.class,Object.class,Object.class,Object.class);
        World exterior=new World(),interior=new World();interior.dimension="tardis";Player p=new Player(interior);Wrapper wrapper=new Wrapper(p);ChunkPos pos=new ChunkPos(-62500,0);Object task=new Object();int[] calls={0};Tracking.watching=true;
        OperationAPI send=args->{calls[0]++;check(args[0]==task&&args[1]==exterior&&args[2].equals(true)&&args[3]==pos,"native task arguments forwarded");check(Redirect.destination==exterior,"packet targets owning overworld, not player TARDIS world");try{check(dimension.invoke(null,wrapper).equals("overworld"),"remote IP watch qualifies for task dimension");}catch(Exception e){throw new RuntimeException(e);}return null;};
        watch.invoke(null,task,exterior,true,pos,send);check(calls[0]==1&&Redirect.destination==null,"native watch executes once and redirect restores");check(dimension.invoke(null,wrapper).equals("tardis"),"native wrapper dimension unchanged outside watch scope");
        RuntimeException failure=new RuntimeException("send failure");OperationAPI broken=args->{throw failure;};try{watch.invoke(null,task,exterior,true,pos,broken);throw new AssertionError("failure swallowed");}catch(java.lang.reflect.InvocationTargetException e){check(e.getCause()==failure,"original packet failure preserved");}check(Redirect.destination==null&&dimension.invoke(null,wrapper).equals("tardis"),"exception restores both scopes");
        ChunkMap manager=new ChunkMap(exterior);OperationAPI forget=args->{calls[0]++;check(args[0]==manager&&args[1]==p&&args[2]==pos,"native unload arguments forwarded");check(Redirect.destination==exterior,"unload targets owning world");return null;};drop.invoke(null,manager,p,pos,forget);check(calls[0]==1,"active IP watch protects chunk from VS unload");Tracking.watching=false;drop.invoke(null,manager,p,pos,forget);check(calls[0]==2&&Redirect.destination==null,"unwatched chunk unloads once in its own world");
    }
    static void stackCodec(Loader loader)throws Exception {
        ItemCodec nativeCodec=new ItemCodec();MapCodec wrapper=(MapCodec)loader.loadClass("dev.fan4.compat.bclib.AliasedStackCodec").getConstructor(MapCodec.class).newInstance(nativeCodec);Ops ops=key->new Result(key instanceof String?key:null);Input input=new Input();Object patch=new Object();input.values.put("id","minecraft:enchanted_book");input.values.put("nbt",patch);input.values.put("count",2);
        try{nativeCodec.decode(ops,input);throw new AssertionError("fixture should reproduce No key item");}catch(IllegalArgumentException expected){}
        check(wrapper.decode(ops,input).value().equals("minecraft:enchanted_book"),"native stack decoder accepts id without resource-map preprocessing");check(nativeCodec.last.get("nbt")==patch&&nativeCodec.last.get("count").equals(2),"count and component patch untouched");check(input.get("item")==null,"input map retained");input.values.put("item","minecraft:diamond");check(wrapper.decode(ops,input).value().equals("minecraft:diamond"),"existing item key wins");Builder output=new Builder();check(wrapper.encode("item",ops,output)==output,"native encoding forwarded");check(wrapper.keys(ops).toList().equals(List.of("item","count","nbt")),"native key stream forwarded");
    }
    static void minecraft(String minecraft,String mapping)throws Exception {
        List<String> lines;try(ZipFile z=new ZipFile(mapping)){lines=new String(z.getInputStream(z.getEntry("mappings/mappings.tiny")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).lines().toList();}
        Map<String,String> types=new HashMap<>(),methods=new HashMap<>(),fields=new HashMap<>();String owner="";
        for(String line:lines){String[] p=line.split("\t");if(p[0].equals("c")){owner=p[1];types.put(owner,p[2]);}else if(p.length==5&&p[1].equals("m"))methods.put(owner+"."+p[3]+p[2],p[4]);else if(p.length==5&&p[1].equals("f"))fields.put(owner+"."+p[3],p[4]);}
        Remapper remap=new Remapper(){public String map(String n){return types.getOrDefault(n,n);}public String mapMethodName(String o,String n,String d){return methods.getOrDefault(o+"."+n+d,n);}public String mapFieldName(String o,String n,String d){return fields.getOrDefault(o+"."+n,n);}};
        Set<String> needed=Set.of("net/minecraft/class_1297","net/minecraft/class_281","net/minecraft/class_8609","net/minecraft/class_1863","net/minecraft/class_310","net/minecraft/class_922","net/minecraft/class_563","net/minecraft/class_5607");
        try(ZipFile z=new ZipFile(minecraft)){for(ZipEntry entry:Collections.list(z.entries()))if(entry.getName().endsWith(".class")&&needed.contains(types.getOrDefault(entry.getName().substring(0,entry.getName().length()-6),""))){ClassNode n=new ClassNode();new ClassReader(z.getInputStream(entry)).accept(new ClassRemapper(n,remap),0);VerifyAddon.classes.put(n.name,n);}}
    }
    static void nativeContracts(String[] jars)throws Exception {
        int count=jars.length;if(count>=2&&jars[count-1].endsWith("intermediary.jar")){minecraft(jars[count-2],jars[count-1]);count-=2;}
        for(int i=0;i<count;i++)VerifyAddon.readJar(jars[i]);
        for(Path p:Files.walk(Path.of("build/generated/classes/dev/fan4/compat/mixin")).filter(p->p.toString().endsWith(".class")).toList()) {
            ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);
            if(n.name.contains("ShipAcknowledgementRecoveryMixin")||n.name.contains("ShipAcknowledgementFlushMixin")||n.name.contains("ShipGuardRecoveryMixin")||n.name.contains("StackCodecAliasMixin")||n.name.contains("TardisShipModelCache")||n.name.contains("PortalSync")||n.name.contains("PortalInvalidate")||n.name.contains("ShipAcoustic")||n.name.contains("DoorwayShipLoading")||n.name.contains("ShipChunkPacketMixin")||n.name.contains("PositionPacketAwaiting")||n.name.contains("MixinShipPortalTargetDistance"))VerifyAddon.injectionTargets(n);
            if(VerifyAddon.classes.containsKey("net/minecraft/class_281")&&(n.name.contains("ShaderCompileScope")||n.name.contains("PositionPacketDimension")||n.name.contains("OptionalRecipeDependencies")||n.name.contains("RecipeStackFormat")||n.name.contains("PortalEntityRetry")))VerifyAddon.injectionTargets(n);
        }
    }
    public static void main(String[] args)throws Exception {
        Loader loader=new Loader();shader(loader);compileScope(loader);recipes();position(loader);models(loader);accessories(loader);doorway(loader);sound(loader);chunkPackets(loader);stackCodec(loader);if(args.length>0)nativeContracts(args);
        System.out.println("PASS: shader declarations/context, component-preserving recipes, packet metadata/corrections, model reuse/reload ordered remote entity sync, doorway bootstrap, dimension-aware chunk packets, unload ownership, bounded acoustic rays and native stack codec aliases");
    }
}
