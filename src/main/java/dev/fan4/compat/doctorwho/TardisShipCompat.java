package dev.fan4.compat.doctorwho;

import dev.fan4.compat.shared.ShipPose;
import dev.fan4.compat.immersiveportals.PortalMotionCompat;
import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;
import static dev.fan4.compat.shared.ShipPose.*;

/** Preserve DWM's shipyard block addresses; transform only display and portal geometry. */
public final class TardisShipCompat {
    private static final String UTILS="org.valkyrienskies.mod.common.VSGameUtilsKt";
    private static final String MC="net.minecraft.";
    private static final String PREFIX="fan4compatWorld_";
    private record Display(long position, String facing) {}
    private record Frozen(long raw,String dimension,Display display) {}
    private static final Map<Object,Map<String,Frozen>> FROZEN=Collections.synchronizedMap(new WeakHashMap<>());
    private record PortalPose(Object inner, Object outer, Point position, Point right, Point up, double width, double height) {}
    private record Exterior(long block,long shipId) {}
    private static final Map<Object,Exterior> EXTERIORS=Collections.synchronizedMap(new dev.fan4.compat.shared.WeakIdentityMap<>());
    private record Recall(long shipId, long position, String dimension) {}
    private static final Map<Object,Map<String,Display>> DISPLAYS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object,PortalPose> PORTALS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object,Recall> RECALLS=Collections.synchronizedMap(new WeakHashMap<>());

