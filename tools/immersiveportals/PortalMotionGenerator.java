import org.objectweb.asm.*;

public final class PortalMotionGenerator implements Opcodes {
    static void generate()throws Exception {
        String root=GenerateAddon.ROOT,helper=root+"immersiveportals/PortalMotionCompat",ci=GenerateAddon.CI;
        String name=root+"mixin/immersiveportals/client/MixinShipPortalMotionData";
        ClassWriter w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/portal/Portal");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$readShipDoorPose","(Lnet/minecraft/class_2487;L"+ci+";)V",null,null);
        GenerateAddon.inject(m,"method_5749(Lnet/minecraft/class_2487;)V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"readClient","(Ljava/lang/Object;Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/client/MixinShipPortalMotionCrossing";w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/teleportation/ClientTeleportationManager");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$prepareShipDoorCrossing","(ZL"+ci+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"manageTeleportation(Z)V");arr.visitEnd();a.visit("remap",false);a.visit("require",1);a.visit("allow",1);
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","Lqouteall/imm_ptl/core/portal/animation/ClientPortalAnimationManagement;foreachCustomAnimatedPortals(Ljava/util/function/Consumer;)V");at.visit("ordinal",0);at.visit("remap",false);at.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,0);m.visitMethodInsn(INVOKESTATIC,helper,"beforeCrossing","(Z)V",false);end(m);GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/client/MixinShipPortalMotionRender";w=GenerateAddon.writer(name,"org/valkyrienskies/core/impl/game/ships/ShipObjectClientWorld");
        m=w.visitMethod(ACC_PRIVATE,"fan4$attachShipDoorToRenderPose","(DL"+ci+";)V",null,null);
        GenerateAddon.inject(m,"updateRenderTransforms(D)V","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,helper,"afterRenderTransforms","(Ljava/lang/Object;)V",false);end(m);GenerateAddon.save(name,w);
        name=root+"mixin/immersiveportals/client/MixinShipPortalMotionCleanup";w=GenerateAddon.writer(name,"qouteall/imm_ptl/core/ClientWorldLoader");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$clearShipDoorMotion","(L"+ci+";)V",null,null);
        GenerateAddon.inject(m,"cleanUp()V","HEAD",false);m.visitCode();m.visitMethodInsn(INVOKESTATIC,helper,"cleanupClient","()V",false);end(m);GenerateAddon.save(name,w);
    }
    static void end(MethodVisitor m){m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();}
}
