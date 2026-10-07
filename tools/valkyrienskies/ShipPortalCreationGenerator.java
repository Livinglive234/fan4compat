import org.objectweb.asm.*;

/** Deny activation before frame searches, remote chunk loads or portal creation. */
public final class ShipPortalCreationGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT, CIR="org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    static void guard(String path,String owner,String selector,String arguments,boolean isStatic,int world,int pos,int callback,boolean optional)throws Exception {
        String name=ROOT+"mixin/"+path;ClassWriter w=GenerateAddon.writer(name,owner);
        MethodVisitor m=w.visitMethod(ACC_PRIVATE|(isStatic?ACC_STATIC:0),"fan4$denyShipPortal","("+arguments+"L"+CIR+";)V",null,null);
        GenerateAddon.inject(m,selector,"HEAD",true);m.visitCode();m.visitVarInsn(ALOAD,world);m.visitVarInsn(ALOAD,pos);
        m.visitMethodInsn(INVOKESTATIC,ROOT+"valkyrienskies/ShipPortalCreation","blocked","(Ljava/lang/Object;Ljava/lang/Object;)Z",false);
        Label allow=new Label();m.visitJumpInsn(IFEQ,allow);m.visitVarInsn(ALOAD,callback);
        if(optional)m.visitMethodInsn(INVOKESTATIC,"java/util/Optional","empty","()Ljava/util/Optional;",false);
        else m.visitFieldInsn(GETSTATIC,"java/lang/Boolean","FALSE","Ljava/lang/Boolean;");
        m.visitMethodInsn(INVOKEVIRTUAL,CIR,"setReturnValue","(Ljava/lang/Object;)V",false);m.visitLabel(allow);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();GenerateAddon.save(name,w);
    }
    static void generate()throws Exception {
        String shapeArgs="Lnet/minecraft/class_1936;Lnet/minecraft/class_2338;Lnet/minecraft/class_2350$class_2351;";
        guard("minecraft/common/ShipNetherPortalCreationMixin","net/minecraft/class_2424","method_30485("+shapeArgs+")Ljava/util/Optional;",shapeArgs,true,0,1,3,true);
        guard("aether/common/ShipAetherPortalCreationMixin","com/aetherteam/aether/block/portal/AetherPortalShape","findEmptyAetherPortalShape("+shapeArgs+")Ljava/util/Optional;",shapeArgs,true,0,1,3,true);
        String args="Lnet/minecraft/class_3218;Lnet/minecraft/class_2338;";
        guard("immersiveportals/common/ShipNetherPortalActivationMixin","qouteall/imm_ptl/core/portal/nether_portal/NetherPortalGeneration","checkPortalGeneration("+args+")Z",args,true,0,1,2,false);
        args="Lqouteall/imm_ptl/core/portal/custom_portal_gen/CustomPortalGeneration;Lnet/minecraft/class_3218;Lnet/minecraft/class_2338;Lnet/minecraft/class_3218;Lnet/minecraft/class_1297;";
        guard("immersiveportals/common/ShipFramePortalActivationMixin","qouteall/imm_ptl/core/portal/custom_portal_gen/form/NetherPortalLikeForm","perform("+args+")Z",args,false,2,3,6,false);
    }
}
