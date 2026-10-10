package dev.fan4.compat.iris;

import java.util.*;
import java.util.function.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Observes IP's existing error result; never consumes another GL error or changes its fallback. */
public final class PortalDepthDiagnostics {
    public static final class Probe {
        private final Supplier<String> reader;private final Consumer<String> log;
        private boolean success,disabled;private int failures;
        public Probe(Supplier<String> reader,Consumer<String> log){this.reader=reader;this.log=log;}
        public int observe(int error) {
            if(disabled||(error==0?success:failures>=8))return error;
            if(error==0)success=true;else failures++;
            try{log.accept("depthCopy="+(error==0?"OK":"FAILED")+" error="+errorName(error)+" "+reader.get());}
            catch(RuntimeException|LinkageError e){disabled=true;try{log.accept("Diagnostics disabled after snapshot failure: "+e.getClass().getSimpleName()+": "+e.getMessage());}catch(RuntimeException|LinkageError ignored){}}
            return error;
        }
    }
    private static final System.Logger LOGGER=System.getLogger("Fan4Compat PortalDepthDiag");
    private static final Probe PROBE=new Probe(PortalDepthDiagnostics::snapshot,m->LOGGER.log(System.Logger.Level.INFO,"[Fan4Compat PortalDepthDiag] "+m));
    private PortalDepthDiagnostics() {}
    public static int observe(int error){return PROBE.observe(error);}
    public static String errorName(int error){return switch(error){case 0->"GL_NO_ERROR(0)";case 1280->"GL_INVALID_ENUM(1280)";case 1281->"GL_INVALID_VALUE(1281)";case 1282->"GL_INVALID_OPERATION(1282)";case 1285->"GL_OUT_OF_MEMORY(1285)";case 1286->"GL_INVALID_FRAMEBUFFER_OPERATION(1286)";default->"UNKNOWN("+error+")";};}
    private static final String GL11="org.lwjgl.opengl.GL11",GL30="org.lwjgl.opengl.GL30";
    private static int integer(int key){return (Integer)exact(GL11,"glGetInteger",new String[]{"int"},key);}
    private static int attachment(int target,int point,int key){return (Integer)exact(GL30,"glGetFramebufferAttachmentParameteri",new String[]{"int","int","int"},target,point,key);}
    private static String describe(int target,int binding) {
        int framebuffer=integer(binding);
        if(framebuffer==0)return "defaultFramebuffer";
        int status=(Integer)exact(GL30,"glCheckFramebufferStatus",new String[]{"int"},target);
        return "fbo="+framebuffer+",status="+status+",depth={"+describeAttachment(target,36096)+"},stencil={"+describeAttachment(target,36128)+"}";
    }
    private static String describeAttachment(int target,int point) {
        int kind=attachment(target,point,36048);
        if(kind==0)return "none";
        // Attachment queries require no texture/renderbuffer binding changes. Bit sizes and
        // component type distinguish DEPTH24, DEPTH32F and packed stencil representations.
        int id=attachment(target,point,36049),depth=attachment(target,point,33302),stencil=attachment(target,point,33303);
        String component=point==36096?String.valueOf(attachment(target,point,33297)):"stencil";
        return "kind="+kind+",object="+id+",depthBits="+depth+",stencilBits="+stencil+",componentType="+component;
    }
    private static String snapshot() {
        String vendor=String.valueOf(exact(GL11,"glGetString",new String[]{"int"},7936));
        String renderer=String.valueOf(exact(GL11,"glGetString",new String[]{"int"},7937));
        String version=String.valueOf(exact(GL11,"glGetString",new String[]{"int"},7938));
        Object main=call(call(type("net.minecraft.class_310"),"method_1551"),"method_1522");
        Object mode=field(type("qouteall.imm_ptl.core.IPGlobal"),"renderMode");
        return "vendor="+vendor+" gpu="+renderer+" gl="+version+" layer="+call(type("qouteall.imm_ptl.core.render.context_management.PortalRendering"),"getPortalLayer")
            +" maxLayers="+call(type("qouteall.imm_ptl.core.render.context_management.PortalRendering"),"getMaxPortalLayer")+" renderModeBeforeFallback="+mode
            +" separatedStencil="+field(type("qouteall.imm_ptl.core.IPCGlobal"),"useSeparatedStencilFormat")
            +" mainSize="+field(main,"field_1480")+"x"+field(main,"field_1477")
            +" source={"+describe(36008,36010)+"} destination={"+describe(36009,36006)+"}";
    }
}
