# Remaining log follow-ups

Reviewed through **0.1.0-beta.33**, 2026-10-11. The latest Windows and Mac
captures end normally; the pack owner confirms the DH overlay and Mac rendering
symptoms are gone. The beta 33 Windows capture retains DH depth attachments,
and the Mac capture reports successful portal depth copying. This does not mean
every warning in the pack is fixed.

Recent captures still contain Macaw's/Biomes O' Plenty model-property warnings,
Create/Flywheel backend fallback, an invalid IronChest resource path, empty
Moonlight/Porting Lib registry notices and a stale update-version list. These
need a demonstrated functional symptom before another runtime patch. The stale
third-party update check is separate from Fan4Compat's release updater.

One Mac `Raycast too far` error came from IP's portal block-targeting path before
shaders were enabled. It was nonfatal; reproduce a targeting/interaction failure
before diagnosing it as an unresolved gameplay defect. An earlier native SQLite
crash was not reproduced in these normal exits; its cause remains unconfirmed.

The following older alpha-log candidates remain historical follow-ups, not proof
that they persist in beta 33. The pack owner accepts cosmetic/resource issues as
low priority. ChestTracker remains explicitly excluded from this patch scope.

| Candidate | Observed evidence | Next useful check |
| --- | --- | --- |
| Amendments model loading | Missing loaders for wall_lantern, carpet_overlay, cauldron, hanging_pot, waterlogged_lily and tool_hook; vanilla fallback used | Check those models visually and audit Amendments' Fabric model-loader registration against the installed Porting Lib. |
| Model format compatibility | Missing forge:separate_transforms and neoforge:obj loaders while porting_lib equivalents are registered | Identify the owning model resources before choosing loader aliases or resource fixes; prefixes alone do not prove compatible formats. |
| IronChest texture | Invalid crystal_chest (semi-transparent).png resource path is ignored | Inspect the model references and texture availability; repair the resource naming if it actually affects the model. |
| BetterEnd disc textures | Four music-disc texture generation failures fall back to generic textures | Cosmetic follow-up: inspect the exact remastered disc resources and texture generator. |
| Create/Flywheel/Iris/IP | GPU backend fallback with active shaders; generic IP incompatibility warning | Existing shader-bridge investigation remains open. Fallback alone does not prove invisible contraptions. |

The missing-reference-map/optional-mod-target warnings, absent data fixers, update
check failures and Iris deprecated-API notices do not by themselves establish a
runtime failure needing an addon patch. Do not suppress them or remove checks
merely to make the log quieter. ChestTracker failures remain explicitly excluded.

Earlier movement, recipe, acoustic, Accessories and DWM model issues have already
received patches. User testing confirms moving-ship exits and other TARDIS
functions; older errors are not a reason to add another fix without recurrence.
WWOO grid artifacts remain unresolved and the experimental border was withdrawn.
