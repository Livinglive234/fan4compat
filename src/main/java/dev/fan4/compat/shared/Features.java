package dev.fan4.compat.shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Every feature ships enabled; a switch exists to turn one off, never to opt in. Troubleshooting aids
 * (diagnostics, debug logging) are not features and default to off. Player-facing on/off switches stored in {@code config/fan4compat.properties}.
 * Only optional or troubleshooting behavior is listed here: fixes that protect saves or
 * prevent crashes stay unconditional. Hook-level switches are read while Mixin selects its
 * classes, so they take effect at the next launch; a few are read live.
 */
public final class Features {
    /** @param requires mod IDs that must all be installed for the switch to have any effect */
    public record Feature(String id,String category,String label,String description,boolean defaultValue,boolean needsRestart,List<String> requires) {}
    public static final String RENDERING="Rendering",SHIPS="Ships & Portals",PACK="Pack integrations",TROUBLESHOOTING="Troubleshooting";
    private static Feature of(String id,String category,String label,String description,boolean def,boolean restart,String... requires) {
        return new Feature(id,category,label,description,def,restart,List.of(requires));
    }
    public static final List<Feature> ALL=List.of(
        of("appleGpuFixes",RENDERING,"Apple GPU portal fixes","Matching depth/stencil formats and a framebuffer-copy fallback so Immersive Portals renders with shaders on Macs. Only acts on Apple GPUs; turn off to test whether it is the cause of a rendering problem.",true,true,"iris","immersive_portals"),
        of("pointBlankStencilFixes",RENDERING,"Point Blank scope stencil fixes","Keeps Point Blank scopes and Immersive Portals from corrupting each other's stencil state (the missing-terrain-after-drawing-a-scoped-gun bug).",true,true,"pointblank"),
        of("freecamFixes",RENDERING,"Freecam with Distant Horizons","Lets Freecam tick and clip distant terrain correctly outside loaded chunks, including the BSL shader overlap fix.",true,true,"freecam","distanthorizons"),
        of("wingTrails",RENDERING,"Elytra Contrails wing trails","Draws wing-tip trails for custom and accessory elytra that Elytra Contrails cannot sample on its own.",true,true,"elytratrails"),
        of("tardisModelCache",RENDERING,"TARDIS model cache","Caches Doctor Who Mod's TARDIS models to reduce per-frame work. Turn off if a TARDIS model ever looks stale.",true,true,"dwm"),
        of("shipPortalBlocking",SHIPS,"Block portals on ships","Stops Nether, Aether and Immersive Portals frames from being lit on a ship. Turn off only if you want to experiment; portals on moving ships can misbehave.",true,true,"valkyrienskies"),
        of("shipAcoustics",SHIPS,"Ship-aware sound occlusion","Makes Sound Physics Remastered trace sound through ship blocks in their own frame. Costs some CPU on busy ships.",true,true,"valkyrienskies","sound_physics_remastered"),
        of("eurekaWarnings",PACK,"Hide Eureka portal warnings","Removes Eureka from Immersive Portals' incompatibility notices; other warnings are not touched.",true,true,"vs_eureka","immersive_portals"),
        of("jadeHelm",PACK,"Hide Jade while at a helm","Hides the Jade overlay while you steer a ship from a helm, and brings it back when you dismount.",true,true,"jade","vs_eureka"),
        of("xaeroTardisMap",PACK,"Xaero map shows the TARDIS exterior","While you are inside a TARDIS that has landed, Xaero's World Map opens on the exterior's dimension, centered on where the TARDIS is. In flight, or when the exterior is not known, the map behaves as normal.",true,true,"xaeroworldmap","dwm"),
        of("xaeroTardisMinimap",PACK,"Xaero minimap and waypoints follow the TARDIS exterior","While you are inside a TARDIS that has landed, Xaero treats you as standing at the exterior: the minimap centers there, new waypoints and the waypoint list belong to the outside dimension, and the minimap and World Map draw what the World Map has stored for that spot. The outside is not loaded on your client, so only terrain you have already mapped appears.",true,true,"xaerominimap","xaeroworldmap","dwm"),
        of("xaeroSharedMap",PACK,"Shared explored map","Terrain that any player has explored shows up on everyone's Xaero World Map, including areas you have not visited. The server records what players around it have loaded and streams it to clients; Xaero replaces it with its own data the moment you load that chunk. Needs Fan4Compat on the server, whose operator picks the source (sharedMapMode: SERVER, CLIENT, BOTH or NORMAL) in fan4compat-xaero.properties. Server admins can read the stored map data.",true,true,"xaeroworldmap"),
        of("xaeroWaypointSync",PACK,"Xaero waypoint sync","Keeps your Xaero's Minimap waypoints on the server so they follow you between PCs. The join is held briefly while the files are exchanged, so Xaero loads the up-to-date copy. Needs Fan4Compat on the server and Xaero's Minimap 26.5.0 on your client; when both sides changed a file, the newer save wins and the other copy is backed up.",true,true,"xaerominimap"),
        of("trinketGraves",PACK,"Trinkets items in graves","Puts equipped Trinkets items into Player Graves graves and restores them to their slot on quick retrieve.",true,false,"trinkets","graves"),
        of("eurekaDebugLogging",TROUBLESHOOTING,"Eureka ship debug logging","Shows Eureka's ship debug messages in the log.",false,true,"vs_eureka"),
        of("xaeroTardisDiagnostics",TROUBLESHOOTING,"Xaero TARDIS diagnostics","While you are inside a TARDIS, logs (when they change) where the exterior is, how the compass is turned and what cave start and layer the minimap is given. Turn on when the minimap, compass or coordinates look wrong.",false,false,"xaerominimap","dwm"),
        of("renderDiagnostics",TROUBLESHOOTING,"Point Blank render diagnostics","Logs OpenGL state around gun and scope drawing. Turning this on also turns on the Distant Horizons diagnostics unless those are set separately.",false,true,"pointblank"),
        of("dhRenderDiagnostics",TROUBLESHOOTING,"Distant Horizons render diagnostics","Logs OpenGL state around Distant Horizons terrain draws.",false,true,"distanthorizons"),
        of("portalDepthDiagnostics",TROUBLESHOOTING,"Portal depth-copy logging","Logs the result of Immersive Portals' depth copy a few times per launch, with GPU and framebuffer details. Turn on when investigating nested-portal rendering.",false,true,"iris","immersive_portals"));
    private static final Map<String,Feature> BY_ID=new HashMap<>();
    static {for(Feature feature:ALL)BY_ID.put(feature.id(),feature);}

