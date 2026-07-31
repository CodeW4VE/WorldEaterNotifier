<div align="center">

# WorldEaterNotifier

**Fabric mod that monitors world eaters, trenchers and bedrock breakers, sending Discord notifications with per‑event ping control when they stop or get obstructed.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img width="480" height="270" alt="WorldEaterGithub_10fps" src="https://github.com/user-attachments/assets/af61e7c3-c241-4913-b961-632b3d1dceda" />

</div>

---

## What is it?

Monitors three machine types: **WorldEater** (TNT‑based), **Trencher** (block‑break‑based) and **BedrockBreaker**. Each has its own timeout, thresholds, Discord ping settings, and message templates. Supports both **Discord webhooks** and a **Discord bot** (JDA) with slash commands, autocomplete, and interactive ping toggles.

## Features

- **Real‑time monitoring** for world eaters, trenchers, and bedrock breakers.
- **Dual Discord delivery** — classic webhook or full JDA bot.
- **Configurable messages** per machine type using `{type}`/`{name}` placeholders.
- **Role‑based pings** with per‑event toggles (global, start, stop, stuck, resumed, shutdown).
- **Discord bot mode** — slash commands (`/worldeater`, `/trencher`, `/bedrockbreaker`, `/config`), autocomplete, subscription button.
- **Multi‑world** support across different dimensions.
- **Persistent configuration** — machines and settings survive restarts.
- **Server shutdown detection** — auto‑stops and notifies.

## Requirements

- Java 21 or higher
- Minecraft 1.21.6 server with Fabric loader
- Fabric Loader 0.16.14 or higher
- Fabric API 0.128.0+1.21.6 or compatible

## Installation

```bash
git clone https://github.com/froyln/WorldEaterNotifier.git
cd WorldEaterNotifier
./gradlew build
cp build/libs/worldeaternotifier-*.jar /path/to/server/mods/
```

## Configuration

The mod creates `config/worldeaternotifier.json` on first run. Edit manually or use in‑game commands.

### Webhook mode (default)

```json
{
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "pingRoleId": "123456789012345678",
  "notificationMode": "webhook",
  "worldEaterSettings": {
    "stopTimeoutSeconds": 60,
    "minTntCount": 3,
    "messages": {
      "start": "{type} **'{name}'** has started.",
      "stuck": "{type} **'{name}'** has stopped due to an obstruction.",
      "resumed": "{type} **'{name}'** has started again.",
      "manualStop": "{type} **'{name}'** was stopped manually.",
      "shutdown": "{type} **'{name}'** was shut down with the server and may have broken."
    }
  },
  ...
}
```

### Bot mode

Set `"notificationMode": "bot"` and add `botToken`, `guildId`, `channelId`.  
Use `/config` slash commands to manage pings, role, channel.

## Usage

```text
/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>
/worldeater start <name>
/worldeater stop <name>
/worldeater list
/worldeater settings show
/worldeater settings setNotificationMode webhook|bot
```

Coordinates auto‑suggest from your position. Tab completes machine names.

### Discord bot slash commands

| Command | Description |
|---------|-------------|
| `/worldeater start\|stop\|list` | Manage world eaters |
| `/trencher start\|stop\|list` | Manage trenchers |
| `/bedrockbreaker start\|stop\|list` | Manage bedrock breakers |
| `/config subscription-button` | Toggle subscribe button on start messages |
| `/config ping-role` | Set the ping role |
| `/config channel` | Set notification channel |
| `/config pings` | Configure per‑event ping settings via UI |

## Building

```bash
./gradlew clean build
# Output: build/libs/worldeaternotifier-*.jar
```

## License

[MIT](LICENSE) © froyln
