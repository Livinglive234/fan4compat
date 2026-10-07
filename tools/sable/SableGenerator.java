import org.objectweb.asm.*;

public final class SableGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT;
    static void sableMixin()throws Exception {
        // A companion library alone is not the Sable physics implementation.
        // Its default companion delegates to VS, so feeding it back into VS's
        // exclusion check recurses. Preserve the original VS companion methods;
        // only skip this exclusion when the companion is exactly the fallback.
        String name=ROOT+"mixin/sable/common/SableCompatMixin";ClassWriter w=GenerateAddon.writer(name,"org/valkyrienskies/mod/compat/SableCompat");
        String companion="dev/ryanhcode/sable/companion/SableCompanion";
        String cir="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
        for(int count:new int[]{2,3}) {
            String operation=count==2?"isChunkInSublevel":"isBlockInSublevel";
            String params="Lnet/minecraft/class_1937;"+"I".repeat(count);
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$"+operation,"("+params+"L"+cir+";)V",null,null);
            GenerateAddon.inject(m,operation+"("+params+")Z","HEAD",true);m.visitCode();
            m.visitFieldInsn(GETSTATIC,companion,"INSTANCE","L"+companion+";");
            m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Object","getClass","()Ljava/lang/Class;",false);
            m.visitLdcInsn(Type.getObjectType("dev/ryanhcode/sable/companion/impl/DefaultSableCompanion"));
            Label done=new Label();m.visitJumpInsn(IF_ACMPNE,done);
            m.visitVarInsn(ALOAD,count+2);m.visitFieldInsn(GETSTATIC,"java/lang/Boolean","FALSE","Ljava/lang/Boolean;");
            m.visitMethodInsn(INVOKEVIRTUAL,cir,"setReturnValue","(Ljava/lang/Object;)V",false);
            m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
}
