# Fan4Compat progress

Updated: 2026-10-07 (America/Chicago). Current build: **0.1.0-alpha.34**.

Fan4Compat is a standalone Fabric 1.21.1 compatibility addon. Original mod jars
remain unchanged. The project is experimental; a successful build does not
establish that portal traversal works correctly in the full modpack.

## Target versions

| Component | Target |
| --- | --- |
| Minecraft | Fabric 1.21.1; Java 21+ for the game |
| Doctor Who Mod | **1.0.38.4 only** for the TARDIS ship hooks |
| Valkyrien Skies | Original `2.4.12-td.9+66a13242ed`; supplied filename says v3.2.0 |
| Immersive Portals | Internal `6.0.6`; supplied filename says 6.0.7 |
| Eureka | `1.5.3-beta.4-td.4+b0d9511582` |
| Distant Horizons | Fabric `3.3.3` |
| Sable / Windchimes | Observed Sable Companion `1.6.0`, bundled in Windchimes `1.2.0+1.21.1` |

Install the latest Fan4Compat jar on client and server, replacing older addon
versions. Do not combine it with the earlier patched VS jars.

## Confirmed in game

- The original ship rendering and interaction fixes worked in the tested setup
  before standalone packaging. This does not validate every later addon change.
- TARDIS key recall lands on the ship deck with DWM 1.0.38.4.
- The TARDIS portal follows the moving exterior, with some occasional disappearance glitching
- Latest-build testing confirms moving-ship exits, waypoint UI, contrails and
  the TARDIS biome label working (pack owner report, 2026-10-07).

## Implemented bridges (validation varies by area)

| Area | Current implementation |
| --- | --- |
| VS / IP rendering and interaction | Cancel duplicate frustum and tracked-entity hooks only with IP installed; use ship-aware interaction distances. |
| Ship chunks in portal worlds | Implement VS's chunk-cache duck on IP's replacement cache, preserve per-world unloads, and notify Sodium in the chunk's own world. VS connectivity initialization remains active-world only. |
| Dynamic dimensions | Queue VS dimension changes outside its permitted mutation window and replay them after the next `preTick`. |
| Distant Horizons | Use its normal `getOrLoadLevel` path when a dynamically created destination missed initial registration. |
| Sable fallback | Prevent recursion between VS and the default Sable Companion plot check. |
| Eureka logging | Disable the two targeted debug INFO messages by default; configuration can enable them. |
| TARDIS addresses | Preserve ship-local block addresses for landing and persistence; transform console and waypoint displays into world coordinates. |
| Recall attachment | Keep the destination at a fixed deck block while the ship moves; reject a removed/replaced ship or blocked landing target. |
| Portal geometry | Transform both endpoints and exterior orientation; place portals before spawning and disable IP's default position animation. |
| Doorway collision | Clip only the front doorway of open ship exteriors with a valid teleportable portal; dynamic shell shapes retain side/back walls and deck. |
| Sonic destination message | Show world coordinates followed by `(On ship)` while keeping the actual target ship-local. |
| Player transfer | Clear VS attachment, drag/interpolation state and queued positions when IP changes the player's dimension. Reject ship motion packets from another dimension. |
| Ship acknowledgement | Acknowledge synchronized loaded ships across portal worlds through VS's normal global known-ship API, preserving unloaded-chunk safeguards. |

## Current goals and validation status

- WWOO / DH lightweight-generation grid seams: alpha 34 adds an experimental
  one-chunk neighbour border. Fresh-LOD in-game comparison is pending.
- Create rendering with Immersive Portals and Iris remains open; see
  [the investigation](docs/create-rendering.md). Flywheel's shader fallback has
  not been bypassed.
- Ship waypoint creation/following and immutable selection were implemented in
  alpha 31; alpha 32 fixes the reported selection callback recursion. The pack
  owner confirms the waypoint UI working. Return travel to an unchanged ship
  still needs confirmation; the later unavailable-ship report followed ship
  disassembly and reassembly.
- Back-slot/custom elytra contrails and TARDIS biome translations were implemented
  in alpha 31 and are now confirmed working by the pack owner. Custom wing tips
  use approximate vanilla elytra geometry.
- Alpha 30's moving-ship exit velocity correction is now confirmed working by
  the pack owner in the latest build.
- Pre-entry ship visibility through reopened doors is a known limitation;
  further work is deferred at the pack owner's request.

## Repository maintenance

Compatibility sources and tests remain grouped by target mod. Update README.md
and this progress log alongside compatibility changes. Dependencies, logs and
experiments stay in ignored work/; clean-build artifacts stay in build/libs.
The documentation review after alpha 32 reconciles stale open goals and validation
notes without changing runtime code or the release version.

## Earlier blocker and correction (alpha.6/7)

Alpha.6 produced repeated server task errors:

```text
Ambiguous or missing compatibility call org.valkyrienskies.core.impl.shadow.Et.getAllShips []
```

Alpha.7 assumed the failing getter exposed unflagged covariant adapters.
It selects the most specific return type when parameter signatures
are identical. Genuine overload ambiguity still raises an error. A regression
test reproduces the unflagged adapter pattern and verifies cached resolution.

### Alpha.7 runtime result

The supplied Prism Launcher log confirms alpha.7 is installed and loaded, but the
same `Et.getAllShips` compatibility exception occurs **356 times**, all through
`ShipTransitCompat.wrongDimensionMotion`. The alpha.7 resolver correction is
therefore insufficient for the actual runtime getter set; its synthetic
regression fixture does not establish compatibility with that set. The cause of
the remaining ambiguity/missing resolution needs inspection of the exact VS jar.

The user confirms entry, placement through the portal and visibility of the ship
from inside remain broken. Ordinary world blocks remain visible from inside.
The log contains **5** `Reject cross-portal action` messages for targets in the
TARDIS dimension, and **3** `moved while colliding with unloaded ships!` warnings
followed by overworld position corrections. These establish server rejection and
an unloaded-ship movement safeguard being triggered, but do not prove their
underlying causes or that the reflection exception causes every symptom.

The **20** `Raycast too far` errors originate in Sound Physics Remastered 1.5.1
sound-occlusion checks, through IP's block traversal guard, on the sound thread.
They are separate from the placement rejection and should not be used as evidence
that placement raycasts are too long. DWM shell hiding is an unconfirmed rendering
hypothesis; hiding only the shell would not by itself explain the whole ship view.

### Alpha.8 correction

The user supplied the exact VS/IP/DWM/Eureka binaries. Inspection of VS `Et`
confirms both public `getAllShips` adapters have `ACC_BRIDGE | ACC_SYNTHETIC`;
the implementation is exposed under an obfuscated name. The resolver's bridge
filter discarded both adapters, leaving no candidates. Alpha.8 includes bridge
methods in covariant resolution. Its regression test now covers an API name
exposed exclusively by flagged bridge adapters, alongside unflagged adapters and
true overload ambiguity.

DWM's `shouldHideExteriorShell` is called only by
`BaseTardisExteriorBlockRenderer`; it exits that block renderer, without cancelling
VS ship render lists. VS's Sodium hook separately iterates ship render lists and
renders their block entities. This excludes that particular shell-hiding call as
a direct whole-ship render cancellation, but does not establish the actual cause
of missing remote ship geometry.

IP's placement rejection is emitted when `canPlayerReach` fails. Its portal
branch checks destination dimension, interactability and transformed-player
distance. No reach bypass or separate entry/placement/rendering fix is included
in alpha.8; those issues remain unresolved pending further investigation and
runtime testing.