    /** Generated mixin (simple name) to the switch that controls it. */
    private static final Map<String,String> HOOKS=new HashMap<>();
    private static void hooks(String id,String... simpleNames){for(String name:simpleNames)HOOKS.put(name,id);}
    static {
        hooks("appleGpuFixes","IrisFramebufferCopyMixin","IrisDepthFormatMixin","IrisSeparateStencilMixin","IrisStencilCleanupMixin");
        hooks("pointBlankStencilFixes","PortalStencilCacheMixin","PortalStencilPrepareMixin","GunStencilMixin","IrisScopeStencilMixin","GunFramebufferMixin","GunAuxFramebufferMixin","GunStencilClearMixin");
        hooks("wingTrails","WingTrailFallbackMixin");
        hooks("shipPortalBlocking","ShipNetherPortalCreationMixin","ShipAetherPortalCreationMixin","ShipNetherPortalActivationMixin","ShipFramePortalActivationMixin");
        hooks("shipAcoustics","ShipAcousticRaycastMixin","ShipAcousticSnapshotMixin");
        hooks("eurekaWarnings","EurekaPortalWarningMixin","EurekaPortalWarningInfoMixin");
        hooks("jadeHelm","JadeHelmMixin");
        hooks("xaeroTardisMap","TardisMapMixin");
        hooks("xaeroSharedMap","SharedMapTickMixin");
        hooks("xaeroTardisMinimap","TardisMapEntityMixin","TardisMapProcessorMixin","TardisMapCaveMixin","TardisMapYawMixin");
    }
    /** The switch for a generated mixin, or null when it is always installed. */
    public static String featureFor(String path,String simple) {
        if(path.startsWith("freecam."))return "freecamFixes";
        if(simple.startsWith("TardisShipModelCache"))return "tardisModelCache";
        if(simple.startsWith("TardisMinimap"))return "xaeroTardisMinimap";
        return HOOKS.get(simple);
    }
    /** Generated mixin simple names that a switch controls, for tests that guard against renames. */
    public static Map<String,String> hookedMixins(){return Map.copyOf(HOOKS);}
    /** True unless the player turned this mixin's feature off. */
    public static boolean allows(String path,String simple) {
        String id=featureFor(path,simple);
        return id==null||enabled(id);
    }

