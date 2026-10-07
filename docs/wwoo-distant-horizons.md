# WWOO / Distant Horizons grid investigation

Inspected: WWOO Fabric 2.6.7 and Distant Horizons 3.3.3 for Minecraft 1.21.1.
The user's logs include DH's explicit warning about WWOO LOD section grid lines.

## Evidence and candidate fix

WWOO contains cross-chunk terrain decoration, including vanilla disk features.
DH's `DhChunkGenerator.generateChunks` reads
`MAX_WORLD_GEN_CHUNK_BORDER_NEEDED`, initialized to zero in the inspected build.
It constructs four overlapping odd-sized generation regions for an even-sized
request and returns only the original requested chunks. StepFeatures generates
features for the region's chunks, with block writes constrained to that region.
An absent neighbour context is a plausible cause of cut-off terrain decoration;
it has not been established as the cause of every visible grid artifact.

Alpha 34 redirects only the per-event border read: zero becomes one for
Overworld FEATURES requests whose target step is FEATURES. Existing nonzero
padding is preserved. Both mod versions are gated before applying the mixin.
The native region allocation and feature pipeline do the remaining work. No
static border field, worldgen data pack, real-world chunk loading API, or user
DH configuration is changed. The extra chunks are transient generation context.
The original output coordinates and callback area remain native.

This increases generation work. For a 16-chunk-wide request, each of the four
native regions grows from 15 x 15 to 17 x 17 slots, about 28% more slots before
reuse or existing-chunk shortcuts. Smaller requests have a higher relative cost;
this is not a measurement of CPU time. Full Minecraft chunk generation and
permanent chunk tickets are not enabled.

## Validation

The standalone checks execute the actual helper, verify the exact native field
selector and reflected fields, and execute an extracted copy of DH's native
allocation math. A deterministic radius-seven cross-border disk fixture compares
the retained interior against a larger reference area, including negative chunk
coordinates. It reproduces clipped edges without overlap and matches the
reference with one ring. These checks do not run Minecraft or prove the whole
WWOO feature set matches normal world generation.

In the user's pack:

1. Install alpha 34 on the client and the server performing DH generation. Keep
   the same DH generator plan and FEATURES chunk generator mode.
2. Compare newly generated LODs from a high viewpoint in a fresh test world using
   the same seed and data packs. Check section boundaries with shaders both on
   and off, then approach the terrain to compare against real chunks.
3. Compare the same scene after restarting with
   `-Dfan4compat.wwooBorder=false`. Compare generation throughput as well.
4. Existing cached LODs are not invalidated automatically. Test fresh data first;
   rebuilding a cache is a separate operation, not part of this addon patch.

If the same grid remains in fresh LODs with the border enabled, record whether
it is missing terrain/vegetation, color bands, lighting, or geometry cracks.
Compression, approximate surface generation and shaders can also produce grid
patterns and require a separate diagnosis. DH's generic WWOO warning remains;
no warning is suppressed before runtime validation.
