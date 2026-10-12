package dev.fan4.compat.xaero;

import dev.fan4.compat.shared.CompatibilityRules;
import dev.fan4.compat.shared.Features;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Starts Xaero waypoint sync. The server side needs only Fabric API; the client side additionally needs the
 * supported Xaero's Minimap, otherwise it stays silent and the server simply skips that player.
 */
public final class XaeroSyncInitializer implements ModInitializer {
    public void onInitialize() {
        System.Logger log=System.getLogger("Fan4Compat");
        if(!Features.enabled("xaeroWaypointSync")||!FabricLoader.getInstance().isModLoaded("fabric-networking-api-v1"))return;
        try {
            XaeroSyncBridge.registerPayloads();
            XaeroSyncBridge.registerServer();
            var xaero=FabricLoader.getInstance().getModContainer("xaerominimap");
            if(FabricLoader.getInstance().getEnvironmentType()==net.fabricmc.api.EnvType.CLIENT&&xaero.isPresent()
                &&CompatibilityRules.supported(Map.of("xaerominimap",xaero.get().getMetadata().getVersion().getFriendlyString()),"xaerominimap"))
                XaeroSyncBridge.registerClient();
            log.log(System.Logger.Level.INFO,"Xaero waypoint sync is active");
        } catch(RuntimeException|LinkageError failure) {
            log.log(System.Logger.Level.WARNING,"Xaero waypoint sync could not start and is disabled",failure);
        }
    }
}
