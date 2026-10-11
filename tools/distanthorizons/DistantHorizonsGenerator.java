import org.objectweb.asm.*;

public final class DistantHorizonsGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT;
    static void dhDimensionMixin()throws Exception {
        String dh="com/seibel/distanthorizons/core/",world=dh+"world/AbstractDhServerWorld",level=dh+"wrapperInterfaces/world/ILevelWrapper",result=dh+"level/AbstractDhServerLevel";
        String name=ROOT+"mixin/distanthorizons/common/DhDynamicDimensionMixin";ClassWriter w=GenerateAddon.writer(name,world);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$loadDynamicLevel","(L"+world+";L"+level+";)L"+result+";",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr=a.visitArray("method");arr.visit(null,"changePlayerLevel(L"+dh+"wrapperInterfaces/misc/IServerPlayerWrapper;L"+dh+"wrapperInterfaces/world/IServerLevelWrapper;L"+dh+"wrapperInterfaces/world/IServerLevelWrapper;)V");arr.visitEnd();a.visit("remap",false);a.visit("require",2);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+world+";getLevel(L"+level+";)L"+result+";");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);
        m.visitMethodInsn(INVOKEINTERFACE,dh+"world/IDhWorld","getOrLoadLevel","(L"+level+";)L"+dh+"level/IDhLevel;",true);
        m.visitTypeInsn(CHECKCAST,result);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    static void portalRenderMixins()throws Exception {
        for(String method:new String[]{"renderLodLayer","renderFadeOpaque","renderFadeTransparent"})
            RenderDiagnosticsGenerator.wrap("distanthorizons/client/"+(method.equals("renderLodLayer")?"DhLodDiagnosticMixin":method.equals("renderFadeOpaque")?"DhOpaqueFadeDiagnosticMixin":"DhTransparentFadeDiagnosticMixin"),"com/seibel/distanthorizons/core/api/internal/ClientApi",method+(method.equals("renderLodLayer")?"(Z)V":"()V"),method,-1,false,false,ROOT+"iris/DhRenderDiagnostics");
        String depthName=ROOT+"mixin/distanthorizons/client/DhDepthDiagnosticMixin";
        ClassWriter depthWriter=GenerateAddon.writer(depthName,"net/irisshaders/iris/compat/dh/DHCompat");
        for(String getter:new String[]{"getDepthTex","getDepthTexNoTranslucent"}) {
            MethodVisitor observer=depthWriter.visitMethod(ACC_PRIVATE,"fan4$observe"+getter,"(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",null,null);
            GenerateAddon.inject(observer,getter+"()I","RETURN",false);observer.visitCode();observer.visitLdcInsn(getter);observer.visitVarInsn(ALOAD,1);
            observer.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","getReturnValueI","()I",false);
            observer.visitMethodInsn(INVOKESTATIC,ROOT+"iris/DhRenderDiagnostics","depth","(Ljava/lang/String;I)V",false);observer.visitInsn(RETURN);observer.visitMaxs(0,0);observer.visitEnd();
        }
        GenerateAddon.save(depthName,depthWriter);
        String name=ROOT+"mixin/distanthorizons/client/DhPortalRenderMixin";
        ClassWriter w=GenerateAddon.writer(name,"com/seibel/distanthorizons/core/api/internal/ClientApi");
        for(String method:new String[]{"renderLodLayer","renderFadeOpaque","renderFadeTransparent"}) {
            boolean layer=method.equals("renderLodLayer");
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$guard"+method,"("+(layer?"Z":"")+"Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",null,null);
            GenerateAddon.inject(m,method+"("+(layer?"Z":"")+")V","HEAD",true);
            m.visitCode();m.visitMethodInsn(INVOKESTATIC,"qouteall/imm_ptl/core/render/context_management/PortalRendering","isRendering","()Z",false);
            Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,layer?2:1);
            m.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfo","cancel","()V",false);
            m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
        name=ROOT+"mixin/iris/client/DhPortalShaderMixin";w=GenerateAddon.writer(name,"net/irisshaders/iris/compat/dh/DHCompat");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$portalDhFrame","(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",null,null);
        GenerateAddon.inject(m,"checkFrame()Z","HEAD",true);m.visitCode();
        m.visitMethodInsn(INVOKESTATIC,"qouteall/imm_ptl/core/render/context_management/PortalRendering","isRendering","()Z",false);
        Label done=new Label();m.visitJumpInsn(IFEQ,done);m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETSTATIC,"java/lang/Boolean","FALSE","Ljava/lang/Boolean;");
        m.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        FieldVisitor texture=w.visitField(ACC_PRIVATE,"fan4$emptyDepth","I",null,null);
        texture.visitAnnotation("Lorg/spongepowered/asm/mixin/Unique;",true).visitEnd();texture.visitEnd();
        for(String getter:new String[]{"getDepthTex","getDepthTexNoTranslucent"}) {
            m=w.visitMethod(ACC_PRIVATE,"fan4$portal"+getter,"(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V",null,null);
            GenerateAddon.inject(m,getter+"()I","HEAD",true);m.visitCode();
            m.visitMethodInsn(INVOKESTATIC,"qouteall/imm_ptl/core/render/context_management/PortalRendering","isRendering","()Z",false);
            Label nativeDepth=new Label(),ready=new Label();m.visitJumpInsn(IFEQ,nativeDepth);
            m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,name,"fan4$emptyDepth","I");m.visitJumpInsn(IFNE,ready);
            m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,ROOT+"iris/PortalDepthTexture","create","()I",false);m.visitFieldInsn(PUTFIELD,name,"fan4$emptyDepth","I");
            m.visitLabel(ready);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,name,"fan4$emptyDepth","I");
            m.visitMethodInsn(INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;",false);
            m.visitMethodInsn(INVOKEVIRTUAL,"org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","setReturnValue","(Ljava/lang/Object;)V",false);
            m.visitLabel(nativeDepth);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        m=w.visitMethod(ACC_PRIVATE,"fan4$releasePortalDepth","(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V",null,null);
        GenerateAddon.inject(m,"clearPipeline()V","HEAD",false);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,name,"fan4$emptyDepth","I");
        m.visitMethodInsn(INVOKESTATIC,ROOT+"iris/PortalDepthTexture","delete","(I)V",false);m.visitVarInsn(ALOAD,0);m.visitInsn(ICONST_0);m.visitFieldInsn(PUTFIELD,name,"fan4$emptyDepth","I");
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
