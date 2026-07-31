---
sidebar_position: 6
---

# FAQ


## Discord slash commands don't appear

Commands are registered when the bot connects. If you just switched to bot mode or updated the bot, restart the server so the bot reconnects and re-registers commands. If they still don't show:

1. Make sure `notificationMode` is `bot`.
2. Verify `botToken`, `guildId`, and `channelId` are set and correct.
3. Confirm the bot was invited with the `applications.commands` scope.

## "You won't get pinged" / "Failed to assign role" when clicking Toggle Ping

The bot needs **Manage Roles** permission, and the bot's role must be **above** the ping role in Server Settings → Roles. Move the bot's role higher and try again.

## Machines won't start ("delivery not configured")

The mod refuses to start machines if the current mode isn't set up:

- **Webhook mode**: `webhookUrl` must be set (`/worldeater settings setWebhookUrl <url>`).
- **Bot mode**: `botToken`, `guildId`, and `channelId` must all be set.

## Can I use webhook and bot mode at the same time?

No. `notificationMode` is either `webhook` or `bot` — only one is active at a time.

## Who can use the commands?

**In game**: op players always; non-op players only if whitelisted (`settings whitelist add <player>`).

**On Discord**: `/config` is Administrator-only. `/worldeater` `/trencher` `/bedrockbreaker` start/stop/list require the `member-discord-role` **or** Administrator. The Toggle Ping button is open to everyone (it only affects the clicker).

The in-game whitelist does **not** apply to Discord commands.

## The bot doesn't connect

- Double-check the token (reset it in the Developer Portal if unsure).
- Check server logs for the bot error at startup.
- Make sure the server has outbound internet access (Discord API).
- Ensure `guildId` matches the server the bot was invited to.

## Nothing is detected even though the machine is running

Check the thresholds:

- **World eater**: `minTntCount` — how many lit TNT entities must be in the region each second. If your machine uses fewer, lower it.
- **Trencher / bedrock breaker**: `minBlocksBroken` — blocks destroyed per explosion. Bedrock breakers default to 1.
- **`stopTimeout`**: seconds of no activity before "stuck" fires. Large machines may legitimately pause; raise the timeout if you get false stuck alerts.

## Machines reset to inactive after a restart

This is by design. On server start every machine loads as **inactive** and must be started manually with `/worldeater start <name>` (or Discord slash command). Active machines are auto-stopped on shutdown and a shutdown notification is sent.
