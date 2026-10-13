/**
 * Every Iris, Minecraft and Fabric member the Bumblezone shader switch reaches by reflection must exist in the real jars. Args: the Iris
 * jar, Fabric's lifecycle and networking jars, and a 1.21.1 Minecraft jar with intermediary names. (Bumblezone itself is only named by id.)
 */
public final class BumblezoneNativeCheck {
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=args;
        String mc="net/minecraft/",iris="net/irisshaders/iris/api/v0/";
        XaeroSyncNativeCheck.method(iris+"IrisApi","getInstance","()",true);
        XaeroSyncNativeCheck.method(iris+"IrisApi","getConfig","()",false);
        XaeroSyncNativeCheck.method(iris+"IrisApiConfig","areShadersEnabled","()Z",false);
        XaeroSyncNativeCheck.method(iris+"IrisApiConfig","setShadersEnabledAndApply","(Z)V",false);
        XaeroSyncNativeCheck.method(mc+"class_310","method_1551","()",true);
        XaeroSyncNativeCheck.field(mc+"class_310","field_1687",false);XaeroSyncNativeCheck.field(mc+"class_310","field_1724",false);
        XaeroSyncNativeCheck.method(mc+"class_1937","method_27983","()",false);
        XaeroSyncNativeCheck.method(mc+"class_5321","method_29177","()",false);
        XaeroSyncNativeCheck.method(mc+"class_1657","method_7353","(L"+mc+"class_2561;Z)",false);
        XaeroSyncNativeCheck.method(mc+"class_2561","method_43470","(Ljava/lang/String;)",true);
        XaeroSyncNativeCheck.field("net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents","END_CLIENT_TICK",true);
        XaeroSyncNativeCheck.method("net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents$EndTick","onEndTick","(L"+mc+"class_310;)",false);
        XaeroSyncNativeCheck.field("net/fabricmc/fabric/api/client/networking/v1/ClientPlayConnectionEvents","DISCONNECT",true);
        XaeroSyncNativeCheck.method("net/fabricmc/fabric/api/client/networking/v1/ClientPlayConnectionEvents$Disconnect","onPlayDisconnect","(",false);
        System.out.println("PASS: every Iris API, Minecraft and Fabric member used by the Bumblezone shader switch exists in the real jars");
    }
}
