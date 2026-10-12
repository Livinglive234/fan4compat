import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Real Xaero World Map, Doctor Who Mod (Fabric) and intermediary-named Minecraft jars: the generated hook binds to
 * GuiMap's constructor, and every member TardisMapCompat reaches reflectively exists.
 * Arguments: world map jar, minimap jar, DWM jar, Minecraft jar, generated classes directory.
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
        jars=Arrays.copyOf(args,4);
        String mc="net/minecraft/",guiMap="xaero/map/gui/GuiMap",key="Lnet/minecraft/class_5321;";
        // Every generated hook must bind to the real classes (constructors, methods, redirected call sites).
        ClassNode gui=find(guiMap);
        int hooks=0,tardis=0;
        try(var files=Files.list(Path.of(args[4],"dev/fan4/compat/mixin/xaero/client"))) {
            for(Path file:files.filter(f->f.toString().endsWith(".class")).toList()) {
                ClassNode mixin=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(mixin,0);
                @SuppressWarnings("unchecked") List<String> targets=(List<String>)mixin.invisibleAnnotations.stream().filter(n->n.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow().values.get(1);
                String target=targets.get(0).replace('.','/');
                VerifyAddon.classes.put(target,find(target));VerifyAddon.injectionTargets(mixin);hooks++;if(mixin.name.contains("/Tardis"))tardis++;
            }
        }
        if(tardis!=12||hooks!=13)throw new AssertionError("Expected twelve TARDIS hooks and thirteen Xaero hooks in all, found "+tardis+" and "+hooks);
        method(guiMap,"getMapProcessor","()",false);
        field(guiMap,"cameraX","D",false);field(guiMap,"cameraZ","D",false);field(guiMap,"shouldResetCameraPos","Z",false);
        method("xaero/map/MapProcessor","getMapWorld","()",false);method("xaero/map/MapProcessor","checkForWorldUpdate","()",false);
        method("xaero/map/world/MapWorld","setCustomDimensionId","("+key+")",false);method("xaero/map/world/MapWorld","getCustomDimensionId","()",false);
        // Minimap and World Map members reached by reflection.
        String session="xaero/hud/minimap/module/MinimapSession",worldState="xaero/hud/minimap/world/state/MinimapWorldState",path="Lxaero/hud/path/XaeroPath;";
        method(session,"getWorldState","()",false);method(session,"getDimensionHelper","()",false);method(session,"getWorldStateUpdater","()",false);method(session,"getWorldManager","()",false);
        method(worldState,"getCustomWorldPath","()",false);method(worldState,"setCustomWorldPath","("+path+")",false);method(worldState,"getAutoRootContainerPath","()",false);
        method("xaero/hud/minimap/world/MinimapDimensionHelper","getDimensionDirectoryName","("+key+")",false);
        method("xaero/hud/path/XaeroPath","resolve","(Ljava/lang/String;)",false);
        field("xaero/hud/minimap/world/state/MinimapWorldStateUpdater","modMain",null,false);method("xaero/hud/minimap/world/state/MinimapWorldStateUpdater","getPotentialWorldNode","("+key+"Z)",false);
        method("xaero/common/HudMod","getSupportMods","()",false);method("xaero/common/mods/SupportMods","worldmap","()",false);
        method("xaero/hud/minimap/world/MinimapWorldManager","getWorld","("+path+")",false);method("xaero/hud/minimap/world/MinimapWorldManager","addWorld","("+path+")",false);
        method("xaero/map/WorldMapSession","getCurrentSession","()",true);method("xaero/map/WorldMapSession","getMapProcessor","()",false);
        method(mc+"class_2382","method_10264","()",false);
        method("xaero/hud/minimap/world/MinimapWorldManager","getAutoRootContainer","()",false);method("xaero/hud/minimap/world/container/MinimapWorldRootContainer","getDimensionType","("+key+")",false);
        method(mc+"class_2874","comp_643","()",false);method(mc+"class_5321","method_29177","()",false);
        // Doctor Who Mod: the client console's synced TARDIS state.
        method("net/drgmes/dwm/utils/helpers/TardisHelper","isTardisDimension","("+key+")",true);
        field("net/drgmes/dwm/blocks/tardis/consoleunits/BaseTardisConsoleUnitBlockEntity","tardisStateManager",null,false);
        String state="net/drgmes/dwm/common/tardis/TardisStateManager";
        method(state,"getSystem","(Ljava/lang/Class;)",false);method(state,"getCurrentExteriorDimension","()",false);method(state,"getCurrentExteriorPosition","()",false);
        method("net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization","isMaterialized","()",false);
        // Flight: the destination, and the synced progress the coordinate display follows while travelling.
        String flight="net/drgmes/dwm/common/tardis/systems/TardisSystemFlight";
        method(state,"getDestinationExteriorPosition","()",false);
        for(String name:new String[]{"inProgress","isInFlight","getProgressPercent"})method(flight,name,"()",false);
        field(flight,"step",null,false);
        // Compass: the exterior's facing, the main interior door, and the door block's facing property.
        method(state,"getCurrentExteriorFacing","()",false);method(state,"getInteriorDoorsTiles","()",false);
        method(mc+"class_2350","method_10144","()",false);method(mc+"class_1937","method_8320","(L"+mc+"class_2338;)",false);
        method(mc+"class_2688","method_28501","()",false);method(mc+"class_2688","method_11654","(L"+mc+"class_2769;)",false);method(mc+"class_2769","method_11899","()",false);
        // Minecraft: the client world, its loaded chunks and block entities, and positions.
        method(mc+"class_310","method_1551","()",true);field(mc+"class_310","field_1687",null,false);field(mc+"class_310","field_1724",null,false);
        method(mc+"class_1937","method_27983","()",false);method(mc+"class_638","method_2935","()",false);
        method(mc+"class_631","method_2857","(IIL"+mc+"class_2806;Z)",false);field(mc+"class_2806","field_12803",null,true);
        method(mc+"class_2818","method_12214","()",false);method(mc+"class_1297","method_23317","()",false);method(mc+"class_1297","method_23321","()",false);
        method(mc+"class_2382","method_10263","()",false);method(mc+"class_2382","method_10260","()",false);
        System.out.println("PASS: all twelve Xaero TARDIS hooks bind to the real Minimap and World Map classes; every Xaero, Doctor Who Mod and Minecraft member used by the helpers exists");
    }
}
