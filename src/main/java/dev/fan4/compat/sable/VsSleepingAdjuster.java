package dev.fan4.compat.sable;

import com.bawnorton.mixinsquared.adjuster.tools.AdjustableAnnotationNode;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import dev.fan4.compat.shared.GameLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.MethodNode;

/**
 * Valkyrien Skies' bed fix wraps two {@code setPos} calls in LivingEntity ({@code setPosToBed} and the stand-up after sleeping), and Sable replaces {@code setPosToBed}
 * outright (@Overwrite) and redirects the same call in the stand-up. Mixin refuses to inject into a method another mixin has replaced unless its own priority is higher,
 * and refuses a wrap beside a redirect of the same call, so with both mods the game does not start. Sable's versions already place the player correctly on its
 * sub-levels, so the two Valkyrien Skies wraps are pointed at a method that does not exist and made optional: they are skipped without an error. The bed fix's third
 * hook (holding a sleeping mob still) is untouched. The annotation is changed in place and returned as it came: MixinSquared renames the handler of a node that
 * comes back as a different one, which Mixin's conform step cannot follow (see {@link IpCollisionAdjuster}).
 */
public final class VsSleepingAdjuster implements MixinAnnotationAdjuster {
    static final String VS_MIXIN="org.valkyrienskies.mod.mixin.feature.bed_fix.MixinLivingEntitySleep";
    static final Set<String> HANDLERS=Set.of("vs$setPosToBedOnShip","vs$worldConvertWakeStandup");
    static final String WRAP="Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;",ABSENT="fan4$disabledBecauseSableDoesThis";

    @Override
    public AdjustableAnnotationNode adjust(List<String> targetClassNames,String mixinClassName,MethodNode handler,AdjustableAnnotationNode annotation){
        if(!VS_MIXIN.equals(mixinClassName)||!HANDLERS.contains(handler.name)||!WRAP.equals(annotation.desc))return annotation;
        GameLog.info("Sable sleeping hook: skipping Valkyrien Skies' "+handler.name+" (Sable replaces the method it wraps)");
        annotation.set("method",new ArrayList<>(List.of(ABSENT)));
        annotation.set("require",0);
        return annotation;
    }
}
