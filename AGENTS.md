# WorldEaterNotifier

Fabric mod (Minecraft 1.21.9–1.21.10, **server-side only**) that monitors world eaters,
trenchers, and bedrock breakers and sends Discord notifications — via webhook or a JDA
bot — with per-event ping control when a machine starts, gets stuck/obstructed, resumes,
is stopped, or the server shuts down.

> **Working with Claude Code?** See [CLAUDE.md](CLAUDE.md). It points to this file for
> context and to `PLAN.md` (local-only, gitignored) for the current task tracker.

## Tech Stack

- **Language:** Java 21
- **Loader/API:** Fabric Loader 0.19.3, Fabric API 0.134.1+1.21.9
- **Build:** Gradle 9.6.1 + Fabric Loom 1.14.10 (older Loom fails to configure this MC
  version's toolchain — "Unsupported unpick version" — bumped from the 1.21.6 branch's
  1.10.5/Gradle 8.12.1, matching `main`'s versions)
- **Mappings:** Yarn 1.21.9+build.1
- **Version range:** this branch covers Minecraft 1.21.9 and 1.21.10 — confirmed by compiling
  the same source against both. It needed exactly one source change from the `1.21.6` branch:
  `GameProfile.getGameProfile().getName()` → `.name()` (this version's rename; `main`'s is
  further along and additionally swapped the whole op-permission check to the new
  `net.minecraft.command.permission` API — that break wasn't reached here, `hasPermissionLevel(2)`
  still compiles at 1.21.9/1.21.10). Built and shipped against the lowest (1.21.9). 1.21.6–1.21.8
  are the `1.21.6` branch (old `getName()`, old Loom/Gradle); 1.21.11 is `main` (new permission
  API too).
- **Mixin:** `ExplosionMixin` targets `ExplosionImpl.destroyBlocks`
- **Dependencies:**
  - Fabric API + Java stdlib (`java.net.http.HttpClient` for webhooks, **Gson** for
    config and webhook JSON payloads).
  - **JDA 5.2.1** (bot mode) — **shaded** into the output jar via the `shade`
    configuration and `remapJar` (see `build.gradle`), with `opus-java` excluded. No
    separate mod install is required for bot mode.
- **Java package:** `com.example.worldeaternotifier` (note: Maven group is
  `com.worldeaternotifier` — they intentionally differ).

## Build & Run

```bash
./gradlew clean build     # -> build/libs/worldeaternotifier-<version>.jar (JDA shaded in)
```

There are **no automated tests**. Verify behavior by loading the jar on a dev server and
exercising commands in-game and in Discord (see "Verifying changes" below).

## Project Structure

```
src/main/java/com/example/worldeaternotifier/
├── WorldEaterNotifierMod.java          # ModInitializer entrypoint; loads config, wires managers, registers events/commands
├── common/
│   ├── BaseMachineDefinition.java      # Immutable record: name, inclusive AABB coords, dimension
│   ├── BaseMachineInstance.java        # Runtime state: active, lastActivityTick, stuckAlertSent, detectionType
│   ├── MachineManager.java             # Generic per-type manager (ConcurrentHashMap<String, BaseMachineInstance> + ModConfig)
│   ├── MachineRegistry.java            # The 3 MachineManager instances (WORLD_EATER/TRENCHER/BEDROCK_BREAKER), keyed by type string
│   ├── MachineCommand.java             # Generic Brigadier command tree, parameterized per machine type
│   ├── DiscordNotifier.java            # Outbound notifications (webhook via HttpClient, or delegate to bot)
│   ├── ExplosionBlockCallback.java     # Fabric event carrying the destroyed-block list to listeners
│   └── PermissionManager.java          # Op / whitelist authorization gate for in-game commands
├── config/
│   └── ModConfig.java                  # Gson JSON config (load/save under config/worldeaternotifier.json)
├── bot/
│   └── DiscordBotManager.java          # JDA lifecycle, pending buffer, slash commands, buttons/selects
├── monitor/
│   └── MonitorCheckHandler.java        # Per-tick + explosion-callback detection logic
└── mixin/
    └── ExplosionMixin.java             # Captures pre/post block state around explosions
```

## Architecture

### Three machine types

WorldEater, Trencher and BedrockBreaker are all instances of the same generic classes,
not separate hierarchies: `MachineManager` (in-memory `ConcurrentHashMap<String,
BaseMachineInstance>` + a `ModConfig` reference, parameterized by a saved-list accessor
and a settings accessor) and `MachineCommand` (one Brigadier tree, parameterized by
machine type, display name, and which optional settings/args apply — `hasDetectionTypeArg`
for Trencher's quarry-like/2-way create arg, `hasMinTntCount`, `hasMinBlocksBroken`).
`MachineRegistry` holds the three `MachineManager` instances (`WORLD_EATER`, `TRENCHER`,
`BEDROCK_BREAKER`) keyed by the type string used everywhere else (Discord, config,
notifications). `WorldEaterNotifierMod` constructs the three `MachineCommand` instances
and registers them. All three share `BaseMachineDefinition`, `BaseMachineInstance`, and
`ModConfig.MachineSettings` (one settings shape for all types — a couple of fields go
unused per type, e.g. BedrockBreaker ignores `minTntCount`).

| Type | Detection | Config settings key |
|------|-----------|---------------------|
| WorldEater | Lit TNT entity count in AABB | `worldEaterSettings` |
| Trencher | Blocks destroyed by explosion in AABB (`quarry-like`), or TNT count (`2-way`) | `trencherSettings` |
| BedrockBreaker | Explosion-block detection (same as quarry-like trencher) | `bedrockBreakerSettings` |

### Detection mechanism

1. **TNT-based** (WorldEater, and `2-way` Trencher): every second
   (`CHECK_INTERVAL_TICKS = 20`), `MonitorCheckHandler.onWorldTick` scans each active
   machine's AABB with `world.getEntitiesByType(EntityType.TNT, box, ...)`. If the count
   ≥ `minTntCount`, `instance.updateLastActivityTick(currentTick)`.
2. **Block-break-based** (`quarry-like` Trencher, BedrockBreaker): `ExplosionMixin` hooks
   `ExplosionImpl.destroyBlocks` — captures block states at HEAD, and at TAIL filters to
   blocks that were non-air/non-TNT and are now air, firing `ExplosionBlockCallback.EVENT`
   with the destroyed positions. `MonitorCheckHandler.onExplosionBlocksDestroyed` counts
   those inside each active machine's AABB; if ≥ `minBlocksBroken`, updates activity tick.
3. **Stuck detection:** `checkStuck()` runs each tick per active machine. If
   `currentTick - lastActivityTick > stopTimeout * 20` and no stuck alert was sent yet, it
   sends a "stuck" notification and marks the flag. When activity resumes,
   `updateLastActivityTick` clears the flag and sends a "resumed" notification.

### State persistence

- Config at `config/worldeaternotifier.json` (Gson, pretty-printed).
- Machine definitions + last active state persist under `worldEaters`, `trenchers`,
  `bedrockBreakers` arrays. `ModConfig.load()` backfills nulls and clamps invalid numeric
  settings to defaults.
- On server start (`onInitialize`), machines load **inactive**; they must be `/start`ed.
- On `SERVER_STOPPING`, active machines get a shutdown notification, are stopped, config is
  saved with `active: false`, and the bot is shut down.

### Notification modes (`config.notificationMode`)

| Mode | Delivery | Configured with |
|------|----------|-----------------|
| `webhook` (default) | HTTPS POST to a Discord webhook URL | `setWebhookUrl`, `setPingRoleId` |
| `bot` | JDA bot: "Toggle Ping" button + slash commands | `setBotToken`, `setGuildId`, `setChannelId`, `setPingRoleId` |

- `DiscordNotifier` methods: `sendStart`, `sendStuck`, `sendResumed`,
  `sendManuallyStopped`, `sendServerShutdown`. Ping prefix built by
  `buildMentionIfAllowed` from `pingRoleId` + per-event toggles.
- **Start guard:** `executeStart()` calls `isDeliveryConfigured()` and refuses to start if
  the current mode's requirements are unmet (webhook → non-blank `webhookUrl`; bot →
  `botToken`+`guildId`+`channelId`).
- **Dynamic command visibility:** settings subcommands use Brigadier `.requires()`
  predicates on `notificationMode` so only the relevant ones are tab-completable per mode.

### Bot mode (`DiscordBotManager`)

Singleton using JDA 5.2.1 with the `GUILD_MEMBERS` intent.

- **Startup:** `JDABuilder.createDefault(token).build()` returns immediately; notifications
  queue (max 50) until the WebSocket is `CONNECTED`, flushed on `ReadyEvent`.
- **Shutdown/restart:** `jda.shutdown()` on server stop or mode switch; `setBotToken`
  triggers `restart(token)`. Guild/channel are read from config at send time — no restart
  needed for those.
- **Buttons:** a single "Toggle Ping" button (`wen:toggle:<type>:<name>`) lets a Discord
  user self-add/remove `pingRoleId`; reply is ephemeral. Shown on start messages only when
  `showSubscriptionButton` is true.
- **Slash commands:** `/config subscription-button|ping-role|channel|pings|member-discord-role`
  (Administrator only), and `/worldeater|/trencher|/bedrockbreaker start|stop|list` (gated
  by Administrator OR `memberDiscordRole`). `start`/`stop` autocomplete existing names, and
  the autocomplete handler itself is access-gated (see invariants below).
- **`/config pings` flow:** select machine type → embed of current settings → select a
  setting → True/False buttons, looping back to the picker.
- **Clearing `memberDiscordRole`:** `/config member-discord-role` takes an *optional* role
  option — omit it to clear the field (falls back to admin-only access). In-game,
  `settings setMemberDiscordRole none|clear|0` clears it the same way.
- **Role-deletion resilience:** a `RoleDeleteEvent` listener watches for the configured
  `memberDiscordRole` or `pingRoleId` being deleted in Discord. If either is deleted, the
  corresponding config field is cleared and saved automatically, and an in-game broadcast
  (`⚠ ...`) explains what happened — this avoids silent, permanent lockouts from a stale
  role reference.

### Configurable messages & pings

- Each machine type has a `messages` block (`MessageTemplates`: `start`, `stuck`,
  `resumed`, `manualStop`, `shutdown`) with `{type}`/`{name}` placeholders. Resolved by
  `DiscordNotifier.templatesFor(machineType)`.
- Each type has `PingSettings` (`enabled`, `onStart`, `onStop`, `onStuck`, `onResumed`,
  `onShutdown`), edited via in-game `discordPings` commands or `/config pings`. All persist
  to JSON.

## Authorization & security invariants

Keep these intact — they were established by a security hardening pass; regressing them
re-introduces known vulnerabilities.

- **In-game command gate** (`PermissionManager`): op = permission level
  GAMEMASTERS/2+; non-op players must be in the shared `whitelist`.
  - `create` / `start` / `stop` / `list` / `delete` / `settings show` / `discordPings` /
    `setStopTimeout` / `setMinTntCount` / `setMinBlocksBroken`: **op OR whitelisted** —
    these only tune detection behavior for a machine the whitelisted player is already
    allowed to operate; none of them touch secrets or delivery config.
  - **Secret-, delivery-, or bot-behavior-mutating settings are op-only:** `setWebhookUrl`,
    `setBotToken`, `setGuildId`, `setChannelId`, `setNotificationMode`,
    `setMemberDiscordRole`, `setPingRoleId`, `showSubscriptionButton`. Enforced via
    `.requires(... && PermissionManager.isOp(s))` (composed with the mode predicate where
    present). `showSubscriptionButton` is included here even though it isn't a secret,
    because it's a *global* Discord-message behavior toggle (affects every notification,
    for every user), not a per-machine tuning knob — same tier as `setNotificationMode`.
  - `whitelist add` / `remove`: **op-only**.
