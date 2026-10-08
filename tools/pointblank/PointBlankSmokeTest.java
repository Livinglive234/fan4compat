import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

public final class PointBlankSmokeTest implements Opcodes {
    public static class Entity {}
    public static class Item {}
    public static class Gun extends Item {}
    public static class CustomGun extends Gun {}
    public static class Stack {
        private final Item item;
        public Stack(Item item){this.item=item;}
        public Item method_7909(){return item;}
    }
    public static class Callback {
        public boolean cancelled;public Object value;
        public void setReturnValue(Object value){cancelled=true;this.value=value;}
    }
    public static void main(String[] args)throws Exception {
        ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/mixin/pointblank/client/StaleGunAnimationMixin.class"))).accept(n,0);
        if(args.length>1){VerifyAddon.readJar(args[1]);VerifyAddon.injectionTargets(n);}
        for(MethodNode m:n.methods)m.access=ACC_PUBLIC|ACC_STATIC;
        Map<String,String> types=Map.of("net/minecraft/class_1309","PointBlankSmokeTest$Entity","net/minecraft/class_1799","PointBlankSmokeTest$Stack","net/minecraft/class_1792","PointBlankSmokeTest$Item","com/vicmatskiv/pointblank/item/GunItem","PointBlankSmokeTest$Gun","org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable","PointBlankSmokeTest$Callback");
        ClassWriter w=new ClassWriter(0);n.accept(new ClassRemapper(w,new Remapper(){public String map(String s){return types.getOrDefault(s,s);}}));
        byte[] b=w.toByteArray();Class<?> c=new ClassLoader(PointBlankSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,b,0,b.length);}}.define();
        var method=c.getMethod("fan4$skipNonGunControllers",Entity.class,Stack.class,Callback.class);
        for(Stack stack:new Stack[]{null,new Stack(new Item()),new Stack(null),new Stack(new Gun()),new Stack(new CustomGun())}){
            Callback cb=new Callback();method.invoke(null,new Entity(),stack,cb);
            boolean invalid=stack==null||!(stack.method_7909() instanceof Gun);
            if(cb.cancelled!=invalid||invalid&&(!(cb.value instanceof Map<?,?> map)||!map.isEmpty()))throw new AssertionError("wrong animation guard result");
            if(cb.cancelled)for(Object ignored:((Map<?,?>)cb.value).values())throw new AssertionError("non-gun controller invoked");
        }
        System.out.println("PASS: stale empty/non-gun/null stacks have no animation controllers; valid guns/subclasses retain native handling; exact Point Blank selector verified");
    }
}
