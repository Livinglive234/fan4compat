package dev.emi.trinkets;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
public interface TrinketSlot {
    boolean[] ALLOW={true};
    static boolean canInsert(class_1799 stack,SlotReference reference,class_1309 entity){return ALLOW[0];}
}
