# Mac nested portals with Iris: investigation and workaround

Updated: 2026-10-10 (America/Chicago). Current implementation: **beta 26** (beta 25 portal workaround retained).

This is the maintained investigation record for shader-enabled nested Immersive
Portals rendering on macOS. Update this document whenever evidence, implementation
or gameplay results change. Keep observed failures, suspected causes, automated
validation and actual GPU results separate.

## Current status

**Beta 25's Mac log confirms the separate attachment layout and a successful
layer-0 depth copy. No compatibility fallback appears in the supplied capture.
Visible nested-portal rendering and the broader gameplay checklist still need
pack-owner confirmation.**

The affected setup is Apple M2 Pro, Apple OpenGL 4.1 Metal - 91.7, Minecraft
1.21.1, Iris 1.8.1+mc1.21.1 and BSL_v10.1.5. The supplied IP jar is named
`immersive-portals-6.0.7-mc1.21.1-fabric.jar`; the compatibility gate uses its
supported metadata version, `6.0.6`. Other Mac GPUs and shader packs have not been
established as affected or fixed by this evidence.

The symptom is a portal visible through another portal disappearing with shaders
on, followed by IP switching to compatibility mode. Ordinary single-level portals
can still render in that mode. Shader-disabled rendering uses a different renderer.

## What depth and stencil do

Depth records which surfaces are in front of others. Stencil supplies a mask that
limits rendering to a portal's visible opening. Recursive portal rendering needs
both, but they can be stored either together in one packed image or in separate
attachments to a framebuffer.

A framebuffer being complete means its attachments form an accepted rendering
target. It does not prove that a particular copy between two complete framebuffers
is valid. Depth precision matching alone also does not prove that their storage
representations are compatible with the driver's copy operation.

## Evidence from beta 24

The pack-owner log recorded the following at 15:29:00 on 2026-10-10:

| Property | Source | Destination |
| --- | --- | --- |
| Framebuffer ID | 1 | 22 |
| Completeness | GL_FRAMEBUFFER_COMPLETE (36053) | GL_FRAMEBUFFER_COMPLETE (36053) |
| Depth bits | 32 | 32 |
| Depth component type | GL_FLOAT (5126) | GL_FLOAT (5126) |
| Stencil bits in depth image | 0 | 8 |
| Stencil attachment | None | Same texture as depth |

The depth-copy operation returned **GL_INVALID_OPERATION (1282)** at portal layer
0 while the render mode was still normal. IP immediately logged:

> Switched to compatibility portal rendering mode. Portal-in-portal wont' be rendered

The next recorded copy also failed at layer 1, and the log then selected
`IrisCompatibilityPortalRenderer`.

This directly establishes the operation that triggers fallback. The depth-only
floating-point source versus packed floating-point depth/stencil destination is
the leading explanation for the rejected copy. The diagnostic bit/component
queries establish those representations; they do not independently identify every
possible driver restriction, such as sample-count differences. A successful beta
25 reproduction will provide stronger evidence that separating the attachments
addresses the actual cause.

### Shader warnings are a separate issue

The shader compiler also reports unsupported extension declarations such as
`GL_ARB_shader_texture_lod` and `GL_ARB_shader_image_load_store`, plus unused or
missing shader attributes. These are compiler/linker warnings in this log; BSL
continues loading. IP's observed fallback is triggered by the depth-copy GL error,
not by parsing those warning messages. Suppressing warnings or pretending the Mac
supports an extension would not repair this copy.

## How IP reaches compatibility mode

With shaders enabled, IP selects `IrisPortalRenderer`. It allocates a secondary
framebuffer for each portal layer and enables stencil support on those targets.
In `doMainRenderings(Matrix4f)`, it binds the source and destination framebuffers,
blits depth with nearest filtering, then reads `glGetError()`.

A nonzero error causes IP to set `IPGlobal.renderMode` to compatibility and print
the chat message. `IrisCompatibilityPortalRenderer` trades recursive rendering for
a simpler path. Fan4Compat preserves this error check and fallback.

One naming trap matters: IP's `useSeparatedStencilFormat` flag selects between
packed `DEPTH24_STENCIL8` and packed `DEPTH32F_STENCIL8`. It does **not** mean that
depth and stencil are separate images. The earlier precision-selection fix could
therefore match 32-bit floating-point depth while still leaving the problematic
packed destination representation.

## Relevant implementation history

| Build | Change | Verification status |
| --- | --- | --- |
| Beta 10 | GL3 framebuffer-copy fallback where `glCopyImageSubData` is unavailable | Automated checks; broader Mac rendering still required |
| Beta 11 | Apple depth-format selection and recursive buffer recreation when the choice changes | Automated checks; beta 24 demonstrated a remaining copy failure |
| Beta 24 | Observe IP's existing depth-copy error and attachment representations | Mac log captured the failure and immediate fallback |
| Beta 25 | Use depth-only float textures with separate stencil renderbuffers for affected Apple portal targets | Clean local build, native checks and Actions passed; Mac gameplay pending |