    private static final Object LOCK=new Object();
    private static Map<String,Boolean> values;
    private static Path path;
    /** The saved value, or the default; {@code -Dfan4compat.feature.<id>} overrides both. */
    public static boolean enabled(String id) {
        Feature feature=BY_ID.get(id);
        if(feature==null)throw new IllegalArgumentException("Unknown feature "+id);
        String override=System.getProperty("fan4compat.feature."+id);
        if(override!=null)return Boolean.parseBoolean(override);
        synchronized(LOCK){return loaded().getOrDefault(id,feature.defaultValue());}
    }
    /** Staged in memory; {@link #save()} writes it out. */
    public static void set(String id,boolean value) {
        if(!BY_ID.containsKey(id))throw new IllegalArgumentException("Unknown feature "+id);
        synchronized(LOCK){loaded().put(id,value);}
    }
    public static void save() {
        synchronized(LOCK) {
            Path file=file();if(file==null)return;
            Properties properties=read(file);
            for(Feature feature:ALL)properties.setProperty(feature.id(),String.valueOf(loaded().getOrDefault(feature.id(),feature.defaultValue())));
            try {
                Files.createDirectories(file.getParent());
                Path temp=file.resolveSibling(file.getFileName()+".tmp");
                try(var writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){
                    properties.store(writer,"Fan4Compat settings. Edit in game through Mod Menu, or here (restart to apply).");
                }
                dev.fan4.compat.shared.AtomicFiles.replace(temp,file);
            } catch(IOException e) {
                dev.fan4.compat.shared.GameLog.warn("Could not save Fan4Compat settings",e);
            }
        }
    }
    /** Re-read the file on next use. Tests point this at a scratch directory. */
    public static void reset(Path configFile) {synchronized(LOCK){values=null;path=configFile;}}
    private static Map<String,Boolean> loaded() {
        if(values==null) {
            values=new HashMap<>();
            Path file=file();
            if(file!=null)for(var entry:read(file).entrySet()) {
                String key=(String)entry.getKey();
                // Unknown keys (such as the updater's) stay in the file untouched; bad values fall back to defaults.
                if(BY_ID.containsKey(key)&&("true".equalsIgnoreCase((String)entry.getValue())||"false".equalsIgnoreCase((String)entry.getValue())))
                    values.put(key,Boolean.parseBoolean((String)entry.getValue()));
            }
        }
        return values;
    }
    private static Path file() {
        if(path!=null)return path;
        try{return path=FabricLoader.getInstance().getConfigDir().resolve("fan4compat.properties");}
        catch(RuntimeException|LinkageError noLoader){return null;}
    }
    private static Properties read(Path file) {
        Properties properties=new Properties();
        if(Files.exists(file))try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){properties.load(reader);}
        catch(IOException|IllegalArgumentException unreadable){
            dev.fan4.compat.shared.GameLog.warn("Could not read Fan4Compat settings; using defaults",unreadable);
        }
        return properties;
    }
}
