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
        System.out.println("PASS: only duplicate IP hooks cancelled; normal VS and original Sable hooks retained");
    }
}
