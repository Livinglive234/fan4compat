import dev.fan4.compat.graves.AccessoryGraveCompat;
import dev.fan4.compat.shared.CompatibilityRules;
import io.wispforest.accessories.pond.DroppedStacksExtension;
import net.minecraft.class_1799;
import java.util.*;

public final class AccessoryGraveTest {
    public static class Inventory {
        public Player field_7546;public int space,received,dirty;
        public void method_7394(class_1799 stack){int moved=Math.min(space,stack.count);space-=moved;received+=moved;stack.count-=moved;}
        public void method_5431(){dirty++;}
    }
    public static class Player implements DroppedStacksExtension {
        public Collection<Object> queue=new ArrayList<>();public Inventory inventory=new Inventory();public int dropped;
        public Player(){inventory.field_7546=this;}
        public Collection<Object> toBeDroppedStacks(){return queue;}
        public void addToBeDroppedStacks(Collection<Object> stacks){queue=stacks;}
        public Inventory method_31548(){return inventory;}
        public Object method_5775(class_1799 stack){dropped+=stack.count;return new Object();}
        public Object method_7328(class_1799 stack,boolean random){return method_5775(stack);}
    }
    public static class Grave {
        private final List<Object> storedItems=new ArrayList<>();public int awards,despawns;
        public Grave(){for(int i=0;i<128;i++)storedItems.add(new class_1799(0));}
        public boolean isInventoryEmpty(){return storedItems.stream().allMatch(s->((class_1799)s).method_7960());}
        private void trySmartDespawn(){despawns++;}
        private void awardStoredExperience(Player player){awards++;}
        public int count(){return storedItems.stream().mapToInt(s->((class_1799)s).count).sum();}
    }
    public static class Window {
        public List<Object> stacks=new ArrayList<>();
        public Window(Grave grave){for(int i=0;i<54;i++)stacks.add(((class_1799)grave.storedItems.get(i)).method_7972());}
        public int method_5439(){return stacks.size();}
        public Object method_5438(int slot){return stacks.get(slot);}
    }
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    public static void main(String[] args) {
        Map<String,String> mods=Map.of("graves","1.0.0","aether","1.5.11","accessories","1.1.0-beta.48+1.21.1");
        String hook="dev.fan4.compat.mixin.graves.common.AccessoryGraveStorageMixin";
        check(CompatibilityRules.applies(mods,false,hook),"standalone Aether/graves gate needs no portals or ships");
        for(String absent:mods.keySet()){var missing=new HashMap<>(mods);missing.remove(absent);check(!CompatibilityRules.applies(missing,false,hook),"missing "+absent);}
        Player player=new Player();for(int i=0;i<16;i++)player.queue.add(new class_1799(i+1));
        check(!AccessoryGraveCompat.inventoryEmpty(true,player.inventory),"accessory-only death eligible");
        Grave failed=new Grave();AccessoryGraveCompat.capture(failed,player);AccessoryGraveCompat.spawned(false,failed);
        check(player.queue.size()==16,"failed spawn retains all normal accessory drops");
        Grave grave=new Grave();AccessoryGraveCompat.capture(grave,player);
        check(grave.count()==136&&player.queue.size()==16,"staged capture copies without consuming queue");
        ((class_1799)player.queue.iterator().next()).count=99;
        check(grave.count()==136,"grave holds independent copies");
        AccessoryGraveCompat.spawned(true,grave);check(player.queue.isEmpty(),"successful spawn consumes drop queue once");
        AccessoryGraveCompat.spawned(true,grave);check(grave.count()==136,"repeat spawn cannot recapture or duplicate");
        player.inventory.space=0;check(AccessoryGraveCompat.retrieveExtra(0,grave,player)==0&&grave.count()==136,"full inventory retains grave contents");
        player.inventory.space=5;check(AccessoryGraveCompat.retrieveExtra(0,grave,player)>0,"partial inventory insertion");
        check(grave.count()+player.inventory.received==136,"partial retrieve conserves items");
        player.inventory.space=1000;AccessoryGraveCompat.retrieveExtra(2,grave,player);
        check(grave.count()==0&&player.inventory.received==136&&grave.awards==1,"complete retrieve and accessory-only XP cleanup");
        // A full vanilla inventory plus functional/cosmetic accessories exceeds one screen.
        Grave full=new Grave();for(int i=0;i<41;i++)full.storedItems.set(i,new class_1799(1));
        Player many=new Player();for(int i=0;i<30;i++)many.queue.add(new class_1799(1));
        AccessoryGraveCompat.capture(full,many);AccessoryGraveCompat.spawned(true,full);
        Window window=new Window(full);for(int i=41;i<54;i++)window.stacks.set(i,new class_1799(0));
        AccessoryGraveCompat.sync(full,window);check(full.count()==58,"GUI sync preserves off-screen stacks");
        AccessoryGraveCompat.prepareScreen(full);check(full.count()==58&&!((class_1799)full.storedItems.get(41)).method_7960(),"reopening exposes overflow without losing items");
        Window reopened=new Window(full);AccessoryGraveCompat.sync(full,reopened);check(full.count()==58,"repeat GUI close conserves backlog");
        Player overflow=new Player();for(int i=0;i<100;i++)overflow.queue.add(new class_1799(1));
        Grave limit=new Grave();AccessoryGraveCompat.capture(limit,overflow);AccessoryGraveCompat.spawned(true,limit);
        check(limit.count()+overflow.dropped==100&&overflow.queue.isEmpty(),"bounded storage overflow drops normally rather than duplicating");
        com.kador.graves.GravesMod.CONFIG.retrieval.dropOverflowOnQuickRetrieve=true;
        Grave drop=new Grave();Player dropping=new Player();dropping.queue.add(new class_1799(12));
        AccessoryGraveCompat.capture(drop,dropping);AccessoryGraveCompat.spawned(true,drop);
        check(AccessoryGraveCompat.retrieveExtra(0,drop,dropping)==1&&dropping.dropped==12&&drop.count()==0,"configured overflow retrieval drops conserved stacks");
        com.kador.graves.GravesMod.CONFIG.retrieval.dropOverflowOnQuickRetrieve=false;
        Player kept=new Player();check(AccessoryGraveCompat.inventoryEmpty(true,kept.inventory),"empty resolved queue does not manufacture drops for KEEP/DESTROY");
        Grave old=new Grave();AccessoryGraveCompat.capture(null,kept);AccessoryGraveCompat.prepareScreen(old);check(old.count()==0,"old/empty graves safe");
        System.out.println("PASS: accessory-only deaths, spawn failure/success, copy isolation, full/partial recovery, GUI backlog conservation and optional gates (fixtures)");
    }
}
