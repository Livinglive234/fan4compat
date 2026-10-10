# Xaero integration: design proposal

**Status: proposal. Nothing here is implemented, gated or shipped.** Written
2026-10-10 against the pack's Xaero versions. Three features are proposed
(A and B for dedicated servers, C for the TARDIS); each needs the pack owner's
approval before any code.

| Mod | Pinned version | Mod ID |
| --- | --- | --- |
| Xaero's Minimap (Fabric 1.21.1) | `26.5.0` | `xaerominimap` |
| Xaero's World Map (Fabric 1.21.1) | `1.46.0` | `xaeroworldmap` |

Both are "All Rights Reserved": Fan4Compat may hook them with generated Mixins,
as it does other mods, but must not copy or bundle their code. Like other
integrations, the hooks would be gated on these exact metadata versions in
`CompatibilityRules` and skipped (with a startup warning) for other versions.

## Contents

- [What was verified in Xaero](#what-was-verified-in-xaero)
- [Feature A: server-stored waypoints](#feature-a-server-stored-waypoints)
- [Feature B: shared explored map](#feature-b-shared-explored-map)
- [Feature C: exterior dimension on the map inside a TARDIS](#feature-c-exterior-dimension-on-the-map-inside-a-tardis)
- [Shared infrastructure](#shared-infrastructure)
- [Configuration](#configuration)
- [Risks and pack-specific concerns](#risks-and-pack-specific-concerns)
- [Testing plan](#testing-plan)
- [Open questions and spike items](#open-questions-and-spike-items)

## What was verified in Xaero

Decompiled Minimap 26.5.0 and World Map 1.46.0, and compared the entry points
below with Minimap 26.6.0 and World Map 1.47.0. The public signatures examined
(`WaypointIO`, `MapWriter`) were identical across those versions. That suggests
these hook points are stable, but it does not prove other internals are.

**Minimap waypoints**

- `xaero.hud.minimap.waypoint.io.WaypointIO` reads and writes a line-based text
  format: a `sets:<current>:<set>:...` line, then one line per waypoint:
  `waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination`.
  The format has no per-waypoint ID or timestamp.
- `xaero.hud.minimap.world.io.MinimapWorldManagerIO` owns the files. Waypoints live
  under Xaero's waypoint folder (`getMinimapFolder()`), in a folder named for the
  server/world root, then per-dimension folders, in `<name>_<multiworld>.txt`
  files. Entry points: `loadWorldsFromAllSources(session, connection)`,
  `loadAllWorlds`, `loadWorld(world, path)`, `saveWorld(world[, overwrite])`,
  `saveWorlds(container)`, `saveAllWorlds(session)`, `getWorldFile(world)`.
  Xaero keeps its own backup folder. The exact on-disk layout was read from
  code, not observed on a real install.
- `ServerWaypointManager` / `ThirdPartyWaypointManager` provide temporary,
  externally injected waypoints. They are not saved or user-editable and are the
  wrong mechanism for personal waypoints.
- Xaero's local waypoint folder is keyed by the server address as the player
  typed it. Different PCs using different addresses get separate folders.

**World Map**

- `MapWriter.writeChunk(...)` converts a loaded world chunk into a `MapTile`:
  16 x 16 `MapBlock` columns, each holding a block state, `height`, `topHeight`,
  `biome`, slope data and overlays (such as water). Colors are derived on the
  client from block textures (`loadBlockColourFromTexture`).
- A `MapTileChunk` is 4 x 4 tiles (64 x 64 blocks) and is the unit saved and drawn.
- `file/worldsave/WorldDataHandler.prepareSingleplayer` reads region files, but
  only through the integrated server. There is no equivalent for a dedicated
  server.
- Xaero's own packets are handshake, rules and tracked-player packets only
  (`MinimapPacketRegister`, `WorldMapMessageRegister`). There is no transport for
  map or waypoint data.

## Feature A: server-stored waypoints

Goal: a player's Xaero waypoints follow them between PCs.

### Mechanism: sync the waypoint files, keyed by player

1. **Upload.** After Xaero saves a world's waypoint file (`saveWorld`), a client
   hook sends that file's text to the server in a Fan4Compat packet.
2. **Store.** The server keeps one copy per player UUID and per Xaero world/dimension
   file, with a version counter and a timestamp, in Fan4Compat's own folder inside
   the world data. Enforce a maximum size per file and per player, and a rate limit.
3. **Download.** On join, the server sends the stored copies. A client hook writes
   them before Xaero's `loadWorldsFromAllSources` reads the folder. If the local
   file differs, the overwritten file is first copied to a backup.
4. **Key by player, not by address.** The client maps whichever local Xaero folder
   it is using for this server to the server's single store, so two PCs that
   connect through different names still share waypoints.

Players without Fan4Compat on both sides are unaffected; Xaero keeps working
locally for them.

### Conflict policy

The text format has no waypoint IDs or timestamps, so a real three-way merge is
not possible. Proposed default is **last writer wins per file**, using the server's
version counter. A client whose local file was changed since its last sync and
whose server version is also newer keeps a timestamped backup of the loser.
A union-merge mode (key: set, name, x, y, z) is a possible later option; it never
loses a waypoint but cannot propagate deletions, so deleted waypoints return.

### Notes

- Death points and waypoint sets are part of the same file and round-trip as text.
- Fan4Compat's existing TARDIS ship waypoints are Doctor Who Mod data stored in
  DWM's own records. They are unrelated to Xaero waypoints.
- Server admins can read stored waypoints on disk. Document this.
- No-code alternative: syncing Xaero's waypoint folder between PCs (Dropbox,
  Syncthing, or a symlink) gives the same effect without the mod, at the cost of
  per-PC setup.

## Feature B: shared explored map

Goal: terrain a player has explored appears on other players' World Maps, including
areas those players have not visited. "Explored" means chunks sent to a real player
by the server, not Distant Horizons LODs and not pregenerated chunks nobody has seen.

### Mechanism: the server records and relays column summaries

1. **Capture (server).** When a chunk is sent to a player, read a compact summary of
   each of its 256 columns: top block state, surface height, biome, and fluid
   depth. This uses ordinary chunk and heightmap lookups on the server thread.
2. **Store.** Per dimension, in Fan4Compat's folder in the world data, with a
   version stamp per chunk so later changes can be sent as updates. Re-capture
   when a chunk is unloaded or changed, throttled.
3. **Relay.** On join, send the player the chunks they are missing (a catch-up
   stream, throttled, nearest or oldest first). Afterwards, send new exploration
   incrementally to other players.
4. **Apply (client).** A World Map hook builds a `MapTile` from a summary and inserts
   it into the correct `MapTileChunk`/region without overwriting tiles the player
   has actually seen themselves. Real chunks the client loads always take priority.

Why server capture rather than client uploads: it does not depend on trusting
client-supplied data, and it needs no Xaero internals on the server.

### Size (estimates, not measurements)

About 1.3 KB per chunk summary raw, probably 400-600 bytes compressed. A full
64 x 64-block region is roughly 10 KB. Catch-up for a large explored world could
reach several MB, hence throttling.

### Scope limits

- World Map only. The corner Minimap is built from nearby loaded chunks; whether it
  would show shared tiles at all is unverified and probably not.
- Detail is limited to the surface summary; no cave layers.
- A shared map reveals terrain players have not explored personally. Provide
  config switches (see below); consider defaulting to off.

## Feature C: exterior dimension on the map inside a TARDIS

Goal (as understood; confirm with the pack owner): while a player is inside the
TARDIS, the map shows only the dimension (and position) the TARDIS exterior is
currently in, not the interior dimension.

### What Xaero already provides (World Map 1.46.0)

- `MapWorld` tracks `currentDimensionId`, `futureDimensionId` and a
  `customDimensionId`. `isUsingCustomDimension()` is true when the displayed
  dimension differs from the player's actual level, so showing another dimension
  is an existing concept (used for cave-mode logic and the dimension switcher).
- `MapProcessor.ignoreWorld(level)` skips levels whose dimension path matches a
  hard-coded list (currently one entry). A gated hook could add DWM TARDIS
  dimensions. Whether Xaero currently creates a separate map for each TARDIS
  dimension is not verified.

### Mechanism

1. **Server.** A small packet tells the client, while it is in a TARDIS dimension,
   the exterior's current dimension and world position (and whether the TARDIS is
   landed or in flight). Fan4Compat's `TardisShipCompat` already reads the current
   exterior dimension/position and, for ships, the transformed world position.
   The server must apply DWM's own access rules before revealing the location to
   a player, and send nothing during flight.
2. **Client, World Map.** Set the displayed dimension to the exterior's, center the
   view and draw the player marker at the exterior position, then restore the
   normal behavior on leaving. Inside the TARDIS dimension, never write
   interior chunks into the exterior dimension's map.
3. **Interior dimensions are not mapped** (via `ignoreWorld` or an equivalent hook).
4. **Moving ships.** The exterior position follows the ship, so updates must be
   sent as it moves, throttled.

### Extension: following the TARDIS during travel

While a TARDIS is flying there is no real exterior; Doctor Who Mod dematerializes
it and rematerializes it at the destination. A "following" map would therefore be a
**synthetic path**, an animation between the departure and the destination, not
tracking of a real moving object.

What the code already exposes (from `TardisShipQol`): the flight object has a
`step` (the travel phase is `PROCESSING`), a `tick` counter and a `duration`; the
TARDIS state gives the current and destination exterior dimension and position.
Duration is a fixed overhead (64 ticks, plus 300 when the dimensions differ) plus
a part that scales with distance. Today Fan4Compat snapshots the departure point
only for destinations on ships; this feature would need the snapshot for every
flight.

Proposed behavior:

1. At flight start, the server sends departure, destination, tick and duration.
   The client advances a progress value locally and re-syncs on corrections (for
   example when Fan4Compat extends the duration for a moving ship destination).
2. Same dimension: glide the map view and marker from departure to destination,
   easing over the distance-dependent part of the flight.
3. Different dimensions: do not invent a mid-route position. Hold the departure
   view and switch to the destination dimension on arrival (a midpoint switch is a
   possible option).
4. On landing, snap to the actual exterior position, which can differ from the
   requested destination because DWM's landing checks can adjust it. On an
   aborted or crashed flight, snap back to the last real exterior.
5. Moving ship destinations: follow the transformed world position of the ship.

Additional risks: the glide can cross large unexplored or unloaded areas and
force Xaero to load many saved map regions quickly, so travel speed or load rate
may need limiting; the shown route is not the TARDIS's real path; and it reveals
the destination during flight, so the same access rules as the console apply.

### Limits and risks

- **Map pollution is the critical risk.** If the map writer records interior blocks
  at interior coordinates into the overworld's map, the saved map is damaged.
  Whether Xaero's writer already refuses to write in a custom dimension is not
  verified and must be confirmed before anything else.
- **Player position.** Xaero reads the player's real coordinates in many places
  (centering, marker, region loading). Overriding all of them is the main hook
  risk.
- **Minimap.** It draws from chunks around the player and keys waypoints by the
  player's actual dimension. The exterior's chunks are usually not loaded on the
  client, so a faithful minimap is much harder. Start with the World Map and, at
  most, exterior-dimension waypoints and coordinates on the minimap.
- **Flight and no exterior.** While in flight there is no exterior dimension. Without
  the travel extension, show the last known exterior or nothing; do not invent a
  position.
- **Which rooms.** The TARDIS interior dimension contains more than the console
  room; this design triggers on being in the TARDIS dimension. Narrowing to the
  console room needs DWM room data and has not been looked at.
- **Interaction with Features A and B.** B already excludes TARDIS dimensions from
  capture. C would let their maps show the exterior instead. Waypoints stay keyed by the
  real dimension unless a later decision remaps them.

## Shared infrastructure

- One Fan4Compat network channel registered on server and client, with a version
  handshake. If either side lacks it, both features stay inactive.
- A server-side store with atomic writes, size caps and per-player rate limits.
- Version gates in `CompatibilityRules` for `xaerominimap` `26.5.0` and
  `xaeroworldmap` `1.46.0`; each feature gates independently. Generated hooks go in
  `fan4compat.mixins.json` with the usual featureLinkAudit coverage.
- Client-only classes must stay out of the common/server code path, using the same
  exact/typed reflection rules as other integrations.

## Configuration

Proposed, not implemented. Server config file `config/fan4compat-xaero.properties`:

| Setting | Proposed default | Purpose |
| --- | --- | --- |
| `waypointSync` | `true` | Enable Feature A. |
| `waypointMaxBytesPerPlayer` | e.g. 256 KB | Storage cap. |
| `sharedMap` | `false` | Enable Feature B. |
| `sharedMapDimensions` | overworld, nether, end, Aether | Allowlist; excludes dynamic and TARDIS dimensions. |
| `sharedMapCatchUpRate` | low | Throttle for join catch-up. |

## Risks and pack-specific concerns

- **Version coupling.** Both features hook internals that Xaero does not document as
  an API. Gate by exact version and expect re-audits on update.
- **Valkyrien Skies ships.** Ship blocks live in remote shipyard chunks. Exclude
  them from capture (or never map them at raw coordinates).
- **Immersive Portals.** Portals send chunks of other dimensions. Capture only
  chunks in the player's own dimension, keyed by the chunk's actual level.
- **Doctor Who Mod dynamic dimensions** (TARDIS interiors) are excluded by default.
- **Privacy.** Admins can read stored waypoints and map data. A shared map exposes
  explored terrain to all players. Both must be documented and configurable.
- **Abuse and load.** Cap sizes, rate-limit uploads, and throttle catch-up so
  joining players cannot stall the server.
- **Xaero server rules.** Respect Xaero's rules packet, which lets servers disable
  map features.
- **Mod updates.** Saved Fan4Compat data would be additive and separate from
  Xaero's files; removing Fan4Compat must leave Xaero working with its local files.

## Testing plan

Follow the repository's existing pattern: executable regression mains in
`tools`/`src/test`, listed in `build.gradle`.

- Waypoint file round trip, including malformed and oversized input, backup
  creation, and last-writer-wins resolution.
- Column-summary encode/decode, compression, and priority of real tiles over
  shared ones.
- Gate tests: Xaero absent, wrong version, one of the two mods present, Fan4Compat
  absent on one side.
- Native-binding check against the real pinned jars (same approach as
  `pointBlankRenderNativeCheck`) for every hooked Xaero signature.
- Manual: two clients with different server addresses on one dedicated server;
  join/leave catch-up; ship, portal and TARDIS dimensions excluded.

## Open questions and spike items

1. **Feeding World Map without a loaded chunk.** `MapWriter.writeChunk` reads from a
   world chunk. Either build `MapTile`s directly from summaries, or present a
   lightweight stand-in chunk to Xaero's own writer to reuse its exact rendering
   path. This decides how fragile Feature B is.
2. **Where to insert tiles** safely with respect to Xaero's region loading, caching
   and save cycle, and how multiworld/dimension keys map to server dimensions.
3. **Whether the Minimap shows shared tiles** at all.
4. **Exact waypoint folder layout** on a real install, and Xaero's backup and
   autosave timing, to choose the right upload trigger.
5. **Merge policy.** Confirm last-writer-wins is acceptable to players who use two
   PCs concurrently.
6. **Feature C spikes.** Confirm the writer does not write in a custom dimension,
   list every place Xaero reads the player's position, check how DWM exposes the
   TARDIS dimension and exterior state on the client, and see whether Xaero
   already makes per-TARDIS map folders.
7. **Scope.** These are new features, not repairs of an incompatibility between
   two mods. The pack owner decides whether they belong in Fan4Compat or in a
   separate addon.

Recommended order: spike items 1-4 and 6 (read-only), then Feature A (smaller,
mostly file syncing), then Feature C or B depending on priority.