These changes address different stages. A GL3 copy fallback does not automatically
solve the recursive renderer's own depth blit, and selecting matching precision
is different from selecting matching storage representation.

## Beta 25 workaround

The patch redirects stencil setup inside `IrisPortalRenderer.prepareRendering`.
It calls the original IP/Porting Lib setup first, then checks the active GPU vendor
and the main depth texture's actual format.

The separate layout is attempted only when:

- Supported IP and Iris versions are installed.
- The vendor string identifies Apple.
- The main source resolves to `DEPTH_COMPONENT32F` (DEPTH32F).
- The secondary target currently uses a recognized packed depth/stencil format.

For that secondary target, the helper detaches the packed attachment, reallocates
its existing depth texture as DEPTH32F, attaches it as depth only, allocates a
same-size `STENCIL_INDEX8` renderbuffer, and attaches that as stencil. The texture
identity is retained, so native code continues referring to the same depth texture.
IP's native layer clears, stencil masks and depth-copy logic remain responsible
for rendering.

The patch does not convert the main framebuffer. Non-Apple GPUs and source formats
outside this path retain native allocation. If the main format changes away from
DEPTH32F, a previously managed layer is rebuilt through native resize/allocation.

### Resource ownership and rollback

Resources are tracked by framebuffer object identity, texture ID and dimensions.
A successful layout is reused rather than allocating each frame. Minecraft's
framebuffer deletion hook releases the owned stencil renderbuffer; native resize,
shader reload and normal deletion go through that lifecycle.

If framebuffer completeness rejects the separate layout, the helper restores the
original packed texture storage and attachment, deletes the new renderbuffer, and
records that target as rejected. It avoids retrying every frame; native recreation
clears the record. The driver may legitimately reject separate depth/stencil
attachments even though it accepts packed storage.

Read/draw framebuffer, texture and renderbuffer bindings restore in `finally`,
including rollback after an allocation exception. Allocation exceptions are not
silently converted into successful setup. The patch leaves IP's original GL-error
fallback intact and never forces recursive rendering after a failed copy.

## Diagnostics and interpretation

`[Fan4Compat PortalDepthDiag]` observes the return value of IP's existing
`glGetError()` call. It returns that same value to IP, consumes no additional GL
errors, and changes no bindings. It reports one successful copy and up to eight
failed copies per launch. Snapshot or logging failure disables the observer without
interrupting IP's renderer.

Reports include the error name/value, GPU/OpenGL identity, portal layer and maximum
layers, render mode before fallback, stencil-format choice, main target dimensions,
both framebuffer statuses, attachment types/object IDs, depth/stencil bit sizes and
depth component types. This is bounded evidence collection, not an error suppressor.

Disable the observer with the JVM argument:

```text
-Dfan4compat.portalDepthDiagnostics=false
```

This excludes the diagnostic mixin; the rendering workaround remains enabled.
Point Blank's separate diagnostics remain off by default as of beta 23 and use a
different flag, `fan4compat.renderDiagnostics`.

The workaround also emits a one-time `[Fan4Compat PortalDepth]` setup message:

- `enabled`: the attempted separate framebuffer layout passed completeness.
- `rejected; restored native packed attachment`: the attempted layout failed
  completeness, so the helper restored native storage.

A setup message alone does not prove the subsequent copy or every nested layer
works. A depth-copy `OK` message proves that sampled copy succeeded, not that all
later portal views are correct. If there is no diagnostic report, check the installed
build, supported metadata versions, diagnostic flag, active renderer and whether
portal depth copying was actually reached.

## Code and verification map

| Responsibility | Source |
| --- | --- |
| Earlier depth precision selection | [DepthFormatCompat.java](../src/main/java/dev/fan4/compat/iris/DepthFormatCompat.java) |
| GL3 helper copy fallback | [FramebufferCopyCompat.java](../src/main/java/dev/fan4/compat/iris/FramebufferCopyCompat.java) |
| Bounded depth-copy diagnostics | [PortalDepthDiagnostics.java](../src/main/java/dev/fan4/compat/iris/PortalDepthDiagnostics.java) |
| Separate attachment ownership/allocation | [SeparateStencilCompat.java](../src/main/java/dev/fan4/compat/iris/SeparateStencilCompat.java) |
| Generated setup, diagnostic and cleanup hooks | [FramebufferCopyGenerator.java](../tools/iris/FramebufferCopyGenerator.java) |
| Optional activation gates | [CompatibilityRules.java](../src/main/java/dev/fan4/compat/shared/CompatibilityRules.java) |
| Diagnostic regressions | [PortalDepthDiagnosticsTest.java](../src/test/java/iris/PortalDepthDiagnosticsTest.java) |
| Allocation/lifecycle regressions | [SeparateStencilTest.java](../src/test/java/iris/SeparateStencilTest.java) |
| Real IP/Minecraft selector checks | [PortalDepthDiagnosticsNativeCheck.java](../tools/iris/PortalDepthDiagnosticsNativeCheck.java) |

