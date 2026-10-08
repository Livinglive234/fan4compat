import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

public final class EurekaWarningSmokeTest implements Opcodes {
    public static class Callback {
        public boolean cancelled;
        public Object value;
        public void setReturnValue(Object v) { cancelled=true;value=v; }
    }
    public static void main(String[] args) throws Exception {
        Path p=Path.of(args[0],"dev/fan4/compat/mixin/eureka/common/EurekaPortalWarningMixin.class");
        ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);
        if(args.length>1) { VerifyAddon.readJar(args[1]);VerifyAddon.injectionTargets(n); }
        for(MethodNode m:n.methods)m.access=ACC_PUBLIC;
        MethodVisitor init=n.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();
        ClassWriter w=new ClassWriter(0);
        n.accept(new ClassRemapper(w,new Remapper(){public String map(String s){return s.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable")?"EurekaWarningSmokeTest$Callback":s;}}));
        byte[] bytes=w.toByteArray();Class<?> c=new ClassLoader(EurekaWarningSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
        Object instance=c.getConstructor().newInstance();var method=c.getMethod("fan4$hideEurekaWarning",String.class,Callback.class);
        for(String id:new String[]{"vs_eureka","create","valkyrienskies","preview","eureka",null}){
            Callback callback=new Callback();method.invoke(instance,id,callback);
            if(callback.cancelled!="vs_eureka".equals(id)||callback.cancelled&&!Boolean.FALSE.equals(callback.value))throw new AssertionError("Unexpected warning decision: "+id);
        }
        ClassNode info=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/eureka/common/EurekaPortalWarningInfoMixin.class"))).accept(info,0);
        if(args.length>1)VerifyAddon.injectionTargets(info);
        for(MethodNode m:info.methods)m.access=ACC_PUBLIC;
        for(FieldNode f:info.fields)f.access=ACC_PUBLIC;
        MethodVisitor ctor=info.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();
        ClassWriter iw=new ClassWriter(0);info.accept(new ClassRemapper(iw,new Remapper(){public String map(String name){return name.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable")?"EurekaWarningSmokeTest$Callback":name;}}));
        byte[] ib=iw.toByteArray();Class<?> ic=new ClassLoader(EurekaWarningSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,ib,0,ib.length);}}.define();Object record=ic.getConstructor().newInstance();
        for(String id:new String[]{"vs_eureka","create","distanthorizons","other",null}) {
            ic.getField("modId").set(record,id);Callback callback=new Callback();ic.getMethod("fan4$hideSevereEurekaWarning",Callback.class).invoke(record,callback);
            if(callback.cancelled!="vs_eureka".equals(id)||callback.cancelled&&!Boolean.FALSE.equals(callback.value))throw new AssertionError("Severe warning filter: "+id);
        }
        System.out.println("PASS: Eureka notice suppressed; Create, other IDs and null preserve native checks; exact IP selector verified");
    }
}