### Portal-view ship loading investigation

The user clarifies the unloaded-ship movement warnings occurred on returning
through the portal, before the ship blocks loaded. They do not independently
establish the reason the ship was invisible from inside.

Static inspection of the supplied binaries establishes a gap between the two
tracking paths:

- VS `MixinImmPtlChunkTracking.addShipChunks` redirects IP
  `ImmPtlChunkTracking.updateForPlayer` chunk enumeration. It requests active
  shipyard chunks for ships intersecting the destination chunk loader area.
  Fan4Compat does not cancel this original hook.
- VS core `CY` generates ship chunk watch/unwatch tasks. Its per-player branch
  compares the ship's `getChunkClaimDimension` with `VsiPlayer.getDimension`.
  A mismatch skips a new watcher or queues removal of an existing watcher,
  before evaluating `getForceWatchingShips`. `MinecraftPlayer.getDimension`
  reports the player's actual current world, without portal-view information.
- Client `getShipObjectWorld(ClientLevel)` delegates to the shared Minecraft
  client ship world. Sodium's ship renderer enumerates that world's loaded ships
  to build ship render lists, then reads blocks from its own render world.
  Having destination blocks cached cannot replace absent client ship data.

These paths explain how portal-view block requests can coexist with loss of VS
ship watching after entry to the TARDIS. This is a confirmed static tracking gap
and a strong candidate for the missing ship view, not in-game packet confirmation.
Simply adding a force-watched ship would not fix the dimension gate. A correction
must recognize IP's actual shipyard chunk watches in the VS watch decision,
preserve ordinary dimension checks when there is no portal watch, and release
that watch when the portal view expires. Client receipt/removal of ship data and
destination shipyard chunk availability should be verified together. The loading correction below implements this bridge.

### Alpha.9 portal ship watching correction

A new IP-only mixin modifies the exact VS `CY` per-player/per-shipyard-chunk
watch decision. IP's `isPlayerWatchingChunk` must report that this chunk in the
ship's own dimension was delivered to this player. For such watches the dimension
gate admits the player and signed distance is treated as inside the chunk, so VS
keeps ship data synchronized for remote portal views and distant same-dimension
views. Without that IP watch, both original results remain unchanged. IP's own
watch expiry/unload delay controls retention; no permanent force-watch set is
created. The native IP shipyard chunk-loading hook remains active.

`./gradlew build --offline -PvsJar=work/dependencies/vs.jar` passed. New tests
cover remote watching, unchanged normal distance rules, player/dimension/chunk
isolation and watch expiry. ASM analysis of the supplied VS core confirms the
exact selector, one dimension call, one distance call and captured ship/player/
packed-chunk local types at both injection points. Generated bytecode checks cover
45 addon classes. No Minecraft runtime test was performed; entry and placement
fixes are not claimed by this change.

## Open issues

1. Stationary ship-mounted TARDIS entry is blocked. Moving entry sometimes works
   only while the portal trails behind the exterior.
2. Placement through the portal into the TARDIS does not work. No separate
   placement fix has been established yet.
3. Some crossings leave the player stuck near Y=0 in the TARDIS dimension while
   the minimap still reports the overworld; an explicit dimension teleport
   restores movement. Logs showed client/server corrections and awaiting-teleport
   rejections. The transfer changes need runtime confirmation.
4. Opening from inside showed the portal moving through the air into position,
   and the ship was missing from the view outside. Alpha.5 addressed spawning
   animation and remote chunk notification; the complete view remains unverified.
5. Sonic message formatting, smooth fast-motion tracking, full shader combinations,
   and passage of ships themselves through portals remain unverified.

## Validation completed

- Alpha.8: `./gradlew build` passed, including bridge-only reflection regression,
  existing smoke tests, 43-class bytecode validation and jar packaging checks.
  No Minecraft runtime validation has been performed for alpha.8.

- Alpha.7: `./gradlew build` passed, including isolated regression tests,
  generated-bytecode checks and jar packaging checks.
- Exact mixin selectors and reflection contracts were checked against the supplied
  VS/IP/Eureka jars, Distant Horizons 3.3.3 and **DWM 1.0.38.4**.
