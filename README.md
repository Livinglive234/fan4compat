# Fan4Compat

A standalone Fabric 1.21.1 compatibility addon for the Fantastic 4 modpack.
It bridges Valkyrien Skies, Immersive Portals, the dynamic dimensions used by
Doctor Who Mod / DimLib and the Sable Companion library bundled with Windchimes,
and carries smaller fixes for other mods in the pack (Xaero, Distant Horizons,
Iris, Point Blank, Aether, Spatial GUI and more). It does not overwrite existing
mod jars, and each fix applies only when the mod it targets is installed at a
supported version.

**Status: beta, 0.1.0-beta.51.**

[Beta 51 release notes](docs/releases/0.1.0-beta.51.md) ·
[Architecture and feature guide](docs/architecture-and-features.md) ·
[Mac nested portal investigation](docs/mac-nested-portals.md) ·
[Release validation and wishlist](docs/release-readiness.md) ·
[Destination portal LOD prototype](docs/portal-destination-lods.md)

## Supported builds

| Component | Target |
| --- | --- |
| Minecraft / loader | Fabric 1.21.1; Java 21 or newer |
| Valkyrien Skies | `2.4.12-td.9+66a13242ed`, exact original jar |
| Immersive Portals | Fabric `6.0.6` (the jar named 6.0.7 is the same build) |
| Eureka | `1.5.3-beta.4-td.4+b0d9511582` |
| Doctor Who Mod | `>=1.0.38.4`, including `1.0.39`; older or unparseable versions skip the ship bridges |
| Distant Horizons | `3.3.3` for Fabric 1.21.1 |
| Freecam | `1.3.0+mc1.21` (client only) |
| Iris | `1.8.1+mc1.21.1` |
| Point Blank | Fabric `2.2.0` |
| Accessories / Player Graves | `1.1.0-beta.48+1.21.1` / `1.0.0` |
| Sound Physics Remastered | `1.21.1-1.5.2` |
| BCLib | `30.4.0` |
| Jade | `15.10.6+fabric` only |
| Elytra Contrails | `1.4.7.5-1.21.1` only |
| Xaero's Minimap | `26.6.0` (or `26.5.0`) |
| Xaero's World Map | `1.47.0` (or `1.46.0`) |
| Spatial GUI | `1.9.1` only |
| Sable Companion | crash observed in `1.6.0`, bundled in Windchimes `1.2.0+1.21.1` |
| Aether, Bumblezone, Trinkets, Chest Tracker | no version check (Chest Tracker was checked against `2.6.7`) |

The ship hooks intentionally pin the VS implementation because their dimension
lifecycle targets include names from that build's obfuscated physics core.
Other mod releases need another compatibility audit.

## Features

**Ships and TARDIS**
- Ship-mounted TARDIS: recall, landing, door portals, coordinates and sonic destinations follow a moving ship.
- Ship waypoints that survive restarts and show when a ship has been deleted.
- Ships load, sync and cross portals reliably with Immersive Portals (duplicate Valkyrien Skies hooks cancelled, ship loading recovery, safe dimension changes, ship-aware reach).
- Portals cannot be lit on ships; Eureka's portal warnings and debug messages are hidden; Valkyrien Skies' per-save log message is hidden by default.
- Ship-aware sound occlusion (Sound Physics Remastered) and Jade hidden while steering a helm.

**Rendering**
- Immersive Portals on Apple GPUs (depth and stencil formats, framebuffer-copy fallback).
- Distant Horizons and Iris shaders behave through portal views; Freecam works with Distant Horizons, including with BSL.
- Point Blank scopes and guns no longer corrupt portal rendering or crash on stale animations.
- Elytra Contrails wing trails for custom and accessory elytra; TARDIS model cache.
- Spatial GUI draws correctly next to Immersive Portals portals, and portals stay visible with its screen open.

**Maps**
- Xaero's map, minimap and waypoints follow the TARDIS exterior.
- Shared explored map: terrain any player has explored shows on everyone's World Map.
- Xaero waypoint sync across PCs through the server.

