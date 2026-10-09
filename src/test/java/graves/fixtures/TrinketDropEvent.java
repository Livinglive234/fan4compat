package dev.emi.trinkets.api.event;
import dev.emi.trinkets.api.*;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
public class TrinketDropEvent {
    public TrinketDropCallback handler=(rule,stack,slot,entity)->rule;
    public TrinketDropCallback invoker(){return handler;}
}
