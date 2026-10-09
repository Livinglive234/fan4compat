# Fan4Compat

A standalone Fabric 1.21.1 compatibility addon for the Fantastic 4 modpack.
It bridges Valkyrien Skies, Immersive Portals, dynamic dimensions used by Doctor
Who Mod / DimLib, and the Sable Companion library bundled with Windchimes.
It does not overwrite existing mod jars.

**Status: beta, 0.1.0-beta.12.** Eureka warnings are suppressed in both normal
and severe incompatibility notices, on client and dedicated server, for the
supported Eureka/IP combination. Other mod warnings remain unchanged.

Distant Horizons LOD and terrain fade draws are skipped while Immersive Portals
renders a portal view; Iris also ignores the main view's cached DH frame during
those draws. Beta 5 also substitutes a far-depth sampler for Iris's DH depth
textures during portal views; shader packs can otherwise sample stale terrain
even when the DH frame flag is disabled. Main-view DH rendering stays enabled. Portal destinations use normal
Minecraft terrain distance; this does not implement distant LODs through portals
or stop background world generation. Requires supported DH/IP (and Iris for the
shader guard), without requiring VS. The pack owner confirmed the BSL_v10.1.5 fix working; the initial LOD load can take longer, with subsequent loads behaving normally.
Includes beta 2/3 dedicated-server lookup and detached TARDIS save fixes.
Ship visibility through reopened doors before exiting remains a known limitation.

[Beta 19 release notes](docs/releases/0.1.0-beta.19.md) ·
[Release validation and wishlist](docs/release-readiness.md) ·
[Destination portal LOD prototype](docs/portal-destination-lods.md)

## Aether accessory graves

With Aether, Accessories 1.1.0-beta.48+1.21.1 and Player Graves 1.0.0 installed,
Aether-slot death drops enter the player's grave, including cosmetic accessories.
Uses Accessories' resolved drop list, preserving its keep-inventory and item
KEEP/DESTROY rules. Captures other Accessories slots too. Grave ownership and
access rules stay in effect; no portals or ship mods are required for this fix.

With exact inventory-layout recovery enabled, crouch-right-click restores
accessories to their original functional/cosmetic slots when those slots are empty
and still accept the items. Occupied, removed or invalid slots use normal inventory
recovery without replacing equipped items. Graves created before beta 14 lack this
slot metadata and recover accessories into normal inventory.
The grave screen has six rows. If it fills, reopen it after taking items to expose
remaining stacks; quick retrieval handles the whole grave. Items persist with the
grave across world saves. Keep Fan4Compat installed until these graves are emptied.
The grave stores up to 128 stacks; accessories beyond that capacity drop normally.
Install the same build on client and server. Gameplay testing is pending.

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

## Beta 11 remaining Jade rays and Mac recursive depth

Jade ignores cached crosshair hits outside its current targeting range before
building a ray, including hits retained across dimension changes. Its configured
range and eye/camera perspective are respected; the global crosshair is unchanged.

On Apple OpenGL, IP's recursive renderer chooses its stencil depth representation
from the actual main depth texture format, including unsized textures' depth bits
and component type. Layer buffers are recreated when that representation changes.
Other GPU vendors retain IP's choice. This targets a possible format mismatch
behind the compatibility-mode switch; the original error check and fallback stay
active. Mac portal-in-portal gameplay confirmation remains pending.

## Beta 10 Jade rays and macOS portal copying

Jade normalizes ship hit, eye and camera vectors into world space before its
ray calculations, preserving shipyard block addresses for overlay lookup.
Requires supported Jade and VS; Eureka and IP are optional for this fix.

With supported Iris/IP, contexts lacking `glCopyImageSubData` use OpenGL 3
framebuffer blits for the portal renderer's depth and color copies. This targets
the native abort on macOS OpenGL 4.1. The existing image-copy path stays active
on GPUs supporting it. Temporary framebuffer objects and scissor/binding state
are cleaned up after each fallback copy. Mac visual confirmation remains pending.

## Beta 9 Freecam with BSL

The reported disappearing LODs occur only with BSL enabled. BSL 10.1.5 has an
independent DH terrain/water distance discard that beta 8's native clip changes
do not affect. Beta 9 bypasses that specific discard while the active Freecam
is in an unloaded full-terrain chunk. Loaded terrain, disabled Freecam and portal
views keep the original rule. Requires supported Freecam, DH and Iris versions;
shader pack files and global settings remain unchanged. In-game confirmation is
pending.