**Pack extras**
- Aether and Trinkets items go into Player Graves graves and return to their slots.
- Aether capes no longer turn your skin layers back on.
- Chest Tracker's button is hidden on screens that cannot hold items.
- Bumblezone turns shaders off on entry and back on when you leave.
- BCLib recipe fix and Sable Companion fallback.

**Built in**
- Automatic updates from this repository's releases.
- Settings screen (Mod Menu + YACL) for every switchable feature.


## Install

1. Add `Fan4Compat-0.1.0-beta.51.jar` to `mods` on the client and server.
2. Install whichever supported target mods you want. No gameplay mod is required
   by Fan4Compat; Fabric Loader, Minecraft 1.21.1 and Java 21 remain required.
   MixinSquared is bundled in the addon.
3. If using ship fixes, use the original supported VS jar, not earlier patched
   `IP-compat-v4` / `IP-Sable-*` builds. Unsupported versions skip their hooks
   and dependent bridges, with a startup warning; they do not block the addon.

Fixes activate only when their required combination is installed at supported
versions. With Point Blank + Doctor Who Mod alone, the gun animation crash fix
and DWM model-cache reuse apply. TARDIS ship features require supported VS;
portal-specific ship hooks also require supported Immersive Portals. Accessories
and Iris portal fixes require IP but do not require VS. DH dynamic dimensions,
BCLib recipe fixes and Elytra Contrails fallback can activate independently.
Sound Physics ship acoustics and Jade helm hiding require VS (Jade also Eureka).
Sound Physics/IP snapshot protection works without VS; without VS installed,
acoustic rays are restricted to cloned chunks and split below IP’s ray limit.
A missing snapshot uses Sound Physics’s default audio environment for that
evaluation. Unsafe level access retains its native ray handling.
No target mods installed means no compatibility mixins apply.

## Automatic Fan4Compat updates

