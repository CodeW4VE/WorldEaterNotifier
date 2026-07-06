<div align="center">

# WorldEaterNotifier

**Fabric mod for Minecraft that monitors world eaters and trenchers, sending Discord notifications when they stop due to obstruction or inactivity.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

<img width="480" height="270" alt="WorldEaterGithub_10fps" src="https://github.com/user-attachments/assets/af61e7c3-c241-4913-b961-632b3d1dceda" />

</div>

---

## What is it?

The mod now supports **two independent machine types**: `WorldEater` (detects activity via lit TNT count) and `Trencher` (detects activity via actual block destruction from explosions, ignoring TNT blocks). Each type has its own configurable stop timeout, minimum activity thresholds, and Discord ping settings.

## Features

- **Real-time monitoring**: Tracks world eaters (TNT-based) and trenchers (block break-based).
- **Discord integration**: Sends instant notifications when a machine stops, starts, resumes, or is stopped manually.
- **Role-based notifications**: Mention specific Discord roles with configurable per‑event toggles (global, start, manual stop, stuck, resumed, server shutdown). Mentions can be disabled while still sending the message.
- **Configurable**: Customize webhook, role ID, stop timeout, and minimum activity thresholds for each machine type.
- **Multi-world support**: Monitor machines across different dimensions.
- **Persistent configuration**: Machine definitions and settings survive server restarts.
- **Server shutdown detection**: Automatically stops all active machines when the server shuts down and notifies Discord.

## Requirements

- [Java](https://www.java.com/) 21 or higher
- [Minecraft](https://www.minecraft.net/) 1.21 server with Fabric loader
- [Fabric Loader](https://fabricmc.net/) 0.19.3 or higher
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.100.8+1.21 or compatible

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

4. Create a `worldeaternotifier-config.json` file in your server's `config` directory with Discord webhook and world eater settings.

## Configuration

The mod creates `config/worldeaternotifier.json` on first run. You can edit it manually or use the in‑game commands.

Example:

```json
{
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "pingRoleId": "123456789012345678",
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
    }
  },
  "worldEaters": [],
  "trenchers": []
}
```

- **webhookUrl**: Discord webhook URL.
- **pingRoleId**: ID of the Discord role to mention. Leave empty or `"0"` to disable all mentions.
- **stopTimeoutSeconds**: How many seconds without activity before the machine is considered stuck.
- **minTntCount / minBlocksBroken**: Minimum activity per check to keep the machine alive.
- **pingSettings**: Toggle global mentions (`enabled`) and individual event notifications.

All settings can be changed in‑game without restart.

## Usage

1. Install the mod and start your server.  
2. Configure the webhook and role ID with `/worldeater settings setWebhookUrl <url>` and `/trencher settings setWebhookUrl <url>` (they share the same webhook).  
3. Create machines:
   - `/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>`  
   - `/trencher create <name> <x1> <y1> <z1> <x2> <y2> <z2>`  
   (Coordinates auto‑suggest the block you stand on; `Tab` completes names.)
4. Start monitoring with `/worldeater start <name>` or `/trencher start <name>`.
5. The mod will automatically send Discord messages when a machine stops unexpectedly (stuck), resumes, or when it is manually stopped.
6. Adjust settings on the fly:
   - `/worldeater settings show` / `/trencher settings show`
   - `/worldeater settings setStopTimeout <seconds>`
   - `/trencher settings setMinBlocksBroken <count>`
   - `/worldeater settings discordPings enable true` and individual events like `onStuck false`
   - And more – use `Tab` to explore.
7. Manage machines: `list`, `stop`, `delete` for both `/worldeater` and `/trencher`.
8. When the server shuts down, all active machines are stopped and a shutdown notification is sent.

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
