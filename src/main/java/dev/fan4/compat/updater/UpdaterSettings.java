package dev.fan4.compat.updater;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

/** The updater's three switches in {@code config/fan4compat-updater.properties}; the updater reads them once at startup. */
public final class UpdaterSettings {
    public record Setting(String key,String label,String description) {}
    public static final List<Setting> ALL=List.of(
        new Setting("enabled","Check for updates","Looks for new Fan4Compat releases on GitHub at startup and every 30 minutes. Turn off to stop all update activity."),
        new Setting("autoDownload","Download updates automatically","Downloads and verifies a new release, then installs it when you quit the game. Turn off to only get a notice that an update exists."),
        new Setting("allowPrereleases","Include beta releases","Also offers published alpha, beta and release-candidate versions. Draft builds and Actions artifacts are never offered."));
    private static final Object LOCK=new Object();
    private static Properties values;
    private static Path path;
    private UpdaterSettings() {}
    /** Every switch defaults to on. */
    public static void applyDefaults(Properties into){for(Setting setting:ALL)into.setProperty(setting.key(),"true");}
    public static boolean get(String key) {
        synchronized(LOCK){return Boolean.parseBoolean(loaded().getProperty(key,"true"));}
    }
    /** Staged in memory; {@link #save()} writes it out. */
    public static void set(String key,boolean value){synchronized(LOCK){loaded().setProperty(key,String.valueOf(value));}}
    public static void save() {
        synchronized(LOCK) {
            Path file=file();if(file==null)return;
            try {
                Files.createDirectories(file.getParent());
                Path temp=file.resolveSibling(file.getFileName()+".tmp");
                try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){
                    loaded().store(writer,"Fan4Compat release updater; disable enabled to stop checks. Updates apply after exit. Backups are in .fan4compat-update/backups.");
                }
                try{Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
            } catch(IOException e) {
                System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Could not save Fan4Compat updater settings",e);
            }
        }
    }
    /** Re-read the file on next use. Tests point this at a scratch directory. */
    public static void reset(Path configFile){synchronized(LOCK){values=null;path=configFile;}}
    private static Properties loaded() {
        if(values==null) {
            values=new Properties();applyDefaults(values);
            Path file=file();
            if(file!=null&&Files.exists(file))try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){values.load(reader);}
            catch(IOException|IllegalArgumentException unreadable){
                System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Could not read Fan4Compat updater settings; using defaults",unreadable);
            }
        }
        return values;
    }
    private static Path file() {
        if(path!=null)return path;
        try{return path=FabricLoader.getInstance().getConfigDir().resolve("fan4compat-updater.properties");}
        catch(RuntimeException|LinkageError noLoader){return null;}
    }
}