- [Alpha.7 GitHub Actions build](https://github.com/Livinglive234/fan4compat/actions/runs/37572094034)
  passed. CI remains a single **Build** job.
- No full Minecraft runtime test was performed in the development workspace.

## Next in-game checks

1. Replace older addon jars with alpha.9 on client and server; verify the `Et.getAllShips`
   errors disappear, then repeat entry/placement/view checks below.
2. With the ship stopped, enter and exit repeatedly without commands. Check
   coordinates, dimension agreement, movement and inventory after each crossing.
3. Place a disposable block inside through the open portal and check that the
   server accepts it. Repeat with a TARDIS on ordinary terrain for comparison.
4. Repeat crossing while the ship moves and rotates; check that closed doors and
   neighboring deck blocks remain solid.
5. Open from inside and check immediate portal attachment and visible ship geometry.
6. Check the sonic's `(On ship)` message, recall to a moving deck, save/reload and
   obstructed landing behavior.

## Scope limits

This repo does not replace Create's own fixes or provide universal modpack
compatibility. DWM's EMF model-reuse errors, BetterEnd recipe errors, the absent
`jeed:effect_provider` serializer and Firefox's download warning are separate
issues. The download warning has not been established as a false positive.

## Build delivery preference

Always run `./gradlew clean build` before delivering a build. Leave the JAR in
`build/libs`; the user does not want builds moved. This session cannot expose
that directory as a chat download and its projectless output folder is read-only.
Deliver downloadable builds through repository GitHub Actions, which also runs
`clean build`.

## Moving portal attachment investigation

The user reports portal/exterior lag persists at higher ship speed. Exact binary
inspection confirms VS `MixinGameRenderer.preRender` calls
`VsiClientShipWorld.updateRenderTransforms(partialTick)`, and ship drawing uses
`ClientShip.getRenderTransform`. Fan4Compat currently updates portal geometry
from the authoritative ship pose on the server state tick, so its client portal
pose is not derived from the same frame transform as the rendered exterior.
This is a plausible timing mismatch; the observed delay has not been measured.

IP exposes client animation updates and `PortalAnimation` frame/tick histories.
`ClientTeleportationManager` uses `checkDynamicTeleportation` only when previous
frame counters and previous/current tick states are valid; otherwise it uses the
static crossing path. `PortalAnimation.updateClientState` records frame history
only for portals considered to be running an animation. Simply setting portal
position in a renderer would therefore not establish correct moving crossings.

A viable implementation should synchronize ship-local doorway position and basis
for both portal endpoints, derive client frame geometry from VS's render transform
after that transform is updated, and maintain IP's frame/tick crossing history
including initialization and teleport counters. The server must keep authoritative
geometry and teleport validation. Cache the attached ship and local basis and
update only registered attached portals, avoiding world scans or extra position
packets. Both the outside origin and inside destination need the same transform.
Detach/ship-unload handling must avoid retaining stale client geometry. This is
an investigated implementation path, not a completed tracking patch or proof
that all motion lag will be eliminated. No new build was created for this check.

## Alpha.10 ship-relative portal motion

Both TARDIS portal endpoints now carry a compact ship-local doorway descriptor
in their existing portal synchronization NBT. Before IP samples client crossings,
registered attached portals use VS's exact default render interpolation routine
with the current partial tick. Previous/current ship tick transforms also populate
IP's moving-portal tick history, and frame history follows IP's teleportation
counter. Missing/gapped history starts with IP's ordinary static crossing path.
After VS updates native render transforms, portal drawing is reconciled to the
published ship render pose. The outside origin and inside destination use the
same local doorway axes, position and dimensions. Server geometry and validation
remain authoritative; no extra position packets or world scans were added.

The client processes only registered attached portals, caches transform math per
ship within each update, clears history on unload/detachment and registrations on
IP client cleanup. Native IP animation drivers keep ownership of their histories.
Custom VS transform providers retain the original server-synchronized portal
behavior; they are not invoked twice or assumed to be pure interpolation functions.
Non-finite, degenerate or non-orthogonal attachment metadata is rejected.

Alpha.10 is built with `clean build`; regression fixtures cover both endpoints,
fast translation, rotation, scaling, tick/frame history and lifecycle cleanup.
Exact VS/IP bytecode checks validate the new injection selectors, interpolation
routine, portal-state construction and history fields. Minecraft runtime validation
is still pending; no claim is made that entry/placement failures are fixed.

## Alpha.11 world-opening crash correction

The supplied alpha.10 log reports `InvalidInjectionException` in
`PortalShipWatchMixin.fan4$dimensionEligible` while VS initializes its server ship
world. The generated `@Local` parameter annotations were runtime-visible, but
MixinExtras declares `@Local` with CLASS retention and its SugarInjector reads
only invisible parameter annotations. Consequently it never processed the sugar
parameters and checked them as ordinary target-method arguments. The same
annotation generation affected both dimension and distance handlers.

Alpha.11 emits both handlers' local annotations as runtime-invisible. A generated
bytecode regression check verifies retention, all three exact local indices, and
both handlers in every build, alongside the existing actual VS local-type checks.
Alpha.10 client render attachment behavior remains included. No Minecraft runtime
test has been performed; this addresses the specific startup failure in the log.

## Alpha.12 watcher local reconstruction correction

The next supplied alpha.11 log gets past signature validation but reports
`SugarApplicationException: Unable to find matching local` for the distance
handler's player at slot 6. The obfuscated static watcher method overwrites
several incoming argument slots: slot 6 begins as a Set and becomes VsiPlayer
inside the loop. Raw ASM frame analysis establishes the actual runtime value,
but does not validate Mixin's reconstructed local table; the earlier tests missed
that distinction. The shutdown `pipeline has not been initialized` error follows
the failed VS startup and is secondary.

Alpha.12 removes all local sugar captures from this mixin. A WrapMethod receives
the original ship and chunk x/z arguments before VS reuses their slots, and
establishes a per-thread invocation scope. A narrow redirect receives the player
directly from the `getPosition` call receiver, preserves the original return, and
sets the current player's IP watch eligibility. The two expression modifiers now
consume only their original boolean/double values. The scope is restored on
normal return and in a catch-all rethrow path, including nested invocations. No
world scan or permanent ship watch is added. Alpha.10 render alignment remains.

`./gradlew clean build --offline` with the supplied VS/IP binaries passed all 20
tasks; bytecode verification covers 54 addon classes. Regression checks execute
the generated wrapper against fixtures, verify every forwarded argument, preserve
the original exception, exercise normal/exceptional scope cleanup, and check
player/chunk/dimension isolation and watch expiry. No Minecraft runtime test has
been performed, so in-game world startup and portal behavior still need retesting.

## Alpha.13 repository organization

Runtime helpers, ASM generators, generated mixin packages and test source folders
are grouped by `doctorwho`, `immersiveportals`, `valkyrienskies`, `eureka`,
`distanthorizons`, `sable` and `shared`. The root `CompatPlugin` still controls
optional-mod loading. Extracted the per-mod generator methods from GenerateAddon
and the portal-transit generation from TardisBridgeGenerator; the shared build
orchestrator now calls the separate generators. Build tools retain default-package
classes and are not shipped. README documents the layout and clean build command.

Updated every runtime/helper package reference, reflective fixture class name,
generated mixin name, mixin configuration and Fabric entrypoint. The mod ID,
configuration filename and saved-data keys remain unchanged. Packaging verification
now checks every configured mixin, plugin and entrypoint resolves to a JAR entry.

The clean build passed all 20 tasks, including VS/IP binary checks and bytecode
verification for 54 classes. A separate comparison of all 54 runtime classes
against alpha.12, normalizing only renamed classes and excluding debug metadata,
found identical bytecode. This is an organization change with no new compatibility
behavior; an actual Minecraft mod-loading test has not been performed.

## Alpha.14 synthetic ship observer correction

The supplied alpha.13 log shows the mixins now apply and the server starts, then
crashes in the first watcher tick calling `ShipObserverPlayer.getPlayer`. The
watch bridge assumed every VsiPlayer was a MinecraftPlayer. Inspection of the
supplied VS binary confirms ShipObserverPlayer is a synthetic watcher with its
own position/dimension/forced ship set, and no getPlayer method.

The bridge now checks for VS MinecraftPlayer before accessing getPlayer. Other
VsiPlayer implementations retain native position, dimension and distance behavior;
they do not enter IP player chunk-watch checks. Tests cover an observer with no
player getter between real player iterations, ordinary dimension/distance rules,
position forwarding, and real-player watch recovery. Binary contracts assert the
actual wrapper and observer APIs. This is independent of the alpha.13 package
organization. The addon remains untested in a live Minecraft runtime.

## Alpha.15 doorway, key range and remote ship acknowledgement

User confirms alpha.14 moving portal alignment is stable. Entry remains an
invisible wall. Spectator entry and entry while cloaked work, implicating the hard
shell. DWM's supplied BaseTardisExteriorBlock returns the same solid shape for
open and closed doors; cloaking alone empties it. Alpha.15 subtracts a front-half
channel from the original collision and outline shapes for an open, registered,
valid teleportable ship portal. The channel follows the raw block facing and
entrance width, retaining at least one pixel of wall at each side. Back and side
faces remain solid, as requested. Removed the earlier whole-column shape
exclusion so it cannot discard these retained walls. Closed/unattached/terrain
shells retain their original shapes, including DWM's native cloaking behavior.
No portal offset or movement-tracking changes were made.

Key opening previously compared ship-local exterior coordinates with the player's
world coordinates. The range redirect transforms the exterior for that comparison
only, retaining the native ten-block Manhattan limit, same-dimension checks,
locking and cooldown behavior. Saved key/exterior addresses stay ship-local.

Opening from inside after teleporting into a closed TARDIS fails to show the ship;
physically visiting the exterior first works. Static tracing shows native VS/IP
chunk enumeration searches all saved ship metadata, not just loaded ships. The
cross-dimension watch warning logs but continues loading; it is not a rejection.
However, our received-ship acknowledgement bridge incorrectly limited IDs to the
player's current dimension. VS uses a global client ship world and global known-
ship IDs, and its native constructor/packet validation has no dimension filter.
Alpha.15 acknowledges all already received loaded ships once, including portal
worlds. This fixes that gap; it does not prove the cold-loading issue is resolved
or force unloaded ships into the client's loaded set.

Placement reach rejections target the exterior's shipyard coordinates in the
interior dimension, far from the actual interior doorway. IP's remote target code
sets its target dimension from portal.getDestDim correctly. Logs do not establish
whether the remaining bad target comes from selection, geometry or world state.
Clearing only the front outline may address interception, but no reach bypass was
added. Four bounded INFO categories collect doorway eligibility, remote target and
transformed eye, rejected reach and nearby portal transforms, and requested versus
delivered shipyard chunk counts while viewing portals from the DWM dimension.
Each category emits at most eight messages per process, five seconds apart;
closed doors do not consume the loading samples. Diagnostic collection stops at
its cap. Restart the game for a fresh comparison.

Clean build passed all 20 tasks with supplied VS/IP/DWM binary checks and bytecode
verification for 60 classes. Fixtures exercise all four door facings, a clear
front with solid back/side faces, closed-door preservation, retained ship-query
walls, key distance and terrain fallback, and acknowledgements across dimensions
without visiting them or sending duplicates. Generated selectors and reflected
DWM/IP fields were checked against the actual binaries. No live game test was
performed. Test first by teleporting inside with closed doors, opening them and
viewing the exterior before physically visiting it; then try the key, ordinary
entry, all solid shell faces and placement through the doorway. Inspect the four
Fan4Compat log categories if a failure remains.

## Alpha.16 ship-aware coordinate scanning

User reports leaving the ship, recalling the TARDIS onto terrain, then setting
world coordinates above a floating ship. Vertical scanning landed on ground,
not the ship. After exiting, the ship was invisible and movement became stuck
near its altitude in every game mode, including spectator; camera and commands
still worked. User confirms this happened with an earlier build or mismatched
versions, not confirmed alpha.15 on both sides. No new log was supplied.

Static inspection confirms alpha.15 and earlier only converted key recall from a
supporting ship deck. Normal DWM getSafeSpot scans terrain at world x/z and never
queries ship surfaces. Its TOP mode tries upward from the requested Y, then
BOTTOM downward on failure; BOTTOM tries downward, then upward. High starting Y
therefore finds the first downward candidate only after an upward failure.

Alpha.16 merges one VS ship-only vertical ray hit into each TOP/BOTTOM directional
scan result. The ray is in world coordinates and returns a ship-local block
address. Convert world facing into the ship frame and validate the landing block
using native findLandingSpot with DIRECT mode, which retains support, clearance,
facing and build-height checks. Compare its transformed world height with the
native terrain result and choose whichever is reached first in that direction.
A closer terrain result wins. Missing ships, missed rays and rejected clearance
retain the native terrain result. DIRECT, NONE and already ship-local destinations
retain their native behavior, and DIRECT validation does not recursively scan.
The scan does not add per-tick polling or permanent chunk loaders. It considers
the nearest ship ray hit; it does not search past an unsafe first ship hit for
additional ship decks. Native terrain and opposite-direction fallback remain.

RaycastContext has two public five-argument constructors, Entity and ShapeContext.
A null entity would be ambiguous under the generic reflective constructor helper.
Added explicit constructor selection and verified the Minecraft 1.21.1 mapping;
the shared regression test proves correct null handling with overloaded fixtures.

The targeted VS binary's isCollidingWithUnloadedShips checks the player's known-
ship IDs and required chunk availability near ship bounds. Its server movement
hook cancels packets and teleports the player back on failure, emitting
"moved while colliding with unloaded ships!". There is no spectator exception.
This is a plausible explanation for the reported movement-only freeze, not a
confirmed runtime diagnosis without a log. Alpha.16 retains the guard, the
alpha.15 received-ship acknowledgement correction and the existing loading bridge.
Do not acknowledge absent ships or bypass missing chunk checks to suppress it.

Clean build passed all 20 tasks with supplied VS/IP/DWM binary selector checks
and bytecode validation for 61 classes. Fixtures exercise high-Y ship versus ground
selection, closer terrain, upward selection and wrong-direction rejection,
ship-local position/facing, rejected native clearance, missed ship hits, local
recall preservation, NONE and DIRECT, plus all existing doorway/lifecycle tests.
No live Minecraft test was performed. Install alpha.16 on both sides and reproduce
coordinate landing first; if movement still sticks, capture the log including
native unloaded-ship warnings and Fan4Compat loading messages.

## Alpha.17 doorway lookup crash correction

The supplied alpha.16 log crashes on the render thread while IP raycasts the
exterior outline. The first failing addon call is openExteriorShape's block-state
property lookup. Class.getMethods resolves an unrelated method signature whose
Porting Lib IPlantable class is absent, producing NoClassDefFoundError before our
property method can run. Recipe/EMF errors earlier in the log are separate.

Replaced the doorway's OPEN, FACING and getBlock lookups with cached MethodHandles
resolved by explicit declaring type, parameter and return signature. These resolve
only the needed virtual method and retain subclass dispatch. The general helper
is unchanged for existing uses. Added a regression fixture containing a method
with an absent optional parameter type: getMethods reproduces the original error,
while the typed lookup succeeds twice and invokes the subclass override. Existing
source shape tests exercise both typed property and block lookup calls.

The same log reports twelve requested exterior shipyard chunks progressing from
zero delivered to all twelve delivered before the crash. This establishes IP's
chunk request/delivery bookkeeping in this run, not ship metadata reception or
successful rendering. No entry, placement or scanning outcome can be established
from this crash. The first remote target diagnostic uses an eye at (0,0,0), so it
is insufficient evidence for a steady-state targeting failure by itself.

Clean build passed all 20 tasks with supplied binary selector checks and bytecode
verification for 63 classes. No live game test was performed. Alpha.17 retains
alpha.16 coordinate scanning and the doorway-only cut with solid side/back walls.
Retest world loading and portal targeting first, then entry, placement and the
high-Y coordinate landing reproduction.


## Alpha.18: preserve scan inputs and IP-owned ship chunks

The alpha.17 test still lands on terrain, blocks reliable doorway entry and
leaves the ship invisible with movement frozen after TARDIS travel. The user
confirmed TOP scanning at Y=1000 and freezes outside the portal in every mode.
The native VS unloaded-ship guard has no spectator exception; the new report
supports investigating missing client ship chunks rather than shell collision
as the explanation for those outdoor freezes.

Two concrete bridge defects were found:

- DWM getSafeSpot overwrites its BlockPos argument as the scan walks up/down.
  Alpha.16/17 injected at RETURN and consequently used the final ground position
  to start the ship ray. Alpha.18 wraps the method, runs native terrain scanning
  exactly once, and compares the ship ray using entry arguments preserved in the
  wrapper frame. TOP retains native upward-first/downward-fallback behavior.
- vs$removeShip erased chunks from IP's authoritative cache on VS metadata
  unloading. IP's server watch could still record those chunks as delivered,
  preventing retransmission. The duck now leaves lifetime to IP's unload packets;
  its getter continues exposing the actual loaded IP shipyard chunks. This does
  not add permanent chunk tickets or bypass the native movement safety guard.

The shell's constructor settings now mark its collision shapes dynamic, so the
vanilla two-argument state collision getter cannot return a portal-independent
precomputed solid shell. Existing clipping still removes only the front doorway
on an open, attached, valid portal and preserves side/back walls. This closes a
cached-shape path; full-pack doorway entry remains a runtime verification item.

Landing diagnostics report the original scan input and distinguish a ray miss
from rejected native clearance. They run on landing scans, not every tick.
Previous globally capped loading/doorway logs were exhausted before the later
failing scenario, so they cannot prove its ship chunks or opening were ready.
Remote placement logs also contain target positions paired with the wrong
portal dimension; that separate interaction issue is not claimed fixed here.

Validation executes the generated landing wrapper with a native-operation
fixture that advances its target to terrain; the wrapper retains high Y and
selects the ship. Tests also execute the dynamic-settings handler, verify its
exact DWM constructor injection, preserve IP chunks across metadata removal and
check normal IP eviction. The final clean build passed all 20 tasks and bytecode/package checks
for 63 addon classes. These checks do not simulate the complete modpack.


## Alpha.19: remote placement and ship arrival refresh

The alpha.18 log and user report confirm the ship renders again and approaching
it outside the TARDIS no longer freezes movement. Returning through the portal
still leaves movement stuck, including spectator. The log explicitly records
both client and server changing from DWM to the overworld at the transformed
ship doorway position; it does not establish an incomplete dimension transfer.
The remaining freeze may still be the native missing-ship/chunk guard, but no
prior diagnostic recorded that guard's actual client result.

Placement rejection has a concrete independent cause. Native VS MixinMinecraft
useOriginalCrosshairForBlockPlacement substitutes its cached source-world hit
for the hit that IP installed in its switched destination world. Alpha.18 logs
show a correct remote interior target near (0,130,-12), while the server receives
an overworld target near (-194,92,662) paired with the DWM dimension. Breaking
uses another path and now works according to the user.

Alpha.19 scopes a cleared VS originalCrosshairTarget around IP's placement-only
withSwitchedContext call, allowing VS's native fallback to use IP's actual hit.
It restores the source hit on normal return, nested calls and exceptions. Normal
ship placement and portal breaking retain their existing paths. Server reach
validation is retained.

After IP installs the destination world and position, the server transfer hook
clears old motion state again and updates IP watches. For ships intersecting a
small two-block arrival area, it requeues valid already-delivered watches in a
3x3 shipyard-chunk area through IP's normal delivery batch. Pending, invalid,
other-player and other-dimension records are not changed. This is one-time
recovery per transfer, not permanent loading or tick polling; it can resend
up to nine chunks per intersecting ship. It addresses an arrival cache/delivery
mismatch, but the supplied log does not prove that mismatch caused this freeze.

A bounded observer on the native isCollidingWithUnloadedShips result records
client synchronization, known ship IDs and nearby active/missing chunks only
when the guard actually blocks the local player. Four samples at least five
seconds apart are available per world/transfer. The budget resets on client
transfer so startup samples cannot consume the return-trip budget. Server-side
observations return before loading any client-only class. The guard is never
bypassed, and unknown ships are not acknowledged by diagnostics.

Tests execute the generated placement wrapper with simulated native VS hit
substitution, including nested calls and exception restoration. Arrival fixtures
check selective requeueing and unchanged IP watch-count bookkeeping; exact
uploaded VS/IP method selectors and reflection contracts are verified. Runtime
modpack placement and successful return to a ship still require user testing.

Final alpha.19 clean build passed all 21 tasks, including 67-class bytecode
validation and standalone JAR packaging. The artifact remains in build/libs.


## Alpha.20: sampled doorway collision evidence

User requested targeted evidence for the remaining invisible-wall TARDIS entry
failure. Collision behavior and retained side/back walls are unchanged.

The VS collision-query bridge now captures DWM source-shape hooks while sampled
lazy BlockCollisions iterables are consumed. Logs identify collision versus
outline calls, shell block/facing/open state, whether the doorway cut actually
ran or why it was skipped, and original versus retained shape boxes. They also
include nearby portal validity/world/entity permission, signed player-foot
distance to its plane, transformed player-body AABB, and a bounded sample of
actual colliders returned to VS. Query/body AABB overlap identifies candidates,
not proof of a final rotated-polygon contact.

The sample returns the same collider objects in the same order. Unsampled calls
retain the original lazy iterable. Thread-local capture is restored in finally,
including native iteration failures. Samples are restricted to player queries
in shipyard space, with one probe per five seconds and at most eight logs per
minute per player instance. Weak-key budgets reset their minute window, so
startup or earlier doorway activity cannot exhaust them for the whole session.
Ordinary ship queries without a shell hook or nearby registered doorway produce
no doorway logs.

Regression checks consume a lazy fixture once, preserve collider identity,
verify closed-door reasons, applied-cut geometry, portal-plane/body-overlap
fields, rate limiting, and capture cleanup after iteration exceptions. Native
Minecraft VoxelShape.method_1090 is mapped getBoundingBoxes in Yarn 1.21.1.
The log prefix is [Fan4Compat doorway collision]. Movement-guard diagnostics
from alpha.19 remain available independently.

Alpha.20 clean build passed all 21 tasks, including bytecode validation for
69 addon classes and standalone JAR verification. No runtime collision fix is
claimed by this diagnostics release.


## Alpha 21: separate client/server doorway registrations

The alpha 20 runtime log and user test confirm exit, interior placement/mining,
and Top landing now work. Entry from a ship still fails. Server collision samples
show `cut=applied`, while open client shells show
`cut=no-matching-valid-teleportable-portal` and retain their full collision shape.
The client samples enumerate server-side portal registrations (`sameWorld=false`).

Minecraft Entity.equals/hashCode compare entity IDs, so client/server copies of a
portal collide in the shared WeakHashMap in an integrated server. WeakHashMap
retains its original key object on replacement. Replace exterior registrations
and portal attachment/history maps with weak object-identity keys, preserving
separate instances and allowing discarded portals to be collected. This also
prevents client detach from deleting the equal-ID server attachment.

The regression fixture now reproduces native entity-ID equality and checks that
both worlds have an eligible doorway, and client removal preserves the server
registration. Existing motion unload/detach and retained doorway-wall checks pass.
No collision dimensions or portal positions were changed. In-game entry still
needs user verification; other chunk-loading/error messages are not claimed fixed.

Validation: clean offline build, all 21 tasks executed; collision/motion regression
checks, bytecode validation for 72 addon classes and alpha 21 JAR packaging pass.


## Alpha 22: ship travel and console quality of life

User confirms alpha 21 now works for entry, exit, placement, mining and landing.
Remove the temporary interaction/collision/movement diagnostic classes, mixins
and logs while retaining functional doorway, portal motion and chunk loading fixes.

Moving ship destinations extend native flight duration and remaining ticks when
the target recedes beyond the original trip allowance. The departure position
and effective normal flight rate are saved with the trip; destination movement
cannot shorten already allocated travel time. A faster receding ship may keep
extending a chase until it slows or changes course.

Current coordinates follow the ship. Native previous/destination aliases of the
current position retain independent world snapshots; distinct ship locations
continue following their own anchors. Coordinate lever notifications use the
live world destination. Direct scanning is substituted only for ship landing
lookup, leaving the user's selected mode unchanged throughout the flight.

ShipWaypoint is an immutable addon anchor using the native waypoint record and
codec, with ship identity in its ID and local coordinates in its position. Saving
current location while landed creates a ship waypoint. Creation snapshots remain
valid if the ship moves while naming it. Coordinate inputs/headings are hidden,
updates are blocked on both client and server, and native access rules still apply.
Applying resolves the authoritative stored entry; a missing/replaced ship reports
unavailable rather than targeting unrelated terrain. Normal waypoints are unchanged.

Add the missing Eureka creative-tab English translation. Optional Jade 15.10.6
compatibility hides its overlay while the local player controls a non-passenger
VS mounting entity and restores it after dismounting without changing settings.

Validation: clean offline alpha 22 build passed all 22 tasks, native DWM injection
selector checks, bytecode and standalone packaging checks. QoL fixtures cover
flight extension/reload, independent displays, temporary scan selection, ship
waypoint creation during movement, access/update restrictions, authoritative
application, missing ships and helm/passenger/dismount overlay behavior. Runtime
Minecraft testing remains with the user.

Aether: no whole-ship dimension transfer is implemented here. A nearby portal
alone does not transfer a ship; player/entity crossings can leave the ship behind.
The user's custom Aether immersive-portals build has not been runtime-tested.


## Alpha 23: prevent Nether and Aether portal activation on ships

User requests that Nether/Aether portals cannot be created on ships. Add early
creation guards for vanilla NetherPortal.getNewPortal, Aether 1.5.11 Fabric
findEmptyAetherPortalShape, IP checkPortalGeneration, and IP NetherPortalLikeForm
perform (also used by frame-based custom portal integrations). Return the native
failure result before searching for frames or starting remote generation.

Check the portal's block address with the exact VS isBlockInShipyard(World,
BlockPos) API. This works without loaded ship metadata and is independent of
player position. Terrain frames remain permitted even when the activating player
is standing on a ship. TARDIS doorway generation is outside these hooks and stays
enabled. This prevents new activations; it does not delete existing portals or
police command-created portal entities. No periodic scanning or debug logs added.

