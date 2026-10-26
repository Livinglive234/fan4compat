import dev.fan4.compat.chesttracker.*;
import gg.moonflower.etched.client.screen.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.fabricmc.fabric.api.event.Event;
import red.jackf.chesttracker.impl.gui.invbutton.ui.InventoryButton;
import red.jackf.chesttracker.api.gui.ScreenBlacklist;

public final class ChestTrackerScreensTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Method register=ChestTrackerScreens.class.getDeclaredMethod("register",ClassLoader.class,java.util.function.Predicate.class);register.setAccessible(true);
        ClassLoader loader=ChestTrackerScreensTest.class.getClassLoader();
        // Only mods that are installed contribute; a listed class that this version of the mod does not have is skipped without failing.
        int added=(Integer)register.invoke(null,loader,(java.util.function.Predicate<String>)id->id.equals("etched")||id.equals("waystones"));
        Set<String> names=new java.util.TreeSet<>();for(Class<?> c:ScreenBlacklist.BLACKLIST)names.add(c.getSimpleName());
        check(added==3&&names.equals(Set.of("EtchingScreen","RadioScreen","WaystoneEditScreen")),"installed mods' listed screens are added, missing classes skipped: "+names);
        check(!names.contains("AlbumJukeboxScreen"),"a screen that holds items (the album jukebox) is not on the list");
        // Chest Tracker's main button is added to every container screen whatever its blacklist says, so it is removed from the listed ones once Chest Tracker has added it.
        Event event=ScreenEvents.AFTER_INIT;
        check(event.orderings.size()==1&&event.orderings.get(0)[0]==Event.DEFAULT_PHASE&&event.listeners.size()==1,"the filter runs in a phase after the default one, where Chest Tracker adds its button");
        ScreenEvents.AfterInit afterInit=(ScreenEvents.AfterInit)event.listeners.values().iterator().next();
        EtchingScreen listed=new EtchingScreen();AlbumJukeboxScreen kept=new AlbumJukeboxScreen();
        InventoryButton listedButton=new InventoryButton(),keptButton=new InventoryButton();Object other=new Object();
        listed.button=listedButton;kept.button=keptButton;
        Screens.getButtons(listed).addAll(List.of(other,listedButton));Screens.getButtons(kept).addAll(List.of(other,keptButton));
        afterInit.afterInit(null,listed,0,0);afterInit.afterInit(null,kept,0,0);
        check(Screens.getButtons(listed).equals(List.of(other))&&listed.button==null,"the button is taken off a listed screen and nothing else is, including Chest Tracker's reference to it");
        check(Screens.getButtons(kept).equals(List.of(other,keptButton))&&kept.button==keptButton,"a screen that holds items keeps its button");
        afterInit.afterInit(null,listed,0,0);
        check(Screens.getButtons(listed).equals(List.of(other)),"running again (a window resize re-inits the screen) is harmless");
        int none=(Integer)register.invoke(null,loader,(java.util.function.Predicate<String>)id->false);
        check(none==0,"with none of the mods installed nothing is added");
        // The list names only mods whose screens cannot hold items in the world.
        check(ScreenList.SCREENS.keySet().containsAll(Set.of("etched","create","waystones","aether","accessories"))&&ScreenList.SCREENS.values().stream().allMatch(l->!l.isEmpty()),"the list covers the pack's mods");
        check(ScreenList.SCREENS.get("create").stream().noneMatch(n->n.contains("Toolbox")||n.contains("PackagePort")||n.contains("Schematic")),"Create's real storage screens (toolbox, package port, schematic blocks) keep the button");
        System.out.println("PASS: Chest Tracker screen blacklist and button removal (only installed mods, missing classes skipped, item-holding screens left out, button removed after Chest Tracker adds it)");
    }
}
