package dev.fan4.compat.iris;

import java.util.*;
import java.util.function.*;
import dev.fan4.compat.shared.Diagnostics;
import static dev.fan4.compat.shared.Diagnostics.*;

/** Opt-in long-running DH observations; never binds objects or reads the GL error queue. */
public final class DhRenderDiagnostics {
    public record Sample(String stage,Map<String,String> before){}
    public static final class Probe {
        private final Supplier<Map<String,String>> reader;private final Consumer<String> log;private final Diagnostics.Gate gate;
        public Probe(LongSupplier clock,Supplier<Map<String,String>> reader,Consumer<String> log){this.reader=reader;this.log=log;gate=new Diagnostics.Gate(clock,log,5_000_000_000L,600);}
        public Sample begin(String stage){
            if(!gate.open()||!gate.due(stage))return null;
            Map<String,String> before=gate.read(reader);
            return before==null?null:new Sample(stage,before);
        }
        public void end(Sample token,boolean exception){
            if(token==null||!gate.open())return;
            try{log.accept("sample="+gate.count()+" stage="+token.stage+" exception="+exception+" before="+token.before+" after="+reader.get());}catch(RuntimeException|LinkageError e){gate.fail(e);}
        }
    }
    private static final boolean ENABLED=Diagnostics.dh();
    private static final Probe PROBE=new Probe(System::nanoTime,DhRenderDiagnostics::snapshot,m->dev.fan4.compat.shared.GameLog.info("[Fan4Compat DHRenderDiag] "+m));
    private DhRenderDiagnostics(){}
    public static Object begin(String stage,Object ignored){if(!ENABLED)return null;try{return PROBE.begin(stage);}catch(RuntimeException|LinkageError e){return null;}}
    public static void end(Object token,boolean exception){try{if(token instanceof Sample s)PROBE.end(s,exception);}catch(RuntimeException|LinkageError ignored){}}
    public static void depth(String getter,int texture){Object token=begin(getter+"="+texture,null);end(token,false);}
    private static Map<String,String> snapshot(){
        Map<String,String> s=new LinkedHashMap<>();String[] names={"drawFbo","readFbo","program","depthFunc","activeTexture","boundTexture2D","stencilFunc","stencilRef"};int[] keys={36006,36010,35725,2932,34016,32873,2962,2967};
        for(int i=0;i<keys.length;i++)s.put(names[i],String.valueOf(integer(keys[i])));
        for(int cap:new int[]{2929,2960,3089})s.put("enabled."+cap,String.valueOf(enabled(cap)));
        s.put("depthWrite",String.valueOf(dev.fan4.compat.shared.CompatCalls.exact("org.lwjgl.opengl.GL11","glGetBoolean",new String[]{"int"},2930)));
        if(integer(36006)!=0)for(int point:new int[]{36096,36128})s.put("attachment."+point,attachmentSummary(point));
        context(s);
        return s;
    }
}