Optional hooks are gated by Aether/IP presence. The Aether selector was checked
against public Fabric 1.21.1-1.5.11; the user's custom immptl-fix jar is unavailable
and still needs runtime verification. IP selectors were checked against supplied
6.0.6 jar; vanilla selector against Yarn 1.21.1 mappings.

Validation: clean offline alpha 23 build, all 23 tasks executed, bytecode checks
for 83 addon classes and standalone packaging pass. New regression checks execute
all four generated handlers, confirm shipyard cancellation/native empty or false
results, terrain passthrough, null/non-World accessor passthrough and exact native
Aether/IP injection selectors. Existing TARDIS and portal checks also pass.


## Alpha 24: log compatibility fixes and fresh doorway ship loading

Investigate the supplied log (alpha 21) and separately reproduce the reported
alpha 22 failure opening a newly created TARDIS entrance from inside after
landing. ChestTracker persistence errors are intentionally left alone.

Doorway anchors now contribute the ship's active chunks directly to IP's normal
base chunk-loader enumeration. Use authoritative all-ships data, rather than
requiring the client or a world-space bounding query to already know the ship.
Only valid, nearby destination-anchored portals accessible to the player qualify;
origin world, ship ownership dimension, removed portals and duplicate ships are
checked. This bootstraps ordinary IP chunk watches and existing VS metadata
tracking without a separate periodic scanner.

