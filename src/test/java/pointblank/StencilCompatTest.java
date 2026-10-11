import dev.fan4.compat.pointblank.StencilCompat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StencilCompatTest {
    static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
    static final class State implements StencilCompat.Backend {
        int actualFunction=514,cachedFunction=514,actualReference=1,cachedReference=1,actualMask=0,cachedMask=0;
        boolean enabled=true;List<String> calls=new ArrayList<>();
        public void function(int f,int r,int m){calls.add("function:"+f+":"+r+":"+m);if(cachedFunction!=f||cachedReference!=r){actualFunction=cachedFunction=f;actualReference=cachedReference=r;}}
        public void mask(int m){calls.add("mask:"+m);if(cachedMask!=m)actualMask=cachedMask=m;}
        public void operation(int a,int b,int c){calls.add("op:"+a+":"+b+":"+c);}
        public void clearValue(int value){calls.add("value:"+value);}
        public void clear(int bits,boolean mac){calls.add("clear:"+bits+":"+mac);}
        public void test(int cap,boolean enable){calls.add("test:"+cap+":"+enable);if(cap==2960)enabled=enable;}
    }
    public static void main(String[] args)throws Exception {
        AtomicBoolean portal=new AtomicBoolean();State state=new State();var policy=new StencilCompat.Policy(portal::get,state);
        // Reproduce the observed cache bypass: native raw GL changes are invisible to the cache.
        state.actualMask=255;state.mask(0);check(state.actualMask==255,"fixture reproduces skipped native mask reset");
        state.actualMask=0;state.calls.clear();
        policy.function(519,255,255);policy.mask(255);
        check(state.actualFunction==state.cachedFunction&&state.actualReference==state.cachedReference&&state.actualMask==state.cachedMask,"scope uses cache-aware setters");
        policy.function(514,1,255);policy.mask(0);
        check(state.actualFunction==514&&state.actualReference==1&&state.actualMask==0,"next portal setup cannot skip its stencil reset");
        policy.operation(7680,7680,7681);policy.clearValue(0);policy.clear(1024,false);policy.test(2960,false);
        check(!state.enabled&&state.calls.contains("op:7680:7680:7681")&&state.calls.contains("clear:1024:false"),"ordinary scope drawing retains operations and clearing");
        state.enabled=true;state.calls.clear();portal.set(true);
        policy.function(519,255,255);policy.mask(255);policy.operation(7680,7680,7681);policy.clearValue(0);policy.clear(1024,false);policy.test(2960,false);policy.test(2960,true);
        check(state.calls.isEmpty()&&state.enabled&&state.actualFunction==514&&state.actualReference==1&&state.actualMask==0,"nested portal pass retains comparison, writes, test and stencil contents");
        policy.clear(1024|256|16384,true);policy.test(3042,true);
        check(state.calls.equals(List.of("clear:16640:true","test:3042:true")),"only stencil bit removed; unrelated clear bits/capabilities and platform flag preserved");
        portal.set(false);policy.mask(255);policy.clear(1024,true);
        check(state.actualMask==255&&state.calls.contains("clear:1024:true"),"normal scope resumes after portal rendering ends");
        state.cachedFunction=517;state.cachedReference=2;state.actualFunction=517;state.actualReference=0;
        state.function(517,2,255);check(state.actualReference==0,"cached identical call skips observed actual-reference correction");
        StencilCompat.synchronizeFunction(state::function,(f,r,m)->{state.actualFunction=f;state.actualReference=r;},517,2,255);
        check(state.actualReference==2&&state.cachedReference==2,"forced function resynchronizes cache and actual reference even for identical arguments");
        generated(policy,state,portal);
        System.out.println("PASS: observed cache-bypass reproduction, cache-aware scope state and portal stencil ownership (test backend)");
    }
    public static final class Dispatch {
        static StencilCompat.Policy policy;static State state;
        public static void function(int f,int r,int m){policy.function(f,r,m);}
        public static void mask(int m){policy.mask(m);}
        public static void operation(int a,int b,int c){policy.operation(a,b,c);}
        public static void clearValue(int v){policy.clearValue(v);}
        public static void clear(int b,boolean mac){policy.clear(b,mac);}
        public static void enable(int c){policy.test(c,true);}
        public static void disable(int c){policy.test(c,false);}
        public static void portalFunction(int f,int r,int m){state.function(f,r,m);}
        public static void portalMask(int m){state.mask(m);}
        public static void portalOperation(int a,int b,int c){state.operation(a,b,c);}
    }
    static void generated(StencilCompat.Policy policy,State state,AtomicBoolean portal)throws Exception {
        Dispatch.policy=policy;Dispatch.state=state;
        for(String simple:List.of("GunStencilMixin","GunStencilClearMixin","PortalStencilCacheMixin")) {
            var node=new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("build/generated/classes/dev/fan4/compat/mixin/pointblank/client/"+simple+".class"))).accept(node,0);
            // Run the generated handlers with a recording GL backend, keeping argument loads intact.
            for(var method:node.methods) {
                method.access=org.objectweb.asm.Opcodes.ACC_PUBLIC|org.objectweb.asm.Opcodes.ACC_STATIC;
                if(simple.equals("GunStencilClearMixin"))for(var instruction:method.instructions)if(instruction instanceof org.objectweb.asm.tree.VarInsnNode local)local.var--;
                for(var instruction:method.instructions)if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call&&call.owner.equals("dev/fan4/compat/pointblank/StencilCompat"))call.owner="StencilCompatTest$Dispatch";
            }
            var writer=new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES|org.objectweb.asm.ClassWriter.COMPUTE_MAXS);node.accept(writer);
            Class<?> generated=new ClassLoader(StencilCompatTest.class.getClassLoader()){Class<?> define(){byte[] bytes=writer.toByteArray();return defineClass(null,bytes,0,bytes.length);}}.define();
            portal.set(true);state.calls.clear();
            for(var method:generated.getDeclaredMethods()) {
                Class<?>[] types=method.getParameterTypes();Object[] values=new Object[types.length];
                for(int i=0;i<values.length;i++)values[i]=types[i]==boolean.class?true:(method.getName().contains("Enable")||method.getName().contains("Disable")?2960:1024);
                method.invoke(null,values);
            }
            check(simple.equals("PortalStencilCacheMixin")?!state.calls.isEmpty():state.calls.isEmpty(),"generated handlers preserve IP ownership: "+simple);
            portal.set(false);state.calls.clear();
            for(var method:generated.getDeclaredMethods()) {
                Object[] values=new Object[method.getParameterCount()];for(int i=0;i<values.length;i++)values[i]=method.getParameterTypes()[i]==boolean.class?true:255;
                method.invoke(null,values);
            }
            check(!state.calls.isEmpty(),"generated handlers forward scope arguments: "+simple);
        }
    }

}
