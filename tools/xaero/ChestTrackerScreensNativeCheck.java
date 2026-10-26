import dev.fan4.compat.chesttracker.*;
import java.util.*;

/** Chest Tracker's blacklist method and every screen class named in {@link ScreenList#SCREENS} must exist in the real jars (args: the jars). */
public final class ChestTrackerScreensNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        XaeroSyncNativeCheck.method("red/jackf/chesttracker/api/gui/ScreenBlacklist","add","([Ljava/lang/Class;)V",true);
        int checked=0;List<String> missing=new ArrayList<>();
        for(var mod:ScreenList.SCREENS.entrySet())
            for(String name:mod.getValue()) {
                try{XaeroSyncNativeCheck.find(name.replace('.','/'));checked++;}
                catch(AssertionError absent){missing.add(mod.getKey()+": "+name);}
            }
        if(!missing.isEmpty())throw new AssertionError("Listed screens missing from the real jars: "+missing);
        System.out.println("PASS: Chest Tracker's blacklist method and all "+checked+" listed screen classes exist in the real jars");
    }
}
