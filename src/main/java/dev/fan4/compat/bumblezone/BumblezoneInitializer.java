package dev.fan4.compat.bumblezone;

import dev.fan4.compat.shared.Features;
import dev.fan4.compat.shared.GameLog;
import java.lang.reflect.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Client side of {@link BumblezoneShaders}: watches the player's dimension and switches the Iris shader pack through Iris's public API. */
public final class BumblezoneInitializer implements ModInitializer {
    private static final String CLIENT="net.minecraft.class_310",TICKS="net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents",
        CONNECTION="net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents",IRIS_API="net.irisshaders.iris.api.v0.IrisApi",
        IRIS_CONFIG="net.irisshaders.iris.api.v0.IrisApiConfig",TARDIS_HELPER="net.drgmes.dwm.utils.helpers.TardisHelper";
    private static final BumblezoneShaders LOGIC=new BumblezoneShaders();
    private static boolean warned;

    public void onInitialize() {
        FabricLoader loader=FabricLoader.getInstance();
        if(loader.getEnvironmentType()!=EnvType.CLIENT||!Features.enabled("bumblezoneShaders"))return;
        if(!loader.isModLoaded("iris")||!loader.isModLoaded("the_bumblezone")||!loader.isModLoaded("fabric-lifecycle-events-v1")||!loader.isModLoaded("fabric-networking-api-v1"))return;
        try {
            call(field(type(TICKS),"END_CLIENT_TICK"),"register",listener(TICKS+"$EndTick",BumblezoneInitializer::tick));
            call(field(type(CONNECTION),"DISCONNECT"),"register",listener(CONNECTION+"$Disconnect",args->guard(()->LOGIC.leftWorld(new IrisHost(null)))));
            GameLog.info("Bumblezone shader switch is active");
        } catch(RuntimeException|LinkageError failure) {
            GameLog.warn("The Bumblezone shader switch could not start and is disabled",failure);
        }
    }
    private static void tick(Object[] args) {
        guard(()->{
            Object client=call(type(CLIENT),"method_1551"),world=field(client,"field_1687"),player=field(client,"field_1724");
            if(world==null||player==null)return;
            Object key=call(world,"method_27983");
            LOGIC.dimension(String.valueOf(call(key,"method_29177")),tardis(key),new IrisHost(player));
        });
    }
    /** True for one of the Doctor Who Mod's TARDIS interior dimensions; false when that mod is not there. */
    private static boolean tardis(Object key) {
        if(!FabricLoader.getInstance().isModLoaded("dwm"))return false;
        return (Boolean)call(type(TARDIS_HELPER),"isTardisDimension",key);
    }
    private static void guard(Runnable action) {
        try{action.run();}
        catch(RuntimeException|LinkageError failure){if(!warned){warned=true;GameLog.warn("The Bumblezone shader switch hit a problem (logged once)",failure);}}
    }
    private static Object listener(String iface,java.util.function.Consumer<Object[]> body) {
        return Proxy.newProxyInstance(BumblezoneInitializer.class.getClassLoader(),new Class<?>[]{type(iface)},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->"Fan4Compat listener";};
            body.accept(args);return null;
        });
    }
    /** Iris's public API, called through its interfaces (the implementation classes are not public). */
    private record IrisHost(Object player) implements BumblezoneShaders.Host {
        private static Object config()throws ReflectiveOperationException {
            return type(IRIS_API).getMethod("getConfig").invoke(type(IRIS_API).getMethod("getInstance").invoke(null));
        }
        public boolean shadersOn() {
            try{return (Boolean)type(IRIS_CONFIG).getMethod("areShadersEnabled").invoke(config());}
            catch(ReflectiveOperationException broken){throw new IllegalStateException("Iris API unavailable",broken);}
        }
        public void setShaders(boolean on) {
            try{type(IRIS_CONFIG).getMethod("setShadersEnabledAndApply",boolean.class).invoke(config(),on);}
            catch(ReflectiveOperationException broken){throw new IllegalStateException("Iris API unavailable",broken);}
        }
        public void message(String text) {
            if(player!=null)call(player,"method_7353",call(type("net.minecraft.class_2561"),"method_43470",text),false);
        }
    }
}
