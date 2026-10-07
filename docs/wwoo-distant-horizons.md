# WWOO / Distant Horizons grid investigation (withdrawn experiment)

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

## Experiment withdrawn in alpha 35

The pack owner requested removal because visual benefit had not been established
and the extra generation work was not justified. Alpha 35 removes the helper,
mixin, generator, version gate and experiment-specific tests. No WWOO generation
patch is active. The `fan4compat.wwooBorder` switch no longer does anything.

Alpha 34 passed standalone checks of the border scope, native allocation math
and a simplified cross-border feature fixture. Those checks did not run
Minecraft or prove that the reported full-pack grid lines would disappear.
WWOO / DH grid seams remain unresolved; no LOD cache or DH user configuration
was changed during either implementation or removal.
