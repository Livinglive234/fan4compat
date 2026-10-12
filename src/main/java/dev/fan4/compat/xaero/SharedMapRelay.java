package dev.fan4.compat.xaero;

import java.util.*;

/**
 * What one player is owed: chunks stored for their dimension that are newer than what their client already has (the client
 * reports, per dimension, the newest stamp it applied), nearest first, then live changes as they are captured. Sent in small
 * batches so a big world cannot flood a connection.
 */
public final class SharedMapRelay {
    private static final int MAX_LIVE=2048;
    private final SharedMapStore store;private final Map<String,Long> since;
    private String dimension;private int chunkX,chunkZ;
    private final Deque<int[]> regions=new ArrayDeque<>();private final Deque<SharedMapChunk> loaded=new ArrayDeque<>();
    private final Deque<SharedMapChunk> live=new ArrayDeque<>();private boolean liveOverflowed;
    public SharedMapRelay(SharedMapStore store,Map<String,Long> since){this.store=store;this.since=new HashMap<>(since);}

    /** The player is in this dimension at this chunk: start (or keep) catching up for it. */
    public synchronized void moved(String dimension,int chunkX,int chunkZ) {
        boolean changedDimension=!dimension.equals(this.dimension);
        this.chunkX=chunkX;this.chunkZ=chunkZ;
        if(!changedDimension)return;
        this.dimension=dimension;loaded.clear();live.clear();liveOverflowed=false;regions.clear();
        List<int[]> all=store.regions(dimension);
        int playerRegionX=chunkX>>SharedMapStore.REGION_SHIFT,playerRegionZ=chunkZ>>SharedMapStore.REGION_SHIFT;
        all.sort(Comparator.comparingLong(r->(long)(r[0]-playerRegionX)*(r[0]-playerRegionX)+(long)(r[1]-playerRegionZ)*(r[1]-playerRegionZ)));
        regions.addAll(all);
    }
    /** A chunk was captured or changed on the server. */
    public synchronized void changed(SharedMapChunk chunk) {
        if(dimension==null||!dimension.equals(chunk.dimension))return;
        if(live.size()>=MAX_LIVE){liveOverflowed=true;return;}
        live.add(chunk);
    }
    /** The next chunks to send, at most {@code max}; empty when the player is up to date. */
    public synchronized List<SharedMapChunk> next(int max) {
        List<SharedMapChunk> batch=new ArrayList<>();
        long floor=since.getOrDefault(dimension==null?"":dimension,0L);
        while(batch.size()<max&&!live.isEmpty())batch.add(live.poll());
        while(batch.size()<max) {
            if(loaded.isEmpty()) {
                int[] region=regions.poll();
                if(region==null) {
                    // A burst of changes overflowed the live queue: go back through the stored chunks newer than the player's floor.
                    if(liveOverflowed&&dimension!=null){liveOverflowed=false;String d=dimension;dimension=null;moved(d,chunkX,chunkZ);continue;}
                    break;
                }
                List<SharedMapChunk> fresh=new ArrayList<>();
                for(SharedMapChunk c:store.region(dimension,region[0],region[1]))if(c.stamp>floor)fresh.add(c);
                fresh.sort(Comparator.comparingLong(c->(long)(c.x-chunkX)*(c.x-chunkX)+(long)(c.z-chunkZ)*(c.z-chunkZ)));
                loaded.addAll(fresh);
                continue;
            }
            batch.add(loaded.poll());
        }
        return batch;
    }
    public synchronized boolean idle(){return live.isEmpty()&&loaded.isEmpty()&&regions.isEmpty();}
}