Stamp missing IP player-dimension metadata on outgoing server position packets,
including the callback-bearing send path, while preserving explicitly supplied
destinations. Clear source-dimension awaiting-position retries after a committed
IP dimension transfer. This targets the observed null ResourceKey encoding crash
and obsolete correction retries; it does not blanket-drop authoritative client
teleports or claim to solve every possible in-flight packet ordering race.

Scope IP shader compiler thread locals around native compilation, with cleanup
on success and failure, so an Iris error cannot poison subsequent IP compilation.
For Iris 1.8.1, add the missing dhFarPlane float declaration only when referenced
and not already declared or macro-defined; preserve version/extension ordering.
Iris's existing live DH uniform supplies its value. Actual BSL/DH rendering still
requires a graphics runtime test.

Bridge BCLib 30.4.0 custom alloying/anvil/infusion result JSON from id to item,
preserving counts and component patches. RecipeManager.apply is the primary
codec path, so repair its resource map before decoding, with deserialize as a
fallback. Skip only the known JEED recipes with an absent effect_provider
serializer and the copper lantern conversion referencing absent Supplementaries
Squared. Keep valid pack overrides and installed dependencies. Supplementaries
need not be installed for stale/bundled recipe data to appear in a resource pack;
no fake serializer or mod dependency was added.

