package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * The server's record of explored terrain, per dimension, in 16x16-chunk region files (deflated batches). Regions load on demand
 * and are written back when evicted or flushed; a damaged region file is set aside rather than overwritten.
 */
public final class SharedMapStore {
    public static final int REGION_SHIFT=4;
    private static final class Region {final Map<Long,SharedMapChunk> chunks=new HashMap<>();boolean dirty;}
    private record Id(String dimension,int x,int z) {}
    private static final int MAX_CHUNKS_PER_REGION=1<<(REGION_SHIFT*2);
    private final Path root;private final int maxLoaded;
    private final LinkedHashMap<Id,Region> loaded=new LinkedHashMap<>(16,0.75f,true);
    public SharedMapStore(Path root,int maxLoadedRegions){this.root=root;this.maxLoaded=Math.max(4,maxLoadedRegions);}
    private static Id id(String dimension,int chunkX,int chunkZ){return new Id(dimension,chunkX>>REGION_SHIFT,chunkZ>>REGION_SHIFT);}
    private Path directory(String dimension){return root.resolve(dimension.replace(':','+').replace('/','%'));}
    private Path file(Id id){return directory(id.dimension).resolve(id.x+"."+id.z+".smc");}

    /** Stores a chunk if its terrain differs from what is held (a newer stamp wins); true when something changed. */
    public synchronized boolean put(SharedMapChunk chunk) {
        if(!chunk.valid())return false;
        Region region=region(id(chunk.dimension,chunk.x,chunk.z));
        SharedMapChunk old=region.chunks.get(chunk.key());
        if(old!=null&&(old.sameTerrain(chunk)||old.stamp>chunk.stamp))return false;
        if(region.chunks.size()>=MAX_CHUNKS_PER_REGION&&old==null)return false;
        region.chunks.put(chunk.key(),chunk);region.dirty=true;
        return true;
    }
    /** Stores a chunk only where nothing is held yet; true when it was added. */
    public synchronized boolean putIfAbsent(SharedMapChunk chunk) {
        return get(chunk.dimension,chunk.x,chunk.z)==null&&put(chunk);
    }
    public synchronized SharedMapChunk get(String dimension,int x,int z){return region(id(dimension,x,z)).chunks.get(SharedMapChunk.key(x,z));}
    /** Every chunk held for one region, in no particular order. */
    public synchronized List<SharedMapChunk> region(String dimension,int regionX,int regionZ) {
        return new ArrayList<>(region(new Id(dimension,regionX,regionZ)).chunks.values());
    }
    /** Regions that exist for a dimension: saved files plus any not yet written. */
    public synchronized List<int[]> regions(String dimension) {
        Set<Long> found=new TreeSet<>();
        Path dir=directory(dimension);
        if(Files.isDirectory(dir))try(Stream<Path> files=Files.list(dir)) {
            for(Path file:files.toList()) {
                String[] parts=file.getFileName().toString().split("\\.");
                if(parts.length==3&&parts[2].equals("smc"))try{found.add(SharedMapChunk.key(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])));}catch(NumberFormatException skipped){}
            }
        } catch(IOException unreadable){}
        for(var entry:loaded.entrySet())if(entry.getKey().dimension.equals(dimension)&&!entry.getValue().chunks.isEmpty())found.add(SharedMapChunk.key(entry.getKey().x,entry.getKey().z));
        List<int[]> result=new ArrayList<>();
        for(long key:found)result.add(new int[]{(int)(key>>32),(int)key});
        return result;
    }
    public synchronized void flush() {
        for(var entry:loaded.entrySet())save(entry.getKey(),entry.getValue());
    }
    private Region region(Id id) {
        Region region=loaded.get(id);
        if(region==null) {
            region=read(id);loaded.put(id,region);
            while(loaded.size()>maxLoaded) {
                var oldest=loaded.entrySet().iterator().next();
                if(oldest.getKey().equals(id))break;
                save(oldest.getKey(),oldest.getValue());loaded.remove(oldest.getKey());
            }
        }
        return region;
    }
    private Region read(Id id) {
        Region region=new Region();Path file=file(id);
        if(!Files.isRegularFile(file))return region;
        try {
            for(SharedMapChunk chunk:SharedMapCodec.decode(Files.readAllBytes(file)))
                if(chunk.dimension.equals(id.dimension)&&(chunk.x>>REGION_SHIFT)==id.x&&(chunk.z>>REGION_SHIFT)==id.z)region.chunks.put(chunk.key(),chunk);
        } catch(IOException|RuntimeException damaged) {
            dev.fan4.compat.shared.GameLog.warn("Shared map region "+file+" is unreadable; starting it empty and keeping a copy",damaged);
            try{Files.move(file,file.resolveSibling(file.getFileName()+".damaged-"+System.currentTimeMillis()),StandardCopyOption.REPLACE_EXISTING);}catch(IOException ignored){}
            return new Region();
        }
        return region;
    }
    private void save(Id id,Region region) {
        if(!region.dirty)return;
        try {
            Path file=file(id);Files.createDirectories(file.getParent());
            Path temp=file.resolveSibling(file.getFileName()+".tmp");
            Files.write(temp,SharedMapCodec.encode(new ArrayList<>(region.chunks.values())));
            dev.fan4.compat.shared.AtomicFiles.replace(temp,file);
            region.dirty=false;
        } catch(IOException failure){dev.fan4.compat.shared.GameLog.warn("Could not save shared map region "+id,failure);}
    }
}
