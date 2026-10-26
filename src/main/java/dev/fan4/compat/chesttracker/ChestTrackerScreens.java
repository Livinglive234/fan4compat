package dev.fan4.compat.chesttracker;

import dev.fan4.compat.shared.Features;
import dev.fan4.compat.shared.GameLog;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Chest Tracker puts its inventory button on every container screen that is not on its blacklist, which by default holds only vanilla's stations
 * (crafting-style screens, enchanting, grindstone, anvil and smithing, loom, stonecutter, cartography, beacon, the player inventory). This adds the same
 * kind of screens from the pack's other mods: stations, settings and selection screens, ghost-slot filters and screens for items held in the hand.
 * Screens that really hold items (furnace-likes, chests, toolboxes, package ports, jukeboxes) keep the button. Client only.
 */
public final class ChestTrackerScreens implements ModInitializer {
    static final String BLACKLIST="red.jackf.chesttracker.api.gui.ScreenBlacklist";

    public void onInitialize() {
        FabricLoader loader=FabricLoader.getInstance();
        if(loader.getEnvironmentType()!=EnvType.CLIENT||!loader.isModLoaded("chesttracker")||!Features.enabled("chestTrackerScreens"))return;
        try {
            int added=register(ChestTrackerScreens.class.getClassLoader(),loader::isModLoaded);
            GameLog.info("Chest Tracker will skip "+added+" screens that cannot hold items");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError failure) {
            GameLog.warn("Could not add screens to Chest Tracker's blacklist; its button stays on them",failure);
        }
    }
    /** Adds every listed screen class of every installed mod to Chest Tracker's blacklist; returns how many were added. A class that is not there (another version of the mod) is skipped. */
    static int register(ClassLoader loader,Predicate<String> installed)throws ReflectiveOperationException {
        List<Class<?>> classes=new ArrayList<>();
        for(var mod:ScreenList.SCREENS.entrySet()) {
            if(!installed.test(mod.getKey()))continue;
            for(String name:mod.getValue())
                try{classes.add(Class.forName(name,false,loader));}
                catch(ClassNotFoundException|LinkageError missing){GameLog.debug("Chest Tracker: "+name+" is not in this version of "+mod.getKey());}
        }
        Method add=Class.forName(BLACKLIST,true,loader).getMethod("add",Class[].class);
        add.invoke(null,(Object)classes.toArray(new Class<?>[0]));
        return classes.size();
    }
}
