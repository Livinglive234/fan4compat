# Release readiness and wishlist

Current build: **0.1.0-beta.24**. Includes an additional Iris DH depth-sampler guard, server save fixes, complete Eureka
notice filtering and DH/Iris portal-render guards. The pack owner confirmed the BSL_v10.1.5 portal fix working. Initial LOD
loading can take longer; subsequent loads behave normally. Beta 6 removes
temporary movement diagnostics and unused collision-query code.
Both client and server should run this build for Eureka server notices.
Beta 7 adds the Freecam tick-boundary fix; beta 8 adjusts nearby LOD clipping only
for its active camera in unloaded terrain. The pack owner reports beta 8 failed
with BSL only; beta 9 targets BSL 10.1.5 terrain/water distance discard. The pack owner confirmed Freecam with BSL working on 2026-10-10.
The individual edge-case checklist below remains useful after future changes.

Beta 10 adds Jade/VS ray-vector normalization and an IP/Iris framebuffer copy
fallback for macOS OpenGL 4.1. Verify Jade targeting on moving/rotating ships,
ordinary terrain and fluid targets; verify BSL portal crossings and visual depth/
color on the Mac. Native/API and fixture tests pass; gameplay/GPU checks pending.

Beta 11 targets Jade's stale crosshair after dimension changes and matches the
Apple recursive renderer's stencil representation to the actual main depth
texture. Native checks and fixture regressions pass. Retest TARDIS crossings
with Jade and BSL nested portals on Mac; the depth-mismatch cause is a candidate,
not confirmed by the beta 10 log. The native error-triggered fallback is retained.

Beta 12 adds automatic public-release downloads and post-exit installation.
Checks/install/backup and standalone-helper regressions pass. Verify a staged
update across restart on actual Windows/macOS clients and the server host;
container hosts that kill helper processes must use the documented pre-start hook.

A beta 1 dedicated-server log exposed a client-only classloading crash during
TARDIS startup and shutdown saves. Beta 2 has a reproducing classloader regression
and passes the clean build. Repeat dedicated-server startup, portal/waypoint use,
world save and restart in the full pack before treating this patch as verified.

Beta 20 adds temporary read-only Point Blank render diagnostics for disappearing
solid terrain with shaders off. Clean build, bytecode, fixture and native-binding
checks pass. GPU reproduction is pending: restart, disable shaders (or remove
Iris), draw the XM3, switch away and capture `latest.log`. This is an investigation
build, not a confirmed fix. Reports use `[Fan4Compat RenderDiag]`, are sampled and
capped, and are off by default; enable with `-Dfan4compat.renderDiagnostics=true`.

Beta 21 adds a candidate stencil-state fix for that report. Point Blank scope
callbacks and IP's normal stencil renderer now share Minecraft's cache; scope
callbacks preserve portal-owned stencil state during nested views. Clean build,
fixture execution and native binding checks pass. Test solid terrain after drawing
and switching away from the XM3, ordinary/nested portals, scope rendering and
shaders off/on. The pack owner confirmed Point Blank fixed on 2026-10-10;
this does not separately confirm every nested-portal case. Diagnostics remain available.

## Confirmed release checks

On 2026-10-10 the pack owner also confirmed Point Blank rendering, Freecam with
BSL, and Graves working. Earlier pending notes below describe historical release
checklists; this broad confirmation does not claim each unusual recovery case,
save/restart permutation or Mac nested-portal path was individually exercised.

On 2026-10-07 the pack owner confirmed the core workflows in singleplayer.
On 2026-10-08 the pack owner confirmed all remaining checks proposed for beta:

| Category | Status |
| --- | --- |
| Moving-ship TARDIS entry/exit, landing, placement and repeat crossings | Confirmed by pack owner |
| Ship waypoints after travel/world restart; save/reload and inventory preservation | Confirmed by pack owner |
| Dedicated-server checks | Earlier checks confirmed; beta 3 startup/save/restart retest pending |
| Shaders enabled/disabled comparison | Confirmed by pack owner |
| Alpha 41 portal audio with and without VS | Confirmed by pack owner |
| Offhand Point Blank gun with main-hand TARDIS key | Confirmed by pack owner |
| Clean beta build, bytecode, packaging and regression checks | Passed locally and in Actions |

