import org.objectweb.asm.*;

/**
 * Sodium 0.8 RenderSectionManager: gives Valkyrien Skies' ships their render lists again. VS's own mixins for this (written for Sodium 0.6) are cancelled for Sodium 0.8
 * (CompatPlugin) and this one does the four jobs through sodium/ShipSections: build a render list per ship after Sodium builds the world's, tick them with the world's,
 * clear them when Sodium resets its lists, and draw them in renderLayer. It also implements VS's RenderSectionManagerDuck, which VS's other Sodium mixins read the ship
 * lists through. Applied only for Sodium 0.8.x with a supported Valkyrien Skies (CompatibilityRules).
 */
public final class SodiumShipGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,SODIUM="net/caffeinemc/mods/sodium/client/",MANAGER=SODIUM+"render/chunk/RenderSectionManager",SECTION=SODIUM+"render/chunk/RenderSection",
        LISTS=SODIUM+"render/chunk/lists/SortedRenderLists",COLLECTOR=SODIUM+"render/chunk/lists/SectionCollector",RENDERER=SODIUM+"render/chunk/ChunkRenderer",
        SORT=SODIUM+"render/chunk/translucent_sorting/SortBehavior",MATRICES=SODIUM+"render/chunk/ChunkRenderMatrices",PASS=SODIUM+"render/chunk/terrain/TerrainRenderPass",
        COMMANDS=SODIUM+"gl/device/CommandList",VIEWPORT=SODIUM+"render/viewport/Viewport",DUCK="org/valkyrienskies/mod/mixinducks/mod_compat/sodium/RenderSectionManagerDuck",
        SHIPS=ROOT+"sodium/ShipSections",LOOKUP=SHIPS+"$SectionLookup",CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
        OPERATION="com/llamalad7/mixinextras/injector/wrapoperation/Operation",NAME=ROOT+"mixin/sodium/client/SodiumShipRenderingMixin";
    static void generate()throws Exception {
        ClassWriter w=GenerateAddon.writer(NAME,MANAGER,DUCK,LOOKUP);
        // Shadowed members of RenderSectionManager. The final ones are marked @Final, as the Mixin docs ask.
        shadowField(w,"level","Lnet/minecraft/class_638;",true);
        shadowField(w,"chunkRenderer","L"+RENDERER+";",true);
        shadowField(w,"sortBehavior","L"+SORT+";",true);
        shadowField(w,"renderLists","L"+LISTS+";",false);
        shadowField(w,"taskLists","Ljava/util/Map;",false);
        shadowField(w,"sectionCollector","L"+COLLECTOR+";",false);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"getRenderSection","(III)L"+SECTION+";",null,null);
        m.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;",true).visitEnd();
        m.visitCode();m.visitInsn(ACONST_NULL);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();

        // VS's duck: the ship lists of this manager.
        m=w.visitMethod(ACC_PUBLIC,"vs_getShipRenderLists","()Ljava/util/WeakHashMap;",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,SHIPS,"lists","(Ljava/lang/Object;)Ljava/util/WeakHashMap;",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        // ShipSections.SectionLookup: Sodium's private section lookup.
        m=w.visitMethod(ACC_PUBLIC,"fan4$section","(III)Ljava/lang/Object;",null,null);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ILOAD,1);m.visitVarInsn(ILOAD,2);m.visitVarInsn(ILOAD,3);
        m.visitMethodInsn(INVOKEVIRTUAL,NAME,"getRenderSection","(III)L"+SECTION+";",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();

        // 1. After the world's render lists are built: one list per ship, and the answer to "does the graph need another pass".
        m=w.visitMethod(ACC_PRIVATE,"fan4$shipRenderLists","(Lnet/minecraft/class_4184;L"+VIEWPORT+";IZL"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"createTerrainRenderList","RETURN",true);
        m.visitCode();
        m.visitVarInsn(ALOAD,5);
        m.visitVarInsn(ALOAD,0);
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"level","Lnet/minecraft/class_638;");
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"taskLists","Ljava/util/Map;");
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"sectionCollector","L"+COLLECTOR+";");
        m.visitVarInsn(ALOAD,2);m.visitVarInsn(ILOAD,3);
        m.visitVarInsn(ALOAD,5);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);m.visitTypeInsn(CHECKCAST,"java/lang/Boolean");m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Boolean","booleanValue","()Z",false);
        m.visitVarInsn(ALOAD,0);
        m.visitMethodInsn(INVOKESTATIC,SHIPS,"afterCreate","(L"+LOOKUP+";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;IZLjava/lang/Object;)Z",false);
        m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // 2. Ticking the visible renders (animated sprites): the world's lists, then each ship's.
        m=w.visitMethod(ACC_PRIVATE,"fan4$tickShipRenders","(L"+OPERATION+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true),arr=a.visitArray("method");arr.visit(null,"tickVisibleRenders");arr.visitEnd();
        a.visit("remap",false);a.visitEnd();
        m.visitCode();
        callOriginal(m);
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"renderLists","L"+LISTS+";");m.visitVarInsn(ASTORE,2);
        m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,SHIPS,"shipLists","(Ljava/lang/Object;)[Ljava/lang/Object;",false);m.visitVarInsn(ASTORE,3);
        m.visitInsn(ICONST_0);m.visitVarInsn(ISTORE,4);
        Label loop=new Label(),done=new Label();
        m.visitLabel(loop);m.visitVarInsn(ILOAD,4);m.visitVarInsn(ALOAD,3);m.visitInsn(ARRAYLENGTH);m.visitJumpInsn(IF_ICMPGE,done);
        m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,3);m.visitVarInsn(ILOAD,4);m.visitInsn(AALOAD);m.visitTypeInsn(CHECKCAST,LISTS);m.visitFieldInsn(PUTFIELD,NAME,"renderLists","L"+LISTS+";");
        callOriginal(m);
        m.visitIincInsn(4,1);m.visitJumpInsn(GOTO,loop);
        m.visitLabel(done);
        m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,2);m.visitFieldInsn(PUTFIELD,NAME,"renderLists","L"+LISTS+";");
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // 3. Sodium resets its lists: so do the ships'.
        m=w.visitMethod(ACC_PRIVATE,"fan4$clearShipRenderLists","(L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"resetRenderLists","TAIL",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,SHIPS,"clear","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();

        // 4. Drawing a layer: just before Sodium flushes its command list, draw each ship's lists with the ship's transform.
        // Locals: this 0, matrices 1, pass 2, x 3-4, y 5-6, z 7-8, callback 9, command list 10 (captured), "sorted" 11.
        m=w.visitMethod(ACC_PRIVATE,"fan4$drawShips","(L"+MATRICES+";L"+PASS+";DDDL"+GenerateAddon.CI+";L"+COMMANDS+";)V",null,null);
        a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;",true);arr=a.visitArray("method");arr.visit(null,"renderLayer");arr.visitEnd();
        a.visit("remap",false);a.visit("require",1);
        arr=a.visitArray("at");AnnotationVisitor at=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/At;");
        at.visit("value","INVOKE");at.visit("target","L"+COMMANDS+";flush()V");at.visit("remap",false);at.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitParameterAnnotation(6,"Lcom/llamalad7/mixinextras/sugar/Local;",false).visitEnd();
        m.visitCode();
        Label off=new Label(),set=new Label();
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"sortBehavior","L"+SORT+";");m.visitFieldInsn(GETSTATIC,SORT,"OFF","L"+SORT+";");
        m.visitJumpInsn(IF_ACMPEQ,off);m.visitInsn(ICONST_1);m.visitJumpInsn(GOTO,set);m.visitLabel(off);m.visitInsn(ICONST_0);m.visitLabel(set);m.visitVarInsn(ISTORE,11);
        m.visitVarInsn(ALOAD,0);
        m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,NAME,"chunkRenderer","L"+RENDERER+";");
        m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,10);m.visitVarInsn(ALOAD,2);
        m.visitVarInsn(DLOAD,3);m.visitVarInsn(DLOAD,5);m.visitVarInsn(DLOAD,7);m.visitVarInsn(ILOAD,11);
        m.visitMethodInsn(INVOKESTATIC,SHIPS,"renderShips","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;DDDZ)V",false);
        m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(NAME,w);
    }
    static void shadowField(ClassWriter w,String name,String desc,boolean isFinal) {
        FieldVisitor f=w.visitField(ACC_PRIVATE,name,desc,null,null);
        f.visitAnnotation("Lorg/spongepowered/asm/mixin/Shadow;",true).visitEnd();
        if(isFinal)f.visitAnnotation("Lorg/spongepowered/asm/mixin/Final;",true).visitEnd();
        f.visitEnd();
    }
    /** original.call(new Object[0]) with the result discarded. */
    static void callOriginal(MethodVisitor m) {
        m.visitVarInsn(ALOAD,1);m.visitInsn(ICONST_0);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        m.visitMethodInsn(INVOKEINTERFACE,OPERATION,"call","([Ljava/lang/Object;)Ljava/lang/Object;",true);m.visitInsn(POP);
    }
}
