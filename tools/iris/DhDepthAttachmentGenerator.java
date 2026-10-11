import org.objectweb.asm.*;
public final class DhDepthAttachmentGenerator implements Opcodes {
    public static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/iris/client/DhDepthAttachmentMixin",helper=GenerateAddon.ROOT+"iris/DhDepthAttachmentCompat";
        ClassWriter w=GenerateAddon.writer(name,"net/irisshaders/iris/targets/RenderTargets");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$rememberDhFramebuffer","(Lcom/google/common/collect/ImmutableSet;[ILorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",null,null);
        GenerateAddon.inject(m,"createDHFramebuffer(Lcom/google/common/collect/ImmutableSet;[I)Lnet/irisshaders/iris/gl/framebuffer/GlFramebuffer;","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","getReturnValue","()Ljava/lang/Object;",false);m.visitMethodInsn(INVOKESTATIC,helper,"remember","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$preserveDhDepth","(Lnet/irisshaders/iris/gl/framebuffer/GlFramebuffer;I)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true),arr=a.visitArray("method");
        arr.visit(null,"resizeIfNeeded(IIIILnet/irisshaders/iris/gl/texture/DepthBufferFormat;Lnet/irisshaders/iris/shaderpack/properties/PackDirectives;)Z");arr.visitEnd();a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/irisshaders/iris/gl/framebuffer/GlFramebuffer;addDepthAttachment(I)V");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ILOAD,2);m.visitMethodInsn(INVOKESTATIC,helper,"refresh","(Ljava/lang/Object;I)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
