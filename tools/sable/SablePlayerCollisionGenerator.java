import org.objectweb.asm.*;

/**
 * Immersive Portals replaces Player.canPlayerFitWithinBlocksAndEntitiesWhen (method_52558) with an @Overwrite (qouteall.imm_ptl.core.mixin.common.collision.MixinPlayer_Collision):
 * the pose's box is limited to the part of the entity's box that is not through a portal before it is tested for collisions. Sable wraps the noCollision call in that
 * method, and Mixin does not let a mixin inject into a method another mixin has merged unless its own priority is higher; both are 1000, so Mixin refuses and the game
 * does not start. This is the same method with the same body at a lower priority (CompatPlugin cancels Immersive Portals' class), so Sable's wrap applies to it.
 * Applied for Sable 2.0.6 with Immersive Portals (CompatibilityRules).
 */
public final class SablePlayerCollisionGenerator implements Opcodes {
    static final String PLAYER="net/minecraft/class_1657",LIVING="net/minecraft/class_1309",POSE="net/minecraft/class_4050",DIMENSIONS="net/minecraft/class_4048",VEC3="net/minecraft/class_243",
        BOX="net/minecraft/class_238",LEVEL="net/minecraft/class_1937",ENTITY="net/minecraft/class_1297",DUCK="qouteall/imm_ptl/core/ducks/IEEntity",
        NAME=GenerateAddon.ROOT+"mixin/sableip/common/IpPlayerCollisionMixin";
    static void generate()throws Exception {
        ClassWriter w=GenerateAddon.writer(NAME,PLAYER,900);
        MethodVisitor m=w.visitMethod(ACC_PUBLIC,"method_52558","(L"+POSE+";)Z",null,null);
        m.visitAnnotation("Lorg/spongepowered/asm/mixin/Overwrite;",true).visitEnd();
        Label active=new Label();
        m.visitCode();
        m.visitVarInsn(ALOAD,0);m.visitTypeInsn(CHECKCAST,LIVING);m.visitVarInsn(ASTORE,2);
        // AABB box = self.getDimensions(pose).makeBoundingBox(self.position());
        m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,1);m.visitMethodInsn(INVOKEVIRTUAL,LIVING,"method_18377","(L"+POSE+";)L"+DIMENSIONS+";",false);
        m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,LIVING,"method_19538","()L"+VEC3+";",false);
        m.visitMethodInsn(INVOKEVIRTUAL,DIMENSIONS,"method_30757","(L"+VEC3+";)L"+BOX+";",false);m.visitVarInsn(ASTORE,3);
        // AABB limited = ((IEEntity)self).ip_getActiveCollisionBox(box);
        m.visitVarInsn(ALOAD,2);m.visitTypeInsn(CHECKCAST,DUCK);m.visitVarInsn(ALOAD,3);
        m.visitMethodInsn(INVOKEINTERFACE,DUCK,"ip_getActiveCollisionBox","(L"+BOX+";)L"+BOX+";",true);m.visitVarInsn(ASTORE,4);
        // if (limited == null) return true;
        m.visitVarInsn(ALOAD,4);m.visitJumpInsn(IFNONNULL,active);m.visitInsn(ICONST_1);m.visitInsn(IRETURN);
        // return self.level().noCollision(self, limited.inflate(1.0E-7));
        m.visitLabel(active);
        m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEVIRTUAL,LIVING,"method_37908","()L"+LEVEL+";",false);
        m.visitVarInsn(ALOAD,2);m.visitVarInsn(ALOAD,4);m.visitLdcInsn(1.0E-7);m.visitMethodInsn(INVOKEVIRTUAL,BOX,"method_1011","(D)L"+BOX+";",false);
        m.visitMethodInsn(INVOKEVIRTUAL,LEVEL,"method_8587","(L"+ENTITY+";L"+BOX+";)Z",false);m.visitInsn(IRETURN);
        m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(NAME,w);
    }
}
