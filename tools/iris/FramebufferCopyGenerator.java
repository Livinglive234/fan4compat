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
    }
}
