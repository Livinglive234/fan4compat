import dev.fan4.compat.shared.CompatCalls;
import org.objectweb.asm.*;

/** Reproduce unflagged covariant adapters emitted by the shipped VS core. */
public class CompatCallsTest implements Opcodes {
    public static class Overloaded {
        public String choose(String value){return "string";}
        public String choose(Object value){return "object";}
        public String fail(){throw new IllegalArgumentException("original failure");}
    }
    public static class Constructors {final String chosen;public Constructors(String s){chosen="string";}public Constructors(Object s){chosen="object";}}
    public static class TypedBase {public Comparable<?> property(Object key){return true;}}
    static Object poisonedState()throws Exception {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(V17,ACC_PUBLIC,"PoisonedState",null,"CompatCallsTest$TypedBase",null);
        MethodVisitor m=w.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,"CompatCallsTest$TypedBase","<init>","()V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"unrelated","(Lmissing/OptionalPlantable;)V",null,null);m.visitCode();m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        m=w.visitMethod(ACC_PUBLIC,"property","(Ljava/lang/Object;)Ljava/lang/Comparable;",null,null);m.visitCode();m.visitInsn(ICONST_0);m.visitMethodInsn(INVOKESTATIC,"java/lang/Boolean","valueOf","(Z)Ljava/lang/Boolean;",false);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        w.visitEnd();byte[] bytes=w.toByteArray();Class<?> c=new ClassLoader(CompatCallsTest.class.getClassLoader()){Class<?> define(){return defineClass("PoisonedState",bytes,0,bytes.length);}}.define();return c.getConstructor().newInstance();
    }
    static void check(boolean ok){if(!ok)throw new AssertionError("Reflection resolution regression");}
    static Object fixture(String name,String[] returns,boolean bridge)throws Exception {
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(V17,ACC_PUBLIC,name,null,"java/lang/Object",null);
        MethodVisitor m=w.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        for(int i=0;i<returns.length;i++) {
            m=w.visitMethod(ACC_PUBLIC|(bridge?ACC_BRIDGE|ACC_SYNTHETIC:0),"getAllShips","()L"+returns[i]+";",null,null);m.visitCode();
            if(returns[i].equals("java/lang/Integer")){m.visitInsn(ICONST_1);m.visitMethodInsn(INVOKESTATIC,"java/lang/Integer","valueOf","(I)Ljava/lang/Integer;",false);}
            else m.visitLdcInsn("adapter-"+i);
            m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        }
        w.visitEnd();byte[] bytes=w.toByteArray();
        Class<?> c=new ClassLoader(CompatCallsTest.class.getClassLoader()){Class<?> define(){return defineClass(name,bytes,0,bytes.length);}}.define();return c.getConstructor().newInstance();
    }
    public static void main(String[] args)throws Exception {
        Object poisoned=poisonedState();
        try{poisoned.getClass().getMethods();throw new AssertionError("missing optional signature did not reproduce");}catch(NoClassDefFoundError expected){}
        check(Boolean.FALSE.equals(CompatCalls.callTyped(TypedBase.class.getName(),"property","java.lang.Comparable",poisoned,new String[]{"java.lang.Object"},new Object())));
        check(Boolean.FALSE.equals(CompatCalls.callTyped(TypedBase.class.getName(),"property","java.lang.Comparable",poisoned,new String[]{"java.lang.Object"},new Object())));
        Constructors selected=(Constructors)CompatCalls.createExact(Constructors.class.getName(),new String[]{"java.lang.String"},(Object)null);
        check(selected.chosen.equals("string"));
        try{CompatCalls.create(Constructors.class.getName(),(Object)null);throw new AssertionError();}catch(IllegalStateException expected){}
        Object world=fixture("CovariantShipWorld",new String[]{"java/lang/CharSequence","java/lang/String","java/lang/Object"},false);
        check(CompatCalls.call(world,"getAllShips").equals("adapter-1"));
        check(CompatCalls.call(world,"getAllShips").equals("adapter-1"));
        Object reverse=fixture("ReverseShipWorld",new String[]{"java/lang/String","java/lang/CharSequence"},false);
        check(CompatCalls.call(reverse,"getAllShips").equals("adapter-0"));
        // The shipped Et exposes only flagged adapters; the implementation is renamed a().
        Object flagged=fixture("FlaggedShipWorld",new String[]{"java/lang/String","java/lang/Object"},true);
        check(CompatCalls.call(flagged,"getAllShips").equals("adapter-0"));
        try{CompatCalls.call(fixture("UnrelatedReturns",new String[]{"java/lang/String","java/lang/Integer"},false),"getAllShips");throw new AssertionError();}catch(IllegalStateException expected){}
        try{CompatCalls.call(new Overloaded(),"choose","value");throw new AssertionError();}catch(IllegalStateException expected){}
        try{CompatCalls.call(new Overloaded(),"choose",(Object)null);throw new AssertionError();}catch(IllegalStateException expected){}
        try{CompatCalls.call(new Overloaded(),"fail");throw new AssertionError();}catch(IllegalArgumentException expected){check(expected.getMessage().equals("original failure"));}
        System.out.println("PASS: VS-style unflagged covariant getters, cached resolution, bridge-only API names, genuine overload rejection and original exception propagation");
    }
}
