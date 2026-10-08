import org.objectweb.asm.*;

public final class FreecamGenerator implements Opcodes {
    static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamUnloadedTickMixin";
        ClassWriter w=GenerateAddon.writer(name,"net/minecraft/class_746");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$allowCameraTick","(Z)Z",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true);
        AnnotationVisitor selectors=a.visitArray("method");selectors.visit(null,"method_5773()V");selectors.visitEnd();
        a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value","INVOKE");at.visit("target","Lnet/minecraft/class_1937;method_33598(II)Z");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"freecam/FreecamTickCompat","allowTick","(ZLjava/lang/Object;)Z",false);
        m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
