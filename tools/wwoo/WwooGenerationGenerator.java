import org.objectweb.asm.*;

/** DH 3.3.3 / WWOO 2.6.7: enlarge only the ephemeral FEATURES batch. */
public final class WwooGenerationGenerator implements Opcodes {
    static void generate()throws Exception {
        String owner="com/seibel/distanthorizons/common/wrappers/worldGeneration/DhChunkGenerator";
        String event="com/seibel/distanthorizons/common/wrappers/worldGeneration/ChunkGenEvent";
        String name=GenerateAddon.ROOT+"mixin/wwoo/common/WwooDhGenerationMixin";
        ClassWriter w=GenerateAddon.writer(name,owner);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$wwooNeighbourRing","(L"+event+";)I",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"generateChunks(L"+event+";)V");arr.visitEnd();
        a.visit("require",1);a.visit("remap",false);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value","FIELD");at.visit("target","L"+owner+";MAX_WORLD_GEN_CHUNK_BORDER_NEEDED:I");
        at.visit("opcode",GETSTATIC);at.visit("ordinal",0);at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);
        m.visitFieldInsn(GETSTATIC,owner,"MAX_WORLD_GEN_CHUNK_BORDER_NEEDED","I");
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"wwoo/WwooGenerationCompat","border","(Ljava/lang/Object;Ljava/lang/Object;I)I",false);
        m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
