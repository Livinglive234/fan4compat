package dev.fan4.compat.xaero;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Server-side copy of each player's Xaero waypoint files, one JSON file per player UUID. Uploads are
 * last-writer-wins; a copy that an upload replaces without having seen it is kept as a bounded,
 * timestamped backup. Size, file-count and rate limits stop a client from filling the disk.
 */
public final class WaypointStore {
    public record Entry(long version,long savedAt,String text) {}
    public record Limits(int maxFileBytes,int maxPlayerBytes,int maxFiles,int backupsPerFile,long minIntervalMillis) {
        /** A full snapshot must fit one server-to-client payload (1 MiB). */
        public static final int MAX_SNAPSHOT_BYTES=700_000;
        public static Limits defaults(){return new Limits(64*1024,256*1024,64,3,2_000);}
        /** Reads {@code waypointMaxBytesPerPlayer} (bytes) and keeps the other limits. */
        public static Limits from(Properties properties) {
            Limits base=defaults();
            int player=base.maxPlayerBytes;
            try{player=Math.min(MAX_SNAPSHOT_BYTES,Math.max(base.maxFileBytes,Integer.parseInt(properties.getProperty("waypointMaxBytesPerPlayer",String.valueOf(base.maxPlayerBytes)).trim())));}
            catch(NumberFormatException ignored){}
            return new Limits(base.maxFileBytes,player,base.maxFiles,base.backupsPerFile,base.minIntervalMillis);
        }
    }
    public sealed interface Result {
        /** @param displaced the previous server copy was replaced although the uploader had not seen it */
        record Accepted(Entry entry,boolean displaced) implements Result {}
        record Rejected(String reason) implements Result {}
    }
    private static final class Player {
        final Map<String,Entry> files=new TreeMap<>();
        final Map<String,List<Entry>> backups=new TreeMap<>();
        final Map<String,Long> lastUpload=new HashMap<>();
        int bytes() {
            int total=0;
            for(Entry e:files.values())total+=WaypointKeys.bytes(e.text);
            for(List<Entry> list:backups.values())for(Entry e:list)total+=WaypointKeys.bytes(e.text);
            return total;
        }
    }
    private final Path root;private final Limits limits;private final Map<UUID,Player> players=new HashMap<>();
    public WaypointStore(Path root,Limits limits){this.root=root;this.limits=limits;}
    public Limits limits(){return limits;}
    public synchronized Result put(UUID id,String key,long baseVersion,String text,long now) {
        if(!WaypointKeys.valid(key))return new Result.Rejected("invalid file name");
        if(!WaypointKeys.validText(text,limits.maxFileBytes))return new Result.Rejected("file too large or not valid text");
        Player player=player(id);
        Entry current=player.files.get(key);
        if(current!=null&&current.text.equals(text))return new Result.Accepted(current,false);
        Long last=player.lastUpload.get(key);
        if(last!=null&&now-last<limits.minIntervalMillis)return new Result.Rejected("uploading too fast");
        if(current==null&&player.files.size()>=limits.maxFiles)return new Result.Rejected("too many files");
        boolean displaced=current!=null&&current.version!=baseVersion;
        Entry created=new Entry(current==null?1:current.version+1,now,text);
        Map<String,Entry> filesBefore=new TreeMap<>(player.files);Map<String,List<Entry>> backupsBefore=new TreeMap<>();
        for(var e:player.backups.entrySet())backupsBefore.put(e.getKey(),new ArrayList<>(e.getValue()));
        if(displaced) {
            List<Entry> list=player.backups.computeIfAbsent(key,unused->new ArrayList<>());
            list.add(current);
            while(list.size()>limits.backupsPerFile)list.remove(0);
        }
        player.files.put(key,created);
        // Over budget: shed the oldest backups first; the live files are never evicted.
        while(player.bytes()>limits.maxPlayerBytes&&dropOldestBackup(player)){}
        if(player.bytes()>limits.maxPlayerBytes) {
            player.files.clear();player.files.putAll(filesBefore);player.backups.clear();player.backups.putAll(backupsBefore);
            return new Result.Rejected("over the storage limit");
        }
        player.lastUpload.put(key,now);
        try{save(id,player);}
        catch(IOException failure) {
            player.files.clear();player.files.putAll(filesBefore);player.backups.clear();player.backups.putAll(backupsBefore);
            dev.fan4.compat.shared.GameLog.warn("Could not store Xaero waypoints for "+id,failure);
            return new Result.Rejected("server could not save the file");
        }
        return new Result.Accepted(created,displaced);
    }
    public synchronized Map<String,Entry> files(UUID id){return new TreeMap<>(player(id).files);}
    public synchronized List<Entry> backups(UUID id,String key){return List.copyOf(player(id).backups.getOrDefault(key,List.of()));}
    private static boolean dropOldestBackup(Player player) {
        String oldestKey=null;long oldest=Long.MAX_VALUE;
        for(var e:player.backups.entrySet())if(!e.getValue().isEmpty()&&e.getValue().get(0).savedAt<oldest){oldest=e.getValue().get(0).savedAt;oldestKey=e.getKey();}
        if(oldestKey==null)return false;
        List<Entry> list=player.backups.get(oldestKey);list.remove(0);if(list.isEmpty())player.backups.remove(oldestKey);
        return true;
    }
    private Player player(UUID id){return players.computeIfAbsent(id,this::load);}
    private Path file(UUID id){return root.resolve(id+".json");}
    private Player load(UUID id) {
        Player player=new Player();Path file=file(id);
        if(!Files.exists(file))return player;
        try {
            JsonObject json=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:json.getAsJsonObject("files").entrySet()){Entry entry=read(e.getValue());if(WaypointKeys.valid(e.getKey())&&entry!=null)player.files.put(e.getKey(),entry);}
            for(var e:json.getAsJsonObject("backups").entrySet()) {
                List<Entry> list=new ArrayList<>();
                for(JsonElement element:e.getValue().getAsJsonArray()){Entry entry=read(element);if(entry!=null)list.add(entry);}
                if(WaypointKeys.valid(e.getKey())&&!list.isEmpty())player.backups.put(e.getKey(),list);
            }
        } catch(IOException|RuntimeException damaged) {
            // Keep the unreadable file for inspection rather than overwriting it silently.
            dev.fan4.compat.shared.GameLog.warn("Xaero waypoint store for "+id+" is unreadable; starting empty and keeping a copy",damaged);
            try{Files.move(file,file.resolveSibling(id+".json.damaged-"+System.currentTimeMillis()),StandardCopyOption.REPLACE_EXISTING);}catch(IOException ignored){}
            return new Player();
        }
        return player;
    }
    private static Entry read(JsonElement element) {
        JsonObject o=element.getAsJsonObject();
        return new Entry(o.get("version").getAsLong(),o.get("savedAt").getAsLong(),o.get("text").getAsString());
    }
    private static JsonObject write(Entry entry) {
        JsonObject o=new JsonObject();o.addProperty("version",entry.version);o.addProperty("savedAt",entry.savedAt);o.addProperty("text",entry.text);return o;
    }
    private void save(UUID id,Player player)throws IOException {
        JsonObject json=new JsonObject(),files=new JsonObject(),backups=new JsonObject();
        for(var e:player.files.entrySet())files.add(e.getKey(),write(e.getValue()));
        for(var e:player.backups.entrySet()){JsonArray array=new JsonArray();for(Entry entry:e.getValue())array.add(write(entry));backups.add(e.getKey(),array);}
        json.add("files",files);json.add("backups",backups);
        Files.createDirectories(root);
        Path temp=file(id).resolveSibling(id+".json.tmp");
        Files.writeString(temp,json.toString(),StandardCharsets.UTF_8);
        dev.fan4.compat.shared.AtomicFiles.replace(temp,file(id));
    }
}
