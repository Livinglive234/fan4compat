import org.objectweb.asm.*;

/**
 * Sodium 0.8 Viewport: puts Immersive Portals' extra portal culling (PortalCulling) on the four Frustum calls its box tests make. Applied only for Sodium 0.8.x with
 * Immersive Portals (CompatibilityRules); IP's own redirect of the old Viewport.isBoxVisible is cancelled for the same combination (CompatPlugin).
 */
public final class SodiumViewportGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,VIEWPORT="net/caffeinemc/mods/sodium/client/render/viewport/Viewport",FRUSTUM="net/caffeinemc/mods/sodium/client/render/viewport/frustum/Frustum",CULLING=ROOT+"sodium/PortalCulling";
    static void generate()throws Exception {
        String name=ROOT+"mixin/sodium/client/SodiumViewportCullingMixin";
        ClassWriter w=GenerateAddon.writer(name,VIEWPORT);
        // {method, frustum call, call descriptor, helper descriptor, handler descriptor, kind}
        String[][] hooks={
            {"isBoxVisible(III)Z","testSection","(FFF)Z","section","(ZFFFF)Z","(L"+FRUSTUM+";FFF)Z","section"},
            {"isBoxVisibleLooser(III)Z","testSectionExpanded","(FFFF)Z","section","(ZFFFF)Z","(L"+FRUSTUM+";FFFF)Z","looser"},
            {"isBoxVisibleDirect(FFFF)Z","testAab","(FFFFFF)Z","box","(ZFFFFFF)Z","(L"+FRUSTUM+";FFFFFF)Z","box"},
            {"getBoxIntersectionDirect(FFFF)I","intersectAab","(FFFFFF)I","intersect","(IFFFFFF)I","(L"+FRUSTUM+";FFFFFF)I","box"}};
        for(int i=0;i<hooks.length;i++) {
            String[] h=hooks[i];boolean looser=h[6].equals("looser"),section=h[6].equals("section")||looser;
            MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$portalCulling"+i,h[5],null,null);
            AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true),arr=a.visitArray("method");arr.visit(null,h[0]);arr.visitEnd();
            a.visit("remap",false);a.visit("require",1);
            AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+FRUSTUM+";"+h[1]+h[2]);at.visit("remap",false);at.visitEnd();a.visitEnd();
            m.visitCode();
            int args=Type.getArgumentTypes(h[5]).length-1;
            m.visitVarInsn(ALOAD,1);for(int f=0;f<args;f++)m.visitVarInsn(FLOAD,2+f);
            m.visitMethodInsn(INVOKEINTERFACE,FRUSTUM,h[1],h[2],true);
            if(section) {
                for(int f=0;f<3;f++)m.visitVarInsn(FLOAD,2+f);
                m.visitLdcInsn(9.125f);if(looser){m.visitVarInsn(FLOAD,5);m.visitInsn(FADD);}
            } else for(int f=0;f<6;f++)m.visitVarInsn(FLOAD,2+f);
            m.visitMethodInsn(INVOKESTATIC,CULLING,h[3],h[4],false);
            m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        GenerateAddon.save(name,w);
    }
}
