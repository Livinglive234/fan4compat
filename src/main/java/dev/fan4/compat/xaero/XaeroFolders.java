package dev.fan4.compat.xaero;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Where Xaero's Minimap keeps one server's waypoint files, and which of them take part in sync. */
public final class XaeroFolders {
    public static final String ANY_ADDRESS="Multiplayer_Any Address";
    private XaeroFolders() {}
    /**
     * Xaero's folder name for a multiplayer address typed as {@code host} or {@code host:port}. IPv6 and
     * other unusual forms return null: the feature stays inactive rather than guessing a folder.
     */
    public static String node(String address,boolean differentiateByAddress) {
        if(!differentiateByAddress)return ANY_ADDRESS;
        if(address==null)return null;
        String host=address.trim();
        if(host.isEmpty()||host.indexOf('[')>=0||host.indexOf(']')>=0)return null;
        int colon=host.indexOf(':');
        if(colon!=host.lastIndexOf(':'))return null;
        if(colon>0)host=host.substring(0,colon);
        while(host.endsWith("."))host=host.substring(0,host.length()-1);
        if(host.isEmpty())return null;
        for(int i=0;i<host.length();i++){char c=host.charAt(i);if(c<0x20||"\\/:*?\"<>|".indexOf(c)>=0)return null;}
        return "Multiplayer_"+host;
    }
    /** {@code differentiate_by_server_address} from Xaero's minimap client.cfg; true when absent or unreadable. */
    public static boolean differentiateByAddress(Path clientConfig) {
        try {
            if(Files.isRegularFile(clientConfig))
                for(String line:Files.readAllLines(clientConfig)) {
                    String trimmed=line.trim();
                    if(trimmed.startsWith("differentiate_by_server_address")){int eq=trimmed.indexOf('=');if(eq>0)return !trimmed.substring(eq+1).trim().equalsIgnoreCase("false");}
                }
        } catch(IOException|RuntimeException unreadable){}
        return true;
    }
    /** The folder for {@code node} under Xaero's minimap folder, reusing an existing one that differs only in case. */
    public static Path serverFolder(Path minimapFolder,String node) {
        try {
            if(Files.isDirectory(minimapFolder))try(Stream<Path> children=Files.list(minimapFolder)) {
                for(Path child:children.toList())if(Files.isDirectory(child)&&child.getFileName().toString().equalsIgnoreCase(node))return child;
            }
        } catch(IOException unreadable){}
        return minimapFolder.resolve(node);
    }
    /** A key under {@code root}, or null if it is invalid or would resolve outside it. */
    public static Path resolve(Path root,String key) {
        if(!WaypointKeys.valid(key))return null;
        Path normalized=root.toAbsolutePath().normalize(),target=normalized.resolve(key).normalize();
        return target.startsWith(normalized)&&!target.equals(normalized)?target:null;
    }
    /**
     * Waypoint files in a server folder, by key: {@code <dimension>/<name>.txt} (or one level deeper). Config
     * files, symbolic links, oversized files and unreadable text are skipped.
     */
    public static Map<String,WaypointSyncPlan.Local> waypointFiles(Path root,int maxFileBytes) {
        Map<String,WaypointSyncPlan.Local> files=new TreeMap<>();
        if(!Files.isDirectory(root))return files;
        try(Stream<Path> walk=Files.walk(root,3)) {
            for(Path path:walk.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)).toList()) {
                String key=root.relativize(path).toString().replace('\\','/');
                if(path.getFileName().toString().equals("config.txt")||!WaypointKeys.valid(key))continue;
                if(Files.size(path)>maxFileBytes)continue;
                String text=new String(Files.readAllBytes(path),java.nio.charset.StandardCharsets.UTF_8);
                if(WaypointKeys.validText(text,maxFileBytes))files.put(key,new WaypointSyncPlan.Local(text,Files.getLastModifiedTime(path).toMillis()));
            }
        } catch(IOException|RuntimeException unreadable){}
        return files;
    }
}
