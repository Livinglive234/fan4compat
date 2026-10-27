package dev.fan4.compat.shared;

import java.util.*;
import java.util.function.*;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Shared plumbing for the opt-in, read-only render diagnostics: switches, sampling gate and GL queries. */
public final class Diagnostics {
    private Diagnostics() {}
    /** A JVM argument wins; otherwise the player's saved setting (config screen or properties file). */
    private static boolean flag(String property,String feature){
        String override=System.getProperty(property);
        return override!=null?Boolean.parseBoolean(override):Features.enabled(feature);
    }
    public static boolean render(){return flag("fan4compat.renderDiagnostics","renderDiagnostics");}
    /** DH sampling also follows the general switch unless its own JVM argument is given. */
    public static boolean dh(){
        String override=System.getProperty("fan4compat.dhRenderDiagnostics");
        return override!=null?Boolean.parseBoolean(override):render()||Features.enabled("dhRenderDiagnostics");
    }
    public static boolean xaero(){return flag("fan4compat.xaeroDiagnostics","xaeroDiagnostics");}
    public static boolean spatialGui(){return flag("fan4compat.spatialGuiDiagnostics","spatialGuiDiagnostics");}
    public static boolean portalDepth(){return flag("fan4compat.portalDepthDiagnostics","portalDepthDiagnostics");}

    /** Per-stage rate limit, report cap and failure latch; one failure silences the whole probe. */
    public static final class Gate {
        private final LongSupplier clock;private final Consumer<String> log;private final long interval;private final int limit;
        private final Map<String,Long> next=new HashMap<>();private int reports;private boolean failed;
        public Gate(LongSupplier clock,Consumer<String> log,long intervalNanos,int limit){this.clock=clock;this.log=log;this.interval=intervalNanos;this.limit=limit;}
        public boolean open(){return !failed&&reports<limit;}
        /** True at most once per interval for each stage. */
        public boolean due(String stage) {
            long now=clock.getAsLong();if(now<next.getOrDefault(stage,Long.MIN_VALUE))return false;
            next.put(stage,now+interval);return true;
        }
        public int count(){return ++reports;}
        public boolean reachedLimit(){return reports==limit;}
        public void fail(Throwable e){failed=true;log.accept("Diagnostics disabled after observation failure: "+e.getClass().getSimpleName()+": "+e.getMessage());}
        /** A private copy of one reader snapshot, or null after latching a failure. */
        public Map<String,String> read(Supplier<Map<String,String>> reader) {
            try{return new LinkedHashMap<>(reader.get());}
            catch(RuntimeException|LinkageError e){fail(e);return null;}
        }
    }

    private static final String GL11="org.lwjgl.opengl.GL11",GL30="org.lwjgl.opengl.GL30",
        PORTALS="qouteall.imm_ptl.core.render.context_management.PortalRendering",GUI="qouteall.imm_ptl.core.render.GuiPortalRendering",
        IRIS="net.irisshaders.iris.api.v0.IrisApi";
    private static final String[] ONE={"int"},THREE={"int","int","int"};
    /** Probed on first render use, not when the mixin plugin reads the switches. */
    private static final class Has {static final boolean PORTALS_=present(PORTALS),GUI_=present(GUI),IRIS_=present(IRIS);}
    public static int integer(int key){return (Integer)exact(GL11,"glGetInteger",ONE,key);}
    public static boolean enabled(int key){return (Boolean)exact(GL11,"glIsEnabled",ONE,key);}
    public static int attachment(int target,int point,int key){return (Integer)exact(GL30,"glGetFramebufferAttachmentParameteri",THREE,target,point,key);}
    /** "kind:object" of a bound draw-framebuffer attachment, or "0:0" when empty. */
    public static String attachmentSummary(int point) {
        int kind=attachment(36009,point,36048);
        return kind+":"+(kind==0?0:attachment(36009,point,36049));
    }
    public static int portalLayer(){return Has.PORTALS_?(Integer)call(type(PORTALS),"getPortalLayer"):0;}
    public static boolean guiCamera(){return Has.GUI_&&(Boolean)call(type(GUI),"isRendering");}
    /** Main target, dimension, portal/camera context and shader state shared by every diagnostic. */
    public static void context(Map<String,String> into) {
        Object client=call(type("net.minecraft.class_310"),"method_1551"),target=call(client,"method_1522");
        into.put("mainFbo",String.valueOf(field(target,"field_1476")));into.put("mainDepthTex",String.valueOf(field(target,"field_1474")));
        Object world=field(client,"field_1687");if(world!=null)into.put("dimension",String.valueOf(call(world,"method_27983")));
        into.put("portalLayer",String.valueOf(portalLayer()));into.put("guiCamera",String.valueOf(guiCamera()));
        into.put("shaders",Has.IRIS_?String.valueOf(call(call(type(IRIS),"getInstance"),"isShaderPackInUse")):"Iris absent");
    }
}