Accessories beta.48 resolves server-global entity IDs across IP's already loaded
client worlds. Missing spawn data is queued for at most 100 ticks, capped at 256
packets, replayed in per-entity order and cleared on disconnect. Expiry/overflow
remove the corresponding entity sequence to avoid applying orphaned deltas.

Fix the remaining IP portal-intersection target distance using VS world-aware
measurement. Extend Sound Physics 1.5.1's existing main-thread cloned-world
snapshot with nearby loaded ship chunks and immutable ship transforms. Audio
rays trace terrain and ships in coherent coordinate frames, retain native ignore
and material addresses, and compare world-space hits. Unknown/unsafe proxies
retain their native path; the audio thread does not query the live client world.

DWM Toyota console, Imperial console, Toyota engine/spinner and item renderers
reuse renderer-owned model roots. Reset transforms/visibility between draws and
invalidate roots when EntityModelLoader's resource data changes. Native model
wrappers and texture choices remain in place; no EMF warning suppression added.

The user confirms Aether 1.5.11's custom change is only the IP datapack pull and
BetterEnd Remastered 30.4.0 is unmodified. Exact uploaded Wover/BCLib were inspected;
Aether's unchanged upstream portal hooks were verified against the public build.
The two large custom Aether/BetterEnd attachments exceeded the executor's 32 MiB
transfer limit, so their full uploaded contents were not inspected.

Validation: clean offline alpha 24 build passes all 24 tasks, stack/type validation
for 111 addon classes and standalone packaging. Regression fixtures cover the
reported missing-component recipes, valid overrides, compile failures/context
cleanup, portal entity spawn/order/expiry/disconnect, packet metadata/corrections,
model resets/reloads, doorway load bootstrap/access/range/dimension and acoustic
snapshot coordinate conversion/nearest hits. New selectors and invocation points
were checked against exact IP, DWM, Accessories, Sound Physics and the official
Minecraft 1.21.1 classes remapped with Fabric intermediary mappings. Full-pack
Minecraft testing, especially freshly opening the doorway after landing, remains
with the user. No new debug logs, ChestTracker changes or whole-ship dimension
transfer behavior were introduced.


## Alpha 25: dimension-aware ship chunk packets and movement/audio follow-up

The supplied alpha 24 log confirms that ship rendering from a freshly opened
TARDIS doorway still fails. It contains 12 VS cross-dimension watch warnings,
20 Sound Physics ray-limit errors, four ordinary overworld chunk-loading failures
on exit and persistent BetterEnd recipe errors. No EMF or Accessories error
appears in this run. The earlier alpha 22 movement-skip errors are absent here;
that absence does not establish that movement is fixed.

Trace native VS ChunkManagement: its deferred watch callback sends shipyard
chunk/light packets directly through the player's connection, without IP's
owning-world redirect scope. This can place remote ship chunks in the current
TARDIS client world rather than the portal's overworld. Wrap the actual deferred
callback in PacketRedirection.withForceRedirect(world), with exception-safe scope
restoration. Treat an existing delivered IP watch as valid for the native task's
same-dimension warning only inside this scope; MinecraftPlayer's global dimension
is unchanged. Wrap VS dropChunk calls similarly and preserve a chunk when IP
still owns a delivered watch. Invalid/expired watches retain normal unload behavior.
This fixes a concrete delivery gap; full runtime ship rendering still needs testing.

Reject VS custom ship motion while the authoritative server handler has an
awaiting teleport position, including no-ship/detach packets. Vanilla already
holds ordinary movement pending acknowledgement; the custom VS handler bypasses
that guard. Resume immediately after the awaiting position is cleared. Retain
existing cross-dimension stale-ship rejection. Do not relax IP collision limits,
add a timed teleport grace period or claim every fast-ship movement case resolved.

