import org.objectweb.asm.*;
import java.nio.file.*;
import org.objectweb.asm.commons.*;

public final class ImmersivePortalsGenerator implements Opcodes {
    static final String ROOT=GenerateAddon.ROOT;
    static final Path OUT=GenerateAddon.OUT;
    static void clientBridges()throws Exception {
        ClientBridgeGenerator.main(new String[0]);
        Remapper mapping=new Remapper() {
            public String map(String name) {
                String prefix="org/valkyrienskies/mod/mixin/mod_compat/immersive_portals/";
                if(name.startsWith(prefix))return ROOT+"mixin/immersiveportals/client/"+name.substring(prefix.length());
                if(name.equals("org/valkyrienskies/mod/compat/ImmersivePortalsShipBridge"))return ROOT+"immersiveportals/ShipClientBridge";
                return name;
            }
        };
        for(Path p:Files.walk(ClientBridgeGenerator.OUT).filter(f->f.toString().endsWith(".class")).toList()) {
            ClassReader r=new ClassReader(Files.readAllBytes(p));ClassWriter w=new ClassWriter(0);r.accept(new ClassRemapper(w,mapping),0);
            Path output=OUT.resolve(mapping.map(r.getClassName())+".class");Files.createDirectories(output.getParent());Files.write(output,w.toByteArray());
        }
    }
    static void chunkCacheDuckMixin()throws Exception {
        String ip="qouteall/imm_ptl/core/chunk_loading/ImmPtlClientChunkMap",chunk="net/minecraft/class_2818",pos="net/minecraft/class_1923",ship="org/valkyrienskies/core/api/ships/ClientShip",map="it/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap";
        String name=ROOT+"mixin/immersiveportals/client/MixinShipChunkCacheDuck";ClassWriter w=GenerateAddon.writer(name,ip,"org/valkyrienskies/mod/mixinducks/client/world/ClientChunkCacheDuck");
        // Read IP's existing cache; it remains the sole owner of chunk lifetime.
        MethodVisitor m=w.visitMethod(ACC_PUBLIC,"vs$getShipChunks","()Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;",null,null);m.visitCode();
        m.visitTypeInsn(NEW,map);m.visitInsn(DUP);m.visitMethodInsn(INVOKESPECIAL,map,"<init>","()V",false);m.visitVarInsn(ASTORE,1);
        m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,ip,"getCopiedChunkList","()Ljava/util/List;",false);m.visitMethodInsn(INVOKEINTERFACE,"java/util/List","iterator","()Ljava/util/Iterator;",true);m.visitVarInsn(ASTORE,2);
        Label loop=new Label(),end=new Label();m.visitLabel(loop);m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEINTERFACE,"java/util/Iterator","hasNext","()Z",true);m.visitJumpInsn(IFEQ,end);
        m.visitVarInsn(ALOAD,2);m.visitMethodInsn(INVOKEINTERFACE,"java/util/Iterator","next","()Ljava/lang/Object;",true);m.visitTypeInsn(CHECKCAST,chunk);m.visitVarInsn(ASTORE,3);
        m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,chunk,"method_12004","()L"+pos+";",false);m.visitVarInsn(ASTORE,4);
        m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,chunk,"method_12200","()Lnet/minecraft/class_1937;",false);m.visitVarInsn(ALOAD,4);m.visitFieldInsn(GETFIELD,pos,"field_9181","I");m.visitVarInsn(ALOAD,4);m.visitFieldInsn(GETFIELD,pos,"field_9180","I");m.visitMethodInsn(INVOKESTATIC,"org/valkyrienskies/mod/common/VSGameUtilsKt","isChunkInShipyard","(Lnet/minecraft/class_1937;II)Z",false);m.visitJumpInsn(IFEQ,loop);
        m.visitVarInsn(ALOAD,1);m.visitVarInsn(ALOAD,4);m.visitMethodInsn(INVOKEVIRTUAL,pos,"method_8324","()J",false);m.visitVarInsn(ALOAD,3);m.visitMethodInsn(INVOKEVIRTUAL,map,"put","(JLjava/lang/Object;)Ljava/lang/Object;",false);m.visitInsn(POP);
        m.visitJumpInsn(GOTO,loop);m.visitLabel(end);m.visitVarInsn(ALOAD,1);m.visitInsn(ARETURN);m.visitMaxs(0,0);m.visitEnd();
        // VS metadata unloads independently of IP watches. Deleting IP chunks here
        // leaves its server delivery records valid, so it never sends them again.
        // IP's normal unload packets still evict chunks and notify its renderer.
        m=w.visitMethod(ACC_PUBLIC,"vs$removeShip","(L"+ship+";)V",null,null);m.visitCode();m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
        GenerateAddon.save(name,w);
    }
}
