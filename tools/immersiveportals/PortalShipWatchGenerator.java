import org.objectweb.asm.*;

/** Capture invocation arguments and the player receiver without reconstructed locals. */
public final class PortalShipWatchGenerator implements Opcodes {
    static final String TARGET="a(Lorg/joml/primitives/AABBd;Lorg/joml/primitives/AABBic;Lorg/valkyrienskies/core/api/world/LevelYRange;Lorg/valkyrienskies/core/api/ships/properties/ShipTransform;Lorg/valkyrienskies/core/impl/shadow/CY;Lorg/valkyrienskies/core/impl/api/ServerShipInternal;Ljava/util/Set;Lorg/joml/Vector3d;DDLjava/util/TreeSet;Ljava/util/TreeSet;II)V";
    static final String OP="com/llamalad7/mixinextras/injector/wrapoperation/Operation",HELPER=GenerateAddon.ROOT+"immersiveportals/PortalShipWatchCompat";
    static void annotation(MethodVisitor m,String injector,String invocation) {
        AnnotationVisitor a=m.visitAnnotation(injector,true),arr=a.visitArray("method");
        arr.visit(null,TARGET);arr.visitEnd();a.visit("remap",false);
        if(invocation!=null) {
            a.visit("require",1);a.visit("expect",1);a.visit("allow",1);
            AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");
            at.visit("value","INVOKE");at.visit("target",invocation);at.visit("remap",false);at.visitEnd();
        }
        a.visitEnd();
    }
    static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/immersiveportals/common/PortalShipWatchMixin";
        ClassWriter w=GenerateAddon.writer(name,"org/valkyrienskies/core/impl/shadow/CY");
        String desc=TARGET.substring(1);Type[] args=Type.getArgumentTypes(desc);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$watchScope",desc.replace(")V","L"+OP+";)V"),null,null);
        annotation(m,"Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",null);
        m.visitCode();m.visitVarInsn(ALOAD,5);m.visitVarInsn(ILOAD,14);m.visitVarInsn(ILOAD,15);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"enter","(Ljava/lang/Object;II)V",false);
        Label start=new Label(),end=new Label(),failure=new Label();m.visitTryCatchBlock(start,end,failure,null);m.visitLabel(start);
        m.visitVarInsn(ALOAD,16);m.visitIntInsn(BIPUSH,args.length);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        int slot=0;
        for(int i=0;i<args.length;i++) {
            m.visitInsn(DUP);m.visitIntInsn(BIPUSH,i);m.visitVarInsn(args[i].getOpcode(ILOAD),slot);
            if(args[i].getSort()==Type.DOUBLE)m.visitMethodInsn(INVOKESTATIC,"java/lang/Double","valueOf","(D)Ljava/lang/Double;",false);
            if(args[i].getSort()==Type.INT)m.visitMethodInsn(INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;",false);
            m.visitInsn(AASTORE);slot+=args[i].getSize();
        }
        m.visitMethodInsn(INVOKEINTERFACE,OP,"call","([Ljava/lang/Object;)Ljava/lang/Object;",true);m.visitInsn(POP);m.visitLabel(end);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"exit","()V",false);m.visitInsn(RETURN);
        m.visitLabel(failure);m.visitVarInsn(ASTORE,17);m.visitMethodInsn(INVOKESTATIC,HELPER,"exit","()V",false);m.visitVarInsn(ALOAD,17);m.visitInsn(ATHROW);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$playerPosition","(Lorg/valkyrienskies/core/internal/world/VsiPlayer;Lorg/joml/Vector3d;)Lorg/joml/Vector3d;",null,null);
        annotation(m,"Lorg/spongepowered/asm/mixin/injection/Redirect;","Lorg/valkyrienskies/core/internal/world/VsiPlayer;getPosition(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;");
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"playerPosition","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"org/joml/Vector3d");m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        for(boolean dimension:new boolean[]{true,false}) {
            String primitive=dimension?"Z":"D",helper=dimension?"dimensionEligible":"watchDistance";
            m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$"+helper,"("+primitive+")"+primitive,null,null);
            annotation(m,"Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",dimension?"Lkotlin/jvm/internal/Intrinsics;areEqual(Ljava/lang/Object;Ljava/lang/Object;)Z":"Lorg/valkyrienskies/core/util/AABBdUtilKt;signedDistanceTo(Lorg/joml/primitives/AABBdc;Lorg/joml/Vector3dc;)D");
            m.visitCode();m.visitVarInsn(dimension?ILOAD:DLOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,helper,"("+primitive+")"+primitive,false);m.visitInsn(dimension?IRETURN:DRETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
}
