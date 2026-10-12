import dev.fan4.compat.xaero.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import xaero.map.region.*;

public final class SharedMapClientTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static final class Region {
        public final Object writerThreadPauseSync=new Object();boolean paused,resting=true,normal=true;int loadState=2,visits;boolean beingWritten,cachePrepared=true;
        final Map<List<Integer>,MapTileChunk> chunks=new HashMap<>();
        public boolean isWritingPaused(){return paused;}public byte getLoadState(){return (byte)loadState;}public void registerVisit(){visits++;}public boolean isResting(){return resting;}
        public boolean isNormalMapData(){return normal;}public void setBeingWritten(boolean b){beingWritten=b;}public Object getChunk(int x,int z){return chunks.get(List.of(x,z));}
        public void setChunk(int x,int z,Object chunk){chunks.put(List.of(x,z),(MapTileChunk)chunk);}public void setAllCachePrepared(boolean b){cachePrepared=b;}
    }
    public static final class Pool {final List<MapTile> made=new ArrayList<>();public Object get(String dimension,int x,int z){MapTile tile=new MapTile(x,z);made.add(tile);return tile;}}
    public static final class MapWorld {Object dimension;public Object getCurrentDimensionId(){return dimension;}}
    public static final class Processor {
        final MapWorld world=new MapWorld();final Map<List<Integer>,Region> regions=new HashMap<>();final Pool pool=new Pool();
        public Object getMapWorld(){return world;}
        public Object getLeafMapRegion(int layer,int x,int z,boolean create){if(layer!=Integer.MAX_VALUE||create)throw new AssertionError("surface layer, never created: "+layer+" "+create);return regions.get(List.of(x,z));}
        public Object getTilePool(){return pool;}public String getCurrentDimension(){return "overworld-dir";}public Object getWorldBlockTintProvider(){return "tint";}public Object getOverlayManager(){return "overlays";}
    }
    public static final class Writer {private final Object blockStateShortShapeCache="shapes";final Processor processor=new Processor();public Object getMapProcessor(){return processor;}}
    static SharedMapChunk chunk(int x,int z,long stamp){return SharedMapTest.chunk("minecraft:overworld",x,z,stamp,"minecraft:grass_block",64+(x&3));}
    static Region loaded(Writer writer,int regionX,int regionZ){Region r=new Region();writer.processor.regions.put(List.of(regionX,regionZ),r);return r;}
    public static void main(String[] args)throws Exception {
        Class<?> applier=Class.forName("dev.fan4.compat.xaero.SharedMapApplier");Method apply=applier.getDeclaredMethod("apply",Object.class,SharedMapChunk.class);apply.setAccessible(true);
        Class<?> resultType=Class.forName("dev.fan4.compat.xaero.SharedMapApplier$Result");
        java.util.function.BiFunction<Writer,SharedMapChunk,String> run=(w,c)->{try{return String.valueOf(apply.invoke(null,w,c));}catch(ReflectiveOperationException e){throw new AssertionError(e.getCause());}};
        Writer writer=new Writer();writer.processor.world.dimension=net.minecraft.class_5321.method_29179("dimension","minecraft:overworld");

        // A normal write into a loaded region creates the tile chunk and tile exactly as Xaero's writer would.
        Region region=loaded(writer,0,0);
        check(run.apply(writer,chunk(5,6,500)).equals("APPLIED"),"loaded region: applied");
        MapTileChunk tileChunk=region.chunks.get(List.of(1,1));
        check(tileChunk!=null&&tileChunk.x==1&&tileChunk.z==1&&tileChunk.loadState==2&&!region.cachePrepared&&region.beingWritten&&region.visits==1,"tile chunk (5>>2, 6>>2)=(1,1) created and registered like the writer");
        MapTile tile=(MapTile)tileChunk.getTile(1,2);
        check(tile!=null&&tile.x==5&&tile.z==6&&tile.loaded&&tile.version==1&&tile.caveStart==Integer.MAX_VALUE&&tile.blocks.size()==256,"tile in slot (5&3, 6&3)=(1,2), 256 blocks, surface layer, interpretation version");
        MapBlock block=(MapBlock)tile.blocks.get(List.of(3,4));
        check(String.valueOf(block.state).equals("state:minecraft:grass_block")&&block.height==65&&block.top==65&&block.light==15&&!block.glowing&&((net.minecraft.class_5321)block.biome).id.toString().equals("minecraft:plains")&&block.prepared==65,"a block carries the state, heights, biome key and full light");
        check(tileChunk.changed&&tileChunk.updates==1&&tileChunk.terrain&&tileChunk.shapes.equals("shapes")&&tileChunk.processor==writer.processor,"textures refreshed with the writer's own shape cache; terrain flagged");
        check(writer.processor.pool.made.size()==1,"one tile taken from the pool");

        // Existing data always wins.
        int updates=tileChunk.updates;
        check(run.apply(writer,chunk(5,6,600)).equals("SKIPPED")&&tileChunk.updates==updates&&writer.processor.pool.made.size()==1,"an existing tile is never replaced");
        check(run.apply(writer,chunk(4,6,600)).equals("APPLIED")&&tileChunk.getTile(0,2)!=null&&region.chunks.size()==1,"a neighbouring slot of the same tile chunk is filled without a second tile chunk");

        // Negative coordinates use floor shifts and masks.
        Region negative=loaded(writer,-1,-1);
        check(run.apply(writer,chunk(-1,-1,700)).equals("APPLIED")&&negative.chunks.get(List.of(7,7)).getTile(3,3)!=null,"chunk (-1,-1): region -1, tile chunk local (7,7), slot (3,3)");

        // Conditions under which Xaero's writer would not write: retry later.
        check(run.apply(writer,chunk(1000,1000,1)).equals("RETRY"),"region not loaded: retry");
        Region busy=loaded(writer,2,0);busy.loadState=1;
        check(run.apply(writer,chunk(64,0,1)).equals("RETRY")&&busy.chunks.isEmpty(),"region still loading: retry, nothing created");
        busy.loadState=2;busy.paused=true;check(run.apply(writer,chunk(64,0,1)).equals("RETRY"),"writing paused: retry");
        busy.paused=false;busy.resting=false;check(run.apply(writer,chunk(64,0,1)).equals("RETRY"),"region not at rest: retry");
        busy.resting=true;busy.chunks.put(List.of(0,0),new MapTileChunk(busy,16,0));
        check(run.apply(writer,chunk(64,0,1)).equals("RETRY"),"tile chunk not loaded yet: retry");
        busy.chunks.get(List.of(0,0)).loadState=2;busy.chunks.get(List.of(0,0)).downloading=true;
        check(run.apply(writer,chunk(64,0,1)).equals("RETRY"),"texture downloading from the GPU: retry");
        busy.chunks.get(List.of(0,0)).downloading=false;check(run.apply(writer,chunk(64,0,1)).equals("APPLIED"),"and once it settles, applied");
        busy.normal=false;busy.chunks.clear();check(run.apply(writer,chunk(64,0,1)).equals("DISCARD"),"not normal map data: discarded");

        // Wrong dimension never writes anywhere.
        writer.processor.world.dimension=net.minecraft.class_5321.method_29179("dimension","minecraft:the_nether");
        check(run.apply(writer,chunk(5,9,1)).equals("DISCARD"),"a chunk for another dimension than the one shown is discarded");
        writer.processor.world.dimension=null;check(run.apply(writer,chunk(5,9,1)).equals("RETRY"),"no dimension yet: retry");

        // Client queue: progress only for applied/skipped chunks, retries rationed, a failure ends the session quietly.
        Path file=Files.createTempDirectory("fan4sharedclient").resolve("state/server.properties");
        Files.createDirectories(file.getParent());Files.writeString(file,"minecraft\\:overworld=300\nnot\\ an\\ id=9\nminecraft\\:the_nether=x\n");
        SharedMapClient.join(file);
        check(SharedMapClient.since().equals(Map.of("minecraft:overworld",300L)),"saved progress loaded; junk ignored");
        Method tick=SharedMapClient.class.getDeclaredMethod("tick",Object.class,Class.forName("dev.fan4.compat.xaero.SharedMapClient$Applier"));tick.setAccessible(true);
        Map<Long,String> script=new HashMap<>();List<Long> seen=new ArrayList<>();
        Object applierProxy=Proxy.newProxyInstance(SharedMapClient.class.getClassLoader(),new Class<?>[]{Class.forName("dev.fan4.compat.xaero.SharedMapClient$Applier")},(p,m,a)->{
            SharedMapChunk c=(SharedMapChunk)a[1];seen.add(c.stamp);
            if(script.getOrDefault(c.stamp,"A").equals("BOOM"))throw new IllegalStateException("xaero changed");
            return Enum.valueOf((Class<Enum>)resultType,script.getOrDefault(c.stamp,"APPLIED"));
        });
        script.put(401L,"RETRY");script.put(402L,"DISCARD");script.put(403L,"SKIPPED");
        SharedMapClient.accept(List.of(chunk(0,0,400),chunk(1,0,401),chunk(2,0,402),chunk(3,0,403),chunk(4,0,404)));
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(400L,401L,402L)),"three chunks per frame: "+seen);
        check(SharedMapClient.since().get("minecraft:overworld")==400,"only applied chunks advance progress (retry and discard do not)");
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(400L,401L,402L,403L,404L))&&SharedMapClient.since().get("minecraft:overworld")==404,"the rest in the next frame; skipped counts as progress");
        script.put(401L,"APPLIED");int before=seen.size();
        for(int i=0;i<57;i++)tick.invoke(null,writer,applierProxy);
        check(seen.size()==before,"a retried chunk waits many frames before being tried again");
        tick.invoke(null,writer,applierProxy);
        check(seen.size()==before+1,"and is then retried once and applied");
        SharedMapClient.accept(List.of(chunk(9,9,999)));script.put(999L,"BOOM");
        tick.invoke(null,writer,applierProxy);
        SharedMapClient.accept(List.of(chunk(10,9,1000)));int count=seen.size();tick.invoke(null,writer,applierProxy);
        check(seen.size()==count,"after a failure the shared map is off for the session and the game is unaffected");
        SharedMapClient.leave();
        check(Files.readString(file).contains("minecraft\\:overworld=404"),"progress is saved when leaving the server");
        SharedMapClient.join(null);SharedMapClient.accept(List.of(new SharedMapChunk("bad",0,0,1,new String[]{"x"},new String[]{"y"},new byte[256],new byte[256],new short[256],new short[256])));
        check(!SharedMapClient.since().containsKey("bad"),"invalid chunks are dropped on receipt");
        System.out.println("PASS: shared map applier mirrors Xaero's write sequence (create/skip/retry/discard cases, coordinates incl. negatives) and the client queue (progress, retry rationing, failure containment, saved progress)");
    }
}
