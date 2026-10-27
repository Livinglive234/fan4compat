import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * The Sodium ship rendering and portal occlusion are compiled against stand-ins, so this checks the real thing: every Sodium, Valkyrien Skies, Immersive Portals,
 * Minecraft and JOML member that ShipSections, PortalOcclusion and the generated mixins call, read or name must exist in the real jars with the exact descriptor (and the right static-ness), and the members the mixins target and shadow
 * must exist on RenderSectionManager and the OcclusionCuller. Args: the compiled main classes directory, the generated classes directory, then the jars (Sodium 0.8,
 * Valkyrien Skies, an intermediary-named Minecraft jar, JOML, Immersive Portals).
 */
public final class SodiumShipNativeCheck {
    static final String SODIUM="net/caffeinemc/mods/sodium/client/",MANAGER=SODIUM+"render/chunk/RenderSectionManager",ROOT="dev/fan4/compat/";
    public static void main(String[] args)throws Exception {
        XaeroSyncNativeCheck.jars=Arrays.copyOfRange(args,2,args.length);
        String section=SODIUM+"render/chunk/RenderSection";
        // What the generated mixin injects into and shadows.
        String vp=SODIUM+"render/viewport/Viewport",lists=SODIUM+"render/chunk/lists/SortedRenderLists",collector=SODIUM+"render/chunk/lists/SectionCollector";
        member(MANAGER,"createTerrainRenderList","(Lnet/minecraft/class_4184;L"+vp+";IZ)Z",false,false);
        member(MANAGER,"tickVisibleRenders","()V",false,false);
        member(MANAGER,"resetRenderLists","()V",false,false);
        member(MANAGER,"renderLayer","(L"+SODIUM+"render/chunk/ChunkRenderMatrices;L"+SODIUM+"render/chunk/terrain/TerrainRenderPass;DDD)V",false,false);
        member(MANAGER,"getRenderSection","(III)L"+section+";",false,false);
        for(String[] f:new String[][]{{"level","Lnet/minecraft/class_638;"},{"chunkRenderer","L"+SODIUM+"render/chunk/ChunkRenderer;"},{"sortBehavior","L"+SODIUM+"render/chunk/translucent_sorting/SortBehavior;"},
            {"renderLists","L"+lists+";"},{"taskLists","Ljava/util/Map;"},{"sectionCollector","L"+collector+";"}})member(MANAGER,f[0],f[1],true,false);
        member(SODIUM+"render/chunk/translucent_sorting/SortBehavior","OFF","L"+SODIUM+"render/chunk/translucent_sorting/SortBehavior;",true,true);
        // The collector's queue types, read by reflection.
        member(collector,"importantRebuildQueueType","L"+SODIUM+"render/chunk/TaskQueueType;",true,false);
        member(collector,"importantSortQueueType","L"+SODIUM+"render/chunk/TaskQueueType;",true,false);
        // VS's duck, which the mixin implements and VS's other mixins read.
        member("org/valkyrienskies/mod/mixinducks/mod_compat/sodium/RenderSectionManagerDuck","vs_getShipRenderLists","()Ljava/util/WeakHashMap;",false,false);
        // The OcclusionCuller hooks of the portal occlusion, and the Viewport call they redirect.
        String cull=SODIUM+"render/chunk/occlusion/OcclusionCuller",visitor=SODIUM+"render/chunk/lists/RenderSectionVisitor",queue=SODIUM+"util/collections/WriteQueue";
        member(cull,"findVisible","(L"+visitor+";L"+vp+";FZI)V",false,false);
        member(cull,"init","(L"+visitor+";L"+queue+";L"+vp+";ZI)V",false,false);
        member(cull,"initWithinWorld","(L"+visitor+";L"+queue+";L"+vp+";ZI)V",false,false);
        member(cull,"isWithinFrustum","(L"+vp+";L"+section+";)Z",false,true);
        member(cull,"getRenderSection","(III)L"+section+";",false,false);
        member(vp,"getChunkCoord","()Lnet/minecraft/class_4076;",false,false);
        // The Immersive Portals mixins that are cancelled for Sodium 0.8 must exist under those names.
        XaeroSyncNativeCheck.find("qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumViewport");
        XaeroSyncNativeCheck.find("qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumOcclusionCuller");
        // Every external reference the compiled code makes.
        int checked=0;
        List<Path> files=new ArrayList<>();
        for(int i=0;i<2;i++)try(Stream<Path> walk=Files.walk(Path.of(args[i]))){
            walk.filter(p->p.toString().replace('\\','/').matches(".*/dev/fan4/compat/(sodium/(ShipSections|PortalOcclusion).*|mixin/sodium/client/Sodium(ShipRendering|PortalOcclusion)Mixin)\\.class")).forEach(files::add);
        }
        if(files.size()<5)throw new AssertionError("Expected the helpers and the generated mixins, found "+files);
        for(Path file:files) {
            ClassNode node=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(node,0);
            for(MethodNode m:node.methods) {
                if(m.instructions==null)continue;
                for(AbstractInsnNode in=m.instructions.getFirst();in!=null;in=in.getNext()) {
                    if(in instanceof MethodInsnNode mi&&external(mi.owner,node.name)){member(mi.owner,mi.name,mi.desc,false,mi.getOpcode()==Opcodes.INVOKESTATIC);checked++;}
                    else if(in instanceof FieldInsnNode fi&&external(fi.owner,node.name)){member(fi.owner,fi.name,fi.desc,true,fi.getOpcode()==Opcodes.GETSTATIC||fi.getOpcode()==Opcodes.PUTSTATIC);checked++;}
                    else if(in instanceof TypeInsnNode ti&&external(ti.desc,node.name)){XaeroSyncNativeCheck.find(ti.desc);checked++;}
                    else if(in instanceof InvokeDynamicInsnNode indy) {
                        // The functional interface a lambda implements: its return type, with the method it implements.
                        String iface=Type.getReturnType(indy.desc).getInternalName();
                        if(external(iface,node.name)&&indy.bsmArgs.length>0&&indy.bsmArgs[0] instanceof Type erased){member(iface,indy.name,erased.getDescriptor(),false,false);checked++;}
                    }
                }
            }
        }
        System.out.println("PASS: every Sodium, Valkyrien Skies, Minecraft and JOML member the Sodium ship rendering names ("+checked+" references in "+files.size()+" classes) exists in the real jars with the exact descriptors");
    }
    static boolean external(String owner,String self){
        return owner!=null&&!owner.startsWith("[")&&!owner.startsWith("java/")&&!owner.startsWith("jdk/")&&!owner.startsWith(ROOT)&&!owner.equals(self)
            &&!owner.startsWith("org/spongepowered/")&&!owner.startsWith("com/llamalad7/");
    }
    /** The member must exist on the owner or something it extends or implements, with the exact descriptor and static-ness. */
    static void member(String owner,String name,String desc,boolean field,boolean statik)throws Exception {
        Deque<String> pending=new ArrayDeque<>(List.of(owner));Set<String> seen=new HashSet<>();
        while(!pending.isEmpty()) {
            String current=pending.removeFirst();
            if(current==null||!seen.add(current))continue;
            if(current.startsWith("java/")) {
                // A JDK supertype (for example Iterable): ask the running JDK.
                Class<?> jdk=Class.forName(current.replace('/','.'));
                if(!field)for(java.lang.reflect.Method m:jdk.getMethods())
                    if(m.getName().equals(name)&&java.lang.invoke.MethodType.methodType(m.getReturnType(),m.getParameterTypes()).toMethodDescriptorString().equals(desc))return;
                continue;
            }
            ClassNode node;try{node=XaeroSyncNativeCheck.find(current);}catch(AssertionError missing){if(current.equals(owner))throw missing;continue;}
            if(field){for(FieldNode f:node.fields)if(f.name.equals(name)&&f.desc.equals(desc)){if(((f.access&Opcodes.ACC_STATIC)!=0)!=statik)throw new AssertionError("Static mismatch: "+owner+"."+name);return;}}
            else for(MethodNode m:node.methods)if(m.name.equals(name)&&m.desc.equals(desc)){if(((m.access&Opcodes.ACC_STATIC)!=0)!=statik)throw new AssertionError("Static mismatch: "+owner+"."+name+desc);return;}
            pending.add(node.superName);pending.addAll(node.interfaces);
        }
        throw new AssertionError("Missing "+(field?"field ":"method ")+owner+"."+name+(field?" ":"")+desc);
    }
}
