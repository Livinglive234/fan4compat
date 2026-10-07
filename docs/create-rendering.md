# Create, Flywheel, Iris and Immersive Portals

Current investigation: 2026-10-07. This remains an open compatibility goal.

## Versions examined

- User's Fabric Create: `6.0.10+mc1.21.1-build.184`, with source in
  [Livinglive234/createfabric](https://github.com/Livinglive234/createfabric).
- Flywheel: `1.0.6-44`.
- Iris: `1.8.1+mc1.21.1`, with an active shader pack.
- Immersive Portals: internal version `6.0.6`.

## What the fallback establishes

Flywheel's `Backends` registers both INSTANCING and INDIRECT with a support
predicate containing `!ShadersModHelper.isShaderPackInUse()`. An active Iris
shader pack makes these GPU backends unavailable. This explains the backend
falling back to OFF; it does not depend on TARDIS console geometry.

OFF means Create must use its ordinary rendering path. It does not, by itself,
establish that a particular contraption is invisible. The IP incompatibility
warning also does not identify which renderer or portal view fails. The user
has not yet identified a specific missing contraption in this setup.

## Existing shader bridge

[Iris & Oculus Flywheel Compat](https://github.com/leon-o/iris-flw-compat)
([Modrinth project](https://modrinth.com/mod/iris-flw-compat)) supplies an existing
bridge to examine. The published Minecraft 1.21.1 versions inspected on Modrinth
are NeoForge builds, rather than a matching Fabric build for these pinned mods.
They cannot be installed as a Fabric fix.

A Fabric bridge needs to integrate Flywheel's generated shaders with Iris's
shader programs, render targets, uniforms and shadow pass. IP adds nested world
rendering, portal clipping and world-specific rendering state. Removing the
backend's shader check would bypass these requirements and is not a rendering
bridge. Fan4Compat does not force an unsupported backend.

## Next implementation targets

1. Reproduce the ordinary Create render path with a stationary contraption and
   a moving contraption, in direct view and through a portal, with shaders both
   enabled and disabled. A specific failure determines whether the next change
   belongs to Create's ordinary renderer, IP world rendering or a shader bridge.
2. Audit the existing shader bridge's reusable Flywheel/Iris integration against
   Flywheel 1.0.6-44 and Iris 1.8.1. Port the platform hooks to Fabric and preserve
   their licensing if code is reused.
3. Verify each nested portal render restores shader targets and clipping state,
   and that shadow passes do not reuse another world's instance lists.

Alpha 31 contains waypoint, biome-name and contrail changes. It does not claim
Create shader backend compatibility and leaves IP's warning intact.
