import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Generates standalone client bridges for the exact Fabric 1.21.1 mod builds. */
public class ClientBridgeGenerator implements Opcodes {
    static final String ROOT = "org/valkyrienskies/mod/";
    static final String MIX = ROOT + "mixin/mod_compat/immersive_portals/";
    static final String UTILS = ROOT + "common/VSGameUtilsKt";
    static final String VEC = "net/minecraft/class_243";
    static final String MC = "net/minecraft/class_310";
    static final String LEVEL = "net/minecraft/class_638";
    static final String CIR = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static final String HELPER = ROOT + "compat/ImmersivePortalsShipBridge";
    static final String DIST = "(L"+VEC+";L"+VEC+";)D";
    static final Path OUT = Path.of(System.getProperty("fan4compat.clientIntermediate", "build/generated/client-intermediate"));

    static ClassWriter writer(String name) {
        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES) {
            protected String getCommonSuperClass(String a, String b) { return "java/lang/Object"; }
        };
        w.visit(V17, ACC_PUBLIC | ACC_SUPER, name, null, "java/lang/Object", null);
        return w;
    }
    static void save(String name, ClassWriter w) throws Exception {
        w.visitEnd(); Path p = OUT.resolve(name+".class"); Files.createDirectories(p.getParent()); Files.write(p,w.toByteArray());
    }
    static void mixin(ClassWriter w, String target) {
        AnnotationVisitor a = w.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;",false);
        AnnotationVisitor arr = a.visitArray("targets");arr.visit(null,target.replace('/','.'));arr.visitEnd();
        a.visit("remap",false);a.visitEnd();
    }
    static void redirect(MethodVisitor m, String method, String target) {
        AnnotationVisitor a = m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Redirect;",true);
        AnnotationVisitor arr = a.visitArray("method"); arr.visit(null,method);arr.visitEnd();
        a.visit("remap",false);a.visit("require",1);
        AnnotationVisitor at = a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value","INVOKE");at.visit("target",target);at.visit("remap",false);at.visitEnd();a.visitEnd();
    }
    static void currentLevel(MethodVisitor m) {
        m.visitMethodInsn(INVOKESTATIC,MC,"method_1551","()L"+MC+";",false);
        m.visitFieldInsn(GETFIELD,MC,"field_1687","L"+LEVEL+";");
    }
    static void distanceHelper() throws Exception {
        ClassWriter w = writer(HELPER);
        MethodVisitor m = w.visitMethod(ACC_PUBLIC | ACC_STATIC,"distanceSquared",DIST,null,null);m.visitCode();
        currentLevel(m);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitInsn(ACONST_NULL);
        m.visitMethodInsn(INVOKESTATIC,UTILS,"squaredDistanceBetweenInclShips","(Lnet/minecraft/class_1937;L"+VEC+";L"+VEC+";Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)D",false);
        m.visitInsn(DRETURN);m.visitMaxs(0,0);m.visitEnd();

        // IP's replacement chunk manager bypasses the ordinary VS initialization hook.
        // Notify per-world rendering; initialize connectivity only in the active world.
        // VS's existing
        // queue drains against Minecraft.level rather than an arbitrary portal world.
        m = w.visitMethod(ACC_PUBLIC | ACC_STATIC,"chunkLoaded","(Lnet/minecraft/class_2818;)V",null,null);m.visitCode();
        m.visitVarInsn(ALOAD,0);Label done = new Label();m.visitJumpInsn(IFNULL,done);
        m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,"net/minecraft/class_2818","method_12200","()Lnet/minecraft/class_1937;",false);
        m.visitTypeInsn(CHECKCAST,LEVEL);m.visitVarInsn(ASTORE,1);
        m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,"net/minecraft/class_2818","method_12004","()Lnet/minecraft/class_1923;",false);m.visitVarInsn(ASTORE,2);
        m.visitVarInsn(ALOAD,1);
        m.visitVarInsn(ALOAD,2);m.visitFieldInsn(GETFIELD,"net/minecraft/class_1923","field_9181","I");
        m.visitVarInsn(ALOAD,2);m.visitFieldInsn(GETFIELD,"net/minecraft/class_1923","field_9180","I");
        m.visitMethodInsn(INVOKESTATIC,UTILS,"isChunkInShipyard","(Lnet/minecraft/class_1937;II)Z",false);m.visitJumpInsn(IFEQ,done);
        // Sodium's tracker belongs to the chunk's own world, including portal views.
        m.visitVarInsn(ALOAD,1);
        m.visitVarInsn(ALOAD,2);m.visitFieldInsn(GETFIELD,"net/minecraft/class_1923","field_9181","I");
        m.visitVarInsn(ALOAD,2);m.visitFieldInsn(GETFIELD,"net/minecraft/class_1923","field_9180","I");
        m.visitMethodInsn(INVOKESTATIC,ROOT+"compat/SodiumCompat","onChunkAdded","(L"+LEVEL+";II)V",false);
        // The original VS connectivity queue still drains only against the active world.
        m.visitVarInsn(ALOAD,1);currentLevel(m);m.visitJumpInsn(IF_ACMPNE,done);
        m.visitVarInsn(ALOAD,2);m.visitInsn(ICONST_1);
        m.visitMethodInsn(INVOKESTATIC,ROOT+"util/ClientConnectivityUpdateQueue","queueChunkForInitialization","(Lnet/minecraft/class_1923;Z)V",false);
        m.visitMethodInsn(INVOKESTATIC,ROOT+"common/BlockStateInfo","isSortedRegistryInitialized","()Z",false);
        Label deferred = new Label();m.visitJumpInsn(IFEQ,deferred);
        m.visitFieldInsn(GETSTATIC,ROOT+"util/ClientConnectivityUpdateQueue","INSTANCE","L"+ROOT+"util/ClientConnectivityUpdateQueue;");
        m.visitMethodInsn(INVOKEVIRTUAL,ROOT+"util/ClientConnectivityUpdateQueue","onRegistriesCompleted","()V",false);
        m.visitLabel(deferred);
        m.visitLabel(done);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();save(HELPER,w);
    }
    static void rangeMixin() throws Exception {
        String name = MIX+"MixinShipPickRange";ClassWriter w = writer(name);mixin(w,"net/minecraft/class_757");
        MethodVisitor m = w.visitMethod(ACC_PRIVATE | ACC_STATIC,"vsip$withinShipReach","(L"+VEC+";Lnet/minecraft/class_2374;D)Z",null,null);
        redirect(m,"method_56154(Lnet/minecraft/class_239;L"+VEC+";D)Lnet/minecraft/class_239;","L"+VEC+";method_24802(Lnet/minecraft/class_2374;D)Z");
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitTypeInsn(CHECKCAST,VEC);
        m.visitMethodInsn(INVOKESTATIC,HELPER,"distanceSquared",DIST,false);
        m.visitVarInsn(DLOAD,2);m.visitVarInsn(DLOAD,2);m.visitInsn(DMUL);m.visitInsn(DCMPG);
        Label no = new Label();m.visitJumpInsn(IFGE,no);m.visitInsn(ICONST_1);m.visitInsn(IRETURN);m.visitLabel(no);m.visitInsn(ICONST_0);m.visitInsn(IRETURN);
        m.visitMaxs(0,0);m.visitEnd();save(name,w);
    }
    static void portalDistanceMixin() throws Exception {
        String name = MIX+"MixinShipPortalTargetDistance";ClassWriter w = writer(name);mixin(w,"qouteall/imm_ptl/core/block_manipulation/BlockManipulationClient");
        MethodVisitor m = w.visitMethod(ACC_PRIVATE | ACC_STATIC,"vsip$shipTargetDistance",DIST,null,null);
        redirect(m,"getCurrentTargetDistance()D","L"+VEC+";method_1022(L"+VEC+";)D");
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"distanceSquared",DIST,false);
        m.visitMethodInsn(INVOKESTATIC,"java/lang/Math","sqrt","(D)D",false);m.visitInsn(DRETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"vsip$portalIntersectionDistance",DIST,null,null);
        redirect(m,"lambda$updatePointedBlock$1(L"+VEC+";FDLcom/mojang/datafixers/util/Pair;)V","L"+VEC+";method_1022(L"+VEC+";)D");
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"distanceSquared",DIST,false);m.visitMethodInsn(INVOKESTATIC,"java/lang/Math","sqrt","(D)D",false);m.visitInsn(DRETURN);m.visitMaxs(0,0);m.visitEnd();save(name,w);
    }
    static void chunkMixin() throws Exception {
        String name = MIX+"MixinShipClientChunkLifecycle";ClassWriter w = writer(name);mixin(w,"qouteall/imm_ptl/core/chunk_loading/ImmPtlClientChunkMap");
        MethodVisitor m = w.visitMethod(ACC_PRIVATE,"vsip$initializeShipChunk","(IILnet/minecraft/class_2540;Lnet/minecraft/class_2487;Ljava/util/function/Consumer;L"+CIR+";)V",null,null);
        AnnotationVisitor a = m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);
        AnnotationVisitor arr = a.visitArray("method");arr.visit(null,"method_16020(IILnet/minecraft/class_2540;Lnet/minecraft/class_2487;Ljava/util/function/Consumer;)Lnet/minecraft/class_2818;");arr.visitEnd();
        a.visit("remap",false);a.visit("require",1);
        arr = a.visitArray("at");AnnotationVisitor at = arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","RETURN");at.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitCode();m.visitVarInsn(ALOAD,6);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);
        m.visitTypeInsn(CHECKCAST,"net/minecraft/class_2818");m.visitMethodInsn(INVOKESTATIC,HELPER,"chunkLoaded","(Lnet/minecraft/class_2818;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();save(name,w);
    }
    static void placementMixin()throws Exception {
        String name=MIX+"MixinShipRemotePlacement",helper="dev/fan4/compat/immersiveportals/PortalInteractionCompat",op="com/llamalad7/mixinextras/injector/wrapoperation/Operation";
        ClassWriter w=writer(name);mixin(w,"qouteall/imm_ptl/core/block_manipulation/BlockManipulationClient");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$remotePlacement","(Ljava/util/function/Supplier;ZL"+op+";)Ljava/lang/Object;",null,null);
        AnnotationVisitor annotation=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true),selectors=annotation.visitArray("method");
        selectors.visit(null,"withSwitchedContext(Ljava/util/function/Supplier;Z)Ljava/lang/Object;");selectors.visitEnd();annotation.visit("remap",false);annotation.visitEnd();
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitMethodInsn(INVOKESTATIC,helper,"begin","(Z)Ljava/lang/Object;",false);m.visitVarInsn(ASTORE,3);
        Label start=new Label(),end=new Label(),failure=new Label();m.visitTryCatchBlock(start,end,failure,null);m.visitLabel(start);
        m.visitVarInsn(ALOAD,2);m.visitInsn(ICONST_2);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");m.visitInsn(DUP);m.visitInsn(ICONST_0);m.visitVarInsn(ALOAD,0);m.visitInsn(AASTORE);m.visitInsn(DUP);m.visitInsn(ICONST_1);m.visitVarInsn(ILOAD,1);m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);m.visitInsn(AASTORE);
        m.visitMethodInsn(INVOKEINTERFACE,op,"call","([Ljava/lang/Object;)Ljava/lang/Object;",true);m.visitVarInsn(ASTORE,4);m.visitLabel(end);
        m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKESTATIC,helper,"end","(Ljava/lang/Object;)V",false);m.visitVarInsn(ALOAD,4);m.visitInsn(ARETURN);
        m.visitLabel(failure);m.visitVarInsn(ASTORE,5);m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKESTATIC,helper,"end","(Ljava/lang/Object;)V",false);m.visitVarInsn(ALOAD,5);m.visitInsn(ATHROW);m.visitMaxs(0,0);m.visitEnd();save(name,w);
    }
    public static void main(String[] args) throws Exception {
        distanceHelper();rangeMixin();portalDistanceMixin();chunkMixin();placementMixin();
    }
}
