package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.Features;
import java.util.Map;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Inside a landed TARDIS, Xaero's World Map opens on the exterior's dimension, centered on the TARDIS, instead of on
 * the interior dimension. Client only. The client's console block entity already holds a copy of the TARDIS state
 * (current exterior dimension and position, materialization), so no packet is needed. The map switches dimension through
 * the same call Xaero's own dimension button makes, and only while the map screen is open: it is undone when the screen
 * closes. Xaero's writer already skips writing whenever the map's dimension is not the player's real one, so no interior
 * chunk can be recorded into the exterior's map.
 */
public final class TardisMapCompat {
    private static final String CLIENT="net.minecraft.class_310",HELPER="net.drgmes.dwm.utils.helpers.TardisHelper",
        CONSOLE="net.drgmes.dwm.blocks.tardis.consoleunits.BaseTardisConsoleUnitBlockEntity",
        MATERIALIZATION="net.drgmes.dwm.common.tardis.systems.TardisSystemMaterialization",STATUS="net.minecraft.class_2806",
        FLIGHT="net.drgmes.dwm.common.tardis.systems.TardisSystemFlight";
    /** Chunks around the player searched for the console; every console of one TARDIS holds the same state. */
    private static final int RADIUS=12;
    private static boolean warned;
    /** The dimension we last put into the map's custom slot, so a stale one is cleared when it no longer applies. */
    private static Object applied;
    public record Exterior(Object dimension,int x,int y,int z) {}
    private TardisMapCompat() {}

