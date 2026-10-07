import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Runs the generated handlers against small lifecycle fixtures, without game jars. */
public class PortalTransitionSmokeTest implements Opcodes {
    public interface LongMap { Object put(long key, Object value); }
    public static class ChunkMap extends HashMap<Long,Object> implements LongMap {
        public Object put(long key,Object value) { return super.put(key,value); }
    }
    public static class World {}
    public static class ClientWorld extends World {}
    public static class Pos {
        public int field_9181,field_9180;
        Pos(int x,int z) {field_9181=x;field_9180=z;}
        public long method_8324() {return ((long)field_9181<<32)^(field_9180&0xffffffffL);}
    }
    public static class Chunk {
        final Pos pos;final ClientWorld world;
        Chunk(ClientWorld w,int x,int z) {world=w;pos=new Pos(x,z);}
        public Pos method_12004() {return pos;}
        public World method_12200() {return world;}
    }
    public interface Claim { boolean contains(int x,int z); }
    public interface Ship { Claim getChunkClaim(); }
    public interface Duck { LongMap vs$getShipChunks(); void vs$removeShip(Ship ship); }
    public static class IpCache {
        public final List<Chunk> chunks=new ArrayList<>();
        public List<Chunk> getCopiedChunkList() {return List.copyOf(chunks);}
        public void method_2859(Pos pos) {chunks.removeIf(c->c.pos.method_8324()==pos.method_8324());}
    }
    public static class ShipUtils { public static boolean isChunkInShipyard(World w,int x,int z) {return x>=100;} }
    public static class Sodium {
        static final List<World> removed=new ArrayList<>(),added=new ArrayList<>();
        public static void onChunkAdded(ClientWorld world,int x,int z) {added.add(world);}
        public static void onChunkRemoved(ClientWorld world,int x,int z) {removed.add(world);}
    }
    public static class Minecraft {
        static final Minecraft INSTANCE=new Minecraft();
        public ClientWorld field_1687;
        public static Minecraft method_1551() {return INSTANCE;}
    }
    public static class Connectivity {
        public static final Connectivity INSTANCE=new Connectivity();
        static int queued,drained;
        public static void queueChunkForInitialization(Pos pos,boolean added) {queued++;}
        public void onRegistriesCompleted() {drained++;}
    }
    public static class BlockInfo {
        static boolean initialized=true;
        public static boolean isSortedRegistryInitialized() {return initialized;}
    }
    public interface LevelWrapper {}
    public interface DhLevel {}
    public static class ServerLevel implements DhLevel {}
    public interface DhWorld { DhLevel getOrLoadLevel(LevelWrapper wrapper); }
    public static class ServerWorld implements DhWorld {
        final Map<LevelWrapper,ServerLevel> levels=new HashMap<>();
        int loads;
        public DhLevel getOrLoadLevel(LevelWrapper wrapper) {return levels.computeIfAbsent(wrapper,w->{loads++;return new ServerLevel();});}
    }
    static Class<?> load(Path source,String className,String parent,Map<String,String> types)throws Exception {
        ClassNode node=new ClassNode();
        new ClassReader(Files.readAllBytes(source)).accept(new ClassRemapper(node,new Remapper() {
            public String map(String name) {return types.getOrDefault(name,name);}
        }),0);
        if(className.equals("SmokeTestChunkLoad"))node.methods.removeIf(m->!m.name.equals("chunkLoaded"));
        node.name=className;node.superName=parent;node.invisibleAnnotations=null;node.visibleAnnotations=null;
        for(MethodNode m:node.methods) {m.visibleAnnotations=null;m.invisibleAnnotations=null;}
        MethodNode ctor=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);
        ctor.instructions.add(new VarInsnNode(ALOAD,0));ctor.instructions.add(new MethodInsnNode(INVOKESPECIAL,parent,"<init>","()V",false));ctor.instructions.add(new InsnNode(RETURN));node.methods.add(ctor);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);node.accept(writer);
        return new ClassLoader(PortalTransitionSmokeTest.class.getClassLoader()) {Class<?> define(){byte[] bytes=writer.toByteArray();return defineClass(className,bytes,0,bytes.length);}}.define();
    }
    static void check(boolean condition) {if(!condition)throw new AssertionError("Portal transition regression");}
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]);String base="PortalTransitionSmokeTest$";
        Map<String,String> types=new HashMap<>();
        String[][] pairs={
            {"qouteall/imm_ptl/core/chunk_loading/ImmPtlClientChunkMap","IpCache"},
            {"org/valkyrienskies/mod/mixinducks/client/world/ClientChunkCacheDuck","Duck"},
            {"org/valkyrienskies/core/api/ships/ClientShip","Ship"},
            {"org/valkyrienskies/core/api/ships/properties/ChunkClaim","Claim"},
            {"org/valkyrienskies/mod/common/VSGameUtilsKt","ShipUtils"},
            {"org/valkyrienskies/mod/compat/SodiumCompat","Sodium"},
            {"net/minecraft/class_2818","Chunk"},{"net/minecraft/class_1923","Pos"},
            {"net/minecraft/class_310","Minecraft"},
            {"org/valkyrienskies/mod/util/ClientConnectivityUpdateQueue","Connectivity"},
            {"org/valkyrienskies/mod/common/BlockStateInfo","BlockInfo"},
            {"net/minecraft/class_1937","World"},{"net/minecraft/class_638","ClientWorld"},
            {"it/unimi/dsi/fastutil/longs/Long2ObjectMap","LongMap"},
            {"it/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap","ChunkMap"},
            {"com/seibel/distanthorizons/core/world/AbstractDhServerWorld","ServerWorld"},
            {"com/seibel/distanthorizons/core/world/IDhWorld","DhWorld"},
            {"com/seibel/distanthorizons/core/wrapperInterfaces/world/ILevelWrapper","LevelWrapper"},
            {"com/seibel/distanthorizons/core/level/AbstractDhServerLevel","ServerLevel"},
            {"com/seibel/distanthorizons/core/level/IDhLevel","DhLevel"}};
        for(String[] pair:pairs)types.put(pair[0],base+pair[1]);
        IpCache cache=(IpCache)load(root.resolve("dev/fan4/compat/mixin/immersiveportals/client/MixinShipChunkCacheDuck.class"),"SmokeTestDuck",base+"IpCache",types).getConstructor().newInstance();
        ClientWorld world=new ClientWorld(),otherWorld=new ClientWorld();IpCache other=new IpCache();
        cache.chunks.addAll(List.of(new Chunk(world,0,0),new Chunk(world,100,0),new Chunk(world,101,0),new Chunk(world,200,0)));
        other.chunks.add(new Chunk(otherWorld,100,0));
        check(((ChunkMap)((Duck)cache).vs$getShipChunks()).size()==3);
        ((Duck)cache).vs$removeShip(()->(x,z)->x>=100&&x<=101);
        check(cache.chunks.size()==4&&((ChunkMap)((Duck)cache).vs$getShipChunks()).size()==3);
        check(other.chunks.size()==1&&Sodium.removed.isEmpty());
        ((Duck)cache).vs$removeShip(()->(x,z)->x==500);check(cache.chunks.size()==4);
        cache.method_2859(new Pos(100,0));check(cache.chunks.size()==3&&((ChunkMap)((Duck)cache).vs$getShipChunks()).size()==2);
        Minecraft.INSTANCE.field_1687=world;
        Class<?> chunks=load(root.resolve("dev/fan4/compat/immersiveportals/ShipClientBridge.class"),"SmokeTestChunkLoad","java/lang/Object",types);
        var chunkLoaded=chunks.getMethod("chunkLoaded",Chunk.class);
        chunkLoaded.invoke(null,new Chunk(world,100,0));
        check(Sodium.added.equals(List.of(world))&&Connectivity.queued==1&&Connectivity.drained==1);
        chunkLoaded.invoke(null,new Chunk(otherWorld,200,0));
        check(Sodium.added.equals(List.of(world,otherWorld))&&Connectivity.queued==1&&Connectivity.drained==1);
        chunkLoaded.invoke(null,new Chunk(otherWorld,0,0));
        chunkLoaded.invoke(null,new Object[]{null});
        check(Sodium.added.size()==2&&Connectivity.queued==1);
        BlockInfo.initialized=false;
        chunkLoaded.invoke(null,new Chunk(world,101,0));
        check(Connectivity.queued==2&&Connectivity.drained==1);
        Class<?> handler=load(root.resolve("dev/fan4/compat/mixin/distanthorizons/common/DhDynamicDimensionMixin.class"),"SmokeTestDh",base+"ServerWorld",types);
        Object instance=handler.getConstructor().newInstance();var load=handler.getDeclaredMethod("fan4$loadDynamicLevel",ServerWorld.class,LevelWrapper.class);load.setAccessible(true);
        ServerWorld server=new ServerWorld();LevelWrapper origin=new LevelWrapper(){},destination=new LevelWrapper(){};
        ServerLevel existing=(ServerLevel)server.getOrLoadLevel(origin);
        check(load.invoke(instance,server,origin)==existing);
        Object newLevel=load.invoke(instance,server,destination);check(newLevel!=null&&server.loads==2);
        check(load.invoke(instance,server,destination)==newLevel&&server.loads==2);
        System.out.println("PASS: generated portal handlers preserve existing levels, load new levels, preserve IP-owned chunks across VS metadata unloading, and isolate world caches");
    }
}
