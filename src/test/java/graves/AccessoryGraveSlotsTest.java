import dev.fan4.compat.graves.AccessoryGraveCompat;
import net.minecraft.class_1799;
import java.util.*;

public final class AccessoryGraveSlotsTest {
    public static class Contents {
        public final List<class_1799> stacks=new ArrayList<>();public int changes;
        public Contents(int size){for(int i=0;i<size;i++)stacks.add(new class_1799(0));}
        public class_1799 method_5438(int index){return stacks.get(index);}
        public void method_5447(int index,class_1799 stack){stacks.set(index,stack);changes++;}
    }
    public static class Container {
        public final Contents normal,cosmetic;public boolean valid=true,canEquip=true;
        public Container(int size){normal=new Contents(size);cosmetic=new Contents(size);}
        public int getSize(){return normal.stacks.size();}
        public Contents getAccessories(){return normal;}
        public Contents getCosmeticAccessories(){return cosmetic;}
        public boolean allowed(boolean cosmetic){return valid&&(cosmetic||canEquip);}
    }
    public static class Capability {
        public final Map<String,Container> containers=new HashMap<>();
        public Map<String,Container> getContainers(){return containers;}
    }
    public record Reference(String slotName,int slot,Container slotContainer) {}
    public static class Nbt {
        public final Map<String,String> values=new HashMap<>();
        public void method_10582(String name,String value){values.put(name,value);}
        public String method_10558(String name){return values.getOrDefault(name,"");}
    }
    static class_1799 item(String name){class_1799 stack=new class_1799(1);stack.item=name;return stack;}
    static void drop(AccessoryGraveTest.Player player,Container container,String name,int index,boolean cosmetic,class_1799 item) {
        AccessoryGraveCompat.recordDrop(player,cosmetic?container.cosmetic:container.normal,new Reference(name,index,container),item);
        player.queue=new ArrayList<>(player.queue);player.queue.add(item);
    }
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    static AccessoryGraveTest.Grave grave(AccessoryGraveTest.Player player) {
        var grave=new AccessoryGraveTest.Grave();AccessoryGraveCompat.capture(grave,player);AccessoryGraveCompat.spawned(true,grave);return grave;
    }
    static int retrieve(AccessoryGraveTest.Grave grave,AccessoryGraveTest.Player player) {
        AccessoryGraveCompat.beginQuickRetrieve(grave,player);
        int fallback=AccessoryGraveCompat.retrieveExtra(0,grave,player);
        return AccessoryGraveCompat.endQuickRetrieve(fallback,grave,player);
    }
    public static void main(String[] args) {
        var player=new AccessoryGraveTest.Player();var cap=new Capability();player.capability=cap;
        Container rings=new Container(2);cap.containers.put("aether:ring",rings);
        AccessoryGraveCompat.beginDrops(player);
        drop(player,rings,"aether:ring",0,false,item("same-ring"));
        drop(player,rings,"aether:ring",1,false,item("same-ring"));
        drop(player,rings,"aether:ring",1,true,item("cosmetic-ring"));
        var captured=grave(player);Nbt nbt=new Nbt();AccessoryGraveCompat.saveSlots(captured,nbt);
        // Persisted item stacks are owned by the native grave codec; metadata must survive a fresh grave instance.
        var reloaded=new AccessoryGraveTest.Grave();
        for(int i=41;i<44;i++) {
            try {
                var f=AccessoryGraveTest.Grave.class.getDeclaredField("storedItems");f.setAccessible(true);
                @SuppressWarnings("unchecked") var source=(List<Object>)f.get(captured);
                @SuppressWarnings("unchecked") var target=(List<Object>)f.get(reloaded);
                target.set(i,((class_1799)source.get(i)).method_7972());
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        }
        AccessoryGraveCompat.loadSlots(reloaded,nbt);
        check(retrieve(reloaded,player)==3,"quick-retrieve count includes all equipped stacks after save/reload");
        check(rings.normal.stacks.get(0).item.equals("same-ring")&&rings.normal.stacks.get(1).item.equals("same-ring"),"identical rings return to separate original indices");
        check(rings.cosmetic.stacks.get(1).item.equals("cosmetic-ring")&&rings.cosmetic.stacks.get(0).count==0,"cosmetic slot retained");
        check(reloaded.count()==0&&player.inventory.received==0&&rings.normal.changes==2&&rings.cosmetic.changes==1,"native setter path, no inventory duplication");
        check(retrieve(reloaded,player)==0,"repeated crouch retrieval cannot duplicate equipped items");
        // Occupied, resized, invalid and missing slots leave items available for normal fallback.
        var blocked=new AccessoryGraveTest.Player();var blockedCap=new Capability();blocked.capability=blockedCap;
        var container=new Container(2);blockedCap.containers.put("aether:ring",container);
        AccessoryGraveCompat.beginDrops(blocked);
        drop(blocked,container,"aether:ring",0,false,item("old-ring"));
        drop(blocked,container,"aether:ring",1,false,item("other-ring"));
        var blockedGrave=grave(blocked);container.normal.stacks.set(0,item("respawn-ring"));
        container.valid=false;
        check(retrieve(blockedGrave,blocked)==0&&blockedGrave.count()==2,"full inventory and unavailable destination retain all items");
        check(container.normal.stacks.get(0).item.equals("respawn-ring"),"respawn equipment never overwritten");
        blocked.inventory.space=2;check(retrieve(blockedGrave,blocked)==2&&blocked.inventory.received==2&&blockedGrave.count()==0,"normal inventory fallback for occupied/invalid slots");
        var missing=new AccessoryGraveTest.Player();missing.capability=new Capability();AccessoryGraveCompat.beginDrops(missing);
        drop(missing,container,"removed:slot",1,false,item("removed-slot-item"));var missingGrave=grave(missing);
        check(retrieve(missingGrave,missing)==0&&missingGrave.count()==1,"removed slot retains recoverable item");
        ((Capability)missing.capability).containers.put("removed:slot",new Container(1));
        check(retrieve(missingGrave,missing)==0&&missingGrave.count()==1,"shrunk slot rejects old index safely");
        // GUI paging can move accessories into the visible normal-inventory range.
        var paged=new AccessoryGraveTest.Player();var pageCap=new Capability();paged.capability=pageCap;
        var pageContainer=new Container(1);pageCap.containers.put("aether:ring",pageContainer);
        AccessoryGraveCompat.beginDrops(paged);
        for(int i=0;i<13;i++)paged.queue.add(item("untagged"));
        drop(paged,pageContainer,"aether:ring",0,false,item("paged-ring"));var pageGrave=grave(paged);
        AccessoryGraveCompat.prepareScreen(pageGrave);
        AccessoryGraveCompat.sync(pageGrave,new AccessoryGraveTest.Window(pageGrave));
        AccessoryGraveCompat.beginQuickRetrieve(pageGrave,paged);
        check(AccessoryGraveCompat.endQuickRetrieve(0,pageGrave,paged)==1&&pageContainer.normal.stacks.get(0).item.equals("paged-ring"),"GUI-refilled slot keeps original accessory destination");
        // Replacing a GUI stack must not transfer the old item's slot metadata.
        var replacement=new AccessoryGraveTest.Player();replacement.capability=pageCap;pageContainer.normal.stacks.set(0,new class_1799(0));
        AccessoryGraveCompat.beginDrops(replacement);drop(replacement,pageContainer,"aether:ring",0,false,item("ring"));
        var replaced=grave(replacement);var window=new AccessoryGraveTest.Window(replaced);window.stacks.set(41,item("deposited-item"));AccessoryGraveCompat.sync(replaced,window);
        AccessoryGraveCompat.beginQuickRetrieve(replaced,replacement);check(AccessoryGraveCompat.endQuickRetrieve(0,replaced,replacement)==0&&pageContainer.normal.stacks.get(0).count==0,"GUI deposit does not inherit equipment origin");
        // Exact-layout opt-out and legacy/malformed tags remain ordinary inventory recovery.
        com.kador.graves.GravesMod.CONFIG.retrieval.restoreExactInventoryLayout=false;
        AccessoryGraveCompat.beginDrops(replacement);drop(replacement,pageContainer,"aether:ring",0,false,item("optout"));var opted=grave(replacement);
        check(retrieve(opted,replacement)==0&&opted.count()==1,"exact layout disabled avoids re-equipping");
        com.kador.graves.GravesMod.CONFIG.retrieval.restoreExactInventoryLayout=true;
        Nbt malformed=new Nbt();malformed.values.put("Fan4CompatAccessorySlots","invalid");AccessoryGraveCompat.loadSlots(opted,malformed);
        replacement.inventory.space=1;check(retrieve(opted,replacement)==1&&opted.count()==0&&pageContainer.normal.stacks.get(0).count==0,"malformed or legacy metadata recovers items normally");
        System.out.println("PASS: original functional/cosmetic slots, identical rings, persisted origins, occupied/missing/shrunk/invalid slots, GUI paging/deposits, count/no duplication and exact-layout opt-out (fixtures)");
    }
}
