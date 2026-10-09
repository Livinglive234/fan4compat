# Automatic Fan4Compat updates

Install beta 12 or newer manually once. Only Fan4Compat updates; Minecraft and
other mods/content packs are not changed. Clients and servers check public
releases at startup/every 30 minutes. No GitHub account/token is required.

## Configuration

`config/fan4compat-updater.properties` defaults:

```properties
enabled=true
autoDownload=true
allowPrereleases=true
```

Restart after changing settings. With autoDownload=false, notices link users to
GitHub Releases; there is no download UI button. Disable enabled to opt out.
AllowPrereleases=false selects stable releases only. Checks happen on a daemon
thread; network outages, GitHub rate limits and failed validation do not block play.

## Installation and recovery

Verified downloads and pending metadata live in `.fan4compat-update`, outside
Fabric's mods directory. Only a regular Fan4Compat jar directly in mods can be
replaced automatically; nested/managed external origins are skipped. Its physical
filename stays the same, while its internal version changes to the downloaded
release. The helper jar contains only the standalone JDK installer class, so it
does not lock the original mod jar on Windows or need Minecraft/Fabric libraries.

On a graceful exit the helper waits for the parent process to end, checks both
staged and current hashes, stores the original under
`.fan4compat-update/backups/<original-sha256>.jar`, and atomically replaces the
original path. Failed replacements remain pending. It refuses a target changed
by a manual/pack update, rather than overwriting it. Backups are retained; delete
unneeded older backups manually. Stop the game/server before restoring a backup
over the existing mod jar; do not add a second Fan4Compat jar to mods.

Abrupt termination can prevent the helper launch. Managed hosts that restart instantly should use a pre-start hook so installation
finishes before Fabric discovers the mod jar. Some container/game hosts kill
child processes as soon as the game exits. For those hosts, run this command from
the game directory **before starting Minecraft/server**, if pending.properties
exists:

```sh
java -cp .fan4compat-update/installer.jar dev.fan4.compat.updater.UpdateInstaller --apply .fan4compat-update/pending.properties
```

The pre-start helper requires Java 17+ (the game uses Java 21). Add it to the host's
pre-start hook; it must run while Minecraft/server is stopped. A failed validation
keeps the old jar and reports details in `.fan4compat-update/installer.log` for the
normal shutdown helper. The server is never restarted automatically. Coordinate
client/server restarts when rolling out runtime compatibility changes.

## Publishing

Successful main push/workflow-dispatch builds publish a release named v<version>
with the exact clean-built jar and SHA256. Prerelease versions stay prereleases.
Bump build.gradle and add docs/releases/<version>.md for each published version.
Existing releases are left untouched, including when Actions is rerun. PR builds
only upload Actions artifacts. The updater accepts exact release jar names,
checksums and Minecraft 1.21.1 metadata from Livinglive234/fan4compat.
