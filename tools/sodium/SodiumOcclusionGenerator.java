import org.objectweb.asm.*;

/**
 * Sodium 0.8 OcclusionCuller: Immersive Portals' portal-aware occlusion search (see sodium/PortalOcclusion). IP's own mixin for it is cancelled for Sodium 0.8
 * (CompatPlugin) because its findVisible handler names Sodium 0.6's Visitor type. The same four hooks: findVisible's useOcclusionCulling argument starts the search
 * state, the two getChunkCoord calls in init and initWithinWorld use the portal's start section when there is one, and isWithinFrustum tolerates the start section's
 * first failed test. Applied only for Sodium 0.8.x with Immersive Portals (CompatibilityRules).
 */
public final class SodiumOcclusionGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,SODIUM="net/caffeinemc/mods/sodium/client/",CULLER=SODIUM+"render/chunk/occlusion/OcclusionCuller",SECTION=SODIUM+"render/chunk/RenderSection",
        VIEWPORT=SODIUM+"render/viewport/Viewport",VISITOR=SODIUM+"render/chunk/lists/RenderSectionVisitor",SECTION_POS="net/minecraft/class_4076",
        OCCLUSION=ROOT+"sodium/PortalOcclusion",HOST=OCCLUSION+"$Host",NAME=ROOT+"mixin/sodium/client/SodiumPortalOcclusionMixin",CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static void generate()throws Exception {
        ClassWriter w=GenerateAddon.writer(NAME,CULLER,HOST);
        FieldVisitor f=w.visitField(ACC_PRIVATE,"fan4$start","L"+SECTION_POS+";",null,null);f.visitAnnotation("Lorg/spongepowered/asm/mixin/Unique;",true).visitEnd();f.visitEnd();
        // Shadows of the OcclusionCuller's private section lookup and its public static frustum test.
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"getRenderSection","(III)L"+SECTION+";",null,null);
        m.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;",true).visitEnd();m.visitCode();m.visitInsn(ACONST_NULL);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC|ACC_STATIC,"isWithinFrustum","(L"+VIEWPORT+";L"+SECTION+";)Z",null,null);
        m.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;",true).visitEnd();m.visitCode();m.visitInsn(ICONST_0);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();

        // PortalOcclusion.Host
        m=w.visitMethod(ACC_PUBLIC,"fan4$section","(III)Ljava/lang/Object;",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ILOAD,1);m.visitVarInsn(ILOAD,2);m.visitVarInsn(ILOAD,3);
        m.visitMethodInsn(INVOKEVIRTUAL,NAME,"getRenderSection","(III)L"+SECTION+";",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"fan4$withinFrustum","(Ljava/lang/Object;Ljava/lang/Object;)Z",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitTypeInsn(CHECKCAST,VIEWPORT);m.visitVarInsn(ALOAD,2);m.visitTypeInsn(CHECKCAST,SECTION);
        m.visitMethodInsn(INVOKESTATIC,NAME,"isWithinFrustum","(L"+VIEWPORT+";L"+SECTION+";)Z",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"fan4$startPoint","(Ljava/lang/Object;)V",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitTypeInsn(CHECKCAST,SECTION_POS);m.visitFieldInsn(PUTFIELD,NAME,"fan4$start","L"+SECTION_POS+";");m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // 1. findVisible(visitor, viewport, searchDistance, useOcclusionCulling, frame): replace useOcclusionCulling. Locals: this 0, value 1, visitor 2, viewport 3.
        m=w.visitMethod(0,"fan4$beginSearch","(ZL"+VISITOR+";L"+VIEWPORT+";FZI)Z",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",true),arr=a.visitArray("method");arr.visit(null,"findVisible");arr.visitEnd();
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","HEAD");at.visitEnd();arr.visitEnd();
        a.visit("argsOnly",true);a.visit("remap",false);a.visit("require",1);a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,3);m.visitVarInsn(ILOAD,1);
        m.visitMethodInsn(INVOKESTATIC,OCCLUSION,"begin","(L"+HOST+";Ljava/lang/Object;Z)Z",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();

        // 2. The search starts from the portal's section when there is one.
        for(String method:new String[]{"init","initWithinWorld"}) {
            m=w.visitMethod(ACC_PRIVATE,"fan4$startSection"+(method.equals("init")?"":"InWorld"),"(L"+VIEWPORT+";)L"+SECTION_POS+";",null,null);
            a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);arr=a.visitArray("method");arr.visit(null,method);arr.visitEnd();a.visit("remap",false);a.visit("require",1);
            at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+VIEWPORT+";getChunkCoord()L"+SECTION_POS+";");at.visit("remap",false);at.visitEnd();a.visitEnd();
            Label none=new Label();
            m.visitCode();m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"fan4$start","L"+SECTION_POS+";");m.visitJumpInsn(IFNULL,none);
            m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"fan4$start","L"+SECTION_POS+";");m.visitInsn(ARETURN);
            m.visitLabel(none);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,VIEWPORT,"getChunkCoord","()L"+SECTION_POS+";",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        }

        // 3. isWithinFrustum (static): tolerate the start section's first failed test.
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$tolerateFrustum","(L"+VIEWPORT+";L"+SECTION+";L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"isWithinFrustum","RETURN",true);
        m.visitCode();m.visitVarInsn(ALOAD,2);
        m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"java/lang/Boolean");m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z",false);
        m.visitMethodInsn(INVOKESTATIC,OCCLUSION,"tolerate","(Z)Z",false);m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(NAME,w);
    }
}
