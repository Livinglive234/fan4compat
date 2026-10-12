package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/**
 * Client-side behavior for one server folder, free of Minecraft types. {@link #sync} runs while the join is
 * held (before Xaero reads its files); {@link #changes} finds later edits to upload.
 */
public final class WaypointClient {
    private final Path folder,stateFile,backups;private final int maxFileBytes;private final LongSupplier clock;
    private final Map<String,WaypointSyncPlan.State> state=new HashMap<>();private final Map<String,String> uploaded=new HashMap<>();
    public WaypointClient(Path serverFolder,Path stateFile,Path backupFolder,int maxFileBytes,LongSupplier clock) {
        this.folder=serverFolder;this.stateFile=stateFile;this.backups=backupFolder;this.maxFileBytes=maxFileBytes;this.clock=clock;
        Properties saved=new Properties();
        if(Files.isRegularFile(stateFile))try(var in=Files.newBufferedReader(stateFile,StandardCharsets.UTF_8)){saved.load(in);}catch(IOException|IllegalArgumentException unreadable){}
        for(String key:saved.stringPropertyNames()) {
            String[] parts=saved.getProperty(key).split(":",2);
            try{if(parts.length==2)state.put(key,new WaypointSyncPlan.State(Long.parseLong(parts[0]),parts[1]));}catch(NumberFormatException skipped){}
        }
    }
    /** Writes the server's newer files into the folder and returns the chunks of local files the server should receive. */
    public List<WaypointProtocol.Chunk> sync(WaypointProtocol.Snapshot snapshot) {
        Map<String,WaypointStore.Entry> server=new TreeMap<>();
        for(WaypointProtocol.Item item:snapshot.items())server.put(item.key(),item.entry());
        Map<String,WaypointSyncPlan.Local> local=XaeroFolders.waypointFiles(folder,maxFileBytes);
        List<WaypointProtocol.Chunk> uploads=new ArrayList<>();
        for(WaypointSyncPlan.Action action:WaypointSyncPlan.plan(server,local,state)) {
            if(action instanceof WaypointSyncPlan.Action.Apply apply) {
                if(write(apply,local.get(apply.key())))state.put(apply.key(),new WaypointSyncPlan.State(apply.version(),WaypointSyncPlan.hash(apply.text())));
            } else if(action instanceof WaypointSyncPlan.Action.Adopt adopt) {
                state.put(adopt.key(),new WaypointSyncPlan.State(adopt.version(),WaypointSyncPlan.hash(local.get(adopt.key()).text())));
            } else if(action instanceof WaypointSyncPlan.Action.Upload upload) {
                uploads.addAll(WaypointProtocol.split(upload.key(),upload.baseVersion(),upload.text()));
                uploaded.put(upload.key(),WaypointSyncPlan.hash(upload.text()));
            }
        }
        saveState();
        return uploads;
    }
    /** Local files that differ from what was last agreed or already sent, as upload chunks. */
    public List<WaypointProtocol.Chunk> changes() {
        List<WaypointProtocol.Chunk> uploads=new ArrayList<>();
        for(var file:XaeroFolders.waypointFiles(folder,maxFileBytes).entrySet()) {
            String hash=WaypointSyncPlan.hash(file.getValue().text());WaypointSyncPlan.State seen=state.get(file.getKey());
            if((seen!=null&&seen.hash().equals(hash))||hash.equals(uploaded.get(file.getKey())))continue;
            uploads.addAll(WaypointProtocol.split(file.getKey(),seen==null?0:seen.version(),file.getValue().text()));
            uploaded.put(file.getKey(),hash);
        }
        return uploads;
    }
    private boolean write(WaypointSyncPlan.Action.Apply apply,WaypointSyncPlan.Local existing) {
        Path target=XaeroFolders.resolve(folder,apply.key());
        if(target==null)return false;
        try {
            if(apply.backupLocal()&&existing!=null&&Files.exists(target)) {
                Path copy=backups.resolve(String.valueOf(clock.getAsLong())).resolve(apply.key().replace('/','_'));
                Files.createDirectories(copy.getParent());Files.copy(target,copy,StandardCopyOption.REPLACE_EXISTING);
            }
            Files.createDirectories(target.getParent());
            Path temp=target.resolveSibling(target.getFileName()+".fan4tmp");
            Files.writeString(temp,apply.text(),StandardCharsets.UTF_8);
            dev.fan4.compat.shared.AtomicFiles.replace(temp,target);
            return true;
        } catch(IOException failure) {
            dev.fan4.compat.shared.GameLog.warn("Could not write synced Xaero waypoints "+apply.key(),failure);
            return false;
        }
    }
    private void saveState() {
        Properties out=new Properties();
        for(var entry:state.entrySet())out.setProperty(entry.getKey(),entry.getValue().version()+":"+entry.getValue().hash());
        try {
            Files.createDirectories(stateFile.getParent());
            Path temp=stateFile.resolveSibling(stateFile.getFileName()+".tmp");
            try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){out.store(writer,"Fan4Compat Xaero waypoint sync state");}
            dev.fan4.compat.shared.AtomicFiles.replace(temp,stateFile);
        } catch(IOException failure){dev.fan4.compat.shared.GameLog.warn("Could not save Xaero sync state",failure);}
    }
}