- **Secret masking:** `settings show` masks both the bot token and the webhook URL
  (`maskToken`). Do not print either in cleartext.
- **Webhook URL validation (anti-SSRF):** `setWebhookUrl` accepts only `https` URLs whose
  host is `discord.com`/`discordapp.com` (or a subdomain). Reject anything else.
- **Mention safety:** outbound messages restrict mentions to roles only — webhook payloads
  include `allowed_mentions: {parse: ["roles"]}` (built with Gson, not string concat), and
  JDA sends use `setAllowedMentions(EnumSet.of(Message.MentionType.ROLE))`. This prevents
  `@everyone`/`@here`/user-mention abuse from templates or names.
- **Discord interaction handlers** that mutate settings (`/config pings` buttons/selects)
  re-check `Permission.ADMINISTRATOR`; don't rely on the entry point being ephemeral.
- **Discord autocomplete is access-gated:** `onCommandAutoCompleteInteraction` calls
  `hasAccess(member)` (admin OR `memberDiscordRole`) before returning machine-name
  suggestions for `start`/`stop`, and replies with an empty list otherwise — a user who
  can't run the command can't enumerate machine names through autocomplete either.
- **`memberDiscordRole`/`pingRoleId` don't go stale silently:** deleting either role in
  Discord auto-clears the corresponding config field (via a `RoleDeleteEvent` listener) and
  broadcasts an in-game warning, instead of leaving a dangling ID that silently denies
  everyone. Both fields also support an explicit clear path (see "Bot mode" above).

