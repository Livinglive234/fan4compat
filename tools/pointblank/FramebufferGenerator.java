import org.objectweb.asm.*;

public final class FramebufferGenerator implements Opcodes {
    public static void generate()throws Exception {
        for(String[] target:new String[][]{
            {"GunFramebufferMixin","com/vicmatskiv/pointblank/client/ClientSystem","preparePipFallback(Lnet/minecraft/class_1799;)V"},
            {"GunAuxFramebufferMixin","com/vicmatskiv/pointblank/client/render/AuxLevelRenderer","renderToTarget(Lnet/minecraft/class_9779;F)V"}}) {
            String name=GenerateAddon.ROOT+"mixin/pointblank/client/"+target[0];ClassWriter w=GenerateAddon.writer(name,target[1]);
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$preserveStencilFramebuffer","(Lcom/vicmatskiv/pointblank/client/render/RenderTargetExt;)V",null,null);
            AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true),arr=a.visitArray("method");arr.visit(null,target[2]);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
            AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lcom/vicmatskiv/pointblank/client/render/RenderTargetExt;enablePointblankStencil()V");at.visit("remap",false);at.visitEnd();a.visitEnd();
            m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"pointblank/FramebufferCompat","enable","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        }
    }
}
