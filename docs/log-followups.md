# Remaining log follow-ups

Reviewed 2026-10-07. Current release: alpha 35. The newest supplied crash excerpt
is the alpha 31 waypoint selection stack overflow, addressed in alpha 32. The
most recent full startup log reviewed here is alpha 28; its warnings are evidence
of unresolved candidates, not proof that every issue persists in alpha 35.
The pack owner accepts the resource/model candidates below as low priority;
they are not release blockers without a demonstrated functional problem.

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