    static Object shipAt(Object world,Object pos) {
        if(world==null) return null;
        return exact(UTILS,"getShipManagingPos",new String[]{MC+"class_1937",MC+"class_2338"},world,pos);
    }
    static Point block(Object pos) { return new Point(number(call(pos,"method_10263")),number(call(pos,"method_10264")),number(call(pos,"method_10260"))); }
    private static Point vector(Object vec) { return new Point(number(field(vec,"field_1352")),number(field(vec,"field_1351")),number(field(vec,"field_1350"))); }
    private static Object vec(Point p) { return create(MC+"class_243",p.x(),p.y(),p.z()); }
    static Object pos(Point p) { return create(MC+"class_2338",(int)Math.floor(p.x()),(int)Math.floor(p.y()),(int)Math.floor(p.z())); }
    private static double number(Object value) { return ((Number)value).doubleValue(); }
    static long packed(Object pos) { return ((Number)call(pos,"method_10063")).longValue(); }
    static Object unpack(long value) { return call(type(MC+"class_2338"),"method_10092",value); }
    static Point facing(Object dir) {
        return switch(((Enum<?>)dir).name()) { case "EAST" -> new Point(1,0,0); case "WEST" -> new Point(-1,0,0); case "SOUTH" -> new Point(0,0,1); default -> new Point(0,0,-1); };
    }
    static Object direction(String name) { return call(type(MC+"class_2350"),"method_10168",name); }
    static ShipPose pose(Object ship,boolean inverse) {
        Object matrix=call(ship,inverse?"getWorldToShip":"getShipToWorld");
        double[] values=new double[16];
        for(int col=0;col<4;col++) for(int row=0;row<4;row++) values[col*4+row]=number(call(matrix,"m"+col+row));
        return new ShipPose(values);
    }
    static Object exteriorWorld(Object state,String slot) {
        if(call(state,"getWorld")==null) return null;
        if(slot.equals("curr")) return call(state,"getExteriorWorld");
        if(slot.equals("dest")) return call(state,"getDestinationExteriorWorld");
        Object server=call(call(state,"getWorld"),"method_8503");
        return call(server,"method_3847",call(state,"getPreviousExteriorDimension"));
    }
    private static String getter(String slot) { return switch(slot) { case "prev" -> "Previous"; case "dest" -> "Destination"; default -> "Current"; }; }
    private static Display display(Object state,String slot) {
        String suffix=getter(slot);
        Object raw=call(state,"get"+suffix+"ExteriorPosition"),dir=call(state,"get"+suffix+"ExteriorFacing");
        Object world=exteriorWorld(state,slot);
        if(world==null) {
            Map<String,Display> saved=DISPLAYS.get(state);
            return saved!=null&&saved.containsKey(slot) ? saved.get(slot) : new Display(packed(raw),cardinal(facing(dir)));
        }
        Object ship=shipAt(world,raw);
        if(ship==null) return new Display(packed(raw),cardinal(facing(dir)));
        ShipPose transform=pose(ship,false); Point p=block(raw);
        Point centre=transform.position(new Point(p.x()+.5,p.y(),p.z()+.5));
        Display live=new Display(packed(pos(centre)),cardinal(transform.direction(facing(dir))));
        Map<String,Frozen> frozen=FROZEN.computeIfAbsent(state,k->new HashMap<>());
        if(!slot.equals("curr") && packed(raw)==packed(call(state,"getCurrentExteriorPosition")) && world==exteriorWorld(state,"curr")) {
            String dimension=call(world,"method_27983").toString();Frozen old=frozen.get(slot);
            if(old!=null&&old.raw==packed(raw)&&old.dimension.equals(dimension))return new Display(old.display.position,live.facing);
            frozen.put(slot,new Frozen(packed(raw),dimension,live));
        }else frozen.remove(slot);
        return live;
    }
    public static Object displayPosition(Object state,String slot) { return unpack(display(state,slot).position); }
    public static Object displayFacing(Object state,String slot) { return direction(display(state,slot).facing); }
    public static void write(Object state,Object tag) {
        for(String slot:List.of("prev","curr","dest")) {
            Display d=display(state,slot);
            call(tag,"method_10544",PREFIX+slot+"Position",d.position);
            call(tag,"method_10582",PREFIX+slot+"Facing",d.facing);
            Map<String,Frozen> frozen=FROZEN.get(state);Frozen snapshot=frozen==null?null:frozen.get(slot);
            if(snapshot!=null){
                call(tag,"method_10544",PREFIX+slot+"FrozenRaw",snapshot.raw);
                call(tag,"method_10582",PREFIX+slot+"FrozenDimension",snapshot.dimension);
            }
        }
        Object currentRaw=call(state,"getCurrentExteriorPosition"),currentShip=shipAt(exteriorWorld(state,"curr"),currentRaw);
        if(currentShip!=null){
            call(tag,"method_10544","fan4compatCurrentShip",call(currentShip,"getId"));
            call(tag,"method_10544","fan4compatCurrentRaw",packed(currentRaw));
            call(tag,"method_10582","fan4compatCurrentFacing",cardinal(facing(call(state,"getCurrentExteriorFacing"))));
        }
        Recall recall=RECALLS.get(state);
        if(recall!=null) {
            call(tag,"method_10544","fan4compatRecallShip",recall.shipId);
            call(tag,"method_10544","fan4compatRecallPosition",recall.position);
            call(tag,"method_10582","fan4compatRecallDimension",recall.dimension);
        }
    }
    public static void read(Object state,Object tag) {
        Map<String,Display> map=new HashMap<>();
        for(String slot:List.of("prev","curr","dest")) if((Boolean)call(tag,"method_10545",PREFIX+slot+"Position"))
            map.put(slot,new Display(((Number)call(tag,"method_10537",PREFIX+slot+"Position")).longValue(),(String)call(tag,"method_10558",PREFIX+slot+"Facing")));
        DISPLAYS.put(state,map);
        Map<String,Frozen> frozen=new HashMap<>();
        for(String slot:List.of("prev","dest"))if(map.containsKey(slot)&&(Boolean)call(tag,"method_10545",PREFIX+slot+"FrozenRaw"))
            frozen.put(slot,new Frozen(((Number)call(tag,"method_10537",PREFIX+slot+"FrozenRaw")).longValue(),(String)call(tag,"method_10558",PREFIX+slot+"FrozenDimension"),map.get(slot)));
        FROZEN.put(state,frozen);
        if((Boolean)call(tag,"method_10545","fan4compatRecallShip")) RECALLS.put(state,new Recall(
            ((Number)call(tag,"method_10537","fan4compatRecallShip")).longValue(),
            ((Number)call(tag,"method_10537","fan4compatRecallPosition")).longValue(),
            (String)call(tag,"method_10558","fan4compatRecallDimension")));
        else RECALLS.remove(state);
    }
    public static long waypointPosition(Object tag,String key) {
        String converted=key.equals("currExteriorPosition") ? PREFIX+"currPosition" : key;
        if(!(Boolean)call(tag,"method_10545",converted)) converted=key;
        return ((Number)call(tag,"method_10537",converted)).longValue();
    }
    public static String waypointString(Object tag,String key) {
        String converted=key.equals("currExteriorFacing") ? PREFIX+"currFacing" : key;
        if(!(Boolean)call(tag,"method_10545",converted)) converted=key;
        return (String)call(tag,"method_10558",converted);
    }
    public static void tick(Object state) {
        if(call(state,"getWorld")==null) return;
        Map<String,Display> map=new HashMap<>();
        for(String slot:List.of("prev","curr","dest")) map.put(slot,display(state,slot));
        Map<String,Display> old=DISPLAYS.put(state,map);
        if(!map.equals(old)) call(state,"markConsoleTilesUpdated");
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("immersive_portals")) updatePortals(call(state,"getPortalsState"));
    }
    public static void updatePortals(Object portalsState) {
        Object inner=field(portalsState,"portalFromTardis"),outer=field(portalsState,"portalToTardis");
        if(inner==null||outer==null) { PORTALS.remove(portalsState); return; }
        Object state=field(portalsState,"tardis"),raw=call(state,"getCurrentExteriorPosition");
        Object ship=shipAt(call(state,"getExteriorWorld"),raw);
        if(ship==null) { PORTALS.remove(portalsState); EXTERIORS.remove(outer); PortalMotionCompat.detach(inner); PortalMotionCompat.detach(outer); return; }
        EXTERIORS.put(outer,new Exterior(packed(raw),((Number)call(ship,"getId")).longValue()));
        ShipPose transform=pose(ship,false); Point face=facing(call(state,"getCurrentExteriorFacing"));
        Point position=transform.position(door(block(raw),face));
        Point right=transform.direction(new Point(face.z(),0,-face.x())),up=transform.direction(new Point(0,1,0));
        Object exterior=call(state,"getExteriorType");
        double width=exterior==null?1:number(field(exterior,"entranceWidth"));
        double height=exterior==null?2:number(field(exterior,"entranceHeight"));
        Point localDoor=door(block(raw),face),localRight=new Point(face.z(),0,-face.x());
        long shipId=((Number)call(ship,"getId")).longValue();
        PortalMotionCompat.attach(outer,shipId,false,localDoor,localRight,new Point(0,1,0),width,height);
        PortalMotionCompat.attach(inner,shipId,true,localDoor,localRight,new Point(0,1,0),width,height);
        PortalPose updated=new PortalPose(inner,outer,position,right,up,width,height);
        PortalPose previous=PORTALS.get(portalsState);
        if(updated.equals(previous)) return;
        if(previous==null||previous.inner!=inner||previous.outer!=outer) {
            call(inner,"disableDefaultAnimation");call(outer,"disableDefaultAnimation");
        }
        call(outer,"setOriginPos",vec(position));
        call(outer,"setOrientationAndSize",vec(right.normalized()),vec(up.normalized()),width*right.length(),height*up.length());
        call(inner,"setDestination",vec(position));
        call(outer,"setDestination",call(inner,"getOriginPos"));
        call(inner,"setScaling",right.length()); call(outer,"setScaling",1/right.length());
        call(type("qouteall.imm_ptl.core.portal.PortalManipulation"),"adjustRotationToConnect",inner,outer);
        call(inner,"reloadAndSyncToClientNextTick"); call(outer,"reloadAndSyncToClientNextTick");
        PORTALS.put(portalsState,updated);
    }

    public static void writeExteriorPortal(Object portal,Object tag) {
        PortalMotionCompat.write(portal,tag);
        Exterior exterior=EXTERIORS.get(portal);
        if(exterior!=null) {
            call(tag,"method_10544","fan4compatExteriorBlock",exterior.block);
            call(tag,"method_10544","fan4compatExteriorShip",exterior.shipId);
        }
    }
    public static void readExteriorPortal(Object portal,Object tag) {
        if((Boolean)call(tag,"method_10545","fan4compatExteriorBlock")&&(Boolean)call(tag,"method_10545","fan4compatExteriorShip"))
            EXTERIORS.put(portal,new Exterior(((Number)call(tag,"method_10537","fan4compatExteriorBlock")).longValue(),((Number)call(tag,"method_10537","fan4compatExteriorShip")).longValue()));
        else EXTERIORS.remove(portal);
    }
    private static Object stateProperty(Object state,Object property) {
        return callTyped(MC+"class_2688","method_11654","java.lang.Comparable",state,new String[]{MC+"class_2769"},property);
    }
    /** Cut only the front doorway at the shape source, before VS builds polygons. */
    public static boolean openExteriorShape(Object blockState,Object view,Object queriedPos) {
        if(!type(MC+"class_1937").isInstance(view))return false;
        Object open=field(type("net.drgmes.dwm.blocks.tardis.exteriors.BaseTardisExteriorBlock"),"OPEN");
        if(!(Boolean)stateProperty(blockState,open))return false;
        Object ship=shipAt(view,queriedPos);if(ship==null)return false;
        Point queried=block(queriedPos);
        synchronized(EXTERIORS) {
            for(var entry:EXTERIORS.entrySet()) {
                Point lower=block(unpack(entry.getValue().block));
                if(((Number)call(ship,"getId")).longValue()!=entry.getValue().shipId||queried.x()!=lower.x()||queried.z()!=lower.z()||(queried.y()!=lower.y()&&queried.y()!=lower.y()+1))continue;
                Object portal=entry.getKey();
                if(call(portal,"method_37908")==view&&!(Boolean)call(portal,"method_31481")&&(Boolean)call(portal,"isPortalValid")&&(Boolean)call(portal,"isTeleportable"))return true;
            }
        }
        return false;
    }
    public static Object doorwayShape(Object state,Object view,Object pos,Object original) {
        boolean eligible=openExteriorShape(state,view,pos);Object result=original;
        if(eligible) {
            Object base=type("net.drgmes.dwm.blocks.tardis.exteriors.BaseTardisExteriorBlock");
            Point face=facing(stateProperty(state,field(base,"FACING")));
            Object exterior=field(callTyped(MC+"class_4970$class_4971","method_26204",MC+"class_2248",state,new String[]{}),"exteriorType");
            double width=number(field(exterior,"entranceWidth"));double[] cut=doorwayCut(face,width);
            Object shape=call(type(MC+"class_259"),"method_1081",cut[0],cut[1],cut[2],cut[3],cut[4],cut[5]);
            result=call(type(MC+"class_259"),"method_1072",original,shape,field(type(MC+"class_247"),"field_16886"));
        }
        return result;
    }
    /** Front half only; preserve at least one model pixel of wall at both edges. */
    public static double[] doorwayCut(Point face,double width) {
        double half=Math.min(Math.max(width,0),.875)/2,lo=.5-half,hi=.5+half;
        if(face.x()>0)return new double[]{.5,0,lo,1,1,hi};
        if(face.x()<0)return new double[]{0,0,lo,.5,1,hi};
        if(face.z()>0)return new double[]{lo,0,.5,hi,1,1};
        return new double[]{lo,0,0,hi,1,.5};
    }
    public static int keyDistance(Object world,Object exterior,Object playerPosition) {
        Object ship=shipAt(world,exterior);
        Object measured=ship==null?exterior:pos(pose(ship,false).position(block(exterior)));
        return ((Number)call(measured,"method_19455",playerPosition)).intValue();
    }

    public static void sonicMessage(Object player,Object original,boolean overlay,Object world,Object hit) {
        Object raw=call(call(hit,"method_17777"),"method_10084"),ship=shipAt(world,raw);
        Object message=original;
        if(ship!=null) {
            Point p=block(raw);Object displayed=pos(pose(ship,false).position(new Point(p.x()+.5,p.y(),p.z()+.5)));
            Point coordinates=block(displayed);
            Object yellow=field(type(MC+"class_124"),"field_1054");
            List<Object> text=new ArrayList<>();
            for(double value:new double[]{coordinates.x(),coordinates.y(),coordinates.z()}) {
                Object literal=call(type(MC+"class_2561"),"method_43470",String.valueOf((int)value));
                text.add(call(literal,"method_27692",yellow));
            }
            @SuppressWarnings("unchecked") java.util.function.Function<List<Object>,Object> localized=(java.util.function.Function<List<Object>,Object>)field(type("net.drgmes.dwm.DWM$TEXTS"),"SONIC_DEVICE_TARDIS_RELOCATED");
            message=call(localized.apply(text),"method_27693"," (On ship)");
        }
        call(player,"method_7353",message,overlay);
    }

    private record Deck(Object ship,Object target,double distance) {}
    /** Read supporting deck blocks directly when another mod bypasses VS's raycast wrapper. */
    private static Deck supportingDeck(Object world,Point feet) {
        Object bounds=create(MC+"class_238",feet.x()-.6,feet.y()-1.5,feet.z()-.6,feet.x()+.6,feet.y()+.3,feet.z()+.6);
        Iterable<?> ships=(Iterable<?>)exact(UTILS,"getShipsIntersecting",new String[]{MC+"class_1937",MC+"class_238"},world,bounds);
        Deck closest=null;
        for(Object ship:ships) {
            ShipPose transform=pose(ship,false);
            if(transform.direction(new Point(0,1,0)).normalized().y()<.5) continue;
            Point local=pose(ship,true).position(feet);
            int x=(int)Math.floor(local.x()),z=(int)Math.floor(local.z());
            for(int y=(int)Math.floor(local.y());y>=(int)Math.floor(local.y())-2;y--) {
                double gap=local.y()-(y+1);
                if(gap<-.15||gap>1.25) continue;
                Object support=pos(new Point(x,y,z));
                Object blockState=call(world,"method_8320",support);
                if(!(Boolean)call(type("net.drgmes.dwm.utils.helpers.WorldHelper"),"checkBlockIsSolid",blockState)) continue;
                Point surface=transform.position(new Point(local.x(),y+1,local.z()));
                double distance=Math.pow(surface.x()-feet.x(),2)+Math.pow(surface.y()-feet.y(),2)+Math.pow(surface.z()-feet.z(),2);
                if(closest==null||distance<closest.distance) closest=new Deck(ship,call(support,"method_10084"),distance);
            }
        }
        return closest;
    }
    /** Called after vanilla recall guards/setters, immediately before flight begins. */
    public static void prepareRecall(Object state,Object player,Object world) {
        RECALLS.remove(state);
        Point feet=vector(call(player,"method_19538"));
        Object shape=field(type(MC+"class_3959$class_3960"),"field_17558"); // COLLIDER
        Object fluid=field(type(MC+"class_3959$class_242"),"field_1348"); // NONE
        Object ctx=create(MC+"class_3959",vec(new Point(feet.x(),feet.y()+.15,feet.z())),vec(new Point(feet.x(),feet.y()-1.25,feet.z())),shape,fluid,player);
        Object hit=exact("org.valkyrienskies.mod.common.world.RaycastUtilsKt","clipIncludeShips",
            new String[]{MC+"class_1937",MC+"class_3959","boolean","java.lang.Long","boolean"},world,ctx,false,null,false);
        Object target=null,ship=null;
        if(!((Enum<?>)call(hit,"method_17783")).name().equals("MISS")) {
            Object support=call(hit,"method_17777");ship=shipAt(world,support);
            if(ship!=null&&((Enum<?>)call(hit,"method_17780")).name().equals("UP")) target=call(support,"method_10084");
        }
        if(target==null) {
            Deck deck=supportingDeck(world,feet);
            if(deck!=null) { ship=deck.ship;target=deck.target; }
        }
        if(target==null) {
            return;
        }
        Point worldFacing=facing(call(state,"getDestinationExteriorFacing"));
        Object localFacing=direction(cardinal(pose(ship,true).direction(worldFacing)));
        call(state,"setDestinationPosition",target); call(state,"setDestinationFacing",localFacing);
        String dimension=call(call(world,"method_27983"),"method_29177").toString();
        RECALLS.put(state,new Recall(((Number)call(ship,"getId")).longValue(),packed(target),dimension));
    }
    static void rememberShipTarget(Object state,Object ship,Object target,Object world) {
        RECALLS.put(state,new Recall(((Number)call(ship,"getId")).longValue(),packed(target),call(call(world,"method_27983"),"method_29177").toString()));
    }
    /** Merge a ship surface into the same directional scan as DWM's terrain result. */
    public static Object scanShipLanding(Object materialization,Object world,Object target,Object worldFacing,Object scanning,Object terrain) {
        String mode=((Enum<?>)scanning).name();
        if(!mode.equals("TOP")&&!mode.equals("BOTTOM"))return terrain;
        if(shipAt(world,target)!=null)return terrain; // Already-local recalls keep the native scan.
        Point requested=block(target);
        double end=((Number)call(world,mode.equals("TOP")?"method_31600":"method_31607")).doubleValue();
        double start=requested.y()+.5;
        if(mode.equals("TOP")?end<=start:end>=start)return terrain;
        Object shape=field(type(MC+"class_3959$class_3960"),"field_17558");
        Object fluid=field(type(MC+"class_3959$class_242"),"field_1348");
        Object ctx=createExact(MC+"class_3959",new String[]{MC+"class_243",MC+"class_243",MC+"class_3959$class_3960",MC+"class_3959$class_242",MC+"class_1297"},vec(new Point(requested.x()+.5,start,requested.z()+.5)),vec(new Point(requested.x()+.5,end,requested.z()+.5)),shape,fluid,null);
        Object hit=exact("org.valkyrienskies.mod.common.world.RaycastUtilsKt","clipIncludeShips",
            new String[]{MC+"class_1937",MC+"class_3959","boolean","java.lang.Long","boolean"},world,ctx,false,null,true);
        if(((Enum<?>)call(hit,"method_17783")).name().equals("MISS")) {
            return terrain;
        }
        Object support=call(hit,"method_17777"),ship=shipAt(world,support);
        if(ship==null)return terrain;
        Object local=call(support,"method_10084");
        Object localFacing=direction(cardinal(pose(ship,true).direction(facing(worldFacing))));
        Object direct=field(type("net.drgmes.dwm.enums.TardisVerticalScanning"),"DIRECT");
        // Reuse DWM's support, two-block clearance, facing and build-height checks.
        Object candidate=call(materialization,"findLandingSpot",world,local,localFacing,direct);
        if(candidate==null) {
            return terrain;
        }
        Point localPosition=block(call(candidate,"pos"));
        double height=pose(ship,false).position(new Point(localPosition.x()+.5,localPosition.y(),localPosition.z()+.5)).y();
        double terrainHeight=terrain==null?Double.NaN:block(call(terrain,"pos")).y();
        if(!preferShipSurface(mode,requested.y(),height,terrainHeight))return terrain;
        return candidate;
    }
    public static boolean preferShipSurface(String mode,double requested,double ship,double terrain) {
        if(mode.equals("TOP"))return ship>=requested&&(Double.isNaN(terrain)||ship<terrain);
        if(mode.equals("BOTTOM"))return ship<=requested&&(Double.isNaN(terrain)||ship>terrain);
        return false;
    }
    /** A deleted/replaced ship must never redirect a pending recall onto terrain. */
    public static boolean invalidRecall(Object materialization,Object world,Object target) {
        Object state=field(materialization,"tardis"); Recall recall=RECALLS.get(state);
        if(recall==null||recall.position!=packed(target)) return false;
        String dimension=call(call(world,"method_27983"),"method_29177").toString();
        if(!recall.dimension.equals(dimension)) return false;
        Object ship=shipAt(world,target);
        return ship==null||((Number)call(ship,"getId")).longValue()!=recall.shipId;
    }
    /** The flyover renderer expects world blocks; use ordinary demat/remat on ship trips. */
    public static boolean flyoverEnabled(Object state) {
        if(!(Boolean)call(state,"isFlyoverEnabled")) return false;
        return shipAt(exteriorWorld(state,"curr"),call(state,"getCurrentExteriorPosition"))==null
            &&shipAt(exteriorWorld(state,"dest"),call(state,"getDestinationExteriorPosition"))==null;
    }
    public static int flightDistance(Object flight,Object from,Object to) {
        Object state=field(flight,"tardis");
        Object currentShip=shipAt(exteriorWorld(state,"curr"),from),destShip=shipAt(exteriorWorld(state,"dest"),to);
        Point a=block(from),b=block(to);
        if(currentShip!=null) a=pose(currentShip,false).position(a);
        if(destShip!=null) b=pose(destShip,false).position(b);
        return (int)Math.min(Integer.MAX_VALUE,Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.z()-b.z()));
    }
}
