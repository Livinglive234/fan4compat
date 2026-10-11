import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public final class DhDiagnosticsNativeCheck {
    public static void main(String[] args)throws Exception {
        VerifyAddon.readJar(args[0]);VerifyAddon.readJar(args[1]);
        for(String name:new String[]{"DhLodDiagnosticMixin","DhOpaqueFadeDiagnosticMixin","DhTransparentFadeDiagnosticMixin","DhDepthDiagnosticMixin"}) {
            ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/distanthorizons/client/"+name+".class"))).accept(node,0);
            VerifyAddon.injectionTargets(node);
            if(!name.equals("DhDepthDiagnosticMixin"))for(MethodNode m:node.methods){
                if(m.tryCatchBlocks.size()!=1||(m.access&Opcodes.ACC_STATIC)!=0)throw new AssertionError("Instance observer must preserve native exceptions");
                AnnotationNode wrap=m.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow();
                String selector=((java.util.List<?>)VerifyAddon.value(wrap,"method")).get(0).toString();
                MethodNode nativeMethod=VerifyAddon.classes.get("com/seibel/distanthorizons/core/api/internal/ClientApi").methods.stream().filter(n->(n.name+n.desc).equals(selector)).findFirst().orElseThrow();
                if((nativeMethod.access&Opcodes.ACC_STATIC)!=0||!m.desc.equals(nativeMethod.desc.replace(")V","Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V")))throw new AssertionError("Native observer descriptor differs");
            }
        }
        ClassNode repair=new ClassNode();new ClassReader(Files.readAllBytes(Path.of("build/generated/classes/dev/fan4/compat/mixin/iris/client/DhDepthAttachmentMixin.class"))).accept(repair,0);VerifyAddon.injectionTargets(repair);
        System.out.println("PASS: DH layer/fade wrappers and Iris depth getter selectors match native jars");
    }
}
