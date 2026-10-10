import java.nio.file.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Verify the observation hook against the real IP renderer, not fixture selectors. */
public final class PortalDepthDiagnosticsNativeCheck {
    public static void main(String[] args)throws Exception {
        String owner="qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer";
        ClassNode target=new ClassNode();
        try(ZipFile jar=new ZipFile(args[0])){new ClassReader(jar.getInputStream(jar.getEntry(owner+".class"))).accept(target,0);}
        MethodNode method=target.methods.stream().filter(m->m.name.equals("doMainRenderings")&&m.desc.equals("(Lorg/joml/Matrix4f;)V")).findFirst().orElseThrow();
        int errors=0,blits=0,fallbacks=0;
        for(var i:method.instructions) {
            if(i instanceof MethodInsnNode m&&m.owner.equals("org/lwjgl/opengl/GL11")&&m.name.equals("glGetError")&&m.desc.equals("()I"))errors++;
            if(i instanceof MethodInsnNode m&&m.owner.equals("org/lwjgl/opengl/GL30")&&m.name.equals("glBlitFramebuffer"))blits++;
            if(i instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals("qouteall/imm_ptl/core/IPGlobal")&&f.name.equals("renderMode"))fallbacks++;
        }
        if(errors!=1||blits!=1||fallbacks!=1)throw new AssertionError("Depth-copy/error/fallback path changed: "+blits+"/"+errors+"/"+fallbacks);
        ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[1],"dev/fan4/compat/mixin/iris/client/IrisPortalDepthDiagnosticMixin.class"))).accept(mixin,0);
        VerifyAddon.classes.put(owner,target);VerifyAddon.injectionTargets(mixin);
        MethodNode handler=mixin.methods.stream().filter(m->m.name.equals("fan4$observeDepthCopy")).findFirst().orElseThrow();
        if(!handler.desc.equals("(I)I")||(handler.access&Opcodes.ACC_STATIC)!=0)throw new AssertionError("Error observer must preserve instance expression signature");
        System.out.println("PASS: native IP depth-copy/error/fallback path and diagnostic injection selector/signature");
    }
}
