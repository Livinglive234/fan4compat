import org.objectweb.asm.*;

public final class MovementDiagnosticsGenerator implements Opcodes {
    static void generate()throws Exception {
        String root=GenerateAddon.ROOT,entity="net/minecraft/class_1297",kind="net/minecraft/class_1313",vec="net/minecraft/class_243",op="com/llamalad7/mixinextras/injector/wrapoperation/Operation",helper=root+"immersiveportals/MovementDiagnostics";
        String name=root+"mixin/immersiveportals/common/MovementDiagnosticMixin";ClassWriter w=GenerateAddon.writer(name,entity);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$observeMovement","(L"+kind+";L"+vec+";L"+op+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true),arr=a.visitArray("method");arr.visit(null,"method_5784(L"+kind+";L"+vec+";)V");arr.visitEnd();a.visit("remap",false);a.visitEnd();m.visitCode();for(int i=0;i<4;i++)m.visitVarInsn(ALOAD,i);m.visitMethodInsn(INVOKESTATIC,helper,"move","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/common/ShipGuardDiagnosticMixin";w=GenerateAddon.writer(name,"org/valkyrienskies/mod/common/util/EntityShipCollisionUtils");
        String cir="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$observeGuard","(L"+entity+";L"+cir+";)V",null,null);GenerateAddon.inject(m,"isCollidingWithUnloadedShips(L"+entity+";)Z","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,cir,"getReturnValueZ","()Z",false);m.visitMethodInsn(INVOKESTATIC,helper,"guard","(Ljava/lang/Object;Z)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/client/MovementTerrainDiagnosticMixin";w=GenerateAddon.writer(name,"net/minecraft/class_310");
        m=w.visitMethod(ACC_PRIVATE,"fan4$observeMissingTerrain","(L"+GenerateAddon.CI+";)V",null,null);GenerateAddon.inject(m,"method_1574()V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,helper,"terrain","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
