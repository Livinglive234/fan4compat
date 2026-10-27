import java.util.*;

/**
 * Every method the Spatial GUI trace injects into must exist, with the exact descriptor, in the real jars (args: the Spatial GUI jar and an intermediary-named
 * 1.21.1 Minecraft jar), and every call the trace makes through reflection must resolve.
 */
public final class SpatialGuiNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        String spatial="org/tastytrash/spatialGUI/",renderer=spatial+"render/SpatialGUIRenderer",manager=spatial+"render/TextureTargetManager",mc="net/minecraft/";
        // Injection targets (exact descriptors).
        XaeroSyncNativeCheck.method(renderer,"onFrameStart","()V",false);
        XaeroSyncNativeCheck.method(renderer,"extractIsolatedScreen","(L"+mc+"class_437;F)V",false);
        XaeroSyncNativeCheck.method(renderer,"capturePerspectiveState","(Lorg/joml/Matrix4f;L"+mc+"class_8251;L"+mc+"class_4587;)V",false);
        XaeroSyncNativeCheck.method(renderer,"renderInWorld","(L"+mc+"class_4587;)V",false);
        XaeroSyncNativeCheck.method(renderer,"renderInWorldPost","()V",false);
        XaeroSyncNativeCheck.method(manager,"prepareTarget","()V",false);
        XaeroSyncNativeCheck.method(manager,"clearTarget","()V",false);
        XaeroSyncNativeCheck.method(mc+"class_761","method_22710","(",false);
        // Reads the snapshot makes.
        XaeroSyncNativeCheck.method(spatial+"client/SpatialGUIClient","renderer","()L"+renderer+";",true);
        XaeroSyncNativeCheck.method(renderer,"getHookedScreen","()L"+mc+"class_437;",false);
        XaeroSyncNativeCheck.method(renderer,"getTargetManager","()L"+manager+";",false);
        XaeroSyncNativeCheck.method(manager,"getInventoryTarget","()L"+mc+"class_6367;",false);
        for(String field:new String[]{"renderingLevel","isExtractingScreen","skipWindowOverride"})XaeroSyncNativeCheck.field(renderer,field,true);
        XaeroSyncNativeCheck.field(mc+"class_310","field_1755",false);
        for(String field:new String[]{"field_1476","field_1480","field_1477"})XaeroSyncNativeCheck.field(mc+"class_276",field,false);
        // The camera readout.
        XaeroSyncNativeCheck.method(mc+"class_757","method_19418","()L"+mc+"class_4184;",false);
        XaeroSyncNativeCheck.field(mc+"class_310","field_1773",false);
        for(String field:new String[]{"field_18712","field_18717","field_18718"})XaeroSyncNativeCheck.field(mc+"class_4184",field,false);
        for(String field:new String[]{"field_1352","field_1351","field_1350"})XaeroSyncNativeCheck.field(mc+"class_243",field,false);
        System.out.println("PASS: every Spatial GUI and Minecraft member used by the portal trace exists in the real jars with the exact descriptors");
    }
}
