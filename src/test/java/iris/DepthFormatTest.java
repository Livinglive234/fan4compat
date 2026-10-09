import dev.fan4.compat.iris.DepthFormatCompat;
import net.minecraft.class_310;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.IPCGlobal;
public final class DepthFormatTest {
    public static class Framebuffer {public int deletions;public int method_30278(){return 45;}public void method_1238(){deletions++;}}
    public static class Layer {public Framebuffer fb=new Framebuffer();}
    public static class Renderer {public Layer[] deferredFbs={new Layer(),new Layer()};}
    public static void main(String[] args){
        class_310.INSTANCE.framebuffer=new Framebuffer();Renderer renderer=new Renderer();Framebuffer first=renderer.deferredFbs[0].fb;
        check(DepthFormatCompat.nvidia(false,renderer),"Apple depth24 selects packed depth24/stencil");
        check(GL11.binding==77&&first.deletions==1&&renderer.deferredFbs[0].fb==null,"format change cleans old layers and restores binding");
        IPCGlobal.useSeparatedStencilFormat=false;renderer=new Renderer();first=renderer.deferredFbs[0].fb;
        check(DepthFormatCompat.nvidia(false,renderer)&&first.deletions==0,"matching format retains layers");
        GL11.depthFormat=36012;check(!DepthFormatCompat.nvidia(true,renderer)&&first.deletions==1,"depth32f selects float stencil and reloads layers");
        GL11.vendor="NVIDIA";int queries=GL11.queries;check(DepthFormatCompat.nvidia(true,null)&&GL11.queries==queries,"non-Apple native path");
        GL11.vendor="Apple";GL11.depthFormat=33189;check(!DepthFormatCompat.nvidia(false,null),"unknown depth representation leaves native choice");
        GL11.depthFormat=6402;GL11.depthBits=24;renderer=new Renderer();IPCGlobal.useSeparatedStencilFormat=true;
        check(DepthFormatCompat.nvidia(false,renderer),"unsized depth uses actual component size");
        check(DepthFormatCompat.resolvedFormat(6402,32,5126)==36012,"unsized float depth32");
        check(DepthFormatCompat.resolvedFormat(6402,16,35863)==6402,"unknown unsized representation preserved");
        renderer=new Renderer();first=renderer.deferredFbs[0].fb;qouteall.imm_ptl.core.render.context_management.PortalRendering.maxLayer=3;
        check(DepthFormatCompat.nvidia(false,renderer)&&first.deletions==0,"native layer-count recreation retains nonnull old buffers");
        GL11.queryFail=true;try{DepthFormatCompat.nvidia(false,null);throw new AssertionError();}catch(IllegalStateException expected){check(GL11.binding==77,"failed query restores texture binding");}
        System.out.println("PASS: Apple depth24/float32 choice, non-Apple/unknown preservation, layer recreation on changes and texture binding restoration (test GL backend)");
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
