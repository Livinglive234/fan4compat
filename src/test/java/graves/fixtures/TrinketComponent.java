package dev.emi.trinkets.api;
import net.minecraft.class_1799;
import java.util.*;
import java.util.function.BiConsumer;
public interface TrinketComponent {
    List<TrinketInventory> inventories();
    default Map<String,Map<String,TrinketInventory>> getInventory() {
        Map<String,Map<String,TrinketInventory>> groups=new HashMap<>();
        for(TrinketInventory inventory:inventories()) {
            String[] id=inventory.type.getId().split("/",2);
            groups.computeIfAbsent(id[0],g->new HashMap<>()).put(id[1],inventory);
        }
        return groups;
    }
    default void forEach(BiConsumer<SlotReference,class_1799> consumer) {
        for(TrinketInventory inventory:inventories())
            for(int i=0;i<inventory.stacks.size();i++)consumer.accept(new SlotReference(inventory,i),inventory.stacks.get(i));
    }
}
