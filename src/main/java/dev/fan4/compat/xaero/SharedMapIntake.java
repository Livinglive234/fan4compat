package dev.fan4.compat.xaero;

import java.util.*;
import java.util.function.Predicate;

/**
 * The server's gate for chunks that clients upload. Uploads are unverified, so a chunk is taken only when the mode allows it,
 * the dimension is shared and is the one the player is in, it passes the structural checks and the world-border test, and the
 * player is inside their rate limit. The client's stamp is ignored: the server stamps with its own clock. In BOTH mode an
 * upload only fills places the server holds nothing for, so a client can never overwrite what the server captured itself.
 */
public final class SharedMapIntake {
    public static final int PER_WINDOW=1200;public static final long WINDOW_MS=10_000;
    private final SharedMapMode mode;private final Set<String> allowed;private final SharedMapStore store;
    private final Map<UUID,long[]> windows=new HashMap<>();
    public SharedMapIntake(SharedMapMode mode,Set<String> allowed,SharedMapStore store){this.mode=mode;this.allowed=allowed;this.store=store;}

    /** The chunks that were added to the store, as stored. */
    public synchronized List<SharedMapChunk> accept(UUID player,String playerDimension,List<SharedMapChunk> uploaded,long now,Predicate<SharedMapChunk> insideWorld) {
        List<SharedMapChunk> stored=new ArrayList<>();
        if(!mode.acceptsUploads||!allowed.contains(playerDimension))return stored;
        long[] window=windows.computeIfAbsent(player,id->new long[]{now,0});
        if(now-window[0]>=WINDOW_MS){window[0]=now;window[1]=0;}
        for(SharedMapChunk chunk:uploaded) {
            if(window[1]>=PER_WINDOW)break;
            window[1]++;
            if(!chunk.valid()||!chunk.dimension.equals(playerDimension)||!insideWorld.test(chunk))continue;
            SharedMapChunk stamped=new SharedMapChunk(chunk.dimension,chunk.x,chunk.z,now,chunk.blocks,chunk.biomes,chunk.block,chunk.biome,chunk.bed,chunk.height,chunk.top);
            if(mode.captures?store.putIfAbsent(stamped):store.put(stamped))stored.add(stamped);
        }
        return stored;
    }
    public synchronized void forget(Set<UUID> online){windows.keySet().retainAll(online);}
}
