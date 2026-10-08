import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Verify portal-only cancellation and the native DH/Iris injection contracts. */
public final class DhPortalRenderSmokeTest implements Opcodes {
    public static class PortalContext {static int depth;public static boolean isRendering(){return depth>0;}}
    public static class Textures {static int allocations,deletions;public static int create(){allocations++;return 91;}public static void delete(int id){if(id!=0)deletions++;}}
    public static class Callback {public boolean cancelled;public Object value;public void cancel(){cancelled=true;}public void setReturnValue(Object v){value=v;cancelled=true;}}
    public static void main(String[] args)throws Exception {
        if(args.length>1)for(int i=1;i<args.length;i++)VerifyAddon.readJar(args[i]);
        for(String path:new String[]{"distanthorizons/client/DhPortalRenderMixin","iris/client/DhPortalShaderMixin"}) {
            ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/"+path+".class"))).accept(n,0);
            if(args.length>1)VerifyAddon.injectionTargets(n);
            for(MethodNode m:n.methods)m.access=(m.access&ACC_STATIC)|ACC_PUBLIC;
            MethodVisitor init=n.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();
            ClassWriter w=new ClassWriter(0);n.accept(new ClassRemapper(w,new Remapper(){public String map(String name){
                if(name.equals("qouteall/imm_ptl/core/render/context_management/PortalRendering"))return "DhPortalRenderSmokeTest$PortalContext";
                if(name.equals("dev/fan4/compat/iris/PortalDepthTexture"))return "DhPortalRenderSmokeTest$Textures";
                if(name.startsWith("org/spongepowered/asm/mixin/injection/callback/CallbackInfo"))return "DhPortalRenderSmokeTest$Callback";
                return name;
            }}));byte[] bytes=w.toByteArray();Class<?> c=new ClassLoader(DhPortalRenderSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();Object receiver=c.getConstructor().newInstance();
            for(int depth:new int[]{0,1,2,0})for(var method:c.getDeclaredMethods())if(method.getName().startsWith("fan4$")&&!method.getName().equals("fan4$releasePortalDepth")) {
                PortalContext.depth=depth;Callback callback=new Callback();
                if(method.getParameterCount()==2)for(boolean deferred:new boolean[]{false,true}) {
                    callback=new Callback();method.invoke(receiver,deferred,callback);check(callback.cancelled==(depth>0));
                } else {method.invoke(receiver,callback);check(callback.cancelled==(depth>0));if(depth>0&&path.startsWith("iris"))check(method.getName().contains("getDepth")?Integer.valueOf(91).equals(callback.value):Boolean.FALSE.equals(callback.value));}
            }
            if(path.startsWith("iris")) {
                check(Textures.allocations==1);
                c.getMethod("fan4$releasePortalDepth",Callback.class).invoke(receiver,new Callback());
                c.getMethod("fan4$releasePortalDepth",Callback.class).invoke(receiver,new Callback());
                check(Textures.deletions==1);
                PortalContext.depth=1;Callback again=new Callback();c.getMethod("fan4$portalgetDepthTex",Callback.class).invoke(receiver,again);
                check(Textures.allocations==2&&Integer.valueOf(91).equals(again.value));
            }
        }
        System.out.println("PASS: opaque/deferred LOD, fade and Iris DH frame guards; nested portals cancel; main view restores; native selectors verified when jars supplied");
    }
    static void check(boolean ok){if(!ok)throw new AssertionError("Portal render isolation");}
}
