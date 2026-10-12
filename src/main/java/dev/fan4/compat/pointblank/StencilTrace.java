package dev.fan4.compat.pointblank;

import java.util.*;
import java.util.function.*;
import dev.fan4.compat.shared.Diagnostics;

/** Short, opt-in chronological trace; only observes native framebuffer/stencil calls. */
public final class StencilTrace {
    public record Sample(String stage,String arguments,Map<String,String> before) {}
    public static final class Probe {
        private final LongSupplier clock;private final Supplier<Map<String,String>> reader;private final Supplier<String> caller;private final Consumer<String> log;
        private final Diagnostics.Gate gate;private final Set<String> seen=new HashSet<>();
        private boolean armed;private long deadline;
        public Probe(LongSupplier clock,Supplier<Map<String,String>> reader,Supplier<String> caller,Consumer<String> log){this.clock=clock;this.reader=reader;this.caller=caller;this.log=log;gate=new Diagnostics.Gate(clock,log,0,96);}
        public void arm(){if(!armed){armed=true;deadline=clock.getAsLong()+45_000_000_000L;}}
        public Sample begin(String stage){return begin(stage,"-");}
        public Sample begin(String stage,String arguments){
            if(!armed||!gate.open()||clock.getAsLong()>=deadline)return null;
            Map<String,String> before=gate.read(reader);
            return before==null?null:new Sample(stage,arguments,before);
        }
        public void end(Sample sample,boolean exception){
            if(sample==null||!gate.open())return;
            try{
                Map<String,String> after=new LinkedHashMap<>(reader.get());
                if(!exception&&sample.stage.equals("framebuffer-bind")&&sample.before.equals(after))return;
                String origin=caller.get();
                if(!seen.add(sample.stage+sample.arguments+sample.before+after+exception+origin))return;
                log.accept("event="+gate.count()+" stage="+sample.stage+" arguments="+sample.arguments+" exception="+exception+" before="+sample.before+" after="+after+" caller="+origin);
            }catch(RuntimeException|LinkageError e){gate.fail(e);}
        }
    }
    private static final boolean ENABLED=Diagnostics.render();
    private static final Probe PROBE=new Probe(System::nanoTime,StencilTrace::read,StencilTrace::caller,
        message->dev.fan4.compat.shared.GameLog.info("[Fan4Compat StencilTrace] "+message));
    private StencilTrace(){}
    public static void arm(){if(ENABLED)PROBE.arm();}
    public static Object begin(String stage,Object ignored){if(!ENABLED)return null;try{return PROBE.begin(stage,String.valueOf(ignored));}catch(RuntimeException|LinkageError e){return null;}}
    public static void end(Object token,boolean exception){try{if(token instanceof Sample sample)PROBE.end(sample,exception);}catch(RuntimeException|LinkageError ignored){}}
    private static Map<String,String> read(){return RenderDiagnostics.stencilSnapshot();}
    private static String caller(){return StackWalker.getInstance().walk(frames->frames.filter(f->!f.getClassName().equals(StencilTrace.class.getName())).limit(18).map(StackWalker.StackFrame::toString).reduce((a,b)->a+" <- "+b).orElse("unknown"));}
}
