package dev.emi.trinkets.api;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
public interface Trinket {
    default TrinketEnums.DropRule getDropRule(class_1799 stack,SlotReference slot,class_1309 entity){return TrinketEnums.DropRule.DEFAULT;}
}
