import org.objectweb.asm.*;

public final class EurekaGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT;
    static void debugMixin()throws Exception {
        String name=ROOT+"mixin/eureka/client/EurekaDebugMixin";ClassWriter w=GenerateAddon.writer(name,"org/valkyrienskies/eureka/fabric/EurekaModFabric$Client");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE | ACC_STATIC,"fan4$optionalShipDebug","(Lorg/slf4j/Logger;Ljava/lang/String;[Ljava/lang/Object;)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"lambda$onInitializeClient$3(Lnet/minecraft/class_310;)V");arr.visitEnd();a.visit("remap",false);a.visit("require",2);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitMethodInsn(INVOKESTATIC,ROOT+"eureka/CompatSettings","eurekaDebugEnabled","()Z",false);
        Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);
        m.visitMethodInsn(INVOKEINTERFACE,"org/slf4j/Logger","info","(Ljava/lang/String;[Ljava/lang/Object;)V",true);m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
