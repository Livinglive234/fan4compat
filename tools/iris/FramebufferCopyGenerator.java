import org.objectweb.asm.*;

public final class FramebufferCopyGenerator implements Opcodes {
    public static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/iris/client/IrisFramebufferCopyMixin";
        ClassWriter w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/compat/iris_compatibility/IPIrisHelper");
        for(String method:new String[]{"newCopyDepthStencil","copyColor"}) {
            MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$compatible"+method,"(Lnet/minecraft/class_276;Lnet/minecraft/class_276;L"+GenerateAddon.CI+";)V",null,null);
            GenerateAddon.inject(m,method+"(Lnet/minecraft/class_276;Lnet/minecraft/class_276;)V","HEAD",true);
            m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitInsn(method.equals("copyColor")?ICONST_1:ICONST_0);
            m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"iris/FramebufferCopyCompat","fallback","(Ljava/lang/Object;Ljava/lang/Object;Z)Z",false);
            Label nativeCopy=new Label();m.visitJumpInsn(IFEQ,nativeCopy);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,GenerateAddon.CI,"cancel","()V",false);m.visitLabel(nativeCopy);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/iris/client/IrisDepthFormatMixin";
        w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$appleDepthRepresentation","(Z)Z",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true),arr=a.visitArray("method");arr.visit(null,"prepareRendering()V");arr.visitEnd();a.visit("require",1);a.visit("remap",false);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lqouteall/imm_ptl/core/IPMcHelper;isNvidiaVideocard()Z");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"iris/DepthFormatCompat","nvidia","(ZLjava/lang/Object;)Z",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/iris/client/IrisPortalDepthDiagnosticMixin";
        w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer");
        m=w.visitMethod(ACC_PRIVATE,"fan4$observeDepthCopy","(I)I",null,null);
        a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true);
        arr=a.visitArray("method");arr.visit(null,"doMainRenderings(Lorg/joml/Matrix4f;)V");arr.visitEnd();a.visit("require",1);a.visit("remap",false);
        at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lorg/lwjgl/opengl/GL11;glGetError()I");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"iris/PortalDepthDiagnostics","observe","(I)I",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);

    }
}
