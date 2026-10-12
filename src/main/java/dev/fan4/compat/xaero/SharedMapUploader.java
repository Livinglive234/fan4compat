package dev.fan4.compat.xaero;

import java.util.*;
import java.util.function.Consumer;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Client side of uploads (modes CLIENT and BOTH): sweeps the regions Xaero has loaded around the player, a few tiles per frame,
 * summarizes tiles the server has not been given in that form, and sends them in small batches. The sweep reads only Xaero's
 * in-memory surface tiles, never the disk, so it covers what the player has mapped near where they are (including old map data
 * Xaero loads from its files as they come near). Idle until the server says uploads are wanted. Render thread only.
 */
final class SharedMapUploader {
    private static final int SURFACE=SharedMapApplier.SURFACE,SWEEP_RADIUS=6,PROBES_PER_FRAME=128,READS_PER_FRAME=4,SEND_EVERY_FRAMES=40,BATCH=64,MAX_OUTGOING=2048,MAX_REMEMBERED=1<<17,REST_FRAMES=600;
    /** Where the player really is, for the sweep and for refusing to upload a map dimension that is not the world they are in. */
    record Where(String dimension,int chunkX,int chunkZ) {}
    private static volatile SharedMapMode mode=SharedMapMode.NORMAL;
    /** For each dimension, the fingerprint of what was last sent or supplied for each chunk, so unchanged terrain is not read into a batch again. */
    private static final Map<String,Map<Long,Long>> sent=new HashMap<>();
    private static final ArrayDeque<SharedMapChunk> outgoing=new ArrayDeque<>();
    private static final List<int[]> sweep=new ArrayList<>();
    private static int sweepRegion,sweepPosition,frame,nextSweep;private static boolean failed;
    private SharedMapUploader() {}

    static void serverMode(SharedMapMode told){mode=told;}
    static synchronized void reset(){mode=SharedMapMode.NORMAL;sent.clear();outgoing.clear();sweep.clear();sweepRegion=0;sweepPosition=0;frame=0;nextSweep=0;failed=false;}
    /** A chunk the server just gave us needs no upload. */
    static synchronized void known(SharedMapChunk chunk){remember(chunk);}
    private static void remember(SharedMapChunk chunk) {
        Map<Long,Long> known=sent.computeIfAbsent(chunk.dimension,d->new HashMap<>());
        if(known.size()>=MAX_REMEMBERED)known.clear();
        known.put(chunk.key(),chunk.contentHash());
    }
    private static boolean unchanged(SharedMapChunk chunk){Map<Long,Long> known=sent.get(chunk.dimension);return known!=null&&Long.valueOf(chunk.contentHash()).equals(known.get(chunk.key()));}

    /** Entry point from the hook on Xaero's map writer. Never throws. */
    static void tick(Object writer,Consumer<List<SharedMapChunk>> sender){
        if(!mode.acceptsUploads||failed||resting())return;
        tick(writer,sender,SharedMapBridge.where());
    }
    /** Between sweeps with nothing waiting to be sent there is nothing to look up this frame. */
    private static synchronized boolean resting() {
        if(sweepRegion<sweep.size()||frame>=nextSweep||!outgoing.isEmpty())return false;
        frame++;return true;
    }
    static synchronized void tick(Object writer,Consumer<List<SharedMapChunk>> sender,Where where) {
        if(!mode.acceptsUploads||failed||where==null)return;
        try {
            frame++;
            Object processor=call(writer,"getMapProcessor"),dimension=SharedMapApplier.shownDimension(processor);
            // Inside a TARDIS Xaero shows the exterior's dimension; those tiles are not this world's terrain, so leave them out.
            if(dimension==null||!where.dimension().equals(String.valueOf(call(dimension,"method_29177"))))return;
            scan(processor,where);
            if(frame%SEND_EVERY_FRAMES==0&&!outgoing.isEmpty()) {
                List<SharedMapChunk> batch=new ArrayList<>();
                while(batch.size()<BATCH&&!outgoing.isEmpty())batch.add(outgoing.poll());
                sender.accept(batch);
            }
        } catch(RuntimeException|LinkageError failure) {
            failed=true;outgoing.clear();
            dev.fan4.compat.shared.GameLog.warn("Shared map uploads disabled for this session after an error",failure);
        }
    }
    private static void scan(Object processor,Where where) {
        if(sweepRegion>=sweep.size()) {
            if(frame<nextSweep)return;
            sweep.clear();sweepRegion=0;sweepPosition=0;
            int centerX=where.chunkX()>>5,centerZ=where.chunkZ()>>5;
            for(int dz=-SWEEP_RADIUS;dz<=SWEEP_RADIUS;dz++)for(int dx=-SWEEP_RADIUS;dx<=SWEEP_RADIUS;dx++)sweep.add(new int[]{centerX+dx,centerZ+dz});
            sweep.sort(Comparator.comparingInt(r->(r[0]-centerX)*(r[0]-centerX)+(r[1]-centerZ)*(r[1]-centerZ)));
        }
        int probes=PROBES_PER_FRAME,reads=READS_PER_FRAME;
        while(probes>0&&reads>0&&sweepRegion<sweep.size()) {
            int[] at=sweep.get(sweepRegion);
            Object region=call(processor,"getLeafMapRegion",SURFACE,at[0],at[1],false);
            if(region==null||((Number)call(region,"getLoadState")).intValue()!=2||!(Boolean)call(region,"isNormalMapData")){sweepRegion++;sweepPosition=0;probes--;continue;}
            // 64 tile chunks of 4x4 chunks each; skip a whole tile chunk at once when Xaero has none there.
            int tileChunk=sweepPosition>>4,slot=sweepPosition&15;
            Object chunk=call(region,"getChunk",tileChunk&7,tileChunk>>3);
            probes--;
            if(chunk==null||((Number)call(chunk,"getLoadState")).intValue()!=2){advance(16-slot);continue;}
            Object tile=call(chunk,"getTile",slot&3,slot>>2);
            if(tile!=null) {
                reads--;
                int chunkX=at[0]*32+(tileChunk&7)*4+(slot&3),chunkZ=at[1]*32+(tileChunk>>3)*4+(slot>>2);
                SharedMapChunk summary=SharedMapReader.read(tile,where.dimension(),chunkX,chunkZ);
                if(summary!=null&&!unchanged(summary)) {
                    remember(summary);
                    if(outgoing.size()>=MAX_OUTGOING)outgoing.poll();
                    outgoing.add(summary);
                }
            }
            advance(1);
        }
        // A finished pass rests for a while: the tiles only change as the player explores.
        if(sweepRegion>=sweep.size())nextSweep=frame+REST_FRAMES;
    }
    private static void advance(int by) {
        sweepPosition+=by;
        if(sweepPosition>=1024){sweepPosition=0;sweepRegion++;}
    }
}
