package dev.fan4.compat.chesttracker;

import dev.fan4.compat.shared.Features;
import dev.fan4.compat.shared.GameLog;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import static dev.fan4.compat.shared.CompatCalls.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Chest Tracker adds its inventory button to every container screen. Its blacklist only turns off remembering what a screen holds (and the remember and
 * rename buttons that go with it); the main button is added regardless. So for the pack's screens that cannot hold items (stations, settings and selection
 * screens, ghost-slot filters, screens for items held in the hand) this does two things: adds them to the blacklist, and removes the button right after Chest Tracker
 * has added it, in a later phase of the same screen-init event. Screens that really hold items keep the button. Client only.
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
            GameLog.warn("Could not stop Chest Tracker's button on screens that cannot hold items; it stays on them",failure);
        }
    }
    /** Resolves the listed screen classes of every installed mod, blacklists them and hides Chest Tracker's button on them; returns how many classes were found. A class that is not there (another version of the mod) is skipped. */
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
        if(!classes.isEmpty())hideButton(List.copyOf(classes));
        return classes.size();
    }
    private static final String EVENTS="net.fabricmc.fabric.api.client.screen.v1.ScreenEvents",BUTTON="red.jackf.chesttracker.impl.gui.invbutton.ui.InventoryButton",
        DUCK="red.jackf.chesttracker.impl.gui.invbutton.CTButtonScreenDuck";
    /** After every screen init, once Chest Tracker has added its button (it does so in the default phase), take it off the screens in the list. */
    private static void hideButton(List<Class<?>> screens) {
        Object event=field(type(EVENTS),"AFTER_INIT"),phase=call(type("net.minecraft.class_2960"),"method_60655","fan4compat","after_chesttracker_button");
        call(event,"addPhaseOrdering",field(type("net.fabricmc.fabric.api.event.Event"),"DEFAULT_PHASE"),phase);
        Object listener=java.lang.reflect.Proxy.newProxyInstance(ChestTrackerScreens.class.getClassLoader(),new Class<?>[]{type(EVENTS+"$AfterInit")},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->"Fan4Compat Chest Tracker button filter";};
            try{removeButton(args[1],screens);}catch(RuntimeException|LinkageError failure){GameLog.debug("Chest Tracker button filter failed",failure);}
            return null;
        });
        call(event,"register",phase,listener);
    }
    private static void removeButton(Object screen,List<Class<?>> screens)throws RuntimeException {
        boolean listed=false;
        for(Class<?> listedScreen:screens)if(listedScreen.isInstance(screen)){listed=true;break;}
        if(!listed)return;
        List<?> widgets=(List<?>)call(type("net.fabricmc.fabric.api.client.screen.v1.Screens"),"getButtons",screen);
        widgets.removeIf(widget->widget.getClass().getName().equals(BUTTON));
        Class<?> duck=type(DUCK);
        // Chest Tracker keeps its own reference for dragging the button; without the button there is nothing to drag.
        if(duck.isInstance(screen))try{duck.getMethod("chesttracker$setButton",type(BUTTON)).invoke(screen,(Object)null);}catch(ReflectiveOperationException ignored){}
    }
}
