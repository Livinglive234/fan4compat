import org.objectweb.asm.*;

public final class WingTrailGenerator implements Opcodes {
    static final String TARGET="method_4054(Lnet/minecraft/class_1309;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V";
    static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/elytratrails/client/WingTrailFallbackMixin";
        ClassWriter w=GenerateAddon.writer(name,"net/minecraft/class_922");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$accessoryWingEmitters","(Lnet/minecraft/class_1309;FFLnet/minecraft/class_4587;Lnet/minecraft/class_4597;IL"+GenerateAddon.CI+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true),arr=a.visitArray("method");arr.visit(null,TARGET);arr.visitEnd();a.visit("remap",false);a.visit("require",1);a.visit("allow",1);
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/minecraft/class_4587;method_22909()V");at.visit("ordinal",0);at.visit("remap",false);at.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,4);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"elytratrails/WingTrailCompat","capture","(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