Generated hooks are `IrisPortalDepthDiagnosticMixin`, `IrisSeparateStencilMixin`
and `IrisStencilCleanupMixin`. They live under the Iris client compatibility group
in the mixin manifest. This integration requires neither VS, DH nor Point Blank.
The cleanup hook observes framebuffer deletion globally but acts only on resources
owned by this helper.

The clean build runs helper regressions, optional-mod gates, bytecode verification,
packaging checks and feature reachability auditing. Native checks additionally
verify the real IP copy/error/fallback path, the setup redirects and Minecraft's
framebuffer deletion selector. They do not exercise Apple's GPU driver or prove
live nested rendering works.

For native checking, use the existing Gradle task with actual intermediary jars:

```text
./gradlew clean build portalDepthDiagnosticsNativeCheck \
  -PportalDepthDiagnosticsJar=/path/to/immersive-portals.jar \
  -PportalDepthMinecraftJar=/path/to/minecraft-intermediary.jar
```

Use the project's JDK 21 build environment. Always perform a clean build when
producing a new test artifact.

## Mac gameplay checklist

Use beta 25 with BSL_v10.1.5 enabled and reproduce the beta 24 scene. Collect the
setup message, depth-copy diagnostics and any compatibility-mode message from the
same launch.

1. View one portal through another and verify destination geometry and clipping.
2. Cross each portal and return; check depth ordering and stencil boundaries.
3. Resize the window and repeat the nested view.
4. Toggle shaders off and on, reload the shader pack, and repeat.
5. Leave and rejoin the world to exercise cleanup and recreation.
6. Check ordinary main-view terrain, water and the gun rendering already confirmed
   working by the pack owner.
7. Compare a non-Apple client to confirm native rendering remains unaffected.

If the separate layout is rejected, retain the native fallback and investigate
what attachment combinations the driver supports. If it is complete but depth
copy still fails, collect more exact format/sample information before changing the
copy path. If depth copying succeeds but nesting remains wrong, investigate
per-layer stencil copying, composition and Iris state restoration separately.
Do not remove error checks merely to hide the fallback.

## Results and maintenance record

| Date/build | Result |
| --- | --- |
| 2026-10-10, beta 24 | Pack-owner Mac log reproduced depth-copy error 1282 and compatibility fallback at layers 0 and 1. |
| 2026-10-10, beta 25 | Clean local build, regression checks, native selector checks and GitHub Actions passed. |
| 2026-10-10, beta 25 Mac log | Separate stencil layout enabled; layer-0 depth copy returned GL_NO_ERROR (0), with complete framebuffers. No compatibility fallback recorded. |
| Beta 25 visual/gameplay checks | Pending explicit confirmation of visible nesting, crossings, resize/reload and rejoin. |

For each future result, add the tested build, exact GPU/driver/shader versions,
reproduction, relevant diagnostic lines, visible outcome and any change to the
working hypothesis. Record partial success as partial success. Update the current
status, implementation description, README, progress notes and affected release
notes when behavior changes. Keep this document as the primary investigation
record and link to it instead of duplicating detailed explanations elsewhere.

### Beta 25 Mac log result

At 19:00:31 the same Apple M2 Pro setup selected `IrisPortalRenderer` and
reported separate float-depth/stencil storage enabled. At 19:00:33 the observed
layer-0 copy returned `GL_NO_ERROR (0)` in normal mode. The source had 32-bit float
depth without stencil; the destination had matching 32-bit float depth without
stencil in its depth image and a distinct renderbuffer with eight stencil bits.
Both framebuffers were complete. No failed depth-copy or compatibility-mode switch
was recorded in this capture, which continues through 19:01:19.

This verifies the changed attachment layout and successful sampled operation on
the affected driver, supporting the storage-representation hypothesis. The observer
logs only the first successful copy per launch, so the layer-0 report does not
establish that a visible nested layer was exercised or rendered correctly. Obtain
explicit visual confirmation before marking the full issue resolved.

## XM3 regression on Mac and beta 26 follow-up

The pack owner reports solid terrain disappearing again after using the XM3 on
Mac, with both shaders enabled and disabled. Windows remains reported working.
The supplied beta 25 log includes Point Blank diagnostics. At 19:00:42 gun
preparation changes read/draw bindings from main framebuffer 1 to default
framebuffer 0, and the main target later acquires packed stencil storage. Native
Point Blank code enables scope stencil by resizing the main framebuffer.

Beta 26 scopes both native stencil-enablement calls, preserving independent
read/draw bindings and resolving the replacement ID after resize. It does not
disable scope stencil or the beta 25 portal workaround. It also applies without
Iris/IP, because the reported failure occurs with shaders disabled too. Later gun
draws change Iris targets as well; those changes may be intentional composition
and have not been blindly reversed. The captured binding leak is evidence for
a focused repair, not proof that it explains every missing-terrain symptom.

Retest drawing/aiming the XM3, switching away, main terrain and ordinary/nested
portals with shaders on and off. Keep `fan4compat.renderDiagnostics=true` enabled.
No explicit beta 26 gameplay result has been received. Windows/Mac behavior has
not been established by matched diagnostic runs; the platform-specific cause
remains uncertain.
