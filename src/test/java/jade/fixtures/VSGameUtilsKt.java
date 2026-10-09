package org.valkyrienskies.mod.common;
import net.minecraft.*;
public class VSGameUtilsKt {
    public static class_243 toWorldCoordinates(class_1937 world,class_243 pos){
        return world.ship&&pos.x()>10000?new class_243(30-pos.z(),pos.y()+10,pos.x()-20000):pos;
    }
    // Exact overload selection must not confuse client-world overloads.
    public static class_243 toWorldCoordinates(Object world,Object pos){throw new AssertionError("wrong overload");}
}
