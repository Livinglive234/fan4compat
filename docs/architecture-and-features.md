# Fan4Compat: architecture, features and contributor guide

This document explains the active implementation at **0.1.0-beta.23**, reviewed
2026-10-10 (America/Chicago). It is an onboarding guide for human contributors and
AI coding assistants. The source and version gates describe what actually runs;
older release notes describe historical builds and can contain superseded behavior.

## Contents

- [Purpose and boundaries](#purpose-and-boundaries)
- [Repository and runtime architecture](#repository-and-runtime-architecture)
- [Which integrations activate](#which-integrations-activate)
- [Coordinate systems and ownership](#coordinate-systems-and-ownership)
- [TARDIS on ships](#tardis-on-ships)
- [Immersive Portals and ship lifecycle](#immersive-portals-and-ship-lifecycle)
- [Dimension lifecycle and Sable](#dimension-lifecycle-and-sable)
- [Distant Horizons, Iris and Freecam](#distant-horizons-iris-and-freecam)
- [Point Blank](#point-blank)
- [Accessory and Trinkets graves](#accessory-and-trinkets-graves)
- [Other compatibility features](#other-compatibility-features)
- [Automatic updates and releases](#automatic-updates-and-releases)
- [Saved data, settings and cleanup](#saved-data-settings-and-cleanup)
- [Build and verification](#build-and-verification)
- [How to change this mod safely](#how-to-change-this-mod-safely)
- [Known limits and abandoned work](#known-limits-and-abandoned-work)
- [Troubleshooting map](#troubleshooting-map)

## Purpose and boundaries

Fan4Compat is a standalone **Fabric Minecraft 1.21.1** addon for the Fantastic 4
pack. It repairs specific incompatibilities between installed mods. The largest
integration lets a Doctor Who Mod TARDIS work on a moving Valkyrien Skies ship,
with Immersive Portals handling its doorway. Smaller integrations address dynamic
worlds, graphics state, equipment persistence, targeting, sound and update delivery.

Abbreviations used below: **DWM** = Doctor Who Mod, **VS** = Valkyrien Skies,
**IP** = Immersive Portals, **DH** = Distant Horizons, and **SPR** = Sound Physics
Remastered. A chunk is Minecraft's horizontal 16-by-16 block unit; a LOD is DH's
simplified distant terrain representation, not a full interactive Minecraft chunk.

A compatibility feature should preserve each owning mod's rules while repairing
the point where another mod bypasses or contradicts them. Examples include keeping
DWM's landing checks, IP's chunk-watch authorization, VS's unloaded-ship movement
guard, and equipment mods' drop/slot-validation rules.

The addon does not replace the original gameplay jars. It packages generated
Mixin hooks and small runtime helpers. No gameplay mod is a mandatory dependency;
features activate for the installed, supported combination. Minecraft, Fabric
Loader and Java are still required, and MixinSquared is bundled. Some fixes are
client-only; shared ship, world, grave and packet behavior needs matching client
and server builds. A client rendering fix alone does not need a server update.

Ship transfers between dimensions are outside the requested scope. Player portal
crossings are supported; that is different from transferring the ship itself.

## Repository and runtime architecture

### Where code lives

| Location | Role |
| --- | --- |
| [`src/main/java/dev/fan4/compat`](../src/main/java/dev/fan4/compat) | Runtime helpers grouped by integrated mod; `shared` contains common utilities. |
| [`tools`](../tools) | ASM generators, native-jar checks and executable smoke fixtures. Default-package tool classes are build-time code. |
| [`src/test/java`](../src/test/java) | Executable regression tests and fake API classes used by fixtures. These are not gameplay classes. |
| [`src/main/resources/fan4compat.mixins.json`](../src/main/resources/fan4compat.mixins.json) | Authoritative inventory of generated common and client mixins. |
| [`src/main/resources/fabric.mod.json`](../src/main/resources/fabric.mod.json) | Mod identity, Java/Minecraft requirements, source URL, entrypoints and bundled library. |
| `src/main/resources/assets/<namespace>/lang` | Translations supplied for Fan4Compat, DWM and Eureka. |
| `build/generated/classes` | Generated mixin bytecode. Recreated by the build; do not edit as source. |
| `build/generated/client-intermediate` | Build-time intermediate client mixins used by generators. |
| `build/libs` | Finished standalone jar. |
| [`docs`](.) | Feature explanations, release notes, investigation history and validation status. |

The public mod ID remains `fan4compat`. Java package names, translation namespaces,
config filenames and saved-data keys have separate responsibilities. Moving a
Java file does not automatically migrate the other three.

### Build-time generation

[`GenerateAddon`](../tools/shared/GenerateAddon.java) calls each integration's
ASM generator. Generators emit classes under
`dev.fan4.compat.mixin.<integration>.common` or `.client`. They embed exact native
method descriptors and helper calls; most use `remap=false` and Minecraft's
intermediary class/member names, such as `net.minecraft.class_2338` for BlockPos.
There is no conventional source file for each generated mixin.

The Gradle project uses the Java plugin, not Fabric Loom for normal addon
compilation. Runtime helpers use reflection so optional mod APIs do not become
unconditional class links. Generators encode the native links only in gated
mixins. The build compiles Java 17 bytecode; Minecraft's declared runtime is Java
21+. CI uses JDK 21. Native checks can consume externally supplied mapped jars,
but those jars are not embedded in the addon.

### Startup and Mixin selection

1. Fabric reads `fabric.mod.json` and `fan4compat.mixins.json`.
2. [`CompatPlugin`](../src/main/java/dev/fan4/compat/CompatPlugin.java) captures mod
   metadata, warns about unsupported pinned versions and applies
   [`CompatibilityRules`](../src/main/java/dev/fan4/compat/shared/CompatibilityRules.java).
3. Client-only hooks are listed in the Mixin config's `client` array; common hooks
   are in `mixins`. A common hook may contain a client branch, but must avoid
   loading client-only types when it runs on a dedicated server.
4. MixinSquared cancels two conflicting VS mixins only with supported VS and IP:
   the duplicate frustum fix and tracked-entity hook. VS retains them without IP.
5. Fabric's main entrypoints initialize Eureka logging settings and the updater.

An unsupported optional version skips its gated hooks rather than stopping the
entire addon. Once a hook is selected, required injection counts remain strict;
a missing native target must not be silently treated as successful compatibility.

### Shared utilities

`CompatCalls` resolves optional classes, fields, constructors and calls, caching
reflection/MethodHandle lookups. It distinguishes genuine overloads from VS's
unflagged covariant getter adapters and selects the most specific compatible
return type for identical argument signatures. `exact` and `callTyped` resolve
specific signatures without linking unrelated client APIs on a server. These
choices matter for both startup and world-save safety.

`ShipPose` transforms positions and directions using copied 4x4 matrices. A
position includes translation; a direction does not. `WeakIdentityMap` keys
objects by identity while permitting garbage collection. This avoids stateful
portal/renderer objects colliding through value equality. Other helpers use weak
maps, scoped ThreadLocals or explicit disconnect cleanup as appropriate; inspect
the particular helper rather than assuming every cache has identical ownership.

## Which integrations activate

Reference metadata versions live in `CompatibilityRules.VERSIONS`. DWM's entry
is now an inclusive minimum, compared using Fabric's semantic version parser:
1.0.38.4 and newer (including 1.0.39) activate its hooks. Older, malformed and
below-minimum prerelease versions do not. Build metadata does not affect ordering.
Other entries remain exact pins. Acceptance enables the existing hooks; native
API compatibility still requires validation when DWM changes its internals. Filenames are not
reliable versions: the supplied IP file says 6.0.7 but its metadata is 6.0.6, and
the supplied VS/Eureka files have different marketing and metadata versions.

| Mod ID | Metadata requirement |
| --- | --- |
| `valkyrienskies` | `2.4.12-td.9+66a13242ed` |
| `immersive_portals` | `6.0.6` |
| `dwm` | `>=1.0.38.4` (inclusive minimum) |
| `vs_eureka` | `1.5.3-beta.4-td.4+b0d9511582` |
| `distanthorizons` | `3.3.3` |
| `iris` | `1.8.1+mc1.21.1` |
| `freecam` | `1.3.0+mc1.21` |
| `pointblank` | `2.2.0` |
| `accessories` | `1.1.0-beta.48+1.21.1` |
| `graves` | `1.0.0` |
| `sound_physics_remastered` | `1.21.1-1.5.1` |
| `bclib` | `30.4.0` |
| `jade` | `15.10.6+fabric` |
| `elytratrails` | `1.4.7.5-1.21.1` |

Trinkets' grave helper separately checks `3.10.0`. Aether is presence-gated for
the grave and Aether portal hooks. Sable checks the presence and identity of its
default companion class instead of adding a general Sable version pin. The
optional-recipe filter checks installed recipe dependencies individually.

| Feature family | Activation rule |
| --- | --- |
| DWM model caching | Supported DWM alone. |
| TARDIS ship coordinates, landing, flight and waypoints | Supported DWM + VS; if IP is installed it must be supported. |
| TARDIS door portal data, geometry and shell clipping | Supported DWM + VS + IP. |
| Ship/IP networking, motion and chunk bridges | Supported VS + IP; DWM is not required for every bridge. |
| VS dimension queue | Supported VS alone. |
| Sable default-companion fix | Supported VS + default companion class present. |
| DH dynamic-level initialization | Supported DH alone. |
| DH portal draws | Supported DH + IP. |
| Iris DH portal frame/depth guard | Supported DH + IP + Iris. |
| Other Iris shader/copy/depth hooks | Supported IP + Iris. |
| Freecam tick/native LOD clipping | Supported Freecam + DH; BSL shader hooks additionally need Iris. |
| Point Blank animation, draw, scope and diagnostics | Supported Point Blank; IP cache bridge additionally needs supported IP. |
| Accessory entity packets | Supported Accessories + IP. |
| Equipment in graves | Supported Graves + Accessories + Aether present; optional supported Trinkets is handled inside that integration. |
| Sound snapshot and portal-only ray bridge | Supported IP + Sound Physics; the portal-only ray bridge is selected when VS is absent. |
| Ship acoustic ray/snapshot bridge | Supported VS + Sound Physics. |
| Jade ship rays | Supported Jade + VS; helm overlay additionally needs Eureka. |
| Eureka warning suppression | Supported Eureka + IP. |
| Eureka debug control | Supported Eureka. |
| Nether portal creation on ships | Supported VS; IP activation hooks additionally need IP. |
| Aether portal creation on ships | Supported VS + Aether present. |
| BCLib stack codec/recipe format | Supported BCLib. |
| Optional recipe dependency filter | BCLib or one of JEED/Supplementaries/Amendments/Moonlight installed. |
| Wing-tip fallback | Supported Elytra Contrails. |
| Updater/settings entrypoints | Always initialized; updater can be disabled and requires an eligible installed jar to operate. |

Treat this table as an explanation of the gate code, not an invitation to enable
hooks for other versions without inspecting their native targets.

## Coordinate systems and ownership

VS stores ship blocks in remote **shipyard coordinates**. A ship transform places
those blocks in world space. The ship's visible overworld position changes while
its stored block addresses remain stable.

Fan4Compat keeps block lookup, saved exterior addresses, slot targets and ship
waypoint anchors in ship-local space. It converts to world space for player-facing
coordinates, distances, portal geometry and acoustic hit positions. Directions
and facing need their own rotation/scale treatment; a translated block position
alone cannot describe a rotating doorway.

For example, a TARDIS stays on the same deck block while its console displays the
ship's moving overworld position. Replacing the saved exterior block with that
display coordinate would break DWM's ability to find its shell in the shipyard.
The same distinction explains why fixing reach checks must not rewrite the block
address used for breaking or placing blocks.

State has an owner: DWM owns flight and access rules, VS owns ship identity and
transforms, IP owns portal worlds and chunk watches, and the equipment/grave mods
own item validity and retrieval rules. Keep those boundaries when extending a fix.

## TARDIS on ships

Sources: [`doctorwho` helpers](../src/main/java/dev/fan4/compat/doctorwho),
[`TardisBridgeGenerator`](../tools/doctorwho/TardisBridgeGenerator.java) and
[`TardisQolGenerator`](../tools/doctorwho/TardisQolGenerator.java).

### Console, levers and position history

`TardisShipCompat` retains DWM's previous/current/destination exterior addresses
and adds transformed display position/facing data to its serialization. Current
position follows the moving ship. A previous/destination slot that aliases the
current exterior location freezes its display position rather than making every
console coordinate march with the ship. A distinct local destination can follow
its own ship location. Facing is still derived from the current transform.

`TardisShipQol.leverPosition` supplies real-world destination coordinates to the
coordinate controls. The sonic message also shows transformed coordinates with
`(On ship)` while the landing target remains local. Detached states with no world
reuse saved display data instead of attempting client/world lookups during saves.

### Recall and coordinate landing

A key recall tries to identify the supporting ship deck beneath the player. A
direct local support-block check supplements raycasts that miss the ship. It
stores a fixed local block, dimension and persistent ship ID so the target follows
that ship throughout the flight and survives a world save.

World-coordinate vertical scans compare ship surfaces with DWM's terrain result.
Candidate surfaces are transformed into local addresses and validated with DWM's
native support, clearance, facing and build-height rules. The selected vertical
scan direction determines which candidate is preferred; Top scanning can choose
a ship above ordinary terrain. Already-local recalls keep the local landing path.

For a local ship target, `scanning` substitutes DIRECT for that lookup only. It
never changes the player's saved console scanning choice. A deleted/replaced
ship or an obstructed fixed target must fail the ship recall rather than silently
landing on ground beneath it. Recall requires the relevant ship to be available;
the addon does not supply a general remote unloaded-ship discovery service.

### Moving destinations and flight duration

Base flight distance is measured in world space rather than across the large
shipyard coordinates. Ship trips use normal dematerialization/rematerialization
because DWM's flyover animation assumes stationary world blocks; the user's
flyover lever choice remains available for ordinary trips.

`TardisShipQol` snapshots departure position and estimates ticks per block from
DWM's existing duration and overhead. While a ship destination moves farther
away during PROCESSING, it extends both the countdown and duration. Required
duration only increases; it does not shorten a flight when the ship approaches.
The chase record is saved and validated on reload. This is a distance-based
extension of DWM's flight timing, not a physical interception/autopilot solver.

### Door geometry, shell collision and keys

DWM supplies paired exterior/interior portals. `updatePortals` attaches their
ship-side endpoint to a local doorway position and axes, transforms them to world
space and synchronizes changed geometry. New portals disable IP's default position
animation so they do not travel visually from shipyard space to the deck. Unchanged
server geometry avoids redundant synchronization. Client motion is described in
the following section.

An open, teleportable ship-mounted TARDIS doorway cuts only the door-facing front
portion of the exterior collision/outline shape before VS builds its collision
polygons. The sides, back, neighboring blocks and deck remain solid. Closed or
ineligible doors retain the original shape. This fixes the invisible shell wall
that spectator/cloaking could previously bypass; it is not general noclip.

Key opening range uses the exterior's transformed world position with DWM's normal
ten-block range. The exterior's stored address stays local.

### Ship waypoints

`ShipWaypoint` is an addon record encoded in the native DWM waypoint ID:
`fan4compat_ship:<ship-id>:<original-waypoint-id>`. DWM's existing record still
stores dimension, local block, facing, name and timestamp; its native codec is
preserved. This is not a replacement registry of all waypoints.

Creation snapshots the local anchor from the console's nested `tardisTag`, so a
ship moving while the naming screen is open does not turn the waypoint into a
plain overworld point. Server handling validates the ship, position, access and
flight state. Selection resolves the server's stored entry, checks normal access
and flight eligibility, and restores the local ship destination. Successful
selection sends DWM's ordinary **Waypoint loaded** message.

Ship waypoint coordinate fields are hidden and updating the waypoint is rejected.
Deleting it still works under native access rules. Screen hooks change widget
visibility instead of recursively changing text through DWM's callbacks.

A waypoint binds to the original VS ID. Disassembly/reassembly can create a new
ID; proximity does not rebind the old waypoint. Server console snapshots check
all registered ship data, including unloaded ships. Missing IDs show crossed-out
names and **This ship has been deleted or reassembled** where coordinates would
normally appear. Reopening/refreshing the screen obtains fresh status. An
unavailable ship cannot be loaded as a destination; save a new waypoint for the
reassembled ship. Ordinary world waypoints are not automatically migrated.

### Model cache and biome names

`ModelBakeCompat` reuses baked roots per renderer owner and model layer. Before
reuse it resets part transforms and restores original visibility/hidden flags;
a changed model-loader generation clears the cache after resource reload. This
reduces repeated model baking without sharing mutable roots across unrelated
renderers. It is independent of ships, IP and Point Blank.

DWM language resources supply both biome key forms used for the TARDIS biome.
They change the displayed name, not biome/world generation.

## Immersive Portals and ship lifecycle

Sources: [`immersiveportals` helpers](../src/main/java/dev/fan4/compat/immersiveportals)
and [generators](../tools/immersiveportals). Several bugs look like collision
problems but are caused by ship data/chunk delivery or stale motion state.

### Picking, mining and placement

Client bridge hooks preserve local ship block hits while applying ship-aware
world distances to interaction reach checks and the portal-versus-block choice.
`PortalInteractionCompat` temporarily clears VS's source-world original-crosshair
override around IP's remote placement, then restores it in cleanup. This lets the
placement use the destination hit rather than accidentally targeting the source
ship. It does not globally replace the crosshair or grant extra reach.

### Client chunk caches and render notifications

IP replaces Minecraft's client chunk manager. The addon restores VS ship-chunk
connectivity initialization and Sodium load notifications on that replacement
path. Sodium is notified in the chunk's own client world, including remote portal
worlds. Connectivity work is limited to the active player world because VS's
native queue drains against that world.

A generated duck-interface bridge supplies the VS chunk-cache methods IP's manager
otherwise lacks. Ship metadata unloads respect IP's existing per-world cache and
normal unload notifications rather than deleting unrelated worlds' chunks.

### Transfer and packet cleanup

`ShipTransitCompat` clears VS attachment, dragging/interpolation and queued
position state when IP changes a player's dimension. It tracks actual ship
acknowledgements separately from locally known IDs and resends after client world
changes. Native player creation can populate known IDs without sending an ACK.

Server ship-motion packets are rejected while a teleport acknowledgement is
pending or when their ship belongs to a different dimension. This prevents a late
source-world packet from moving the player back after arrival. `PositionPacketCompat`
stamps missing IP dimension metadata while the authoritative sender is available,
and clears old awaiting-position correction state after a committed transfer.

### Following a moving doorway without launching the player

`PortalMotionCompat.Attachment` describes which portal side belongs to a ship,
its local position/axes and its size. The server persists this descriptor; the
client resolves it against synchronized VS transforms.

Before IP samples crossing geometry, the client builds previous/current/interpolated
portal states from VS's tick transforms and supplies consecutive frame history.
After VS updates render transforms, portal drawing is reconciled with the exact
ship render transform. Both drawing and crossing therefore follow the same ship
pose. Genuine IP animation drivers and custom ship transform providers are left
alone. Removed/detached/unloaded cases clear the addon's history.

For the local player on these attached doorways, IP's portal-point velocity is
zeroed because VS already supplies deck movement. After crossing, a five-tick
IP teleport cooldown lets native dragging settle and prevents immediate reverse
crossings. Ship arrival seeds the dragging ship ID and avoids applying a second
boarding impulse. This replaces the older approach that flung players forward to
prevent recrossing; it does not add a launch velocity to get them out of the door.

### Watch authorization and dimension-correct delivery

`PortalShipWatchCompat` scopes ship/chunk checks to the relevant VS watch operation.
A valid requested IP watch can qualify the player for initial ship tracking even
from another dimension. Delivered packet checks still use IP's normal
`isPlayerWatchingChunk` path. Requested and delivered watches are deliberately
different; synthetic VS observers do not acquire privileges through this bridge.

`ShipChunkPackets` uses IP packet redirection with the chunk's owning server world.
It permits the dimension exception only for a genuine player with the relevant
watch. A VS unwatch must not drop a client chunk still owned by a delivered IP
watch. Scoped state is restored in `finally`, including nested calls and failures.

### Loading required for exits and movement

`DoorwayShipLoading` supplies loaders to IP's existing cadence for nearby ships
and eligible TARDIS ship-side portal anchors. It includes empty active shipyard
chunks outside the visible hull; VS's native movement guard needs those too.
Nearby-player searches use a 48-block box; portal anchors require a valid,
teleportable portal within 64 blocks and matching ship/destination dimension.
The helper enumerates the selected ship's active chunks, not only visible blocks.

`ShipLoadRecovery` repairs native acknowledgement/delivery timing when the local
player is blocked. Client attempts are limited to eight, spaced by two seconds,
and restricted to nearby loaded ships. Early authenticated server ACKs are
retained only for IDs already registered by the server, with a ten-second expiry
and bounded pending sets. Server repairs requeue valid, already delivered nearby
IP watches through normal batching; invalid, pending or other-player watches do
not authorize a repair.

These helpers do not disable collisions or VS's unloaded-ship guard. Removing
them because ship preview rendering was abandoned can restore the movement
freeze or TARDIS exit timeout. Transit loading is still needed even though a ship
may remain invisible through a freshly reopened door until the player exits.

## Dimension lifecycle and Sable

`DimensionQueue` fixes VS physics-stage errors when DimLib/DWM register, update
or remove dynamic dimensions outside VS's allowed mutation window. Startup
registration remains synchronous. After physics ticks begin, out-of-phase calls
are queued per world, in order. The queue opens and flushes after `preTick`, closes
before `POST_TICK_START`, and is forgotten at shutdown. Exceptions from the
original operations propagate; the fix does not disable VS consistency checks.

The DH dynamic-dimension hook replaces destination/source `getLevel` calls in
its player-level change with the native `getOrLoadLevel` path. It reuses existing
DH levels or initializes dynamic worlds missed at initial registration. It does
not change player inventories or silence genuine level-loading failures.

`SableGenerator` handles the default Sable Companion bundled by another mod. That
fallback asks VS about its plots; feeding it back into VS's Sable exclusion check
recurses. When the companion is exactly the default implementation, VS does not
classify its own ship chunks as independent Sable sublevels. Genuine non-default
Sable implementations keep their normal path.

## Distant Horizons, Iris and Freecam

Sources: [DH generators](../tools/distanthorizons),
[`iris` helpers](../src/main/java/dev/fan4/compat/iris),
[`freecam` helpers](../src/main/java/dev/fan4/compat/freecam).

### Wrong-world LODs through portals

DH maintains rendering data for the main view. During a nested IP portal view,
reusing it can draw the source dimension behind the destination, especially when
Iris/BSL samples stale DH depth textures.

The DH hook cancels LOD terrain and opaque/transparent terrain-fade draws only
while `PortalRendering.isRendering()` is true. Iris's DH frame check returns false
in that scope. Its DH depth getters provide a complete far-depth texture instead
of the main view's stale depth. `PortalDepthTexture` allocates a complete one-pixel
depth texture initialized to 1.0 (no terrain hit), restores its binding and cleans
up failed allocation; the hook deletes it when the pipeline clears. Normal main-view DH rendering continues.

This is suppression, not destination LOD rendering. It does not lower IP's chunk
cap to five, stop background generation, or fix slow first-time server LOD delivery.
Destination views currently use normal Minecraft terrain distance. The pack owner
confirmed the BSL portal-background fix, while noting a slower initial LOD load.

### Shader compilation housekeeping

`ShaderCompileCompat` clears IP's shader program type/name ThreadLocals even if
Iris compilation fails. If compiled shader source uses `dhFarPlane` without a
float declaration or macro, it inserts `uniform float dhFarPlane;` after version/
extension directives. Iris already binds that live uniform. Existing declarations,
comment-only mentions and unrelated shaders are preserved. This is a narrow
source repair, not a general shader-pack conversion.

### macOS copy and recursive depth compatibility

`FramebufferCopyCompat` checks the availability of `glCopyImageSubData`. When the
context lacks it, the portal copy uses OpenGL 3 framebuffer blits for color and
depth/stencil textures. It validates temporary framebuffer completeness, restores
read/draw bindings and scissor state, and deletes temporary objects in cleanup.
Capable GPUs retain the original copy path.

`DepthFormatCompat` additionally handles Apple-reported GL vendors. It inspects
the actual main depth texture representation, including depth bits/component type
for an unsized format, and chooses IP's corresponding recursive stencil format.
It restores the texture binding and recreates matching-count layer buffers if the
representation changes. The native error-triggered compatibility fallback stays
active. These fixes do not guarantee portal-in-portal support on every Mac; GPU
confirmation of the recursive path remains separate from bytecode/fixture checks.
An AMD GPU on Windows does not use the same Apple OpenGL implementation.

### Freecam beyond full chunks and nearby LOD holes

`FreecamTickCompat` relaxes one loaded-terrain tick condition only for the active
Freecam camera. The real player still obeys the original condition. This enables
camera movement beyond full Minecraft chunks without requesting more chunks,
changing ship collision guards or making LOD terrain interactable.

When that active camera is in an unloaded full-terrain chunk, native DH near-plane
clipping is capped at half a block and its near-clip uniform is zero. Normal
player views, loaded camera chunks and portal views retain native behavior.

BSL 10.1.5 also has its own DH terrain/water overlap discard. `FreecamShaderCompat`
recognizes that specific source pattern and adds a uniform to bypass the discard
only for the active unloaded camera. Ambiguous/nonmatching sources are unchanged;
shader-pack files are not edited on disk. The uniform is updated for program use.
This BSL source fix is not a claim of support for every shader pack. The reported
BSL-only Freecam hole still needs independent gameplay confirmation.

## Point Blank

Sources: [`pointblank` helpers](../src/main/java/dev/fan4/compat/pointblank) and
[generators/checks](../tools/pointblank).

### Stale animation and offhand state

Animation callbacks on empty/non-gun stacks return no controllers, preventing the
AirBlockItem-to-GunItem cast crash. Draw transitions reject a stack that does not
match the cached gun or whose native fire mode is unresolved. Gun type and saved
weapon UUID must agree. Valid animations, ammo and fire-mode data stay native.

`PointBlankDrawCompat` routes the client-tick draw call to Point Blank's operable
gun context, including an offhand gun with a TARDIS key in the main hand. It does
not treat the key as a gun or invent a fallback fire mode.

### Scope and portal stencil state

The stencil buffer is a per-pixel mask used to limit drawing to selected areas,
such as a portal opening or scope lens. Its comparison and write settings must
agree with the renderer's assumptions.

The beta 20 log exposed actual/cache disagreement in stencil function/reference
and write mask while solid terrain disappeared with shaders off. Both Point
Blank scope callbacks and IP's normal stencil renderer contain raw stencil setters
that bypass Minecraft's cache. A later cached setter can then skip a needed GL call.

`StencilCompat` routes scope stencil function/mask/operation changes through
RenderSystem. `PortalStencilCacheMixin` does the same for IP's normal stencil
renderer, only with both supported mods installed. `GunStencilMixin` is an
interface mixin because the native scope callbacks are static interface lambdas;
`GunStencilClearMixin` also guards Point Blank's preparatory clear.

While IP renders a nested portal view, scope callbacks leave its stencil function,
mask, operations, clear value and test state alone. Clears remove only the stencil
bit; other clear bits survive. IP's own setters always run through the cache and
are not suppressed. Ordinary scope draws keep their operations. Scope-specific
stencil masking is suppressed in portal views to preserve portal ownership, so
scope visuals in those views deserve a regression check.

The pack owner confirmed that beta 21 stopped the terrain from disappearing.
Portal regression testing was unavailable and accepted as unverified; do not
reinterpret that as a measured confirmation on Windows, Mac or every shader path.

### Temporary diagnostics

`RenderDiagnostics` and four `*DiagnosticMixin` wrappers observe world rendering,
gun preparation, item rendering and scope-world rendering. Reports include actual
GL and cached stencil/depth/color state, framebuffer attachments, viewport and
shader status. They do not repair state or consume `glGetError`.

Sampling is at most once per second per stage, identical reports are deduplicated,
and reporting stops after 96 reports per launch. A snapshot failure disables the
probe instead of replacing the native render failure. Reports are tagged
`[Fan4Compat RenderDiag]`. Differences are clues, not automatic proof of a bug:
`-1` and `255` masks can be effectively equivalent on an eight-bit stencil buffer.

Diagnostics are off by default; JVM argument `-Dfan4compat.renderDiagnostics=true` installs these observation mixins
on startup. The animation/draw/stencil fixes are always enabled. These diagnostics
are temporary investigation code and may be removed after adequate confirmation.

## Accessory and Trinkets graves

Sources: [`graves` helpers](../src/main/java/dev/fan4/compat/graves) and
[`GravesGenerator`](../tools/graves/GravesGenerator.java).

### Capturing items without duplication or premature removal

`AccessoryGraveCompat` consumes Accessories' already resolved death-drop queue,
including functional and cosmetic slots. It does not independently decide to drop
all live equipped items. Native KEEP/DESTROY decisions remain authoritative. The
empty-inventory check includes graveable equipment so equipment-only deaths can
still create a grave.

Copied stacks are staged in the grave. The original queue/Trinkets equipment is
cleared only after native grave spawning succeeds. Spawn failure leaves the old
drop path available. Storage is bounded at 128 stacks; overflow follows the item
drop fallback after success. Ownership/access remain Player Graves' rules.

### Storage, screen and original slots

The native player's first 41 inventory positions are preserved. The grave has
128 stored positions but a six-row, 54-slot visible window. Reopening compacts
remaining hidden stacks into empty visible slots. GUI synchronization preserves
the hidden backlog, and quick retrieval processes it without requiring repeated
screen openings. Native item serialization retains the expanded contents.

Resolved source slot name/index/cosmetic type is stored separately beside each
grave slot. Crouch-right-click with `restoreExactInventoryLayout` enabled first
tries the original equipment slot. It must still exist, be empty, accept the item
and satisfy stack limits. Accessories uses its native menu slot validation/setter;
Trinkets uses its own insertion rules. Occupied/removed/invalid slots fall back
to normal inventory retrieval without replacing equipped items.

Counts, XP/locator cleanup and smart despawn include directly restored equipment.
The grave's overflow retrieval setting determines whether inventory leftovers may
be dropped; an overflow drop is removed from storage only when native spawning
succeeds. Old graves without slot metadata recover to ordinary inventory.

### Why Trinkets needs an additional pass

Player Graves cancels Player.dropInventory after creating a grave, bypassing
Trinkets' tail drop hook. `TrinketGraveCompat` mirrors the supported Trinkets API
sequence: item rule, drop callback, slot rule, then DEFAULT resolution. Explicit
KEEP remains equipped; DESTROY is never stored and is cleared only on successful
grave creation. Default keep-inventory behavior follows the fact that Graves
already elected to grave this death; it is not a blanket override of explicit KEEP.

The helper records group/name and slot index for original-slot restoration, and
uses the existing grave hooks rather than adding another Trinkets injector family.
Absent/unsupported Trinkets is skipped; API collection failure logs and returns
no entries. This support currently lives under the Aether + Accessories + Graves
gate, not as an independent Graves/Trinkets-only integration.

### Load-time hazards already fixed

The grave constructor and retrieval methods require precise injector signatures.
`quickRetrieveAll` returns a value and needs CallbackInfoReturnable, not CallbackInfo.
Static lambda targets require compatible handlers. Screen-size constant handlers
must be static because a constructor constant can appear before `super()` and
before `this` is initialized. Beta 13–18 included grave GUI/startup faults fixed
by later builds; do not copy their generators as a working implementation.

Keep the addon installed until extended graves are emptied. Enlarged storage and
slot metadata are persistent behavior, not a purely cosmetic inventory screen.

## Other compatibility features

### Accessories entity synchronization through portals

`PortalEntitySyncCompat` resolves server-global entity IDs across IP's client
worlds rather than assuming the active world contains them. Packets arriving
before their entity are queued in order per entity, bounded to 256 entries and
100 client ticks. Expiry/overflow removes the affected entity's queued group;
player/connection changes clear the queue. Replay is guarded against requeueing.
This avoids applying incomplete or out-of-order equipment state to portal-watched
entities. It does not create missing entities or bypass packet ownership.

### Sound Physics snapshots and ship rays

`PortalSoundCompat` holds one published cloned-world snapshot for each sound
evaluation via a scoped ThreadLocal. Missing snapshots use the native default
sound environment. Nested scopes and exceptions restore prior state.

Without VS, acoustic rays are clipped to available cloned chunks and split into
segments of at most 256 blocks, returning the earliest hit. With VS,
`ShipSoundRaycast` captures terrain/ship chunk data and transforms during the
main-thread clone, then traces terrain and each ship in its own coordinate frame.
It converts hit positions/normals back to world space and chooses the closest
result. The audio evaluation reads the snapshot rather than live moving world
state. Only already available chunk data is cloned; this is not an audio-driven
world generation or chunk-loading service. Uncached regions are not fabricated
as solid walls. `AcousticBounds` supplies finite clipping bounds.

### BCLib/BetterEnd stack formats and optional recipes

`RecipeDataCompat` repairs supported BCLib/BetterEnd custom recipe result objects
using `id` where the old parser expects `item`, scoped to alloying, anvil and
infusion recipes. Existing counts and component-patch `nbt` content remain.
`StackCodecCompat` separately wraps BCLib's MapLike decoder so a missing `item`
lookup can fall back to `id`, including non-recipe decoding. It does not globally
change Minecraft's ItemStack codec.

The optional recipe filter removes specific JEED effect-provider entries if JEED
is absent and the copper-lantern conversion referencing `suppsquared` if that mod
is absent. A resource namespace does not prove the owning mod is installed;
another resource pack can contribute it. The filter is deliberately specific,
not a rule to delete every recipe mentioning an optional mod.

### Jade targeting and helm overlay

`ShipRaycastCompat` normalizes hit, eye and camera vectors into world space for
Jade's rerays and distance comparisons while retaining local block addresses for
actual overlay lookup. Cached hits outside the configured targeting range are
ignored, including stale hits after dimension changes. Eye/camera perspective
and range are respected; the global crosshair is not overwritten.

`HelmOverlayCompat` hides Jade only while the player rides a VS mounting entity
that is an active helm rather than a passenger seat. Dismounting restores normal
behavior; no global Jade setting is changed.

### Eureka translations, warnings and debug logging

The Eureka language resource names its creative group **Eureka! Airships**.
Supported Eureka/IP warning hooks remove only Eureka's entry from IP's normal
and severe incompatibility notices, on client/server. Create/DH and other warnings
are not globally hidden; removing a warning is not proof all integrations work.

`CompatSettings` disables the two targeted unconditional Eureka ship debug INFO
calls by default. Other log channels remain. The property file and JVM override
are documented below.

### Elytra Contrails fallback

`WingTrailCompat` supplies vanilla-sized wing-tip emitters only when a visible,
gliding player has no native samples for that frame, including custom/accessory
elytra render paths. It skips shadow passes, reuses a vanilla elytra model, uses
the rendered pose and returns world-space samples. Matrix pushes/pops are paired
in cleanup. Existing samples and trail settings remain native; non-vanilla wing
shapes get approximate emitter positions, not a geometry-specific implementation.

### Preventing portal construction on ships

`ShipPortalCreation.blocked` detects shipyard block addresses through VS. Vanilla
Nether, Aether and IP portal activation hooks reject frames located on a ship,
including unloaded shipyard addresses. Ordinary terrain frames retain their
native behavior. This prevents constructing these portals on ships; it does not
transfer ships, disable every existing portal near a ship or add a global portal ban.

## Automatic updates and releases

Sources: [`updater`](../src/main/java/dev/fan4/compat/updater),
[hosting details](automatic-updates.md) and
[build workflow](../.github/workflows/build.yml).

`UpdateInitializer` checks public GitHub releases from
`Livinglive234/fan4compat` on startup and every 30 minutes, off the game thread.
Only an eligible regular Fan4Compat jar directly inside `mods` is updated;
development installs, ambiguous origins and symlink targets are excluded.

`ReleaseUpdates` selects a newer recognized version, skipping drafts, malformed
versions and disallowed prereleases. Recognized suffixes are alpha, beta and rc;
for the same numeric version their order is alpha < beta < rc < stable. It requires
the expected jar, repository release URLs, a valid SHA-256 from GitHub's asset
digest or a checksum asset, and compatible mod ID/Minecraft metadata. Downloads are staged outside `mods`.

`UpdateInstaller` is extracted into its own JDK-only helper jar. On graceful
shutdown it waits for the game/server process to end, rechecks old/new hashes,
backs up the original and atomically replaces the original jar path. It refuses
manual changes, corrupted downloads and unsafe targets. Failure keeps the current
jar/pending update; there is no non-atomic delete-first fallback. Nothing is hot
loaded and the server does not restart itself. Client notices are posted on the
client thread when a player is available.

The workflow runs a clean build and uploads Actions artifacts. Successful
versioned main pushes/manual runs create **draft releases** with the jar and
checksum, unless that release already exists. PR builds only create artifacts.
Existing release assets are immutable in this workflow. A maintainer deliberately
publishes a tested draft; the updater cannot obtain drafts or Actions artifacts,
even with prereleases enabled. Documentation commits at the same version do not
overwrite an existing release or promote it.

## Saved data, settings and cleanup

### Data contracts

These are additive keys inside owning mods' native data, not a separate world DB.
They are compatibility contracts: renaming Java packages does not migrate them.

| Key/prefix | Purpose |
| --- | --- |
| `fan4compatWorld_<prev/curr/dest>Position`, `...Facing` | Transformed TARDIS display snapshots. |
| `fan4compatWorld_<prev/dest>FrozenRaw`, `...FrozenDimension` | Identity for frozen non-current display slots. |
| `fan4compatCurrentShip`, `fan4compatCurrentRaw`, `fan4compatCurrentFacing` | Current local ship anchor used by waypoint creation. |
| `fan4compatRecallShip`, `fan4compatRecallPosition`, `fan4compatRecallDimension` | Pending recall identity/local target. |
| `fan4compatExteriorBlock`, `fan4compatExteriorShip` | Exterior portal's local shell identity. |
| `fan4compatShipDoorPose` | Encoded local portal attachment: ship, endpoint side, position, axes and size. |
| `fan4compatChase` | Flight origin, timing parameters, required duration and target ship ID. |
| `fan4compatDeletedShipWaypoints` | Server-provided missing-ID status for waypoint UI. |
| `fan4compat_ship:` waypoint ID prefix | Persistent binding to original ship identity plus native waypoint ID. |
| `Fan4CompatAccessorySlots` | Grave-slot origin metadata, including cosmetic and Trinkets flags. |

Malformed descriptor/metadata inputs are handled according to the specific
reader; do not assume unknown data should be wiped. Portal pose decoding checks
finite values, positive sizes and orthogonal axes. Flight decoding checks timing
and finite coordinates. Invalid grave slot metadata falls back to ordinary item
recovery while leaving item stacks intact. Detached TARDIS serialization must
not treat unavailable world context as a deleted ship.

### Configuration

| Setting | Default | Effect |
| --- | --- | --- |
| `config/fan4compat.properties`: `eurekaDebugLogging` | `false` | Enable the targeted Eureka debug INFO calls after restart. |
| JVM `fan4compat.shipDebug` | Unset | Overrides Eureka's file value. |
| `config/fan4compat-updater.properties`: `enabled` | `true` | Enable background release checks. |
| Same file: `autoDownload` | `true` | Stage verified updates; false gives availability notices only. |
| Same file: `allowPrereleases` | `true` | Allow published alpha/beta/rc releases; never drafts/artifacts. |
| JVM `fan4compat.renderDiagnostics` | `false` | Set true to install temporary Point Blank observation hooks at launch. |
| Graves' `restoreExactInventoryLayout` | Owned by Graves | Determines original equipment-slot recovery. |
| Graves' `dropOverflowOnQuickRetrieve` | Owned by Graves | Determines retrieval overflow drop behavior. |

Updater files are under the game directory's `.fan4compat-update`: helper,
staged jar, pending properties, installer log and hash-named backups. On hosts
that kill child processes, a pre-start hook can apply a pending update while the
game is stopped:

```sh
java -cp .fan4compat-update/installer.jar dev.fan4.compat.updater.UpdateInstaller --apply .fan4compat-update/pending.properties
```

The old `fan4compat.wwooBorder` option no longer does anything. Removed movement
observer settings should not be presented as active features merely because a
historical test/task or release note mentions them.

## Build and verification

Use JDK 21 for the documented development workflow. On Linux/macOS:

```sh
./gradlew clean build
```

On Windows use `gradlew.bat clean build`. Artifacts are in `build/libs`.
Always use a clean build when producing a new test/release artifact; generated
classes must not carry stale mixins from an earlier feature or package layout.

The build runs executable regression mains, not just annotation-driven unit
tests. [`build.gradle`](../build.gradle) is the task inventory and wiring source.
It includes generated-handler fixtures alongside ordinary helper tests.

| Check family | What it establishes |
| --- | --- |
| Optional gates/cancellation | Empty/reduced setups, supported versions, required combinations and selective VS cancellation. |
| Ship/TARDIS/motion/transition fixtures | Coordinate math, landing/waypoint rules, motion history, transfer cleanup, watch/packet ownership and bounded recovery. |
| Equipment fixtures | Item conservation, spawn failure, backlog, drop rules, origin persistence and original-slot fallbacks. |
| Rendering fixtures | GL-resource cleanup with fake backends, shader source selection, cached-state rules and generated handler execution. |
| Sound/recipe/codec fixtures | Scoped snapshots, bounded rays, frame conversion and narrowly selected data repairs. |
| Updater fixtures | Release/version policy, real file checks/replacement, backup/refusal behavior and standalone helper process waiting. |
| `verifyBytecode` | Stack/type validation of packaged helper and generated bytecode. |
| `verifyJar` | Configured classes/entrypoints/bundled library and archive integrity; no copied original VS classes. |
| `featureLinkAudit` | No unconfigured generated hooks or unreachable packaged classes from entrypoints/plugin/active mixins. Structural reachability, not proof of user value. |
| `portalLodPrototypeTest` | CPU-only proposal behavior; does not install a destination-LOD renderer. |

Some smoke tasks optionally inspect supplied native jars. Others, such as
`gravesNativeCheck` and `pointBlankRenderNativeCheck`, skip unless their required
properties are supplied. A normal CI pass does not imply every optional native
check ran. Use task-specific properties rather than casually adding broad
`minecraftJar`/`ipJar` flags that also activate unrelated checks.

Example native Point Blank check (substitute actual jar paths):

```sh
./gradlew clean build pointBlankRenderNativeCheck \
  -PpointBlankRenderJar=/path/to/pointblank.jar \
  -PpointBlankRenderMinecraftJar=/path/to/minecraft-intermediary.jar \
  -PpointBlankRenderPortalJar=/path/to/immersive-portals.jar
```

Example Graves check:

```sh
./gradlew clean build gravesNativeCheck \
  -PgravesJar=/path/to/graves.jar \
  -PgravesAccessoriesJar=/path/to/accessories.jar \
  -PgravesMinecraftJar=/path/to/minecraft-intermediary.jar \
  -PtrinketsJar=/path/to/trinkets.jar
```

Native checks validate selected methods, descriptors, modifiers, fields and
injector contracts. They do not replace actual Mixin loading, full modpack startup,
GPU rendering or dedicated-server playtests. Record automated and user-reported
results separately. Beta 21 terrain rendering is user-confirmed; its portal
regression check was accepted as unverified. See [release readiness](release-readiness.md).

## How to change this mod safely

1. Identify the exact installed metadata versions, concrete trigger and owning
   mod's native path. Logs can contain optional-class warnings without a fault;
   a generic compatibility warning does not reproduce invisible contraptions.
2. Read the relevant runtime helper, generator, gate, Mixin config and fixture
   together. Editing generated output alone will be lost at the next clean build.
3. Inspect real native signatures before adding an injector. Match static/instance
   behavior, interface/class kind, return type and CallbackInfo variant. Keep
   required match counts and test constructor handlers before `super()` carefully.
4. Preserve local/world coordinate distinctions, access rules, original operation
   calls, exceptions and cleanup. Bound retries/queues; do not remove a guard just
   because bypassing it hides a freeze. Do not mutate live worlds from audio threads.
5. Keep common/server code free of unconditional client links. Use exact/typed
   reflection when overload enumeration would load client-only parameter types.
6. Add the new hook to `fan4compat.mixins.json` and `CompatibilityRules`, including
   absent/unsupported combinations. Keep helper, generator and tests in the proper
   integration folders. A package move also needs embedded helper owners updated.
7. Run the relevant meaningful fixtures and native checks, then a clean build.
   Update this guide/README/PROGRESS and release notes to describe final behavior,
   including limitations and which validation actually ran.
8. For a changed runtime artifact, bump `build.gradle` and add
   `docs/releases/<version>.md`. Push for Actions/draft release, test, and publish
   deliberately. Never overwrite an existing release to distribute a new binary.

For an AI contributor, these are maintenance conventions, not authorization to
revive withdrawn features or publish drafts. Follow the current task's scope.

## Known limits and abandoned work

| Area | Actual status |
| --- | --- |
| Moving ship TARDIS landing/entry/exit, placement and waypoints | Pack-owner confirmations exist for core workflows; repeat affected cases after changes. |
| Ship appearance through freshly reopened TARDIS doors | Known limitation/wishlist. Transit-loading helpers remain necessary. No separate preview renderer is active. |
| Launching a player forward to avoid recrossing | Superseded by motion-frame correction/cooldown; do not reintroduce the fling. |
| Ship transfer between dimensions | Outside requested scope. Player transfer helpers must remain. |
| Destination DH LODs beyond five chunks through portals | Isolated CPU prototype in tools; no runtime renderer or five-chunk config write. |
| WWOO/DH grid seams | Border-generation experiment withdrawn; helper, hook, gate and dedicated experiment tests removed. No active seam fix. |
| Create/Flywheel/Iris | Investigation only. Active shaders intentionally disable inspected GPU backends; no forced backend or complete shader bridge. |
| Mac recursive portals | Capability/depth fixes implemented; full GPU confirmation remains pending and native fallback remains. |
| Freecam BSL LOD holes | Narrow native/BSL candidate fixes implemented; do not claim all shaders are handled. |
| Beta 21 Point Blank terrain | User-confirmed improvement; portal regression accepted as unverified. |
| ChestTracker persistence errors | Explicitly excluded from current patch scope. |
| Older Amendments/IronChest/BetterEnd resource warnings | Accepted low-priority candidates, not generalized loader fixes. |

Investigation references: [Create rendering](create-rendering.md),
[withdrawn WWOO experiment](wwoo-distant-horizons.md),
[destination LOD prototype](portal-destination-lods.md) and
[log follow-ups](log-followups.md). Historical warnings and older documents are
not proof a new patch is required in the current build.

## Troubleshooting map

| Symptom | Start here | Distinction to preserve |
| --- | --- | --- |
| TARDIS doorway acts like an invisible wall | DWM doorway shape/portal eligibility | Shell collision differs from missing chunks or teleport rejection. |
| Doors lag behind moving ship | PortalMotionCompat + DWM attachment synchronization | Render transform, tick transform and crossing history must agree. |
| Repeated entry/exit or fling off deck | Portal point velocity, dragging state and crossing cooldown | Fix frame/impulse duplication rather than launching the player. |
| Cannot move near a ship even in spectator | DoorwayShipLoading, ACK timing and ShipLoadRecovery | Native unloaded-ship guard is independent of ordinary collision. |
| Mining through portal works, placement fails | PortalInteractionCompat and remote reach bridge | Source crosshair override must not replace destination hit. |
| Top scan lands below floating ship | Coordinate scan candidate comparison/native validation | Requested world coordinate and chosen local target are different values. |
| Waypoint binds to old overworld point | Creation's nested tardisTag and ShipWaypoint encoding | Display coordinate cannot substitute for local anchor. |
| Waypoint unavailable after reassembly | Persistent ship ID and server status | A new ship ID requires a new waypoint. |
| Dimension-stage exception/save-time client class error | DimensionQueue, gate and exact reflection | Startup timing and dedicated-server class loading are separate concerns. |
| Source-world LOD terrain behind portal | DH draw/frame/depth guards | Main-view cached depth differs from valid destination rendering. |
| Freecam stops or nearby LODs vanish | Active-camera tick/coverage checks and BSL pattern | Camera movement, full chunks and shader clipping are separate layers. |
| Solid blocks vanish with XM3/shaders off | StencilCompat + RenderDiag reports | Cached GL setters may skip changes; every reported mask difference is not harmful. |
| Grave loses equipment or restores wrong slot | Native resolved drop queue, spawn result and slot metadata | Copy before committing; validate before equipping; keep overflow stored on failure. |
| Grave GUI fails to load | Generated injector return/static/constructor contracts | Fixture success alone does not prove Mixin startup safety. |
| Equipment packet cannot find entity | PortalEntitySyncCompat worlds/queue | Entity can belong to a portal world or arrive after its packet. |
| Long acoustic ray failures | Snapshot bounds and ShipSoundRaycast frame conversion | Audio uses available published data, not live world loading. |
| Update not installed | Updater config, published release eligibility, pending file/helper log | Drafts are deliberately ignored; host process policy may require pre-start installation. |

When reporting a result, include exact versions, client/server arrangement,
reproduction steps and whether shaders/portal views were involved. Keep confirmed
observations distinct from hypotheses so the next contributor can act on evidence.

### Beta 24 portal depth-copy observer

`iris/PortalDepthDiagnostics` and generated `IrisPortalDepthDiagnosticMixin`
observe the result of IP's existing `glGetError()` call after the depth blit in
`IrisPortalRenderer.doMainRenderings`. The same result returns to IP, including
errors that trigger compatibility mode. There are no additional error-queue reads
or GL binding changes. Read-only attachment queries report object identities,
depth/stencil bit sizes and component types for the still-bound source and
destination framebuffers. One success and at most eight failures are logged per
launch, using `[Fan4Compat PortalDepthDiag]`; snapshot/logging failures disable the
observer and preserve rendering. The startup flag
`-Dfan4compat.portalDepthDiagnostics=false` excludes this mixin while retaining
other fixes. It needs supported IP and Iris, without VS, DH or Point Blank.
The beta 24 Mac log reproduced the depth-copy failure and compatibility fallback.
See the maintained [Mac investigation](mac-nested-portals.md) for evidence and
beta 25 workaround status. Point Blank, Freecam/BSL and Graves have been confirmed
working by the pack owner on 2026-10-10; earlier pending notes are historical.

### Beta 25 Apple separate depth/stencil targets

`iris/SeparateStencilCompat` redirects IP's stencil enablement only inside
`IrisPortalRenderer.prepareRendering` through `IrisSeparateStencilMixin`, first
preserving the native call. For Apple and a DEPTH32F main texture, each portal
layer's existing texture gets DEPTH32F storage plus a separate STENCIL_INDEX8
renderbuffer. This avoids the depth-only to packed depth/stencil blit captured
in beta 24. It does not reallocate or replace the main framebuffer. Storage is
reused by framebuffer identity/texture/size. `IrisStencilCleanupMixin` observes
Minecraft framebuffer deletion to free owned stencil resources, including native
resize and shader reload. A rejected layout is restored to its native packed
texture/attachment, then skipped until recreation; GL bindings restore even on
allocation failure. IP's stencil clears and depth-copy error fallback are intact.
Diagnostics remain enabled; Mac gameplay verification is still required.

Detailed evidence, lifecycle contracts and ongoing test results are maintained in
[Mac nested portals with Iris](mac-nested-portals.md).

### Beta 26 Point Blank lazy-stencil framebuffer scope

`pointblank/FramebufferCompat` and generated `GunFramebufferMixin` /
`GunAuxFramebufferMixin` redirect the `RenderTargetExt.enablePointblankStencil`
calls in native gun preparation and auxiliary scope rendering. The native call
still executes. Read and draw framebuffer bindings are captured independently and
restored in finally; if a binding referred to the resized target, it resolves to
the new framebuffer ID. Failed recreation maps an invalid target ID to framebuffer
0. Other shader/portal target IDs retain their exact bindings. The helper requires
Point Blank only and does not depend on Iris, IP or diagnostics being enabled.
`GunFramebufferTest` reproduces the observed main-to-zero leak and verifies
replacement mapping, distinct targets and failures. Native checks cover both
redirects. This addresses a captured state leak, not a proven full Mac XM3 repair.

### Beta 27 deferred render diagnostics

`pointblank/RenderDiagnostics` now samples actual shared/default/Iris scope
setup/cleanup callbacks and the IP GUI-world camera boundary through generated
`GunScope*DiagnosticMixin`, `GunDefault*DiagnosticMixin`,
`GunIris*DiagnosticMixin` and `GunGuiDiagnosticMixin`. Static/interface wrappers
are emitted by `RenderDiagnosticsGenerator`; native checks verify real signatures
and finally paths. Context reports GUI camera, portal layer and dimension. The
opt-in flag remains unchanged; per-stage caps reserve the total 96-report budget
for multiple rendering paths. No rendering state is repaired by these observers.

Beta 28 extends `pointblank/StencilCompat` to reapply owned stencil functions
after cache updates, and adds `IrisScopeStencilMixin` for Iris glow/muzzle-flash
raw calls. Regression/native checks cover the cached-no-op mismatch and callback
bindings. Mac XM3/exterior-camera visual recovery still needs testing; retain
`-Dfan4compat.renderDiagnostics=true`. See the maintained Mac investigation.
