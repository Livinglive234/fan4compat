import dev.fan4.compat.chesttracker.*;
import java.lang.reflect.Method;
import java.util.Set;
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
        int none=(Integer)register.invoke(null,loader,(java.util.function.Predicate<String>)id->false);
        check(none==0,"with none of the mods installed nothing is added");
        // The list names only mods whose screens cannot hold items in the world.
        check(ScreenList.SCREENS.keySet().containsAll(Set.of("etched","create","waystones","aether","accessories"))&&ScreenList.SCREENS.values().stream().allMatch(l->!l.isEmpty()),"the list covers the pack's mods");
        check(ScreenList.SCREENS.get("create").stream().noneMatch(n->n.contains("Toolbox")||n.contains("PackagePort")||n.contains("Schematic")),"Create's real storage screens (toolbox, package port, schematic blocks) keep the button");
        System.out.println("PASS: Chest Tracker screen blacklist (only installed mods, missing classes skipped, item-holding screens left out)");
    }
}
