package dev.emi.trinkets.api;
import net.minecraft.class_1799;
import java.util.*;
import java.util.function.BiConsumer;
public interface TrinketComponent {
    List<TrinketInventory> inventories();
    default void forEach(BiConsumer<SlotReference,class_1799> consumer) {
        for(TrinketInventory inventory:inventories())
            for(int i=0;i<inventory.stacks.size();i++)consumer.accept(new SlotReference(inventory,i),inventory.stacks.get(i));
    }
}
