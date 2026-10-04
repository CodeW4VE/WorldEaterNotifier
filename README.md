<div align="center">

# WorldEaterNotifier

**Fabric mod that monitors world eaters and trenchers, sending Discord notifications with per‑event ping control when they stop or get obstructed.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20%2F%2026.3-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.5%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%20%2F%2025-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img width="480" height="270" alt="WorldEaterGithub_10fps" src="https://github.com/user-attachments/assets/af61e7c3-c241-4913-b961-632b3d1dceda" />

</div>

---

## What is it?

Fabric mod that monitors **world eaters** (TNT-based), **trenchers**, and **bedrock breakers** (block destruction-based), sending Discord webhook notifications with per‑event ping control and fully customizable messages when they stop, start, or get obstructed.

## Documentation

**For a complete guide on how to configure all the features please visit the documentation:**
**[WorldEaterNotifier Documentation](https://codew4ve.github.io/WorldEaterNotifier/)**

## Features

- **Real-time monitoring**: Tracks world eaters (TNT count), trenchers, and bedrock breakers (block destruction).
- **Discord integration**: Sends instant notifications when a machine starts, stops, resumes, or is manually stopped.
- **Customizable messages**: Every notification message is configurable per machine type via JSON — use `{type}` and `{name}` placeholders.
- **Role-based notifications**: Mention specific Discord roles with per‑event toggles (global, start, manual stop, stuck, resumed, server shutdown). All configurable in‑game via commands.
- **Discord bot mode**: JDA-powered bot with interactive toggle button and slash commands (`/config`, `/worldeater start|stop|list`, `/trencher start|stop|list`, `/bedrockbreaker start|stop|list`) with autocomplete.
- **Configurable thresholds**: Stop timeout, minimum TNT count / blocks broken per machine type.
- **Multi-world support**: Monitor machines across different dimensions.
- **Persistent configuration**: Machine definitions, settings, and ping toggles survive server restarts. Messages are editable in JSON.
- **Server shutdown detection**: Automatically stops all active machines on shutdown and notifies Discord.

## Requirements

| Minecraft | Java | Fabric Loader | Fabric API |
| --- | --- | --- | --- |
| 1.21.11 | 21+ | 0.18.1+ | 0.141.4+1.21.11 |
| 26.3 | 25+ | 0.19.5+ | 0.161.0+26.3 |

Install on the dedicated server only. Clients do not need this mod. JDA is bundled; no separate Discord library is needed.

## Installation and updates

1. Download the jar matching your Minecraft version from [GitHub Releases](https://github.com/CodeW4VE/WorldEaterNotifier/releases) or [Modrinth](https://modrinth.com/mod/worldeaternotifier-w4ve).
2. Put it and the matching [Fabric API](https://modrinth.com/mod/fabric-api) jar in the server's `mods/` folder.
3. Start the server. The mod creates `config/worldeaternotifier.json` automatically.
4. Configure delivery and define your machines with the commands below.

Keep `config/worldeaternotifier.json` when updating. Machine definitions, settings, message templates, and the whitelist are retained. Machines load **inactive** after a restart; start them explicitly when ready.

To build both supported versions:

```bash
python3 tools/multiversion.py
```

The jars are written to `build/multiversion/`. The builder uses Java 21 for 1.21.11 and Java 25 for 26.3. In CI these are supplied by `JAVA_HOME` and `JAVA_HOME_25_X64`; locally they default to `~/.local/opt/jdk-21` and `~/.local/opt/jdk-25`.

## Configuration

The mod creates `config/worldeaternotifier.json` on first run. You can edit it manually or use the in‑game commands.

Example:

```json
{
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "pingRoleId": "123456789012345678",
  "notificationMode": "webhook",
  "botToken": "",
  "guildId": "",
  "channelId": "",
  "showSubscriptionButton": true,
  "memberDiscordRole": "",
  "worldEaterSettings": {
    "stopTimeoutSeconds": 60,
    "minTntCount": 3,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    },
    "messages": {
      "start": "{type} **'{name}'** has started.",
      "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
      "resumed": "{type} **'{name}'** has started again.",
      "manualStop": "{type} **'{name}'** was stopped manually.",
      "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
    }
  },
  "trencherSettings": {
    "stopTimeoutSeconds": 180,
    "minBlocksBroken": 20,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    },
    "messages": {
      "start": "{type} **'{name}'** has started.",
      "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
      "resumed": "{type} **'{name}'** has started again.",
      "manualStop": "{type} **'{name}'** was stopped manually.",
      "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
    }
  },
  "bedrockBreakerSettings": {
    "stopTimeoutSeconds": 60,
    "minBlocksBroken": 1,
    "pingSettings": {
      "enabled": true,
      "onStart": true,
      "onStop": true,
      "onStuck": true,
      "onResumed": true,
      "onShutdown": true
    },
    "messages": {
      "start": "{type} **'{name}'** has started.",
      "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
      "resumed": "{type} **'{name}'** has started again.",
      "manualStop": "{type} **'{name}'** was stopped manually.",
      "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
    }
  },
  "worldEaters": [],
  "trenchers": [],
  "bedrockBreakers": [],
  "whitelist": []
}
```

- **webhookUrl**: Discord webhook URL.
- **pingRoleId**: ID of the Discord role to mention. Leave empty or `"0"` to disable all mentions.
- **notificationMode**: `"webhook"` (default) or `"bot"` — switches between webhook and JDA bot delivery.
- **botToken**: Discord bot token (required for bot mode).
- **guildId** / **channelId**: Discord guild and channel for bot notifications and slash commands.
- **showSubscriptionButton**: Whether to show the "Toggle Ping" button on start messages (bot mode).
- **memberDiscordRole**: Discord role ID that gates `/worldeater start|stop|list` (and trencher/bedrockbreaker equivalents) via slash commands; admins always have access.
- **stopTimeoutSeconds**: How many seconds without activity before the machine is considered stuck.
- **minTntCount / minBlocksBroken**: Minimum activity per check to keep the machine alive.
- **pingSettings**: Per-machine-type ping toggles (`enabled`, `onStart`, `onStop`, `onStuck`, `onResumed`, `onShutdown`) — these now **persist** across restarts.
- **messages**: Customizable Discord notification messages per machine type. Use `{type}` for the machine type name and `{name}` for the machine instance name.

## Usage

1. Install the mod and start your server.
2. Configure delivery:
   - **Webhook mode** (default): `/worldeater settings setWebhookUrl <url>`
   - **Bot mode**: `/worldeater settings setBotToken <token>`, set `setGuildId` and `setChannelId`, then `/worldeater settings setNotificationMode bot`
3. Create machines:
   - `/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   - `/trencher create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   - `/bedrockbreaker create <name> <x1> <y1> <z1> <x2> <y2> <z2>`
   (Coordinates auto‑suggest the block you stand on; `Tab` completes names.)
4. Start monitoring with `/worldeater start <name>`, `/trencher start <name>`, or `/bedrockbreaker start <name>`.
5. The mod automatically sends Discord messages when a machine stops unexpectedly (stuck), resumes, or is manually stopped.
6. In bot mode, Discord slash commands mirror the Minecraft commands:
   - `/worldeater start <name>`, `/worldeater stop <name>`, `/worldeater list` (autocomplete suggests existing names)
   - Same for `/trencher` and `/bedrockbreaker`
   - `/config pings` — interactive flow to toggle per-machine-type ping settings
7. Adjust settings on the fly:
   - `/worldeater settings show`, `/trencher settings show`, `/bedrockbreaker settings show`
   - `/worldeater settings setStopTimeout <seconds>`
   - `/trencher settings setMinBlocksBroken <count>`
   - `/worldeater settings discordPings enable true` and individual events like `onStuck false`
   - `/worldeater settings showSubscriptionButton <true|false>` (bot mode)
   - `/worldeater settings setMemberDiscordRole <roleId>` (bot mode)
   - Edit notification messages directly in the JSON file.
   - Use `Tab` to explore all subcommands.
8. Manage machines: `list`, `stop`, `delete` for all three types.
9. When the server shuts down, all active machines are stopped and a shutdown notification is sent.

## Dependencies

- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://fabricmc.net/use/api/)
- Standard Minecraft & Java libraries

## Building from Source

Requires:
- [Gradle](https://gradle.org/) 8.10.2 or higher (automatically downloaded via gradlew)
- Java 21 or higher

Build command:
```bash
./gradlew clean build
```

Generated artifact: `build/libs/worldeaternotifier-*.jar`

## License

[MIT](LICENSE) © froyln

## Credits

Original mod by **Kisde**. This repository is the W4VE fork, maintained under the MIT license.
