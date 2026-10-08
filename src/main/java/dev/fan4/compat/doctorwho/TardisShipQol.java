package dev.fan4.compat.doctorwho;

import java.util.*;
import dev.fan4.compat.shared.ShipPose.Point;
import static dev.fan4.compat.shared.CompatCalls.*;
import static dev.fan4.compat.doctorwho.TardisShipCompat.*;

/** Optional DWM QoL hooks, preserving native waypoint codecs and access gates. */
public final class TardisShipQol {
    private static final String DELETED_WAYPOINTS="fan4compatDeletedShipWaypoints";
    private static final String DWM="net.drgmes.dwm.",FLIGHT=DWM+"common.tardis.systems.TardisSystemFlight";
    private record Chase(Point origin,double ticksPerBlock,int overhead,int required,long shipId) {}
    private record CreateAnchor(long ship,long raw,long displayed,String facing) {}
    private static final Map<Object,CreateAnchor> CREATIONS=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    private static final Map<Object,Chase> CHASES=Collections.synchronizedMap(new WeakHashMap<>());
    private static Point worldPoint(Object world,Object position){Object ship=shipAt(world,position);return ship==null?block(position):pose(ship,false).position(block(position));}
    private static double distance(Point a,Point b){return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.z()-b.z());}
    /** Snapshot the departure; do not move the starting point with its former ship. */
    public static void startFlight(Object flight){
        CHASES.remove(flight);
        if(!((Enum<?>)field(flight,"step")).name().equals("PROCESSING"))return;
        Object state=field(flight,"tardis"),world=exteriorWorld(state,"dest"),target=call(state,"getDestinationExteriorPosition"),ship=shipAt(world,target);
        if(ship==null)return;
        Point origin=worldPoint(exteriorWorld(state,"curr"),call(state,"getCurrentExteriorPosition"));
        int duration=((Number)field(flight,"duration")).intValue();
        int overhead=64+(call(state,"getCurrentExteriorDimension").equals(call(state,"getDestinationExteriorDimension"))?0:300);
        double initial=distance(origin,worldPoint(world,target));
        double rate=initial>0?Math.max(1,duration-overhead)/initial:836d/10000;
        CHASES.put(flight,new Chase(origin,rate,Math.min(overhead,duration),duration,((Number)call(ship,"getId")).longValue()));
    }
    public static void updateFlight(Object flight){
        if(!((Enum<?>)field(flight,"step")).name().equals("PROCESSING")){CHASES.remove(flight);return;}
        Chase chase=CHASES.get(flight);if(chase==null)return;
        Object state=field(flight,"tardis"),world=exteriorWorld(state,"dest"),target=call(state,"getDestinationExteriorPosition"),ship=shipAt(world,target);
        if(ship==null||((Number)call(ship,"getId")).longValue()!=chase.shipId)return;
        int required=extendedDuration(chase.required,chase.overhead,chase.ticksPerBlock,distance(chase.origin,worldPoint(world,target)));
        int extra=required-chase.required;if(extra<=0)return;
        int tick=((Number)field(flight,"tick")).intValue(),duration=((Number)field(flight,"duration")).intValue();
        extra=Math.min(extra,Integer.MAX_VALUE-Math.max(tick,duration));if(extra<=0)return;
        writeField(flight,"tick",tick+extra);writeField(flight,"duration",duration+extra);
        CHASES.put(flight,new Chase(chase.origin,chase.ticksPerBlock,chase.overhead,chase.required+extra,chase.shipId));
        call(state,"method_80");call(state,"markConsoleTilesUpdated");
    }
    public static int extendedDuration(int prior,int overhead,double ticksPerBlock,double distance){
        return Math.max(prior,(int)Math.min(Integer.MAX_VALUE,overhead+Math.ceil(Math.max(0,distance)*ticksPerBlock)));
    }
    public static void writeFlight(Object flight,Object tag){
        writeWaypointStatus(flight,tag);
        Chase chase=CHASES.get(flight);if(chase==null)return;
        call(tag,"method_10582","fan4compatChase",chase.origin.x()+","+chase.origin.y()+","+chase.origin.z()+","+chase.ticksPerBlock+","+chase.overhead+","+chase.required+","+chase.shipId);
    }
    public static void readFlight(Object flight,Object tag){
        CHASES.remove(flight);if(!(Boolean)call(tag,"method_10545","fan4compatChase"))return;
        try{
            String[] s=((String)call(tag,"method_10558","fan4compatChase")).split(",");if(s.length!=7)return;
            Point origin=new Point(Double.parseDouble(s[0]),Double.parseDouble(s[1]),Double.parseDouble(s[2]));double rate=Double.parseDouble(s[3]);int overhead=Integer.parseInt(s[4]),required=Integer.parseInt(s[5]);long ship=Long.parseLong(s[6]);
            if(!Double.isFinite(origin.x())||!Double.isFinite(origin.y())||!Double.isFinite(origin.z())||!Double.isFinite(rate)||rate<=0||overhead<0||required<overhead)return;
            CHASES.put(flight,new Chase(origin,rate,overhead,required,ship));
        }catch(IllegalArgumentException ignored){}
    }
    public static Object leverPosition(Object state){
        Object world=exteriorWorld(state,"dest"),raw=call(state,"getDestinationExteriorPosition"),ship=shipAt(world,raw);
        if(ship==null)return raw;
        Point p=block(raw);return pos(pose(ship,false).position(new Point(p.x()+.5,p.y(),p.z()+.5)));
    }
    /** A local ship landing uses Direct only for this lookup, never changes the console setting. */
    public static Object scanning(Object materialization,Object world,Object target,Object selected){
        return shipAt(world,target)==null?selected:field(type(DWM+"enums.TardisVerticalScanning"),"DIRECT");
    }
    private static ShipWaypoint waypoint(Object entry){return ShipWaypoint.decode((String)call(entry,"id"));}
    private static Object stored(Object flight,Object entry){
        for(Object candidate:(List<?>)field(flight,"waypoints"))if(call(candidate,"id").equals(call(entry,"id")))return candidate;return null;
    }
    public static boolean immutableWaypoint(Object flight,Object oldEntry,Object newEntry){
        Object original=stored(flight,oldEntry);return waypoint(newEntry)!=null||(original!=null&&waypoint(original)!=null);
    }
    /** Called inside DWM's existing create packet handler, with the acting player. */
    public static void addWaypoint(Object flight,Object entry,Object player){
        Object state=field(flight,"tardis"),world=exteriorWorld(state,"curr"),raw=call(state,"getCurrentExteriorPosition"),ship=shipAt(world,raw);
        ShipWaypoint requested=waypoint(entry);
        if(requested!=null){
            if(ship==null||((Number)call(ship,"getId")).longValue()!=requested.shipId()
                ||!call(entry,"dimension").equals(call(state,"getCurrentExteriorDimension"))
                ||packed(call(entry,"blockPos"))!=packed(raw)||(Boolean)call(flight,"inProgress")
                ||!(Boolean)call(state,"checkAccess",player,true,false))return;
        }else if(ship!=null&&call(entry,"dimension").equals(call(state,"getCurrentExteriorDimension"))
            &&packed(call(entry,"blockPos"))==packed(displayPosition(state,"curr"))){
            if((Boolean)call(flight,"inProgress")||!(Boolean)call(state,"checkAccess",player,true,false))return;
            ShipWaypoint anchor=new ShipWaypoint(((Number)call(ship,"getId")).longValue(),(String)call(entry,"id"));
            entry=copyWaypoint(entry,anchor.encode(),raw,call(state,"getCurrentExteriorFacing"),call(entry,"name")+" (Ship)");
        }
        call(flight,"addWaypointEntry",entry);
    }
    /** Handle only ship waypoints. Resolve the stored entry and enforce normal edit access. */
    public static boolean applyWaypoint(Object state,Object packet,Object player){
        Object requested=call(packet,"waypointEntry");if(waypoint(requested)==null)return false;
        if(!(Boolean)call(state,"checkAccess",player,true,false))return true;
        Object flight=call(state,"getSystem",type(FLIGHT));
        if(!(Boolean)call(flight,"isEnabled")||(Boolean)call(flight,"inProgress"))return true;
        Object entry=stored(flight,requested);if(entry==null)return true;
        ShipWaypoint anchor=waypoint(entry);Object world=call(call(call(state,"getWorld"),"method_8503"),"method_3847",call(entry,"dimension"));
        Object ship=shipAt(world,call(entry,"blockPos"));
        if(ship==null||((Number)call(ship,"getId")).longValue()!=anchor.shipId()){
            call(player,"method_7353",call(type("net.minecraft.class_2561"),"method_43471","fan4compat.waypoint.ship_unavailable"),true);return true;
        }
        call(state,"setDestinationDimension",call(entry,"dimension"));call(state,"setDestinationPosition",call(entry,"blockPos"));call(state,"setDestinationFacing",call(entry,"facing"));
        rememberShipTarget(state,ship,call(entry,"blockPos"),world);
        call(state,"markConsoleTilesUpdated");
        call(player,"method_7353",field(type(DWM+"DWM$TEXTS"),"MONITOR_WAYPOINT_LOADED"),true);return true;
    }
    /** Server snapshots use all ship data, including ships whose chunks are unloaded. */
    private static void writeWaypointStatus(Object flight,Object tag){
        Object state=field(flight,"tardis");if(state==null)return;
        Object world=call(state,"getWorld");
        // Detached console states can be serialized without an attached world.
        // Preserve their saved data; missing world context is not a deleted ship.
        if(world==null||(Boolean)field(world,"field_9236"))return;
        Object server=call(world,"method_8503");if(server==null)return;
        Object ships=exact("org.valkyrienskies.mod.common.VSGameUtilsKt","getShipObjectWorld",
            new String[]{"net.minecraft.server.MinecraftServer"},server);
        Object all=call(ships,"getAllShips");List<String> deleted=new ArrayList<>();
        for(Object entry:(List<?>)field(flight,"waypoints")){
            ShipWaypoint anchor=waypoint(entry);
            if(anchor!=null&&call(all,"getById",anchor.shipId())==null)deleted.add((String)call(entry,"id"));
        }
        call(tag,"method_10582",DELETED_WAYPOINTS,String.join("\n",deleted));
    }
    public static boolean deletedWaypoint(Object screen,Object entry){
        if(entry==null||waypoint(entry)==null)return false;
        Object tag=call(call(field(screen,"tag"),"method_10562","tardisTag"),"method_10562","TardisSystemFlight");
        String id=(String)call(entry,"id"),deleted=(String)call(tag,"method_10558",DELETED_WAYPOINTS);
        return Arrays.asList(deleted.split("\n")).contains(id);
    }
    public static Object waypointText(Object row,Object text){
        Object screen=field(field(row,"this$0"),"parent"),entry=field(row,"waypointEntry");
        return deletedWaypoint(screen,entry)?call(text,"method_27692",field(type("net.minecraft.class_124"),"field_1055")):text;
    }
    /** Reuse the native coordinate label position; wrap within the detail column. */
    public static int coordinateLabel(Object screen,Object context,Object renderer,Object text,int x,int y,int color,boolean shadow){
        if(text==field(type(DWM+"DWM$TEXTS"),"MONITOR_WAYPOINTS_COORDS")&&shipSelected(screen)){
            Object entry=field(field(screen,"selected"),"waypointEntry");
            if(deletedWaypoint(screen,entry)){
                Object message=call(type("net.minecraft.class_2561"),"method_43471","fan4compat.waypoint.ship_deleted");
                int width=((Number)call(field(screen,"nameField"),"method_25368")).intValue();
                call(context,"method_51440",renderer,message,x,y,Math.max(1,width),color);
            }
            return 0;
        }
        return ((Number)call(context,"method_51439",renderer,text,x,y,color,shadow)).intValue();
    }
    private static Object copyWaypoint(Object entry,String id,Object position,Object facing,Object name){
        return createExact(DWM+"common.tardis.systems.flight.TardisFlightWaypointEntry",
            new String[]{"java.lang.String","net.minecraft.class_5321","net.minecraft.class_2338","net.minecraft.class_2350","java.lang.String","long"},
            id,call(entry,"dimension"),position,facing,name,call(entry,"timestamp"));
    }
    public static void rememberCreation(Object screen){
        Object parent=field(screen,"parentScreen"),origin=field(screen,"originBlockPos");
        if(parent==null||origin==null||!type(DWM+"blocks.tardis.consoleunits.screens.TardisConsoleUnitMonitorWaypointsScreen").isInstance(parent))return;
        Object tag=call(field(parent,"tag"),"method_10562","tardisTag");if(!(Boolean)call(tag,"method_10545","fan4compatCurrentShip"))return;
        long displayed=((Number)call(tag,"method_10537","fan4compatWorld_currPosition")).longValue();
        if(packed(origin)!=displayed)return;
        CREATIONS.put(screen,new CreateAnchor(((Number)call(tag,"method_10537","fan4compatCurrentShip")).longValue(),((Number)call(tag,"method_10537","fan4compatCurrentRaw")).longValue(),displayed,(String)call(tag,"method_10558","fan4compatCurrentFacing")));
    }
    public static Object createdWaypoint(Object screen,Object entry){
        CreateAnchor anchor=CREATIONS.get(screen);
        if(anchor==null||packed(call(entry,"blockPos"))!=anchor.displayed)return entry;
        ShipWaypoint binding=new ShipWaypoint(anchor.ship,(String)call(entry,"id"));
        return copyWaypoint(entry,binding.encode(),unpack(anchor.raw),direction(anchor.facing),call(entry,"name")+" (Ship)");
    }
    public static boolean creationSelected(Object screen){return CREATIONS.containsKey(screen);}
    public static void updateCreation(Object screen){
        if(!CREATIONS.containsKey(screen))return;
        for(String name:List.of("xField","yField","zField")){Object widget=field(screen,name);if(widget!=null){call(widget,"method_1862",false);call(widget,"method_25365",false);call(widget,"method_1888",false);}}
        for(String name:List.of("resetXButton","resetYButton","resetZButton")){Object widget=field(screen,name);if(widget!=null){writeField(widget,"field_22764",false);writeField(widget,"field_22763",false);}}
    }
    public static boolean shipSelected(Object screen){Object selection=field(screen,"selected");return selection!=null&&waypoint(field(selection,"waypointEntry"))!=null;}
    public static void updateScreen(Object screen){
        if(!shipSelected(screen))return;
        for(String name:List.of("xField","yField","zField")){Object widget=field(screen,name);if(widget!=null){call(widget,"method_1862",false);call(widget,"method_25365",false);call(widget,"method_1888",false);}}
        Object name=field(screen,"nameField");if(name!=null)call(name,"method_1888",false);
        for(String button:List.of("updateButton","resetNameButton","resetXButton","resetYButton","resetZButton")){Object widget=field(screen,button);if(widget!=null){writeField(widget,"field_22763",false);writeField(widget,"field_22764",false);}}
    }
}
