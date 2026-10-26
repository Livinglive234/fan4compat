package net.fabricmc.fabric.api.event;

import java.util.*;
import net.minecraft.class_2960;

/** Stand-in for Fabric's Event: records phase orderings and listeners. */
public class Event {
    public static final class_2960 DEFAULT_PHASE=class_2960.method_60655("fabric","default");
    public final List<class_2960[]> orderings=new ArrayList<>();
    public final Map<class_2960,Object> listeners=new LinkedHashMap<>();
    public void addPhaseOrdering(class_2960 first,class_2960 second){orderings.add(new class_2960[]{first,second});}
    public void register(class_2960 phase,Object listener){listeners.put(phase,listener);}
}
