package dev.fan4.compat;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import dev.fan4.compat.shared.CompatibilityRules;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CompatPlugin implements IMixinConfigPlugin, MixinCanceller {
    private static boolean registered;
    private static boolean hasSable() {
        return CompatPlugin.class.getClassLoader().getResource("dev/ryanhcode/sable/companion/impl/DefaultSableCompanion.class") != null;
    }
    public void onLoad(String mixinPackage) {
        Map<String,String> mods=installedMods();
        for (var entry : CompatibilityRules.VERSIONS.entrySet()) {
            if (mods.containsKey(entry.getKey()) && !CompatibilityRules.supported(mods,entry.getKey()))
                System.getLogger("Fan4Compat").log(System.Logger.Level.WARNING,
                    "Skipping compatibility hooks for " + entry.getKey() + ": supported " + entry.getValue() + ", installed " + mods.get(entry.getKey()));
        }
        MixinSquaredBootstrap.init();
        if (!registered) { MixinCancellerRegistrar.register(this); registered = true; }
    }
    public boolean shouldCancel(List<String> targets, String mixinClassName) {
        Map<String,String> mods=installedMods();
        return shouldCancelFor(CompatibilityRules.supported(mods,"valkyrienskies") && CompatibilityRules.supported(mods,"immersive_portals"), hasSable(), mixinClassName);
    }
    public static boolean shouldCancelFor(boolean portals, boolean sable, String name) {
        if (portals && (name.equals("org.valkyrienskies.mod.mixin.feature.fix_frustum_dead_loop.MixinFrustum")
            || name.equals("org.valkyrienskies.mod.mixin.server.world.MixinChunkMap$TrackedEntity"))) return true;
        return false;
    }
    private static Map<String,String> installedMods() {
        Map<String,String> mods=new HashMap<>();
        for (var mod : FabricLoader.getInstance().getAllMods())
            mods.put(mod.getMetadata().getId(),mod.getMetadata().getVersion().getFriendlyString());
        return mods;
    }
    public boolean shouldApplyMixin(String target, String name) {
        return CompatibilityRules.applies(installedMods(),hasSable(),name);
    }
    public String getRefMapperConfig() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
