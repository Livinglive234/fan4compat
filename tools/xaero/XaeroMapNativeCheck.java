import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Real Xaero World Map, Doctor Who Mod (Fabric) and intermediary-named Minecraft jars: the generated hook binds to
 * GuiMap's constructor, and every member TardisMapCompat reaches reflectively exists.
 * Arguments: world map jar, DWM jar, Minecraft jar, generated classes directory.
 */
public final class XaeroMapNativeCheck implements Opcodes {
    static String[] jars;
    static ClassNode find(String owner)throws Exception {
        for(String jar:jars)try(ZipFile zip=new ZipFile(jar)) {
            var entry=zip.getEntry(owner+".class");
            if(entry!=null){ClassNode node=new ClassNode();new ClassReader(zip.getInputStream(entry)).accept(node,0);return node;}
        }
        throw new AssertionError("Missing native class: "+owner);
    }
    static void method(String owner,String name,String parameters,boolean statik)throws Exception {
        Deque<String> pending=new ArrayDeque<>(List.of(owner));Set<String> seen=new HashSet<>();
        while(!pending.isEmpty()) {
            String current=pending.removeFirst();if(current==null||!seen.add(current))continue;
            ClassNode node;try{node=find(current);}catch(AssertionError external){continue;}
            for(MethodNode m:node.methods)if(m.name.equals(name)&&m.desc.startsWith(parameters)&&(m.access&ACC_PUBLIC)!=0&&((m.access&ACC_STATIC)!=0)==statik)return;
            pending.add(node.superName);pending.addAll(node.interfaces);
        }
        throw new AssertionError("Missing native method: "+owner+"."+name+parameters+(statik?" (static)":""));
    }
    static void field(String owner,String name,String desc,boolean statik)throws Exception {
        for(FieldNode f:find(owner).fields)if(f.name.equals(name)&&(desc==null||f.desc.equals(desc))&&((f.access&ACC_STATIC)!=0)==statik)return;
        throw new AssertionError("Missing native field: "+owner+"."+name);
    }
    public static void main(String[] args)throws Exception {
        jars=Arrays.copyOf(args,3);
        String mc="net/minecraft/",guiMap="xaero/map/gui/GuiMap",key="Lnet/minecraft/class_5321;";
        // The hook must bind to the real constructor.
        ClassNode gui=find(guiMap);
        ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(Path.of(args[3],"dev/fan4/compat/mixin/xaero/client/TardisMapMixin.class"))).accept(mixin,0);
        VerifyAddon.classes.put(guiMap,gui);VerifyAddon.injectionTargets(mixin);
        method(guiMap,"getMapProcessor","()",false);
        field(guiMap,"cameraX","D",false);field(guiMap,"cameraZ","D",false);field(guiMap,"shouldResetCameraPos","Z",false);
        method("xaero/map/MapProcessor","getMapWorld","()",false);method("xaero/map/MapProcessor","checkForWorldUpdate","()",false);
        method("xaero/map/world/MapWorld","setCustomDimensionId","("+key+")",false);method("xaero/map/world/MapWorld","getCustomDimensionId","()",false);
        // Doctor Who Mod: the client console's synced TARDIS state.
        method("net/drgmes/dwm/utils/helpers/TardisHelper","isTardisDimension","("+key+")",true);
        field("net/drgmes/dwm/blocks/tardis/consoleunits/BaseTardisConsoleUnitBlockEntity","tardisStateManager",null,false);
        String state="net/drgmes/dwm/common/tardis/TardisStateManager";
        method(state,"getSystem","(Ljava/lang/Class;)",false);method(state,"getCurrentExteriorDimension","()",false);method(state,"getCurrentExteriorPosition","()",false);
        method("net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization","isMaterialized","()",false);
        // Minecraft: the client world, its loaded chunks and block entities, and positions.
        method(mc+"class_310","method_1551","()",true);field(mc+"class_310","field_1687",null,false);field(mc+"class_310","field_1724",null,false);
        method(mc+"class_1937","method_27983","()",false);method(mc+"class_638","method_2935","()",false);
        method(mc+"class_631","method_2857","(IIL"+mc+"class_2806;Z)",false);field(mc+"class_2806","field_12803",null,true);
        method(mc+"class_2818","method_12214","()",false);method(mc+"class_1297","method_23317","()",false);method(mc+"class_1297","method_23321","()",false);
        method(mc+"class_2382","method_10263","()",false);method(mc+"class_2382","method_10260","()",false);
        System.out.println("PASS: Xaero map hook binds to GuiMap's constructor; every Xaero, Doctor Who Mod and Minecraft member used by the TARDIS map helper exists");
    }
}
