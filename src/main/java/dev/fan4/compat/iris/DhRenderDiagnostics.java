package dev.fan4.compat.iris;

import java.util.*;
import java.util.function.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Opt-in long-running DH observations; never binds objects or reads the GL error queue. */
public final class DhRenderDiagnostics {
    public record Sample(String stage,Map<String,String> before){}
    public static final class Probe {
        private final LongSupplier clock;private final Supplier<Map<String,String>> reader;private final Consumer<String> log;
        private final Map<String,Long> next=new HashMap<>();private int reports;private boolean failed;
        public Probe(LongSupplier clock,Supplier<Map<String,String>> reader,Consumer<String> log){this.clock=clock;this.reader=reader;this.log=log;}
        public Sample begin(String stage){
            if(failed||reports>=600)return null;long now=clock.getAsLong();if(now<next.getOrDefault(stage,Long.MIN_VALUE))return null;
            next.put(stage,now+5_000_000_000L);
            try{return new Sample(stage,new LinkedHashMap<>(reader.get()));}catch(RuntimeException|LinkageError e){failure(e);return null;}
        }
        public void end(Sample token,boolean exception){if(token==null||failed||reports>=600)return;try{log.accept("sample="+(++reports)+" stage="+token.stage+" exception="+exception+" before="+token.before+" after="+reader.get());}catch(RuntimeException|LinkageError e){failure(e);}}
        private void failure(Throwable e){failed=true;log.accept("Diagnostics disabled after observation failure: "+e.getClass().getSimpleName()+": "+e.getMessage());}
    }
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("fan4compat.dhRenderDiagnostics",System.getProperty("fan4compat.renderDiagnostics","false")));
    private static final System.Logger LOGGER=System.getLogger("Fan4Compat DHRenderDiag");
    private static final Probe PROBE=new Probe(System::nanoTime,DhRenderDiagnostics::snapshot,m->LOGGER.log(System.Logger.Level.INFO,"[Fan4Compat DHRenderDiag] "+m));
    private DhRenderDiagnostics(){}
    public static Object begin(String stage,Object ignored){if(!ENABLED)return null;try{return PROBE.begin(stage);}catch(RuntimeException|LinkageError e){return null;}}
    public static void end(Object token,boolean exception){try{if(token instanceof Sample s)PROBE.end(s,exception);}catch(RuntimeException|LinkageError ignored){}}
    public static void depth(String getter,int texture){Object token=begin(getter+"="+texture,null);end(token,false);}
    private static int integer(int key){return (Integer)exact("org.lwjgl.opengl.GL11","glGetInteger",new String[]{"int"},key);}
    private static Map<String,String> snapshot(){
        Map<String,String> s=new LinkedHashMap<>();String[] names={"drawFbo","readFbo","program","depthFunc","activeTexture","boundTexture2D","stencilFunc","stencilRef"};int[] keys={36006,36010,35725,2932,34016,32873,2962,2967};
        for(int i=0;i<keys.length;i++)s.put(names[i],String.valueOf(integer(keys[i])));
        for(int cap:new int[]{2929,2960,3089})s.put("enabled."+cap,String.valueOf(exact("org.lwjgl.opengl.GL11","glIsEnabled",new String[]{"int"},cap)));
        s.put("depthWrite",String.valueOf(exact("org.lwjgl.opengl.GL11","glGetBoolean",new String[]{"int"},2930)));
        if(integer(36006)!=0)for(int point:new int[]{36096,36128}){
            int kind=(Integer)exact("org.lwjgl.opengl.GL30","glGetFramebufferAttachmentParameteri",new String[]{"int","int","int"},36009,point,36048);
            s.put("attachment."+point,kind+":"+(kind==0?0:exact("org.lwjgl.opengl.GL30","glGetFramebufferAttachmentParameteri",new String[]{"int","int","int"},36009,point,36049)));
        }
        Object client=call(type("net.minecraft.class_310"),"method_1551"),target=call(client,"method_1522");s.put("mainFbo",String.valueOf(field(target,"field_1476")));s.put("mainDepthTexture",String.valueOf(field(target,"field_1474")));
        Object world=field(client,"field_1687");if(world!=null)s.put("dimension",String.valueOf(call(world,"method_27983")));
        if(present("qouteall.imm_ptl.core.render.context_management.PortalRendering"))s.put("portalLayer",String.valueOf(call(type("qouteall.imm_ptl.core.render.context_management.PortalRendering"),"getPortalLayer")));
        if(present("qouteall.imm_ptl.core.render.GuiPortalRendering"))s.put("guiCamera",String.valueOf(call(type("qouteall.imm_ptl.core.render.GuiPortalRendering"),"isRendering")));
        if(present("net.irisshaders.iris.api.v0.IrisApi"))s.put("shaders",String.valueOf(call(call(type("net.irisshaders.iris.api.v0.IrisApi"),"getInstance"),"isShaderPackInUse")));
        return s;
    }
}
