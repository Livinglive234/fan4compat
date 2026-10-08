import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

public final class PortalSoundSmokeTest {
    public static class Vec {public double field_1352,field_1351,field_1350;public Vec(double x,double y,double z){field_1352=x;field_1351=y;field_1350=z;}}
    public static class Pos {public Pos(int x,int y,int z){}}
    public static class Dir {public static Dir method_10142(double x,double y,double z){return new Dir();}}
    enum Kind {MISS,BLOCK}
    public static class Hit {public Vec end;Kind kind;Hit(Vec end,Kind kind){this.end=end;this.kind=kind;}public Kind method_17783(){return kind;}public static Hit method_17778(Vec v,Dir d,Pos p){return new Hit(v,Kind.MISS);}}
    public record ChunkPos(int field_9181,int field_9180) {}
    public static class Clone {public Map<ChunkPos,Object> clonedLevelChunks=new HashMap<>();public int method_31607(){return -64;}public int method_31605(){return 384;}}
    public static class Client {static final Client INSTANCE=new Client();public static Client method_1551(){return INSTANCE;}}
    public static class Access {static Object proxy;public static Object getClientLevelProxy(Client client){return proxy;}}
    public static class Physics {static int defaults,source;static boolean auxiliary;public static void setDefaultEnvironment(int s,boolean aux){defaults++;source=s;auxiliary=aux;}}
    public static class Operation {Function<Object[],Object> fn;Operation(Function<Object[],Object> fn){this.fn=fn;}public Object call(Object[] args){return fn.apply(args);}}
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        Map<String,String> types=Map.of("net.minecraft.class_310","PortalSoundSmokeTest$Client","net.minecraft.class_243","PortalSoundSmokeTest$Vec","net.minecraft.class_2338","PortalSoundSmokeTest$Pos","net.minecraft.class_2350","PortalSoundSmokeTest$Dir","net.minecraft.class_3965","PortalSoundSmokeTest$Hit","com.sonicether.soundphysics.utils.LevelAccessUtils","PortalSoundSmokeTest$Access","com.sonicether.soundphysics.SoundPhysics","PortalSoundSmokeTest$Physics","com.sonicether.soundphysics.world.ClonedClientLevel","PortalSoundSmokeTest$Clone");
        ClassWriter w=new ClassWriter(0);new ClassReader(Files.readAllBytes(Path.of("build/classes/java/main/dev/fan4/compat/soundphysics/PortalSoundCompat.class"))).accept(new ClassRemapper(w,new Remapper(){public Object mapValue(Object value){return value instanceof String s?types.getOrDefault(s,s):super.mapValue(value);}}),0);
        byte[] bytes=w.toByteArray();Class<?> helper=new ClassLoader(PortalSoundSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
        var eval=helper.getMethod("evaluate",Object.class,int.class,double.class,double.class,double.class,Object.class,Object.class,boolean.class);
        var snapshot=helper.getMethod("snapshot");var scoped=helper.getMethod("scoped");
        int[] calls={0};Operation nativeEval=new Operation(a->{calls[0]++;return a;});
        check(eval.invoke(null,nativeEval,17,1d,2d,3d,"category","sound",true)==null&&calls[0]==0&&Physics.defaults==1&&Physics.source==17&&Physics.auxiliary,"missing snapshot uses native default without environment work");
        Clone first=new Clone(),second=new Clone();Access.proxy=first;
        Operation nested=new Operation(a->{try{check(snapshot.invoke(null)==second,"nested snapshot");return "nested";}catch(Exception e){throw new RuntimeException(e);}});
        Operation outer=new Operation(a->{try{
            check(snapshot.invoke(null)==first,"captured snapshot");Access.proxy=second;
            check(snapshot.invoke(null)==first,"world change cannot replace captured proxy");
            check(eval.invoke(null,nested,1,1d,2d,3d,null,null,false).equals("nested"),"nested return");
            check(snapshot.invoke(null)==first,"outer scope restored");
            boolean[] isolated={false};Thread thread=new Thread(()->{try{isolated[0]=snapshot.invoke(null)==null;}catch(Exception e){throw new RuntimeException(e);}});thread.start();thread.join();check(isolated[0],"sound thread isolation");return a;
        }catch(Exception e){throw new RuntimeException(e);}});
        Object[] forwarded=(Object[])eval.invoke(null,outer,31,4d,5d,6d,"category","sound",false);
        check(Arrays.equals(forwarded,new Object[]{31,4d,5d,6d,"category","sound",false})&&!(Boolean)scoped.invoke(null),"arguments/return preserved and scope removed");
        try{eval.invoke(null,new Operation(a->{throw new IllegalStateException("native failure");}),1,1d,2d,3d,null,null,false);throw new AssertionError("exception swallowed");}catch(java.lang.reflect.InvocationTargetException e){check(e.getCause() instanceof IllegalStateException,"original failure retained");}check(!(Boolean)scoped.invoke(null),"exceptional cleanup");
        var ray=helper.getMethod("rayCast",Object.class,Object.class,Object.class,Object.class,Object.class);Object ignore=new Object();int[] rays={0};
        Operation trace=new Operation(a->{rays[0]++;check(a[3]==ignore,"ignore block forwarded");Vec x=(Vec)a[1],y=(Vec)a[2];double d=Math.abs(x.field_1352-y.field_1352)+Math.abs(x.field_1351-y.field_1351)+Math.abs(x.field_1350-y.field_1350);check(d<512,"native IP ray limit");return new Hit(y,Kind.MISS);});
        first.clonedLevelChunks.put(new ChunkPos(0,0),new Object());Vec from=new Vec(-10000,64,8),to=new Vec(10000,64,8);
        Hit miss=(Hit)ray.invoke(null,first,from,to,ignore,trace);check(rays[0]==1&&miss.end==to,"only cached interval traversed; original miss endpoint retained");
        Clone large=new Clone();for(int x=0;x<80;x++)large.clonedLevelChunks.put(new ChunkPos(x,0),new Object());rays[0]=0;
        ray.invoke(null,large,new Vec(-1000,64,8),new Vec(5000,64,8),ignore,trace);check(rays[0]==5,"long cached segment subdivided");
        rays[0]=0;Hit wall=new Hit(new Vec(260,64,8),Kind.BLOCK);Operation wallTrace=new Operation(a->{rays[0]++;return rays[0]==2?wall:new Hit((Vec)a[2],Kind.MISS);});check(ray.invoke(null,large,from,to,ignore,wallTrace)==wall&&rays[0]==2,"first native hit preserved");
        rays[0]=0;ray.invoke(null,first,new Vec(100,64,100),new Vec(200,64,200),ignore,trace);ray.invoke(null,new Clone(),from,to,ignore,trace);check(rays[0]==0,"outside and empty cache do no work");
        Object unsafe=new Object();Operation passthrough=new Operation(a->{check(a[0]==unsafe&&a[1]==from&&a[2]==to,"nonclone arguments retained");return wall;});check(ray.invoke(null,unsafe,from,to,ignore,passthrough)==wall,"unsafe/native proxies unchanged");
        if(args.length>0){VerifyAddon.readJar(args[0]);for(String name:new String[]{"PortalSoundSnapshotMixin","PortalSoundRayMixin"}){ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/soundphysics/client/"+name+".class"))).accept(n,0);VerifyAddon.injectionTargets(n);}
            ClassNode clone=VerifyAddon.classes.get("com/sonicether/soundphysics/world/ClonedClientLevel");check(clone.fields.stream().anyMatch(f->f.name.equals("clonedLevelChunks")&&f.desc.equals("Ljava/util/HashMap;")),"native cloned cache contract");
            check(VerifyAddon.exists("com/sonicether/soundphysics/SoundPhysics","setDefaultEnvironment","(IZ)V",new HashSet<>()),"native fallback contract");
        }
        System.out.println("PASS: missing/steady/nested/thread-isolated snapshot, native fallback, argument/exception preservation, bounded/split rays, earliest hit, uncached space and native selectors");
    }
}
