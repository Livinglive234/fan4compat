package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Client side of the shared map: chunk summaries received from the server wait here and are written into Xaero's map a few per
 * frame by {@link #tick}. Progress (the newest stamp applied per dimension) is saved so the server can send only what is new.
 */
public final class SharedMapClient {
    private static final int MAX_QUEUED=4096,PER_FRAME=3,RETRY_EVERY_FRAMES=60,FLUSH_EVERY_FRAMES=1800;
    private static final ArrayDeque<SharedMapChunk> incoming=new ArrayDeque<>(),waiting=new ArrayDeque<>();
    private static final Map<String,Long> since=new HashMap<>();
    private static Path stateFile;private static int frame;private static boolean dirty,warned,disabled;
    private SharedMapClient() {}

    /** Connection joined: load the progress saved for this server. */
    public static synchronized void join(Path file) {
        incoming.clear();waiting.clear();since.clear();stateFile=file;dirty=false;disabled=false;SharedMapUploader.reset();
        if(file!=null&&Files.isRegularFile(file)) {
            Properties saved=new Properties();
            try(var in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){saved.load(in);}catch(IOException|IllegalArgumentException unreadable){return;}
            for(String key:saved.stringPropertyNames())try{if(SharedMapChunk.validId(key))since.put(key,Long.parseLong(saved.getProperty(key)));}catch(NumberFormatException skipped){}
        }
    }
    public static synchronized void leave(){flush();incoming.clear();waiting.clear();stateFile=null;SharedMapUploader.reset();}
    public static synchronized Map<String,Long> since(){return new HashMap<>(since);}

    /** Called on the network thread's behalf from the receive handler. */
    public static synchronized void accept(List<SharedMapChunk> chunks) {
        if(disabled)return;
        for(SharedMapChunk chunk:chunks) {
            if(!chunk.valid())continue;
            if(incoming.size()>=MAX_QUEUED)incoming.poll();
            incoming.add(chunk);
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
        try {
            for(int done=0;done<PER_FRAME&&!incoming.isEmpty();done++) {
                SharedMapChunk chunk=incoming.poll();
                if(settle(chunk,applier.apply(writer,chunk))&&waiting.size()<MAX_QUEUED)waiting.add(chunk);
            }
            // Chunks whose region was not loaded yet get another look now and then (the player pans the map, regions load).
            if(frame%RETRY_EVERY_FRAMES==0)
                for(int tries=Math.min(PER_FRAME*4,waiting.size());tries>0;tries--) {
                    SharedMapChunk chunk=waiting.poll();
                    if(settle(chunk,applier.apply(writer,chunk)))waiting.add(chunk);
                }
            // Save progress once a batch has been worked off (the frame counter only runs while there is work), or now and then.
            if(dirty&&incoming.isEmpty()&&waiting.isEmpty()||frame%FLUSH_EVERY_FRAMES==0)flush();
        } catch(RuntimeException|LinkageError failure) {
            // One bad chunk or an Xaero change must not break the map: stop applying for this launch.
            incoming.clear();waiting.clear();disabled=true;
            if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("Shared map disabled for this session after an error",failure);}
        }
    }
    /** Records what became of one chunk; true when it has to wait and be tried again. */
    private static boolean settle(SharedMapChunk chunk,SharedMapApplier.Result result) {
        if(result==SharedMapApplier.Result.RETRY)return true;
        if(result==SharedMapApplier.Result.DISCARD)return false;
        since.merge(chunk.dimension,chunk.stamp,Math::max);dirty=true;
        if(result==SharedMapApplier.Result.APPLIED)SharedMapUploader.known(chunk);
        return false;
    }
    private static void flush() {
        if(!dirty||stateFile==null)return;
        Properties out=new Properties();for(var e:since.entrySet())out.setProperty(e.getKey(),String.valueOf(e.getValue()));
        try {
            Files.createDirectories(stateFile.getParent());
            Path temp=stateFile.resolveSibling(stateFile.getFileName()+".tmp");
            try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){out.store(writer,"Fan4Compat shared map progress");}
            dev.fan4.compat.shared.AtomicFiles.replace(temp,stateFile);dirty=false;
        } catch(IOException failure){dev.fan4.compat.shared.GameLog.warn("Could not save shared map progress",failure);}
    }
}
