import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.commons.*;

/**
 * Runs the generated Aether cape mixin's handler against stand-ins for Minecraft's Player, PlayerModelPart and data tracker: every part except the cape
 * must come back exactly as the player's skin customisation bits say, and the cape part must be left to the Aether.
 */
public final class AetherCapeSmokeTest implements Opcodes {
    public static class Callback {
        public boolean cancelled;public Object value;
        public void setReturnValue(Object v){cancelled=true;value=v;}
    }
    public static class Accessor {}
    public static class Tracker {
        public byte flags;
        public Object method_12789(Accessor accessor){return Byte.valueOf(flags);}
    }
    public static class FakePlayer {
        public static final Accessor field_7518=new Accessor();
        public final Tracker tracker=new Tracker();
        public Tracker method_5841(){return tracker;}
    }
    public static class Part {
        public static final Part field_7559=new Part(1),JACKET=new Part(2),LEFT_SLEEVE=new Part(4),RIGHT_SLEEVE=new Part(8),LEFT_PANTS=new Part(16),RIGHT_PANTS=new Part(32),HAT=new Part(64);
        final int mask;Part(int mask){this.mask=mask;}
        public int method_7430(){return mask;}
    }
    public static void main(String[] args)throws Exception {
        Path p=Path.of(args[0],"dev/fan4/compat/mixin/aether/common/AetherCapeSkinLayersMixin.class");
        ClassNode n=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(n,0);
        // The handler is injected at the head of Player.isModelPartShown, before the Aether's own (lower priority number).
        boolean first=false;
        if(n.invisibleAnnotations!=null)for(AnnotationNode a:n.invisibleAnnotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")&&a.values.contains(Integer.valueOf(500)))first=true;
        if(!first)throw new AssertionError("The mixin must have a priority below the default 1000 so it runs before the Aether's");
        String fake=AetherCapeSmokeTest.class.getName().replace('.','/');
        n.superName=fake+"$FakePlayer";
        for(MethodNode m:n.methods)m.access=ACC_PUBLIC;
        MethodVisitor init=n.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,fake+"$FakePlayer","<init>","()V",false);init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES){protected String getCommonSuperClass(String a,String b){return "java/lang/Object";}};
        n.accept(new ClassRemapper(w,new Remapper(){public String map(String s){return switch(s) {
            case "net/minecraft/class_1657","net/minecraft/class_1297"->fake+"$FakePlayer";
            case "net/minecraft/class_1664"->fake+"$Part";
            case "net/minecraft/class_2945"->fake+"$Tracker";
            case "net/minecraft/class_2940"->fake+"$Accessor";
            case "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable"->fake+"$Callback";
            default->s;};}}));
        byte[] bytes=w.toByteArray();
        Class<?> c=new ClassLoader(AetherCapeSmokeTest.class.getClassLoader()){Class<?> define(){return defineClass(null,bytes,0,bytes.length);}}.define();
        Object player=c.getConstructor().newInstance();
        var method=c.getMethod("fan4$keepSkinLayers",Part.class,Callback.class);
        var tracker=(Tracker)FakePlayer.class.getField("tracker").get(player);
        Part[] parts={Part.JACKET,Part.LEFT_SLEEVE,Part.RIGHT_SLEEVE,Part.LEFT_PANTS,Part.RIGHT_PANTS,Part.HAT};
        for(int flags:new int[]{0,2,4|8,16|32|64,0x7f,1}) {
            tracker.flags=(byte)flags;
            for(Part part:parts) {
                Callback callback=new Callback();method.invoke(player,part,callback);
                boolean expected=(flags&part.mask)==part.mask;
                if(!callback.cancelled||!Boolean.valueOf(expected).equals(callback.value))throw new AssertionError("part "+part.mask+" with customisation "+flags+": expected "+expected+" got "+callback.value);
            }
            Callback cape=new Callback();method.invoke(player,Part.field_7559,cape);
            if(cape.cancelled)throw new AssertionError("the cape part must be left to the Aether (customisation "+flags+")");
        }
        System.out.println("PASS: Aether cape slot no longer forces jacket, sleeves, pants or hat back on (each part follows the skin customisation bits; the cape part is left alone; handler runs before the Aether's)");
    }
}
