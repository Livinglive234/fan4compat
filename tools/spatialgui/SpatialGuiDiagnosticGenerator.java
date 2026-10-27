import org.objectweb.asm.*;

/**
 * Observation-only hooks for the Spatial GUI / Immersive Portals trace: each calls SpatialGuiDiagnostics.stage with a label and changes nothing. Applied
 * only when the diagnostics switch is on and Spatial GUI 1.9.1 is installed (CompatibilityRules). The world-render hooks use a high priority number so they run after
 * Spatial GUI's own handlers at the start and end of LevelRenderer.renderLevel and see the state those handlers leave.
 */
public final class SpatialGuiDiagnosticGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,HELPER=ROOT+"spatialgui/SpatialGuiDiagnostics",SPATIAL="org/tastytrash/spatialGUI/render/";
    static void generate()throws Exception {
        hooks("SpatialGuiLevelDiagnosticMixin","net/minecraft/class_761",3000,new String[][]{{"method_22710","HEAD","level head"},{"method_22710","TAIL","level tail"}});
        hooks("SpatialGuiTargetDiagnosticMixin",SPATIAL+"TextureTargetManager",1000,new String[][]{
            {"prepareTarget()V","HEAD","prepareTarget head"},{"prepareTarget()V","TAIL","prepareTarget tail"},
            {"clearTarget()V","HEAD","clearTarget head"},{"clearTarget()V","TAIL","clearTarget tail"}});
        hooks("SpatialGuiRendererDiagnosticMixin",SPATIAL+"SpatialGUIRenderer",1000,new String[][]{
            {"onFrameStart()V","HEAD","onFrameStart"},
            {"extractIsolatedScreen(Lnet/minecraft/class_437;F)V","HEAD","extractIsolatedScreen head"},{"extractIsolatedScreen(Lnet/minecraft/class_437;F)V","TAIL","extractIsolatedScreen tail"},
            {"capturePerspectiveState(Lorg/joml/Matrix4f;Lnet/minecraft/class_8251;Lnet/minecraft/class_4587;)V","HEAD","capturePerspectiveState"},
            {"renderInWorld(Lnet/minecraft/class_4587;)V","HEAD","renderInWorld head"},{"renderInWorld(Lnet/minecraft/class_4587;)V","TAIL","renderInWorld tail"},
            {"renderInWorldPost()V","HEAD","renderInWorldPost"}});
    }
    /** points: {target method, injection point, stage label}. */
    static void hooks(String simpleName,String target,int priority,String[][] points)throws Exception {
        String name=ROOT+"mixin/spatialgui/client/"+simpleName;
        ClassWriter w=GenerateAddon.writer(name,target,priority);
        for(int i=0;i<points.length;i++) {
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$spatialGuiStage"+i,"(L"+GenerateAddon.CI+";)V",null,null);
            GenerateAddon.inject(m,points[i][0],points[i][1],false);
            m.visitCode();m.visitLdcInsn(points[i][2]);m.visitMethodInsn(INVOKESTATIC,HELPER,"stage","(Ljava/lang/String;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
}
