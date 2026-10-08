import org.objectweb.asm.*;

/** Guard stale animation callbacks without altering valid gun animation handling. */
public final class PointBlankGenerator implements Opcodes {
    static void generate() throws Exception {
        String name=GenerateAddon.ROOT+"mixin/pointblank/client/StaleGunAnimationMixin";
        ClassWriter w=GenerateAddon.writer(name,"com/vicmatskiv/pointblank/client/DynamicGeoListener");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$skipNonGunControllers","(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",null,null);
        GenerateAddon.inject(m,"getControllers(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;)Ljava/util/Map;","HEAD",true);
        m.visitCode();Label skip=new Label(),done=new Label();
        m.visitVarInsn(ALOAD,1);m.visitJumpInsn(IFNULL,skip);
        m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,"net/minecraft/class_1799","method_7909","()Lnet/minecraft/class_1792;",false);
        m.visitTypeInsn(INSTANCEOF,"com/vicmatskiv/pointblank/item/GunItem");m.visitJumpInsn(IFNE,done);
        m.visitLabel(skip);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,"java/util/Collections","emptyMap","()Ljava/util/Map;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
