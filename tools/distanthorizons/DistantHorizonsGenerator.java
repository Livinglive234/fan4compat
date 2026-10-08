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
        m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
