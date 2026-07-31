---
sidebar_position: 2
---

# Minecraft Commands


All three machine types have the same command structure. Commands are gated by the [permission system](faq) — op players always have access; non-op players must be whitelisted.

- `/worldeater` — TNT-based machines
- `/trencher` — block-destruction machines
- `/bedrockbreaker` — block-destruction machines

Each command supports the following subcommands (using `/worldeater` as the example):

## Create

```
/worldeater create <name> <x1> <y1> <z1> <x2> <y2> <z2>
```

Defines a rectangular monitoring region. The two corners are normalized, so order doesn't matter. The dimension is the dimension you're in when you run the command.

## Start / Stop

```
/worldeater start <name>
/worldeater stop <name>
```

Start begins monitoring and sends a "started" Discord notification. Stop ends monitoring and sends a "manually stopped" notification.

> **Start guard**: the mod refuses to start a machine if the current notification mode isn't configured (webhook mode needs a `webhookUrl`; bot mode needs `botToken` + `guildId` + `channelId`).

## List / Delete

```
/worldeater list
/worldeater delete <name>
```

`list` shows all machines with their active/inactive status. `delete` removes a machine definition.

## Settings

```
/worldeater settings show
```

Shows the current settings for that machine type.

### Delivery settings

| Command | Mode | Description |
|---|---|---|
| `settings setWebhookUrl <url>` | webhook | Sets the Discord webhook URL (shared across all three types). |
| `settings setPingRoleId <roleId>` | both | Role mentioned when pings are enabled. `0` or empty disables mentions. |
| `settings setBotToken <token>` | bot | Sets the bot token and restarts the bot. |
| `settings setGuildId <id>` | bot | Sets the Discord guild (server) ID. |
| `settings setChannelId <id>` | bot | Sets the Discord channel for notifications. |
| `settings setMemberDiscordRole <roleId>` | bot | Role that can use the Discord start/stop/list slash commands. |
| `settings setNotificationMode <webhook\|bot>` | both | Switches delivery mode and updates visible commands. |
| `settings showSubscriptionButton <true\|false>` | bot | Show/hide the "Toggle Ping" button on start messages. |

Settings commands that are irrelevant to the current mode are hidden from tab-completion. For example, bot-only settings only appear when `notificationMode` is `bot`.

### Detection thresholds

| Command | Applies to | Default |
|---|---|---|
| `settings setStopTimeout <seconds>` | all | 60 (world eater / bedrock breaker), 180 (trencher) |
| `settings setMinTntCount <count>` | world eater | 20 |
| `settings setMinBlocksBroken <count>` | trencher / bedrock breaker | 3 / 1 |

### Discord ping toggles

```
/worldeater settings discordPings show
/worldeater settings discordPings enable <true|false>
/worldeater settings discordPings onStart <true|false>
/worldeater settings discordPings onStop <true|false>
/worldeater settings discordPings onStuck <true|false>
/worldeater settings discordPings onResumed <true|false>
/worldeater settings discordPings onShutdown <true|false>
```

Each toggle controls whether the ping role is mentioned for that event. The global `enable` must be `true` for any per-event toggle to take effect. These persist in the JSON config across restarts.

### Whitelist

```
/worldeater settings whitelist list
/worldeater settings whitelist add <player>
/worldeater settings whitelist remove <player>
```

The whitelist is **shared** across all three commands. Op players always have access; non-op players must be on the list. Only op players can add/remove names.
