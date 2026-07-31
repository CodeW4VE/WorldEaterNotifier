---
sidebar_position: 1
---

# Quick Start


The fastest way to get WorldEaterNotifier running with a webhook (the default delivery mode).

## Requirements

- **Java 21** or higher
- **Minecraft 1.21.11** server
- **Fabric Loader** 0.18.1 or higher
- **Fabric API** 0.141.4+1.21.11 (or compatible)

## 1. Install the mod

1. Build the JAR from source, or grab it from the [releases](https://github.com/CodeW4VE/WorldEaterNotifier/releases).
2. Copy `worldeaternotifier-*.jar` into your server's `mods` folder.
3. Start the server. The mod creates `config/worldeaternotifier.json` on first run.

## 2. Configure the webhook

1. In Discord, open your server → a text channel → **Edit Channel → Integrations → Webhooks → New Webhook**.
2. Copy the webhook URL (`https://discord.com/api/webhooks/...`).
3. In the game, run:

```
/worldeater settings setWebhookUrl <url>
```

That's it for delivery. Optionally set a role to ping:

```
/worldeater settings setPingRoleId <role-id>
```

See [Webhook Mode](webhook-setup) for the full guide.

## 3. Create a machine

Machines are defined by a name and the two opposite corners of a rectangular region (inclusive). Coordinates are entered as absolute block coordinates.

```
/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>
/trencher create <name> <x1> <y1> <z1> <x2> <y2> <z2>
/bedrockbreaker create <name> <x1> <y1> <z1> <x2> <y2> <z2>
```

Example:

```
/worldeater create my_eater 100 64 200 120 64 240
```

The corner order does not matter — the mod normalizes them.

## 4. Start monitoring

```
/worldeater start my_eater
```

A "started" notification is sent to Discord. The machine is now monitored:

- **World eaters**: if fewer than `minTntCount` lit TNT entities are in the region each second, the "activity clock" doesn't tick.
- **Trenchers / bedrock breakers**: if fewer than `minBlocksBroken` blocks are destroyed by an explosion in the region, activity doesn't tick.
- If no activity for `stopTimeout` seconds, a **stuck** notification is sent. If activity resumes, a **resumed** notification is sent.

Use `/worldeater stop <name>` to stop monitoring manually.

## 5. Shutdown behavior

When the server shuts down:

- All active machines are stopped (set to `active: false` in the config).
- A **shutdown** notification is sent for each active machine.
- On next server start, machines are **inactive** — you must start them again manually.

## Next steps

- Full in-game commands: [Minecraft Commands](minecraft-commands)
- Discord bot mode instead of webhook: [Bot Mode](bot-setup)
- Customizing notifications: [Message Templates](message-templates)
