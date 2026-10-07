import org.objectweb.asm.*;

public final class ValkyrienSkiesGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT;
    static final String CORE=GenerateAddon.CORE;
    static final String CI=GenerateAddon.CI;
    static final String QUEUE=GenerateAddon.QUEUE;
    static void lifecycle(ClassWriter w,String target,String helper,String at) {
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$"+helper,"(L"+CI+";)V",null,null);GenerateAddon.inject(m,target,at,false);
        m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESTATIC,QUEUE,helper,"(Ljava/lang/Object;)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void dimensionOperation(ClassWriter w,String name,String desc) {
        Type[] params=Type.getArgumentTypes(desc);
        String handler=desc.replace(")V","L"+CI+";)V");
        MethodVisitor m=w.visitMethod(ACC_PRIVATE,"fan4$defer$"+name,handler,null,null);GenerateAddon.inject(m,name+desc,"HEAD",true);m.visitCode();
        m.visitVarInsn(ALOAD,0);m.visitLdcInsn(name);m.visitIntInsn(BIPUSH,params.length);m.visitTypeInsn(ANEWARRAY,"java/lang/Object");
        int local=1;
        for(int i=0;i<params.length;i++) {
            m.visitInsn(DUP);m.visitIntInsn(BIPUSH,i);m.visitVarInsn(params[i].getOpcode(ILOAD),local);
            if(params[i].getSort()==Type.DOUBLE)m.visitMethodInsn(INVOKESTATIC,"java/lang/Double","valueOf","(D)Ljava/lang/Double;",false);
            m.visitInsn(AASTORE);local+=params[i].getSize();
        }
        m.visitMethodInsn(INVOKESTATIC,QUEUE,"defer","(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Z",false);
        Label pass=new Label();m.visitJumpInsn(IFEQ,pass);m.visitVarInsn(ALOAD,local);m.visitMethodInsn(INVOKEVIRTUAL,CI,"cancel","()V",false);m.visitLabel(pass);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    static void dimensionMixin()throws Exception {
        String name=ROOT+"mixin/valkyrienskies/common/DynamicDimensionMixin";ClassWriter w=GenerateAddon.writer(name,CORE);
        lifecycle(w,"preTick()V","openAndFlush","RETURN");
        lifecycle(w,"i()V","close","HEAD");
        lifecycle(w,"destroyWorld()V","forget","RETURN");
        dimensionOperation(w,"addDimension","(Ljava/lang/String;Lorg/valkyrienskies/core/api/world/LevelYRange;Lorg/joml/Vector3dc;DD)V");
        dimensionOperation(w,"updateDimension","(Ljava/lang/String;Lorg/joml/Vector3dc;Ljava/lang/Double;Ljava/lang/Double;)V");
        dimensionOperation(w,"removeDimension","(Ljava/lang/String;)V");GenerateAddon.save(name,w);
    }
}
