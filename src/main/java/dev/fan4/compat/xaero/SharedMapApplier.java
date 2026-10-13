package dev.fan4.compat.xaero;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Puts one shared chunk summary into Xaero's World Map the way Xaero's own writer puts in a chunk it has just read: take the
 * region's locks, require it loaded and at rest, find or create the tile chunk, take a tile from the pool, fill its blocks,
 * hand it to the tile chunk and refresh the textures. Tiles are left "not written once", so Xaero's real writer replaces them
 * the first time the player actually loads that chunk. Existing tiles are never touched. Client thread only, from the hook on
 * {@code MapWriter.onRender}.
 */
final class SharedMapApplier {
    enum Result {
        /** A tile was written. */APPLIED,
        /** Xaero already has a tile there; nothing to do. */SKIPPED,
        /** Not possible right now (region not loaded, writing, or busy); try again later. */RETRY,
        /** Does not belong in the map being shown; forget it without recording progress. */DISCARD
    }
    /** Xaero's surface layer: its layer id for a cave start of Integer.MAX_VALUE. */
    static final int SURFACE=Integer.MAX_VALUE;
    private static final String CHUNK="xaero.map.region.MapTileChunk",BLOCK="xaero.map.region.MapBlock",TILE="xaero.map.region.MapTile",
        FAST="xaero.map.region.MapUpdateFastConfig";
    private static final Map<String,Object> STATES=new HashMap<>(),BIOMES=new HashMap<>();
    private SharedMapApplier() {}

