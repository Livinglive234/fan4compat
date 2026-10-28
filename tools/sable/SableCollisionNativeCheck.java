import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * The real Sable and Immersive Portals jars (args: Sable jar, Immersive Portals jar) must have what sable.IpCollisionAdjuster relies on: Immersive Portals' collision
 * redirect handler with its @Redirect on Entity.move and a single call to the plain Entity.collide, and Sable's handler for the same call. Sable's mixin must have the higher priority number.
 */
public final class SableCollisionNativeCheck implements Opcodes {
    static ClassNode read(String jar,String name)throws Exception {
        try(ZipFile zip=new ZipFile(jar)) {
            var entry=zip.getEntry(name+".class");
            if(entry==null)throw new AssertionError("Missing class "+name+" in "+jar);
            ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(entry)).accept(node,0);return node;
        }
    }
    static MethodNode method(ClassNode node,String name,String desc){
        return node.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new AssertionError("Missing "+node.name+"."+name+desc));
    }
    /** Mixin injector annotations have runtime retention, so they are the visible ones. */
    static java.util.stream.Stream<AnnotationNode> annotations(MethodNode method){
        java.util.List<AnnotationNode> all=new java.util.ArrayList<>();
        if(method.visibleAnnotations!=null)all.addAll(method.visibleAnnotations);
        if(method.invisibleAnnotations!=null)all.addAll(method.invisibleAnnotations);
        return all.stream();
    }
    /** An annotation's values as text, nested annotations and lists included. */
    static String flat(Object value){
        if(value instanceof AnnotationNode node)return node.desc+(node.values==null?"":flat(node.values));
        if(value instanceof java.util.List<?> list){StringBuilder out=new StringBuilder("[");for(Object o:list)out.append(flat(o)).append(' ');return out.append(']').toString();}
        return String.valueOf(value);
    }
    public static void main(String[] args)throws Exception {
        String handlerDesc="(Lnet/minecraft/class_1297;Lnet/minecraft/class_243;)Lnet/minecraft/class_243;";
        ClassNode ip=read(args[1],"qouteall/imm_ptl/core/mixin/common/collision/MixinEntity");
        MethodNode ipHandler=method(ip,"redirectHandleCollisions",handlerDesc);
        AnnotationNode redirect=annotations(ipHandler).filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")).findFirst().orElseThrow(()->new AssertionError("Immersive Portals' handler is not a @Redirect"));
        if(!flat(redirect.values).contains("Entity;move("))throw new AssertionError("Immersive Portals' redirect no longer targets Entity.move: "+flat(redirect.values));
        ClassNode sable=read(args[0],"dev/ryanhcode/sable/mixin/entity/entity_sublevel_collision/EntityMixin");
        MethodNode sableHandler=method(sable,"sable$collideRedirect",handlerDesc);
        int collides=0;
        for(AbstractInsnNode in=ipHandler.instructions.getFirst();in!=null;in=in.getNext())
            if(in instanceof MethodInsnNode call&&call.name.equals("method_17835")&&call.desc.equals("(Lnet/minecraft/class_243;)Lnet/minecraft/class_243;"))collides++;
        if(collides!=1)throw new AssertionError("Immersive Portals' handler should call Entity.collide exactly once (the call Sable's redirect takes over), found "+collides);
        boolean sableRedirectsMove=annotations(sableHandler).anyMatch(a->a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")&&flat(a.values).contains("method_5784")&&flat(a.values).contains("method_17835"));
        if(!sableRedirectsMove)throw new AssertionError("Sable's handler is no longer a @Redirect of Entity.collide in Entity.move: "+sableHandler.visibleAnnotations);
        int sablePriority=-1,ipPriority=-1;
        for(AnnotationNode a:sable.invisibleAnnotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))for(int i=0;i+1<a.values.size();i+=2)if(a.values.get(i).equals("priority"))sablePriority=(Integer)a.values.get(i+1);
        for(AnnotationNode a:ip.invisibleAnnotations)if(a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))for(int i=0;i+1<a.values.size();i+=2)if(a.values.get(i).equals("priority"))ipPriority=(Integer)a.values.get(i+1);
        if(ipPriority>=0&&sablePriority<=ipPriority)throw new AssertionError("Sable's mixin must have the higher priority number (applied after Immersive Portals'): "+sablePriority+" vs "+ipPriority);
        // Player.canPlayerFitWithinBlocksAndEntitiesWhen: Immersive Portals overwrites it, Sable wraps the noCollision call in it.
        ClassNode ipPlayer=read(args[1],"qouteall/imm_ptl/core/mixin/common/collision/MixinPlayer_Collision");
        MethodNode overwrite=method(ipPlayer,"method_52558","(Lnet/minecraft/class_4050;)Z");
        if(annotations(overwrite).noneMatch(x->x.desc.equals("Lorg/spongepowered/asm/mixin/Overwrite;")))throw new AssertionError("Immersive Portals' Player.method_52558 is no longer an @Overwrite");
        ClassNode duck=read(args[1],"qouteall/imm_ptl/core/ducks/IEEntity");
        method(duck,"ip_getActiveCollisionBox","(Lnet/minecraft/class_238;)Lnet/minecraft/class_238;");
        ClassNode sablePlayer=read(args[0],"dev/ryanhcode/sable/mixin/player_standup/PlayerMixin");
        boolean wraps=sablePlayer.methods.stream().anyMatch(m->annotations(m).anyMatch(x->x.desc.endsWith("WrapOperation;")&&flat(x.values).contains("method_52558")&&flat(x.values).contains("method_8587")));
        if(!wraps)throw new AssertionError("Sable's PlayerMixin no longer wraps Level.noCollision in Player.method_52558");
        System.out.println("PASS: Immersive Portals' collision redirect and Sable's collide handler are where the adjuster expects them in the real jars");
    }
}