Install beta 12 or newer once on each client and server. Fan4Compat then checks
[this repository's releases](https://github.com/Livinglive234/fan4compat/releases)
on startup and every 30 minutes, including prereleases by default. New jars are
checksum-verified and staged in `.fan4compat-update` outside `mods`. After a normal
exit, an independent installer waits for the game/server process to finish and
atomically replaces the original jar for the next launch. The running game is
unchanged; the server never restarts itself. Clients receive a chat notice when
an update is ready. The original jar is backed up outside `mods`.

Settings: `config/fan4compat-updater.properties`. Set `enabled=false` to stop all
checks, `autoDownload=false` for notices only, or `allowPrereleases=false` for
stable releases only. These settings take effect after restarting. Only Fan4Compat
is updated; other mods, configs and content packs remain yours to manage.

Successful versioned main-branch builds automatically prepare **draft releases**
with jars and checksums. Test builds remain available in Actions; the updater
ignores drafts and Actions artifacts. After testing, publish the draft on GitHub
to make it available to players. Beta/alpha/RC versions remain prereleases.
Existing release assets are never overwritten; bump the version and add release
notes for each new release. Pull requests only upload Actions artifacts.

See [updater details and server hosting](docs/automatic-updates.md), including
pre-start installation for hosts that kill child processes at shutdown.

## Settings screen and options

With **Mod Menu** and **Yet Another Config Lib** (YACL 3) installed, Fan4Compat gets a
config button in Mod Menu. Without them nothing changes; there is no dependency on either.
Options for mods you do not have installed are greyed out and name what they need.
Everything is saved to `config/fan4compat.properties`, which you can also edit by hand.
An **Updater settings** button on the first tab opens a second screen for the automatic update
switches (check for updates, download automatically, include beta releases), saved to
`config/fan4compat-updater.properties`; they apply at the next launch.

| Option | Key | Default | Applies |
| --- | --- | --- | --- |
| Apple GPU portal fixes | `appleGpuFixes` | on | after restart |
| Point Blank scope stencil fixes | `pointBlankStencilFixes` | on | after restart |
| Freecam with Distant Horizons | `freecamFixes` | on | after restart |
| Elytra Contrails wing trails | `wingTrails` | on | after restart |
| TARDIS model cache | `tardisModelCache` | on | after restart |
| Block portals on ships | `shipPortalBlocking` | on | after restart |
| Ship-aware sound occlusion | `shipAcoustics` | on | after restart |
| Hide Eureka portal warnings | `eurekaWarnings` | on | after restart |
| Hide Jade while at a helm | `jadeHelm` | on | after restart |
| Xaero map shows the TARDIS exterior | `xaeroTardisMap` | on | after restart |
| Xaero minimap and waypoints follow the TARDIS exterior | `xaeroTardisMinimap` | on | after restart |
| Shared explored map | `xaeroSharedMap` | on | after restart |
| Xaero waypoint sync | `xaeroWaypointSync` | on | after restart |
| Bumblezone turns shaders off | `bumblezoneShaders` | on | after restart |
| Spatial GUI keeps its screen steady beside portals | `spatialGuiPortalFix` | on | after restart |
| Aether capes keep your skin layers | `aetherCapeSkinLayers` | on | after restart |
| Chest Tracker skips non-storage screens | `chestTrackerScreens` | on | after restart |
| Trinkets items in graves | `trinketGraves` | on | immediately |
| Valkyrien Skies save logging | `shipSaveLogging` | off | after restart |
| Eureka ship debug logging | `eurekaDebugLogging` | off | after restart |
| Point Blank render diagnostics | `renderDiagnostics` | off | after restart |
| Distant Horizons render diagnostics | `dhRenderDiagnostics` | off | after restart |
| Portal depth-copy logging | `portalDepthDiagnostics` | off | after restart |
| Xaero diagnostics | `xaeroDiagnostics` | off | immediately |
| Spatial GUI portal diagnostics | `spatialGuiDiagnostics` | off | after restart |

**Default-on rule.** Every feature ships enabled by default, including each new one. A settings switch exists so the pack owner or a player can turn a feature off, never so they have to turn it on. Troubleshooting aids (render and Distant Horizons diagnostics, Valkyrien Skies save logging, portal depth-copy logging, Xaero diagnostics, Eureka debug logging) are not features: they stay off unless the pack owner decides otherwise.

Only optional behavior and troubleshooting aids are listed. Fixes that protect saves or prevent
crashes (ship loading, portal transit, grave storage, shader compile repairs) are not switchable.
A JVM argument `-Dfan4compat.feature.<key>=true|false` overrides the file for one launch, and the
older `-Dfan4compat.renderDiagnostics`, `-Dfan4compat.dhRenderDiagnostics`,
`-Dfan4compat.portalDepthDiagnostics`, `-Dfan4compat.xaeroDiagnostics` and `-Dfan4compat.shipDebug` arguments still work.
The Xaero diagnostics write `[Fan4Compat XaeroDiag]` lines to `latest.log`: inside a TARDIS the exterior, compass turn, cave start and layer (when they change); the shared map's queue and progress every few seconds; and each step of the waypoint exchange on the server and the client.
The render diagnostics switch also turns on the Distant Horizons diagnostics unless those are set
separately. Xaero waypoint sync has its entry above.

## Remaining issues and limits

Create/Flywheel with active Iris shaders remains an open compatibility task.
Flywheel's backend fallback is intentional in the inspected version; enabling it
requires a shader integration, rather than removing its guard. No specific
contraption rendering failure has yet been reproduced. See
[the investigation](docs/create-rendering.md).

Ship visibility through reopened TARDIS doors before exiting remains a known
limitation and is now on the [wishlist](docs/release-readiness.md#wishlist--forward-thinking).
It is not a blocker for releasing the existing feature set. TARDIS entry, exit,
placement and landing were confirmed in alpha 21; this does not validate every
later change. The pack owner subsequently confirmed moving-ship exits, ship
waypoint UI, contrails and the TARDIS biome label working in the latest build.
Ship waypoint return after travel and restart, save/reload and inventory
preservation are now confirmed in singleplayer. Dedicated-server verification
and an explicit shader on/off comparison remain outstanding.
That confirmation covers the tested setup; every shader and wing variant has
not been individually verified.

Ship recall targets the supporting deck beneath the player, rather than a deck
viewed from elsewhere. The ship must stay loaded during the flight. Remote
ship views and all shader/render combinations remain unverified. Portal
compatibility is scoped to player crossings and views of ship-mounted doorways. Client connectivity restoration deliberately
targets the active player world; the existing VS queue drains against that world.
ChestTracker persistence errors remain outside this addon's current scope.
Older resource/model warning candidates are accepted as low priority by the pack
owner and tracked in [log follow-ups](docs/log-followups.md). Older logs do not
establish a new regression in the current build.

## Release readiness and future work

Singleplayer testing now confirms the core TARDIS workflows, ship waypoint return
after restart, save/reload, inventory preservation and repeated crossings.
The next step toward beta is dedicated-server verification of those workflows
and an explicit comparison with the supported shaders enabled and disabled.
Builds already pass automated checks. A stable release follows beta playtesting
and resolution of reproducible core regressions.
See [the checklist and wishlist](docs/release-readiness.md). No release stage has
been changed by this documentation update.

## Build

Install a Java 17+ **JDK** for building. The game needs Java 21+.
Then run from this repository:

```sh
./gradlew clean build
```

On Windows, use `gradlew.bat clean build`. The wrapper downloads the pinned Gradle
version automatically; Python is not required.

The jar is written to `build/libs`. Gradle resolves checksum-verified build
dependencies, compiles the addon, generates intermediary-name mixins, runs the
dimension scheduling, cancellation, ship geometry, summon attachment and portal-handler tests, and
verifies generated bytecode.
MixinSquared is bundled as a nested library. Existing game/mod jars are not
required to build, and none are modified or redistributed.

The ASM generation in `tools` makes this exact-version bridge buildable without
bundling Minecraft or the unofficial port binaries. Runtime helpers live in
`src`. There is one GitHub Actions job, **Build**, which runs `./gradlew clean build` and
uploads the jar.

## Repository layout

Compatibility code is grouped by the mod being integrated with Valkyrien Skies:

| Folder | Responsibility |
| --- | --- |
| `doctorwho` | Ship-mounted TARDIS recall, landing, portals, coordinates and sonic destinations |
| `immersiveportals` | Portal crossing, remote ship/chunk watching and client render alignment |
| `valkyrienskies` | Physics-stage dimension mutation queue |
| `eureka` | Optional ship debug logging and settings |
| `freecam` | Active camera ticking beyond full terrain chunks with DH |
| `distanthorizons` | Initialize destination DH levels during dimension changes |
| `sable` | Handle Sable Companion's default VS fallback |
| `iris`, `accessories`, `bclib`, `soundphysics` | Version-specific shader, portal entity, recipe and acoustic bridges |
| `elytratrails` | Fallback wing-tip samples for custom and accessory elytra rendering |
| `pointblank` | Guard stale non-gun animation callbacks (Fabric 2.2.0) |
| `jade` | Hide the overlay while piloting a Eureka helm |
| `shared` | Reflection, ship geometry, generation and verification utilities |

Runtime Java packages are under `src/main/java/dev/fan4/compat/<folder>`.
`CompatPlugin` stays at the package root to handle optional-mod loading.
ASM generators and their fixture tests are under `tools/<folder>`; these are
build-time tools with default-package classes and are excluded from the addon JAR.
The ordinary regression tests are grouped under `src/test/java/<folder>`.
Distant Horizons and Sable need only generated mixins, so their implementation
source lives in `tools`, rather than runtime helper packages.

Generated mixins use `dev.fan4.compat.mixin.<folder>.common` or `.client`.
The single `fan4compat.mixins.json` lists their full names relative to the mixin
package. Packaging checks verify all configured mixins, the plugin and the Fabric
entrypoint exist in the JAR. Any future package move must update these references
and the generators' helper class names together. Mod ID, configuration filename
and saved-data keys are independent of these Java package names.

## Maintenance conventions

Keep runtime helpers, generators and tests grouped by the mod they support.
Use `shared` only for utilities used across integrations. Keep investigations in
`docs/` and temporary dependencies, logs and experiments in ignored `work/`.
Generated classes and release JARs belong in ignored `build/`; the Gradle wrapper
JAR is intentionally tracked.

Update this README and `PROGRESS.md` with each compatibility change, including
supported versions, user-visible behavior and outstanding runtime validation.
Keep completed implementations separate from open goals and preserve historical
results as history. Produce release artifacts with `./gradlew clean build`.
