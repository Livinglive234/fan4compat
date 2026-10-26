import dev.fan4.compat.chesttracker.*;
import java.util.*;

/** Chest Tracker's blacklist method and every screen class named in {@link ScreenList#SCREENS} must exist in the real jars (args: the jars, including Fabric's screen API and API base jars and an intermediary Minecraft jar). */
public final class ChestTrackerScreensNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        XaeroSyncNativeCheck.method("red/jackf/chesttracker/api/gui/ScreenBlacklist","add","([Ljava/lang/Class;)V",true);
        // The button filter: Fabric's screen init event, its phase ordering, the button list, and Chest Tracker's button and its reference on the screen.
        String events="net/fabricmc/fabric/api/client/screen/v1/ScreenEvents",event="net/fabricmc/fabric/api/event/Event",id="Lnet/minecraft/class_2960;";
        XaeroSyncNativeCheck.field(events,"AFTER_INIT",true);
        XaeroSyncNativeCheck.method(events+"$AfterInit","afterInit","(Lnet/minecraft/class_310;Lnet/minecraft/class_437;II)V",false);
        XaeroSyncNativeCheck.method(event,"addPhaseOrdering","("+id+id+")V",false);
        XaeroSyncNativeCheck.method(event,"register","("+id+"Ljava/lang/Object;)V",false);
        XaeroSyncNativeCheck.field(event,"DEFAULT_PHASE",true);
        XaeroSyncNativeCheck.method("net/fabricmc/fabric/api/client/screen/v1/Screens","getButtons","(Lnet/minecraft/class_437;)Ljava/util/List;",true);
        XaeroSyncNativeCheck.method("net/minecraft/class_2960","method_60655","(Ljava/lang/String;Ljava/lang/String;)",true);
        XaeroSyncNativeCheck.find("red/jackf/chesttracker/impl/gui/invbutton/ui/InventoryButton");
        XaeroSyncNativeCheck.method("red/jackf/chesttracker/impl/gui/invbutton/CTButtonScreenDuck","chesttracker$setButton","(Lred/jackf/chesttracker/impl/gui/invbutton/ui/InventoryButton;)V",false);
        int checked=0;List<String> missing=new ArrayList<>();
        for(var mod:ScreenList.SCREENS.entrySet())
            for(String name:mod.getValue()) {
                try{XaeroSyncNativeCheck.find(name.replace('.','/'));checked++;}
                catch(AssertionError absent){missing.add(mod.getKey()+": "+name);}
            }
        if(!missing.isEmpty())throw new AssertionError("Listed screens missing from the real jars: "+missing);
        System.out.println("PASS: Chest Tracker's blacklist method, its button class and screen reference, Fabric's screen init event and button list, and all "+checked+" listed screen classes exist in the real jars");
    }
}
