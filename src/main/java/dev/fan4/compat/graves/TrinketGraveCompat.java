package dev.fan4.compat.graves;

import java.util.*;
import java.util.function.BiConsumer;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/**
 * Player Graves cancels Player.dropInventory once a grave exists, so Trinkets' own drop pass
 * (the tail of LivingEntity.dropInventory) never runs and equipped trinkets stay on the body.
 * Resolve Trinkets' drop rules here instead and stage the stacks into the grave.
 * Public Trinkets API only, no injectors; any failure leaves the previous behavior untouched.
 */
final class TrinketGraveCompat {
    private static final String API="dev.emi.trinkets.api.TrinketsApi",TRINKET="dev.emi.trinkets.api.Trinket",
        REFERENCE="dev.emi.trinkets.api.SlotReference",RULE="dev.emi.trinkets.api.TrinketEnums$DropRule",
        CALLBACK="dev.emi.trinkets.api.event.TrinketDropCallback",STACK="net.minecraft.class_1799",LIVING="net.minecraft.class_1309";
    static final String SUPPORTED="3.10.0";
    private static boolean warned;
    /** destroy: Trinkets would delete the stack; it is cleared on success and never graved. */
    record Entry(Object inventory,int index,Object stack,boolean destroy) {}
    private TrinketGraveCompat() {}
    static boolean enabled() {
        if(!present(API))return false;
        try {
            return FabricLoader.getInstance().getModContainer("trinkets")
                .map(mod->SUPPORTED.equals(mod.getMetadata().getVersion().getFriendlyString())).orElse(true);
        } catch(RuntimeException|LinkageError unavailable) {
            // No loader outside the game (fixtures): the class probe above is the only gate.
            return true;
        }
    }
    /** Stacks Trinkets would drop or destroy for this death, in Trinkets' own iteration order. */
    static List<Entry> collect(Object player) {
        if(!enabled())return List.of();
        try {
            Optional<?> component=(Optional<?>)call(type(API),"getTrinketComponent",player);
            if(component.isEmpty())return List.of();
            List<Object[]> equipped=new ArrayList<>();
            call(component.get(),"forEach",(BiConsumer<Object,Object>)(reference,stack)->equipped.add(new Object[]{reference,stack}));
            boolean keepInventory=keepInventory(player);
            List<Entry> entries=new ArrayList<>();
            for(Object[] slot:equipped) {
                Object reference=slot[0],stack=slot[1];
                if((boolean)call(stack,"method_7960"))continue;
                String rule=rule(player,reference,stack);boolean defaulted="DEFAULT".equals(rule);
                if(defaulted)rule=keepInventory?"KEEP":vanishes(stack)?"DESTROY":"DROP";
                // Graves already ignored keepInventory to reach this point; honor only explicit item/slot/event KEEP.
                if(rule.equals("KEEP")&&!(defaulted&&keepInventory))continue;
                if(!rule.equals("KEEP")&&!rule.equals("DROP")&&!rule.equals("DESTROY"))continue;
                entries.add(new Entry(call(reference,"inventory"),((Number)call(reference,"index")).intValue(),stack,rule.equals("DESTROY")));
            }
            return entries;
        } catch(RuntimeException|LinkageError failure) {
            if(!warned) {
                warned=true;
                System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Trinkets grave capture disabled: "+failure);
            }
            return List.of();
        }
    }
    static boolean anyGraveable(Object player) {
        for(Entry entry:collect(player))if(!entry.destroy)return true;
        return false;
    }
    /** Empty only slots still holding the exact captured stack, after the grave exists. */
    static void clear(List<Entry> entries) {
        for(Entry entry:entries) {
            try {
                if(call(entry.inventory,"method_5438",entry.index)==entry.stack)
                    call(entry.inventory,"method_5447",entry.index,field(type(STACK),"field_8037"));
            } catch(RuntimeException|LinkageError failure) {
                System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,"Trinkets slot not cleared after grave capture: "+failure);
            }
        }
    }
    /** Mirrors Trinkets 3.10.0: item rule, then the drop event, then the slot type's rule. */
    private static String rule(Object player,Object reference,Object stack) {
        String[] parameters={RULE,STACK,REFERENCE,LIVING};
        Object trinket=call(type(API),"getTrinket",call(stack,"method_7909"));
        Object rule=callTyped(TRINKET,"getDropRule",RULE,trinket,new String[]{STACK,REFERENCE,LIVING},stack,reference,player);
        Object invoker=call(field(type(CALLBACK),"EVENT"),"invoker");
        rule=callTyped(CALLBACK,"drop",RULE,invoker,parameters,rule,stack,reference,player);
        if(name(rule).equals("DEFAULT"))rule=call(call(call(reference,"inventory"),"getSlotType"),"getDropRule");
        return name(rule);
    }
    private static String name(Object rule){return ((Enum<?>)rule).name();}
    private static boolean keepInventory(Object player) {
        Object rules=call(call(player,"method_37908"),"method_8450");
        return (boolean)call(rules,"method_8355",field(type("net.minecraft.class_1928"),"field_19389"));
    }
    private static boolean vanishes(Object stack) {
        return (boolean)call(type("net.minecraft.class_1890"),"method_60142",stack,field(type("net.minecraft.class_9701"),"field_51655"));
    }
}