A full in-game + Discord permission audit (root gate, per-subcommand gating, autocomplete,
`memberDiscordRole` end-to-end) found no open items — see `PLAN.md` for the detailed
matrices. Separately, some lower-priority hardening from the original security pass is
still open: `syncCommandTree` advertises the full command tree to every client
(info-disclosure only — server-side `.requires()` still blocks execution), secrets are
stored in plaintext at rest, and `ModConfig` mutation/`save()` isn't locked across the
server tick thread and JDA callback threads.

## In-game commands

`/worldeater`, `/trencher`, `/bedrockbreaker` share this structure (trencher adds a
`<type>` arg on `create`; TNT/blocks settings differ per type):

```
<command> create <name> [<type>] <x1> <y1> <z1> <x2> <y2> <z2>
<command> start|stop|delete <name>
<command> list
<command> settings show
<command> settings setWebhookUrl <url>            # webhook mode, op-only
<command> settings setBotToken|setGuildId|setChannelId <v>   # bot mode, op-only
<command> settings setMemberDiscordRole <roleId|none|clear|0>   # bot mode, op-only; keyword clears it
<command> settings setNotificationMode <webhook|bot>   # op-only
<command> settings setPingRoleId <roleId>         # op-only
<command> settings setStopTimeout <seconds>
<command> settings setMinTntCount <count>         # /worldeater (+ /trencher for 2-way)
<command> settings setMinBlocksBroken <count>     # /trencher, /bedrockbreaker
<command> settings showSubscriptionButton <bool>  # bot mode, op-only
<command> settings discordPings show|enable|onStart|onStop|onStuck|onResumed|onShutdown
<command> settings whitelist list|add|remove      # add/remove op-only
```