    static Result apply(Object writer,SharedMapChunk shared) {
        Object processor=call(writer,"getMapProcessor"),dimension=shownDimension(processor);
        if(dimension==null)return Result.RETRY;
        if(!shared.dimension.equals(String.valueOf(call(dimension,"method_29177"))))return Result.DISCARD;
        int tileX=shared.x>>2,tileZ=shared.z>>2,regionX=shared.x>>5,regionZ=shared.z>>5,localX=tileX&7,localZ=tileZ&7,insideX=shared.x&3,insideZ=shared.z&3;
        Object region=call(processor,"getLeafMapRegion",SURFACE,regionX,regionZ,false);
        // Xaero's own writer creates the region for terrain it is about to write and asks for it to be loaded; a region that is not in
        // memory (terrain nobody has mapped here, or far from the player) is requested the same way, a few at a time.
        if(region==null&&mayRequestLoad())region=call(processor,"getLeafMapRegion",SURFACE,regionX,regionZ,true);
        if(region==null)return Result.RETRY;
        synchronized(field(region,"writerThreadPauseSync")) {
            if((Boolean)call(region,"isWritingPaused"))return Result.RETRY;
            Object chunk;boolean created=false;
            synchronized(region) {
                if(((Number)call(region,"getLoadState")).intValue()!=2){requestLoad(processor,region);return Result.RETRY;}
                call(region,"registerVisit");
                if(!(Boolean)call(region,"isResting"))return Result.RETRY;
                if(!(Boolean)call(region,"isNormalMapData"))return Result.DISCARD;
                call(region,"setBeingWritten",true);
                chunk=call(region,"getChunk",localX,localZ);
                if(chunk==null) {
                    chunk=create(CHUNK,region,tileX,tileZ);
                    call(region,"setChunk",localX,localZ,chunk);
                    call(chunk,"setLoadState",(byte)2);
                    call(region,"setAllCachePrepared",false);
                    created=true;
                }
            }
            if(((Number)call(chunk,"getLoadState")).intValue()!=2||(Boolean)call(call(chunk,"getLeafTexture"),"shouldDownloadFromPBO"))return Result.RETRY;
            if(call(chunk,"getTile",insideX,insideZ)!=null)return Result.SKIPPED;
            Object tile=call(call(processor,"getTilePool"),"get",(String)call(processor,"getCurrentDimension"),shared.x,shared.z);
            call(chunk,"setChanged",true);
            Object overlays=null;
            for(int z=0;z<16;z++)
                for(int x=0;x<16;x++) {
                    int index=shared.index(x,z);
                    Object block=create(BLOCK),biome=biome(shared.biomeAt(x,z));
                    call(block,"prepareForWriting",(int)shared.height[index]);
                    // A fluid is what Xaero itself stores as an overlay (opacity from its depth) on the terrain block, which is the block it records.
                    String fluid=shared.fluidAt(x,z);
                    call(block,"write",state(shared.blockAt(x,z)),(int)shared.height[index],(int)shared.top[index],biome,(byte)15,false,false);
                    if(fluid!=null) {
                        if(overlays==null)overlays=overlayBuilder(processor);
                        call(overlays,"startBuilding");
                        call(overlays,"build",state(fluid),Math.max(1,shared.top[index]-shared.height[index]),(byte)15,processor,biome);
                        call(overlays,"finishBuilding",block);
                    }
                    call(tile,"setBlock",x,z,block);
                }
            call(tile,"setWorldInterpretationVersion",field(type(TILE),"CURRENT_WORLD_INTERPRETATION_VERSION"));
            call(tile,"setWrittenCave",SURFACE,0);
            Object shapes=field(writer,"blockStateShortShapeCache");
            call(chunk,"setTile",insideX,insideZ,tile,shapes,processor);
            call(tile,"setLoaded",true);
            if(created&&(Boolean)call(chunk,"includeInSave"))call(chunk,"setHasHadTerrain");
            if((Boolean)call(chunk,"wasChanged"))
                call(chunk,"updateBuffers",processor,call(processor,"getWorldBlockTintProvider"),call(processor,"getOverlayManager"),false,shapes,create(FAST,processor));
            return Result.APPLIED;
        }
    }
    /** Regions we have asked Xaero to load and that have not reached the loaded state yet. */
    private static final Map<Object,Long> LOADING=new IdentityHashMap<>();
    private static final int MAX_PENDING_LOADS=4;private static final long GIVE_UP_NANOS=30_000_000_000L;
    private static int loadBudget;
    /** Once per frame, from the client queue: how many regions may be asked to load this frame. */
    static void newFrame(){loadBudget=1;}
    /** This frame's budget is not spent and fewer than the limit are still loading. */
    private static boolean mayRequestLoad(){return loadBudget>0&&pendingLoads()<MAX_PENDING_LOADS;}
    /** A new connection: forget regions of the old one. */
    static void reset(){LOADING.clear();overlayBuilder=null;overlayManager=null;}
    private static int pendingLoads() {
        // A region that has loaded, or that has not in half a minute (so one stuck load cannot block every other), stops counting.
        long now=System.nanoTime();
        LOADING.entrySet().removeIf(entry->((Number)call(entry.getKey(),"getLoadState")).intValue()==2||now-entry.getValue()>GIVE_UP_NANOS);
        return LOADING.size();
    }
    /** What Xaero's writer does for a region it needs that is not loaded: mark it being written and queue the load. */
    private static void requestLoad(Object processor,Object region) {
        if(!mayRequestLoad())return;
        synchronized(region) {
            if(!(Boolean)call(region,"isResting")||!(Boolean)call(region,"canRequestReload_unsynced")||((Number)call(region,"getLoadState")).intValue()==2)return;
            call(region,"setBeingWritten",true);
            call(call(processor,"getMapSaveLoad"),"requestLoad",region,"Fan4Compat shared map");
            LOADING.put(region,System.nanoTime());loadBudget--;
        }
    }
    private static Object overlayManager,overlayBuilder;
    /** Xaero's own helper for stacking overlays on a block, over the processor's overlay manager (so identical overlays are shared). */
    private static Object overlayBuilder(Object processor) {
        Object manager=call(processor,"getOverlayManager");
        if(overlayBuilder==null||manager!=overlayManager){overlayManager=manager;overlayBuilder=create("xaero.map.region.OverlayBuilder",manager);}
        return overlayBuilder;
    }
    /** The key of the dimension Xaero's map is showing, or null before it has one. */
    static Object shownDimension(Object processor) {
        Object mapWorld=processor==null?null:call(processor,"getMapWorld");
        return mapWorld==null?null:call(mapWorld,"getCurrentDimensionId");
    }
    private static Object state(String id) {
        return STATES.computeIfAbsent(id,key->call(call(field(type("net.minecraft.class_7923"),"field_41175"),"method_10223",call(type("net.minecraft.class_2960"),"method_60654",key)),"method_9564"));
    }
    private static Object biome(String id) {
        return BIOMES.computeIfAbsent(id,key->call(type("net.minecraft.class_5321"),"method_29179",field(type("net.minecraft.class_7924"),"field_41236"),call(type("net.minecraft.class_2960"),"method_60654",key)));
    }
}
