package dev.fan4.compat.shared;

import java.lang.reflect.Method;

/**
 * Writes to the game's own log (Minecraft's log4j logger, which is what ends up in latest.log). {@code System.Logger} goes to Java's
 * default logging instead and never reaches latest.log, so every message the addon wants a player to see or send us goes through here.
 * Falls back to {@code System.Logger} when log4j is not there (tests).
 */
public final class GameLog {
    private static final Object LOGGER;private static final Method INFO,WARN,WARN_FAILURE,DEBUG,DEBUG_FAILURE;
    static {
        Object logger=null;Method info=null,warn=null,warnFailure=null,debug=null,debugFailure=null;
        try {
            ClassLoader loader=GameLog.class.getClassLoader();
            Class<?> manager=Class.forName("org.apache.logging.log4j.LogManager",true,loader),type=Class.forName("org.apache.logging.log4j.Logger",true,loader);
            logger=manager.getMethod("getLogger",String.class).invoke(null,"Fan4Compat");
            info=type.getMethod("info",String.class);warn=type.getMethod("warn",String.class);warnFailure=type.getMethod("warn",String.class,Throwable.class);
            debug=type.getMethod("debug",String.class);debugFailure=type.getMethod("debug",String.class,Throwable.class);
        } catch(ReflectiveOperationException|LinkageError absent){logger=null;}
        LOGGER=logger;INFO=info;WARN=warn;WARN_FAILURE=warnFailure;DEBUG=debug;DEBUG_FAILURE=debugFailure;
    }
    private GameLog() {}
    public static void info(String message){write(INFO,System.Logger.Level.INFO,message,null);}
    public static void warn(String message){write(WARN,System.Logger.Level.WARNING,message,null);}
    public static void warn(String message,Throwable failure){write(WARN_FAILURE,System.Logger.Level.WARNING,message,failure);}
    public static void debug(String message){write(DEBUG,System.Logger.Level.DEBUG,message,null);}
    public static void debug(String message,Throwable failure){write(DEBUG_FAILURE,System.Logger.Level.DEBUG,message,failure);}
    private static void write(Method method,System.Logger.Level level,String message,Throwable failure) {
        if(LOGGER!=null)try{if(failure==null||method.getParameterCount()==1)method.invoke(LOGGER,message);else method.invoke(LOGGER,message,failure);return;}catch(ReflectiveOperationException|LinkageError broken){}
        if(failure==null)System.getLogger("Fan4Compat").log(level,message);else System.getLogger("Fan4Compat").log(level,message,failure);
    }
}
