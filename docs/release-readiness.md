# Release readiness and wishlist

Current build: **0.1.0-beta.13**. Includes an additional Iris DH depth-sampler guard, server save fixes, complete Eureka
notice filtering and DH/Iris portal-render guards. The pack owner confirmed the BSL_v10.1.5 portal fix working. Initial LOD
loading can take longer; subsequent loads behave normally. Beta 6 removes
temporary movement diagnostics and unused collision-query code.
Both client and server should run this build for Eureka server notices.
Beta 7 adds the Freecam tick-boundary fix; beta 8 adjusts nearby LOD clipping only
for its active camera in unloaded terrain. The pack owner reports beta 8 failed
with BSL only; beta 9 targets BSL 10.1.5 terrain/water distance discard. In-game
confirmation remains pending. Test approaching distant LODs, backing
away, returning to loaded terrain, disabling Freecam and normal player movement.

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

## Confirmed release checks

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
checks pass; gameplay/save-restart confirmation is pending. Publish the draft
release only after these checks.
