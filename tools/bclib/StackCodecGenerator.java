import org.objectweb.asm.*;

/** Delegate BCLib's codec, only adapting a missing item key during decoding. */
public final class StackCodecGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,MAP="com/mojang/serialization/MapCodec",OPS="com/mojang/serialization/DynamicOps",LIKE="com/mojang/serialization/MapLike",RESULT="com/mojang/serialization/DataResult",BUILDER="com/mojang/serialization/RecordBuilder",WRAPPER=ROOT+"bclib/AliasedStackCodec";
    static void generate()throws Exception {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(V17,ACC_PUBLIC|ACC_SUPER,WRAPPER,null,MAP,null);w.visitField(ACC_PRIVATE|ACC_FINAL,"delegate","L"+MAP+";",null,null).visitEnd();
        MethodVisitor m=w.visitMethod(ACC_PUBLIC,"<init>","(L"+MAP+";)V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,MAP,"<init>","()V",false);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitFieldInsn(PUTFIELD,WRAPPER,"delegate","L"+MAP+";");LogCompatibilityGenerator.end(m);
        m=w.visitMethod(ACC_PUBLIC,"decode","(L"+OPS+";L"+LIKE+";)L"+RESULT+";",null,null);m.visitCode();delegate(m);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,ROOT+"bclib/StackCodecCompat","alias","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,LIKE);m.visitMethodInsn(INVOKEVIRTUAL,MAP,"decode","(L"+OPS+";L"+LIKE+";)L"+RESULT+";",false);ret(m);
        m=w.visitMethod(ACC_PUBLIC,"encode","(Ljava/lang/Object;L"+OPS+";L"+BUILDER+";)L"+BUILDER+";",null,null);m.visitCode();delegate(m);for(int i=1;i<4;i++)m.visitVarInsn(ALOAD,i);m.visitMethodInsn(INVOKEVIRTUAL,MAP,"encode","(Ljava/lang/Object;L"+OPS+";L"+BUILDER+";)L"+BUILDER+";",false);ret(m);
        m=w.visitMethod(ACC_PUBLIC,"keys","(L"+OPS+";)Ljava/util/stream/Stream;",null,null);m.visitCode();delegate(m);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,MAP,"keys","(L"+OPS+";)Ljava/util/stream/Stream;",false);ret(m);GenerateAddon.save(WRAPPER,w);
        String name=ROOT+"mixin/bclib/common/StackCodecAliasMixin",field="CODEC_ITEM_STACK_WITH_NBT";w=GenerateAddon.writer(name,"org/betterx/bclib/util/ItemUtil");FieldVisitor f=w.visitField(ACC_PRIVATE|ACC_STATIC,field,"L"+MAP+";",null,null);for(String annotation:new String[]{"Shadow","Final","Mutable"})f.visitAnnotation("Lorg/spongepowered/asm/mixin/"+annotation+";",true).visitEnd();f.visitEnd();
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$stackKeyAlias","(L"+GenerateAddon.CI+";)V",null,null);GenerateAddon.inject(m,"<clinit>()V","RETURN",false);m.visitCode();m.visitTypeInsn(NEW,WRAPPER);m.visitInsn(DUP);m.visitFieldInsn(GETSTATIC,name,field,"L"+MAP+";");m.visitMethodInsn(INVOKESPECIAL,WRAPPER,"<init>","(L"+MAP+";)V",false);m.visitFieldInsn(PUTSTATIC,name,field,"L"+MAP+";");LogCompatibilityGenerator.end(m);GenerateAddon.save(name,w);
    }
    static void delegate(MethodVisitor m){m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,WRAPPER,"delegate","L"+MAP+";");}
    static void ret(MethodVisitor m){m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();}
}
