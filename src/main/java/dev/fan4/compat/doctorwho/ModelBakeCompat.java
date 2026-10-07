package dev.fan4.compat.doctorwho;

import java.util.*;
import java.util.stream.Stream;
import dev.fan4.compat.shared.WeakIdentityMap;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Renderer-owned baked roots, reset between draws and discarded on resource reload. */
public final class ModelBakeCompat {
    private record Part(Object part,boolean visible,boolean hidden) {
        void reset(){call(part,"method_41923");writeField(part,"field_3665",visible);writeField(part,"field_38456",hidden);}
    }
    private record Root(Object root,List<Part> parts) {}
    private static final Map<Object,Map<Object,Root>> ROOTS=new WeakIdentityMap<>();
    private static Object generation;
    public static Object root(Object owner,Object context,Object layer,String method) {
        Object loader=call(call(type("net.minecraft.class_310"),"method_1551"),"method_31974");
        Object current=field(loader,"field_27542");
        if(current!=generation){ROOTS.clear();generation=current;}
        var layers=ROOTS.computeIfAbsent(owner,unused->new HashMap<>());
        Root cached=layers.get(layer);
        if(cached==null) {
            Object root=call(context,method,layer);
            List<Part> parts=new ArrayList<>();
            try(Stream<?> stream=(Stream<?>)call(root,"method_32088")) {
                stream.forEach(part->parts.add(new Part(part,(Boolean)field(part,"field_3665"),(Boolean)field(part,"field_38456"))));
            }
            cached=new Root(root,List.copyOf(parts));layers.put(layer,cached);
        } else cached.parts.forEach(Part::reset);
        return cached.root;
    }
}
