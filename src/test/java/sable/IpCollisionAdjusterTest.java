import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import com.bawnorton.mixinsquared.adjuster.tools.AdjustableRedirectNode;
import dev.fan4.compat.sable.IpCollisionAdjuster;
import dev.fan4.compat.shared.CompatibilityRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

public final class IpCollisionAdjusterTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}

    static final String IP="qouteall.imm_ptl.core.mixin.common.collision.MixinEntity",SABLE="dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision.EntityMixin";
    static AnnotationNode redirect(){
        AnnotationNode node=new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/Redirect;");
        AnnotationNode at=new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        at.values=new ArrayList<>(List.of("value","INVOKE","target","Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"));
        node.values=new ArrayList<>(List.of("method",new ArrayList<>(List.of("Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V")),"at",at));
        return node;
    }
    static MethodNode handler(String name){
        MethodNode method=new MethodNode(Opcodes.ACC_PRIVATE,name,"(Lnet/minecraft/class_1297;Lnet/minecraft/class_243;)Lnet/minecraft/class_243;",null,null);
        method.visibleAnnotations=new ArrayList<>(List.of(redirect()));
        return method;
    }

    /** What MixinSquared's ExtensionAnnotationAdjust does with one handler: it keeps hold of the visible-annotation list, walks it, asks the adjuster, then clears that same list and adds the answers back. */
    static boolean walk(IpCollisionAdjuster adjuster,String mixin,MethodNode method){
        List<AnnotationNode> held=method.visibleAnnotations;
        if(held==null)return false;
        List<AnnotationNode> adjusted=new ArrayList<>();
        boolean modified=false;
        for(AnnotationNode node:held) {
            AdjustableAnnotationNode in=AdjustableAnnotationNode.fromNode(node);
            AdjustableAnnotationNode answer=adjuster.adjust(List.of("net.minecraft.class_1297"),mixin,method,in);
            if(answer!=null)adjusted.add(answer);
            if(answer!=in)modified=true;
        }
        held.clear();
        held.addAll(adjusted);
        return modified;
    }
    static String methodOf(MethodNode method){
        AnnotationNode redirect=method.visibleAnnotations.stream().filter(a->a.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")).findFirst().orElseThrow();
        return redirect.values.get(1).toString();
    }
    public static void main(String[] args){
        IpCollisionAdjuster adjuster=new IpCollisionAdjuster();
        // Sable's collision redirect is moved into Immersive Portals' handler, walked the way MixinSquared walks it.
        MethodNode sable=handler("sable$collideRedirect");
        sable.visibleAnnotations.add(new AnnotationNode("Lorg/spongepowered/asm/mixin/Unique;"));
        check(!walk(adjuster,SABLE,sable),"MixinSquared must not see a different node, or it renames the handler and Mixin's conform step fails");
        check(sable.name.equals("sable$collideRedirect"),"the handler keeps its name");
        check(methodOf(sable).equals("[@MixinSquared:Handler]"),"the redirect now targets a handler: "+methodOf(sable));
        AnnotationNode marker=sable.visibleAnnotations.stream().filter(a->"Lcom/bawnorton/mixinsquared/TargetHandler;".equals(a.desc)).findFirst().orElseThrow(()->new AssertionError("no TargetHandler marker survived the walk"));
        check(marker.values.equals(List.of("mixin","qouteall.imm_ptl.core.mixin.common.collision.MixinEntity","name","redirectHandleCollisions")),"the marker names Immersive Portals' handler: "+marker.values);
        check(sable.visibleAnnotations.stream().anyMatch(a->a.desc.equals("Lorg/spongepowered/asm/mixin/Unique;")),"the other annotations are kept");
        check(sable.visibleAnnotations.stream().filter(a->"Lcom/bawnorton/mixinsquared/TargetHandler;".equals(a.desc)).count()==1,"one marker only");
        walk(adjuster,SABLE,sable);
        check(sable.visibleAnnotations.stream().filter(a->"Lcom/bawnorton/mixinsquared/TargetHandler;".equals(a.desc)).count()==1&&methodOf(sable).equals("[@MixinSquared:Handler]"),"walked twice, still one marker");
        // Everything else is left alone.
        MethodNode other=handler("someOtherHandler");
        walk(adjuster,SABLE,other);
        check(other.visibleAnnotations.size()==1&&methodOf(other).contains("Entity;move("),"another Sable handler is not touched");
        MethodNode elsewhere=handler("sable$collideRedirect");
        walk(adjuster,"some.other.Mixin",elsewhere);
        check(elsewhere.visibleAnnotations.size()==1&&methodOf(elsewhere).contains("Entity;move("),"another mod's handler of the same name is not touched");
        MethodNode ipOwn=handler("redirectHandleCollisions");
        walk(adjuster,IP,ipOwn);
        check(ipOwn.visibleAnnotations.size()==1&&methodOf(ipOwn).contains("Entity;move("),"Immersive Portals' own redirect is left where it is");
        // Only with the Sable build it was written for and a supported Immersive Portals.
        Map<String,String> both=Map.of("sable","2.0.6","immersive_portals","6.0.6");
        check(CompatibilityRules.sablePortals(both),"Sable 2.0.6 with Immersive Portals 6.0.6");
        check(!CompatibilityRules.sablePortals(Map.of("sable","2.0.7","immersive_portals","6.0.6")),"another Sable build is skipped");
        check(!CompatibilityRules.sablePortals(Map.of("immersive_portals","6.0.6"))&&!CompatibilityRules.sablePortals(Map.of("sable","2.0.6")),"needs both mods");
        System.out.println("PASS: Sable / Immersive Portals collision (Sable's redirect is moved into Immersive Portals' handler, marker added once, other handlers and mods untouched, Sable 2.0.6 + Immersive Portals gate)");
    }
}
