package dev.fan4.compat;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class CompatPlugin implements IMixinConfigPlugin, MixinCanceller {
    private static boolean registered;
    private static boolean present(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    private static boolean hasSable() {
        return CompatPlugin.class.getClassLoader().getResource("dev/ryanhcode/sable/companion/impl/DefaultSableCompanion.class") != null;
    }
    public void onLoad(String mixinPackage) {
        var vs = FabricLoader.getInstance().getModContainer("valkyrienskies").orElseThrow();
        String version = vs.getMetadata().getVersion().getFriendlyString();
        if (!version.equals("2.4.12-td.9+66a13242ed")) {
            throw new IllegalStateException("Fan4Compat requires the ORIGINAL Valkyrien Skies 2.4.12-td.9+66a13242ed jar. Remove earlier patched VS jars. Found: " + version);
        }
        if (present("immersive_portals")) {
            String portals = FabricLoader.getInstance().getModContainer("immersive_portals").orElseThrow().getMetadata().getVersion().getFriendlyString();
            if (!portals.equals("6.0.6")) throw new IllegalStateException("Fan4Compat currently targets Immersive Portals Fabric 6.0.6; found " + portals);
        }
        MixinSquaredBootstrap.init();
        if (!registered) { MixinCancellerRegistrar.register(this); registered = true; }
    }
    public boolean shouldCancel(List<String> targets, String mixinClassName) {
        return shouldCancelFor(present("immersive_portals"), hasSable(), mixinClassName);
    }
    public static boolean shouldCancelFor(boolean portals, boolean sable, String name) {
        if (portals && (name.equals("org.valkyrienskies.mod.mixin.feature.fix_frustum_dead_loop.MixinFrustum")
            || name.equals("org.valkyrienskies.mod.mixin.server.world.MixinChunkMap$TrackedEntity"))) return true;
        return false;
    }
    public boolean shouldApplyMixin(String target, String name) {
        if ((name.endsWith("ShipAcousticRaycastMixin") || name.endsWith("ShipAcousticSnapshotMixin"))) return present("sound_physics_remastered") && FabricLoader.getInstance().getModContainer("sound_physics_remastered").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.21.1-1.5.1");
        if (name.contains(".mixin.accessories.")) return present("immersive_portals") && present("accessories") && FabricLoader.getInstance().getModContainer("accessories").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.1.0-beta.48+1.21.1");
        if (name.endsWith("ShaderCompileScopeMixin")) return present("immersive_portals") && present("iris") && FabricLoader.getInstance().getModContainer("iris").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.8.1+mc1.21.1");
        if ((name.endsWith("RecipeStackFormatMixin") || name.endsWith("StackCodecAliasMixin"))) return present("bclib") && FabricLoader.getInstance().getModContainer("bclib").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("30.4.0");
        if (name.endsWith("PositionPacketDimensionMixin") || name.endsWith("PositionPacketAwaitingMixin") || name.endsWith("DoorwayShipLoadingMixin") || name.endsWith("ShipChunkPacketMixin")) return present("immersive_portals");
        if (name.endsWith("ShipAetherPortalCreationMixin")) return present("aether");
        if (name.endsWith("ShipNetherPortalActivationMixin") || name.endsWith("ShipFramePortalActivationMixin")) return present("immersive_portals");
        if (name.endsWith("PortalShipWatchMixin") || name.endsWith("ShipPortalTransferMixin") || name.endsWith("ShipMotionDimensionMixin") || name.endsWith("ShipAcknowledgementRecoveryMixin") || name.endsWith("ShipAcknowledgementFlushMixin") || name.endsWith("MovementDiagnosticMixin") || name.endsWith("ShipGuardDiagnosticMixin") || name.endsWith("MovementTerrainDiagnosticMixin")) return present("immersive_portals");
        if (name.endsWith("JadeHelmMixin")) return present("jade") && present("vs_eureka") && FabricLoader.getInstance().getModContainer("jade").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("15.10.6+fabric");
        if (name.endsWith("WingTrailFallbackMixin")) return present("elytratrails") && FabricLoader.getInstance().getModContainer("elytratrails").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.4.7.5-1.21.1");
        if (name.contains("TardisShip")) {
            boolean supported = present("dwm") && Set.of("1.0.38.4").contains(FabricLoader.getInstance().getModContainer("dwm").orElseThrow().getMetadata().getVersion().getFriendlyString());
            return supported && (!(name.endsWith("TardisShipPortalMixin") || name.endsWith("TardisShipPortalDataMixin") || name.endsWith("TardisShipCollisionMixin") || name.endsWith("TardisShipExteriorShapeMixin")) || present("immersive_portals"));
        }
        if (name.endsWith("DhDynamicDimensionMixin")) return present("distanthorizons") && FabricLoader.getInstance().getModContainer("distanthorizons").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("3.3.3");
        if (name.contains(".client.MixinShip")) return present("immersive_portals");
        if (name.endsWith("SableCompatMixin")) return hasSable();
        if (name.endsWith("StaleGunAnimationMixin")) return present("pointblank") && FabricLoader.getInstance().getModContainer("pointblank").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("2.2.0");
        if (name.endsWith("EurekaPortalWarningMixin")) return present("immersive_portals") && present("vs_eureka");
        if (name.endsWith("EurekaDebugMixin")) return present("vs_eureka") && FabricLoader.getInstance().getModContainer("vs_eureka").orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.5.3-beta.4-td.4+b0d9511582");
        return true;
    }
    public String getRefMapperConfig() { return null; }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
