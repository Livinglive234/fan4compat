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
        boolean loadRequested;public boolean canRequestReload_unsynced(){return !loadRequested;}
        public boolean isNormalMapData(){return normal;}public void setBeingWritten(boolean b){beingWritten=b;}public Object getChunk(int x,int z){return chunks.get(List.of(x,z));}
        public void setChunk(int x,int z,Object chunk){chunks.put(List.of(x,z),(MapTileChunk)chunk);}public void setAllCachePrepared(boolean b){cachePrepared=b;}
    }
    public static final class Pool {final List<MapTile> made=new ArrayList<>();public Object get(String dimension,int x,int z){MapTile tile=new MapTile(x,z);made.add(tile);return tile;}}
    public static final class SaveLoad {final List<Region> requested=new ArrayList<>();public void requestLoad(Object region,String reason){((Region)region).loadRequested=true;requested.add((Region)region);}}
    public static final class MapWorld {Object dimension;public Object getCurrentDimensionId(){return dimension;}}
    public static final class Processor {
        final MapWorld world=new MapWorld();final Map<List<Integer>,Region> regions=new HashMap<>();final Pool pool=new Pool();final SaveLoad saveLoad=new SaveLoad();boolean allowCreate;int created;
        public Object getMapSaveLoad(){return saveLoad;}
        public Object getMapWorld(){return world;}
        public Object getLeafMapRegion(int layer,int x,int z,boolean create){
            if(layer!=Integer.MAX_VALUE||create&&!allowCreate)throw new AssertionError("surface layer, created only when this test allows it: "+layer+" "+create);
            Region found=regions.get(List.of(x,z));
            if(found==null&&create){found=new Region();found.loadState=0;regions.put(List.of(x,z),found);created++;}
            return found;
        }
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

        // Water is written the way Xaero stores it: the terrain is the block, the fluid an overlay whose opacity is its depth.
        byte[] under=new byte[256],sand=new byte[256];Arrays.fill(under,(byte)1);Arrays.fill(sand,(byte)1);
        short[] bedY=new short[256],surfaceY=new short[256];Arrays.fill(bedY,(short)60);Arrays.fill(surfaceY,(short)64);
        SharedMapChunk lake=new SharedMapChunk("minecraft:overworld",8,8,50,new String[]{"minecraft:water","minecraft:sand"},new String[]{"minecraft:plains"},sand,new byte[256],under,bedY,surfaceY);
        Region lakeRegion=loaded(writer,0,0);
        check(run.apply(writer,lake).equals("APPLIED"),"a lake chunk is written");
        MapBlock lakeBlock=(MapBlock)((MapTile)lakeRegion.chunks.get(List.of(2,2)).getTile(0,0)).blocks.get(List.of(3,4));
        check(String.valueOf(lakeBlock.state).equals("state:minecraft:sand")&&lakeBlock.height==60&&lakeBlock.top==64,"the block is the bed, with the bed and surface heights");
        check(lakeBlock.overlays.size()==1&&String.valueOf(lakeBlock.overlays.get(0).state).equals("state:minecraft:water")&&lakeBlock.overlays.get(0).opacity==4,"the water is one overlay whose opacity is its depth (4)");
        check(lakeRegion.chunks.get(List.of(2,2)).getTile(0,0)!=null&&((MapBlock)((MapTile)lakeRegion.chunks.get(List.of(2,2)).getTile(0,0)).blocks.get(List.of(0,0))).overlays.size()==1,"every column of a lake chunk has its overlay");
        check(((MapBlock)((MapTile)region.chunks.get(List.of(1,1)).getTile(1,2)).blocks.get(List.of(3,4))).overlays.isEmpty(),"dry land has no overlay");
        // A region that is not in memory is created and loaded the way Xaero's own writer does it, a few at a time.
        Method newFrame=applier.getDeclaredMethod("newFrame");newFrame.setAccessible(true);
        writer.processor.allowCreate=true;
        check(run.apply(writer,chunk(2000,0,1)).equals("RETRY")&&writer.processor.created==0,"with no load budget this frame, no region is created");
        newFrame.invoke(null);
        check(run.apply(writer,chunk(2000,0,1)).equals("RETRY")&&writer.processor.created==1&&writer.processor.saveLoad.requested.size()==1,"an unloaded region is created and Xaero is asked to load it");
        Region far=writer.processor.regions.get(List.of(62,0));
        check(far.loadState==0&&far.beingWritten&&far.loadRequested,"marked as being written and requested, like the writer's own loads");
        newFrame.invoke(null);run.apply(writer,chunk(2001,0,1));
        check(writer.processor.saveLoad.requested.size()==1,"a region already requested is not requested again every frame");
        far.loadState=2;
        check(run.apply(writer,chunk(2000,0,1)).equals("APPLIED"),"and once Xaero has loaded it, the chunk is written");
        for(int i=0;i<5;i++){newFrame.invoke(null);run.apply(writer,chunk(3000+i*32,0,1));}
        check(writer.processor.created==5&&writer.processor.saveLoad.requested.size()==5,"at most four regions are waiting to load at once: "+writer.processor.created);
        writer.processor.regions.get(List.of(93,0)).loadState=2;newFrame.invoke(null);run.apply(writer,chunk(3000+4*32,0,1));
        check(writer.processor.created==6,"when one has loaded, the next is requested");
        writer.processor.allowCreate=false;

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
        SharedMapClient.accept(List.of(chunk(0,0,400),chunk(32,0,401),chunk(64,0,402),chunk(96,0,403),chunk(128,0,404)));   // one region each
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(400L,401L,402L)),"three chunks per frame: "+seen);
        check(SharedMapClient.since().get("minecraft:overworld")==400,"only applied chunks advance progress (retry and discard do not)");
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(400L,401L,402L,401L,403L,404L)),"next frame: the waiting chunk gets one place, the rest go to new ones: "+seen);
        check(SharedMapClient.since().get("minecraft:overworld")==400,"progress stays behind the chunk still waiting, though newer ones were written");
        script.put(401L,"APPLIED");
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(400L,401L,402L,401L,403L,404L,401L)),"with nothing new to do, the waiting chunk is tried again at once and written");
        check(SharedMapClient.since().get("minecraft:overworld")==401,"progress now stops just before the chunk that was for another dimension (402), so the server sends it again next time");
        // Chunks of one region wait together: a region that turns one away is not asked again for its others in the same frame.
        seen.clear();script.put(5000L,"RETRY");
        SharedMapClient.accept(List.of(chunk(256,0,5000),chunk(257,0,5001),chunk(258,0,5002),chunk(288,0,5003)));
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(5000L,5003L)),"a region that said no is asked once; the other regions' chunks go on: "+seen);
        script.remove(5000L);seen.clear();
        tick.invoke(null,writer,applierProxy);
        check(seen.equals(List.of(5000L)),"next frame one waiting chunk is tried, and it is the region's first: "+seen);
        for(int i=0;i<8;i++)tick.invoke(null,writer,applierProxy);
        check(seen.containsAll(List.of(5000L,5001L,5002L))&&SharedMapClient.since().get("minecraft:overworld")==401,"once the region is ready its chunks are all written ("+seen+")");
        SharedMapClient.accept(List.of(chunk(9,9,999)));script.put(999L,"BOOM");
        tick.invoke(null,writer,applierProxy);
        SharedMapClient.accept(List.of(chunk(10,9,1000)));int count=seen.size();tick.invoke(null,writer,applierProxy);
        check(seen.size()==count,"after a failure the shared map is off for the session and the game is unaffected");
        SharedMapClient.leave();
        check(Files.readString(file).contains("minecraft\\:overworld=401"),"progress is saved when leaving the server");
        // A region that never becomes ready is given up on: its chunks are owed, saved, and hold nothing else back.
        Path owedFile=Files.createTempDirectory("fan4owed").resolve("owed.properties");
        SharedMapClient.join(owedFile);script.clear();script.put(7000L,"RETRY");
        SharedMapClient.accept(List.of(chunk(256,0,7000),chunk(300,0,7001)));
        tick.invoke(null,writer,applierProxy);
        check(SharedMapClient.since().get("minecraft:overworld")==6999&&SharedMapClient.owed().isEmpty(),"while a chunk waits, progress stays behind it");
        for(int i=0;i<3700;i++)tick.invoke(null,writer,applierProxy);
        check(SharedMapClient.since().get("minecraft:overworld")==7001,"a region stuck for a minute no longer holds the progress back");
        check(SharedMapClient.owed().equals(Map.of("minecraft:overworld",Set.of(SharedMapChunk.key(256,0)))),"the stuck chunk is owed instead");
        SharedMapClient.leave();
        check(Files.readString(owedFile).contains("owed\\ minecraft\\:overworld=256,0,7000"),"owed chunks are saved with the progress");
        SharedMapClient.join(owedFile);
        check(SharedMapClient.owed().equals(Map.of("minecraft:overworld",Set.of(SharedMapChunk.key(256,0))))&&SharedMapClient.since().get("minecraft:overworld")==7001,"and loaded again at the next join");
        script.put(7000L,"APPLIED");SharedMapClient.accept(List.of(chunk(256,0,7000)));tick.invoke(null,writer,applierProxy);
        check(SharedMapClient.owed().isEmpty(),"an owed chunk that is written is no longer owed");
        SharedMapClient.leave();
        SharedMapClient.join(null);SharedMapClient.accept(List.of(new SharedMapChunk("bad",0,0,1,new String[]{"x"},new String[]{"y"},new byte[256],new byte[256],new short[256],new short[256])));
        check(!SharedMapClient.since().containsKey("bad"),"invalid chunks are dropped on receipt");
        System.out.println("PASS: shared map applier mirrors Xaero's write sequence (create/skip/retry/discard cases, coordinates incl. negatives) and the client queue (progress, per-region waiting, owed chunks, retry rationing, failure containment, saved progress)");
    }
}
