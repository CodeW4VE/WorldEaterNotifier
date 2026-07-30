# WorldEaterNotifier

Fabric mod (Minecraft 1.21.11, server-side only) that monitors world eaters, trenchers, and bedrock breakers, sending Discord webhook notifications with per-event ping control when they stop or get obstructed.

## Tech Stack

- **Language:** Java 21
- **Framework:** Fabric Loader 0.18.1, Fabric API 0.141.4+1.21.11
- **Build:** Gradle (Fabric Loom 1.14.10)
- **Mappings:** Yarn 1.21.11+build.4
- **Dependencies:** None beyond Fabric API + stdlib (java.net.http for Discord webhooks, Gson for config)
- **Mixin:** ExplosionMixin targeting `ExplosionImpl.destroyBlocks`

## Project Structure

```
src/main/java/com/example/worldeaternotifier/
├── WorldEaterNotifierMod.java          # Mod entrypoint (ModInitializer)
├── common/
│   ├── BaseMachineDefinition.java      # Immutable record: name, AABB coords, dimension
│   ├── BaseMachineInstance.java        # Runtime state: active, lastActivityTick, stuckAlertSent
│   ├── DiscordNotifier.java            # HTTP client for Discord webhooks (sendAsync)
│   ├── ExplosionBlockCallback.java     # Fabric event firing block-destroyed list to listeners
│   └── PermissionManager.java          # Permission/whitelist gate for commands
├── config/
│   └── ModConfig.java                  # Gson-based JSON config (load/save to config dir)
├── worldeater/
│   ├── WorldEaterManager.java          # Singleton registry + CRUD for WorldEater instances
│   └── WorldEaterCommand.java          # Brigadier command tree for /worldeater
├── trencher/
│   ├── TrencherManager.java            # Singleton registry + CRUD for Trencher instances
│   └── TrencherCommand.java            # Brigadier command tree for /trencher
├── bedrockbreaker/
│   ├── BedrockBreakerManager.java      # Singleton registry + CRUD for BedrockBreaker instances
│   └── BedrockBreakerCommand.java      # Brigadier command tree for /bedrockbreaker
├── monitor/
│   └── MonitorCheckHandler.java        # Per-tick + explosion callback logic
└── mixin/
    └── ExplosionMixin.java             # Mixin: captures pre/post state of explosion blocks
```

## Architecture

### Three machine types

Each machine type follows the same pattern: a `*Manager` (singleton, in-memory `ConcurrentHashMap<String, BaseMachineInstance>`) and a `*Command` (Brigadier command tree). They share the same `BaseMachineDefinition` and `BaseMachineInstance` classes.

| Type | Detection | Config settings key |
|------|-----------|-------------------|
| WorldEater | Lit TNT entity count in AABB | `worldEaterSettings` |
| Trencher | Non-TNT blocks destroyed by explosion in AABB | `trencherSettings` |
| BedrockBreaker | Same explosion-block detection as Trencher | `bedrockBreakerSettings` |

### Detection mechanism

1. **TNT-based (WorldEater):** Every second (`CHECK_INTERVAL_TICKS = 20`), `MonitorCheckHandler.onWorldTick` scans each active WorldEater's AABB via `world.getEntitiesByType(EntityType.TNT, box, ...)`. If count >= `minTntCount`, `instance.updateLastActivityTick(currentTick)` is called.

2. **Block-break-based (Trencher, BedrockBreaker):** `ExplosionMixin` hooks `ExplosionImpl.destroyBlocks` — before destruction it captures block states, after it filters out air/TNT and fires `ExplosionBlockCallback.EVENT` with the list of actually-destroyed positions. `MonitorCheckHandler.onExplosionBlocksDestroyed` counts how many fall inside each active machine's AABB. If count >= `minBlocksBroken`, updates activity tick.

3. **Stuck detection:** `checkStuck()` runs every tick for all active machines. If `currentTick - lastActivityTick > stopTimeout * 20` and no stuck alert has been sent yet, it sends a Discord "stuck" notification and marks the alert sent. If activity resumes (`updateLastActivityTick` clears the flag), a "resumed" notification is sent.

### State persistence

- Config stored at `config/worldeaternotifier.json` (Gson).
- Machine definitions + active state persisted in the same JSON file under `worldEaters`, `trenchers`, `bedrockBreakers` arrays.
- On server start (`WorldEaterNotifierMod.onInitialize`), all machines are loaded but set inactive. They must be manually `/start`ed.
- On server shutdown (`SERVER_STOPPING`), all active machines are stopped, shutdown notifications sent, and config saved with `active: false`.

### Discord notifications

Sent asynchronously via `java.net.http.HttpClient.sendAsync`. Methods: `sendStart`, `sendStuck`, `sendResumed`, `sendManuallyStopped`, `sendServerShutdown`. Ping built from `buildMentionIfAllowed` using `pingRoleId` and per-event toggles.

### Configurable messages

Each machine type has its own `messages` block in the JSON config (under `worldEaterSettings`, `trencherSettings`, `bedrockBreakerSettings`). The `MessageTemplates` class holds 5 templates (`start`, `stuck`, `resumed`, `manualStop`, `shutdown`) using `{type}`/`{name}` placeholders. `DiscordNotifier.templatesFor(machineType)` resolves which set to use.

### PingSettings are runtime-only

`PingSettings` (booleans: `enabled`, `onStart`, `onStop`, `onStuck`, `onResumed`, `onShutdown`) and their reference in each settings class are marked `transient` — Gson skips them during serialization. They are initialized to defaults on load and modified via `discordPings` commands in-memory. Changes are not persisted to JSON.

### Permissions

- Op players (permission level GAMEMASTERS/2+) can always use commands.
- Non-op players must be in the shared `whitelist` in config.
- Whitelist management requires op.

## Key Conventions

- **Managers are singletons** with `getInstance()`, hold a `ModConfig` reference.
- **Commands are static** with `register(CommandDispatcher, CommandRegistryAccess, RegistrationEnvironment)`.
- **Machine types are strings:** `"WorldEater"`, `"Trencher"`, `"BedrockBreaker"`.
- **New machine type = new package** with `*Manager` + `*Command`, add config section in `ModConfig`, wire in `WorldEaterNotifierMod`, add detection logic in `MonitorCheckHandler`.
- **No dependency injection.** Everything is manual singleton wiring.
- **Coordinates are inclusive** AABB using `Math.min/max` of two corner positions.
- **Server-side only** (`"environment": "server"` in `fabric.mod.json`).

## Build & Run

```bash
./gradlew clean build          # produces build/libs/worldeaternotifier-*.jar
```

## Commands

Each machine type has `/worldeater`, `/trencher`, `/bedrockbreaker` with identical structure:

```
<command> create <name> <x1> <y1> <z1> <x2> <y2> <z2>
<command> start <name>
<command> stop <name>
<command> list
<command> delete <name>
<command> settings show
<command> settings setWebhookUrl <url>
<command> settings setPingRoleId <roleId>
<command> settings setStopTimeout <seconds>
<command> settings setMinTntCount <count>          # /worldeater only
<command> settings setMinBlocksBroken <count>      # /trencher and /bedrockbreaker
<command> settings discordPings show/enable/onStart/onStop/onStuck/onResumed/onShutdown
<command> settings whitelist list/add/remove
```

Whitelist is shared across all three commands.
