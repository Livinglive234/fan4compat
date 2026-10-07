import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

/** Execute the generated placement scope against VS's source-hit override. */
public class PortalInteractionSmokeTest {
    public static class Client {
        static final Client INSTANCE=new Client();Object hit;
        public static Client method_1551(){return INSTANCE;}
        public Object vs$getOriginalCrosshairTarget(){return hit;}
        public void vs$setOriginalCrosshairTarget(Object h){hit=h;}
        Object interceptedHit(Object incoming){return hit==null?incoming:hit;}
    }
    public interface Original {Object call(Object... args);}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        String helper="dev.fan4.compat.immersiveportals.PortalInteractionCompat";
        ClassLoader loader=new ClassLoader(PortalInteractionSmokeTest.class.getClassLoader()) {
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith(helper)&&!name.equals("GeneratedPlacement"))return super.loadClass(name,resolve);
                Class<?> cached=findLoadedClass(name);if(cached!=null)return cached;
                try {
                    boolean generated=name.equals("GeneratedPlacement");
                    Path file=generated?Path.of(args[1],"dev/fan4/compat/mixin/immersiveportals/client/MixinShipRemotePlacement.class"):Path.of(args[0],name.replace('.','/')+".class");
                    ClassWriter w=new ClassWriter(0);new ClassReader(Files.readAllBytes(file)).accept(new ClassRemapper(w,new Remapper(){
                        public String map(String n){if(n.equals("com/llamalad7/mixinextras/injector/wrapoperation/Operation"))return "PortalInteractionSmokeTest$Original";if(n.equals("dev/fan4/compat/mixin/immersiveportals/client/MixinShipRemotePlacement"))return "GeneratedPlacement";return n;}
                        public Object mapValue(Object v){return v instanceof String s&&s.equals("net.minecraft.class_310")?"PortalInteractionSmokeTest$Client":super.mapValue(v);}
                    }),0);
                    byte[] bytes=w.toByteArray();Class<?> c=defineClass(name,bytes,0,bytes.length);if(resolve)resolveClass(c);return c;
                }catch(Exception e){throw new ClassNotFoundException(name,e);}
            }
        };
        Class<?> scope=loader.loadClass("GeneratedPlacement");var run=scope.getDeclaredMethod("fan4$remotePlacement",Supplier.class,boolean.class,Original.class);run.setAccessible(true);
        Object source=new Object(),destination=new Object();Client.INSTANCE.hit=source;int[] calls={0};Supplier<Object> action=()->Client.INSTANCE.interceptedHit(destination);
        Original original=values->{check(values[0]==action,"forward supplier unchanged");calls[0]++;return ((Supplier<?>)values[0]).get();};
        check(run.invoke(null,action,true,original)==destination&&Client.INSTANCE.hit==source&&calls[0]==1,"placement uses destination hit and restores original source hit");
        check(run.invoke(null,action,false,original)==source&&Client.INSTANCE.hit==source,"breaking preserves VS's native crosshair scope");
        try {run.invoke(null,action,true,(Original)values->{throw new IllegalStateException("fixture");});throw new AssertionError("expected failure");}
        catch(java.lang.reflect.InvocationTargetException e){check(e.getCause() instanceof IllegalStateException&&Client.INSTANCE.hit==source,"exception restores source hit");}
        Original nested=values->{check(Client.INSTANCE.hit==null,"outer placement clears override");try {check(run.invoke(null,action,true,original)==destination&&Client.INSTANCE.hit==null,"nested scope restores cleared outer override");}catch(Exception e){throw new RuntimeException(e);}return destination;};
        check(run.invoke(null,action,true,nested)==destination&&Client.INSTANCE.hit==source,"nested placement restores source once outer scope ends");
        Client.INSTANCE.hit=null;check(run.invoke(null,action,true,original)==destination&&Client.INSTANCE.hit==null,"null original crosshair remains null");
        System.out.println("PASS: generated remote placement scope, original-call forwarding, destination hits, nested scopes, exceptional restoration and unchanged breaking");
    }
}
