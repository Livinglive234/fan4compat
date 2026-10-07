import org.objectweb.asm.*;

public final class ShipTransitGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,CI=GenerateAddon.CI,CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static void end(MethodVisitor m){m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();}
    static void transit() throws Exception {
        String helper=ROOT+"immersiveportals/ShipTransitCompat",entity="net/minecraft/class_1297",player="net/minecraft/class_3222",world="net/minecraft/class_3218",vec="net/minecraft/class_243";
        String name=ROOT+"mixin/immersiveportals/common/ShipPortalTransferMixin";ClassWriter w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/teleportation/ServerTeleportationManager");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$resetShipMotion","(L"+player+";L"+world+";L"+world+";L"+vec+";L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"changePlayerDimension(L"+player+";L"+world+";L"+world+";L"+vec+";)V","HEAD",false);m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"resetShipMotion","(Ljava/lang/Object;)V",false);end(m);
        // Rebuild arrival watches after IP has installed the destination world/position.
        m=w.visitMethod(ACC_PRIVATE,"fan4$refreshArrival","(L"+player+";L"+world+";L"+world+";L"+vec+";L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"changePlayerDimension(L"+player+";L"+world+";L"+world+";L"+vec+";)V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"finishTransfer","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=ROOT+"mixin/immersiveportals/client/MixinShipPortalTransfer";w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/teleportation/ClientTeleportationManager");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$resetShipMotion","(Lnet/minecraft/class_746;Lnet/minecraft/class_638;Lnet/minecraft/class_638;L"+vec+";L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"changePlayerDimension(Lnet/minecraft/class_746;Lnet/minecraft/class_638;Lnet/minecraft/class_638;L"+vec+";)V","HEAD",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,helper,"resetShipMotion","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=ROOT+"mixin/immersiveportals/client/MixinShipKnownShips";w=GenerateAddon.writer(name,"net/minecraft/class_310");
        m=w.visitMethod(ACC_PRIVATE,"fan4$acknowledgeLoadedShips","(L"+CI+";)V",null,null);
        GenerateAddon.inject(m,"method_1574()V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,helper,"acknowledgeLoadedShips","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=ROOT+"mixin/immersiveportals/common/ShipMotionDimensionMixin";w=GenerateAddon.writer(name,"org/valkyrienskies/mod/common/networking/VSGamePackets");
        String packet="org/valkyrienskies/mod/common/networking/PacketPlayerShipMotion",wrapper="org/valkyrienskies/core/internal/world/VsiPlayer";
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$rejectOldShipMotion","(L"+packet+";L"+wrapper+";L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"registerHandlers$lambda$14$lambda$11(L"+packet+";L"+wrapper+";)Lkotlin/Unit;","HEAD",true);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"wrongDimensionMotion","(Ljava/lang/Object;Ljava/lang/Object;)Z",false);
        Label keep=new Label();m.visitJumpInsn(IFEQ,keep);m.visitVarInsn(ALOAD,2);m.visitFieldInsn(GETSTATIC,"kotlin/Unit","INSTANCE","Lkotlin/Unit;");m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(keep);end(m);GenerateAddon.save(name,w);
    }
}
