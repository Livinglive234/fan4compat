package dev.fan4.compat.jade;

import static dev.fan4.compat.shared.CompatCalls.*;

/** Jade compares/rerays hit vectors in world space; block addresses stay in ship space. */
public final class ShipRaycastCompat {
    private ShipRaycastCompat() {}
    public static Object worldPosition(Object position,Object entity) {
        return exact("org.valkyrienskies.mod.common.VSGameUtilsKt","toWorldCoordinates",
            new String[]{"net.minecraft.class_1937","net.minecraft.class_243"},call(entity,"method_37908"),position);
    }
    /** A crosshair result can survive the frame in which the camera changes dimensions. */
    public static Object currentHit(Object hit,Object entity,double blockRange,double entityRange) {
        if(hit==null)return null;
        Object position=worldPosition(call(hit,"method_17784"),entity);
        Object eye=worldPosition(call(entity,"method_5836",1.0f),entity);
        Object config=call(type("snownee.jade.api.config.IWailaConfig"),"get");
        Object mode=call(call(config,"getGeneral"),"getPerspectiveMode");
        Object origin=eye;
        if(!((Enum<?>)mode).name().equals("EYE")) {
            Object client=call(type("net.minecraft.class_310"),"method_1551");
            Object camera=call(field(client,"field_1773"),"method_19418");
            origin=worldPosition(call(camera,"method_19326"),entity);
        }
        double offset=Math.sqrt(((Number)call(eye,"method_1025",origin)).doubleValue());
        double distance=((Number)call(position,"method_1025",origin)).doubleValue();
        return usableHit(distance,offset,blockRange,entityRange)?hit:null;
    }
    public static boolean usableHit(double squaredDistance,double eyeCameraOffset,double blockRange,double entityRange) {
        double reach=Math.max(blockRange,entityRange)+eyeCameraOffset+1.0;
        return Double.isFinite(squaredDistance)&&Double.isFinite(reach)&&reach>=0&&squaredDistance<=reach*reach;
    }
}
