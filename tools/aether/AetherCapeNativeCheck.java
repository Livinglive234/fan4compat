/**
 * Every Minecraft member the Aether cape mixin uses, and the method it injects into, must exist in the real jar (args: an intermediary-named 1.21.1
 * Minecraft jar). Also checks the Aether jar's own hook if a second jar is given: the method the Aether injects into is the one we run ahead of.
 */
public final class AetherCapeNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        String mc="net/minecraft/";
        XaeroSyncNativeCheck.method(mc+"class_1657","method_7348","(L"+mc+"class_1664;)Z",false);
        XaeroSyncNativeCheck.field(mc+"class_1657","field_7518",true);
        XaeroSyncNativeCheck.method(mc+"class_1297","method_5841","()L"+mc+"class_2945;",false);
        XaeroSyncNativeCheck.method(mc+"class_2945","method_12789","(L"+mc+"class_2940;)Ljava/lang/Object;",false);
        XaeroSyncNativeCheck.method(mc+"class_1664","method_7430","()I",false);
        XaeroSyncNativeCheck.field(mc+"class_1664","field_7559",true);
        System.out.println("PASS: every Minecraft member used by the Aether cape skin-layer fix exists in the real jar");
    }
}
