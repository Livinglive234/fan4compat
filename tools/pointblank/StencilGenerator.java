import org.objectweb.asm.*;

/** Exact invocation redirects, including an interface mixin for native static scope lambdas. */
public final class StencilGenerator implements Opcodes {
    static final String PROVIDER="com/vicmatskiv/pointblank/client/render/RenderTypeProvider",HELPER=GenerateAddon.ROOT+"pointblank/StencilCompat";
    static final String[] SCOPES=java.util.stream.IntStream.rangeClosed(3,7).mapToObj(i->"lambda$static$"+i+"(Lcom/vicmatskiv/pointblank/client/render/RenderTypeKey;)V").toArray(String[]::new);
    static void redirect(ClassWriter w,boolean statik,String[] methods,String owner,String target,String desc,String helper)throws Exception {
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|(statik?ACC_STATIC:0),"fan4$"+target,desc,null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true),arr=a.visitArray("method");
        for(String selector:methods)arr.visit(null,selector);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+owner+";"+target+desc);at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();int local=statik?0:1;for(Type arg:Type.getArgumentTypes(desc)){m.visitVarInsn(arg.getOpcode(ILOAD),local);local+=arg.getSize();}
        m.visitMethodInsn(INVOKESTATIC,HELPER,helper,desc,false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    public static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/pointblank/client/GunStencilMixin";
        ClassWriter w=GenerateAddon.writer(name,PROVIDER);
        // Override the class header: RenderTypeProvider is an interface, not a class.
        w.visit(V17,ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT,name,null,"java/lang/Object",null);
        String gl="org/lwjgl/opengl/GL11",system="com/mojang/blaze3d/systems/RenderSystem";
        redirect(w,true,SCOPES,gl,"glStencilFunc","(III)V","function");
        redirect(w,true,SCOPES,gl,"glStencilMask","(I)V","mask");
        redirect(w,true,SCOPES,gl,"glEnable","(I)V","enable");
        redirect(w,true,SCOPES,gl,"glDisable","(I)V","disable");
        redirect(w,true,SCOPES,system,"stencilFunc","(III)V","function");
        redirect(w,true,SCOPES,system,"stencilMask","(I)V","mask");
        redirect(w,true,SCOPES,system,"stencilOp","(III)V","operation");
        redirect(w,true,SCOPES,system,"clearStencil","(I)V","clearValue");
        redirect(w,true,SCOPES,system,"clear","(IZ)V","clear");
        GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/pointblank/client/GunStencilClearMixin";w=GenerateAddon.writer(name,"com/vicmatskiv/pointblank/client/ClientSystem");
        redirect(w,false,new String[]{"clearStencil()V"},system,"clearStencil","(I)V","clearValue");
        redirect(w,false,new String[]{"clearStencil()V"},system,"clear","(IZ)V","clear");
        GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/pointblank/client/PortalStencilCacheMixin";
        w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/render/renderer/RendererUsingStencil");
        String[] portalMethods={"myFinishRendering()V","renderPortalViewAreaToStencil(Lqouteall/imm_ptl/core/portal/Portal;Lorg/joml/Matrix4f;)V","clampStencilValue(I)V","setStencilLimitation(I)V"};
        // Static redirects may target both static and instance methods; never suppress IP's own state.
        redirect(w,true,portalMethods,gl,"glStencilFunc","(III)V","portalFunction");
        redirect(w,true,portalMethods,gl,"glStencilOp","(III)V","portalOperation");
        redirect(w,true,portalMethods,gl,"glStencilMask","(I)V","portalMask");
        GenerateAddon.save(name,w);
    }
}
