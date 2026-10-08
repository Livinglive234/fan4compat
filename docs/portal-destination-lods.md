# Destination LODs through portals: isolated prototype

Requested behavior: limit normal destination terrain to five chunks and draw
Distant Horizons terrain beyond that radius, including distant Aether islands.
This is investigation/prototype work. The live beta 7 portal suppression remains
active; this commit does not enable destination rendering or change IP config.

## Verified native constraints

Checked DH 3.3.3, IP's internal version 6.0.6 (supplied 6.0.7 filename), and
Minecraft 1.21.1 bytecode.

- IP exposes indirectLoadingRadiusCap. Its ordinary portal loader consults that
  cap; scaling/global portals have additional rules. A global setting alone does
  not guarantee an exact five-chunk draw radius for every portal type.
- WorldRenderInfo.getRenderDistance() supplies a portal view's terrain distance.
  A scoped five-chunk override can leave the main view unchanged.
- DH ClientApi owns shared RENDER_STATE and RENDER_PARAMS, including mutable
  matrices and shader/depth state. Removing the existing guard reintroduces the
  wrong-world rendering bug unless those resources are isolated and restored.
- DH's IP accessor saves the actual main world/camera. Destination rendering needs
  an explicit destination wrapper and camera, with no substitutions in unrelated
  player, network or background-generation code.
- DhClientLevel.clientTick() and DhClientServerLevel.clientTick() select the
  player's current DH level. Offscreen destination data needs separate preparation.
- Remote destination network requests need a separate design preserving server
  authorization. Cached destination LODs are the initial scope; a fresh destination
  with no available data must retain its normal terrain coverage.
- DH's native GL terrain fragment shader has camera-relative vertexWorldPos.
  A destination clip plane can be translated into that coordinate system. Its
  framebuffer compositing, stencil masking and shader compilation still need
  actual GPU validation.

## Concrete runtime proposal

Start with shaders disabled, one ordinary unscaled portal layer, and available
cached destination LOD data. Scope the destination world, camera, terrain radius
and matrices to that draw. Use separate destination depth/color resources and
restore the main render state in finally, including exceptions. Disable temporal
history for the portal view, clip to its exit plane, and preserve stencil masking
when compositing. Keep beta 5 shader guards for Iris/BSL until their independent
pipeline/depth resources are implemented and tested.

Apply the five-chunk terrain limit only when the destination LOD renderer is ready.
Do not reduce coverage for unavailable data, shader views, mirrors, scaled portals
or unsupported versions. Defer permanent config writes until that readiness can
be guaranteed; changing the cap first can expose empty distant terrain.

## Validation completed

portalLodPrototypeTest exercises CPU-only world/camera/depth ownership, matrix
copies, nested scope restoration, exceptions, low-distance preservation, the
five-chunk policy, missing-data/shader fallbacks and clipping-plane translation.
The prototype lives in tools and is excluded from the addon jar. It does not
exercise Minecraft, compile GLSL or validate GPU/framebuffer behavior.

## Integration blocker

Automatic approval review rejected the proposed runtime wiring because it combined
numerous untested rendering mixins, reflective state mutation and shader rewriting.
No rejected wiring was applied. The isolated prototype is a safer alternative and
a reviewable basis for approval of runtime integration. Main-world LODs and current
portal shader guards remain untouched.
