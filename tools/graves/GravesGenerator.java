import org.objectweb.asm.*;
import dev.fan4.compat.graves.AccessoryGraveCompat;

/** Native descriptors pinned to Player Graves 1.0.0 and Accessories beta 48. */
public final class GravesGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,HELPER=ROOT+"graves/AccessoryGraveCompat",CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static void constant(ClassWriter w,String handler,String[] methods,int from,int to) {
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,handler,"(I)I",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",true),arr=a.visitArray("method");
        for(String method:methods)arr.visit(null,method);arr.visitEnd();a.visit("require",1);a.visit("remap",false);
        arr=a.visitArray("constant");AnnotationVisitor c=arr.visitAnnotation(null,"Lorg/spongepowered/asm/mixin/injection/Constant;");c.visit("intValue",from);c.visitEnd();arr.visitEnd();a.visitEnd();
        m.visitCode();m.visitLdcInsn(to);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void returnValue(MethodVisitor m,String selector,String at) {
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",true),arr=a.visitArray("method");arr.visit(null,selector);arr.visitEnd();a.visit("require",1);a.visit("remap",false);
        AnnotationVisitor point=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");point.visit("value",at);point.visitEnd();a.visitEnd();
    }
    public static void generate()throws Exception {
        String name=ROOT+"mixin/graves/common/AccessoryGraveStorageMixin",grave="com/kador/graves/entity/GraveEntity";
        ClassWriter w=GenerateAddon.writer(name,grave);
        constant(w,"fan4$storageSize",new String[]{"<init>","captureInventory","getStoredStack","quickRetrieveAll"},45,AccessoryGraveCompat.STORAGE);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$capture","(Lnet/minecraft/class_3222;L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,"createFromPlayer","RETURN",false);m.visitCode();m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"capture","(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$syncWindow","(Lnet/minecraft/class_1263;L"+GenerateAddon.CI+";)V",null,null);
        GenerateAddon.inject(m,"syncFromInventory(Lnet/minecraft/class_1263;)V","HEAD",true);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"sync","(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,GenerateAddon.CI,"cancel","()V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$retrieveAccessories","(ILnet/minecraft/class_3222;)I",null,null);returnValue(m,"quickRetrieveExact","RETURN");
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"retrieveExtra","(ILjava/lang/Object;Ljava/lang/Object;)I",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$prepareWindow","(Lnet/minecraft/class_3222;L"+GenerateAddon.CI+";)V",null,null);GenerateAddon.inject(m,"openInventoryScreen","HEAD",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"prepareScreen","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$beginQuickRetrieve","(Lnet/minecraft/class_3222;L"+CIR+";)V",null,null);GenerateAddon.inject(m,"quickRetrieveAll","HEAD",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"beginQuickRetrieve","(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PRIVATE,"fan4$finishQuickRetrieve","(ILnet/minecraft/class_3222;)I",null,null);returnValue(m,"quickRetrieveAll","RETURN");
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKESTATIC,HELPER,"endQuickRetrieve","(ILjava/lang/Object;Ljava/lang/Object;)I",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();
        for(String[] persistence:new String[][]{{"method_5652","saveSlots"},{"method_5749","loadSlots"}}) {
            m=w.visitMethod(ACC_PRIVATE,"fan4$"+persistence[1],"(Lnet/minecraft/class_2487;L"+GenerateAddon.CI+";)V",null,null);GenerateAddon.inject(m,persistence[0]+"(Lnet/minecraft/class_2487;)V","RETURN",false);
            m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,persistence[1],"(Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        String lambda="lambda$openInventoryScreen$0";
        constant(w,"fan4$sixRows",new String[]{lambda},5,6);
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$sixRowType","(Lnet/minecraft/class_3917;)Lnet/minecraft/class_3917;",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",true),arr=a.visitArray("method");arr.visit(null,lambda);arr.visitEnd();a.visit("require",1);a.visit("remap",false);
        AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","FIELD");at.visit("target","Lnet/minecraft/class_3917;field_18667:Lnet/minecraft/class_3917;");at.visit("remap",false);at.visitEnd();a.visitEnd();
        m.visitCode();m.visitFieldInsn(GETSTATIC,"net/minecraft/class_3917","field_17327","Lnet/minecraft/class_3917;");m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/graves/common/AccessoryGraveScreenMixin";w=GenerateAddon.writer(name,"com/kador/graves/inventory/GraveInventory");
        constant(w,"fan4$screenSize",new String[]{"<init>"},45,AccessoryGraveCompat.VISIBLE);
        GenerateAddon.save(name,w);
        name=ROOT+"mixin/graves/common/AccessoryGraveSpawnMixin";w=GenerateAddon.writer(name,"net/minecraft/class_3218");
        m=w.visitMethod(ACC_PRIVATE,"fan4$commitAfterSpawn","(Lnet/minecraft/class_1297;L"+CIR+";)V",null,null);GenerateAddon.inject(m,"method_8649(Lnet/minecraft/class_1297;)Z","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValueZ","()Z",false);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKESTATIC,HELPER,"spawned","(ZLjava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/graves/common/AccessoryGraveDeathSlotsMixin";w=GenerateAddon.writer(name,"io/wispforest/accessories/impl/AccessoriesEventHandler");
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$beginDeathSlots","(Lnet/minecraft/class_1309;Lnet/minecraft/class_1282;L"+CIR+";)V",null,null);GenerateAddon.inject(m,"onDeath","HEAD",false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"beginDrops","(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        String dropArgs="(Lio/wispforest/accessories/api/DropRule;Lnet/minecraft/class_1309;Lio/wispforest/accessories/impl/ExpandedSimpleContainer;Lio/wispforest/accessories/api/slot/SlotReference;Lnet/minecraft/class_1282;Z";
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$recordDeathSlot",dropArgs+"L"+CIR+";)V",null,null);GenerateAddon.inject(m,"dropStack"+dropArgs+")Lnet/minecraft/class_1799;","RETURN",false);
        m.visitCode();m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,3);m.visitVarInsn(ALOAD,6);m.visitMethodInsn(INVOKEVIRTUAL,CIR,"getReturnValue","()Ljava/lang/Object;",false);m.visitMethodInsn(INVOKESTATIC,HELPER,"recordDrop","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
        name=ROOT+"mixin/graves/common/AccessoryGraveEmptyMixin";w=GenerateAddon.writer(name,"net/minecraft/class_1661");
        m=w.visitMethod(ACC_PRIVATE,"fan4$pendingAccessories","(Z)Z",null,null);returnValue(m,"method_5442()Z","RETURN");
        m.visitCode();m.visitVarInsn(ILOAD,1);m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"inventoryEmpty","(ZLjava/lang/Object;)Z",false);m.visitInsn(IRETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
}
