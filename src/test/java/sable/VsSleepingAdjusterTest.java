import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import dev.fan4.compat.sable.VsSleepingAdjuster;
import dev.fan4.compat.shared.CompatibilityRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public final class VsSleepingAdjusterTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static final String VS="org.valkyrienskies.mod.mixin.feature.bed_fix.MixinLivingEntitySleep";

    static MethodNode handler(String name,String annotation,String method){
        MethodNode node=new MethodNode(Opcodes.ACC_PRIVATE,name,"()V",null,null);
        AnnotationNode a=new AnnotationNode(annotation);
        a.values=new ArrayList<>(List.of("method",new ArrayList<>(List.of(method))));
        node.visibleAnnotations=new ArrayList<>(List.of(a));
        return node;
    }
    /** MixinSquared's walk: the list is kept, walked, cleared and refilled with the answers; a different node coming back renames the handler. */
    static boolean walk(VsSleepingAdjuster adjuster,String mixin,MethodNode method){
        List<AnnotationNode> held=method.visibleAnnotations,adjusted=new ArrayList<>();
        boolean modified=false;
        for(AnnotationNode node:held) {
            AdjustableAnnotationNode in=AdjustableAnnotationNode.fromNode(node);
            AdjustableAnnotationNode answer=adjuster.adjust(List.of("net.minecraft.class_1309"),mixin,method,in);
            if(answer!=null)adjusted.add(answer);
            if(answer!=in)modified=true;
        }
        held.clear();held.addAll(adjusted);
        return modified;
    }
    public static void main(String[] args){
        VsSleepingAdjuster adjuster=new VsSleepingAdjuster();
        String wrap="Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
        for(String name:new String[]{"vs$setPosToBedOnShip","vs$worldConvertWakeStandup"}) {
            MethodNode method=handler(name,wrap,"setPosToBed");
            check(!walk(adjuster,VS,method),"the wrap comes back as the same node, so the handler is not renamed: "+name);
            AnnotationNode answer=method.visibleAnnotations.get(0);
            check(answer.values.get(1).equals(List.of("fan4$disabledBecauseSableDoesThis")),"retargeted to a method that does not exist: "+answer.values);
            check(answer.values.contains("require")&&answer.values.get(answer.values.indexOf("require")+1).equals(0),"and optional: "+answer.values);
            check(method.name.equals(name),"handler keeps its name");
        }
        MethodNode travel=handler("vs$holdSleepingMobStill","Lorg/spongepowered/asm/mixin/injection/Inject;","travel");
        walk(adjuster,VS,travel);
        check(travel.visibleAnnotations.get(0).values.get(1).equals(List.of("travel")),"the bed fix's travel hook is untouched");
        MethodNode other=handler("vs$setPosToBedOnShip",wrap,"setPosToBed");
        walk(adjuster,"some.other.Mixin",other);
        check(other.visibleAnnotations.get(0).values.get(1).equals(List.of("setPosToBed")),"another mod's handler of that name is untouched");
        check(CompatibilityRules.sableShips(Map.of("sable","2.0.6","valkyrienskies","2.4.12-td.9+66a13242ed")),"Sable 2.0.6 with Valkyrien Skies");
        check(!CompatibilityRules.sableShips(Map.of("sable","2.0.6"))&&!CompatibilityRules.sableShips(Map.of("sable","2.0.7","valkyrienskies","2.4.12-td.9+66a13242ed")),"needs both, at the supported builds");
        System.out.println("PASS: Sable / Valkyrien Skies sleeping (the two bed wraps are skipped in place without renaming, the travel hook and other mods are untouched, gate)");
    }
}
