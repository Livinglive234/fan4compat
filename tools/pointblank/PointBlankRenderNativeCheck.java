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
        for(String[] binding:new String[][]{{"GunFramebufferMixin","com/vicmatskiv/pointblank/client/ClientSystem"},{"GunAuxFramebufferMixin","com/vicmatskiv/pointblank/client/render/AuxLevelRenderer"}}) {
            ClassNode nativeClass=read(args[0],binding[1]),mixin=new ClassNode();
            new ClassReader(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("build/generated/classes/dev/fan4/compat/mixin/pointblank/client/"+binding[0]+".class"))).accept(mixin,0);
            VerifyAddon.classes.put(nativeClass.name,nativeClass);VerifyAddon.injectionTargets(mixin);
            MethodNode handler=mixin.methods.stream().filter(m->m.name.equals("fan4$preserveStencilFramebuffer")).findFirst().orElseThrow();
            if((handler.access&ACC_STATIC)!=0||!handler.desc.equals("(Lcom/vicmatskiv/pointblank/client/render/RenderTargetExt;)V"))throw new AssertionError("Framebuffer redirect receiver mismatch");
        }
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
        try(var generated=java.nio.file.Files.list(java.nio.file.Path.of("build/generated/classes/dev/fan4/compat/mixin/pointblank/client"))) {
            for(var file:generated.filter(p->p.getFileName().toString().endsWith("DiagnosticMixin.class")).toList()) {
                String simple=file.getFileName().toString();
                if(simple.startsWith("GunGui")&&args.length<3)continue;
                if(!simple.startsWith("GunScope")&&!simple.startsWith("GunDefault")&&!simple.startsWith("GunIris")&&!simple.startsWith("GunGui"))continue;
                ClassNode mixin=new ClassNode();new ClassReader(java.nio.file.Files.readAllBytes(file)).accept(mixin,0);
                AnnotationNode annotation=mixin.invisibleAnnotations.stream().filter(a->a.desc.endsWith("/Mixin;")).findFirst().orElseThrow();
                String owner=((java.util.List<?>)VerifyAddon.value(annotation,"targets")).get(0).toString().replace('.','/');
                ClassNode nativeClass=read(simple.startsWith("GunGui")?args[2]:args[0],owner);
                MethodNode handler=mixin.methods.stream().filter(m->m.name.equals("fan4$observe")).findFirst().orElseThrow();
                AnnotationNode wrap=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/WrapMethod;")).findFirst().orElseThrow();
                String selector=((java.util.List<?>)VerifyAddon.value(wrap,"method")).get(0).toString();
                MethodNode nativeMethod=nativeClass.methods.stream().filter(m->(m.name+m.desc).equals(selector)).findFirst().orElseThrow();
                if((nativeMethod.access&ACC_STATIC)==0||(handler.access&ACC_STATIC)==0||((nativeClass.access^mixin.access)&ACC_INTERFACE)!=0||handler.tryCatchBlocks.size()!=1)throw new AssertionError("Static/interface/finally mismatch: "+simple);
                if(!handler.desc.equals(nativeMethod.desc.replace(")V","Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V")))throw new AssertionError("Diagnostic arguments differ: "+simple);
            }
        }
        System.out.println("PASS: deferred scope callbacks and GUI camera wrappers match native methods/static/interface/finally contracts");
        validateStencil(args);
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
    static void validateStencil(String[] args)throws Exception {
        for(String simple:args.length>2?new String[]{"GunStencilMixin","GunStencilClearMixin","IrisScopeStencilMixin","PortalStencilCacheMixin"}:new String[]{"GunStencilMixin","GunStencilClearMixin","IrisScopeStencilMixin"}) {
            boolean iris=simple.equals("IrisScopeStencilMixin"),provider=simple.equals("GunStencilMixin"),portal=simple.equals("PortalStencilCacheMixin");
            String owner=portal?"qouteall/imm_ptl/core/render/renderer/RendererUsingStencil":iris?"com/vicmatskiv/pointblank/compat/iris/IrisRenderTypeProvider":provider?StencilGenerator.PROVIDER:"com/vicmatskiv/pointblank/client/ClientSystem";
            ClassNode nativeClass=read(portal?args[2]:args[0],owner),mixin=new ClassNode();
            new ClassReader(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("build/generated/classes/dev/fan4/compat/mixin/pointblank/client/"+simple+".class"))).accept(mixin,0);
            if(((nativeClass.access^mixin.access)&ACC_INTERFACE)!=0)throw new AssertionError("Mixin interface/class mismatch");
            for(MethodNode handler:mixin.methods) {
                if((handler.access&ACC_STATIC)!=(provider||portal||iris?ACC_STATIC:0))throw new AssertionError("Redirect static-ness mismatch");
                AnnotationNode annotation=handler.visibleAnnotations.stream().filter(a->a.desc.endsWith("/Redirect;")).findFirst().orElseThrow();
                java.util.Map<String,Object> properties=new java.util.HashMap<>();for(int i=0;i<annotation.values.size();i+=2)properties.put((String)annotation.values.get(i),annotation.values.get(i+1));
                AnnotationNode at=(AnnotationNode)properties.get("at");String target=null;for(int i=0;i<at.values.size();i+=2)if(at.values.get(i).equals("target"))target=(String)at.values.get(i+1);
                int split=target.indexOf(';'),paren=target.indexOf('(');String callOwner=target.substring(1,split),name=target.substring(split+1,paren),desc=target.substring(paren);
                if(!handler.desc.equals(desc))throw new AssertionError("Redirect arguments differ from invocation");
                int matches=0;
                for(Object selector:(java.util.List<?>)properties.get("method")) {
                    String selected=(String)selector;MethodNode method=nativeClass.methods.stream().filter(m->(m.name+m.desc).equals(selected)).findFirst().orElseThrow();
                    if(!portal&&(method.access&ACC_STATIC)!=(provider||iris?ACC_STATIC:0))throw new AssertionError("Native method static-ness mismatch");
                    for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals(callOwner)&&call.name.equals(name)&&call.desc.equals(desc))matches++;
                }
                if(matches<1||!Integer.valueOf(1).equals(properties.get("require")))throw new AssertionError("Missing required stencil invocation");
            }
        }
        System.out.println("PASS: all stencil redirects bind native calls with correct static and interface types");
    }

}