    /** Called at the end of {@code GuiMap}'s constructor. Never throws: the map must always open. */
    public static void opened(Object gui) {
        try {
            if(!Features.enabled("xaeroTardisMap"))return;
            Object client=call(type(CLIENT),"method_1551"),world=field(client,"field_1687"),player=field(client,"field_1724");
            if(world==null||player==null)return;
            Exterior exterior=(Boolean)call(type(HELPER),"isTardisDimension",call(world,"method_27983"))?exterior(world,player):null;
            Object processor=call(gui,"getMapProcessor"),mapWorld=call(processor,"getMapWorld");
            if(exterior==null) {
                // Not in a landed TARDIS: undo our own earlier switch, but never one the player chose themselves.
                if(applied!=null&&applied.equals(call(mapWorld,"getCustomDimensionId"))){call(mapWorld,"setCustomDimensionId",(Object)null);call(processor,"checkForWorldUpdate");}
                applied=null;
                return;
            }
            call(mapWorld,"setCustomDimensionId",exterior.dimension());
            call(processor,"checkForWorldUpdate");
            applied=exterior.dimension();
            writeField(gui,"cameraX",exterior.x()+0.5);writeField(gui,"cameraZ",exterior.z()+0.5);
            writeField(gui,"shouldResetCameraPos",false);
        } catch(RuntimeException|LinkageError failure) {
            if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("TARDIS exterior on the Xaero map is unavailable: "+failure);}
        }
    }

    /** Continuous mode (minimap): keep the World Map viewing the exterior while inside a landed TARDIS, and put it back after. */
    static void applyMapDimension(Exterior exterior) {
        if(!present("xaero.map.WorldMapSession"))return;
        Object session=call(type("xaero.map.WorldMapSession"),"getCurrentSession");
        if(session==null)return;
        Object processor=call(session,"getMapProcessor");
        if(processor==null)return;
        Object mapWorld=call(processor,"getMapWorld");
        if(mapWorld==null)return;
        Object custom=call(mapWorld,"getCustomDimensionId");
        if(exterior==null){
            if(applied!=null&&applied.equals(custom)){call(mapWorld,"setCustomDimensionId",(Object)null);call(processor,"checkForWorldUpdate");}
            applied=null;
            return;
        }
        if(exterior.dimension().equals(custom)){applied=exterior.dimension();return;}
        call(mapWorld,"setCustomDimensionId",exterior.dimension());
        call(processor,"checkForWorldUpdate");
        applied=exterior.dimension();
    }

    /** Called when the map screen is closed or replaced: put the map back, but only if the view is still the one we set. */
    public static void closed(Object gui) {
        try {
            if(applied==null||TardisLocation.current()!=null)return;
            Object processor=call(gui,"getMapProcessor"),mapWorld=call(processor,"getMapWorld");
            if(applied.equals(call(mapWorld,"getCustomDimensionId"))){call(mapWorld,"setCustomDimensionId",(Object)null);call(processor,"checkForWorldUpdate");}
            applied=null;
        } catch(RuntimeException|LinkageError failure) {
            if(!warned){warned=true;dev.fan4.compat.shared.GameLog.warn("TARDIS exterior on the Xaero map could not be undone: "+failure);}
        }
    }

    /**
     * Where the exterior really is. On a Valkyrien Skies ship DWM's own address is a shipyard block, far from the world
     * position; Fan4Compat's ship support keeps the transformed world position beside it (and syncs it to the client with the
     * rest of the state), so prefer that. Without ship data it is the same block DWM reports.
     */
    private static Object exteriorPosition(Object state) {
        try {
            Object shown=dev.fan4.compat.doctorwho.TardisShipCompat.displayPosition(state,"curr");
            if(shown!=null)return shown;
        } catch(RuntimeException|LinkageError unavailable){}
        return call(state,"getCurrentExteriorPosition");
    }

    /** The landed exterior of the TARDIS the player is inside, or null when unknown or in flight. */
    static Exterior exterior(Object world,Object player){Object state=state(world,player);return state==null?null:landed(state);}
    /** The landed exterior of a TARDIS state, or null while it is not materialized. */
    static Exterior landed(Object state) {
        if(!(Boolean)call(call(state,"getSystem",type(MATERIALIZATION)),"isMaterialized"))return null;
        Object position=exteriorPosition(state);
        return new Exterior(call(state,"getCurrentExteriorDimension"),(Integer)call(position,"method_10263"),(Integer)call(position,"method_10264"),(Integer)call(position,"method_10260"));
    }
    /**
     * How far the exterior's compass is turned from the interior's, in degrees, or NaN when it cannot be told. DWM puts a player who walks
     * out of the main interior door at the exterior's facing, and one who walks in at the interior door's facing, so what is straight ahead
     * when leaving (the door's facing turned half a circle) becomes the exterior's facing. The door positions are synced with the state;
     * the door's facing is read from the client's own copy of the interior.
     */
    static float yawOffset(Object world,Object state) {
        try {
            Object position=null;
            for(Object key:((Map<?,?>)call(state,"getInteriorDoorsTiles")).keySet())position=key;   // the last door is the main one
            if(position==null)return noOffset("the synced state lists no interior door");
            Object block=call(world,"method_8320",position),facing=facingOf(block);
            if(facing==null)return noOffset("the interior door block has no facing: "+block);
            float turn=((Float)call(call(state,"getCurrentExteriorFacing"),"method_10144")-(Float)call(facing,"method_10144")-180)%360;
            return turn<-180?turn+360:turn>180?turn-360:turn;
        } catch(RuntimeException|LinkageError unavailable){return noOffset(String.valueOf(unavailable));}
    }
    /** The value of a block state's "facing" property, or null when it has none. */
    private static Object facingOf(Object block) {
        for(Object property:(java.util.Collection<?>)call(block,"method_28501"))
            if("facing".equals(call(property,"method_11899")))return call(block,"method_11654",property);
        return null;
    }
    private static boolean warnedOffset;
    /** The compass stays as Xaero has it; say why once, since it is otherwise invisible. */
    private static float noOffset(String reason) {
        if(!warnedOffset){warnedOffset=true;dev.fan4.compat.shared.GameLog.warn("Xaero compass keeps the interior's orientation: "+reason);}
        return Float.NaN;
    }
    /**
     * Where the TARDIS is while it is not landed, for a coordinate display: x, y, z (block centre, as for a landed one), or null when
     * it is landed. DWM keeps the departure as the current position until it lands and sets the destination at the end, so in flight
     * the position is worked out from the departure, the destination and the flight's synced progress; before takeoff it is the
     * departure, and while materializing at the destination it is the destination.
     */
    static double[] travelling(Object state) {
        if((Boolean)call(call(state,"getSystem",type(MATERIALIZATION)),"isMaterialized"))return null;
        Object from=exteriorPosition(state),to=call(state,"getDestinationExteriorPosition"),flight=call(state,"getSystem",type(FLIGHT));
        double fraction=0;
        if((Boolean)call(flight,"inProgress")) {
            if((Boolean)call(flight,"isInFlight"))fraction=Math.max(0,Math.min(1,((Integer)call(flight,"getProgressPercent"))/100.0));
            else if("PROCESSING".equals(String.valueOf(field(flight,"step"))))fraction=1;
        }
        double x=(Integer)call(from,"method_10263"),y=(Integer)call(from,"method_10264"),z=(Integer)call(from,"method_10260");
        double dx=(Integer)call(to,"method_10263"),dy=(Integer)call(to,"method_10264"),dz=(Integer)call(to,"method_10260");
        return new double[]{x+(dx-x)*fraction+0.5,y+(dy-y)*fraction,z+(dz-z)*fraction+0.5};
    }
    /** The console's TARDIS state for the TARDIS the player is inside, or null when none is loaded nearby. */
    static Object state(Object world,Object player) {
        int centerX=(int)Math.floor((Double)call(player,"method_23317"))>>4,centerZ=(int)Math.floor((Double)call(player,"method_23321"))>>4;
        Object chunks=call(world,"method_2935"),full=field(type(STATUS),"field_12803");
        Class<?> console=type(CONSOLE);
        for(int radius=0;radius<=RADIUS;radius++)
            for(int dx=-radius;dx<=radius;dx++)
                for(int dz=-radius;dz<=radius;dz++) {
                    if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                    Object chunk=call(chunks,"method_2857",centerX+dx,centerZ+dz,full,false);
                    if(chunk==null)continue;
                    for(Object entity:((Map<?,?>)call(chunk,"method_12214")).values()) {
                        if(!console.isInstance(entity))continue;
                        return field(entity,"tardisStateManager");
                    }
                }
        return null;
    }
}
