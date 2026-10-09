# Code and retained features

Runtime helpers and generated hooks are grouped by the mod they support. Shared
reflection and ship-pose utilities support several integrations. Generators and
regression fixtures live under tools and are not shipped as gameplay classes.

| Area | Retained purpose |
| --- | --- |
| Doctor Who | Ship landing, tracking, console coordinates, keys, waypoints, model caching and door-face clipping |
| Immersive Portals / VS | Moving portal poses, player entry/exit, authorized chunk delivery, native ship loading recovery and portal restrictions on ships |
| Freecam | Active camera ticking outside full chunks and nearby LOD clipping correction with DH |
| DH / Iris | Dynamic dimensions, portal render suppression and shader depth handling |
| Eureka / Jade | Creative name, supported warning filtering, quiet logging and helm HUD behavior |
| Aether / Player Graves | Resolved accessory death drops, persistent grave storage, GUI and original functional/cosmetic slot recovery |
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

Freecam BSL support: `freecam/FreecamShaderCompat`, generated `FreecamBslShaderMixin`
and `FreecamBslUniformMixin` target the BSL DH overlap discard through Iris.
`FreecamShaderTest` covers source selection and per-draw uniform restoration.

Jade targeting: `jade/ShipRaycastCompat` and generated `JadeShipRaycastMixin`
normalize ray vectors without changing ship block addresses (`JadeRaycastTest`).
Mac portal copying: `iris/FramebufferCopyCompat` and `IrisFramebufferCopyMixin`
provide a capability-gated GL3 fallback (`FramebufferCopyTest`).

Beta 11 extends `jade/ShipRaycastCompat` with scoped cached-hit range validation.
`iris/DepthFormatCompat` and generated `IrisDepthFormatMixin` select matching
Apple main/stencil depth formats; `DepthFormatTest` covers representation changes
and resource/binding restoration. The native renderer error fallback is retained.

Updater: `updater/UpdateInitializer` owns config, background checks, notices and
shutdown handoff. `ReleaseUpdates` selects and verifies public release assets.
`UpdateInstaller` is a standalone JDK-only post-exit installer with backup and
atomic replacement. `UpdateTest` exercises release policy and actual file/process
behavior. `.github/workflows/build.yml` publishes immutable versioned release jars.

Accessory graves: `graves/AccessoryGraveCompat`, generated `AccessoryGrave*Mixin`
hooks and `GravesGenerator` consume only Accessories' resolved death queue after a
successful grave spawn. Native grave serialization retains extra stacks; GUI sync
preserves the hidden backlog. `AccessoryGraveTest` covers item conservation and
`gravesNativeCheck` verifies the supplied jar's hooks and persistence calls.

Beta 14 adds `AccessoryGraveDeathSlotsMixin` to record native resolved-drop origins.
`AccessoryGraveCompat` persists source slot names/indices/cosmetic flags in NBT,
keeps them attached through GUI paging and restores through native Accessories
menu validation/setters before normal quick recovery. `AccessoryGraveSlotsTest`
covers persisted destinations, slot conflicts and count/item conservation.
