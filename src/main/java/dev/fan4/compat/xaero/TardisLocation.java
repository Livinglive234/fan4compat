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
    /**
     * What the last lookup found, replaced as a whole so every hook sees one consistent picture: the console's TARDIS state (kept so a
     * flight can be followed every frame without searching again), the landed exterior, whether the player is in a TARDIS dimension at
     * all, and how far the exterior's compass is turned from the interior's (NaN: unknown).
     */
    private record Snapshot(Object state,TardisMapCompat.Exterior exterior,boolean inside,float yawOffset) {}
    private static final Snapshot NOTHING=new Snapshot(null,null,false,Float.NaN);
    private static volatile Snapshot snapshot;
    private static volatile long checkedAt;
    private static volatile Object client;
    private static boolean warned;
    /** The custom world path and map dimension this class put in place, so only our own are ever undone. */
    private static Object appliedWorldPath;
    /** The latest minimap session seen by {@link #frame}, used to look up the exterior dimension's type. */
    private static volatile Object lastSession;
    private TardisLocation() {}

    private static Object client(){Object c=client;return c!=null?c:(client=call(type(CLIENT),"method_1551"));}
    private static Snapshot snapshot() {
        Snapshot known=snapshot;long now=System.nanoTime();
        if(known!=null&&now-checkedAt<PERIOD_NANOS)return known;
        synchronized(TardisLocation.class) {
            known=snapshot;
            if(known!=null&&now-checkedAt<PERIOD_NANOS)return known;
            checkedAt=now;
            Snapshot found=NOTHING;
            try {
                if(Features.enabled("xaeroTardisMinimap")) {
                    Object world=field(client(),"field_1687"),player=field(client(),"field_1724");
                    if(world!=null&&player!=null&&(Boolean)call(type(HELPER),"isTardisDimension",call(world,"method_27983"))) {
                        Object state=TardisMapCompat.state(world,player);
                        TardisMapCompat.Exterior exterior=state==null?null:TardisMapCompat.landed(state);
                        found=new Snapshot(state,exterior,true,exterior==null?Float.NaN:TardisMapCompat.yawOffset(world,state));
                        Snapshot seen=found;
                        XaeroTrace.changed("location",()->seen.exterior()==null?"inside a TARDIS that is not landed (state "+(seen.state()!=null)+")"
                            :"exterior "+call(seen.exterior().dimension(),"method_29177")+" at "+seen.exterior().x()+" "+seen.exterior().y()+" "+seen.exterior().z()+", compass turned by "+seen.yawOffset()+" degrees");
                    }
                }
            } catch(RuntimeException|LinkageError failure){warn(failure);}
            return snapshot=found;
        }
    }
    /** The landed exterior of the TARDIS the player is inside, refreshed a few times a second; null when inactive. */
    public static TardisMapCompat.Exterior current(){return snapshot().exterior();}
    /** Forget the cached location so the next call looks again (a world change, or a test). */
    public static void refresh(){snapshot=null;}
    private static void warn(Throwable failure){if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("Xaero TARDIS location unavailable: "+failure);}}
    private static boolean isPlayer(Object entity) {
        try{return entity!=null&&entity==field(client(),"field_1724");}
        catch(RuntimeException|LinkageError failure){warn(failure);return false;}
    }
    /** The landed exterior when {@code entity} is the player, else null. */
    private static TardisMapCompat.Exterior landed(Object entity){return isPlayer(entity)?current():null;}

    // ---- hook entry points ---------------------------------------------------------------------------------

    /** Redirect of {@code entity.getX()}: the exterior for the player, the original for everyone else. */
    public static double x(Object entity,double original){TardisMapCompat.Exterior e=landed(entity);return e==null?original:e.x()+0.5;}
    public static double y(Object entity,double original){TardisMapCompat.Exterior e=landed(entity);return e==null?original:e.y();}
    public static double z(Object entity,double original){TardisMapCompat.Exterior e=landed(entity);return e==null?original:e.z()+0.5;}
    /**
     * The coordinates the minimap's information display shows: the exterior while landed, and while the TARDIS travels the point it has
     * reached between departure and destination (updated every frame). The map itself stays on the landed exterior; there is no outside
     * to draw in flight.
     */
    public static double displayX(Object entity,double original){double[] p=position(entity);return p==null?original:p[0];}
    public static double displayY(Object entity,double original){double[] p=position(entity);return p==null?original:p[1];}
    public static double displayZ(Object entity,double original){double[] p=position(entity);return p==null?original:p[2];}
    private static double[] position(Object entity) {
        if(!isPlayer(entity))return null;
        try {
            TardisMapCompat.Exterior landed=current();
            if(landed!=null)return new double[]{landed.x()+0.5,landed.y(),landed.z()+0.5};
            Object state=snapshot().state();
            return state==null?null:TardisMapCompat.travelling(state);
        } catch(RuntimeException|LinkageError failure){warn(failure);return null;}
    }
    /** For Xaero's {@code EntityUtils.getEntityX/Y/Z} (which interpolate): the exterior's coordinate, or null to run unchanged. */
    public static Double entityX(Object entity){TardisMapCompat.Exterior e=landed(entity);return e==null?null:e.x()+0.5;}
    public static Double entityY(Object entity){TardisMapCompat.Exterior e=landed(entity);return e==null?null:(double)e.y();}
    public static Double entityZ(Object entity){TardisMapCompat.Exterior e=landed(entity);return e==null?null:e.z()+0.5;}
    /**
     * Redirect of the camera's yaw behind the minimap's rotation and compass: while the TARDIS is landed, turned by the difference
     * between the interior's and the exterior's compass so that north on the map is north outside.
     */
    public static float cameraYaw(Object camera,float original) {
        float turn=snapshot().yawOffset();
        return Float.isNaN(turn)?original:original+turn;
    }
    /** The same for the World Map's player arrow, which reads the player's own yaw. */
    public static float yaw(Object entity,float original){return isPlayer(entity)?cameraYaw(entity,original):original;}
    /** Redirect of the client world's entity list in Xaero's radar: nothing is tracked inside a TARDIS, where the entities are the interior's. */
    public static Object entities(Object world,Object original) {
        try {
            if(world!=field(client(),"field_1687"))return original;
            return snapshot().inside()?java.util.List.of():original;
        } catch(RuntimeException|LinkageError failure){warn(failure);return original;}
    }
    /** Redirect of {@code clientWorld.dimension()}: the exterior's dimension while the player is inside a landed TARDIS. */
    public static Object dimension(Object world,Object original) {
        try {
            if(world!=field(client(),"field_1687"))return original;
            TardisMapCompat.Exterior e=current();
            return e==null?original:e.dimension();
        } catch(RuntimeException|LinkageError failure){warn(failure);return original;}
    }

    /** Xaero's own scan finds, in open air, a cave start four above the player's feet (the exterior is not loaded to scan); matching it keeps stored layers current. */
    private static final int OPEN_AIR_ABOVE=4;
    /**
     * The cave start (Xaero's "caving" height) for the virtual location, or null to let Xaero work it out. Xaero scans the
     * real world's blocks around the player for a roof; inside the TARDIS that is the interior and the exterior is not loaded,
     * so it would find none and show the top view (the bedrock ceiling in the Nether). Under a ceiling dimension use the
     * exterior's own height (as Xaero would find it in open air) so the minimap shows the layer at that level; above the
     * ceiling (a TARDIS on the Nether roof) there is nothing overhead, so it is the plain top view, which is where that terrain was
     * mapped; otherwise the plain top view too.
     */
    public static Integer caving() {
        TardisMapCompat.Exterior exterior=current();
        if(exterior==null)return null;
        try {
            int ceiling=ceilingTop(exterior.dimension());
            int start=ceiling>=0&&exterior.y()<ceiling?exterior.y()+OPEN_AIR_ABOVE:Integer.MAX_VALUE;
            XaeroTrace.changed("cave",()->"cave start "+(start==Integer.MAX_VALUE?"top view":start+" (layer "+(start>>4)+")")+" for exterior y "+exterior.y()+", ceiling "+ceiling);
            if(start!=Integer.MAX_VALUE)return start;
        } catch(RuntimeException|LinkageError failure){warn(failure);}
        return Integer.MAX_VALUE;
    }
    /** The height of a dimension's ceiling (its logical height), or -1 when it has none. */
    private static int ceilingTop(Object dimension) {
        Object session=lastSession;
        if(session!=null) {
            Object type=call(call(call(session,"getWorldManager"),"getAutoRootContainer"),"getDimensionType",dimension);
            if(type!=null)return (Boolean)call(type,"comp_643")?(Integer)call(type,"comp_653"):-1;
        }
        // Xaero has not recorded this dimension's type yet: the one ceiling dimension every pack has.
        return "minecraft:the_nether".equals(String.valueOf(call(dimension,"method_29177")))?128:-1;
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
