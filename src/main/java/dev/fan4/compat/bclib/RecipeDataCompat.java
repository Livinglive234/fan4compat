package dev.fan4.compat.bclib;
import java.util.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Bridge generated 1.21 stack JSON to BCLib's custom item/nbt codec. */
public final class RecipeDataCompat {
    public static void repair(Object identifier,Object json) {
        String id=identifier.toString();if(!id.startsWith("betterend:")&&!id.startsWith("bclib:"))return;
        if(!(Boolean)call(json,"isJsonObject"))return;
        Object object=call(json,"getAsJsonObject"),type=call(object,"get","type");
        if(type==null)return;String kind=call(type,"getAsString").toString();
        if(!Set.of("bclib:alloying","bclib:anvil","betterend:infusion").contains(kind))return;
        Object output=call(object,"get","result");
        if(output==null||!(Boolean)call(output,"isJsonObject"))return;
        output=call(output,"getAsJsonObject");
        if((Boolean)call(output,"has","id")&&!(Boolean)call(output,"has","item"))call(output,"add","item",call(output,"remove","id"));
        // BCLib's nbt field is already a component patch: keep it and all counts intact.
    }
    public static Map<?,?> available(Map<?,?> input,boolean jeed,boolean suppsquared) {
        return available(input,jeed,suppsquared,true);
    }
    private static Map<?,?> available(Map<?,?> input,boolean jeed,boolean suppsquared,boolean stacks) {
        Map<Object,Object> output=new LinkedHashMap<>();
        for(var entry:input.entrySet()) {
            String id=entry.getKey().toString();Object value=entry.getValue();
            if(!jeed&&Set.of("jeed:inebriation","jeed:remedy").contains(id)&&(Boolean)call(value,"isJsonObject")) {
                Object kind=call(call(value,"getAsJsonObject"),"get","type");
                if(kind!=null&&call(kind,"getAsString").equals("jeed:effect_provider"))continue;
            }
            if(!suppsquared&&id.equals("supplementaries:copper_lantern_conversion")&&value.toString().contains("\"suppsquared:copper_lantern\""))continue;
            if(stacks)repair(entry.getKey(),value);
            output.put(entry.getKey(),value);
        }
        return output;
    }
    public static Map<?,?> available(Map<?,?> input) {
        var loader=net.fabricmc.loader.api.FabricLoader.getInstance();
        boolean stacks=loader.getModContainer("bclib").map(mod->mod.getMetadata().getVersion().getFriendlyString().equals("30.4.0")).orElse(false);
        return available(input,loader.isModLoaded("jeed"),loader.isModLoaded("suppsquared"),stacks);
    }
}
