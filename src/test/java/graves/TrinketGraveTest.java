import dev.emi.trinkets.api.*;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import dev.fan4.compat.graves.AccessoryGraveCompat;
import io.wispforest.accessories.pond.DroppedStacksExtension;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import java.util.*;

/** Trinkets items must reach the grave even though Graves cancels Player.dropInventory. */
public final class TrinketGraveTest {
    public static class Inventory {public Wearer field_7546;}
    public static class Wearer extends class_1309 implements DroppedStacksExtension, TrinketComponent {
        public final Inventory inventory=new Inventory();public final List<TrinketInventory> slots=new ArrayList<>();public int dropped;
        public Wearer(){inventory.field_7546=this;TrinketsApi.COMPONENTS.put(this,this);}
        public Collection<Object> toBeDroppedStacks(){return new ArrayList<>();}
        public void addToBeDroppedStacks(Collection<Object> stacks){}
        public Object method_5775(class_1799 stack){dropped+=stack.count;return new Object();}
        public List<TrinketInventory> inventories(){return slots;}
    }
    static class_1799 item(String name){class_1799 stack=new class_1799(1);stack.item=name;return stack;}
    static TrinketInventory slot(Wearer wearer,DropRule rule,class_1799... stacks) {
        TrinketInventory inventory=new TrinketInventory(new SlotType(rule),stacks.length);
        for(int i=0;i<stacks.length;i++)inventory.stacks.set(i,stacks[i]);
        wearer.slots.add(inventory);return inventory;
    }
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static void reset() {
        TrinketsApi.TRINKETS.clear();
        TrinketDropCallback.EVENT.handler=(rule,stack,slot,entity)->rule;
    }
    static Trinket rule(DropRule rule){return new Trinket(){public DropRule getDropRule(class_1799 s,SlotReference r,class_1309 e){return rule;}};}
    public static void main(String[] args) {
        reset();
        // Slot-type rule DEFAULT, keepInventory off: the elytra case. It must move into the grave and leave the slot.
        Wearer player=new Wearer();class_1799 elytra=item("crystallite_elytra");
        TrinketInventory cape=slot(player,DropRule.DEFAULT,elytra);
        check(!AccessoryGraveCompat.inventoryEmpty(true,player.inventory),"trinket-only death is eligible for a grave");
        var failed=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(failed,player);AccessoryGraveCompat.spawned(false,failed);
        check(cape.stacks.get(0)==elytra&&player.dropped==0,"failed spawn leaves trinkets for Trinkets' normal drop");
        var grave=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(grave,player);
        check(grave.count()==1&&cape.stacks.get(0)==elytra,"staged capture copies without removing the equipped stack");
        elytra.count=7;check(grave.count()==1,"grave holds an independent copy");elytra.count=1;
        AccessoryGraveCompat.spawned(true,grave);
        check(cape.stacks.get(0).method_7960()&&grave.count()==1,"successful spawn empties the trinket slot");
        AccessoryGraveCompat.spawned(true,grave);check(grave.count()==1,"repeat spawn cannot duplicate");

        // keepInventory on: Graves ignored it, so default-KEEP trinkets are graved too; explicit KEEP stays equipped.
        reset();Wearer kept=new Wearer();kept.world.rules.keepInventory=true;
        class_1799 cape2=item("cape"),soulbound=item("soulbound"),slotKeep=item("slotkeep");
        TrinketsApi.TRINKETS.put("soulbound",rule(DropRule.KEEP));
        slot(kept,DropRule.DEFAULT,cape2,soulbound);TrinketInventory keepSlot=slot(kept,DropRule.KEEP,slotKeep);
        var keptGrave=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(keptGrave,kept);AccessoryGraveCompat.spawned(true,keptGrave);
        check(keptGrave.count()==1&&kept.slots.get(0).stacks.get(0).method_7960(),"keepInventory default goes to the grave");
        check(kept.slots.get(0).stacks.get(1)==soulbound&&keepSlot.stacks.get(0)==slotKeep,"explicit item/slot KEEP stays equipped and is not graved");

        // DESTROY: vanishing curse and explicit rules are honored, never graved, and removed on success.
        reset();Wearer cursed=new Wearer();class_1799 vanishing=item("vanishing");vanishing.vanishing=true;
        class_1799 destroyed=item("destroyed");TrinketsApi.TRINKETS.put("destroyed",rule(DropRule.DESTROY));
        slot(cursed,DropRule.DEFAULT,vanishing,destroyed);
        check(AccessoryGraveCompat.inventoryEmpty(true,cursed.inventory),"destroy-only trinkets never create a grave");
        var burned=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(burned,cursed);AccessoryGraveCompat.spawned(true,burned);
        check(burned.count()==0&&cursed.slots.get(0).stacks.get(0).method_7960()&&cursed.slots.get(0).stacks.get(1).method_7960(),"destroyed trinkets vanish like native Trinkets");

        // The drop event and a DROP item rule both resolve before the slot type rule.
        reset();Wearer events=new Wearer();class_1799 forced=item("forced"),dropped=item("dropped");
        TrinketsApi.TRINKETS.put("dropped",rule(DropRule.DROP));
        TrinketDropCallback.EVENT.handler=(rule,stack,slot,entity)->stack.item.equals("forced")?DropRule.KEEP:rule;
        slot(events,DropRule.DEFAULT,forced,dropped);
        var eventGrave=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(eventGrave,events);AccessoryGraveCompat.spawned(true,eventGrave);
        check(eventGrave.count()==1&&events.slots.get(0).stacks.get(0)==forced,"drop event KEEP overrides default; DROP item rule is graved");
        reset();

        // Slot-type DROP with keepInventory on: native rule is DROP, so it is graved.
        Wearer typed=new Wearer();typed.world.rules.keepInventory=true;class_1799 charm=item("charm");slot(typed,DropRule.DROP,charm);
        var typedGrave=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(typedGrave,typed);AccessoryGraveCompat.spawned(true,typedGrave);
        check(typedGrave.count()==1,"slot-type DROP is graved");

        // Capacity: stacks beyond the 128-slot grave drop normally once, never duplicated.
        Wearer many=new Wearer();class_1799[] lots=new class_1799[100];for(int i=0;i<100;i++)lots[i]=item("t"+i);
        TrinketInventory all=slot(many,DropRule.DEFAULT,lots);
        var limit=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(limit,many);AccessoryGraveCompat.spawned(true,limit);
        check(limit.count()+many.dropped==100,"bounded storage overflow drops once");
        check(all.stacks.stream().allMatch(class_1799::method_7960),"all captured slots emptied exactly once");

        // No trinkets and non-living players behave exactly as before.
        Wearer bare=new Wearer();check(AccessoryGraveCompat.inventoryEmpty(true,bare.inventory),"no trinkets leaves empty-inventory logic unchanged");
        var plain=new AccessoryGraveTest.Player();
        check(AccessoryGraveCompat.inventoryEmpty(true,plain.inventory),"players without a trinket component are unaffected");
        System.out.println("PASS: Trinkets items graved on empty inventory, staged copy and spawn failure, slot clearing, keepInventory/explicit KEEP/DESTROY/event rules and bounded overflow (fixtures)");
    }
}
