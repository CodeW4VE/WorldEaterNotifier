<div align="center">

# WorldEaterNotifier

**Fabric mod that monitors world eaters, trenchers, and bedrock breakers, sending Discord notifications with per‑event ping control when they stop or get obstructed.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.16.14%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img width="480" height="270" alt="WorldEaterGithub_10fps" src="https://github.com/user-attachments/assets/af61e7c3-c241-4913-b961-632b3d1dceda" />

</div>

---

## What is it?

The mod supports **three independent machine types**, each with its own configurable stop timeout, minimum activity thresholds, and Discord ping settings:

- **`WorldEater`** — detects activity via lit TNT count.
- **`Trencher`** — detects activity via actual block destruction from explosions, ignoring TNT blocks.
- **`BedrockBreaker`** — detects activity via block destruction, tuned for slower bedrock-breaking setups.

Access to the commands can be restricted to op players and a shared whitelist, so you can let trusted non‑op players manage machines without giving them full server permissions.

## Features

- **Real-time monitoring**: Tracks world eaters (TNT-based), trenchers, and bedrock breakers (block break-based).
- **Discord integration**: Sends instant notifications when a machine stops, starts, resumes, or is stopped manually.
- **Role-based notifications**: Mention specific Discord roles with configurable per‑event toggles (global, start, manual stop, stuck, resumed, server shutdown). Mentions can be disabled while still sending the message.
- **Configurable**: Customize webhook, role ID, stop timeout, and minimum activity thresholds for each machine type.
- **Whitelist system**: Op players (permission level 2+) can always use every command. Non‑op players need to be on the shared whitelist. Only ops can add or remove names from it.
- **Multi-world support**: Monitor machines across different dimensions.
- **Persistent configuration**: Machine definitions and settings survive server restarts.
- **Server shutdown detection**: Automatically stops all active machines when the server shuts down and notifies Discord.

## Requirements

- [Java](https://www.java.com/) 21 or higher
- [Minecraft](https://www.minecraft.net/) 1.21.6 server with Fabric loader
- [Fabric Loader](https://fabricmc.net/) 0.16.14 or higher
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.128.0+1.21.6 or compatible

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/froyln/WorldEaterNotifier.git
   cd WorldEaterNotifier
   ```

2. Build the mod:
   ```bash
   ./gradlew build
   ```

3. Copy the generated `.jar` file from `build/libs/` to your server's `mods` folder:
   ```bash
   cp build/libs/worldeaternotifier-*.jar /path/to/server/mods/
   ```

4. Create a `worldeaternotifier-config.json` file in your server's `config` directory with Discord webhook and machine settings, or just start the server once and let the mod generate a default one.

## Configuration

The mod creates `config/worldeaternotifier.json` on first run. You can edit it manually or use the in‑game commands.

Example:

```json
{
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "pingRoleId": "123456789012345678",
  "worldEaterSettings": {
    "stopTimeoutSeconds": 60,
    "minTntCount": 20,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    }
  },
  "trencherSettings": {
    "stopTimeoutSeconds": 180,
    "minBlocksBroken": 3,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    }
  },
  "bedrockBreakerSettings": {
    "stopTimeoutSeconds": 180,
    "minBlocksBroken": 1,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    }
  },
  "worldEaters": [],
  "trenchers": [],
  "bedrockBreakers": [],
  "whitelist": []
}
```

- **webhookUrl**: Discord webhook URL. Shared across all three machine types.
- **pingRoleId**: ID of the Discord role to mention. Leave empty or `"0"` to disable all mentions.
- **stopTimeoutSeconds**: How many seconds without activity before the machine is considered stuck.
- **minTntCount / minBlocksBroken**: Minimum activity per check to keep the machine alive.
- **pingSettings**: Toggle global mentions (`enabled`) and individual event notifications.
- **whitelist**: Player names (case-insensitive) allowed to use the commands without being op. Shared by `/worldeater`, `/trencher`, and `/bedrockbreaker`.

All settings can be changed in‑game without restart.

## Permissions

- **Op players** (permission level 2+, including console) can always use every command for every machine type.
- **Non-op players** can only use the commands if their name is on the whitelist.
- **Only op players** can add or remove names from the whitelist.

Manage the whitelist with:

```
/worldeater settings whitelist list
/worldeater settings whitelist add <player>
/worldeater settings whitelist remove <player>
```

The same subcommands are available under `/trencher settings whitelist` and `/bedrockbreaker settings whitelist` — they all read and write the same shared list.

## Usage

1. Install the mod and start your server.
2. Configure the webhook and role ID with `/worldeater settings setWebhookUrl <url>` and `/trencher settings setWebhookUrl <url>` (all machine types share the same webhook).
3. Create machines:
   - `/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   - `/trencher create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   - `/bedrockbreaker create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   (Coordinates auto‑suggest the block you stand on; `Tab` completes names.)
4. Start monitoring with `/worldeater start <name>`, `/trencher start <name>`, or `/bedrockbreaker start <name>`.
5. The mod will automatically send Discord messages when a machine stops unexpectedly (stuck), resumes, or when it is manually stopped.
6. Adjust settings on the fly:
   - `/worldeater settings show` / `/trencher settings show` / `/bedrockbreaker settings show`
   - `/worldeater settings setStopTimeout <seconds>`
   - `/trencher settings setMinBlocksBroken <count>`
   - `/bedrockbreaker settings setMinBlocksBroken <count>`
   - `/worldeater settings discordPings enable true` and individual events like `onStuck false`
   - `/worldeater settings whitelist add <player>` (op only)
   - And more – use `Tab` to explore.
7. Manage machines: `list`, `stop`, `delete` for `/worldeater`, `/trencher`, and `/bedrockbreaker`.
8. When the server shuts down, all active machines are stopped and a shutdown notification is sent.

## Dependencies

- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://fabricmc.net/use/api/)
- Standard Minecraft & Java libraries

## Building from Source

Requires:
- [Gradle](https://gradle.org/) 8.12.1 or higher (automatically downloaded via gradlew)
- Java 21 or higher

Build command:
```bash
./gradlew clean build
```

Generated artifact: `build/libs/worldeaternotifier-*.jar`

## License

[MIT](LICENSE) © froyln
