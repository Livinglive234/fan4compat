import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

public class WingTrailSmokeTest {
    public static class Player {boolean gliding=true,invisible;int id=7;public boolean method_6128(){return gliding;}public boolean method_5767(){return invisible;}public int method_5628(){return id;}}
    public record Vec(double x,double y,double z){public Vec method_1019(Vec other){return new Vec(x+other.x,y+other.y,z+other.z);}}
    public static class Matrices {int depth;double z;public void method_22903(){depth++;}public void method_22909(){depth--;z=0;}public void method_22904(double x,double y,double value){z+=value;}}
    public static class Model {static int created,animated;public Model(Part root){created++;}public static Data method_31994(){return new Data();}public void method_17079(Player entity,float a,float b,float c,float d,float e){animated++;}public Part elytratrails$getLeftWing(){return new Part(-1);}public Part elytratrails$getRightWing(){return new Part(1);}}
    public record Part(double x) {Part(){this(0);}}
    public static class Data {public Part method_32109(){return new Part();}}
    public static class Util {static boolean fail;public static Vec computeWingTipLocal(Part p,boolean left){return new Vec(p.x,1,0);}public static Vec transformLocalPointThroughPart(Matrices m,Part p,Vec v){if(fail)throw new IllegalStateException("transform failed");return new Vec(v.x,v.y,v.z+m.z);}}
    public static class Shader {static boolean shadow;public static boolean isShadowPass(){return shadow;}}
    public static class Sampler {public Map<Integer,List<Vec>> gatheredTrailsThisFrame=new HashMap<>();int inserts;public void insertWingTips(int id,Vec l,Vec r){inserts++;gatheredTrailsThisFrame.put(id,List.of(l,r));}}
    public static class Trails {static final Sampler sampler=new Sampler();public static Sampler getWingtipSampler(){return sampler;}}
    public static class Camera {public Vec method_19326(){return new Vec(100,200,300);}}
    public static class Renderer {public Camera method_19418(){return new Camera();}}
    public static class Client {public Renderer field_1773=new Renderer();public static Client method_1551(){return new Client();}}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Map<String,String> types=new HashMap<>();String[][] pairs={{"net.minecraft.class_1657","Player"},{"net.minecraft.class_563","Model"},{"net.minecraft.class_630","Part"},{"net.minecraft.class_243","Vec"},{"net.minecraft.class_310","Client"},{"dbrighthd.elytratrails.util.ShaderChecksUtil","Shader"},{"dbrighthd.elytratrails.rendering.TrailSystem","Trails"},{"dbrighthd.elytratrails.util.ModelTransformationUtil","Util"}};
        for(String[] p:pairs)types.put(p[0],"WingTrailSmokeTest$"+p[1]);
        ClassWriter w=new ClassWriter(0);new ClassReader(Files.readAllBytes(Path.of(args[0],"dev/fan4/compat/elytratrails/WingTrailCompat.class"))).accept(new ClassRemapper(w,new Remapper(){public Object mapValue(Object v){return v instanceof String s?types.getOrDefault(s,s):super.mapValue(v);}}),0);
        byte[] bytes=w.toByteArray();Class<?> h=new ClassLoader(WingTrailSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass("dev.fan4.compat.elytratrails.WingTrailCompat",bytes,0,bytes.length);}}.define();var capture=h.getMethod("capture",Object.class,Object.class);
        Player p=new Player();Matrices m=new Matrices();capture.invoke(null,p,m);check(Trails.sampler.inserts==1&&m.depth==0,"missing custom/accessory wing samples filled and matrix restored");check(Trails.sampler.gatheredTrailsThisFrame.get(7).equals(List.of(new Vec(99,201,300.125),new Vec(101,201,300.125))),"body matrix and camera position supply world emitters");
        capture.invoke(null,p,m);check(Trails.sampler.inserts==1,"native or previous frame samples are not duplicated");Trails.sampler.gatheredTrailsThisFrame.clear();p.gliding=false;capture.invoke(null,p,m);check(Trails.sampler.inserts==1,"grounded players emit no fallback trails");p.gliding=true;p.invisible=true;capture.invoke(null,p,m);check(Trails.sampler.inserts==1,"invisible players do not gain fallback trails");p.invisible=false;Shader.shadow=true;capture.invoke(null,p,m);check(Trails.sampler.inserts==1,"shadow pass does not sample");Shader.shadow=false;capture.invoke(null,new Object(),m);check(Trails.sampler.inserts==1,"nonplayers unchanged");
        Util.fail=true;try{capture.invoke(null,p,m);throw new AssertionError("missing transform failure");}catch(java.lang.reflect.InvocationTargetException e){check(e.getCause() instanceof IllegalStateException,"failure propagated");}check(m.depth==0,"matrix scope cleans up on transform failure");Util.fail=false;capture.invoke(null,p,m);check(Model.created==1,"fallback model reused across frames");
        if(args.length==4){VerifyAddon.readJar(args[1]);LogCompatibilitySmokeTest.minecraft(args[2],args[3]);ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/elytratrails/client/WingTrailFallbackMixin.class"))).accept(node,0);VerifyAddon.injectionTargets(node);
            String[][] calls={{"net/minecraft/class_563","method_31994","()Lnet/minecraft/class_5607;"},{"net/minecraft/class_563","method_17079","(Lnet/minecraft/class_1309;FFFFF)V"},{"net/minecraft/class_5607","method_32109","()Lnet/minecraft/class_630;"},{"dbrighthd/elytratrails/util/ModelTransformationUtil","transformLocalPointThroughPart","(Lnet/minecraft/class_4587;Lnet/minecraft/class_630;Lnet/minecraft/class_243;)Lnet/minecraft/class_243;"},{"dbrighthd/elytratrails/rendering/WingTipSampler","insertWingTips","(ILnet/minecraft/class_243;Lnet/minecraft/class_243;)V"}};
            for(String[] c:calls)check(VerifyAddon.exists(c[0],c[1],c[2],new HashSet<>()),"missing native contract "+Arrays.toString(c));
        }
        System.out.println("PASS: custom/accessory wing fallback, existing sample preservation, gliding/visibility/shadow gates, world coordinates, model reuse and matrix cleanup");
    }
}
