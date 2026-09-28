# Changes in the unofficial 26.1.2 port

## Unreleased

Based on upstream Creeper Healing 2.1.4 (commit [`4fcf0a0`](https://github.com/ArkoSammy12/creeper-healing/commit/4fcf0a002218dd6d2080f6c1c413a81c537897c1)). These changes are not in the existing `v2.1.4+26.1.2-unofficial` prerelease.

- Rename the port's mod ID from `creeperhealing_2612` to `creeperhealing_sylvan` and its display name to "Creeper Healing: Sylvan Edition (Unofficial)". Remove the old JAR before updating so two copies do not handle the same explosions. Existing `scheduled-explosions.json` world saves and `config/creeper-healing.toml` retain their paths.

### Behavior to know before updating

- Normal sequential block healing follows upstream timing. Blast-resistance mode can still heal in bursts because its block delays have random offsets.
- Daytime tasks now wait and recheck for light if all affected positions are unlit before the first block heals. Upstream ends such a task at that point.
- Blocks that cannot be restored remain pending and retry without a fixed limit. Upstream may skip or finish them. A block from an old explosion can therefore reappear after a player removes a later construction at that position. There is no in-game command to cancel one pending task; disabling a healing source affects only new explosions.

### Reliability

- Keep pending blocks when placement fails, preserve their normal sequential delay, and isolate collision checks by dimension.
- Preserve saved mode-specific block delays across restarts. Upstream resets later blocks to the ordinary configured delay during startup.
- Save new tasks promptly, attempt progress saves within 200 server ticks, and checkpoint active waiting tasks every 1200 server ticks. Preserve unreadable save files and write replacements through a temporary file with a backup. A crash or failed write can still roll back progress since the last successful save.
- Skip additional explosion capture when healing is disabled for that source, bound indirect block collection, and return a failed command result when a config value cannot be set.
