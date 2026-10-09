package dev.fan4.compat.graves;

import java.util.*;
import com.google.gson.*;
import dev.fan4.compat.shared.WeakIdentityMap;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Captures only Accessories' already-resolved death drops, never live equipment. */
public final class AccessoryGraveCompat {
    public static final int STORAGE=128, VISIBLE=54, PLAYER_SLOTS=41;
    private record Capture(Object player,List<Object> overflow,List<TrinketGraveCompat.Entry> trinkets) {}
    private record Slot(String name,int index,boolean cosmetic,boolean trinket) {}
    private static final Map<Object,Capture> PENDING=Collections.synchronizedMap(new WeakIdentityMap<>());
    private static final Map<Object,IdentityHashMap<Object,Slot>> DEATH_SLOTS=Collections.synchronizedMap(new WeakIdentityMap<>());
    private static final Map<Object,Map<Integer,Slot>> GRAVE_SLOTS=Collections.synchronizedMap(new WeakIdentityMap<>());
    private static final Map<Object,Integer> RESTORED=Collections.synchronizedMap(new WeakIdentityMap<>());
    private static final String SLOT_TAG="Fan4CompatAccessorySlots";
    private AccessoryGraveCompat() {}
    @SuppressWarnings("unchecked") private static List<Object> items(Object grave){return (List<Object>)field(grave,"storedItems");}
    @SuppressWarnings("unchecked") private static Collection<Object> drops(Object player){return (Collection<Object>)call(player,"toBeDroppedStacks");}
    private static boolean empty(Object stack){return (boolean)call(stack,"method_7960");}
    private static Object copy(Object stack){return call(stack,"method_7972");}
    private static Object emptyStack(){return field(type("net.minecraft.class_1799"),"field_8037");}
    private static Map<Integer,Slot> origins(Object grave){return GRAVE_SLOTS.computeIfAbsent(grave,unused->new HashMap<>());}
    /** Remember the source only after native drop rules actually return a stack. */
    public static void beginDrops(Object entity) {
        if(type("io.wispforest.accessories.pond.DroppedStacksExtension").isInstance(entity))DEATH_SLOTS.put(entity,new IdentityHashMap<>());
    }
    public static void recordDrop(Object entity,Object container,Object reference,Object stack) {
        var pending=DEATH_SLOTS.get(entity);
        if(pending==null||stack==null||empty(stack))return;
        Object source=call(reference,"slotContainer");
        if(source==null)return;
        boolean cosmetic=container==call(source,"getCosmeticAccessories");
        pending.put(stack,new Slot((String)call(reference,"slotName"),((Number)call(reference,"slot")).intValue(),cosmetic,false));
    }
    public static boolean inventoryEmpty(boolean original,Object inventory) {
        if(!original)return false;
        Object player=field(inventory,"field_7546");
        if(!type("io.wispforest.accessories.pond.DroppedStacksExtension").isInstance(player))return original;
        return drops(player).stream().allMatch(AccessoryGraveCompat::empty)&&!TrinketGraveCompat.anyGraveable(player);
    }
    public static void capture(Object grave,Object player) {
        if(grave==null)return;
        List<Object> slots=items(grave),overflow=new ArrayList<>();int next=PLAYER_SLOTS;
        var death=DEATH_SLOTS.remove(player);var origins=origins(grave);
        for(Object stack:drops(player))if(!empty(stack)) {
            Object saved=copy(stack);
            if(next<slots.size()) {
                Slot origin=death==null?null:death.get(stack);
                if(origin!=null)origins.put(next,origin);
                slots.set(next++,saved);
            } else overflow.add(saved);
        }
        // Trinkets stacks follow the accessories; their slots are emptied only once the grave spawns.
        List<TrinketGraveCompat.Entry> trinkets=TrinketGraveCompat.collect(player);
        for(var entry:trinkets) {
            if(entry.destroy())continue;
            Object saved=copy(entry.stack());
            if(next<slots.size()) {
                origins.put(next,new Slot(entry.slot(),entry.index(),false,true));
                slots.set(next++,saved);
            } else overflow.add(saved);
        }
        PENDING.put(grave,new Capture(player,overflow,trinkets));
    }
    /** The original drop queue remains intact until spawnEntity reports success. */
    public static void spawned(boolean success,Object entity) {
        Capture capture=PENDING.remove(entity);
        if(!success||capture==null)return;
        call(capture.player,"addToBeDroppedStacks",List.of());
        for(Object stack:capture.overflow)call(capture.player,"method_5775",stack);
        TrinketGraveCompat.clear(capture.trinkets);
    }
    /** The six-row vanilla screen is a window; reopen it to expose remaining stacks. */
    public static void prepareScreen(Object grave) {
        List<Object> slots=items(grave);int next=VISIBLE;
        for(int i=0;i<VISIBLE;i++)if(empty(slots.get(i))) {
            while(next<slots.size()&&empty(slots.get(next)))next++;
            if(next==slots.size())break;
            slots.set(i,slots.get(next));slots.set(next,emptyStack());
            var origins=origins(grave);origins.remove(i);Slot origin=origins.remove(next++);
            if(origin!=null)origins.put(i,origin);
        }
    }
    public static void sync(Object grave,Object inventory) {
        List<Object> slots=items(grave);int bound=Math.min(VISIBLE,((Number)call(inventory,"method_5439")).intValue());
        for(int i=0;i<bound;i++) {
            Object stack=call(inventory,"method_5438",i);
            if(empty(stack)||!sameItem(slots.get(i),stack))origins(grave).remove(i);
            slots.set(i,copy(stack));
        }
        for(int i=bound;i<VISIBLE;i++){slots.set(i,emptyStack());origins(grave).remove(i);}
        privateCall(grave,"trySmartDespawn");
    }
    private static boolean sameItem(Object left,Object right){return (boolean)call(type("net.minecraft.class_1799"),"method_31577",left,right);}
    public static void saveSlots(Object grave,Object nbt) {
        JsonArray saved=new JsonArray();List<Object> slots=items(grave);
        for(var entry:new TreeMap<>(origins(grave)).entrySet()) {
            int index=entry.getKey();if(index<0||index>=slots.size()||empty(slots.get(index)))continue;
            Slot origin=entry.getValue();JsonObject item=new JsonObject();
            item.addProperty("graveSlot",index);item.addProperty("name",origin.name);item.addProperty("index",origin.index);item.addProperty("cosmetic",origin.cosmetic);if(origin.trinket)item.addProperty("trinket",true);saved.add(item);
        }
        call(nbt,"method_10582",SLOT_TAG,saved.toString());
    }
    public static void loadSlots(Object grave,Object nbt) {
        Map<Integer,Slot> origins=origins(grave);origins.clear();
        String saved=(String)call(nbt,"method_10558",SLOT_TAG);
        if(saved.isEmpty()||saved.length()>32768)return;
        try {
            JsonArray entries=JsonParser.parseString(saved).getAsJsonArray();
            if(entries.size()>STORAGE)return;
            Map<Integer,Slot> loaded=new HashMap<>();
            for(JsonElement element:entries) {
                JsonObject item=element.getAsJsonObject();int index=item.get("graveSlot").getAsInt(),slot=item.get("index").getAsInt();String name=item.get("name").getAsString();
                if(index<0||index>=items(grave).size()||slot<0||slot>1024||name.isEmpty()||name.length()>512||empty(items(grave).get(index)))continue;
                loaded.put(index,new Slot(name,slot,item.get("cosmetic").getAsBoolean(),item.has("trinket")&&item.get("trinket").getAsBoolean()));
            }
            origins.putAll(loaded);
        } catch(RuntimeException ignored) {
            // Old or invalid metadata falls back to ordinary recovery; item data is intact.
        }
    }
    public static void beginQuickRetrieve(Object grave,Object player) {
        int restored=0;
        Object config=call(type("com.kador.graves.GravesMod"),"getConfig");
        if((boolean)field(field(config,"retrieval"),"restoreExactInventoryLayout")) {
            // Trinkets stacks return to their original trinket slot when it is free and still valid.
            for(var entry:new TreeMap<>(origins(grave)).entrySet()) {
                Slot origin=entry.getValue();Object stack=items(grave).get(entry.getKey());
                if(!origin.trinket||empty(stack)||!TrinketGraveCompat.restore(player,origin.name,origin.index,copy(stack)))continue;
                items(grave).set(entry.getKey(),emptyStack());origins(grave).remove(entry.getKey());restored++;
            }
            Object capability=call(type("io.wispforest.accessories.api.AccessoriesCapability"),"get",player);
            if(capability!=null) {
                Map<?,?> containers=(Map<?,?>)call(capability,"getContainers");
                for(var entry:new TreeMap<>(origins(grave)).entrySet()) {
                    int index=entry.getKey();Object stack=items(grave).get(index);Slot origin=entry.getValue();
                    if(origin.trinket||empty(stack))continue;
                    Object container=containers.get(origin.name);
                    if(container==null||origin.index>=((Number)call(container,"getSize")).intValue())continue;
                    Object contents=call(container,origin.cosmetic?"getCosmeticAccessories":"getAccessories");
                    if(!empty(call(contents,"method_5438",origin.index)))continue;
                    Object slot=create("io.wispforest.accessories.menu.AccessoriesInternalSlot",container,origin.cosmetic,origin.index,0,0);
                    if(!(boolean)call(slot,"method_7680",stack)||((Number)call(stack,"method_7947")).intValue()>((Number)call(slot,"method_7676",stack)).intValue())continue;
                    call(slot,"method_7673",copy(stack));
                    items(grave).set(index,emptyStack());origins(grave).remove(index);restored++;
                }
            }
        }
        RESTORED.put(grave,restored);
    }
    public static int endQuickRetrieve(int moved,Object grave,Object player) {
        int restored=RESTORED.getOrDefault(grave,0);RESTORED.remove(grave);
        origins(grave).keySet().removeIf(index->empty(items(grave).get(index)));
        if(restored>0)finishRetrieve(grave,player);
        return moved+restored;
    }
    private static void finishRetrieve(Object grave,Object player) {
        call(call(player,"method_31548"),"method_5431");
        if((boolean)call(grave,"isInventoryEmpty")) {
            privateCall(grave,"awardStoredExperience",player);
            call(type("com.kador.graves.entity.GraveLocatorCompass"),"removeForGrave",player,grave);
        }
        privateCall(grave,"trySmartDespawn");
    }
    private static Object privateCall(Object grave,String name,Object... args) {
        try {
            for(var method:grave.getClass().getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length) {
                method.setAccessible(true);return method.invoke(grave,args);
            }
            throw new NoSuchMethodException(name);
        } catch(ReflectiveOperationException e){throw new IllegalStateException("Grave compatibility call failed: "+name,e);}
    }
    public static int retrieveExtra(int moved,Object grave,Object player) {
        List<Object> slots=items(grave);Object inventory=call(player,"method_31548");
        Object config=call(type("com.kador.graves.GravesMod"),"getConfig");
        boolean dropOverflow=(boolean)field(field(config,"retrieval"),"dropOverflowOnQuickRetrieve");
        int extra=0;
        for(int i=PLAYER_SLOTS;i<slots.size();i++) {
            Object stack=slots.get(i);if(empty(stack))continue;
            Object remaining=copy(stack);call(inventory,"method_7394",remaining);
            if(!empty(remaining)&&dropOverflow) {
                // Clear only when the native item spawn succeeds.
                if(call(player,"method_7328",copy(remaining),false)!=null)remaining=emptyStack();
            }
            if(((Number)call(remaining,"method_7947")).intValue()==((Number)call(stack,"method_7947")).intValue())continue;
            slots.set(i,remaining);extra++;
        }
        if(extra>0)finishRetrieve(grave,player);
        return moved+extra;
    }
}