Game results above are user-reported, not automated full-pack simulations.
Optional-mod gates are regression-tested, including no targets, reduced setups,
missing VS/IP and unsupported versions. Repeat affected checks after runtime
changes. Log warning count alone is not a release criterion.

Beta 1 freezes the current supported feature set. Exact supported versions,
installation instructions and known limitations remain in the README and
[release notes](releases/0.1.0-beta.12.md).

## Move from beta to stable

Run the beta during normal singleplayer and dedicated-server play across several
sessions, including restarts and moving ships. Resolve reproducible regressions,
repeat affected checks, then produce a clean tagged 0.1.0 release. Verify the
published artifact, source link and release notes match that tag. Any remaining
limitation must be documented and must not break the supported core workflow.

Create/Flywheel shader integration and WWOO grid seams do not automatically block
release of the existing compatibility bridges. Unsupported integrations must not
be advertised as supported. The pack owner accepts the older Amendments,
IronChest and BetterEnd cosmetic warning candidates as low priority.

## Wishlist / forward thinking

- Destination DH LODs beyond five chunks through portals: [isolated prototype](portal-destination-lods.md). Runtime integration and GPU validation remain pending.

- **Load and render the exterior ship through the TARDIS entrance before
  stepping out**, including opening from inside after the doors were closed or
  after a new entrance portal was created. Support stationary and moving ships
  without permanent global chunk tickets, movement freezes or teleport loops.
  This remains a known limitation and future work, not a beta/stable blocker.
- Create/Flywheel GPU rendering with Iris shaders and Immersive Portals, if a
  supported shader bridge can be implemented and tested.
- Revisit WWOO / Distant Horizons grid seams only with a demonstrated fix whose
  generation cost is acceptable; the alpha 34 border experiment remains removed.

## Beta 13 accessory grave checks

With Aether and Player Graves installed on client/server: die with functional and
cosmetic accessories, retrieve them via GUI and quick retrieval, and confirm no
world drops or duplicate equipped items. Test accessories without normal inventory
or XP, a full respawn inventory, keepInventory/keepAccessoryInventory, KEEP and
DESTROY rules, protected graves, and save/restart before recovery. For a full grave
screen, take items and reopen to recover remaining stacks. Native API and fixture
checks pass; gameplay/save-restart confirmation is pending. Publish a release
only after these checks.

## Beta 14 original accessory-slot checks

Create a new grave with two rings and cosmetic accessories. Crouch-right-click
with exact inventory-layout recovery enabled; verify each item returns to the
same functional/cosmetic slot and native equipment effects/client rendering update.
Repeat after server save/restart and after taking some items via the grave GUI.
Equipped respawn items must remain intact; occupied or unavailable original slots
use ordinary recovery, retaining items in the grave if inventory is full and
overflow drops are disabled. Disable exact-layout recovery and confirm ordinary
inventory retrieval. Beta 13 graves remain inventory-only because their original
slot identities were not saved. Automated checks pass; gameplay remains pending.

## Beta 17 Trinkets grave checks

With Trinkets and Player Graves installed: equip items in several Trinkets slots (cape elytra,
ring, charm) and die with keepInventory off, then on. Confirm every trinket appears in the
grave and the slots are empty after respawn; recover with quick retrieval and the GUI. Die
carrying only trinkets (empty inventory) and confirm a grave is created. Equip a vanishing-curse
trinket and confirm it is destroyed and not graved. Run gravesNativeCheck against the supplied
jars (-PgravesJar, -PgravesAccessoriesJar, -PgravesMinecraftJar, -PtrinketsJar) before publishing.

## Beta 24 Mac diagnostic check

With BSL enabled, reproduce a portal visible through another portal. Capture
`[Fan4Compat PortalDepthDiag]` and IP's compatibility-mode chat message in
`latest.log`. A successful initial copy proves the hook ran but does not prove
every later nested layer works. A failure reports the original GL error and the
source/destination depth representations. The observer preserves native fallback;
it does not itself enable nested rendering or fix a failed copy.
