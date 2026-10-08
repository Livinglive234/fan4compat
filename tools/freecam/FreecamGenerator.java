import org.objectweb.asm.*;

public final class FreecamGenerator implements Opcodes {
    static void generate()throws Exception {
        String name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamUnloadedTickMixin";
        ClassWriter w=GenerateAddon.writer(name,"net/minecraft/class_746");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$allowCameraTick","(Z)Z",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true);
        AnnotationVisitor selectors=a.visitArray("method");selectors.visit(null,"method_5773()V");selectors.visitEnd();
        a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value","INVOKE");at.visit("target","Lnet/minecraft/class_1937;method_33598(II)Z");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"freecam/FreecamTickCompat","allowTick","(ZLjava/lang/Object;)Z",false);
        m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);

        String cir="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
        name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamLodNearClipMixin";
        w=GenerateAddon.writer(name,"com/seibel/distanthorizons/core/util/RenderUtil");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$unloadedCameraNearClip","(L"+cir+";)V",null,null);
        GenerateAddon.inject(m,"getNearClipPlaneInBlocks()F","RETURN",true);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,cir,"getReturnValueF","()F",false);
        m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"freecam/FreecamTickCompat","nearClip","(F)F",false);
        m.visitMethodInsn(INVOKESTATIC,"java/lang/Float","valueOf","(F)Ljava/lang/Float;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,cir,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamLodClipUniformMixin";
        w=GenerateAddon.writer(name,"com/seibel/distanthorizons/common/render/openGl/terrain/GlDhTerrainShaderProgram");
        m=w.visitMethod(ACC_PRIVATE,"fan4$unloadedCameraLodClip","(Lcom/seibel/distanthorizons/api/methods/events/sharedParameterObjects/DhApiRenderParam;L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"fillUniformData(Lcom/seibel/distanthorizons/api/methods/events/sharedParameterObjects/DhApiRenderParam;)V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,GenerateAddon.ROOT+"freecam/FreecamTickCompat","lodClip","(Ljava/lang/Object;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);

        String shaderHelper=GenerateAddon.ROOT+"freecam/FreecamShaderCompat";
        name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamBslShaderMixin";
        w=GenerateAddon.writer(name,"net/irisshaders/iris/pipeline/transform/TransformPatcher");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$bslFreecamSource","(Ljava/lang/String;)Ljava/lang/String;",null,null);
        a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",true);
        selectors=a.visitArray("method");selectors.visit(null,"patchDHTerrain(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lit/unimi/dsi/fastutil/objects/Object2ObjectMap;)Ljava/util/Map;");selectors.visitEnd();
        a.visit("argsOnly",true);a.visit("index",5);a.visit("require",1);a.visit("remap",false);at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","HEAD");at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,shaderHelper,"source","(Ljava/lang/String;)Ljava/lang/String;",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=GenerateAddon.ROOT+"mixin/freecam/client/FreecamBslUniformMixin";
        w=GenerateAddon.writer(name,"net/irisshaders/iris/compat/dh/IrisLodRenderProgram");
        m=w.visitMethod(ACC_PRIVATE,"fan4$bslFreecamUniform","(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4fc;IFL"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"fillUniformData(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4fc;IF)V","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,shaderHelper,"uniforms","(Ljava/lang/Object;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
