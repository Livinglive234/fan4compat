import org.objectweb.asm.*;

public final class PortalSoundGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",OP="com/llamalad7/mixinextras/injector/wrapoperation/Operation",H=ROOT+"soundphysics/PortalSoundCompat";
    static void generate()throws Exception {
        String name=ROOT+"mixin/soundphysics/client/PortalSoundSnapshotMixin",owner="com/sonicether/soundphysics/SoundPhysics",args="IDDDLnet/minecraft/class_3419;Lnet/minecraft/class_2960;Z";
        ClassWriter w=GenerateAddon.writer(name,owner);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$evaluateSnapshot","("+args+"L"+OP+";)Lnet/minecraft/class_243;",null,null);
        AnnotationVisitor an=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true);AnnotationVisitor methods=an.visitArray("method");methods.visit(null,"evaluateEnvironment("+args+")Lnet/minecraft/class_243;");methods.visitEnd();an.visit("remap",false);an.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,10);m.visitVarInsn(ILOAD,0);m.visitVarInsn(DLOAD,1);m.visitVarInsn(DLOAD,3);m.visitVarInsn(DLOAD,5);m.visitVarInsn(ALOAD,7);m.visitVarInsn(ALOAD,8);m.visitVarInsn(ILOAD,9);
        m.visitMethodInsn(INVOKESTATIC,H,"evaluate","(Ljava/lang/Object;IDDDLjava/lang/Object;Ljava/lang/Object;Z)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"net/minecraft/class_243");m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$stableProxy","(L"+CIR+";)V",null,null);GenerateAddon.inject(m,"getLevelProxy()Lcom/sonicether/soundphysics/world/ClientLevelProxy;","HEAD",true);
        m.visitCode();m.visitMethodInsn(INVOKESTATIC,H,"scoped","()Z",false);Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,H,"snapshot","()Ljava/lang/Object;",false);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/soundphysics/client/PortalSoundRayMixin";w=GenerateAddon.writer(name,"com/sonicether/soundphysics/utils/RaycastUtils");args="Lnet/minecraft/class_1922;Lnet/minecraft/class_243;Lnet/minecraft/class_243;Lnet/minecraft/class_2338;";
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$cachedRay","("+args+"L"+OP+";)Lnet/minecraft/class_3965;",null,null);
        an=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true);methods=an.visitArray("method");methods.visit(null,"rayCast("+args+")Lnet/minecraft/class_3965;");methods.visitEnd();an.visit("remap",false);an.visitEnd();m.visitCode();for(int i=0;i<5;i++)m.visitVarInsn(ALOAD,i);m.visitMethodInsn(INVOKESTATIC,H,"rayCast","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"net/minecraft/class_3965");m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
