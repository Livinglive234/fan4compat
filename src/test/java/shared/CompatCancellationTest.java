import dev.fan4.compat.CompatPlugin;
public class CompatCancellationTest {
    static void check(boolean result) { if(!result)throw new AssertionError(); }
    public static void main(String[] args) {
        String frustum="org.valkyrienskies.mod.mixin.feature.fix_frustum_dead_loop.MixinFrustum";
        String tracked="org.valkyrienskies.mod.mixin.server.world.MixinChunkMap$TrackedEntity";
        String sable="org.valkyrienskies.mod.mixin.mod_compat.sableCompanion.MixinDefaultSableCompanion";
        check(CompatPlugin.shouldCancelFor(true,false,frustum));
        check(CompatPlugin.shouldCancelFor(true,false,tracked));
        check(!CompatPlugin.shouldCancelFor(false,true,frustum));
        check(!CompatPlugin.shouldCancelFor(false,true,tracked));
        check(!CompatPlugin.shouldCancelFor(true,true,sable));
        check(!CompatPlugin.shouldCancelFor(true,true,"org.valkyrienskies.mod.mixin.server.world.MixinServerLevel"));
        String viewport="qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumViewport";
        check(CompatPlugin.shouldCancelFor(false,false,true,viewport));
        check(!CompatPlugin.shouldCancelFor(true,true,false,viewport));
        check(CompatPlugin.shouldCancelFor(false,false,true,"qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumOcclusionCuller"));
        check(!CompatPlugin.shouldCancelFor(false,false,false,"qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumOcclusionCuller"));
        check(!CompatPlugin.shouldCancelFor(false,false,true,"qouteall.imm_ptl.core.compat.mixin.sodium.MixinSodiumRenderSectionManager"));
        // VS's two Sodium mixins that cannot apply to Sodium 0.8 are cancelled for Sodium 0.8 with a supported VS, and only then; the ones that can apply stay.
        String common="org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinRenderSectionManager",fabric="org.valkyrienskies.mod.fabric.mixin.compat.sodium.MixinRenderSectionManager";
        check(CompatPlugin.shouldCancelFor(false,false,true,true,common)&&CompatPlugin.shouldCancelFor(false,false,true,true,fabric));
        check(!CompatPlugin.shouldCancelFor(false,false,false,true,common)&&!CompatPlugin.shouldCancelFor(false,false,true,false,fabric));
        for(String keep:new String[]{"org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinSodiumWorldRenderer","org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinChunkTracker","org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinDefaultChunkRenderer","org.valkyrienskies.mod.mixin.mod_compat.sodium.MixinRenderSection"})
            check(!CompatPlugin.shouldCancelFor(true,true,true,true,keep));
        System.out.println("PASS: only duplicate IP hooks cancelled; normal VS and original Sable hooks retained");
    }
}