## Beta 8 Freecam nearby LOD clipping

While the active Freecam camera is in an unloaded full-terrain chunk, DH's near
plane is capped at half a block and its native terrain near-clip uniform is zero.
This targets native clipping around a camera where full chunks cannot replace them.
Native clipping returns in loaded terrain or when Freecam is disabled. Normal
player views, other dimensions and portal views retain their native behavior.
No terrain generation or permanent DH setting is changed. In-game verification
of the reported disappearing/reappearing terrain remains pending.

## Beta 7 Freecam / Distant Horizons

The active Freecam camera can tick beyond Minecraft's loaded terrain chunks when
Freecam `1.3.0+mc1.21` (the 1.21.1 download's internal version) and DH `3.3.3` are
installed. Minecraft's inherited player tick otherwise returns at that boundary,
even when DH displays distant terrain. The hook changes that one tick condition
for the active camera only; the real player retains the native terrain check.
It does not request extra chunks, enable interactions with LODs, or alter ship
collision guards. Camera movement beyond the boundary needs in-game verification.

## Beta 6 cleanup

Removed temporary movement diagnostics and unused collision-query code. Native
ship loading recovery and chunk preparation needed for TARDIS exits remain.
No separate ship-preview renderer is retained.
[Code and feature map](docs/code-feature-map.md) explains the retained paths.

## Alpha 35: remove the WWOO border experiment

The experimental WWOO / Distant Horizons generation border from alpha 34 has
been removed at the pack owner's request. Its visual benefit was unverified and
it added generation work. DH now uses its original generation path; the
`fan4compat.wwooBorder` JVM switch has no effect in this build.

WWOO grid seams remain an unresolved upstream compatibility issue. The existing
Distant Horizons dynamic-dimension bridge remains enabled for its supported
version. [Investigation notes](docs/wwoo-distant-horizons.md) are retained as history.

## Alpha 33 deleted ship waypoints

Ship waypoints whose original ship ID no longer exists show a crossed-out name
in the waypoint list. Selecting one shows **This ship has been deleted or
reassembled** in the coordinate area. The message wraps within the detail column.
You can still remove the waypoint; it remains immutable and preserves normal
access rules. Successfully loading an available ship waypoint also shows DWM's
normal **Waypoint loaded** message.

Status is supplied by the server when the console data is refreshed (reopen the
waypoint screen after deleting a ship). The check uses all registered ship data,
so an existing ship whose chunks are unloaded is not marked deleted. Names and
saved anchors are unchanged, and a reassembled ship with a new ID does not revive
the old waypoint. This display change still needs in-game verification.

## Alpha 32 waypoint screen crash fix

Selecting a ship waypoint hides its coordinate fields without changing their
text. This prevents recursive DWM change callbacks and the reported stack overflow.
Saved waypoint data remains intact.

## Alpha 31 goals

Ship waypoint creation now reads DWM's nested `tardisTag`, so it preserves the
ship identity and local landing address even while the ship moves during naming.
Create a new waypoint while landed on the ship; existing ordinary world waypoints
are not automatically rebound. Ship waypoints keep the existing access rules,
hide coordinates, and cannot be updated. They bind to a persistent VS ship ID.
Disassembling and reassembling a ship can replace that ID; a waypoint to the old
ship then reports that the ship is unavailable. Save a new waypoint while landed
on the reassembled ship. Waypoints are not rebound to another ship by proximity.

Elytra Contrails `1.4.7.5-1.21.1` receives fallback wing emitters when a gliding
player's custom or accessory renderer supplies none. The fallback uses a vanilla
elytra model with the player's rendered body pose, including roll, and preserves
existing native samples and trail settings. Custom wing shapes use approximate
vanilla-sized wing tips. Other Elytra Contrails versions skip this hook.

The missing TARDIS biome translations are supplied, including the key reported
by Xaero. Create/Flywheel rendering remains open; see [the investigation](docs/create-rendering.md).
The pack owner confirmed waypoint UI, contrails and the TARDIS biome label working.
Return travel using ship waypoints is now confirmed in singleplayer, including
a world restart. Reassembly still requires a new waypoint for the new ship ID.

## Ship loading recovery (alpha 27)

Acknowledgements are now sent even when native player construction already marked
ships known locally, and resent after portal world transitions. Early acknowledgements
wait briefly for the authoritative server ship to finish loading. Nearby active ship
chunks remain watched through IP's existing loading cadence, including empty chunks
outside the visible hull that the VS movement guard needs.

When the native guard blocks the local player, bounded recovery retransmits native
acknowledgements and selectively requeues valid delivered IP watches near the player.
Collision, teleport validation and IP's normal chunk batching remain in place.
These changes require in-game verification against the reported boundary freeze.

## Install

1. Add `Fan4Compat-0.1.0-beta.12.jar` to `mods` on the client and server.
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

## Supported builds

| Component | Target |
| --- | --- |
| Minecraft / loader | Fabric 1.21.1; Java 21 or newer |
| Valkyrien Skies | `2.4.12-td.9+66a13242ed`, exact original jar |
| Immersive Portals | Fabric `6.0.6`, supplied filename says 6.0.7 |
| Eureka debug control | `1.5.3-beta.4-td.4+b0d9511582` |
| Doctor Who Mod ship bridges | `1.0.38.4` only; other DWM versions skip these hooks |
| Optional log fixes | Iris `1.8.1+mc1.21.1`, Accessories `1.1.0-beta.48+1.21.1`, Sound Physics `1.21.1-1.5.1`, BCLib `30.4.0` |
| Sable Companion crash observed | `1.6.0`, bundled in Windchimes `1.2.0+1.21.1` |
| Elytra Contrails fallback | `1.4.7.5-1.21.1` only |
| Jade helm overlay | `15.10.6+fabric` only |
| Freecam / DH unloaded camera tick | Freecam `1.3.0+mc1.21`, DH `3.3.3`; client only |
| Distant Horizons dynamic-world transfer | `3.3.3` for Fabric 1.21.1 |

The ship hooks intentionally pin the VS implementation because their dimension
lifecycle targets include names from that build's obfuscated physics core.
Other mod releases need another compatibility audit.

## Fixes

* Cancel VS's duplicate frustum and tracked-entity mixins only while IP is
  installed. Preserve them when IP is absent. MixinSquared performs cancellation
  at runtime; original jars stay intact.
* Preserve ship-local hit coordinates, while using ship-aware distances for
  Minecraft's final interaction reach check and IP's portal-versus-block choice.
* Restore VS ship-chunk connectivity initialization and Sodium notification
  after IP's replacement client chunk manager loads ship chunks. Sodium is notified
  in the chunk's own world, including remote portal views. Connectivity uses only
  the active player world because the VS queue drains against that world.
* Restore loaded-ship acknowledgements after portal-world changes: synchronized
  loaded ships in the active dimension are acknowledged once through VS's normal
  known-ship API. VS's unloaded chunk and unsynchronized-world guards remain active.
* Resolve VS's unflagged covariant getter adapters by their most specific return
  type when parameter signatures are identical. This fixes alpha.6's repeated
  `Et.getAllShips` reflection error; genuine method overloads remain guarded.
* Clear VS's ship attachment, drag/interpolation state and queued position update
  before IP changes a player's dimension on both client and server. Reject C2S
  ship motion from a ship in another dimension, preventing a late overworld packet
  from replacing the arrival position inside the TARDIS.
* Implement VS's ship-chunk cache interface on IP's client chunk manager. Ship
  unloads use IP's existing per-world cache and normal unload notifications;
  unrelated chunks and other worlds' caches remain intact. This fixes the
  `ImmPtlClientChunkMap` / `ClientChunkCacheDuck` cast failure on dimension changes.
* Defer out-of-phase VS dimension adds, updates and removals until the next
  `preTick` finishes. Close the mutation window before `POST_TICK_START`.
  Startup registration remains synchronous, operation order is preserved, and
  VS consistency exceptions remain active. This addresses the observed TARDIS
  dimension registration after `CLEAR_FOR_RESET`.
* Use Distant Horizons' normal `getOrLoadLevel` path when its player-transfer
  callback encounters a dynamically created world that missed initial level
  registration. Existing level instances are reused. This fixes the observed
  null level in `AbstractDhServerWorld.changePlayerLevel`; it does not suppress
  DH's own level-loading errors or replace inventory data.
* When Sable Companion is only its default fallback, do not feed its VS-backed
  plot check back into VS's Sable exclusion logic. This prevents recursion and
  avoids misclassifying VS ship chunks as a separate Sable sublevel. Genuine
  non-default Sable implementations retain their normal path.
* Disable the two unconditional `[VS-TD-DBG-CLIENT]` INFO calls in the targeted
  Eureka client callback by default. Other Eureka logging stays intact.

* Keep DWM exterior block positions in VS ship-local coordinates. Display the
  transformed world position and facing on the console and new waypoint screen.
* Transform both TARDIS door portal endpoints and the exterior portal orientation
  when the ship moves or rotates. Stationary endpoints avoid repeated syncs.
* Position ship-attached door portals before spawning them and disable IP's default
  position animation so a newly opened portal does not animate from shipyard space.
* Cut a doorway through the front half of an open ship-mounted TARDIS shell when
  its active portal is teleportable. Preserve side walls, the back, deck and
  neighboring blocks. Closed doors keep DWM's original collision and outline.
* Use the exterior's transformed world position for the key's normal ten-block
  opening range. Keep saved exterior addresses in ship-local coordinates.
* Acknowledge already received ships across portal dimensions, without requiring
  the player to visit the ship's dimension first.
* Show world coordinates followed by `(On ship)` in the sonic destination message,
  while preserving the ship-local landing target.
* Coordinate-based vertical scans also compare VS ship surfaces with terrain,
  validating ship-local landing space through DWM's native checks.
* Key recall recognizes the ship deck below the player. A direct ship-local
  support-block check handles raycasts that miss the deck or report only terrain. Store a fixed block
  position and facing on that ship, so the destination follows its movement
  throughout the flight. Use DWM's direct safe-landing check; an obstructed or
  removed target fails rather than falling through to terrain. Save the target
  ship identity with the recall so this check survives a world reload.
* Measure ship flights in world coordinates. Ship trips use ordinary demat/remat
  because DWM's flyover animation assumes stationary world block positions;
  the saved flyover lever setting remains intact for ordinary trips.

Immersive Portals’ Eureka incompatibility notice is suppressed while Fan4Compat
and Eureka are installed. Other mod warnings (including Create), shader warnings
and severe incompatibility checks remain active. This does not edit IP settings.

Point Blank Fabric 2.2.0: stale gun animation callbacks with empty or non-gun
item stacks return no controllers, preventing the `AirBlockItem` → `GunItem`
cast crash. Valid gun animations retain their native handling. Alpha 39 also rejects draw
transitions when the passed item differs from the cached gun or its native fire
mode is unresolved. This prevents the subsequent null fire-mode ammo crash
without changing ammo data or fabricating a fire mode. Alpha 40 corrects the
client-tick draw call to use Point Blank’s operable gun context, including an
offhand gun while a TARDIS key is in the main hand. Gun type and saved weapon UUID
must match the selected state; other draws retain their original stack.

## Eureka debug option

Fan4Compat creates `config/fan4compat.properties` with:

```properties
eurekaDebugLogging=false
```

Set it to `true` and restart the game to enable Eureka's ship debug INFO messages.
The optional JVM argument `-Dfan4compat.shipDebug=true` overrides the file setting.

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

## Validate in game

Test in a copy of the world, starting with the original VS jar and Fan4Compat.

1. Start the client and a dedicated server.
2. Assemble a small Eureka ship; check its model, helm outline, helm interaction,
   steering, and save/reload with Sodium and Iris.
3. Place a new TARDIS and open it to create a new interior; repeat with another
   TARDIS, then reopen an existing interior and reload the world. Check return
   travel with Distant Horizons, a few disposable inventory items and an inventory
   comparison before and after. Verify a stationary TARDIS before a ship-mounted
   one. With DWM 1.0.38.4, summon while standing on a ship deck, then move
   and rotate the ship before arrival. Check it lands at the same deck block,
   the console shows world coordinates, and both portal directions track the
   doors. Enter and exit with the ship stopped, then moving. Open the doors from
   inside and check that the portal appears attached immediately and the ship is
   visible after exiting; pre-entry ship visibility is a known limitation. Set a sonic destination on the deck and check world coordinates
   followed by `(On ship)`; repeat on terrain to check the ordinary message.
   Check repeated crossings without commands, stationary entry, inventory and
   dimension agreement, then repeat while moving. Check closed-door and nearby deck collisions. Repeat after save/reload. Block the landing space and verify failure
   instead of a ground landing.
4. Restore Windchimes and check startup, nearby ship rendering and interaction.
5. Check stationary and moving ship-mounted TARDIS doorways with shaders
   enabled and disabled.

The standalone build and static verification are not substitutes for these
runtime checks. In particular, the new dimension queue is covered by isolated
tests, but has not yet been tested with the full Minecraft/DimLib runtime.

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
