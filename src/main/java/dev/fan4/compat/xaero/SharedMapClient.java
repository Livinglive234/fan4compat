package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Client side of the shared map: chunk summaries received from the server wait here and are written into Xaero's map a few per
 * frame by {@link #tick}. Progress per dimension is saved so the server can send only what is new, and it never moves past a
 * chunk that has not been written: the server resends everything newer than the progress it is told, so a chunk that was dropped,
 * is still waiting for its region to load, or belonged to a dimension that was not shown must hold the progress back.
 */
public final class SharedMapClient {
    private static final int MAX_QUEUED=4096,PER_FRAME=3,FLUSH_EVERY_FRAMES=1800,STATS_EVERY_FRAMES=600;
    private static final ArrayDeque<SharedMapChunk> incoming=new ArrayDeque<>(),waiting=new ArrayDeque<>();
    /** Newest stamp written per dimension; stamps of queued chunks per dimension; and the lowest point a lost chunk pins progress to. */
    private static final Map<String,Long> applied=new HashMap<>(),lost=new HashMap<>();
    private static final Map<String,TreeMap<Long,Integer>> unsettled=new HashMap<>();
    private static final int[] RESULTS=new int[SharedMapApplier.Result.values().length];
    private static Path stateFile;private static int frame;private static boolean dirty,warned,disabled;
    private SharedMapClient() {}

    /** Connection joined: load the progress saved for this server. */
    public static synchronized void join(Path file) {
        incoming.clear();waiting.clear();applied.clear();lost.clear();unsettled.clear();Arrays.fill(RESULTS,0);stateFile=file;dirty=false;disabled=false;SharedMapUploader.reset();SharedMapApplier.reset();
        if(file!=null&&Files.isRegularFile(file)) {
            Properties saved=new Properties();
            try(var in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){saved.load(in);}catch(IOException|IllegalArgumentException unreadable){return;}
            for(String key:saved.stringPropertyNames())try{if(SharedMapChunk.validId(key))applied.put(key,Long.parseLong(saved.getProperty(key)));}catch(NumberFormatException skipped){}
        }
    }
    public static synchronized void leave(){flush();incoming.clear();waiting.clear();unsettled.clear();stateFile=null;SharedMapUploader.reset();SharedMapApplier.reset();}
    /** What to tell the server: for each dimension, the newest stamp such that every chunk up to it has been written. */
    public static synchronized Map<String,Long> since() {
        Map<String,Long> progress=new HashMap<>(applied);
        for(var entry:unsettled.entrySet())if(!entry.getValue().isEmpty())progress.merge(entry.getKey(),entry.getValue().firstKey()-1,Math::min);
        for(var entry:lost.entrySet())progress.merge(entry.getKey(),entry.getValue(),Math::min);
        return progress;
    }
    private static void hold(SharedMapChunk chunk){unsettled.computeIfAbsent(chunk.dimension,d->new TreeMap<>()).merge(chunk.stamp,1,Integer::sum);}
    private static void release(SharedMapChunk chunk) {
        var stamps=unsettled.get(chunk.dimension);Integer held=stamps==null?null:stamps.get(chunk.stamp);
        if(held==null)return;
        if(held<=1)stamps.remove(chunk.stamp);else stamps.put(chunk.stamp,held-1);
    }
    /** The chunk will not be written (queue full, or not for the dimension shown): the server must send it again next time. */
    private static void lose(SharedMapChunk chunk){release(chunk);lost.merge(chunk.dimension,chunk.stamp-1,Math::min);dirty=true;}

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
        if(disabled||(incoming.isEmpty()&&waiting.isEmpty()))return;
        frame++;
        SharedMapApplier.newFrame();
        try {
            // New chunks first, but one place per frame goes to the ones waiting for their region, so they cannot starve.
            // When nothing new is arriving they get up to a full frame's share, but only every fourth frame, so a chunk that can never be written costs little.
            int retries=Math.min(waiting.size(),incoming.isEmpty()&&frame%4==0?PER_FRAME:1);
            for(int tries=retries;tries>0;tries--) {
                SharedMapChunk chunk=waiting.poll();
                if(settle(chunk,applier.apply(writer,chunk)))waiting.add(chunk);
            }
            for(int done=retries;done<PER_FRAME&&!incoming.isEmpty();done++) {
                SharedMapChunk chunk=incoming.poll();
                if(settle(chunk,applier.apply(writer,chunk))) {if(waiting.size()<MAX_QUEUED)waiting.add(chunk);else lose(chunk);}
            }
            // Save progress once a batch has been worked off (the frame counter only runs while there is work), or now and then.
            if(dirty&&incoming.isEmpty()&&waiting.isEmpty()||frame%FLUSH_EVERY_FRAMES==0)flush();
            if(frame%STATS_EVERY_FRAMES==0)XaeroTrace.event(()->"shared map client: "+incoming.size()+" queued, "+waiting.size()+" waiting for a region; written "
                +RESULTS[SharedMapApplier.Result.APPLIED.ordinal()]+", already there "+RESULTS[SharedMapApplier.Result.SKIPPED.ordinal()]+", other dimension "
                +RESULTS[SharedMapApplier.Result.DISCARD.ordinal()]+"; progress "+since());
        } catch(RuntimeException|LinkageError failure) {
            // One bad chunk or an Xaero change must not break the map: stop applying for this launch.
            incoming.clear();waiting.clear();unsettled.clear();disabled=true;
            if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("Shared map disabled for this session after an error",failure);}
        }
    }
    /** Records what became of one chunk; true when it has to wait and be tried again. */
    private static boolean settle(SharedMapChunk chunk,SharedMapApplier.Result result) {
        RESULTS[result.ordinal()]++;
        if(result==SharedMapApplier.Result.RETRY)return true;
        if(result==SharedMapApplier.Result.DISCARD){lose(chunk);return false;}
        release(chunk);applied.merge(chunk.dimension,chunk.stamp,Math::max);dirty=true;
        if(result==SharedMapApplier.Result.APPLIED)SharedMapUploader.known(chunk);
        return false;
    }
    private static void flush() {
        if(!dirty||stateFile==null)return;
        Properties out=new Properties();for(var e:since().entrySet())out.setProperty(e.getKey(),String.valueOf(e.getValue()));
        try {
            Files.createDirectories(stateFile.getParent());
            Path temp=stateFile.resolveSibling(stateFile.getFileName()+".tmp");
            try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){out.store(writer,"Fan4Compat shared map progress");}
            dev.fan4.compat.shared.AtomicFiles.replace(temp,stateFile);dirty=false;
        } catch(IOException failure){dev.fan4.compat.shared.GameLog.warn("Could not save shared map progress",failure);}
    }
}
