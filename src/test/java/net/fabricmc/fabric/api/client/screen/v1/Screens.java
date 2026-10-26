package net.fabricmc.fabric.api.client.screen.v1;

import java.util.*;

public final class Screens {
    public static final Map<Object,List<Object>> BUTTONS=new IdentityHashMap<>();
    public static List<Object> getButtons(Object screen){return BUTTONS.computeIfAbsent(screen,s->new ArrayList<>());}
}
