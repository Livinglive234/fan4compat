import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class VerifyAddon {
    static final Map<String,ClassNode> classes=new HashMap<>();
    static void readJar(String path)throws Exception {
        try(ZipFile z=new ZipFile(path)) {var entries=z.entries();while(entries.hasMoreElements()) {var e=entries.nextElement();if(!e.getName().endsWith(".class"))continue;ClassNode c=new ClassNode();new ClassReader(z.getInputStream(e)).accept(c,0);classes.put(c.name,c);}}
    }
    static boolean exists(String owner,String name,String desc,Set<String> seen) {
        if(!seen.add(owner))return false;ClassNode c=classes.get(owner);if(c==null)return false;
        if(c.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(desc)))return true;
        if(c.superName!=null&&exists(c.superName,name,desc,seen))return true;
        return c.interfaces.stream().anyMatch(i->exists(i,name,desc,seen));
    }
    static Object value(AnnotationNode a,String key) {
        if(a.values==null)return null;
        for(int i=0;i<a.values.size();i+=2)if(a.values.get(i).equals(key))return a.values.get(i+1);
        return null;
    }
    static void injectionTargets(ClassNode c) {
        if(c.invisibleAnnotations==null)return;
        AnnotationNode mixin=c.invisibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElse(null);
        if(mixin==null)return;
        Object raw=value(mixin,"targets");if(!(raw instanceof List<?> targets))return;
        String owner=targets.get(0).toString().replace('.','/');ClassNode target=classes.get(owner);
        if(target==null)throw new AssertionError("Unverified mixin target "+owner);
        for(MethodNode m:c.methods) {
            if(m.visibleAnnotations==null)continue;
            for(AnnotationNode a:m.visibleAnnotations) {
                if(!a.desc.endsWith("/Inject;")&&!a.desc.endsWith("/Redirect;")&&!a.desc.endsWith("/ModifyExpressionValue;")&&!a.desc.endsWith("/ModifyArg;")&&!a.desc.endsWith("/WrapMethod;")&&!a.desc.endsWith("/ModifyVariable;")&&!a.desc.endsWith("/WrapOperation;"))continue;
                Object selectors=value(a,"method");if(!(selectors instanceof List<?> methods))continue;
                int invocationCount=0;
                for(Object selector:methods) {
                    String s=selector.toString();int bracket=s.indexOf('(');String name=s.substring(0,bracket),desc=s.substring(bracket);
                    MethodNode tm=target.methods.stream().filter(n->n.name.equals(name)&&n.desc.equals(desc)).findFirst().orElseThrow(()->new AssertionError("Missing injection selector "+owner+"."+s));
                    if(a.desc.endsWith("/WrapMethod;"))continue;
                    Object points=value(a,"at");
                    List<?> atPoints=points instanceof List<?> list ? list : List.of(points);
                    for(Object point:atPoints) {
                        AnnotationNode at=(AnnotationNode)point;
                        if(!"INVOKE".equals(value(at,"value"))) continue;
                        String invocation=(String)value(at,"target");int semi=invocation.indexOf(';'),callBracket=invocation.indexOf('(',semi);
                        String callOwner=invocation.substring(1,semi),callName=invocation.substring(semi+1,callBracket),callDesc=invocation.substring(callBracket);
                        int count=0;for(var i:tm.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals(callOwner)&&call.name.equals(callName)&&call.desc.equals(callDesc))count++;
                        if(count==0) throw new AssertionError("Missing invocation at "+owner+"."+s+": "+invocation);
                        invocationCount+=count;
                    }
                }
                if(invocationCount>0 && invocationCount<(Integer)value(a,"require")) throw new AssertionError("Injection count="+invocationCount+", required="+value(a,"require")+" at "+owner);

            }
        }
    }
    static boolean fieldExists(String owner,String name) {
        ClassNode c=classes.get(owner);return c!=null&&(c.fields.stream().anyMatch(f->f.name.equals(name))||fieldExists(c.superName,name));
    }
    static void tardisContracts() {
        String state="net/drgmes/dwm/common/tardis/TardisStateManager";
        if(!classes.containsKey(state))return;
        String[][] fields={
            {"net/drgmes/dwm/blocks/tardis/exteriors/BaseTardisExteriorBlock","OPEN","FACING","exteriorType"},
            {"net/drgmes/dwm/DWM$TEXTS","SONIC_DEVICE_TARDIS_RELOCATED"},
            {"net/drgmes/dwm/compat/immersiveportals/ImmersivePortals$TardisPortalsState","tardis","portalFromTardis","portalToTardis"},
            {"net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization","tardis"},
            {"net/drgmes/dwm/common/tardis/systems/TardisSystemFlight","tardis"},
            {"net/drgmes/dwm/common/tardis/exteriors/TardisExteriorEntry","entranceWidth","entranceHeight"}};
        for(String[] entry:fields)for(int i=1;i<entry.length;i++)if(!fieldExists(entry[0],entry[i]))throw new AssertionError("Missing reflected field "+entry[0]+"."+entry[i]);
        String[][] calls={
            {"net/drgmes/dwm/utils/helpers/WorldHelper","checkBlockIsSolid","(Lnet/minecraft/class_2680;)Z"},
            {"org/valkyrienskies/mod/common/VSGameUtilsKt","getShipsIntersecting","(Lnet/minecraft/class_1937;Lnet/minecraft/class_238;)Ljava/lang/Iterable;"},
            {state,"getWorld","()Lnet/minecraft/class_3218;"},
            {state,"getExteriorWorld","()Lnet/minecraft/class_3218;"},
            {state,"getDestinationExteriorWorld","()Lnet/minecraft/class_3218;"},
            {state,"getPreviousExteriorDimension","()Lnet/minecraft/class_5321;"},
            {state,"markConsoleTilesUpdated","()V"},
            {state,"getPortalsState","()Lnet/drgmes/dwm/compat/immersiveportals/ImmersivePortals$TardisPortalsState;"},
            {state,"getExteriorType","()Lnet/drgmes/dwm/common/tardis/exteriors/TardisExteriorEntry;"},
            {state,"getSystem","(Ljava/lang/Class;)Lnet/drgmes/dwm/common/tardis/systems/TardisBaseSystem;"},
            {state,"setDestinationPosition","(Lnet/minecraft/class_2338;)V"},
            {state,"setDestinationFacing","(Lnet/minecraft/class_2350;)V"},
            {"net/drgmes/dwm/common/tardis/systems/TardisSystemMaterialization","setVerticalScanning","(Lnet/drgmes/dwm/enums/TardisVerticalScanning;)V"},
            {"org/valkyrienskies/mod/common/VSGameUtilsKt","getShipManagingPos","(Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;)Lorg/valkyrienskies/core/api/ships/Ship;"},
            {"org/valkyrienskies/mod/common/world/RaycastUtilsKt","clipIncludeShips","(Lnet/minecraft/class_1937;Lnet/minecraft/class_3959;ZLjava/lang/Long;Z)Lnet/minecraft/class_3965;"},
            {"qouteall/imm_ptl/core/portal/Portal","disableDefaultAnimation","()V"},
            {"qouteall/imm_ptl/core/portal/Portal","isPortalValid","()Z"},
            {"qouteall/imm_ptl/core/portal/Portal","canTeleportEntity","(Lnet/minecraft/class_1297;)Z"},
            {"qouteall/imm_ptl/core/portal/Portal","getOriginPos","()Lnet/minecraft/class_243;"},
            {"qouteall/imm_ptl/core/portal/Portal","setOriginPos","(Lnet/minecraft/class_243;)V"},
            {"qouteall/imm_ptl/core/portal/Portal","setDestination","(Lnet/minecraft/class_243;)V"},
            {"qouteall/imm_ptl/core/portal/Portal","setOrientationAndSize","(Lnet/minecraft/class_243;Lnet/minecraft/class_243;DD)V"},
            {"qouteall/imm_ptl/core/portal/Portal","setScaling","(D)V"},
            {"qouteall/imm_ptl/core/portal/Portal","reloadAndSyncToClientNextTick","()V"},
            {"qouteall/imm_ptl/core/portal/PortalManipulation","adjustRotationToConnect","(Lqouteall/imm_ptl/core/portal/Portal;Lqouteall/imm_ptl/core/portal/Portal;)V"}};
        for(String[] call:calls)if(!exists(call[0],call[1],call[2],new HashSet<>()))throw new AssertionError("Missing reflected call "+Arrays.toString(call));
        for(String slot:List.of("Previous","Current","Destination"))for(String[] property:new String[][]{{"Position","class_2338"},{"Facing","class_2350"}})
            if(!exists(state,"get"+slot+"Exterior"+property[0],"()Lnet/minecraft/"+property[1]+";",new HashSet<>()))throw new AssertionError("Missing exterior getter "+slot+property[0]);
        System.out.println("PASS: exact DWM, VS and IP reflection contracts verified");
    }
    static void transitContracts() {
        String utils="org/valkyrienskies/mod/common/VSGameUtilsKt",drag="org/valkyrienskies/mod/common/util/EntityDraggingInformation";
        if(!classes.containsKey(utils))return;
        String[][] calls={
            {"org/valkyrienskies/mod/mixinducks/client/MinecraftDuck","vs$getOriginalCrosshairTarget","()Lnet/minecraft/class_239;"},
            {"org/valkyrienskies/mod/mixinducks/client/MinecraftDuck","vs$setOriginalCrosshairTarget","(Lnet/minecraft/class_239;)V"},
            {"qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking","immediatelyUpdateForPlayer","(Lnet/minecraft/class_3222;)V"},
            {"qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking","getPlayerInfo","(Lnet/minecraft/class_3222;)Lqouteall/imm_ptl/core/chunk_loading/PlayerChunkLoading;"},
            {"qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking","getWatchRecordForChunk","(Lnet/minecraft/class_5321;II)Lit/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap;"},
            {"qouteall/imm_ptl/core/chunk_loading/PlayerChunkLoading","markPendingLoading","(Lqouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTracking$PlayerWatchRecord;)V"},
            {utils,"getShipObjectWorld","(Lnet/minecraft/class_638;)Lorg/valkyrienskies/core/internal/world/VsiClientShipWorld;"},
            {utils,"getShipObjectWorld","(Lnet/minecraft/class_3218;)Lorg/valkyrienskies/core/internal/world/VsiServerShipWorld;"},
            {utils,"getDimensionId","(Lnet/minecraft/class_1937;)Ljava/lang/String;"},
            {"org/valkyrienskies/core/internal/world/VsiClientShipWorld","isSyncedWithServer","()Z"},
            {"org/valkyrienskies/core/internal/world/VsiClientShipWorld","getLoadedShips","()Lorg/valkyrienskies/core/internal/ships/VsiQueryableShipData;"},
            {"org/valkyrienskies/core/internal/world/VsiShipWorld","getAllShips","()Lorg/valkyrienskies/core/internal/ships/VsiQueryableShipData;"},
            {"org/valkyrienskies/core/internal/ships/VsiQueryableShipData","getById","(J)Lorg/valkyrienskies/core/api/ships/Ship;"},
            {"org/valkyrienskies/core/api/ships/Ship","getChunkClaimDimension","()Ljava/lang/String;"},
            {"org/valkyrienskies/mod/common/util/MinecraftPlayer","getPlayer","()Lnet/minecraft/class_1657;"},
            {"org/valkyrienskies/mod/common/networking/PacketPlayerShipMotion","getShipID","()J"},
            {"org/valkyrienskies/mod/common/util/IEntityDraggingInformationProvider","getDraggingInformation","()Lorg/valkyrienskies/mod/common/util/EntityDraggingInformation;"},
            {"org/valkyrienskies/mod/mixinducks/feature/tickets/PlayerKnownShipsDuck","vs_isKnownShip","(J)Z"},
            {"org/valkyrienskies/mod/mixinducks/feature/tickets/PlayerKnownShipsDuck","vs_addKnownShip","(J)V"},
            {"org/valkyrienskies/mod/mixinducks/world/entity/PlayerDuck","vs_setQueuedPositionUpdate","(Lnet/minecraft/class_243;)V"},
            {"org/valkyrienskies/mod/mixinducks/world/entity/PlayerDuck","vs_setHandledMovePacket","(Z)V"}};
        for(String[] call:calls)if(!exists(call[0],call[1],call[2],new HashSet<>()))throw new AssertionError("Missing transit call "+Arrays.toString(call));
        for(String[] setters:new String[][]{
            {"Ljava/lang/Long;","LastShipStoodOn","LastShipStoodOnServerWriteOnly"},
            {"Lorg/joml/Vector3dc;","AddedMovementLastTick","LerpPositionOnShip","RelativeVelocityOnShip","RelativePositionOnShip","PreviousRelativeVelocityOnShip","CachedLastPosition","ServerRelativePlayerPosition"},
            {"Ljava/lang/Double;","LerpYawOnShip","LerpHeadYawOnShip","LerpPitchOnShip","RelativeYawOnShip","RelativeHeadYawOnShip","RelativePitchOnShip","DraggedArmorStandRelYaw","ServerRelativePlayerYaw"},
            {"Z","RestoreCachedLastPosition","ShouldImpulseMovement"},{"D","AddedYawRotLastTick"},{"I","LerpSteps","HeadLerpSteps"}})
            for(int n=1;n<setters.length;n++)if(!exists(drag,"set"+setters[n],"("+setters[0]+")V",new HashSet<>()))throw new AssertionError("Missing drag reset "+setters[n]);
        System.out.println("PASS: exact ship transfer and motion packet reflection contracts verified");
    }
    public static void main(String[] args)throws Exception {
        for(int i=1;i<args.length;i++)readJar(args[i]);
        tardisContracts();transitContracts();
        int count=0;
        for(Path p:Files.walk(Path.of(args[0])).filter(f->f.toString().endsWith(".class")).toList()) {
            ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(c,0);
            if(args.length>1)injectionTargets(c);
            Set<String> signatures=new HashSet<>();
            for(MethodNode m:c.methods) {
                if(!signatures.add(m.name+m.desc))throw new AssertionError("Duplicate JVM method in "+c.name+": "+m.name+m.desc);
                if((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE))==0)new Analyzer<>(new BasicVerifier()).analyze(c.name,m);
                for(var i:m.instructions)if(i instanceof MethodInsnNode call&&(call.owner.startsWith("org/valkyrienskies/")||call.owner.startsWith("qouteall/imm_ptl/")||call.owner.startsWith("com/seibel/distanthorizons/")||call.owner.startsWith("net/drgmes/dwm/"))&&classes.containsKey(call.owner)&&!exists(call.owner,call.name,call.desc,new HashSet<>()))throw new AssertionError("Unresolved compatibility method "+call.owner+"."+call.name+call.desc);
            }
            count++;
        }
        System.out.println("PASS: bytecode stack/type validation for "+count+" addon classes");
        if(args.length>1) {
            ClassNode core=classes.get("org/valkyrienskies/core/impl/shadow/Et");
            if(core==null)throw new AssertionError("Wrong VS implementation");
            for(String name:List.of("preTick","i","destroyWorld","addDimension","removeDimension","updateDimension"))if(core.methods.stream().noneMatch(m->m.name.equals(name)))throw new AssertionError("Missing dimension target "+name);
            for(String[] marker:new String[][]{{"preTick","PRE_TICK"},{"i","POST_TICK_START"}}) {
                MethodNode m=core.methods.stream().filter(n->n.name.equals(marker[0])&&n.desc.equals("()V")).findFirst().orElseThrow();boolean found=false;
                for(var i:m.instructions)if(i instanceof FieldInsnNode field&&field.name.equals(marker[1]))found=true;
                if(!found)throw new AssertionError("Wrong lifecycle stage "+marker[0]);
            }
            System.out.println("PASS: exact VS implementation and physics lifecycle targets verified");
        }
    }
}
