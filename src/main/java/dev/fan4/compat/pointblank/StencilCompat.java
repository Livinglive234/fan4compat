package dev.fan4.compat.pointblank;

import java.util.function.BooleanSupplier;
import static dev.fan4.compat.shared.CompatCalls.*;

/** Scope stencil changes must share Minecraft's cache and respect portal-owned masks. */
public final class StencilCompat {
    public interface Backend {
        void function(int function,int reference,int mask);
        void mask(int mask);
        void operation(int fail,int depthFail,int pass);
        void clearValue(int value);
        void clear(int bits,boolean mac);
        void test(int capability,boolean enabled);
    }
    public static final class Policy {
        private final BooleanSupplier portal;private final Backend backend;
        public Policy(BooleanSupplier portal,Backend backend){this.portal=portal;this.backend=backend;}
        public void function(int function,int reference,int mask){if(!portal.getAsBoolean())backend.function(function,reference,mask);}
        public void mask(int mask){if(!portal.getAsBoolean())backend.mask(mask);}
        public void operation(int fail,int depthFail,int pass){if(!portal.getAsBoolean())backend.operation(fail,depthFail,pass);}
        public void clearValue(int value){if(!portal.getAsBoolean())backend.clearValue(value);}
        public void clear(int bits,boolean mac){int allowed=portal.getAsBoolean()?bits&~1024:bits;if(allowed!=0)backend.clear(allowed,mac);}
        public void test(int capability,boolean enabled){if(capability!=2960||!portal.getAsBoolean())backend.test(capability,enabled);}
    }
    private static final String PORTAL="qouteall.imm_ptl.core.render.context_management.PortalRendering";
    private static final boolean HAS_PORTALS=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("immersive_portals")
        .map(mod->mod.getMetadata().getVersion().getFriendlyString().equals(dev.fan4.compat.shared.CompatibilityRules.VERSIONS.get("immersive_portals"))).orElse(false)&&present(PORTAL);
    private static final String SYSTEM="com.mojang.blaze3d.systems.RenderSystem";
    private static final String[] ONE={"int"},THREE={"int","int","int"};
    private static final Policy POLICY=new Policy(()->HAS_PORTALS&&(Boolean)call(type(PORTAL),"isRendering"),new Backend(){
        public void function(int function,int reference,int mask){exact(SYSTEM,"stencilFunc",THREE,function,reference,mask);}
        public void mask(int mask){exact(SYSTEM,"stencilMask",ONE,mask);}
        public void operation(int fail,int depthFail,int pass){exact(SYSTEM,"stencilOp",THREE,fail,depthFail,pass);}
        public void clearValue(int value){exact(SYSTEM,"clearStencil",ONE,value);}
        public void clear(int bits,boolean mac){exact(SYSTEM,"clear",new String[]{"int","boolean"},bits,mac);}
        public void test(int capability,boolean enabled){exact("org.lwjgl.opengl.GL11",enabled?"glEnable":"glDisable",ONE,capability);}
    });
    private StencilCompat() {}
    public static void function(int function,int reference,int mask){POLICY.function(function,reference,mask);}
    public static void mask(int mask){POLICY.mask(mask);}
    public static void operation(int fail,int depthFail,int pass){POLICY.operation(fail,depthFail,pass);}
    public static void clearValue(int value){POLICY.clearValue(value);}
    public static void clear(int bits,boolean mac){POLICY.clear(bits,mac);}
    // IP owns the mask during portal rendering, so its setters always run.
    public static void portalFunction(int function,int reference,int mask){exact(SYSTEM,"stencilFunc",THREE,function,reference,mask);}
    public static void portalMask(int mask){exact(SYSTEM,"stencilMask",ONE,mask);}
    public static void portalOperation(int fail,int depthFail,int pass){exact(SYSTEM,"stencilOp",THREE,fail,depthFail,pass);}
    public static void enable(int capability){POLICY.test(capability,true);}
    public static void disable(int capability){POLICY.test(capability,false);}
}
