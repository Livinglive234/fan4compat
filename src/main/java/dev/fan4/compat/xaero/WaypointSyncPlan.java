package dev.fan4.compat.xaero;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * What a client does when the server lists its stored copies: compare the server's version counter,
 * the local file, and the version/hash recorded at the last successful sync. Pure decision logic;
 * the caller performs the file and network work.
 */
public final class WaypointSyncPlan {
    public record Local(String text,long modified) {}
    /** Server version and the hash of the text last agreed with the server. */
    public record State(long version,String hash) {}
    public sealed interface Action {
        String key();
        /** Write the server text locally, first backing up the local file when it differs from the server's history. */
        record Apply(String key,String text,long version,boolean backupLocal) implements Action {}
        /** Send the local text; the server backs up what it displaces when baseVersion is stale. */
        record Upload(String key,long baseVersion,String text) implements Action {}
        /** Same text on both sides: record the version, no file work. */
        record Adopt(String key,long version) implements Action {}
    }
    private WaypointSyncPlan() {}
    public static String hash(String text) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format("%02x",b));return out.toString();
        } catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static List<Action> plan(Map<String,WaypointStore.Entry> server,Map<String,Local> local,Map<String,State> state) {
        List<Action> actions=new ArrayList<>();
        Set<String> keys=new TreeSet<>(server.keySet());keys.addAll(local.keySet());
        for(String key:keys) {
            WaypointStore.Entry remote=server.get(key);Local mine=local.get(key);State seen=state.get(key);
            if(remote==null){actions.add(new Action.Upload(key,0,mine.text));continue;}
            if(mine==null){actions.add(new Action.Apply(key,remote.text(),remote.version(),false));continue;}
            if(mine.text.equals(remote.text())){actions.add(new Action.Adopt(key,remote.version()));continue;}
            boolean localChanged=seen==null||!hash(mine.text).equals(seen.hash);
            boolean serverChanged=seen==null||remote.version()!=seen.version;
            if(!localChanged)actions.add(new Action.Apply(key,remote.text(),remote.version(),false));
            else if(!serverChanged)actions.add(new Action.Upload(key,seen.version,mine.text));
            // Both sides changed: the newer save wins and the other copy is kept as a backup.
            else if(mine.modified>remote.savedAt())actions.add(new Action.Upload(key,remote.version(),mine.text));
            else actions.add(new Action.Apply(key,remote.text(),remote.version(),true));
        }
        return actions;
    }
}
