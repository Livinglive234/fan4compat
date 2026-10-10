import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Pin all generated diagnostic bindings and reflection fields to supplied native jars. */
public final class PointBlankRenderNativeCheck implements Opcodes {
    static ClassNode read(String jar,String owner)throws Exception {
        try(ZipFile zip=new ZipFile(jar)){ClassNode n=new ClassNode();new ClassReader(zip.getInputStream(zip.getEntry(owner+".class"))).accept(n,0);return n;}
    }
    public static void main(String[] args)throws Exception {
        String[][] targets={
            {"GunRenderDiagnosticMixin","com/vicmatskiv/pointblank/client/render/GunItemRenderer","method_3166","(Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V"},
            {"GunPrepareDiagnosticMixin","com/vicmatskiv/pointblank/client/ClientSystem","preRender","(Lcom/vicmatskiv/pointblank/client/GunClientState;Lnet/minecraft/class_9779;)V"},
            {"GunAuxDiagnosticMixin","com/vicmatskiv/pointblank/client/render/AuxLevelRenderer","renderToTarget","(Lnet/minecraft/class_9779;F)V"},
            {"GunWorldDiagnosticMixin","net/minecraft/class_761","method_22710","(Lnet/minecraft/class_9779;ZLnet/minecraft/class_4184;Lnet/minecraft/class_757;Lnet/minecraft/class_765;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"}};
        for(String[] target:targets) {
            ClassNode nativeClass=read(target[1].startsWith("net/")?args[1]:args[0],target[1]);
            MethodNode nativeMethod=nativeClass.methods.stream().filter(m->m.name.equals(target[2])&&m.desc.equals(target[3])).findFirst().orElseThrow();
            if((nativeMethod.access&ACC_STATIC)!=0)throw new AssertionError("Instance wrapper on static method");
            var path=java.nio.file.Path.of("build/generated/classes/dev/fan4/compat/mixin/pointblank/client/"+target[0]+".class");
            ClassNode mixin=new ClassNode();new ClassReader(java.nio.file.Files.readAllBytes(path)).accept(mixin,0);
            MethodNode handler=mixin.methods.stream().filter(m->m.name.equals("fan4$observe")).findFirst().orElseThrow();
            String expected=target[3].replace(")V","Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V");
            if(!handler.desc.equals(expected)||(handler.access&ACC_STATIC)!=0||handler.tryCatchBlocks.size()!=1)throw new AssertionError("Wrapper shape changed: "+target[0]);
            AnnotationNode annotation=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow();
            if(!annotation.values.toString().contains(target[2]+target[3]))throw new AssertionError("Wrong selector");
        }
        String[][] fields={
            {"com/mojang/blaze3d/platform/GlStateManager","STENCIL","DEPTH","COLOR_MASK"},
            {"com/mojang/blaze3d/platform/GlStateManager$class_1035","field_5149","field_5153"},
            {"com/mojang/blaze3d/platform/GlStateManager$class_1034","field_5148","field_16203","field_5147"},
            {"com/mojang/blaze3d/platform/GlStateManager$class_1026","field_5076"},
            {"com/mojang/blaze3d/platform/GlStateManager$class_1022","field_5063","field_5062","field_5061","field_5060"},
            {"net/minecraft/class_276","field_1476","field_1474"}};
        for(String[] contract:fields) {
            ClassNode nativeClass=read(args[1],contract[0]);
            for(int i=1;i<contract.length;i++){String name=contract[i];if(nativeClass.fields.stream().noneMatch(f->f.name.equals(name)))throw new AssertionError("Missing diagnostic field "+contract[0]+"."+name);}
        }
        System.out.println("PASS: all four Point Blank/Minecraft render wrappers match native modifiers/descriptors; finally handlers and cached-state fields verified");
    }
}
