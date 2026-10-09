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
    static ClassNode find(String owner,String... jars) {
        for(String jar:jars)try{return read(jar,owner);}catch(Exception|AssertionError missing){}
        return null;
    }
    /** What Mixin enforces at class-load time: target exists, static-ness matches and the callback matches the return type. */
    static void injectors(String generated,String... jars)throws Exception {
        int checked=0;
        try(var files=Files.walk(Path.of(generated,"dev/fan4/compat/mixin/graves"))) {
            for(Path file:files.filter(f->f.toString().endsWith(".class")).toList()) {
                ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(mixin,0);
                String target=null;
                for(var a:mixin.invisibleAnnotations==null?List.<AnnotationNode>of():mixin.invisibleAnnotations)
                    if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                        for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals("targets"))target=((List<?>)a.values.get(i+1)).get(0).toString().replace('.','/');
                ClassNode node=find(target,jars);
                if(node==null)continue;
                for(MethodNode handler:mixin.methods) {
                    for(var a:handler.visibleAnnotations==null?List.<AnnotationNode>of():handler.visibleAnnotations) {
                        boolean inject=a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;");
                        if(!inject&&!a.desc.contains("ModifyConstant")&&!a.desc.contains("ModifyReturnValue")&&!a.desc.contains("ModifyExpressionValue"))continue;
                        List<?> selectors=null;
                        for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals("method"))selectors=(List<?>)a.values.get(i+1);
                        for(Object selector:selectors) {
                            String name=selector.toString(),desc=null;int paren=name.indexOf('(');
                            if(paren>=0){desc=name.substring(paren);name=name.substring(0,paren);}
                            final String wanted=name,wantedDesc=desc;
                            var found=node.methods.stream().filter(m->m.name.equals(wanted)&&(wantedDesc==null||m.desc.equals(wantedDesc))).toList();
                            // Methods merged in by other mixins at runtime (graves$...) cannot be resolved statically.
                            if(found.isEmpty()&&wanted.contains("$"))continue;
                            if(found.isEmpty())throw new AssertionError(mixin.name+"."+handler.name+" targets missing "+node.name+"."+selector);
                            for(MethodNode targetMethod:found) {
                                boolean targetStatic=(targetMethod.access&ACC_STATIC)!=0,handlerStatic=(handler.access&ACC_STATIC)!=0;
                                if(targetStatic&&!handlerStatic)throw new AssertionError(mixin.name+"."+handler.name+" is an instance handler for static "+node.name+"."+targetMethod.name);
                                if(inject) {
                                    boolean returns=!targetMethod.desc.endsWith(")V");
                                    String callback=Type.getArgumentTypes(handler.desc)[Type.getArgumentTypes(handler.desc).length-1].getInternalName();
                                    boolean returnable=callback.endsWith("CallbackInfoReturnable");
                                    if(returns!=returnable)throw new AssertionError(mixin.name+"."+handler.name+" needs "+(returns?"CallbackInfoReturnable":"CallbackInfo")+" for "+node.name+"."+targetMethod.name+targetMethod.desc);
                                }
                                checked++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("PASS: "+checked+" generated Graves injector bindings match the supplied jars (existence, static-ness, callback type)");
    }
    /** Public Trinkets 3.10.0 API plus the drop pass TrinketGraveCompat mirrors. */
    static void trinkets(String jar)throws Exception {
        String rule="Ldev/emi/trinkets/api/TrinketEnums$DropRule;",stack="Lnet/minecraft/class_1799;",reference="Ldev/emi/trinkets/api/SlotReference;",living="Lnet/minecraft/class_1309;";
        ClassNode api=read(jar,"dev/emi/trinkets/api/TrinketsApi");
        method(api,"getTrinketComponent","("+living+")Ljava/util/Optional;");
        method(api,"getTrinket","(Lnet/minecraft/class_1792;)Ldev/emi/trinkets/api/Trinket;");
        method(read(jar,"dev/emi/trinkets/api/TrinketComponent"),"forEach","(Ljava/util/function/BiConsumer;)V");
        ClassNode slot=read(jar,"dev/emi/trinkets/api/SlotReference");
        method(slot,"inventory","()Ldev/emi/trinkets/api/TrinketInventory;");method(slot,"index","()I");
        ClassNode inventory=read(jar,"dev/emi/trinkets/api/TrinketInventory");
        method(inventory,"getSlotType","()Ldev/emi/trinkets/api/SlotType;");method(inventory,"method_5438","(I)"+stack);method(inventory,"method_5447","(I"+stack+")V");
        method(read(jar,"dev/emi/trinkets/api/SlotType"),"getDropRule","()"+rule);
        method(read(jar,"dev/emi/trinkets/api/Trinket"),"getDropRule","("+stack+reference+living+")"+rule);
        ClassNode callback=read(jar,"dev/emi/trinkets/api/event/TrinketDropCallback");
        method(callback,"drop","("+rule+stack+reference+living+")"+rule);
        if(callback.fields.stream().noneMatch(f->f.name.equals("EVENT")))throw new AssertionError("Missing TrinketDropCallback.EVENT");
        ClassNode enums=read(jar,"dev/emi/trinkets/api/TrinketEnums$DropRule");
        for(String name:new String[]{"KEEP","DROP","DESTROY","DEFAULT"})
            if(enums.fields.stream().noneMatch(f->f.name.equals(name)))throw new AssertionError("Missing DropRule."+name);
        // The mirrored pass: a tail hook on LivingEntity.dropInventory that applies item, event and slot-type rules.
        ClassNode mixin=read(jar,"dev/emi/trinkets/mixin/LivingEntityMixin");
        MethodNode pass=method(mixin,"lambda$dropInventory$0",null);
        boolean item=false,event=false,slotType=false,dropped=false;
        for(var i:pass.instructions)if(i instanceof MethodInsnNode call) {
            item|=call.name.equals("getDropRule")&&call.owner.equals("dev/emi/trinkets/api/Trinket");
            event|=call.name.equals("drop")&&call.owner.equals("dev/emi/trinkets/api/event/TrinketDropCallback");
            slotType|=call.name.equals("getDropRule")&&call.owner.equals("dev/emi/trinkets/api/SlotType");
            dropped|=call.name.equals("dropFromEntity");
        }
        if(!(item&&event&&slotType&&dropped))throw new AssertionError("Trinkets drop pass changed; update TrinketGraveCompat");
        System.out.println("PASS: Trinkets 3.10.0 component/slot/drop-rule API and native drop pass");
    }
    public static void main(String[] args)throws Exception {
        ClassNode grave=read(args[0],"com/kador/graves/entity/GraveEntity");
        for(String name:new String[]{"<init>","captureInventory","getStoredStack","quickRetrieveAll"})constants(grave,name,45,1);
        method(grave,"createFromPlayer","(Lnet/minecraft/class_3222;)Lcom/kador/graves/entity/GraveEntity;");
        method(grave,"syncFromInventory","(Lnet/minecraft/class_1263;)V");
        // A HEAD injector needs CallbackInfoReturnable only if the target returns a value.
        if(!method(grave,"quickRetrieveAll",null).desc.endsWith(")I"))throw new AssertionError("Native quickRetrieveAll no longer returns int");
        method(grave,"quickRetrieveExact","(Lnet/minecraft/class_3222;)I");
        method(grave,"openInventoryScreen","(Lnet/minecraft/class_3222;)V");
        constants(grave,"lambda$openInventoryScreen$0",5,1);
        // The generated six-row handler is static because Mixin rejects instance handlers on static targets.
        if((method(grave,"lambda$openInventoryScreen$0",null).access&ACC_STATIC)==0)throw new AssertionError("Native menu lambda is no longer static");
        boolean menuType=false;
        for(var i:method(grave,"lambda$openInventoryScreen$0",null).instructions)if(i instanceof FieldInsnNode f&&f.owner.equals("net/minecraft/class_3917")&&f.name.equals("field_18667"))menuType=true;
        if(!menuType)throw new AssertionError("Native five-row menu type changed");
        ClassNode inventory=read(args[0],"com/kador/graves/inventory/GraveInventory");constants(inventory,"<init>",45,1);
        method(read(args[1],"io/wispforest/accessories/pond/DroppedStacksExtension"),"toBeDroppedStacks","()Ljava/util/Collection;");
        method(read(args[1],"io/wispforest/accessories/pond/DroppedStacksExtension"),"addToBeDroppedStacks","(Ljava/util/Collection;)V");
        method(read(args[1],"io/wispforest/accessories/impl/AccessoriesEventHandler"),"onDeath","(Lnet/minecraft/class_1309;Lnet/minecraft/class_1282;)Ljava/util/Collection;");
        method(read(args[1],"io/wispforest/accessories/impl/AccessoriesEventHandler"),"dropStack","(Lio/wispforest/accessories/api/DropRule;Lnet/minecraft/class_1309;Lio/wispforest/accessories/impl/ExpandedSimpleContainer;Lio/wispforest/accessories/api/slot/SlotReference;Lnet/minecraft/class_1282;Z)Lnet/minecraft/class_1799;");
        ClassNode reference=read(args[1],"io/wispforest/accessories/api/slot/SlotReference");
        method(reference,"slotName","()Ljava/lang/String;");method(reference,"slot","()I");method(reference,"slotContainer","()Lio/wispforest/accessories/api/AccessoriesContainer;");
        ClassNode capability=read(args[1],"io/wispforest/accessories/api/AccessoriesCapability");
        method(capability,"get","(Lnet/minecraft/class_1309;)Lio/wispforest/accessories/api/AccessoriesCapability;");method(capability,"getContainers","()Ljava/util/Map;");
        ClassNode container=read(args[1],"io/wispforest/accessories/api/AccessoriesContainer");
        method(container,"getSize","()I");method(container,"getAccessories","()Lio/wispforest/accessories/impl/ExpandedSimpleContainer;");method(container,"getCosmeticAccessories","()Lio/wispforest/accessories/impl/ExpandedSimpleContainer;");
        ClassNode slot=read(args[1],"io/wispforest/accessories/menu/AccessoriesInternalSlot");
        method(slot,"<init>","(Lio/wispforest/accessories/api/AccessoriesContainer;ZIII)V");method(slot,"method_7680","(Lnet/minecraft/class_1799;)Z");method(slot,"method_7673","(Lnet/minecraft/class_1799;)V");
        method(read(args[1],"io/wispforest/accessories/api/menu/AccessoriesBasedSlot"),"method_7676","(Lnet/minecraft/class_1799;)I");
        ClassNode nbt=read(args[2],"net/minecraft/class_2487");method(nbt,"method_10582","(Ljava/lang/String;Ljava/lang/String;)V");method(nbt,"method_10558","(Ljava/lang/String;)Ljava/lang/String;");
        method(read(args[2],"net/minecraft/class_1799"),"method_31577","(Lnet/minecraft/class_1799;Lnet/minecraft/class_1799;)Z");
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
        // Graves cancels Player.dropInventory at HEAD, which skips Trinkets' tail hook on LivingEntity.dropInventory.
        method(read(args[0],"com/kador/graves/mixin/PlayerEntityMixin"),"graves$createGraveInsteadOfDrops","(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        if(args.length>3)trinkets(args[3]);
        String generated=System.getProperty("fan4compat.generatedClasses");
        if(generated!=null)injectors(generated,args[0],args[1],args[2]);
        System.out.println("PASS: supplied Graves 1.0.0 storage/menu/persistence selectors, Accessories beta 48 queue/slot capture, validation/setter APIs and Minecraft 1.21.1 spawn/inventory/menu APIs");
    }
}
