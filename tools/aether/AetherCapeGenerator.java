import org.objectweb.asm.*;

/**
 * The Aether's cape slot forces Player.isModelPartShown to true for EVERY skin part whenever a cape is equipped (its injected check never looks at
 * which part was asked about), so wearing a cape turns jacket, sleeves, pants and hat back on even when the player switched them off. This mixin runs
 * first (lower priority number) and, for every part except the cape, answers exactly what vanilla answers: the player's own skin customisation bits.
 * The cape part is left to the Aether.
 */
public final class AetherCapeGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,PLAYER="net/minecraft/class_1657",ENTITY="net/minecraft/class_1297",PART="net/minecraft/class_1664",
        TRACKER="net/minecraft/class_2945",ACCESSOR="net/minecraft/class_2940",CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static void skinLayersMixin() throws Exception {
        String name=ROOT+"mixin/aether/common/AetherCapeSkinLayersMixin";
        ClassWriter w=GenerateAddon.writer(name,PLAYER,500);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$keepSkinLayers","(L"+PART+";L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"method_7348(L"+PART+";)Z","HEAD",true);
        m.visitCode();
        Label cape=new Label(),shown=new Label(),done=new Label();
        m.visitVarInsn(ALOAD,1);m.visitFieldInsn(GETSTATIC,PART,"field_7559","L"+PART+";");m.visitJumpInsn(IF_ACMPEQ,cape);
        m.visitVarInsn(ALOAD,2);
        // vanilla: (entityData.get(DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask()
        m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,ENTITY,"method_5841","()L"+TRACKER+";",false);
        m.visitFieldInsn(GETSTATIC,PLAYER,"field_7518","L"+ACCESSOR+";");
        m.visitMethodInsn(INVOKEVIRTUAL,TRACKER,"method_12789","(L"+ACCESSOR+";)Ljava/lang/Object;",false);
        m.visitTypeInsn(CHECKCAST,"java/lang/Byte");m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Byte","byteValue","()B",false);
        m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,PART,"method_7430","()I",false);m.visitInsn(IAND);
        m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,PART,"method_7430","()I",false);
        m.visitJumpInsn(IF_ICMPEQ,shown);m.visitInsn(ICONST_0);m.visitJumpInsn(GOTO,done);
        m.visitLabel(shown);m.visitInsn(ICONST_1);
        m.visitLabel(done);m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitLabel(cape);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
