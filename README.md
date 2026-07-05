<div align="center">

# WorldEaterNotifier

**Fabric mod for Minecraft 1.21 that notifies a Discord role when a world eater stops due to obstruction.**

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-62B47D?logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.3%2B-87CEEB?logo=fabric&logoColor=white)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=java&logoColor=white)](https://www.java.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## What is it?

A Fabric mod designed for Minecraft 1.21 servers that monitors world eater activity and sends real-time notifications to Discord. When a world eater stops due to obstruction, the mod instantly notifies a designated Discord role so your server staff can respond immediately.

## Features

- **Real-time monitoring**: Tracks world eater positions and status in real-time.
- **Discord integration**: Sends instant notifications when a world eater stops.
- **Role-based notifications**: Ping specific Discord roles for quick staff response.
- **Configurable**: Customize world eater dimensions, coordinates, and Discord webhook settings.
- **Multi-world support**: Monitor world eaters across multiple dimensions.

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

Create a `config/worldeaternotifier-config.json` file:

```json
{
  "discordWebhookUrl": "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN",
  "discordRoleId": "YOUR_ROLE_ID"
}
```

## Usage

1. Install the mod on your Fabric server.
2. Configure the `worldeaternotifier-config.json` file with your Discord webhook and world eater locations.
3. Start your server.
4. Use the `/worldeater` command to manage world eaters in-game:
   - `/worldeater add <name> <dimension>` - Add a new world eater to monitor
   - `/worldeater list` - List all monitored world eaters
   - `/worldeater remove <name>` - Stop monitoring a world eater

5. The mod will automatically send Discord notifications when monitored world eaters stop.

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
