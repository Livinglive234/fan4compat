import org.objectweb.asm.*;

public final class ShipChunkPacketGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT,OP="com/llamalad7/mixinextras/injector/wrapoperation/Operation",HELPER=ROOT+"immersiveportals/ShipChunkPackets",TARGET="org/valkyrienskies/mod/common/world/ChunkManagement";
    static void generate()throws Exception {
        String name=ROOT+"mixin/immersiveportals/common/ShipChunkPacketMixin";ClassWriter w=GenerateAddon.writer(name,TARGET);
        String args="Lorg/valkyrienskies/core/internal/world/chunks/VsiChunkWatchTask;Lnet/minecraft/class_3218;ZLnet/minecraft/class_1923;";
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$routeWatch","("+args+"L"+OP+";)V",null,null);
        AnnotationVisitor a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;",true),arr=a.visitArray("method");arr.visit(null,"tickChunkLoading$lambda$3$lambda$2("+args+")V");arr.visitEnd();a.visit("remap",false);a.visitEnd();m.visitCode();m.visitVarInsn(ALOAD,0);m.visitVarInsn(ALOAD,1);m.visitVarInsn(ILOAD,2);m.visitVarInsn(ALOAD,3);m.visitVarInsn(ALOAD,4);m.visitMethodInsn(INVOKESTATIC,HELPER,"watch","(Ljava/lang/Object;Ljava/lang/Object;ZLjava/lang/Object;Ljava/lang/Object;)V",false);LogCompatibilityGenerator.end(m);
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$remoteWatchDimension","(Lorg/valkyrienskies/mod/common/util/MinecraftPlayer;)Ljava/lang/String;",null,null);TardisBridgeGenerator.redirect(m,"tickChunkLoading$lambda$3$lambda$2("+args+")V","Lorg/valkyrienskies/mod/common/util/MinecraftPlayer;getDimension()Ljava/lang/String;");m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,HELPER,"watchDimension","(Ljava/lang/Object;)Ljava/lang/String;",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        String accessor="org/valkyrienskies/mod/mixin/accessors/server/level/ChunkMapAccessor";
        m=w.visitMethod(ACC_PRIVATE|ACC_STATIC,"fan4$routeDrop","(L"+accessor+";Lnet/minecraft/class_3222;Lnet/minecraft/class_1923;L"+OP+";)V",null,null);
        a=m.visitAnnotation("Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",true);arr=a.visitArray("method");arr.visit(null,"tickChunkLoading(Lorg/valkyrienskies/core/internal/world/VsiServerShipWorld;Lnet/minecraft/server/MinecraftServer;)V");arr.visitEnd();a.visit("remap",false);a.visit("require",1);AnnotationVisitor at=a.visitAnnotation("at","Lorg/spongepowered/asm/mixin/injection/At;");at.visit("value","INVOKE");at.visit("target","L"+accessor+";callDropChunk(Lnet/minecraft/class_3222;Lnet/minecraft/class_1923;)V");at.visit("remap",false);at.visitEnd();a.visitEnd();m.visitCode();for(int i=0;i<4;i++)m.visitVarInsn(ALOAD,i);m.visitMethodInsn(INVOKESTATIC,HELPER,"drop","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",false);LogCompatibilityGenerator.end(m);GenerateAddon.save(name,w);
    }
}
