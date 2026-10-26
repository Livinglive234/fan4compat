package red.jackf.chesttracker.api.gui;

import java.util.*;

/** Stand-in for Chest Tracker's blacklist: same name and signature, so the real reflective call is exercised. */
public class ScreenBlacklist {
    public static final Set<Class<?>> BLACKLIST=new LinkedHashSet<>();
    public static void add(Class<?>... classes){BLACKLIST.addAll(Arrays.asList(classes));}
}
