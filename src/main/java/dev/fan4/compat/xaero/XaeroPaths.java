package dev.fan4.compat.xaero;

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
        catch(IOException|IllegalArgumentException unreadable){dev.fan4.compat.shared.GameLog.warn("Could not read "+file+"; using defaults",unreadable);}
        return config;
    }
    /** The folder of the world a (dedicated or integrated) server is running. */
    static Path worldRoot(Object server){return (Path)call(server,"method_27050",field(type("net.minecraft.class_5218"),"field_24188"));}
    /** Xaero's folder name for a server address, or null when it cannot be mapped. */
    static String node(Path configDir,String address) {
        return XaeroFolders.node(address,XaeroFolders.differentiateByAddress(configDir.resolve("xaero").resolve("minimap").resolve("client.cfg")));
    }
    /** Where the client side keeps Fan4Compat's own Xaero state (sync progress, backups, shared map progress). */
    static Path clientData(Path gameDir){return gameDir.resolve(".fan4compat-xaero");}
}
