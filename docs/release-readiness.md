# Release readiness and wishlist

Current build: **0.1.0-beta.3**. Fixes dedicated-server VS helper resolution and detached TARDIS console saves.

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
[release notes](releases/0.1.0-beta.3.md).

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

- **Load and render the exterior ship through the TARDIS entrance before
  stepping out**, including opening from inside after the doors were closed or
  after a new entrance portal was created. Support stationary and moving ships
  without permanent global chunk tickets, movement freezes or teleport loops.
  This remains a known limitation and future work, not a beta/stable blocker.
- Create/Flywheel GPU rendering with Iris shaders and Immersive Portals, if a
  supported shader bridge can be implemented and tested.
- Revisit WWOO / Distant Horizons grid seams only with a demonstrated fix whose
  generation cost is acceptable; the alpha 34 border experiment remains removed.
