import org.objectweb.asm.*;
public final class ShipLoadRecoveryGenerator implements Opcodes {
    static void generate()throws Exception {
        String root=GenerateAddon.ROOT,ci=GenerateAddon.CI,cir="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",helper=root+"immersiveportals/ShipLoadRecovery";
        String guard=root+"mixin/immersiveportals/common/ShipGuardRecoveryMixin";
        ClassWriter guardWriter=GenerateAddon.writer(guard,"org/valkyrienskies/mod/common/util/EntityShipCollisionUtils");
        MethodVisitor observer=guardWriter.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$recoverGuard","(Lnet/minecraft/class_1297;L"+cir+";)V",null,null);
        GenerateAddon.inject(observer,"isCollidingWithUnloadedShips(Lnet/minecraft/class_1297;)Z","RETURN",false);
        observer.visitCode();observer.visitVarInsn(ALOAD,0);observer.visitVarInsn(ALOAD,1);
        observer.visitMethodInsn(INVOKEVIRTUAL,cir,"getReturnValueZ","()Z",false);
        observer.visitMethodInsn(INVOKESTATIC,helper,"guard","(Ljava/lang/Object;Z)V",false);
        LogCompatibilityGenerator.end(observer);GenerateAddon.save(guard,guardWriter);
        String name=root+"mixin/immersiveportals/common/ShipAcknowledgementRecoveryMixin";ClassWriter w=GenerateAddon.writer(name,"org/valkyrienskies/mod/common/networking/VSGamePackets");
        String args="Lorg/valkyrienskies/mod/common/networking/PacketChangeKnownShips;Lorg/valkyrienskies/core/internal/world/VsiPlayer;";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$recoverAcknowledgement","("+args+"L"+cir+";)V",null,null);
        GenerateAddon.inject(m,"registerHandlers$lambda$14$lambda$13("+args+")Lkotlin/Unit;","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"acknowledged","(Ljava/lang/Object;Ljava/lang/Object;)V",false);LogCompatibilityGenerator.end(m);GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/common/ShipAcknowledgementFlushMixin";w=GenerateAddon.writer(name,"org/valkyrienskies/mod/common/world/ChunkManagement");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$flushAcknowledgements","(Lorg/valkyrienskies/core/internal/world/VsiServerShipWorld;Lnet/minecraft/server/MinecraftServer;L"+ci+";)V",null,null);
        GenerateAddon.inject(m,"tickChunkLoading(Lorg/valkyrienskies/core/internal/world/VsiServerShipWorld;Lnet/minecraft/server/MinecraftServer;)V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"flush","(Ljava/lang/Object;)V",false);LogCompatibilityGenerator.end(m);GenerateAddon.save(name,w);
    }
}
