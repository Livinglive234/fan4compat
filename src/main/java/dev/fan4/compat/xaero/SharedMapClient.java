package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Client side of the shared map: chunk summaries received from the server wait here and are written into Xaero's map a few per
 * frame by {@link #tick}. Chunks whose region is not ready wait grouped by region, so one unready region costs one attempt, not
 * one per chunk. Progress per dimension is saved so the server can send only what is new, and it never moves past a chunk that
 * has not been written, except for chunks owed individually: a group that stays stuck for a minute is given up for this session
 * and its chunks are recorded as owed, which the server sends again at the next join however old they are. That way one region
 * that cannot be written holds nothing else back.
 */
public final class SharedMapClient {
    private static final int MAX_QUEUED=4096,PER_FRAME=3,FLUSH_EVERY_FRAMES=1800,STATS_EVERY_FRAMES=600,GIVE_UP_FRAMES=3600,CHECK_EVERY_FRAMES=60;
    private static final ArrayDeque<SharedMapChunk> incoming=new ArrayDeque<>();
    /** Chunks waiting for one region of one dimension, and the frame that group last made progress. */
    private record Region(String dimension,int x,int z){}
    private static final class Group{final ArrayDeque<SharedMapChunk> chunks=new ArrayDeque<>();int progressed=frame;}
    private static final Map<Region,Group> waiting=new LinkedHashMap<>();
    private static final Set<Region> blocked=new HashSet<>();
    private static int waitingCount;
    /** Chunks given up on for now, per dimension, oldest first: chunk key to stamp. The server sends these again whatever the progress. */
    private static final Map<String,LinkedHashMap<Long,Long>> owed=new HashMap<>();
    private static final int MAX_OWED=SharedMapCodec.MAX_OWED;
    /** Newest stamp written per dimension; stamps of queued chunks per dimension; and the lowest point a lost chunk pins progress to. */
    private static final Map<String,Long> applied=new HashMap<>(),lost=new HashMap<>();
    private static final Map<String,TreeMap<Long,Integer>> unsettled=new HashMap<>();
    private static final int[] RESULTS=new int[SharedMapApplier.Result.values().length];
    private static Path stateFile;private static int frame;private static boolean dirty,warned,disabled;
    private SharedMapClient() {}

    /** Connection joined: load the progress saved for this server. */
    public static synchronized void join(Path file) {
        incoming.clear();clearWaiting();applied.clear();lost.clear();unsettled.clear();owed.clear();Arrays.fill(RESULTS,0);stateFile=file;dirty=false;disabled=false;SharedMapUploader.reset();SharedMapApplier.reset();
        if(file!=null&&Files.isRegularFile(file)) {
            Properties saved=new Properties();
            try(var in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){saved.load(in);}catch(IOException|IllegalArgumentException unreadable){return;}
            for(String key:saved.stringPropertyNames())
                try {
                    if(key.startsWith(OWED_PREFIX)){loadOwed(key.substring(OWED_PREFIX.length()),saved.getProperty(key));continue;}
                    if(SharedMapChunk.validId(key))applied.put(key,Long.parseLong(saved.getProperty(key)));
                } catch(NumberFormatException skipped){}
        }
    }
    public static synchronized void leave(){flush();incoming.clear();clearWaiting();unsettled.clear();owed.clear();stateFile=null;SharedMapUploader.reset();SharedMapApplier.reset();}
    private static void clearWaiting(){waiting.clear();blocked.clear();waitingCount=0;}
    /** What to tell the server: for each dimension, the newest stamp such that every chunk up to it has been written. */
    public static synchronized Map<String,Long> since() {
        Map<String,Long> progress=new HashMap<>(applied);
        for(var entry:unsettled.entrySet())if(!entry.getValue().isEmpty())progress.merge(entry.getKey(),entry.getValue().firstKey()-1,Math::min);
        for(var entry:lost.entrySet())progress.merge(entry.getKey(),entry.getValue(),Math::min);
        return progress;
    }
    /** What to tell the server besides the progress: chunks it must send again whatever their stamp. */
    public static synchronized Map<String,Set<Long>> owed() {
        Map<String,Set<Long>> keys=new HashMap<>();
        for(var entry:owed.entrySet())if(!entry.getValue().isEmpty())keys.put(entry.getKey(),new LinkedHashSet<>(entry.getValue().keySet()));
        return keys;
    }
    private static void hold(SharedMapChunk chunk){unsettled.computeIfAbsent(chunk.dimension,d->new TreeMap<>()).merge(chunk.stamp,1,Integer::sum);}
    private static void release(SharedMapChunk chunk) {
        var stamps=unsettled.get(chunk.dimension);Integer held=stamps==null?null:stamps.get(chunk.stamp);
        if(held==null)return;
        if(held<=1)stamps.remove(chunk.stamp);else stamps.put(chunk.stamp,held-1);
    }
    /** The chunk will not be written (queue full, or not for the dimension shown): the server must send it again next time. */
    private static void lose(SharedMapChunk chunk){release(chunk);lost.merge(chunk.dimension,chunk.stamp-1,Math::min);dirty=true;}

    /** The chunk is not going to be written in this session, but the server has to send it again next time, whatever the progress. */
    private static void owe(SharedMapChunk chunk) {
        var chunks=owed.computeIfAbsent(chunk.dimension,d->new LinkedHashMap<>());
        chunks.merge(chunk.key(),chunk.stamp,Math::max);
        if(chunks.size()>MAX_OWED) {
            // Too many to list: the oldest falls back to holding the progress back, which is the same promise at a coarser grain.
            var oldest=chunks.entrySet().iterator().next();
            lost.merge(chunk.dimension,oldest.getValue()-1,Math::min);chunks.remove(oldest.getKey());
        }
    }
    private static Region regionOf(SharedMapChunk chunk){return new Region(chunk.dimension,chunk.x>>5,chunk.z>>5);}
    private static void addWaiting(SharedMapChunk chunk) {
        waiting.computeIfAbsent(regionOf(chunk),r->new Group()).chunks.add(chunk);waitingCount++;
    }

    /** Called on the network thread's behalf from the receive handler. */
    public static synchronized void accept(List<SharedMapChunk> chunks) {
        if(disabled)return;
        for(SharedMapChunk chunk:chunks) {
            if(!chunk.valid())continue;
            if(incoming.size()>=MAX_QUEUED)lose(incoming.poll());
            incoming.add(chunk);hold(chunk);
        }
    }
    interface Applier{SharedMapApplier.Result apply(Object writer,SharedMapChunk chunk);}
    /** Entry point for the hook on Xaero's map writer. */
    public static void tick(Object writer){
        tick(writer,SharedMapApplier::apply);
        SharedMapUploader.tick(writer,SharedMapBridge::upload);
    }
    /** Called every frame from the hook on Xaero's writer. Never throws. */
    static synchronized void tick(Object writer,Applier applier) {
        if(disabled||(incoming.isEmpty()&&waitingCount==0))return;
        frame++;
        SharedMapApplier.newFrame();blocked.clear();
        try {
            // New chunks first, but one place per frame goes to the ones waiting for their region, so they cannot starve.
            // When nothing new is arriving they get up to a full frame's share, but only every fourth frame, so a chunk that can never be written costs little.
            int retries=Math.min(waitingCount,incoming.isEmpty()&&frame%4==0?PER_FRAME:1),placed=retry(writer,applier,retries);
            while(placed<PER_FRAME&&!incoming.isEmpty()) {
                SharedMapChunk chunk=incoming.poll();
                // A region that just turned a chunk away will do the same for the rest of its chunks this frame.
                if(blocked.contains(regionOf(chunk))){wait(chunk);continue;}
                placed++;
                SharedMapApplier.Result result=applier.apply(writer,chunk);
                if(settle(chunk,result)){wait(chunk);blocked.add(regionOf(chunk));}
            }
            if(frame%CHECK_EVERY_FRAMES==0)giveUpOnStuckRegions();
            // Save progress once a batch has been worked off (the frame counter only runs while there is work), or now and then.
            if(dirty&&incoming.isEmpty()&&waitingCount==0||frame%FLUSH_EVERY_FRAMES==0)flush();
            if(frame%STATS_EVERY_FRAMES==0)XaeroTrace.event(()->"shared map client: "+incoming.size()+" queued, "+waitingCount+" waiting in "+waiting.size()+" regions; written "
                +RESULTS[SharedMapApplier.Result.APPLIED.ordinal()]+", already there "+RESULTS[SharedMapApplier.Result.SKIPPED.ordinal()]+", other dimension "
                +RESULTS[SharedMapApplier.Result.DISCARD.ordinal()]+"; owed "+owed().values().stream().mapToInt(Set::size).sum()+"; progress "+since());
        } catch(RuntimeException|LinkageError failure) {
            // One bad chunk or an Xaero change must not break the map: stop applying for this launch.
            incoming.clear();clearWaiting();unsettled.clear();disabled=true;
            if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("Shared map disabled for this session after an error",failure);}
        }
    }
    private static void wait(SharedMapChunk chunk){if(waitingCount<MAX_QUEUED)addWaiting(chunk);else lose(chunk);}
    /** Tries up to {@code attempts} waiting chunks, region by region; a region that turns one away is not tried again this frame and goes to the back of the line. Returns the attempts made. */
    private static int retry(Object writer,Applier applier,int attempts) {
        int used=0;List<Region> stuck=new ArrayList<>();
        for(Region region:new ArrayList<>(waiting.keySet())) {
            if(used>=attempts)break;
            Group group=waiting.get(region);
            while(used<attempts&&!group.chunks.isEmpty()) {
                used++;
                SharedMapChunk chunk=group.chunks.peek();
                if(settle(chunk,applier.apply(writer,chunk))){blocked.add(region);stuck.add(region);break;}
                group.chunks.poll();waitingCount--;group.progressed=frame;
            }
            if(group.chunks.isEmpty())waiting.remove(region);
        }
        for(Region region:stuck){Group group=waiting.remove(region);if(group!=null)waiting.put(region,group);}
        return used;
    }
    /** A region that has taken nothing for a minute is not coming: its chunks are owed to a later session instead of holding the progress back. */
    private static void giveUpOnStuckRegions() {
        for(var entries=waiting.entrySet().iterator();entries.hasNext();) {
            Group group=entries.next().getValue();
            if(frame-group.progressed<GIVE_UP_FRAMES)continue;
            for(SharedMapChunk chunk:group.chunks){release(chunk);owe(chunk);}
            waitingCount-=group.chunks.size();entries.remove();dirty=true;
        }
    }
    /** Records what became of one chunk; true when it has to wait and be tried again. */
    private static boolean settle(SharedMapChunk chunk,SharedMapApplier.Result result) {
        RESULTS[result.ordinal()]++;
        if(result==SharedMapApplier.Result.RETRY)return true;
        if(result==SharedMapApplier.Result.DISCARD){lose(chunk);return false;}
        release(chunk);applied.merge(chunk.dimension,chunk.stamp,Math::max);dirty=true;
        var pending=owed.get(chunk.dimension);if(pending!=null)pending.remove(chunk.key());
        if(result==SharedMapApplier.Result.APPLIED)SharedMapUploader.known(chunk);
        return false;
    }
    private static final String OWED_PREFIX="owed ";
    private static void loadOwed(String dimension,String list) {
        if(!SharedMapChunk.validId(dimension))return;
        for(String entry:list.split(";")) {
            String[] part=entry.split(",");
            if(part.length!=3||owed.computeIfAbsent(dimension,d->new LinkedHashMap<>()).size()>=MAX_OWED)continue;
            try{owed.get(dimension).put(SharedMapChunk.key(Integer.parseInt(part[0]),Integer.parseInt(part[1])),Long.parseLong(part[2]));}catch(NumberFormatException skipped){}
        }
    }
    private static void flush() {
        if(!dirty||stateFile==null)return;
        Properties out=new Properties();for(var e:since().entrySet())out.setProperty(e.getKey(),String.valueOf(e.getValue()));
        for(var e:owed.entrySet())if(!e.getValue().isEmpty()) {
            StringJoiner chunks=new StringJoiner(";");
            for(var chunk:e.getValue().entrySet())chunks.add((int)(chunk.getKey()>>32)+","+(int)(long)chunk.getKey()+","+chunk.getValue());
            out.setProperty(OWED_PREFIX+e.getKey(),chunks.toString());
        }
        try {
            Files.createDirectories(stateFile.getParent());
            Path temp=stateFile.resolveSibling(stateFile.getFileName()+".tmp");
            try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){out.store(writer,"Fan4Compat shared map progress");}
            dev.fan4.compat.shared.AtomicFiles.replace(temp,stateFile);dirty=false;
        } catch(IOException failure){dev.fan4.compat.shared.GameLog.warn("Could not save shared map progress",failure);}
    }
}
