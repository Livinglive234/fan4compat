import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

/** Execute all four guards and prove that terrain portals still proceed. */
public final class ShipPortalCreationSmokeTest {
    public static class World {}
    public static class ServerWorld extends World {}
    public record Pos(boolean shipyard) {}
    public static class Axis {}
    public static class Entity {}
    public static class Generation {}
    public static class Callback {
        public boolean cancelled;public Object result;
        public void setReturnValue(Object value){cancelled=true;result=value;}
    }
    public static class Utils {
        public static boolean isBlockInShipyard(World world,Pos position){return position.shipyard();}
    }
    static final Map<String,String> TYPES=Map.of(
        "net/minecraft/class_1937",World.class.getName().replace('.','/'),
        "net/minecraft/class_1936",World.class.getName().replace('.','/'),
        "net/minecraft/class_3218",ServerWorld.class.getName().replace('.','/'),
        "net/minecraft/class_2338",Pos.class.getName().replace('.','/'),
        "net/minecraft/class_2350$class_2351",Axis.class.getName().replace('.','/'),
        "net/minecraft/class_1297",Entity.class.getName().replace('.','/'),
        "qouteall/imm_ptl/core/portal/custom_portal_gen/CustomPortalGeneration",Generation.class.getName().replace('.','/'),
        "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",Callback.class.getName().replace('.','/'),
        "org/valkyrienskies/mod/common/VSGameUtilsKt",Utils.class.getName().replace('.','/'));
    static class Loader extends ClassLoader {
        Loader(){super(ShipPortalCreationSmokeTest.class.getClassLoader());}
        protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            if(!name.equals("dev.fan4.compat.valkyrienskies.ShipPortalCreation")&&!name.startsWith("dev.fan4.compat.mixin."))return super.loadClass(name,resolve);
            Class<?> found=findLoadedClass(name);if(found!=null)return found;
            try {
                Path path=Path.of(name.contains(".mixin.")?"build/generated/classes":"build/classes/java/main",name.replace('.','/')+".class");ClassWriter out=new ClassWriter(0);
                new ClassReader(Files.readAllBytes(path)).accept(new ClassRemapper(out,new Remapper(){public String map(String value){return TYPES.getOrDefault(value,value);}public Object mapValue(Object value){if(value instanceof String text)return TYPES.getOrDefault(text.replace('.','/'),text).replace('/','.');return super.mapValue(value);}}),0);
                if(name.contains(".mixin.")){MethodVisitor ctor=out.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();}
                byte[] bytes=out.toByteArray();return defineClass(name,bytes,0,bytes.length);
            }catch(Exception e){throw new ClassNotFoundException(name,e);}
        }
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Loader loader=new Loader();var blocked=loader.loadClass("dev.fan4.compat.valkyrienskies.ShipPortalCreation").getMethod("blocked",Object.class,Object.class);
        check(!(Boolean)blocked.invoke(null,null,new Pos(true)),"null world passes");check(!(Boolean)blocked.invoke(null,new Object(),new Pos(true)),"non-world accessor passes");
        check((Boolean)blocked.invoke(null,new ServerWorld(),new Pos(true)),"shipyard denied without requiring loaded ship metadata");check(!(Boolean)blocked.invoke(null,new ServerWorld(),new Pos(false)),"terrain permitted even if actor is on a ship");
        for(String path:List.of("minecraft.common.ShipNetherPortalCreationMixin","aether.common.ShipAetherPortalCreationMixin","immersiveportals.common.ShipNetherPortalActivationMixin","immersiveportals.common.ShipFramePortalActivationMixin")) {
            Class<?> type=loader.loadClass("dev.fan4.compat.mixin."+path);java.lang.reflect.Method handler=type.getDeclaredMethods()[0];handler.setAccessible(true);Object instance=Modifier.isStatic(handler.getModifiers())?null:type.getConstructor().newInstance();
            boolean framed=path.contains("ShipFrame"),optional=path.contains("CreationMixin");
            for(boolean shipyard:new boolean[]{false,true}) {
                Callback callback=new Callback();Pos pos=new Pos(shipyard);
                Object[] inputs=framed?new Object[]{new Generation(),new ServerWorld(),pos,new ServerWorld(),new Entity(),callback}:optional?new Object[]{new World(),pos,new Axis(),callback}:new Object[]{new ServerWorld(),pos,callback};
                handler.invoke(instance,inputs);check(callback.cancelled==shipyard,"activation cancellation "+path);if(shipyard)check(optional?callback.result.equals(Optional.empty()):Boolean.FALSE.equals(callback.result),"native failure return "+path);
            }
        }
        if(args.length>0){for(String jar:args)VerifyAddon.readJar(jar);for(String path:List.of("aether/common/ShipAetherPortalCreationMixin","immersiveportals/common/ShipNetherPortalActivationMixin","immersiveportals/common/ShipFramePortalActivationMixin")){ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin",path+".class"))).accept(node,0);VerifyAddon.injectionTargets(node);}}
        System.out.println("PASS: executed vanilla, Aether and IP portal creation guards; shipyard rejection, native failure returns and terrain passthrough");
    }
}
