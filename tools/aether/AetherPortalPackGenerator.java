import org.objectweb.asm.*;

/**
 * The Aether 1.5.11 registers its Immersive Portals data pack in setupImmersivePortalsPack but never calls it from packSetup (The-Aether-Team/The-Aether#2851),
 * so the Aether portal does not turn into an Immersive Portals portal. This calls it at the end of packSetup. Both hooks go through PortalPack.firstTime, so a jar that
 * already calls it registers the pack once. Applied for Aether 1.5.11 with Immersive Portals (CompatibilityRules).
 */
public final class AetherPortalPackGenerator implements Opcodes {
    static final String AETHER="com/aetherteam/aether/Aether",EVENT="com/aetherteam/aetherfabric/events/AddPackFindersEvent",PACK=GenerateAddon.ROOT+"aether/PortalPack",
        NAME=GenerateAddon.ROOT+"mixin/aether/common/AetherPortalPackMixin",SETUP="setupImmersivePortalsPack",DESC="(L"+EVENT+";)V";
    static void generate()throws Exception {
        ClassWriter w=GenerateAddon.writer(NAME,AETHER);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,SETUP,DESC,null,null);
        m.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;",true).visitEnd();m.visitCode();m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // End of packSetup: register the pack.
        m=w.visitMethod(ACC_PRIVATE,"fan4$addPortalPack","(L"+EVENT+";L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"packSetup"+DESC,"TAIL",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESPECIAL,NAME,SETUP,DESC,false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // Start of the registration itself: skip it when this event already went through.
        m=w.visitMethod(ACC_PRIVATE,"fan4$registerOnce","(L"+EVENT+";L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,SETUP+DESC,"HEAD",true);
        Label first=new Label();
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,PACK,"firstTime","(Ljava/lang/Object;)Z",false);m.visitJumpInsn(IFNE,first);
        m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,GenerateAddon.CI,"cancel","()V",false);
        m.visitLabel(first);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(NAME,w);
    }
}
