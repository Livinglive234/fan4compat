import org.objectweb.asm.*;

public final class JadeGenerator implements Opcodes {
    public static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/jade/client/JadeShipRaycastMixin";
        ClassWriter w=GenerateAddon.writer(name,"snownee/jade/overlay/RayTracing");
        String selector="rayTrace(Lnet/minecraft/class_1297;DD)Lnet/minecraft/class_239;";
        // Normalize only vectors consumed by Jade, not the hit result/block position itself.
        String[][] points={{"class_239","method_17784","()"},{"class_3965","method_17784","()"},{"class_3966","method_17784","()"},{"class_1297","method_5836","(F)"},{"class_4184","method_19326","()"}};
        for(int i=0;i<points.length;i++) {
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$worldRayPosition"+i,"(Lnet/minecraft/class_243;Lnet/minecraft/class_1297;DD)Lnet/minecraft/class_243;",null,null);
            AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true),arr=a.visitArray("method");arr.visit(null,selector);arr.visitEnd();a.visit("require",1);a.visit("remap",false);
            AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lnet/minecraft/"+points[i][0]+";"+points[i][1]+points[i][2]+"Lnet/minecraft/class_243;");at.visit("remap",false);at.visitEnd();a.visitEnd();
            m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"jade/ShipRaycastCompat","worldPosition","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"net/minecraft/class_243");m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
}
