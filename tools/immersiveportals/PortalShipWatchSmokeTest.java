import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class PortalShipWatchSmokeTest {
    public interface Original {Object call(Object...args);}
    public static class Player {}
    public static class Wrapper {Object player;Wrapper(Object p){player=p;}public Object getPlayer(){return player;}public Object getPosition(Object target){return target;}}
    public static class Observer {public Object getPosition(Object target){return target;}}
    public static class Ship {String dimension;Ship(String d){dimension=d;}public String getChunkClaimDimension(){return dimension;}}
    public static class Utils {public static String getResourceKey(String d){return d;}}
    public static class Tracking {
        static Set<String> watched=new HashSet<>();static Player owner;
        public static boolean isPlayerWatchingChunk(Player p,String dimension,int x,int z){return p==owner&&watched.contains(dimension+":"+x+":"+z);}
    }
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static void scopeContract(String generated)throws Exception {
        ClassNode c=new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(generated,"dev/fan4/compat/mixin/immersiveportals/common/PortalShipWatchMixin.class"))).accept(c,0);
        int handlers=0;
        for(MethodNode m:c.methods)if(m.name.startsWith("fan4$")) {
            handlers++;
            check(m.visibleParameterAnnotations==null&&m.invisibleParameterAnnotations==null,"watcher must not capture reconstructed locals");
            if(m.name.equals("fan4$watchScope")) {
                check(m.desc.equals(PortalShipWatchGenerator.TARGET.substring(1).replace(")V","L"+PortalShipWatchGenerator.OP+";)V")),"wrapper captures exact target arguments");
                check(m.tryCatchBlocks.size()==1&&m.tryCatchBlocks.get(0).type==null,"scope must clean up on all exceptions");
                int cleanup=0,operation=0;
                for(var i:m.instructions)if(i instanceof MethodInsnNode call) {
                    if(call.name.equals("exit"))cleanup++;
                    if(call.owner.equals(PortalShipWatchGenerator.OP)&&call.name.equals("call"))operation++;
                }
                check(cleanup==2&&operation==1,"one original call, normal and exceptional cleanup");
            }
            if(m.name.equals("fan4$watchDistance"))check(m.desc.equals("(D)D"),"distance modifier consumes only the expression");
            if(m.name.equals("fan4$dimensionEligible"))check(m.desc.equals("(Z)Z"),"dimension modifier consumes only the expression");
        }
        check(handlers==4,"all watcher handlers checked");
        System.out.println("PASS: invocation scope, direct player receiver, no local sugar, exception-safe wrapper");
    }
    static void contract(String jar)throws Exception {
        try(ZipFile z=new ZipFile(jar)) {
            ClassNode c=new ClassNode();new ClassReader(z.getInputStream(z.getEntry("org/valkyrienskies/core/impl/shadow/CY.class"))).accept(c,0);
            String selector=PortalShipWatchGenerator.TARGET;String desc=selector.substring(1);
            MethodNode m=c.methods.stream().filter(n->n.name.equals("a")&&n.desc.equals(desc)).findFirst().orElseThrow();
            // Preserve reference descriptors to prove the captured locals at both call sites.
            BasicInterpreter interpreter=new BasicInterpreter(Opcodes.ASM9){
                public BasicValue newValue(Type t){return t!=null&&(t.getSort()==Type.OBJECT||t.getSort()==Type.ARRAY)?new BasicValue(t):super.newValue(t);}
                public BasicValue merge(BasicValue a,BasicValue b){return a.equals(b)?a:super.merge(a,b);}
            };
            Frame<BasicValue>[] frames=new Analyzer<>(interpreter).analyze(c.name,m);
            int dimension=0,distance=0;
            for(int i=0;i<m.instructions.size();i++)if(m.instructions.get(i) instanceof MethodInsnNode call) {
                boolean match=call.owner.equals("kotlin/jvm/internal/Intrinsics")&&call.name.equals("areEqual");
                boolean range=call.owner.equals("org/valkyrienskies/core/util/AABBdUtilKt")&&call.name.equals("signedDistanceTo");
                if(match||range) {
                    Frame<BasicValue> f=frames[i];
                    check(f.getLocal(5).getType().getInternalName().equals("org/valkyrienskies/core/impl/api/ServerShipInternal"),"ship local changed");
                    check(f.getLocal(6).getType().getInternalName().equals("org/valkyrienskies/core/internal/world/VsiPlayer"),"player local changed");
                    check(f.getLocal(24).equals(BasicValue.LONG_VALUE),"chunk local changed");
                    if(match)dimension++;else distance++;
                }
            }
            check(dimension==1&&distance==1,"exactly one dimension and distance injection");
            System.out.println("PASS: exact VS watcher selector and raw frame types at both expression points");
        }
    }
    public static void main(String[] args)throws Exception {
        Map<String,String> names=Map.of("org.valkyrienskies.mod.common.util.MinecraftPlayer","PortalShipWatchSmokeTest$Wrapper","net.minecraft.class_3222","PortalShipWatchSmokeTest$Player","net.minecraft.class_5321","java.lang.String","org.valkyrienskies.mod.common.VSGameUtilsKt","PortalShipWatchSmokeTest$Utils","qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking","PortalShipWatchSmokeTest$Tracking");
        String name="dev.fan4.compat.immersiveportals.PortalShipWatchCompat";
        ClassLoader loader=new ClassLoader(PortalShipWatchSmokeTest.class.getClassLoader()) {
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException {
                if(n.equals("GeneratedWatcher")) {
                    Class<?> existing=findLoadedClass(n);if(existing!=null)return existing;
                    try {
                        ClassWriter writer=new ClassWriter(0);
                        new ClassReader(Files.readAllBytes(Path.of(args[1],"dev/fan4/compat/mixin/immersiveportals/common/PortalShipWatchMixin.class"))).accept(new ClassRemapper(writer,new Remapper(){
                            public String map(String value) {
                                if(value.equals(PortalShipWatchGenerator.OP))return "PortalShipWatchSmokeTest$Original";
                                if(value.equals("dev/fan4/compat/mixin/immersiveportals/common/PortalShipWatchMixin"))return "GeneratedWatcher";
                                if(value.startsWith("org/joml/")||value.startsWith("org/valkyrienskies/")||value.equals("java/util/Set")||value.equals("java/util/TreeSet"))return "java/lang/Object";
                                return value;
                            }
                        }),0);
                        byte[] bytes=writer.toByteArray();return defineClass(n,bytes,0,bytes.length);
                    }catch(Exception e){throw new ClassNotFoundException(n,e);}
                }
                if(!n.startsWith(name))return super.loadClass(n,resolve);
                Class<?> c=findLoadedClass(n);
                if(c==null)try {
                    ClassWriter w=new ClassWriter(0);
                    new ClassReader(Files.readAllBytes(Path.of(args[0],n.replace('.','/')+".class"))).accept(new ClassRemapper(w,new Remapper(){public Object mapValue(Object v){return v instanceof String value?names.getOrDefault(value,value):super.mapValue(v);}}),0);
                    byte[] bytes=w.toByteArray();c=defineClass(n,bytes,0,bytes.length);
                }catch(Exception e){throw new ClassNotFoundException(n,e);}
                if(resolve)resolveClass(c);return c;
            }
        };
        Class<?> helper=loader.loadClass(name);
        var dim=helper.getMethod("dimensionEligible",boolean.class);
        var dist=helper.getMethod("watchDistance",double.class);
        var enter=helper.getMethod("enter",Object.class,int.class,int.class);
        var exit=helper.getMethod("exit");
        var position=helper.getMethod("playerPosition",Object.class,Object.class);
        Tracking.owner=new Player();Wrapper player=new Wrapper(Tracking.owner);Ship ship=new Ship("overworld");Object destination=new Object();
        check(!(Boolean)dim.invoke(null,false),"outside scope preserves dimension");
        check((Double)dist.invoke(null,9000d)==9000d,"outside scope preserves distance");
        enter.invoke(null,ship,-100,200);
        try {
            check(position.invoke(null,player,destination)==destination,"original position return preserved");
            check(!(Boolean)dim.invoke(null,false),"unwatched remote ship rejected");
            check((Boolean)dim.invoke(null,true),"normal dimension preserved");
            check((Double)dist.invoke(null,9000d)==9000d,"normal distance preserved");
            Tracking.watched.add("overworld:-100:200");
            position.invoke(null,player,destination);
            check((Boolean)dim.invoke(null,false),"portal watch bridges dimension");
            check((Double)dist.invoke(null,9000d)==-1d,"portal watch bridges distance");
            check(position.invoke(null,new Observer(),destination)==destination,"synthetic observer keeps its original position call");
            check(!(Boolean)dim.invoke(null,false),"synthetic observer restores ordinary dimension gate");
            check((Boolean)dim.invoke(null,true),"synthetic observer retains its existing dimension eligibility");
            check((Double)dist.invoke(null,9000d)==9000d,"synthetic observer retains ordinary distance");
            position.invoke(null,player,destination);
            check((Boolean)dim.invoke(null,false),"real player after observer still receives portal watch");
            position.invoke(null,new Wrapper(new Player()),destination);
            check(!(Boolean)dim.invoke(null,false),"each player resets watch eligibility");
            position.invoke(null,player,destination);
            enter.invoke(null,ship,-100,201);
            try {
                position.invoke(null,player,destination);
                check(!(Boolean)dim.invoke(null,false),"neighbor chunk not retained");
            }finally{exit.invoke(null);}
            check((Boolean)dim.invoke(null,false),"nested scope restores outer player watch");
            enter.invoke(null,new Ship("tardis"),-100,200);
            try {
                position.invoke(null,player,destination);
                check(!(Boolean)dim.invoke(null,false),"other dimension not retained");
                throw new IllegalStateException("simulated original watcher failure");
            }catch(IllegalStateException expected){}finally{exit.invoke(null);}
            check((Boolean)dim.invoke(null,false),"exception cleanup restores outer scope");
            Tracking.watched.clear();position.invoke(null,player,destination);
            check(!(Boolean)dim.invoke(null,false),"expired watch releases tracking");
            check((Double)dist.invoke(null,9000d)==9000d,"expired watch restores distance");
            position.invoke(null,new Wrapper(null),destination);
            check(!(Boolean)dim.invoke(null,false),"disconnected player rejected");
        }finally{exit.invoke(null);}
        check(!(Boolean)dim.invoke(null,false),"scope removed after watcher completes");
        System.out.println("PASS: remote watches, player/chunk/dimension isolation, nested scopes, expiry and cleanup");
        Class<?> generated=loader.loadClass("GeneratedWatcher");
        var wrapper=Arrays.stream(generated.getDeclaredMethods()).filter(m->m.getName().equals("fan4$watchScope")).findFirst().orElseThrow();wrapper.setAccessible(true);
        Object[] forwarded=new Object[14];forwarded[5]=ship;forwarded[8]=1d;forwarded[9]=2d;forwarded[12]=-100;forwarded[13]=200;
        Object[] invocation=Arrays.copyOf(forwarded,15);
        Tracking.watched.add("overworld:-100:200");
        invocation[14]=(Original)(actual)-> {
            check(Arrays.equals(actual,forwarded),"wrapper must forward every argument once and in order");
            try {position.invoke(null,player,destination);check((Boolean)dim.invoke(null,false),"generated wrapper enters the correct ship/chunk scope");}
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
            return null;
        };
        wrapper.invoke(null,invocation);
        check(!(Boolean)dim.invoke(null,false),"generated wrapper cleans up normal return");
        RuntimeException marker=new RuntimeException("original watcher error");
        invocation[14]=(Original)(actual)->{throw marker;};
        try{wrapper.invoke(null,invocation);throw new AssertionError("missing original exception");}
        catch(java.lang.reflect.InvocationTargetException e){check(e.getCause()==marker,"original exception propagated unchanged");}
        check(!(Boolean)dim.invoke(null,false),"generated wrapper cleans up exceptional return");
        System.out.println("PASS: executed generated wrapper, argument forwarding, original call and exceptional cleanup");
        scopeContract(args[1]);
        if(args.length>2) {
            contract(args[2]);
            try(ZipFile z=new ZipFile(args[2])) {
                ClassNode minecraft=new ClassNode(),observer=new ClassNode();
                new ClassReader(z.getInputStream(z.getEntry("org/valkyrienskies/mod/common/util/MinecraftPlayer.class"))).accept(minecraft,0);
                new ClassReader(z.getInputStream(z.getEntry("org/valkyrienskies/mod/common/util/ShipObserverPlayer.class"))).accept(observer,0);
                check(minecraft.methods.stream().anyMatch(m->m.name.equals("getPlayer")&&m.desc.equals("()Lnet/minecraft/class_1657;")),"real wrapper exposes getPlayer");
                check(observer.methods.stream().noneMatch(m->m.name.equals("getPlayer")),"actual synthetic observer has no player getter");
                check(minecraft.interfaces.contains("org/valkyrienskies/core/internal/world/VsiPlayer")&&observer.interfaces.contains("org/valkyrienskies/core/internal/world/VsiPlayer"),"both observer kinds implement the watcher interface");
            }
            System.out.println("PASS: exact MinecraftPlayer versus ShipObserverPlayer API contracts");
        }
    }
}
