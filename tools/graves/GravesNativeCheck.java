import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Verify pinned native APIs and constant selectors without launching Minecraft. */
public final class GravesNativeCheck implements Opcodes {
    static ClassNode read(String jar,String owner)throws Exception {
        try(ZipFile zip=new ZipFile(jar)) {
            var entry=zip.getEntry(owner+".class");if(entry==null)throw new AssertionError("Missing native class: "+owner);
            ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(entry)).accept(node,0);return node;
        }
    }
    static MethodNode method(ClassNode node,String name,String desc) {
        return node.methods.stream().filter(m->m.name.equals(name)&&(desc==null||m.desc.equals(desc))).findFirst().orElseThrow(()->new AssertionError("Missing native method: "+node.name+"."+name+desc));
    }
    static void constants(ClassNode node,String name,int value,int expected) {
        int count=0;
        for(var i:method(node,name,null).instructions) {
            if(i instanceof IntInsnNode n&&n.operand==value)count++;
            if(i instanceof InsnNode&&i.getOpcode()==ICONST_0+value&&value>=0&&value<=5)count++;
            if(i instanceof LdcInsnNode n&&Integer.valueOf(value).equals(n.cst))count++;
        }
        if(count!=expected)throw new AssertionError("Native constant selector changed: "+name+" "+value+" found "+count);
    }
    public static void main(String[] args)throws Exception {
        ClassNode grave=read(args[0],"com/kador/graves/entity/GraveEntity");
        for(String name:new String[]{"<init>","captureInventory","getStoredStack","quickRetrieveAll"})constants(grave,name,45,1);
        method(grave,"createFromPlayer","(Lnet/minecraft/class_3222;)Lcom/kador/graves/entity/GraveEntity;");
        method(grave,"syncFromInventory","(Lnet/minecraft/class_1263;)V");
        method(grave,"quickRetrieveExact","(Lnet/minecraft/class_3222;)I");
        method(grave,"openInventoryScreen","(Lnet/minecraft/class_3222;)V");
        constants(grave,"lambda$openInventoryScreen$0",5,1);
        boolean menuType=false;
        for(var i:method(grave,"lambda$openInventoryScreen$0",null).instructions)if(i instanceof FieldInsnNode f&&f.owner.equals("net/minecraft/class_3917")&&f.name.equals("field_18667"))menuType=true;
        if(!menuType)throw new AssertionError("Native five-row menu type changed");
        ClassNode inventory=read(args[0],"com/kador/graves/inventory/GraveInventory");constants(inventory,"<init>",45,1);
        method(read(args[1],"io/wispforest/accessories/pond/DroppedStacksExtension"),"toBeDroppedStacks","()Ljava/util/Collection;");
        method(read(args[1],"io/wispforest/accessories/pond/DroppedStacksExtension"),"addToBeDroppedStacks","(Ljava/util/Collection;)V");
        method(read(args[1],"io/wispforest/accessories/impl/AccessoriesEventHandler"),"onDeath","(Lnet/minecraft/class_1309;Lnet/minecraft/class_1282;)Ljava/util/Collection;");
        method(read(args[2],"net/minecraft/class_3218"),"method_8649","(Lnet/minecraft/class_1297;)Z");
        method(read(args[2],"net/minecraft/class_1661"),"method_5442","()Z");
        ClassNode screen=read(args[2],"net/minecraft/class_3917");
        if(screen.fields.stream().noneMatch(f->f.name.equals("field_17327")))throw new AssertionError("Missing six-row screen type");
        // Native grave save/load uses Minecraft's complete list codec, not a hardcoded 45-slot loop.
        for(String selector:new String[]{"method_5652","method_5749"}) {
            MethodNode m=method(grave,selector,"(Lnet/minecraft/class_2487;)V");
            boolean codec=false;
            for(var i:m.instructions)if(i instanceof MethodInsnNode invoke&&invoke.owner.equals("net/minecraft/class_1262"))codec=true;
            if(!codec)throw new AssertionError("Native persistence changed: "+selector);
        }
        System.out.println("PASS: supplied Graves 1.0.0 storage/menu/persistence selectors, Accessories beta 48 queue API and Minecraft 1.21.1 spawn/inventory/menu APIs");
    }
}
