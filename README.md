<div align="center">

# WorldEaterNotifier

Discord alerts when a world eater, trencher or bedrock breaker starts, stops or gets stuck.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11%20%2F%2026.3-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.5%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%20%2F%2025-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img width="480" height="270" alt="WorldEaterGithub_10fps" src="https://github.com/user-attachments/assets/af61e7c3-c241-4913-b961-632b3d1dceda" />

</div>

A W4VE fork of [Kisde's WorldEaterNotifier](https://modrinth.com/mod/worldeaternotifier), kept
up to date for Minecraft 1.21.11 and 26.3.

- Watches world eaters (by TNT), trenchers and bedrock breakers (by blocks broken) inside the
  areas you define.
- Sends a Discord message when a machine starts, stops, gets stuck, resumes or is stopped by hand,
  and when the server shuts down.
- Pings a Discord role, with a toggle per event.
- Webhook mode, or bot mode with a "Toggle Ping" button and slash commands (`/worldeater start|stop|list`, and the same for `/trencher` and `/bedrockbreaker`).
- Every message is editable, and thresholds (timeout, minimum TNT or blocks) are set per machine type.
- Works across dimensions. Server-side only.

## Installation

| Minecraft | Java | Fabric Loader | Fabric API |
| --- | --- | --- | --- |
| 1.21.11 | 21+ | 0.18.1+ | 0.141.4+1.21.11 |
| 26.3 | 25+ | 0.19.5+ | 0.161.0+26.3 |

Put the jar and [Fabric API](https://modrinth.com/mod/fabric-api) in the dedicated server's
`mods/`. Clients need nothing. JDA (the Discord library) is bundled. The first start creates
`config/worldeaternotifier.json`; keep it when updating. Machines load inactive after a restart.

## Usage

```
/worldeater settings setWebhookUrl <url>
/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>
/worldeater start <name>
```

`/trencher` and `/bedrockbreaker` work the same way. Coordinates suggest the block you stand on,
and `Tab` completes names and subcommands.

Full guide: [WorldEaterNotifier documentation](https://codew4ve.github.io/WorldEaterNotifier/).

<!-- modrinth:end -->

## Details

### All commands

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

### Configuration file

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
- **notificationMode**: `"webhook"` (default) or `"bot"`: switches between webhook and JDA bot delivery.
- **botToken**: Discord bot token (required for bot mode).
- **guildId** / **channelId**: Discord guild and channel for bot notifications and slash commands.
- **showSubscriptionButton**: Whether to show the "Toggle Ping" button on start messages (bot mode).
- **memberDiscordRole**: Discord role ID that gates `/worldeater start|stop|list` (and trencher/bedrockbreaker equivalents) via slash commands; admins always have access.
- **stopTimeoutSeconds**: How many seconds without activity before the machine is considered stuck.
- **minTntCount / minBlocksBroken**: Minimum activity per check to keep the machine alive.
- **pingSettings**: Per-machine-type ping toggles (`enabled`, `onStart`, `onStop`, `onStuck`, `onResumed`, `onShutdown`): these now **persist** across restarts.
- **messages**: Customizable Discord notification messages per machine type. Use `{type}` for the machine type name and `{name}` for the machine instance name.

## Building from Source

Requires:
- [Gradle](https://gradle.org/) 8.10.2 or higher (automatically downloaded via gradlew)
- Java 21 or higher

Build command:
```bash
./gradlew clean build
```

Generated artifact: `build/libs/worldeaternotifier-*.jar`

To build both supported versions, run `python3 tools/multiversion.py`. The jars are written to
`build/multiversion/`, using Java 21 for 1.21.11 and Java 25 for 26.3 (in CI from `JAVA_HOME` and
`JAVA_HOME_25_X64`; locally `~/.local/opt/jdk-21` and `~/.local/opt/jdk-25`).

## License

[MIT](LICENSE) © froyln

## Credits

Original mod by **Kisde**. This repository is the W4VE fork, maintained under the MIT license.
