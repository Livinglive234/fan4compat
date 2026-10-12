import dev.fan4.compat.shared.Features;
import java.nio.file.*;
import java.util.*;
import net.drgmes.dwm.blocks.tardis.consoleunits.BaseTardisConsoleUnitBlockEntity;

public final class TardisMapCompatTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static final class Key {final String name;Key(String name){this.name=name;}public Object method_29177(){return name;}}
    public static final class Pos {final int x,y,z;Pos(int x,int z){this.x=x;this.y=70;this.z=z;}public int method_10263(){return x;}public int method_10264(){return y;}public int method_10260(){return z;}}
    public static final class Player {public double method_23317(){return 40.5;}public double method_23321(){return -70.2;}}
    public static final class Chunk {final Map<Object,Object> entities=new HashMap<>();public Map<Object,Object> method_12214(){return entities;}}
    public static final class Chunks {
        final Map<List<Integer>,Chunk> loaded=new HashMap<>();
        public Chunk method_2857(int x,int z,Object status,boolean create){check(!create&&status==net.minecraft.class_2806.field_12803,"loaded chunks only, never created");return loaded.get(List.of(x,z));}
    }
    public static final class World {final Key key=new Key("dwm:tardis_a");final Chunks chunks=new Chunks();public Object method_27983(){return key;}public Chunks method_2935(){return chunks;}}
    public static final class MapWorld {Object custom;public void setCustomDimensionId(Object key){custom=key;}public Object getCustomDimensionId(){return custom;}}
    public static final class Processor {final MapWorld world=new MapWorld();int updates;public MapWorld getMapWorld(){return world;}public void checkForWorldUpdate(){updates++;}}
    public static final class Gui {private double cameraX,cameraZ;private boolean shouldResetCameraPos=true;final Processor processor=new Processor();public Processor getMapProcessor(){return processor;}}
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("fan4tardismap");Features.reset(dir.resolve("fan4compat.properties"));
        Features.set("xaeroTardisMinimap",false);
        World world=new World();net.minecraft.class_310.INSTANCE.field_1687=world;net.minecraft.class_310.INSTANCE.field_1724=new Player();
        BaseTardisConsoleUnitBlockEntity console=new BaseTardisConsoleUnitBlockEntity();
        Key overworld=new Key("minecraft:overworld");console.tardisStateManager.dimension=overworld;console.tardisStateManager.position=new Pos(1200,-340);
        // The player stands in chunk (2,-5); put the console two chunks away.
        Chunk chunk=new Chunk();chunk.entities.put("console",console);chunk.entities.put("other",new Object());world.chunks.loaded.put(List.of(4,-5),chunk);
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;

        Gui gui=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(gui);
        check(gui.processor.world.custom==overworld&&gui.processor.updates==1,"landed: the map is switched to the exterior's dimension through Xaero's own update");
        check(field(gui,"cameraX").equals(1200.5)&&field(gui,"cameraZ").equals(-339.5)&&field(gui,"shouldResetCameraPos").equals(false),"camera on the exterior, no reset to the player");

        console.tardisStateManager.materialization.landed=false;
        Gui flying=new Gui();flying.processor.world.custom=overworld;dev.fan4.compat.xaero.TardisMapCompat.opened(flying);
        check(flying.processor.world.custom==null&&flying.processor.updates==1&&field(flying,"shouldResetCameraPos").equals(true),"in flight: our earlier switch is undone and the camera is left alone");
        console.tardisStateManager.materialization.landed=true;

        Gui theirs=new Gui();Key chosen=new Key("minecraft:the_end");theirs.processor.world.custom=chosen;
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;
        dev.fan4.compat.xaero.TardisMapCompat.opened(theirs);
        check(theirs.processor.world.custom==chosen&&theirs.processor.updates==0,"outside a TARDIS the player's own dimension choice is never touched");
        Gui leaving=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(leaving);
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;
        dev.fan4.compat.xaero.TardisMapCompat.opened(new Gui());
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=false;
        Gui afterExit=new Gui();afterExit.processor.world.custom=overworld;dev.fan4.compat.xaero.TardisMapCompat.opened(afterExit);
        check(afterExit.processor.world.custom==null,"leaving the TARDIS clears the exterior view we set");
        net.drgmes.dwm.utils.helpers.TardisHelper.tardis=true;

        world.chunks.loaded.clear();world.chunks.loaded.put(List.of(2+13,-5),chunk);
        Gui far=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(far);
        check(far.processor.world.custom==null,"a console beyond the search radius is not found; the map behaves as normal");
        world.chunks.loaded.clear();world.chunks.loaded.put(List.of(2+12,-5),chunk);
        Gui edge=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(edge);
        check(edge.processor.world.custom==overworld,"a console at the edge of the radius is found");

        // Closing the screen undoes our switch, but only ours.
        Gui open=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(open);
        check(open.processor.world.custom==overworld,"map open on the exterior");
        int updates=open.processor.updates;dev.fan4.compat.xaero.TardisMapCompat.closed(open);
        check(open.processor.world.custom==null&&open.processor.updates==updates+1,"closing the screen restores the player's own dimension");
        dev.fan4.compat.xaero.TardisMapCompat.closed(open);
        check(open.processor.updates==updates+1,"closing again does nothing");
        Gui chosenGui=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(chosenGui);chosenGui.processor.world.custom=chosen;
        dev.fan4.compat.xaero.TardisMapCompat.closed(chosenGui);
        check(chosenGui.processor.world.custom==chosen,"a dimension the player picked afterwards is left alone on close");

        // With the minimap mode on, it owns the view: closing the map screen leaves it in place.
        Features.set("xaeroTardisMinimap",true);dev.fan4.compat.xaero.TardisLocation.refresh();
        Gui owned=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(owned);dev.fan4.compat.xaero.TardisMapCompat.closed(owned);
        check(owned.processor.world.custom==overworld,"minimap mode keeps the exterior view after the map screen closes");
        Features.set("xaeroTardisMinimap",false);dev.fan4.compat.xaero.TardisLocation.refresh();

        Features.set("xaeroTardisMap",false);
        Gui off=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(off);
        check(off.processor.world.custom==null&&off.processor.updates==0,"switch off: nothing happens");
        Features.set("xaeroTardisMap",true);
        net.minecraft.class_310.INSTANCE.field_1724=null;
        Gui noPlayer=new Gui();dev.fan4.compat.xaero.TardisMapCompat.opened(noPlayer);
        check(noPlayer.processor.world.custom==null,"no player yet: nothing happens, no error");
        Features.reset(null);
        System.out.println("PASS: TARDIS exterior on the Xaero map: landed switch and camera, close/flight/exit cleanup, own choices respected, search radius and switch");
    }
    static Object field(Object target,String name)throws Exception {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
}
