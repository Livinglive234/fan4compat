package dev.emi.trinkets.api;
import net.minecraft.class_1799;
import java.util.*;
public class TrinketInventory {
    public final SlotType type;public final List<class_1799> stacks=new ArrayList<>();
    public TrinketInventory(SlotType type,int size){this.type=type;for(int i=0;i<size;i++)stacks.add(class_1799.field_8037);}
    public SlotType getSlotType(){return type;}
    public class_1799 method_5438(int index){return stacks.get(index);}
    public void method_5447(int index,class_1799 stack){stacks.set(index,stack);}
}
