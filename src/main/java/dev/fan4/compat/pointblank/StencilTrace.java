package dev.fan4.compat.pointblank;

import java.util.*;
import java.util.function.*;

/** Short, opt-in chronological trace; only observes native framebuffer/stencil calls. */
public final class StencilTrace {
    public record Sample(String stage,String arguments,Map<String,String> before) {}
    public static final class Probe {
        private final LongSupplier clock;private final Supplier<Map<String,String>> reader;private final Supplier<String> caller;private final Consumer<String> log;
        private final Set<String> seen=new HashSet<>();
        private boolean armed,failed;private long deadline;private int reports;
        public Probe(LongSupplier clock,Supplier<Map<String,String>> reader,Supplier<String> caller,Consumer<String> log){this.clock=clock;this.reader=reader;this.caller=caller;this.log=log;}
        public void arm(){if(!armed){armed=true;deadline=clock.getAsLong()+45_000_000_000L;}}
        public Sample begin(String stage){return begin(stage,"-");}
        public Sample begin(String stage,String arguments){
            if(!armed||failed||reports>=96||clock.getAsLong()>=deadline)return null;
            try{return new Sample(stage,arguments,new LinkedHashMap<>(reader.get()));}
            catch(RuntimeException|LinkageError e){failure(e);return null;}
        }
        public void end(Sample sample,boolean exception){
            if(sample==null||failed||reports>=96)return;
            try{
                Map<String,String> after=new LinkedHashMap<>(reader.get());
                if(!exception&&sample.stage.equals("framebuffer-bind")&&sample.before.equals(after))return;
                String origin=caller.get();
                if(!seen.add(sample.stage+sample.arguments+sample.before+after+exception+origin))return;
                log.accept("event="+(++reports)+" stage="+sample.stage+" arguments="+sample.arguments+" exception="+exception+" before="+sample.before+" after="+after+" caller="+origin);
            }catch(RuntimeException|LinkageError e){failure(e);}
        }
        private void failure(Throwable e){failed=true;log.accept("Trace disabled after observation failure: "+e.getClass().getSimpleName());}
    }
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("fan4compat.renderDiagnostics","false"));
    private static final System.Logger LOGGER=System.getLogger("Fan4Compat StencilTrace");
    private static final Probe PROBE=new Probe(System::nanoTime,StencilTrace::read,StencilTrace::caller,
        message->LOGGER.log(System.Logger.Level.INFO,"[Fan4Compat StencilTrace] "+message));
    private StencilTrace(){}
    public static void arm(){if(ENABLED)PROBE.arm();}
    public static Object begin(String stage,Object ignored){if(!ENABLED)return null;try{return PROBE.begin(stage,String.valueOf(ignored));}catch(RuntimeException|LinkageError e){return null;}}
    public static void end(Object token,boolean exception){try{if(token instanceof Sample sample)PROBE.end(sample,exception);}catch(RuntimeException|LinkageError ignored){}}
    private static Map<String,String> read(){return RenderDiagnostics.stencilSnapshot();}
    private static String caller(){return StackWalker.getInstance().walk(frames->frames.filter(f->!f.getClassName().equals(StencilTrace.class.getName())).limit(18).map(StackWalker.StackFrame::toString).reduce((a,b)->a+" <- "+b).orElse("unknown"));}
}
