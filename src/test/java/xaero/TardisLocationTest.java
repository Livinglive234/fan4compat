import dev.fan4.compat.shared.Features;
import dev.fan4.compat.xaero.TardisLocation;
import java.nio.file.*;
import java.util.*;
import net.drgmes.dwm.blocks.tardis.consoleunits.BaseTardisConsoleUnitBlockEntity;

public final class TardisLocationTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static final class XPath {final String text;XPath(String text){this.text=text;}public XPath resolve(String node){return new XPath(text+"/"+node);}
        public boolean equals(Object o){return o instanceof XPath p&&p.text.equals(text);}public int hashCode(){return text.hashCode();}public String toString(){return text;}}
    public static final class State {XPath custom;final XPath root=new XPath("Multiplayer_h");
        public Object getCustomWorldPath(){return custom;}public void setCustomWorldPath(Object path){custom=(XPath)path;}public Object getAutoRootContainerPath(){return root;}}
    public static final class DimHelper {public String getDimensionDirectoryName(Object key){return "dim%"+((TardisMapCompatTest.Key)key).name;}}
    public static final class Support {public boolean worldmap(){return true;}}
    public static final class Mod {public Support getSupportMods(){return new Support();}}
    public static final class Updater {public Object modMain=new Mod();public String getPotentialWorldNode(Object key,boolean worldMap){return worldMap?"mw$default":"null";}}
    public static final class DimType {final boolean ceiling;DimType(boolean ceiling){this.ceiling=ceiling;}public boolean comp_643(){return ceiling;}public int comp_653(){return 128;}}
    public static final class Container {Object type;public Object getDimensionType(Object key){return type;}}
    public static final class Manager {final Container container=new Container();public Object getAutoRootContainer(){return container;}
        final Set<XPath> worlds=new HashSet<>();final List<XPath> added=new ArrayList<>();
        public Object getWorld(Object path){return worlds.contains(path)?path:null;}public Object addWorld(Object path){worlds.add((XPath)path);added.add((XPath)path);return path;}}
    public static final class Session {final State state=new State();final Manager manager=new Manager();
        public State getWorldState(){return state;}public DimHelper getDimensionHelper(){return new DimHelper();}public Updater getWorldStateUpdater(){return new Updater();}public Manager getWorldManager(){return manager;}}
    public static void main(String[] args)throws Exception {
        Features.reset(Files.createTempDirectory("fan4location").resolve("fan4compat.properties"));
        TardisMapCompatTest.World world=new TardisMapCompatTest.World();net.minecraft.class_310.INSTANCE.field_1687=world;
        TardisMapCompatTest.Player player=new TardisMapCompatTest.Player();net.minecraft.class_310.INSTANCE.field_1724=player;
        BaseTardisConsoleUnitBlockEntity console=new BaseTardisConsoleUnitBlockEntity();
        TardisMapCompatTest.Key overworld=new TardisMapCompatTest.Key("minecraft:overworld");console.tardisStateManager.dimension=overworld;console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,-340);
        TardisMapCompatTest.Chunk chunk=new TardisMapCompatTest.Chunk();chunk.entities.put("console",console);world.chunks.loaded.put(List.of(2,-5),chunk);
        TardisMapCompatTest.Processor processor=new TardisMapCompatTest.Processor();xaero.map.WorldMapSession mapSession=new xaero.map.WorldMapSession();mapSession.processor=processor;xaero.map.WorldMapSession.current=mapSession;
        Session session=new Session();Object stranger=new Object(),originalDimension=new Object();

        // Outside a TARDIS everything is unchanged.
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;TardisLocation.refresh();
        check(TardisLocation.x(player,5.5)==5.5&&TardisLocation.y(player,6)==6&&TardisLocation.z(player,7.5)==7.5,"positions unchanged outside a TARDIS");
        check(TardisLocation.entityX(player)==null&&TardisLocation.dimension(world,originalDimension)==originalDimension,"no override outside a TARDIS");
        TardisLocation.frame(session);
        check(session.state.custom==null&&processor.world.custom==null&&session.manager.added.isEmpty(),"frame leaves the waypoint world and map alone");

        // Inside a landed TARDIS the player is at the exterior.
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;TardisLocation.refresh();
        check(TardisLocation.x(player,5.5)==1200.5&&TardisLocation.y(player,6)==70.0&&TardisLocation.z(player,7.5)==-339.5,"the player reads as the exterior");
        check(TardisLocation.x(stranger,5.5)==5.5&&TardisLocation.entityX(stranger)==null,"other entities are untouched");
        check(TardisLocation.entityX(player)==1200.5&&TardisLocation.entityY(player)==70.0&&TardisLocation.entityZ(player)==-339.5,"interpolating helpers return the exterior too");
        check(TardisLocation.dimension(world,originalDimension)==overworld&&TardisLocation.dimension(new Object(),originalDimension)==originalDimension,"the client world's dimension reads as the exterior's, other worlds' do not");
        TardisLocation.frame(session);
        XPath expected=new XPath("Multiplayer_h/dim%minecraft:overworld/mw$default");
        check(expected.equals(session.state.custom)&&session.manager.added.equals(List.of(expected)),"waypoint world switched to the exterior's, created if it did not exist");
        check(processor.world.custom==overworld&&processor.updates==1,"World Map viewing the exterior while inside");
        TardisLocation.frame(session);
        check(session.manager.added.size()==1&&processor.updates==1,"a steady state does no repeated work");

        // Cave start: under a ceiling the exterior's own height, otherwise the plain top view; never Xaero's scan of the interior.
        session.manager.container.type=new DimType(false);
        check(TardisLocation.caving()==Integer.MAX_VALUE,"an open-sky exterior keeps the top view");
        session.manager.container.type=new DimType(true);
        check(TardisLocation.caving()==74,"a ceiling exterior shows the layer at the TARDIS's height as Xaero finds it in open air (y 70 + 4)");
        session.manager.container.type=null;
        check(TardisLocation.caving()==Integer.MAX_VALUE,"unknown type, not the Nether: top view");
        console.tardisStateManager.dimension=new TardisMapCompatTest.Key("minecraft:the_nether");TardisLocation.refresh();
        check(TardisLocation.caving()==74,"unknown type but the Nether: still a ceiling dimension");
        console.tardisStateManager.dimension=overworld;TardisLocation.refresh();
        // On top of the ceiling there is nothing overhead: the top view, where that terrain was mapped.
        session.manager.container.type=new DimType(true);
        console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,127,-340);TardisLocation.refresh();
        check(TardisLocation.caving()==131,"just under the ceiling (y 127): the layer at that height");
        console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,128,-340);TardisLocation.refresh();
        check(TardisLocation.caving()==Integer.MAX_VALUE,"on the roof (y 128): the top view");
        console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,150,-340);TardisLocation.refresh();
        check(TardisLocation.caving()==Integer.MAX_VALUE,"above the roof: the top view");
        session.manager.container.type=null;console.tardisStateManager.dimension=new TardisMapCompatTest.Key("minecraft:the_nether");TardisLocation.refresh();
        check(TardisLocation.caving()==Integer.MAX_VALUE,"unknown type, the Nether, on the roof: the top view");
        console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,70,-340);TardisLocation.refresh();
        check(TardisLocation.caving()==74,"unknown type, the Nether, under the roof: the layer");
        console.tardisStateManager.dimension=overworld;TardisLocation.refresh();
        console.tardisStateManager.position=new TardisMapCompatTest.Pos(1200,-340);TardisLocation.refresh();

        // Landing state, switch and leaving all undo exactly what we did.
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();
        TardisLocation.frame(session);
        check(session.state.custom==null&&processor.world.custom==null&&TardisLocation.x(player,5.5)==5.5&&TardisLocation.caving()==null,"in flight everything returns to normal, Xaero works out its own cave start");
        // The diagnostics switch only adds log lines: nothing else changes with it on.
        Features.set("xaeroDiagnostics",true);console.tardisStateManager.materialization.landed=true;TardisLocation.refresh();
        check(TardisLocation.caving()!=null&&TardisLocation.x(player,5.5)==1200.5,"with the Xaero diagnostics on, behavior is unchanged");
        Features.set("xaeroDiagnostics",false);
        // Compass: the exterior's orientation while landed; the radar is empty anywhere inside a TARDIS.
        console.tardisStateManager.materialization.landed=true;TardisLocation.refresh();
        check(TardisLocation.cameraYaw(null,30f)==30f&&TardisLocation.yaw(player,30f)==30f,"no door or facing known: yaw unchanged");
        console.tardisStateManager.doors.put(new TardisMapCompatTest.Pos(1,2),null);console.tardisStateManager.doors.put(new TardisMapCompatTest.Pos(5,6),null);
        world.doorState.direction=new TardisMapCompatTest.Dir(180);console.tardisStateManager.facing=new TardisMapCompatTest.Dir(0);TardisLocation.refresh();
        check(TardisLocation.cameraYaw(null,30f)==30f,"exterior south, interior door facing north (walking out is south): the same compass");
        console.tardisStateManager.facing=new TardisMapCompatTest.Dir(270);TardisLocation.refresh();
        check(TardisLocation.cameraYaw(null,30f)==-60f&&TardisLocation.yaw(player,30f)==-60f&&TardisLocation.yaw(stranger,30f)==30f,"exterior east: leaving south (0) comes out facing east (270), so the interior yaw is turned by -90; other entities untouched");
        world.doorState.direction=new TardisMapCompatTest.Dir(90);console.tardisStateManager.facing=new TardisMapCompatTest.Dir(0);TardisLocation.refresh();
        check(TardisLocation.cameraYaw(null,0f)==90f,"door facing west (walking out is east, 270): coming out facing south (0) is a quarter turn on (+90)");
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();
        check(TardisLocation.cameraYaw(null,30f)==30f&&TardisLocation.yaw(player,30f)==30f,"in flight there is no outside: the interior's compass and arrow");
        List<Object> entities=List.of("zombie");
        check(TardisLocation.entities(world,entities)!=entities&&((Iterable<?>)TardisLocation.entities(world,entities)).iterator().hasNext()==false,"radar: no entities while inside a TARDIS, landed or not");
        check(TardisLocation.entities(new Object(),entities)==entities,"another world's entities untouched");
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;TardisLocation.refresh();
        check(TardisLocation.entities(world,entities)==entities&&TardisLocation.cameraYaw(null,30f)==30f,"outside a TARDIS: radar and compass are Xaero's own");
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;console.tardisStateManager.doors.clear();console.tardisStateManager.facing=null;
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();

        // The coordinate display: the exterior while landed, and a point between departure and destination while travelling.
        console.tardisStateManager.materialization.landed=true;TardisLocation.refresh();
        check(TardisLocation.displayX(player,5.5)==1200.5&&TardisLocation.displayY(player,6)==70.0&&TardisLocation.displayZ(player,7.5)==-339.5&&TardisLocation.displayX(stranger,5.5)==5.5,"landed: the display shows the exterior, other entities untouched");
        console.tardisStateManager.destination=new TardisMapCompatTest.Pos(1400,-140);
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();
        console.tardisStateManager.flight.set("NONE",false,0);
        check(TardisLocation.displayX(player,5.5)==1200.5&&TardisLocation.displayZ(player,7.5)==-339.5,"not landed, no flight: still at the departure point");
        console.tardisStateManager.flight.set("WAIT_FOR_DEMAT",false,100);
        check(TardisLocation.displayX(player,5.5)==1200.5,"taking off: still at the departure, whatever the progress says");
        console.tardisStateManager.flight.set("PROCESSING",true,0);
        check(TardisLocation.displayX(player,5.5)==1200.5&&TardisLocation.displayZ(player,7.5)==-339.5,"flight just begun: departure");
        console.tardisStateManager.flight.set("PROCESSING",true,50);
        check(TardisLocation.displayX(player,5.5)==1300.5&&TardisLocation.displayZ(player,7.5)==-239.5&&TardisLocation.displayY(player,6)==70.0,"half way: the midpoint, updated live without waiting for a lookup");
        console.tardisStateManager.flight.set("PROCESSING",true,100);
        check(TardisLocation.displayX(player,5.5)==1400.5&&TardisLocation.displayZ(player,7.5)==-139.5,"flight complete: the destination");
        console.tardisStateManager.flight.set("PROCESSING",true,250);
        check(TardisLocation.displayX(player,5.5)==1400.5,"progress is clamped to the destination");
        console.tardisStateManager.flight.set("PROCESSING",false,100);
        check(TardisLocation.displayX(player,5.5)==1400.5,"landing: the destination");
        check(TardisLocation.x(player,5.5)==5.5&&TardisLocation.caving()==null,"the map and waypoints still treat a travelling TARDIS as having no outside");
        console.tardisStateManager.flight.set("NONE",false,0);
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;TardisLocation.refresh();
        check(TardisLocation.displayX(player,5.5)==5.5,"outside a TARDIS the display is the player's own");
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();
        console.tardisStateManager.materialization.landed=true;TardisLocation.refresh();TardisLocation.frame(session);
        check(expected.equals(session.state.custom),"landing again restores the exterior");
        Features.set("xaeroTardisMinimap",false);TardisLocation.refresh();TardisLocation.frame(session);
        check(session.state.custom==null&&TardisLocation.x(player,5.5)==5.5,"switch off: inactive and undone");
        Features.set("xaeroTardisMinimap",true);TardisLocation.refresh();TardisLocation.frame(session);
        check(expected.equals(session.state.custom)&&processor.world.custom==overworld,"back on, exterior applied again");
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;TardisLocation.refresh();TardisLocation.frame(session);
        check(session.state.custom==null&&processor.world.custom==null,"leaving the TARDIS puts the waypoint world and the map back");
        XPath theirs=new XPath("Multiplayer_h/dim%minecraft:the_nether/mw$default");session.state.custom=theirs;processor.world.custom=new TardisMapCompatTest.Key("minecraft:the_end");
        TardisLocation.frame(session);
        check(theirs.equals(session.state.custom)&&processor.world.custom!=null,"a waypoint world and map dimension the player chose themselves are never cleared");

        // A failure inside Xaero never reaches the game.
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;TardisLocation.refresh();
        TardisLocation.frame(new Object());
        check(TardisLocation.x(player,1.5)==1200.5,"a broken session object is ignored");
        net.minecraft.class_310.INSTANCE.field_1724=null;TardisLocation.refresh();
        check(TardisLocation.x(player,1.5)==1.5,"no player: unchanged");
        Features.reset(null);
        System.out.println("PASS: TARDIS location: exterior position for the player only, dimension and waypoint world switched and undone, own choices kept, failures contained");
    }
}
