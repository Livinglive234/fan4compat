package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.Features;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * While the player is inside a landed TARDIS, Xaero's minimap and World Map treat them as standing at the exterior:
 * their position and the waypoint world follow the outside. Client only; every method is exception-safe and returns
 * the unchanged value when inactive. The generated hooks in Xaero's classes call the static methods here.
 */
public final class TardisLocation {
    private static final String CLIENT="net.minecraft.class_310",HELPER="net.drgmes.dwm.utils.helpers.TardisHelper";
    private static final long PERIOD_NANOS=250_000_000L;
    private static volatile TardisMapCompat.Exterior cached;
    private static long checkedAt;
    private static boolean warned,haveChecked;
    /** The custom world path and map dimension this class put in place, so only our own are ever undone. */
    private static Object appliedWorldPath;
    /** The latest minimap session seen by {@link #frame}, used to look up the exterior dimension's type. */
    private static volatile Object lastSession;
    private TardisLocation() {}

    /** The landed exterior of the TARDIS the player is inside, refreshed a few times a second; null when inactive. */
    public static TardisMapCompat.Exterior current() {
        long now=System.nanoTime();
        synchronized(TardisLocation.class) {
            if(haveChecked&&now-checkedAt<PERIOD_NANOS)return cached;
            haveChecked=true;checkedAt=now;
            TardisMapCompat.Exterior found=null;
            try {
                if(Features.enabled("xaeroTardisMinimap")) {
                    Object client=call(type(CLIENT),"method_1551"),world=field(client,"field_1687"),player=field(client,"field_1724");
                    if(world!=null&&player!=null&&(Boolean)call(type(HELPER),"isTardisDimension",call(world,"method_27983")))found=TardisMapCompat.exterior(world,player);
                }
            } catch(RuntimeException|LinkageError failure){warn(failure);}
            return cached=found;
        }
    }
    /** Forget the cached location so the next call looks again (a world change, or a test). */
    public static synchronized void refresh(){haveChecked=false;cached=null;}
    private static void warn(Throwable failure){if(!warned){warned=true;System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Xaero TARDIS location unavailable: "+failure);}}
    private static boolean isPlayer(Object entity) {
        try{return entity!=null&&entity==field(call(type(CLIENT),"method_1551"),"field_1724");}
        catch(RuntimeException|LinkageError failure){warn(failure);return false;}
    }

    // ---- hook entry points ---------------------------------------------------------------------------------

    /** Redirect of {@code entity.getX()}: the exterior for the player, the original for everyone else. */
    public static double x(Object entity,double original){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?original:e.x()+0.5;}
    public static double y(Object entity,double original){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?original:e.y();}
    public static double z(Object entity,double original){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?original:e.z()+0.5;}
    /** For Xaero's {@code EntityUtils.getEntityX/Y/Z} (which interpolate): the exterior's coordinate, or null to run unchanged. */
    public static Double entityX(Object entity){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?null:e.x()+0.5;}
    public static Double entityY(Object entity){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?null:(double)e.y();}
    public static Double entityZ(Object entity){TardisMapCompat.Exterior e=isPlayer(entity)?current():null;return e==null?null:e.z()+0.5;}
    /** Redirect of {@code clientWorld.dimension()}: the exterior's dimension while the player is inside a landed TARDIS. */
    public static Object dimension(Object world,Object original) {
        try {
            if(world!=field(call(type(CLIENT),"method_1551"),"field_1687"))return original;
            TardisMapCompat.Exterior e=current();
            return e==null?original:e.dimension();
        } catch(RuntimeException|LinkageError failure){warn(failure);return original;}
    }

    /**
     * The cave start (Xaero's "caving" height) for the virtual location, or null to let Xaero work it out. Xaero scans the
     * real world's blocks around the player for a roof; inside the TARDIS that is the interior and the exterior is not loaded,
     * so it would find none and show the top view (the bedrock ceiling in the Nether). Under a ceiling dimension use the
     * exterior's own height so the minimap shows the layer at that level; otherwise the plain top view.
     */
    public static Integer caving() {
        TardisMapCompat.Exterior exterior=current();
        if(exterior==null)return null;
        try{if(hasCeiling(exterior.dimension()))return exterior.y()+1;}
        catch(RuntimeException|LinkageError failure){warn(failure);}
        return Integer.MAX_VALUE;
    }
    private static boolean hasCeiling(Object dimension) {
        Object session=lastSession;
        if(session!=null) {
            Object type=call(call(call(session,"getWorldManager"),"getAutoRootContainer"),"getDimensionType",dimension);
            if(type!=null)return (Boolean)call(type,"comp_643");
        }
        // Xaero has not recorded this dimension's type yet: the one ceiling dimension every pack has.
        return "minecraft:the_nether".equals(String.valueOf(call(dimension,"method_29177")));
    }

    /** Once per minimap frame: keep the waypoint world and the World Map's viewed dimension matching the player's location. */
    public static void frame(Object session) {
        try {
            lastSession=session;
            TardisMapCompat.Exterior exterior=current();
            applyWaypointWorld(session,exterior);
            TardisMapCompat.applyMapDimension(exterior);
        } catch(RuntimeException|LinkageError failure){warn(failure);}
    }
    /** Xaero's waypoint menu switches worlds with {@code MinimapWorldState.setCustomWorldPath}; do the same for the exterior. */
    static void applyWaypointWorld(Object session,TardisMapCompat.Exterior exterior) {
        Object state=call(session,"getWorldState"),custom=call(state,"getCustomWorldPath");
        if(exterior==null) {
            if(appliedWorldPath!=null&&appliedWorldPath.equals(custom))call(state,"setCustomWorldPath",(Object)null);
            appliedWorldPath=null;
            return;
        }
        Object auto=call(state,"getAutoRootContainerPath");
        Object updater=call(session,"getWorldStateUpdater");
        Object container=call(auto,"resolve",(String)call(call(session,"getDimensionHelper"),"getDimensionDirectoryName",exterior.dimension()));
        boolean worldMap=(Boolean)call(call(field(updater,"modMain"),"getSupportMods"),"worldmap");
        String node=(String)call(updater,"getPotentialWorldNode",exterior.dimension(),worldMap);
        if(node==null)return;
        Object path=call(container,"resolve",node);
        if(path.equals(custom)){appliedWorldPath=path;return;}
        // The exterior's waypoint world exists if it was ever visited; make it if not, as Xaero does for a new world.
        Object manager=call(session,"getWorldManager");
        if(call(manager,"getWorld",path)==null)call(manager,"addWorld",path);
        call(state,"setCustomWorldPath",path);
        appliedWorldPath=path;
    }
}
