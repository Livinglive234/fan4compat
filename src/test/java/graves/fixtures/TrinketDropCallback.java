package dev.emi.trinkets.api.event;
import dev.emi.trinkets.api.*;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
public interface TrinketDropCallback {
    TrinketDropEvent EVENT=new TrinketDropEvent();
    TrinketEnums.DropRule drop(TrinketEnums.DropRule rule,class_1799 stack,SlotReference slot,class_1309 entity);
}
