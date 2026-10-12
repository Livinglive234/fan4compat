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
    public static final class Manager {final Set<XPath> worlds=new HashSet<>();final List<XPath> added=new ArrayList<>();
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

        // Landing state, switch and leaving all undo exactly what we did.
        console.tardisStateManager.materialization.landed=false;TardisLocation.refresh();
        TardisLocation.frame(session);
        check(session.state.custom==null&&processor.world.custom==null&&TardisLocation.x(player,5.5)==5.5,"in flight everything returns to normal");
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
