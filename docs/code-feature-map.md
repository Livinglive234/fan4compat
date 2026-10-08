# Code and retained features

Runtime helpers and generated hooks are grouped by the mod they support. Shared
reflection and ship-pose utilities support several integrations. Generators and
regression fixtures live under tools and are not shipped as gameplay classes.

| Area | Retained purpose |
| --- | --- |
| Doctor Who | Ship landing, tracking, console coordinates, keys, waypoints, model caching and door-face clipping |
| Immersive Portals / VS | Moving portal poses, player entry/exit, authorized chunk delivery, native ship loading recovery and portal restrictions on ships |
| Freecam | Active camera ticking outside full chunks when DH is installed |
| DH / Iris | Dynamic dimensions, portal render suppression and shader depth handling |
| Eureka / Jade | Creative name, supported warning filtering, quiet logging and helm HUD behavior |
| Point Blank | Offhand and held-item compatibility |
| Other integrations | Accessories packets, Sound Physics portal behavior, BCLib codecs/recipes, Sable dimensions and Elytra Contrails fallback |

## Why doorway loading remains

DoorwayShipLoading prepares ship chunks using Immersive Portals' existing loading
cadence. Nearby-player chunks and active TARDIS portal anchors supply the chunks
needed before a player exits, including empty active chunks required by the native
VS movement guard. Removing that path can reintroduce arrival freezes. It does
not install a separate ship-preview renderer or alternate ship packet protocol.

ShipGuardRecoveryMixin invokes bounded recovery directly when the native guard
blocks movement. ShipTransitCompat, ShipLoadRecovery, PortalShipWatchCompat and
ShipChunkPackets preserve transition acknowledgements and authorized delivery.
PortalMotionCompat follows the exterior geometry and supplies anchors for transit.

Temporary movement observers, their generators and the unused shipCollisions
passthrough are removed in beta 6. The WWOO border experiment remains removed.
Player portal transfer code is required; it does not transfer ships between worlds.

The build's featureLinkAudit rejects unconfigured generated mixins and packaged
classes with no reference path from an active mixin, plugin or entrypoint. This
checks structural reachability; it does not replace in-game regression testing.
