import org.objectweb.asm.*;

/** Two client hooks on Xaero's map screen: choose what it shows when it opens, and undo that when it closes. */
public final class XaeroMapGenerator implements Opcodes {
    public static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/xaero/client/TardisMapMixin";
        ClassWriter w=GenerateAddon.writer(name,"xaero/map/gui/GuiMap");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$tardisMapOpened","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"<init>(Lnet/minecraft/class_437;Lnet/minecraft/class_437;Lxaero/map/MapProcessor;Lnet/minecraft/class_1297;)V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"xaero/TardisMapCompat","opened","(Ljava/lang/Object;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        // Screen.removed(): the switch lasts only while the map screen is open, so the minimap and the map writer see no change afterward.
        m=w.visitMethod(ACC_PRIVATE,"fan4$tardisMapClosed","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"method_25432()V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"xaero/TardisMapCompat","closed","(Ljava/lang/Object;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
    }
}
