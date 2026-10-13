import java.lang.reflect.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.*;

/** The log4j filter hides Valkyrien Skies' save message only, whichever way log4j hands it over. */
public final class SaveLogFilterTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Class<?> type=Class.forName("dev.fan4.compat.valkyrienskies.SaveLogFilter");
        Method make=type.getDeclaredMethod("filter",ClassLoader.class);make.setAccessible(true);
        Filter filter=(Filter)make.invoke(null,SaveLogFilterTest.class.getClassLoader());
        String noise=" ShipSavedData.save(): pipeline bytes = {} KB",other=" something else from VS2";
        Log4jLogEvent noisy=Log4jLogEvent.newBuilder().setLoggerName("VS2").setLevel(Level.INFO).setMessage(new ParameterizedMessage(noise,0)).build(),
            quiet=Log4jLogEvent.newBuilder().setLoggerName("VS2").setLevel(Level.INFO).setMessage(new SimpleMessage(other)).build();
        check(filter.filter(noisy)==Filter.Result.DENY,"the save message event is denied");
        check(filter.filter(quiet)==Filter.Result.NEUTRAL,"other VS2 messages are untouched");
        check(filter.filter(null,Level.INFO,null,noise,new Object[]{0})==Filter.Result.DENY&&filter.filter(null,Level.INFO,null,other,new Object[0])==Filter.Result.NEUTRAL,"the string form is filtered the same way");
        check(filter.filter(null,Level.INFO,null,new SimpleMessage(noise),null)==Filter.Result.DENY&&filter.filter(null,Level.INFO,null,(Object)other,null)==Filter.Result.NEUTRAL,"message and object forms too");
        check(filter.filter(null,Level.INFO,null,(String)null,new Object[0])==Filter.Result.NEUTRAL,"a null message is left alone");
        check(filter.getOnMatch()==Filter.Result.NEUTRAL&&filter.getOnMismatch()==Filter.Result.NEUTRAL&&filter.isStarted()&&!filter.isStopped(),"lifecycle answers keep log4j satisfied");
        // Installed on the real root logger config, an INFO message from another logger passes and the noise does not reach an appender.
        Method install=type.getDeclaredMethod("install",ClassLoader.class);install.setAccessible(true);
        install.invoke(null,SaveLogFilterTest.class.getClassLoader());
        var context=(org.apache.logging.log4j.core.LoggerContext)org.apache.logging.log4j.LogManager.getContext(SaveLogFilterTest.class.getClassLoader(),false);
        var root=context.getConfiguration().getRootLogger();
        check(root.hasFilter()&&root.getFilter().filter(noisy)==Filter.Result.DENY&&root.getFilter().filter(quiet)==Filter.Result.NEUTRAL,"installed on the root logger config");
        System.out.println("PASS: Valkyrien Skies save message filter (event, string, message and object forms, other messages untouched, installed on the root logger)");
    }
}
