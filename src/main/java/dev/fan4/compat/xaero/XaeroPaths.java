package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.GameLog;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Where the Xaero features keep and find their files, in one place for waypoint sync and the shared map. */
final class XaeroPaths {
    private XaeroPaths() {}
    /** The server operator's settings file; a missing or unreadable file means defaults (and an unreadable one is logged). */
    static Properties config() {
        Properties config=new Properties();
        Path file=FabricLoader.getInstance().getConfigDir().resolve("fan4compat-xaero.properties");
        if(Files.isRegularFile(file))try(var in=Files.newBufferedReader(file)){config.load(in);}
        catch(IOException|IllegalArgumentException unreadable){GameLog.warn("Could not read "+file+"; using defaults",unreadable);}
        return config;
    }
    /** The folder of the world a (dedicated or integrated) server is running. */
    static Path worldRoot(Object server){return (Path)call(server,"method_27050",field(type("net.minecraft.class_5218"),"field_24188"));}
    /** Xaero's folder name for a server address, or null when it cannot be mapped. */
    static String node(Path configDir,String address) {
        return XaeroFolders.node(address,XaeroFolders.differentiateByAddress(configDir.resolve("xaero").resolve("minimap").resolve("client.cfg")));
    }
    /**
     * Where the client side keeps Fan4Compat's own Xaero state (waypoint sync state and backups, shared map progress): a plain
     * {@code fan4compat} folder in the game folder. Earlier builds used a hidden {@code .fan4compat-xaero}; it is renamed on first use
     * so nothing saved there is lost (and left alone if a new folder already exists).
     */
    static Path clientData(Path gameDir) {
        Path data=gameDir.resolve("fan4compat"),old=gameDir.resolve(".fan4compat-xaero");
        if(Files.isDirectory(old)&&!Files.exists(data)) {
            try{Files.move(old,data);GameLog.info("Moved Fan4Compat's Xaero data from "+old+" to "+data);}
            catch(IOException failure){GameLog.warn("Could not move "+old+" to "+data+"; starting without it",failure);}
        }
        return data;
    }
}
