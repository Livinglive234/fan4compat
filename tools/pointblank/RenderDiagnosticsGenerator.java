import org.objectweb.asm.*;

/** Wrap exact native void methods; the diagnostic finally never changes render behavior. */
public final class RenderDiagnosticsGenerator implements Opcodes {
    static final String OP="com/llamalad7/mixinextras/injector/wrapoperation/Operation",HELPER=GenerateAddon.ROOT+"pointblank/RenderDiagnostics";
    static void wrap(String simple,String owner,String selector,String stage,int item)throws Exception {wrap(simple,owner,selector,stage,item,false,false);}
    static void wrap(String simple,String owner,String selector,String stage,int item,boolean statik,boolean iface)throws Exception {
        String name=GenerateAddon.ROOT+"mixin/pointblank/client/"+simple;ClassWriter w=GenerateAddon.writer(name,owner);
        if(iface)w.visit(V17,ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT,name,null,"java/lang/Object",null);
        String desc=selector.substring(selector.indexOf('('));Type[] args=Type.getArgumentTypes(desc);
        String handler=desc.substring(0,desc.indexOf(')'))+"L"+OP+";)V";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|(statik?ACC_STATIC:0),"fan4$observe",handler,null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true),array=a.visitArray("method");array.visit(null,selector);array.visitEnd();a.visit("remap",false);a.visitEnd();
        int[] locals=new int[args.length];int opLocal=statik?0:1;for(int i=0;i<args.length;i++){locals[i]=opLocal;opLocal+=args[i].getSize();}int token=opLocal+1,error=token+1;
        m.visitCode();m.visitLdcInsn(stage);if(item<0)m.visitInsn(ACONST_NULL);else m.visitVarInsn(ALOAD,locals[item]);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"begin","(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;",false);m.visitVarInsn(ASTORE,token);
        Label begin=new Label(),finish=new Label(),fail=new Label();m.visitTryCatchBlock(begin,finish,fail,null);m.visitLabel(begin);
        m.visitVarInsn(ALOAD,opLocal);m.visitLdcInsn(args.length);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        for(int i=0;i<args.length;i++) {
            m.visitInsn(DUP);m.visitLdcInsn(i);m.visitVarInsn(args[i].getOpcode(ILOAD),locals[i]);
            if(args[i].getSort()==Type.INT)m.visitMethodInsn(INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;",false);
            if(args[i].getSort()==Type.FLOAT)m.visitMethodInsn(INVOKESTATIC,"java/lang/Float","valueOf","(F)Ljava/lang/Float;",false);
            if(args[i].getSort()==Type.BOOLEAN)m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);
            m.visitInsn(AASTORE);
        }
        m.visitMethodInsn(INVOKEINTERFACE,OP,"call","([Ljava/lang/Object;)Ljava/lang/Object;",true);m.visitInsn(POP);m.visitLabel(finish);
        m.visitVarInsn(ALOAD,token);m.visitInsn(ICONST_0);m.visitMethodInsn(INVOKESTATIC,HELPER,"end","(Ljava/lang/Object;Z)V",false);m.visitInsn(RETURN);
        m.visitLabel(fail);m.visitVarInsn(ASTORE,error);m.visitVarInsn(ALOAD,token);m.visitInsn(ICONST_1);m.visitMethodInsn(INVOKESTATIC,HELPER,"end","(Ljava/lang/Object;Z)V",false);m.visitVarInsn(ALOAD,error);m.visitInsn(ATHROW);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    public static void generate()throws Exception {
        String key="(Lcom/vicmatskiv/pointblank/client/render/RenderTypeKey;)V";
        for(int i=3;i<=7;i++)wrap("GunScope"+i+"DiagnosticMixin",StencilGenerator.PROVIDER,"lambda$static$"+i+key,"scope-shared-"+i,0,true,true);
        for(int i=20;i<=21;i++)wrap("GunDefault"+i+"DiagnosticMixin","com/vicmatskiv/pointblank/client/render/DefaultRenderTypeProvider","lambda$createGlowRenderType$"+i+"()V","scope-default-glow-"+i,-1,true,false);
        for(String[] lambda:new String[][]{{"createDefaultItemRenderType","5"},{"createDefaultItemRenderType","6"},{"createPipRenderType","9"},{"createPipRenderType","10"},{"createPipShutterOverlayRenderType","13"},{"createPipShutterOverlayRenderType","14"},{"createPipMaskRenderType","18"},{"createPipOverlayRenderType","19"},{"createPipOverlayRenderType","20"}})
            wrap("GunIris"+lambda[1]+"DiagnosticMixin","com/vicmatskiv/pointblank/compat/iris/IrisRenderTypeProvider","lambda$"+lambda[0]+"$"+lambda[1]+key,"scope-iris-"+lambda[0]+"-"+lambda[1],0,true,false);
        for(int i=23;i<=26;i++)wrap("GunIris"+i+"DiagnosticMixin","com/vicmatskiv/pointblank/compat/iris/IrisRenderTypeProvider","lambda$"+(i<=24?"createGlowRenderType":"createMuzzleFlashRenderType")+"$"+i+"()V","scope-iris-effect-"+i,-1,true,false);
        wrap("GunGuiDiagnosticMixin","qouteall/imm_ptl/core/render/GuiPortalRendering","renderWorldIntoFrameBuffer(Lqouteall/imm_ptl/core/render/context_management/WorldRenderInfo;Lnet/minecraft/class_276;)V","gui-camera",1,true,false);
        wrap("GunRenderDiagnosticMixin","com/vicmatskiv/pointblank/client/render/GunItemRenderer","method_3166(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V","gun-item",0);
        wrap("GunPrepareDiagnosticMixin","com/vicmatskiv/pointblank/client/ClientSystem","preRender(Lcom/vicmatskiv/pointblank/client/GunClientState;Lnet/minecraft/class_9779;)V","gun-prepare",-1);
        wrap("GunAuxDiagnosticMixin","com/vicmatskiv/pointblank/client/render/AuxLevelRenderer","renderToTarget(Lnet/minecraft/class_9779;F)V","scope-world",-1);
        wrap("GunWorldDiagnosticMixin","net/minecraft/class_761","method_22710(Lnet/minecraft/class_9779;ZLnet/minecraft/class_4184;Lnet/minecraft/class_757;Lnet/minecraft/class_765;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V","world",-1);
    }
}
