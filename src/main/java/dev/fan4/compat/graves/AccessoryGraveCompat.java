package dev.fan4.compat.graves;

import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Captures only Accessories' already-resolved death drops, never live equipment. */
public final class AccessoryGraveCompat {
    public static final int STORAGE=128, VISIBLE=54, PLAYER_SLOTS=41;
    private record Capture(Object player,List<Object> overflow) {}
    private static final Map<Object,Capture> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    private AccessoryGraveCompat() {}
    @SuppressWarnings("unchecked") private static List<Object> items(Object grave){return (List<Object>)field(grave,"storedItems");}
    @SuppressWarnings("unchecked") private static Collection<Object> drops(Object player){return (Collection<Object>)call(player,"toBeDroppedStacks");}
    private static boolean empty(Object stack){return (boolean)call(stack,"method_7960");}
    private static Object copy(Object stack){return call(stack,"method_7972");}
    private static Object emptyStack(){return field(type("net.minecraft.class_1799"),"field_8037");}
    public static boolean inventoryEmpty(boolean original,Object inventory) {
        if(!original)return false;
        Object player=field(inventory,"field_7546");
        if(!type("io.wispforest.accessories.pond.DroppedStacksExtension").isInstance(player))return original;
        return drops(player).stream().allMatch(AccessoryGraveCompat::empty);
    }
    public static void capture(Object grave,Object player) {
        if(grave==null)return;
        List<Object> slots=items(grave),overflow=new ArrayList<>();int next=PLAYER_SLOTS;
        for(Object stack:drops(player))if(!empty(stack)) {
            Object saved=copy(stack);
            if(next<slots.size())slots.set(next++,saved);else overflow.add(saved);
        }
        PENDING.put(grave,new Capture(player,overflow));
    }
    /** The original drop queue remains intact until spawnEntity reports success. */
    public static void spawned(boolean success,Object entity) {
        Capture capture=PENDING.remove(entity);
        if(!success||capture==null)return;
        call(capture.player,"addToBeDroppedStacks",List.of());
        for(Object stack:capture.overflow)call(capture.player,"method_5775",stack);
    }
    /** The six-row vanilla screen is a window; reopen it to expose remaining stacks. */
    public static void prepareScreen(Object grave) {
        List<Object> slots=items(grave);int next=VISIBLE;
        for(int i=0;i<VISIBLE;i++)if(empty(slots.get(i))) {
            while(next<slots.size()&&empty(slots.get(next)))next++;
            if(next==slots.size())break;
            slots.set(i,slots.get(next));slots.set(next++,emptyStack());
        }
    }
    public static void sync(Object grave,Object inventory) {
        List<Object> slots=items(grave);int bound=Math.min(VISIBLE,((Number)call(inventory,"method_5439")).intValue());
        for(int i=0;i<bound;i++)slots.set(i,copy(call(inventory,"method_5438",i)));
        for(int i=bound;i<VISIBLE;i++)slots.set(i,emptyStack());
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
        if(extra>0) {
            call(inventory,"method_5431");
            if((boolean)call(grave,"isInventoryEmpty")) {
                privateCall(grave,"awardStoredExperience",player);
                call(type("com.kador.graves.entity.GraveLocatorCompass"),"removeForGrave",player,grave);
            }
            privateCall(grave,"trySmartDespawn");
        }
        return moved+extra;
    }
}