Sound snapshots now convert a shipyard listener origin before the world-space
nearby-ship query, include its owning ship and clone nearby already loaded terrain
when needed. Do not reinterpret unowned shipyard chunks as physical terrain.
Trace only the immutable snapshot's cached extents; subdivide a long cached
segment into at most 256-block pieces, keeping IP's 512-block traversal safeguard.
Unknown/unsafe proxies keep their native path. Audio-thread code only uses copied
transforms and cloned chunks; no live-world query, new periodic scan or log muting.

Because BetterEnd failures persist despite alpha 24's resource-map repair, add
a version-pinned alias at BCLib 30.4.0's CODEC_ITEM_STACK_WITH_NBT decoder itself.
The delegating MapCodec accepts a missing item key from id, preserves existing
item precedence, leaves count/component patches unchanged and forwards native
encoding/key enumeration. This avoids relying on recipe preprocessing order or
the outer serializer shape. Other ItemStack codecs and mod versions are untouched.

Inspect the user's Livinglive234/createfabric main source and exact Flywheel
1.0.6-44 source/binary. Both INSTANCING and INDIRECT deliberately require
!ShadersModHelper.isShaderPackInUse(). The current Iris shader setup therefore
explains indirect -> off; TARDIS console geometry is not part of the selection
check. No unsupported backend is forced and no GPU capability/shader guard is
removed. An actual Iris/Flywheel shader bridge would require its own exact-version
and IP portal-rendering audit; this addon does not implement that backend.

Validation: clean offline alpha 25 build passes all 24 tasks, packaging and
stack/type checks for 119 addon classes. Native selectors include VS's actual
watch callback/dropChunk call and BCLib ItemUtil initialization, alongside IP,
DWM, Accessories, Sound Physics and remapped official Minecraft targets. Fixtures
verify owning-world sends, native argument forwarding, exceptional scope cleanup,
portal-owned unload protection, shipyard listener conversion, cached-space ray
limits/subdivision, teleport ACK/resumption and the generated delegating stack
codec's id/item handling and component/encoder preservation. No game/GPU runtime
was available. ChestTracker remains excluded; the DH generation, old snow data
and cosmetic resource warnings were not modified in this focused follow-up.


## Alpha 26: bounded movement diagnostics, no retreat bypass

The alpha 25 log confirms successful startup and the BetterEnd recipe errors are
absent. Twenty remaining ray-limit errors originate in portal targeting and Jade,
not Sound Physics. Two ordinary overworld chunk-loading failures remain. The user
reports immediate movement stops in both directions around a roughly 20-block
ship boundary even while ship geometry is visible. Visible geometry does not
establish that all active collision chunks or server-known ship acknowledgements
are available, but the native missing-ship guard has not yet been proven responsible.

Keep the speculative retreat patch only in work/experiments; it is excluded from
source, generated classes, mixin configuration and the release. Instead wrap native
Entity.move to observe requested versus actual movement and scope the original
isCollidingWithUnloadedShips return value. The guard hook is noncancellable, does
not replace its result and does not change collision. A client tick observer also
checks current terrain chunk availability while movement keys are pressed, covering
the possibility that movement never reaches Entity.move.

Reports identify client/server, dimension, position, player bounds, noClip, movement,
terrain availability, ship synchronization, server awaiting teleport, standing ship,
nearby ship IDs/bounds, known and loaded state, and missing active chunk coordinates
under the transformed player box with the native one-block margin. Up to eight
ships and 64 candidate chunk cells per ship are inspected per emitted sample.
All chunk queries use getChunkAsView and do not force load. Clipped movement
alone is not attributed to a particular solid block or advertised as a proven cause.

Logging is enabled in this diagnostic release, disabled by JVM property
fan4compat.movementDebug=false, throttled to three seconds and capped at 12
samples per player per dimension visit. Only one diagnostic failure is logged per
player. Weak references prevent the budgets from retaining old worlds. No collision
or movement bypass is installed.

Validation: clean build passed 24 tasks, including generated native selectors,
movement forwarding, original exception preservation, nested scope restoration,
budget throttling/caps and bytecode verification. In-game reproduction remains
necessary to identify the blocking check.


## Alpha 27: acknowledgement and missing active chunk recovery

Alpha 26's diagnostic log proves native isCollidingWithUnloadedShips is cancelling
movement: ship 5 is loaded but initially unknown to the server, and later known to
the client with missing active shipyard chunks [-1791617,768127/768128/768129].
At the outer boundary movement in both directions has actualDistance=0 despite
terrainLoaded=true and shipWorldSynced=true. Rendering the hull does not establish
that these outer active chunks exist in the client's world cache.

Native VS player construction may populate known IDs without sending C2S ACKs.
Native ACK handling also discards adds if the authoritative loaded-ships set is
not ready. Alpha 25's known-flag-based deduplication cannot repair either state.
Track actual sends independently per client player/world, resend after IP world
transitions, and forget unloaded IDs so they can be acknowledged on reappearance.

A noncancellable native ACK return hook retains early authenticated adds for
server-existing ship IDs, bounded to 64 pending IDs per player and ten seconds.
The existing VS chunk-loading tick flushes only nonempty pending queues, accepts
IDs after authoritative load, and removes expired, deleted, removed-player and
explicitly withdrawn requests. Collision/teleport checks are retained.

The existing doorway/base-loader enumeration now also watches active chunks for
ships spatially intersecting a 48-block region around the physical player, with
dimension filtering and ship deduplication. This covers outer active chunks while
approaching or leaving a ship, independently of a live doorway. It uses IP's normal
loader cadence; no additional periodic full-world ship scan is introduced.

A confirmed native client guard block requests recovery through existing native
ACK packets: at most eight nearby loaded ships, once per two seconds, up to eight
rounds per uninterrupted block. On the server, validated same-dimension ships
within 64 blocks may refresh player tracking and selectively requeue valid already
delivered watches under the transformed player bounding box plus the native
one-block margin (at most 64 cells). Pending, invalid, other-player and unrelated
ship chunk records are unchanged. The normal IP batch/light/attachment delivery
path is used, not raw packet injection or fabricated chunks. Server recovery is
also limited to once per ship/player per two seconds.

Diagnostic logging defaults off again, with -Dfan4compat.movementDebug=true to
reenable the bounded alpha 26 observers if needed. Their native guard observer
continues to trigger recovery independently of logging. The speculative retreat
bypass remains excluded.

Validation: clean build passed 25 tasks, including 128-class bytecode validation,
exact native injection targets, prepopulated known-ID acknowledgement, transition
resends/stable-world deduplication, deferred add/removal/expiry/disconnect behavior,
unknown and cross-dimension isolation, selective IP requeueing, client throttling,
negative-coordinate chunk margins and disabled diagnostic passthrough. Runtime
verification of the boundary freeze still requires the user's modpack.


## Alpha 28: bootstrap ship tracking from pending portal watches

Reopening the TARDIS creates a new portal pair; the existing pre-spawn hook already
attaches both portals, and the pose cache includes their identities. The VS core
watcher nevertheless required IP chunks to have been delivered before bridging
the player's dimension/distance. That can prevent pending doorway chunk requests
from bootstrapping ship metadata after remote tracking had been disconnected.

The core watcher now accepts a valid IP request for that exact player, ship
dimension and active chunk, including pending delivery. Packet redirect/drop
checks still require delivered watches. Invalid/deleted watches release tracking;
synthetic observers retain native behavior. No new polling or permanent ticket
is added. Regression fixtures cover pending requests, invalidation, cross-world
isolation and preservation of the delivered-only packet check. Full modpack
validation of outside-open -> enter -> close -> reopen inside remains necessary.

Validation: clean offline build passed all 25 tasks and bytecode verification
for 128 addon classes against the supplied VS, IP and DWM jars.


