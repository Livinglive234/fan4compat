package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.CompatibilityRules;
import dev.fan4.compat.shared.Features;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Starts the Xaero features that need networking: waypoint sync and the shared explored map. The server side needs only Fabric
 * API; the client side additionally needs the supported Xaero mod, otherwise it stays silent and the server skips that player.
 */
public final class XaeroSyncInitializer implements ModInitializer {
    private static boolean supported(String id) {
        var container=FabricLoader.getInstance().getModContainer(id);
        return container.isPresent()&&CompatibilityRules.supported(Map.of(id,container.get().getMetadata().getVersion().getFriendlyString()),id);
    }
    public void onInitialize() {
        FabricLoader loader=FabricLoader.getInstance();
        if(!loader.isModLoaded("fabric-networking-api-v1"))return;
        boolean client=loader.getEnvironmentType()==EnvType.CLIENT;
        if(Features.enabled("xaeroWaypointSync")) {
            try {
                XaeroSyncBridge.registerPayloads();
                XaeroSyncBridge.registerServer();
                if(client&&supported("xaerominimap"))XaeroSyncBridge.registerClient();
                dev.fan4.compat.shared.GameLog.info("Xaero waypoint sync is active");
            } catch(RuntimeException|LinkageError failure) {
                dev.fan4.compat.shared.GameLog.warn("Xaero waypoint sync could not start and is disabled",failure);
            }
        }
        if(Features.enabled("xaeroSharedMap")) {
            try {
                SharedMapBridge.registerPayloads();
                // A dedicated server records and relays; a client install (including its own single-player world) only receives.
                if(!client&&loader.isModLoaded("fabric-lifecycle-events-v1"))SharedMapBridge.registerServer();
                if(client&&supported("xaeroworldmap"))SharedMapBridge.registerClient();
                dev.fan4.compat.shared.GameLog.info("Xaero shared map is active");
            } catch(RuntimeException|LinkageError failure) {
                dev.fan4.compat.shared.GameLog.warn("Xaero shared map could not start and is disabled",failure);
            }
        }
    }
}
