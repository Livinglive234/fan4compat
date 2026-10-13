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
        // A reusable event is its own message (getMessage returns itself): it must be read, not followed in a circle.
        org.apache.logging.log4j.core.impl.MutableLogEvent reusable=new org.apache.logging.log4j.core.impl.MutableLogEvent();
        reusable.setMessage(new ParameterizedMessage(noise,0));
        check(reusable.getMessage()==reusable||reusable.getMessage()!=null,"fixture: a mutable event");
        check(filter.filter(reusable)==Filter.Result.DENY,"a reusable event carrying the save message is denied");
        org.apache.logging.log4j.core.impl.MutableLogEvent other2=new org.apache.logging.log4j.core.impl.MutableLogEvent();other2.setMessage(new SimpleMessage(other));
        check(filter.filter(other2)==Filter.Result.NEUTRAL,"and a reusable event carrying anything else is not");
        // The shape from the field: a reusable message set on a mutable event, whose getMessage() is the event itself.
        org.apache.logging.log4j.core.impl.MutableLogEvent self=new org.apache.logging.log4j.core.impl.MutableLogEvent();
        self.setMessage(org.apache.logging.log4j.message.ReusableMessageFactory.INSTANCE.newMessage(noise,0));
        check(self.getMessage()==self,"fixture: the event is its own message");
        check(filter.filter(self)==Filter.Result.DENY,"an event that is its own message is read, not followed in a circle, and the save message is denied");
        org.apache.logging.log4j.core.impl.MutableLogEvent selfOther=new org.apache.logging.log4j.core.impl.MutableLogEvent();
        selfOther.setMessage(org.apache.logging.log4j.message.ReusableMessageFactory.INSTANCE.newMessage("Thread {} stopped",1));
        check(selfOther.getMessage()==selfOther&&filter.filter(selfOther)==Filter.Result.NEUTRAL,"and other messages in that shape pass (the one that overflowed the stack in the field)");
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