## Alpha 29: moving ship arrival settling

The alpha 28 log shows rapid alternating teleports through the new doorway pair.
The reported ship speed was about 10 m/s with the TARDIS facing forward;
stationary exits work. The reverse exterior portal sweeps across the stationary
arrival eye position within a few frames.

Successful attached doorway crossings clear pre-transfer animation history and
use IP's native five-tick teleport settling interval. Exterior arrivals resume
native dragging on the synchronized destination ship, after checking dimension;
interior arrivals remain detached. TAIL injection avoids extending the interval
on rejected teleports. Ordinary portals retain their existing behavior.

Per user direction, work on pre-entry ship visibility through reopened doors is
deferred. The proposed doorway delivery refresh was removed before publication.
Existing loading safeguards remain; alpha 29 changes only moving crossing
settling. Regression fixtures cover native drag attachment, settling, history
cleanup, interior and ordinary portal isolation. The forward-facing exit at
10 m/s still needs validation in the full pack.


## Alpha 30: avoid duplicated portal and deck velocity

The user reports being flung off the ship after alpha 29 stopped immediate
recrossing. IP transformEntityVelocity subtracts source portal-point motion and
adds destination portal-point motion to the player's velocity. VS moves attached
players with the deck separately; setLastShipStoodOn also enables a boarding
impulse that subtracts deck motion from velocity on its next drag pass.

For the local client player crossing a registered, loaded ship doorway, pass
zero portal-point velocities into IP's existing transform. Rotation, scaling and
the player's walking velocity still use the original IP transformation. Native
ship dragging supplies deck motion, and the already-relative arrival disables
the redundant boarding impulse. Other entities, ordinary portals, mismatched
ship dimensions and missing ship metadata retain original point velocities.
The five-tick settling interval remains. Pre-entry ship visibility is deferred.

Fixtures cover both doorway directions, other players, ordinary portals, missing
metadata and impulse suppression. The actual moving ship exit still requires
full-pack testing.

Validation: clean alpha 30 build passed all 25 tasks and bytecode verification
for 128 addon classes against the supplied native jars.


## Alpha 31: waypoint creation, contrails and biome naming

Fixed the waypoint creation hook to read the nested monitor `tardisTag`, matching
DWM's actual screen data. The previous fixture used a flat tag and missed the
runtime bug. The corrected fixture names a waypoint while the ship moves,
verifies its ship identity/local address, then exercises authoritative selection,
access denial, coordinate hiding, immutable entries and missing-ship rejection.
Existing ordinary waypoints remain ordinary; create a new waypoint on the ship.

For Elytra Contrails 1.4.7.5-1.21.1, capture a vanilla-sized fallback wing model
inside the player's rendered body transform only when a gliding player has no
existing emitter samples. This covers back-slot and custom renderers without
a whitelist of item IDs. The normal trail manager still owns configuration,
speed/gliding gates, rendering and lifetime. Native samples remain preferred.
Invisible players, nonplayers and shader shadow passes receive no fallback.
Custom wing shapes may need geometry-specific emitters for exact tip placement.

Supply `dwm.biome.tardis` and `biome.dwm.tardis` English translations as TARDIS.
DWM's remaining language entries are unchanged.

Create investigation: Flywheel 1.0.6-44 deliberately disables INSTANCING and
INDIRECT with active Iris shaders. The available 1.21.1 Iris/Flywheel bridge
builds inspected are NeoForge, not a compatible Fabric artifact. See
[docs/create-rendering.md](docs/create-rendering.md) for pinned versions, evidence
and the port/render-state work still required. No shader guard is bypassed and
IP's incompatibility warning is retained. No specific missing contraption has
yet been identified by the user; warning text alone is not that reproduction.

Validation: clean alpha 31 build passed all 26 tasks, packaging and bytecode
verification for 130 addon classes, including exact contrail and Minecraft
renderer API/injection checks. The three implemented fixes still require
modpack testing; Create rendering remains an open goal.


## Alpha 32: ship waypoint selection stack overflow

The supplied alpha 31 trace shows updateScreen calling TextFieldWidget.setText,
which triggers DWM's coordinate change listener and update(), recursively
reentering updateScreen until StackOverflowError. Hide and disable coordinate
widgets without rewriting their text; the coordinate label is already hidden.
The original fixture had no text listener and missed this behavior. It now
executes reentrant change callbacks, verifies that hiding fires none, and that
a native text update refreshes once without recursive clearing. Ordinary
waypoints and saved ship anchors remain unchanged.

Validation: clean alpha 32 build passed all 26 tasks and bytecode verification
for 130 addon classes, including the reentrant waypoint text-listener regression.

## Latest-build runtime confirmation (2026-10-07)

The pack owner reports that biome labels, contrails, exits onto moving ships and
ship waypoints are working. Current status sections now record that confirmation;
earlier pending-validation entries above describe the status at those releases.
No runtime changes or new release build are needed for this documentation update.

## Ship waypoint identity clarification (2026-10-07)

The subsequent "ship is not available" report occurred after travelling away and
then disassembling/reassembling the saved ship. Ship waypoints store the VS ship
ID and ship-local landing address; replacement ships can have different IDs.
The old waypoint must reject that replacement instead of silently binding to it.
Save a new waypoint while landed on the reassembled ship. This report does not
establish a lookup failure for an unchanged ship; that return-trip validation is
still outstanding. Runtime code and release version remain unchanged.

## Alpha 33: deleted ship waypoint display

Write an authoritative missing-ship-ID snapshot into the existing flight NBT used
by console screens. Use VS all-ship data rather than loaded ship/chunk state.
Keep names, IDs, access rules, native waypoint codecs and removal behavior intact.
The waypoint list applies strikethrough only to confirmed missing ship IDs.
Selecting such an entry replaces the normally hidden coordinate label with
"This ship has been deleted or reassembled", wrapped to the detail column width.
Status refreshes with console data; reopen the screen after deleting a ship.

Regression fixtures cover unloaded-but-existing ships, removed IDs, replacement
IDs, fresh status recovery, client non-authority, ordinary waypoints, name styling
and the exact native coordinate placement. In-game display verification is pending.

Validation: clean alpha 33 build passed all 26 tasks, exact DWM injection checks,
standalone packaging and stack/type verification for 131 addon classes.

Successful ship waypoint application also sends DWM's native MONITOR_WAYPOINT_LOADED
message ("Waypoint loaded"). Rejected/missing ship targets never show success.

## Alpha 34: experimental lightweight WWOO / DH neighbour border

The pack owner requests a fix that preserves lightweight LOD generation.
Inspected exact public WWOO 2.6.7 and DH 3.3.3 jars: DH's native batch border is
zero, while WWOO contains terrain features that extend across chunk boundaries.
Add one temporary neighbour ring only to Overworld FEATURES batches targeting
FEATURES, gated to these exact versions. Preserve existing nonzero padding,
native requested output coordinates, other modes and dimensions. No full-save
chunk generation, permanent tickets or cache deletion is introduced.

This is a candidate boundary fix, not a confirmed runtime solution to the grid.
Existing LOD caches stay unchanged; compare fresh LODs with and without JVM
property -Dfan4compat.wwooBorder=false. Generation costs increase according to
batch size. See docs/wwoo-distant-horizons.md for evidence and test procedure.

Validation: clean alpha 34 build passed all 27 tasks, packaging and bytecode
stack/type checks for 133 addon classes. Exact DH field contracts and extracted
native allocation math pass, as do cross-border feature and scope fixtures.
