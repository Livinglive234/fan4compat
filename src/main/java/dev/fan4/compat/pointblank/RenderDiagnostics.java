package dev.fan4.compat.pointblank;

import java.nio.*;
import java.util.*;
import java.util.function.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Temporary read-only snapshots; never repairs state or consumes the GL error queue. */
public final class RenderDiagnostics {
    private static final boolean ENABLED=Boolean.parseBoolean(System.getProperty("fan4compat.renderDiagnostics","false"));
    public record Sample(String stage,String context,Map<String,String> before) {}
    public static final class Probe {
        private final LongSupplier clock;private final Supplier<Map<String,String>> reader;private final Consumer<String> log;
        private final Map<String,Long> next=new HashMap<>();private final Set<String> seen=new HashSet<>();
        private int reports;private boolean failed;
        public Probe(LongSupplier clock,Supplier<Map<String,String>> reader,Consumer<String> log){this.clock=clock;this.reader=reader;this.log=log;}
        public Sample begin(String stage,String context) {
            if(failed||reports>=96)return null;
            long now=clock.getAsLong();if(now<next.getOrDefault(stage,Long.MIN_VALUE))return null;
            next.put(stage,now+1_000_000_000L);
            try{return new Sample(stage,context,new LinkedHashMap<>(reader.get()));}
            catch(RuntimeException|LinkageError e){failure(e);return null;}
        }
        public void end(Sample sample,boolean exception) {
            if(sample==null||failed||reports>=96)return;
            try {
                Map<String,String> after=new LinkedHashMap<>(reader.get());
                String fingerprint=sample.stage+sample.context+sample.before+after+exception;
                if(!seen.add(fingerprint))return;
                Map<String,String> changes=new LinkedHashMap<>();
                for(var entry:after.entrySet())if(!Objects.equals(sample.before.get(entry.getKey()),entry.getValue()))changes.put(entry.getKey(),sample.before.get(entry.getKey())+" -> "+entry.getValue());
                log.accept("stage="+sample.stage+" item="+sample.context+" exception="+exception+" before="+sample.before+" after="+after+" changes="+changes+" cacheMismatchBefore="+mismatches(sample.before)+" cacheMismatchAfter="+mismatches(after));
                if(++reports==96)log.accept("Report limit reached; restart Minecraft for another diagnostic session.");
            } catch(RuntimeException|LinkageError e){failure(e);}
        }
        private void failure(Throwable e){failed=true;log.accept("Diagnostics disabled after snapshot failure: "+e.getClass().getSimpleName()+": "+e.getMessage());}
    }
    public static Map<String,String> mismatches(Map<String,String> values) {
        Map<String,String> result=new LinkedHashMap<>();
        for(String name:new String[]{"depthWrite","stencilFunc","stencilRef","stencilReadMask","stencilWriteMask","colorWrite"}) {
            String actual=values.get(name),cached=values.get("cached."+name);
            if(actual!=null&&cached!=null&&!actual.equals(cached))result.put(name,"GL="+actual+", cached="+cached);
        }
        return result;
    }
    private static final System.Logger LOGGER=System.getLogger("Fan4Compat RenderDiag");
    private static final Probe PROBE=new Probe(System::nanoTime,RenderDiagnostics::snapshot,message->LOGGER.log(System.Logger.Level.INFO,"[Fan4Compat RenderDiag] "+message));
    private RenderDiagnostics() {}
    public static Object begin(String stage,Object item) {
        if(!ENABLED)return null;
        try{return PROBE.begin(stage,item==null?"-":String.valueOf(item));}
        catch(RuntimeException|LinkageError e){return null;}
    }
    public static void end(Object token,boolean exception){try{if(token instanceof Sample sample)PROBE.end(sample,exception);}catch(RuntimeException|LinkageError ignored){}}
    private static final String GL="org.lwjgl.opengl.GL11";
    private static int integer(int key){return (Integer)exact(GL,"glGetInteger",new String[]{"int"},key);}
    private static boolean enabled(int key){return (Boolean)exact(GL,"glIsEnabled",new String[]{"int"},key);}
    private static int attachment(int point,int key){return (Integer)exact("org.lwjgl.opengl.GL30","glGetFramebufferAttachmentParameteri",new String[]{"int","int","int"},36009,point,key);}
    private static Map<String,String> snapshot() {
        Map<String,String> state=new LinkedHashMap<>();
        String[] names={"drawFbo","readFbo","program","depthFunc","stencilFunc","stencilRef","stencilReadMask","stencilWriteMask","stencilFail","stencilDepthFail","stencilDepthPass","backStencilFunc","backStencilRef","backStencilReadMask","backStencilWriteMask"};
        int[] keys={36006,36010,35725,2932,2962,2967,2963,2968,2964,2965,2966,34816,36003,36004,36005};
        for(int i=0;i<keys.length;i++)state.put(names[i],String.valueOf(integer(keys[i])));
        state.put("depthWrite",String.valueOf((Boolean)exact(GL,"glGetBoolean",new String[]{"int"},2930)));
        String[] toggles={"depthTest","stencilTest","blend","cull","scissor"};int[] caps={2929,2960,3042,2884,3089};
        for(int i=0;i<caps.length;i++)state.put(toggles[i],String.valueOf(enabled(caps[i])));
        ByteBuffer color=ByteBuffer.allocateDirect(4);exact(GL,"glGetBooleanv",new String[]{"int","java.nio.ByteBuffer"},3107,color);
        StringJoiner colors=new StringJoiner(",");for(int i=0;i<4;i++)colors.add(String.valueOf(color.get(i)!=0));state.put("colorWrite",colors.toString());
        IntBuffer viewport=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asIntBuffer();exact(GL,"glGetIntegerv",new String[]{"int","java.nio.IntBuffer"},2978,viewport);
        state.put("viewport",viewport.get(0)+","+viewport.get(1)+","+viewport.get(2)+","+viewport.get(3));
        if(integer(36006)!=0) {
            state.put("fboStatus",String.valueOf(exact("org.lwjgl.opengl.GL30","glCheckFramebufferStatus",new String[]{"int"},36009)));
            for(int point:new int[]{36096,36128}) {
                int kind=attachment(point,36048);state.put(point==36096?"depthAttachment":"stencilAttachment",kind+":"+(kind==0?0:attachment(point,36049)));
            }
        }
        Object cache=type("com.mojang.blaze3d.platform.GlStateManager"),stencil=field(cache,"STENCIL"),function=field(stencil,"field_5149");
        state.put("cached.stencilFunc",String.valueOf(field(function,"field_5148")));
        state.put("cached.stencilRef",String.valueOf(field(function,"field_16203")));
        state.put("cached.stencilReadMask",String.valueOf(field(function,"field_5147")));
        state.put("cached.stencilWriteMask",String.valueOf(field(stencil,"field_5153")));
        state.put("cached.depthWrite",String.valueOf(field(field(cache,"DEPTH"),"field_5076")));
        Object mask=field(cache,"COLOR_MASK");colors=new StringJoiner(",");for(String name:new String[]{"field_5063","field_5062","field_5061","field_5060"})colors.add(String.valueOf(field(mask,name)));state.put("cached.colorWrite",colors.toString());
        Object client=call(type("net.minecraft.class_310"),"method_1551"),target=call(client,"method_1522");
        state.put("mainFbo",String.valueOf(field(target,"field_1476")));state.put("mainDepthTex",String.valueOf(field(target,"field_1474")));
        Object config=type("com.vicmatskiv.pointblank.Config");for(String name:new String[]{"pipScopesEnabled","pipFallbackModDetected","customShadersEnabled"})state.put(name,String.valueOf(field(config,name)));
        if(present("net.irisshaders.iris.api.v0.IrisApi"))state.put("shaders",String.valueOf(call(call(type("net.irisshaders.iris.api.v0.IrisApi"),"getInstance"),"isShaderPackInUse")));else state.put("shaders","Iris absent");
        return state;
    }
}
