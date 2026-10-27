import org.objectweb.asm.*;

/**
 * Keeps Spatial GUI's saved perspective (projection and view matrices) from being overwritten by Immersive Portals' portal views: capturePerspectiveState returns
 * at once while SpatialGuiPortalFix reports a portal view is rendering. Applied only for Spatial GUI 1.9.1 with Immersive Portals (CompatibilityRules).
 * Not hooked: shouldCapture. Spatial GUI resets its camera state whenever that is false, so reporting false during portal views reset it every frame.
 */
public final class SpatialGuiPerspectiveGenerator implements Opcodes {
    static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/spatialgui/client/SpatialGuiPerspectiveMixin";
        ClassWriter w=GenerateAddon.writer(name,"org/tastytrash/spatialGUI/render/SpatialGUIRenderer");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$keepMainPerspective","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"capturePerspectiveState(Lorg/joml/Matrix4f;Lnet/minecraft/class_8251;Lnet/minecraft/class_4587;)V","HEAD",true);
        m.visitCode();m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"spatialgui/SpatialGuiPortalFix","skipCapture","()Z",false);
        Label capture=new Label();m.visitJumpInsn(IFEQ,capture);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,GenerateAddon.CI,"cancel","()V",false);
        m.visitLabel(capture);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
    }
}
