package dev.fan4.compat;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import dev.fan4.compat.shared.CompatCalls;
import dev.fan4.compat.shared.CompatibilityRules;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CompatPlugin implements IMixinConfigPlugin, MixinCanceller {
    private static boolean registered;
    private static final boolean HAS_SABLE = CompatCalls.present("dev.ryanhcode.sable.companion.impl.DefaultSableCompanion");
    private static Map<String,String> installed;
    public void onLoad(String mixinPackage) {
        Map<String,String> mods=installedMods();
        for (var entry : CompatibilityRules.VERSIONS.entrySet()) {
            if (mods.containsKey(entry.getKey()) && !CompatibilityRules.supported(mods,entry.getKey()))
                dev.fan4.compat.shared.GameLog.warn("Skipping compatibility hooks for " + entry.getKey() + ": supported " + CompatibilityRules.requirement(entry.getKey()) + ", installed " + mods.get(entry.getKey()));
        }
        MixinSquaredBootstrap.init();
        if (!registered) {
            MixinCancellerRegistrar.register(this);
            if (CompatibilityRules.sablePortals(mods) && dev.fan4.compat.shared.Features.enabled("sablePortalCollision")) {
                com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar.register(new dev.fan4.compat.sable.IpCollisionAdjuster());
                dev.fan4.compat.shared.GameLog.info("Sable collision hook registered: Sable's Entity.collide redirect will run inside Immersive Portals' collision handler");
            }
            if (CompatibilityRules.sableShips(mods) && dev.fan4.compat.shared.Features.enabled("sableSleeping")) {
                com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar.register(new dev.fan4.compat.sable.VsSleepingAdjuster());
                dev.fan4.compat.shared.GameLog.info("Sable sleeping hook registered: Valkyrien Skies' two bed wraps are skipped");
            }
            registered = true;
        }
    }
    /** Immersive Portals' @Overwrite of Player.canPlayerFitWithinBlocksAndEntitiesWhen; replaced by sable.IpPlayerCollisionMixin, which Sable can inject into (see SablePlayerCollisionGenerator). */
    static final String IP_PLAYER_COLLISION="qouteall.imm_ptl.core.mixin.common.collision.MixinPlayer_Collision";
    public boolean shouldCancel(List<String> targets, String mixinClassName) {
        Map<String,String> mods=installedMods();
        if (mixinClassName.equals(IP_PLAYER_COLLISION) && CompatibilityRules.sablePortals(mods) && dev.fan4.compat.shared.Features.enabled("sablePortalCollision")) return true;
        return shouldCancelFor(CompatibilityRules.portalsOnVs(mods), HAS_SABLE, CompatibilityRules.sodium08(mods), CompatibilityRules.supported(mods,"valkyrienskies"), mixinClassName);
    }
    public static boolean shouldCancelFor(boolean portals, boolean sable, String name) { return shouldCancelFor(portals, sable, false, name); }
    /** IP's redirect of Sodium 0.6's Viewport.isBoxVisible has no call to redirect in Sodium 0.8 and its OcclusionCuller handler names Sodium 0.6's Visitor type (and IP's injectors are required), so they are replaced by sodium.client.SodiumViewportCullingMixin and SodiumPortalOcclusionMixin. */
    public static boolean shouldCancelFor(boolean portals, boolean sable, boolean sodium08, String name) { return shouldCancelFor(portals, sable, sodium08, false, name); }
    /**
     * Valkyrien Skies' own hooks for drawing ships through Sodium were written for Sodium 0.6. Two of its mixin classes mix hooks that cannot apply to Sodium 0.8 (the
     * render-list creation and the layer drawing) with ones that can, and the duck interface the others read through, so both classes are replaced as a whole by
     * sodium.client.SodiumShipRenderingMixin. They are cancelled whatever the feature switch says: left in, the first of them stops the world from loading.
     */
    public static boolean shouldCancelFor(boolean portals, boolean sable, boolean sodium08, boolean vs, String name) {
        if (sodium08 && vs && (name.equals("org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinRenderSectionManager")
            || name.equals("org.valkyrienskies.mod.fabric.mixin.compat.sodium.MixinRenderSectionManager"))) return true;
        if (sodium08 && (name.equals("qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumViewport") || name.equals("qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumOcclusionCuller"))) return true;
        if (portals && (name.equals("org.valkyrienskies.mod.mixin.feature.fix_frustum_dead_loop.MixinFrustum")
            || name.equals("org.valkyrienskies.mod.mixin.server.world.MixinChunkMap$TrackedEntity"))) return true;
        return false;
    }
    private static synchronized Map<String,String> installedMods() {
        if (installed == null) {
            Map<String,String> mods=new HashMap<>();
            for (var mod : FabricLoader.getInstance().getAllMods())
                mods.put(mod.getMetadata().getId(),mod.getMetadata().getVersion().getFriendlyString());
            installed = Map.copyOf(mods);
        }
        return installed;
    }
    public boolean shouldApplyMixin(String target, String name) {
        return CompatibilityRules.applies(installedMods(),HAS_SABLE,name);
    }
    public String getRefMapperConfig() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