The whitelist is shared across all three commands.

## Key Conventions

- **Managers are `MachineManager` instances** held in `MachineRegistry` (`WORLD_EATER`,
  `TRENCHER`, `BEDROCK_BREAKER`), each holding a `ModConfig` reference.
- **Commands are `MachineCommand` instances**, one per type, constructed in
  `WorldEaterNotifierMod` and registered via `register(dispatcher, registryAccess, environment)`.
- **Machine types are strings:** `"WorldEater"`, `"Trencher"`, `"BedrockBreaker"` — used as
  `MachineRegistry` keys and passed through to `DiscordNotifier`/`BaseMachineInstance`.
- **Adding a machine type** = a new `MachineManager`/`MachineCommand` pair wired into
  `MachineRegistry` + `WorldEaterNotifierMod`, a settings section in `ModConfig`, and
  detection logic in `MonitorCheckHandler`.
- **No dependency injection** — manual wiring throughout, `MachineRegistry` is the one shared registry.
- **Coordinates** form an inclusive AABB using `Math.min/max` of the two corners.
- **Server-side only** (`"environment": "server"` in `fabric.mod.json`).
- When architecture or conventions change, update this file; when task status changes,
  update `PLAN.md`.

## Commit and comment rules for AI agents

- **Never add AI/session/model attribution to a commit.** No `Co-Authored-By: Claude` (or
  any other assistant) trailer, no `Claude-Session:`/session-link trailer, no mention of the
  model or tool used anywhere in the subject or body. Commit messages describe the change,
  not who or what wrote it. If a commit template or tool default appends this automatically,
  strip it before committing.
- **No unrequested comments.** Don't add class/method doc comments that just restate the
  name (`/** Manages the thing */` above `class ThingManager`), and don't leave prose
  explaining a design decision, a session's reasoning, or "why this file exists" in the code.
  If a decision needs explaining, put it in the commit message or `PLAN.md`, not a comment
  block. A comment is only worth adding for a genuinely non-obvious runtime constraint (an
  API quirk, a platform version gotcha, a workaround for a specific bug) — one or two lines,
  not a paragraph.
