package dev.fan4.compat.valkyrienskies;

import dev.fan4.compat.shared.Features;
import dev.fan4.compat.shared.GameLog;
import java.lang.reflect.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Valkyrien Skies logs "ShipSavedData.save(): pipeline bytes = N KB" at INFO on every world save. This hides that one message (unless the
 * "Valkyrien Skies save logging" switch is on) with a
 * log4j filter on the root logger config, by reflection (log4j is not a compile dependency). Nothing else VS logs is touched.
 */
public final class SaveLogFilter implements ModInitializer {
    static final String NOISE="ShipSavedData.save(): pipeline bytes";
    private static final String CORE="org.apache.logging.log4j.core.";
    public void onInitialize() {
        if(!FabricLoader.getInstance().isModLoaded("valkyrienskies")||Features.enabled("shipSaveLogging"))return;
        try{install(SaveLogFilter.class.getClassLoader());}
        catch(ReflectiveOperationException|RuntimeException|LinkageError failure){GameLog.warn("Could not hide Valkyrien Skies' save messages; they stay in the log",failure);}
    }
    static void install(ClassLoader loader)throws ReflectiveOperationException {
        Object context=Class.forName("org.apache.logging.log4j.LogManager",true,loader).getMethod("getContext",ClassLoader.class,boolean.class).invoke(null,loader,false);
        Class<?> coreContext=Class.forName(CORE+"LoggerContext",true,loader);
        if(!coreContext.isInstance(context))return;
        Object root=Class.forName(CORE+"config.Configuration",true,loader).getMethod("getRootLogger").invoke(coreContext.getMethod("getConfiguration").invoke(context));
        Class<?> filterable=Class.forName(CORE+"filter.Filterable",true,loader),filterType=Class.forName(CORE+"Filter",true,loader);
        filterable.getMethod("addFilter",filterType).invoke(root,filter(loader));
    }
    /** A log4j {@code Filter} that denies the one message and has no opinion on anything else. */
    static Object filter(ClassLoader loader)throws ClassNotFoundException {
        Class<?> filterType=Class.forName(CORE+"Filter",true,loader);
        return Proxy.newProxyInstance(loader,new Class<?>[]{filterType},(proxy,method,args)->{
            Class<?> result=method.getReturnType();
            if(method.getDeclaringClass()==Object.class)return switch(method.getName()){case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->"Fan4Compat save message filter";};
            if(method.getName().equals("filter"))return constant(result,args!=null&&isNoise(args)?"DENY":"NEUTRAL");
            if(result==boolean.class)return !method.getName().equals("isStopped")&&!method.getName().equals("stop");
            if(result.isEnum())return constant(result,method.getName().equals("getState")?"STARTED":"NEUTRAL");
            return null;
        });
    }
    private static Object constant(Class<?> type,String name) {
        for(Object value:type.getEnumConstants())if(((Enum<?>)value).name().equals(name))return value;
        return type.getEnumConstants()[0];
    }
    /** Any argument that is the message: a string, a log4j Message (its format), a LogEvent (its message), or an object printed as one. */
    private static boolean isNoise(Object[] args) {
        for(Object arg:args)if(arg!=null&&!arg.getClass().isArray()&&noisy(arg))return true;
        return false;
    }
    private static boolean noisy(Object arg) {
        try {
            if(arg instanceof CharSequence text)return text.toString().contains(NOISE);
            Class<?> type=arg.getClass();
            if(Class.forName(CORE+"LogEvent",true,type.getClassLoader()).isInstance(arg))return noisy(type.getMethod("getMessage").invoke(arg));
            if(Class.forName("org.apache.logging.log4j.message.Message",true,type.getClassLoader()).isInstance(arg))return String.valueOf(type.getMethod("getFormat").invoke(arg)).contains(NOISE);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){}
        return false;
    }
}
